package dev.breda.beats.core

import kotlin.math.*

enum class FxMode(val label:String,val description:String) {
    STUDIO("STUDIO","Passa-baixas, saturação e delay sincronizado nos controles."),
    FILTER_SWEEP("FILTER SWEEP","Filtro passa-baixas com cutoff ajustável."),
    VINYL_COLOR("VINYL SIM","Filtro, saturação e pequenas variações de rotação e ruído."),
    LO_FI("LO-FI","Reduza a taxa de amostragem e a resolução de bits."),
    ROOM_REVERB("ROOM REVERB","Adicione uma ambiência estéreo curta."),
    BEAT_REPEAT("BEAT REPEAT","Capture uma semicolcheia e repita no BPM atual."),
    DJFX_LOOPER("DJFX LOOPER","Loop sincronizado com tamanho, velocidade/direção e mix nos três controles."),
    TAPE_ECHO("TAPE ECHO","Eco ping-pong sincronizado com realimentação filtrada."),
    ISOLATOR("ISOLATOR","Misture o sinal original com médios removidos."),
    SCATTER("SCATTER","Fragmentos curtos do áudio são reorganizados em tempo real."),
    SLICER("SLICER","Recorte rítmico sincronizado ao BPM."),
    CHORUS("CHORUS","Atraso modulado para ampliar e engrossar o som."),
    COMPRESSOR("COMPRESSOR","Controle picos e aproxime a dinâmica.")
}

data class FxSettings(
    val filter:Float=1f,
    val drive:Float=0f,
    val delay:Float=0f,
    val mode:FxMode=FxMode.STUDIO,
    val amount:Float=.55f
) {
    init {
        require(listOf(filter,drive,delay,amount).all{it.isFinite() && it in 0f..1f})
    }

    fun selectMode(next:FxMode):FxSettings {
        if(next==mode)return this
        return when(next) {
            FxMode.DJFX_LOOPER -> copy(mode=next,amount=.2f,drive=1f,delay=.8f)
            FxMode.TAPE_ECHO -> copy(mode=next,amount=.45f,drive=0f,delay=.4f)
            else -> if(mode==FxMode.DJFX_LOOPER) copy(mode=next,drive=0f,delay=0f) else copy(mode=next)
        }
    }
}

class FxProcessor(private val rate:Int=44100) {
    private val delayL=FloatArray(rate*2)
    private val delayR=FloatArray(rate*2)
    private var position=0
    private var lastL=0f
    private var lastR=0f
    private var sweepL=0f
    private var sweepR=0f
    private var sweepAlpha=1f
    private var tapeL=0f
    private var tapeR=0f
    private var tapeAlpha=1f
    private var vinylPhase=0.0
    private var vinylRandom=0x475B7A31
    private var crushL=0f
    private var crushR=0f
    private var crushCounter=0
    private val repeatL=FloatArray(rate)
    private val repeatR=FloatArray(rate)
    private var repeatLength=max(1,rate/4)
    private var repeatPosition=0
    private var repeatCaptured=false
    private val djLoopL=FloatArray(rate*2)
    private val djLoopR=FloatArray(rate*2)
    private var djLength=max(1,rate/4)
    private var djWrite=0
    private var djRead=0.0
    private var djCaptured=false
    private val scatterL=FloatArray((rate/2).coerceAtLeast(1))
    private val scatterR=FloatArray((rate/2).coerceAtLeast(1))
    private var scatterPosition=0
    private var scatterFilled=0
    private var scatterRead=0
    private var scatterLength=max(1,rate/100)
    private var scatterRemaining=0
    private var scatterRandom=0x1648A239
    private val chorusL=FloatArray((rate/8).coerceAtLeast(32))
    private val chorusR=FloatArray((rate/8).coerceAtLeast(32))
    private var chorusPosition=0
    private var chorusPhase=0.0
    private var isolatorLowL=0f
    private var isolatorLowR=0f
    private var isolatorMidL=0f
    private var isolatorMidR=0f
    private var isolatorLowAlpha=1f
    private var isolatorMidAlpha=1f
    private var compressorEnvelope=0f
    private var slicePosition=0
    private var beatFrames=rate
    private val roomSizes=intArrayOf(
        (rate*.0297).roundToInt().coerceAtLeast(1),
        (rate*.0371).roundToInt().coerceAtLeast(1),
        (rate*.0411).roundToInt().coerceAtLeast(1),
        (rate*.0533).roundToInt().coerceAtLeast(1)
    )
    private val roomL=Array(roomSizes.size){FloatArray(roomSizes[it])}
    private val roomR=Array(roomSizes.size){FloatArray(roomSizes[it])}
    private val roomPositions=IntArray(roomSizes.size)
    private var config=FxSettings()
    private var alpha=1f
    private var delayFrames=rate/4

    var left=0f
        private set
    var right=0f
        private set

    fun configure(settings:FxSettings,bpm:Int) {
        if(config.mode!=settings.mode)resetPerformanceState()
        config=settings
        val cutoff=40.0+settings.filter*settings.filter*17960
        alpha=if(settings.filter>=.999f)1f else (1-exp(-2*PI*cutoff/rate)).toFloat()
        val sweepCutoff=80.0+settings.amount*settings.amount*15920
        sweepAlpha=(1-exp(-2*PI*sweepCutoff/rate)).toFloat()
        val tapeCutoff=14000.0-settings.amount*10000
        tapeAlpha=(1-exp(-2*PI*tapeCutoff/rate)).toFloat()
        isolatorLowAlpha=(1-exp(-2*PI*180/rate)).toFloat()
        isolatorMidAlpha=(1-exp(-2*PI*4200/rate)).toFloat()
        val safeBpm=bpm.coerceIn(40,200)
        beatFrames=(60.0/safeBpm*rate).roundToInt().coerceAtLeast(1)
        delayFrames=(beatFrames/2).coerceIn(1,delayL.size-1)
        repeatLength=(beatFrames/4).coerceIn(1,repeatL.size)
        val divisions=intArrayOf(1,2,4,8,16,32)
        val division=divisions[(settings.amount*5).roundToInt().coerceIn(0,divisions.lastIndex)]
        djLength=(beatFrames/division).coerceIn(1,djLoopL.size)
        if(djRead>=djLength)djRead%=djLength
        scatterLength=(beatFrames/division).coerceIn(1,scatterL.size)
    }

    fun process(l:Float,r:Float) {
        lastL+=alpha*(l-lastL)
        lastR+=alpha*(r-lastR)
        var a=lastL
        var b=lastR
        if(config.mode!=FxMode.DJFX_LOOPER && config.drive>0f) {
            val gain=1+config.drive*7
            a=tanh(a*gain)/tanh(gain)
            b=tanh(b*gain)/tanh(gain)
        }

        var delayMix=config.delay
        var delayFeedback=.35f
        var pingPong=false
        when(config.mode) {
            FxMode.STUDIO -> Unit
            FxMode.FILTER_SWEEP -> {
                sweepL+=sweepAlpha*(a-sweepL)
                sweepR+=sweepAlpha*(b-sweepR)
                a=sweepL
                b=sweepR
            }
            FxMode.VINYL_COLOR -> {
                tapeL+=tapeAlpha*(a-tapeL)
                tapeR+=tapeAlpha*(b-tapeR)
                val gain=1f+config.amount*3f
                val wobble=1f+sin(vinylPhase).toFloat()*config.amount*.012f
                a=(tanh(tapeL*gain)/tanh(gain))*wobble
                b=(tanh(tapeR*gain)/tanh(gain))*wobble
                vinylRandom=vinylRandom*1664525+1013904223
                val hiss=(((vinylRandom ushr 8) and 0xFFFF).toFloat()/32767.5f-1f)*.00045f*config.amount
                val crackle=if((vinylRandom and 0x3FFF)==0)if(vinylRandom<0) -.012f else .012f else 0f
                a+=hiss+crackle*config.amount
                b+=hiss+crackle*config.amount
                vinylPhase+=2*PI/(rate*(.9+config.amount*.5))
                if(vinylPhase>=2*PI)vinylPhase-=2*PI
            }
            FxMode.LO_FI -> {
                if(crushCounter==0) {
                    val bits=(14-(config.amount*10).roundToInt()).coerceIn(4,14)
                    val levels=((1 shl bits)-1).toFloat()
                    crushL=(a*levels).roundToInt().toFloat()/levels
                    crushR=(b*levels).roundToInt().toFloat()/levels
                    crushCounter=1+(config.amount*31).roundToInt()
                }
                crushCounter--
                a=crushL
                b=crushR
            }
            FxMode.ROOM_REVERB -> {
                var wetL=0f
                var wetR=0f
                val feedback=.58f+config.amount*.22f
                for(i in roomSizes.indices) {
                    val at=roomPositions[i]
                    val delayedL=roomL[i][at]
                    val delayedR=roomR[i][at]
                    wetL+=delayedL
                    wetR+=delayedR
                    roomL[i][at]=a*.35f+delayedL*feedback
                    roomR[i][at]=b*.35f+delayedR*feedback
                    roomPositions[i]=(at+1)%roomSizes[i]
                }
                val mix=config.amount*.78f
                a=a*(1-mix)+(wetL/roomSizes.size)*mix
                b=b*(1-mix)+(wetR/roomSizes.size)*mix
            }
            FxMode.BEAT_REPEAT -> {
                if(!repeatCaptured) {
                    repeatL[repeatPosition]=a
                    repeatR[repeatPosition]=b
                    repeatPosition++
                    if(repeatPosition>=repeatLength) {
                        repeatPosition=0
                        repeatCaptured=true
                    }
                } else {
                    if(repeatPosition>=repeatLength)repeatPosition=0
                    val mix=config.amount
                    a=a*(1-mix)+repeatL[repeatPosition]*mix
                    b=b*(1-mix)+repeatR[repeatPosition]*mix
                    repeatPosition++
                }
            }
            FxMode.DJFX_LOOPER -> {
                val speed=config.drive*2f-1f
                if(!djCaptured) {
                    djLoopL[djWrite]=a
                    djLoopR[djWrite]=b
                    djWrite++
                    if(djWrite>=djLength) {
                        djWrite=0
                        djCaptured=true
                        djRead=if(speed<0f)(djLength-1).toDouble() else 0.0
                    }
                } else {
                    val base=djRead.toInt().coerceIn(0,djLength-1)
                    val next=(base+1)%djLength
                    val fraction=(djRead-base).toFloat()
                    val loopL=djLoopL[base]+(djLoopL[next]-djLoopL[base])*fraction
                    val loopR=djLoopR[base]+(djLoopR[next]-djLoopR[base])*fraction
                    val mix=config.delay
                    a=a*(1-mix)+loopL*mix
                    b=b*(1-mix)+loopR*mix
                    djRead+=speed
                    if(djRead<0.0)djRead+=djLength
                    else if(djRead>=djLength)djRead-=djLength
                }
                delayMix=0f
            }
            FxMode.TAPE_ECHO -> {
                pingPong=true
                delayFeedback=.22f+config.amount*.68f
            }
            FxMode.ISOLATOR -> {
                isolatorLowL+=isolatorLowAlpha*(a-isolatorLowL)
                isolatorLowR+=isolatorLowAlpha*(b-isolatorLowR)
                isolatorMidL+=isolatorMidAlpha*(a-isolatorMidL)
                isolatorMidR+=isolatorMidAlpha*(b-isolatorMidR)
                val isolatedL=isolatorLowL+(a-isolatorMidL)
                val isolatedR=isolatorLowR+(b-isolatorMidR)
                a=a*(1-config.amount)+isolatedL*config.amount
                b=b*(1-config.amount)+isolatedR*config.amount
            }
            FxMode.SCATTER -> {
                scatterL[scatterPosition]=a
                scatterR[scatterPosition]=b
                scatterPosition=(scatterPosition+1)%scatterL.size
                scatterFilled=(scatterFilled+1).coerceAtMost(scatterL.size)
                if(scatterRemaining<=0) {
                    scatterRandom=scatterRandom*1664525+1013904223
                    val available=scatterFilled.coerceAtLeast(1)
                    val offset=((scatterRandom ushr 1)%available).coerceIn(0,available-1)
                    scatterRead=(scatterPosition-1-offset+scatterL.size)%scatterL.size
                    scatterRemaining=scatterLength
                }
                val offset=(scatterLength-scatterRemaining).coerceAtLeast(0)
                val at=(scatterRead+offset)%scatterL.size
                val mix=config.amount
                a=a*(1-mix)+scatterL[at]*mix
                b=b*(1-mix)+scatterR[at]*mix
                scatterRemaining--
            }
            FxMode.SLICER -> {
                val stepFrames=(beatFrames/16).coerceAtLeast(1)
                val step=(slicePosition/stepFrames)%8
                val gate=if(step<2 || step in 4..5)1f else .08f
                val gain=1-config.amount+gate*config.amount
                a*=gain
                b*=gain
                slicePosition=(slicePosition+1)%beatFrames
            }
            FxMode.CHORUS -> {
                val lfo=sin(chorusPhase)
                val modDepth=rate*.018*config.amount
                val delay=(rate*.008+((lfo+1)*.5*modDepth)).roundToInt().coerceIn(1,chorusL.lastIndex)
                val read=(chorusPosition-delay+chorusL.size)%chorusL.size
                val wetL=chorusL[read]
                val wetR=chorusR[read]
                chorusL[chorusPosition]=a
                chorusR[chorusPosition]=b
                chorusPosition=(chorusPosition+1)%chorusL.size
                val mix=.15f+config.amount*.3f
                a=a*(1-mix)+wetL*mix
                b=b*(1-mix)+wetR*mix
                chorusPhase+=2*PI*.25/rate
                if(chorusPhase>=2*PI)chorusPhase-=2*PI
            }
            FxMode.COMPRESSOR -> {
                val level=max(abs(a),abs(b))
                val smoothing=if(level>compressorEnvelope).02f else .0005f
                compressorEnvelope+=smoothing*(level-compressorEnvelope)
                val threshold=.42f
                val ratio=1f+config.amount*7f
                val compressed=if(compressorEnvelope>threshold) {
                    (threshold+(compressorEnvelope-threshold)/ratio)/compressorEnvelope.coerceAtLeast(.0001f)
                } else 1f
                val gain=1-config.amount+compressed*config.amount
                a*=gain
                b*=gain
            }
        }

        val read=(position-delayFrames+delayL.size)%delayL.size
        val delayedL=delayL[read]
        val delayedR=delayR[read]
        if(pingPong) {
            tapeL+=tapeAlpha*(delayedR-tapeL)
            tapeR+=tapeAlpha*(delayedL-tapeR)
            left=a+tapeL*delayMix
            right=b+tapeR*delayMix
            delayL[position]=a+delayedR*delayFeedback
            delayR[position]=b+delayedL*delayFeedback
        } else {
            left=a+delayedL*delayMix
            right=b+delayedR*delayMix
            delayL[position]=a+delayedL*.35f
            delayR[position]=b+delayedR*.35f
        }
        position=(position+1)%delayL.size
    }

    private fun resetPerformanceState() {
        sweepL=0f
        sweepR=0f
        tapeL=0f
        tapeR=0f
        vinylPhase=0.0
        vinylRandom=0x475B7A31
        crushL=0f
        crushR=0f
        crushCounter=0
        repeatL.fill(0f)
        repeatR.fill(0f)
        repeatPosition=0
        repeatCaptured=false
        djLoopL.fill(0f)
        djLoopR.fill(0f)
        djLength=rate/4
        djWrite=0
        djRead=0.0
        djCaptured=false
        scatterL.fill(0f)
        scatterR.fill(0f)
        scatterPosition=0
        scatterFilled=0
        scatterRead=0
        scatterRemaining=0
        scatterRandom=0x1648A239
        chorusL.fill(0f)
        chorusR.fill(0f)
        chorusPosition=0
        chorusPhase=0.0
        isolatorLowL=0f
        isolatorLowR=0f
        isolatorMidL=0f
        isolatorMidR=0f
        compressorEnvelope=0f
        slicePosition=0
        roomL.forEach{it.fill(0f)}
        roomR.forEach{it.fill(0f)}
        roomPositions.fill(0)
    }

    fun clear() {
        delayL.fill(0f)
        delayR.fill(0f)
        lastL=0f
        lastR=0f
        left=0f
        right=0f
        position=0
        resetPerformanceState()
    }
}
