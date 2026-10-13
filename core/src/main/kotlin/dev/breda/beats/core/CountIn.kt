package dev.breda.beats.core

import kotlin.math.*

enum class CountInMode { THREE_SECONDS, ONE_BAR, NONE }
data class CountInTick(val number:Int,val waitMillis:Long,val accent:Boolean)
object CountIn {
    fun ticks(mode:CountInMode,bpm:Int):List<CountInTick> {
        require(bpm in 40..200)
        return when(mode){
            CountInMode.NONE->emptyList()
            CountInMode.THREE_SECONDS->List(3){i->CountInTick(3-i,1000,i==0)}
            CountInMode.ONE_BAR->List(4){i->
                val a=(i*60000.0/bpm).roundToLong()
                val b=((i+1)*60000.0/bpm).roundToLong()
                CountInTick(4-i,b-a,i==0)
            }
        }
    }
}
object ClickSound {
    fun pcm(accent:Boolean,rate:Int=44100):FloatArray {
        val frames=(rate*.025).roundToInt();val frequency=if(accent)1600.0 else 1100.0
        return FloatArray(frames){i->val time=i.toDouble()/rate;(sin(2*PI*frequency*time)*exp(-time*160)*min(1.0,time/.001)*.35).toFloat()}
    }
}
