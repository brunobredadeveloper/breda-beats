package dev.breda.beats.core
import kotlin.math.*
data class FxSettings(val filter:Float=1f,val drive:Float=0f,val delay:Float=0f){
    init{require(listOf(filter,drive,delay).all{it.isFinite() && it in 0f..1f})}
}
class FxProcessor(private val rate:Int=44100){
    private val delayL=FloatArray(rate*2);private val delayR=FloatArray(rate*2);private var position=0
    private var lastL=0f;private var lastR=0f;private var config=FxSettings();private var alpha=1f;private var delayFrames=rate/4
    var left=0f;private set
    var right=0f;private set
    fun configure(settings:FxSettings,bpm:Int){
        config=settings;val cutoff=40.0+settings.filter*settings.filter*17960
        alpha=if(settings.filter>=.999f)1f else (1-exp(-2*PI*cutoff/rate)).toFloat()
        delayFrames=(60.0/bpm/2*rate).roundToInt().coerceIn(1,delayL.size-1)
    }
    fun process(l:Float,r:Float){
        lastL+=alpha*(l-lastL);lastR+=alpha*(r-lastR)
        var a=lastL;var b=lastR
        if(config.drive>0f){val gain=1+config.drive*7;a=tanh(a*gain)/tanh(gain);b=tanh(b*gain)/tanh(gain)}
        val read=(position-delayFrames+delayL.size)%delayL.size
        left=a+delayL[read]*config.delay;right=b+delayR[read]*config.delay
        delayL[position]=a+delayL[read]*.35f;delayR[position]=b+delayR[read]*.35f;position=(position+1)%delayL.size
    }
    fun clear(){delayL.fill(0f);delayR.fill(0f);lastL=0f;lastR=0f;left=0f;right=0f;position=0}
}
