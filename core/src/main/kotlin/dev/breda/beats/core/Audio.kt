package dev.breda.beats.core
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*
data class Sample(val pcm: FloatArray, val rate: Int = 44100) {
 init { require(rate in 8000..96000); require(pcm.isNotEmpty()); require(pcm.size <= rate * 120) }
 fun edit(start: Float, end: Float, reverse: Boolean, gain: Float, drive: Float): Sample {
  require(start >= 0f && end <= 1f && start < end){"Escolha um intervalo de corte válido"}; val a=(start*pcm.size).toInt().coerceAtMost(pcm.lastIndex); val b=(end*pcm.size).toInt().coerceAtLeast(a+1).coerceAtMost(pcm.size)
  val out=pcm.copyOfRange(a,b)
  if(reverse)out.reverse()
  val fade=min(128,out.size/2)
  for(i in out.indices) { var v=out[i]*gain; if(drive>0f)v=(tanh(v*(1+drive*8))/tanh(1+drive*8)); if(i<fade)v*=i.toFloat()/fade; if(i>=out.size-fade)v*=(out.size-1-i).toFloat()/fade;out[i]=v.coerceIn(-1f,1f) }
  return Sample(out,rate)
 }
}
object Wav {
 fun encode(s:Sample): ByteArray {
  val b=ByteBuffer.allocate(44+s.pcm.size*2).order(ByteOrder.LITTLE_ENDIAN)
  b.put("RIFF".toByteArray());b.putInt(36+s.pcm.size*2);b.put("WAVEfmt ".toByteArray());b.putInt(16);b.putShort(1);b.putShort(1);b.putInt(s.rate);b.putInt(s.rate*2);b.putShort(2);b.putShort(16);b.put("data".toByteArray());b.putInt(s.pcm.size*2)
  s.pcm.forEach{b.putShort((it.coerceIn(-1f,1f)*32767).toInt().toShort())};return b.array()
 }
 fun decode(bytes:ByteArray):Sample {
  require(bytes.size in 44..4_000_000){"WAV muito grande ou incompleto"}
  val b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
  fun tag(i:Int)=String(bytes,i,4,Charsets.US_ASCII)
  require(tag(0)=="RIFF" && tag(8)=="WAVE"){"Use WAV PCM de 16 bits"}
  var pos=12;var channels=0;var rate=0;var bits=0;var format=0;var data:FloatArray?=null
  while(pos+8<=bytes.size){val size=b.getInt(pos+4);require(size>=0 && size<=bytes.size-pos-8){"WAV inválido"};val at=pos+8
   when(tag(pos)){"fmt "->{require(size>=16);format=b.getShort(at).toInt();channels=b.getShort(at+2).toInt();rate=b.getInt(at+4);bits=b.getShort(at+14).toInt()}
    "data"->{require(format==1 && bits==16 && channels in 1..2 && rate in 8000..96000){"Use WAV PCM 16 bits mono ou estéreo"};val n=size/(2*channels);require(n>0 && n<=rate*10){"Limite: 10 segundos por sample"};data=FloatArray(n){i->(0 until channels).sumOf{c->b.getShort(at+(i*channels+c)*2).toDouble()/32768}.toFloat()/channels}}
   };pos=at+size+(size%2)
  };return Sample(requireNotNull(data){"Áudio não encontrado"},rate)
 }
}
data class Pattern(val bpm:Int=88, val swing:Float=0f, val steps:List<Set<Int>> = List(16){emptySet()}) {
 init { require(bpm in 40..200);require(swing in 0f..0.45f);require(steps.size in 16..128 && steps.size%16==0);require(steps.all{it.all{p->p in 0..15}}) }
 fun toggle(step:Int,pad:Int)=copy(steps=steps.mapIndexed{i,s->if(i==step)if(pad in s)s-pad else s+pad else s})
 fun stepSeconds(step:Int)=60.0/bpm/4*(if(step%2==0)1+swing else 1-swing)
 fun render(samples:List<Sample>):Sample {
  require(samples.size==16);val rate=44100;val length=(60.0/bpm*steps.size/4*rate).roundToInt();val out=FloatArray(length);var time=0.0
  steps.forEachIndexed { step,pads ->val offset=(time*rate).roundToInt();pads.forEach{p->val s=samples[p];val count=(s.pcm.size.toDouble()/s.rate*rate).toInt();for(i in 0 until min(count,length-offset)){val source=i.toDouble()*s.rate/rate;val a=source.toInt().coerceAtMost(s.pcm.lastIndex);val next=(a+1).coerceAtMost(s.pcm.lastIndex);out[offset+i]+=s.pcm[a]+(s.pcm[next]-s.pcm[a])*(source-a).toFloat()}};time+=stepSeconds(step)}
  val peak=out.maxOf{abs(it)};if(peak>.95f)for(i in out.indices)out[i]*=.95f/peak
  return Sample(out,rate)
 }
}
object Kit {
 val names=listOf("KICK","SNARE","HAT CLOSED","HAT OPEN","CLAP","RIM","SHAKER","PERC","BASS C","BASS F","BASS G","CHORD","VINYL","BELL","TOM","FX")
 fun create(pad:Int):Sample {
  val rate=44100;val duration=when(pad){3,8,9,10,11,13->.8;else->.3};var seed=pad+123
  fun noise():Double {seed=seed*1664525+1013904223;return ((seed ushr 8)/8388608.0)-1}
  return Sample(FloatArray((rate*duration).toInt()){i->val t=i.toDouble()/rate;val n=noise();val v=when(pad){0->sin(2*PI*(45*t+60*.02*(1-exp(-t/.02))))*exp(-t*16);1,4->(n*.8+sin(2*PI*170*t)*.2)*exp(-t*22);2,3,6->n*exp(-t*if(pad==3)8 else 55)*.35;5,7->sin(2*PI*480*t)*exp(-t*45);8,9,10->sin(2*PI*listOf(65.4,87.3,98.0)[pad-8]*t)*exp(-t*4)*.6;11->(sin(2*PI*130.8*t)+sin(2*PI*155.56*t)+sin(2*PI*196*t))*exp(-t*5)*.16;12->n*exp(-t*18)*.2;13->sin(2*PI*523.25*t)*exp(-t*7)*.4;14->sin(2*PI*(120*t+1-exp(-t*30)))*exp(-t*18)*.6;else->sin(2*PI*(800*t-300*t*t))*exp(-t*16)*.3};(v*min(1.0,t/.002)).toFloat()})
 }
}
