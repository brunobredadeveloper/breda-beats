package dev.breda.beats.core

import kotlin.math.*

enum class FxMode(val label:String,val description:String) {
    STUDIO("STUDIO","Filtro, saturação e delay sincronizado nos controles abaixo."),
    FILTER_SWEEP("FILTER SWEEP","Varra o cutoff do filtro no master."),
    VINYL_COLOR("VINYL COLOR","Saturação suave, perda de agudos e leve oscilação."),
    LO_FI("LO-FI","Reduza a taxa de amostragem e a resolução de bits."),
    ROOM_REVERB("ROOM REVERB","Adicione uma ambiência estéreo curta."),
    BEAT_REPEAT("BEAT REPEAT","Capture uma semicolcheia e repita no BPM atual.")
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
    private var crushL=0f
    private var crushR=0f
    private var crushCounter=0
    private val repeatL=FloatArray(rate)
    private val repeatR=FloatArray(rate)
    private var repeatLength=max(1,rate/4)
    private var repeatPosition=0
    private var repeatCaptured=false
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
        val tapeCutoff=14000.0-settings.amount*9000
        tapeAlpha=(1-exp(-2*PI*tapeCutoff/rate)).toFloat()
        val safeBpm=bpm.coerceIn(40,200)
        delayFrames=(60.0/safeBpm/2*rate).roundToInt().coerceIn(1,delayL.size-1)
        repeatLength=(60.0/safeBpm/4*rate).roundToInt().coerceIn(1,repeatL.size)
    }

    fun process(l:Float,r:Float) {
        lastL+=alpha*(l-lastL)
        lastR+=alpha*(r-lastR)
        var a=lastL
        var b=lastR
        if(config.drive>0f) {
            val gain=1+config.drive*7
            a=tanh(a*gain)/tanh(gain)
            b=tanh(b*gain)/tanh(gain)
        }

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
                val wobble=1f+sin(vinylPhase).toFloat()*config.amount*.015f
                a=(tanh(tapeL*gain)/tanh(gain))*wobble
                b=(tanh(tapeR*gain)/tanh(gain))*wobble
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
        }

        val read=(position-delayFrames+delayL.size)%delayL.size
        left=a+delayL[read]*config.delay
        right=b+delayR[read]*config.delay
        delayL[position]=a+delayL[read]*.35f
        delayR[position]=b+delayR[read]*.35f
        position=(position+1)%delayL.size
    }

    private fun resetPerformanceState() {
        sweepL=0f
        sweepR=0f
        tapeL=0f
        tapeR=0f
        vinylPhase=0.0
        crushL=0f
        crushR=0f
        crushCounter=0
        repeatL.fill(0f)
        repeatR.fill(0f)
        repeatPosition=0
        repeatCaptured=false
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
