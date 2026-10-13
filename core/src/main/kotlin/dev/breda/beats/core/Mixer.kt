package dev.breda.beats.core
import kotlin.math.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class Channel(val level:Float=1f,val pan:Float=0f,val mute:Boolean=false,val solo:Boolean=false) {
 init { require(level.isFinite() && level in 0f..1f);require(pan.isFinite() && pan in -1f..1f) }
 fun gains(anySolo:Boolean):Pair<Float,Float> {
  if(mute || (anySolo && !solo))return 0f to 0f
  val angle=(pan+1)*PI/4
  return (cos(angle)*level).toFloat() to (sin(angle)*level).toFloat()
 }
}
data class StudioProject(val samples:List<Sample> = List(16){Kit.create(it)}, val pattern:Pattern=Pattern().toggle(0,0).toggle(4,1).toggle(8,0).toggle(12,1), val channels:List<Channel> = List(16){Channel()},val title:String="Minha primeira beat",val master:Float=.7f,val names:List<String> = Kit.names) {
 init {require(samples.size==16 && channels.size==16);require(title.isNotBlank() && title.length<=80);require(master in 0f..1f);require(names.size==16 && names.all{it.isNotBlank() && it.length<=24})}
 fun channel(pad:Int,channel:Channel)=copy(channels=channels.mapIndexed{i,c->if(i==pad)channel else c})
 fun clearTrack(pad:Int)=copy(pattern=pattern.copy(steps=pattern.steps.map{it-pad}))
 fun resize(bars:Int):StudioProject {
  require(bars in 1..8)
  return copy(pattern=pattern.copy(steps=List(bars*16){i->pattern.steps.getOrNull(i)?:emptySet()}))
 }
}
class EditHistory(private val capacity:Int=20) {
 private val past=ArrayDeque<StudioProject>();private val future=ArrayDeque<StudioProject>()
 val canUndo get()=past.isNotEmpty();val canRedo get()=future.isNotEmpty()
 fun committed(previous:StudioProject){past.addLast(previous);while(past.size>capacity)past.removeFirst();future.clear()}
 fun undoTarget():StudioProject?=past.lastOrNull()
 fun redoTarget():StudioProject?=future.lastOrNull()
 fun didUndo(current:StudioProject){past.removeLast();future.addLast(current)}
 fun didRedo(current:StudioProject){future.removeLast();past.addLast(current)}
}
data class StereoMix(val left:FloatArray,val right:FloatArray,val rate:Int=44100) {
 init {require(left.size==right.size && left.isNotEmpty())}
 fun wav():ByteArray {
  val bytes=left.size*4;val b=ByteBuffer.allocate(44+bytes).order(ByteOrder.LITTLE_ENDIAN)
  b.put("RIFF".toByteArray());b.putInt(36+bytes);b.put("WAVEfmt ".toByteArray());b.putInt(16);b.putShort(1);b.putShort(2);b.putInt(rate);b.putInt(rate*4);b.putShort(4);b.putShort(16);b.put("data".toByteArray());b.putInt(bytes)
  for(i in left.indices){b.putShort((left[i].coerceIn(-1f,1f)*32767).roundToInt().toShort());b.putShort((right[i].coerceIn(-1f,1f)*32767).roundToInt().toShort())};return b.array()
 }
}
object MixRenderer {
 fun render(project:StudioProject,master:Float=project.master,onlyPad:Int?=null):StereoMix {
  require(master in 0f..1f);val rate=44100;val p=project.pattern;val size=(60.0/p.bpm*p.steps.size/4*rate).roundToInt();val left=FloatArray(size);val right=FloatArray(size);val solo=project.channels.any{it.solo};var time=0.0
  p.steps.forEachIndexed{step,pads->val offset=(time*rate).roundToInt();pads.forEach{pad->if(onlyPad==null || pad==onlyPad){val (l,r)=project.channels[pad].gains(solo);val s=project.samples[pad];val frames=(s.pcm.size.toDouble()/s.rate*rate).toInt();for(i in 0 until min(frames,size-offset)){val pos=i.toDouble()*s.rate/rate;val a=pos.toInt().coerceAtMost(s.pcm.lastIndex);val b=(a+1).coerceAtMost(s.pcm.lastIndex);val v=(s.pcm[a]+(s.pcm[b]-s.pcm[a])*(pos-a).toFloat())*master;left[offset+i]+=v*l;right[offset+i]+=v*r}}};time+=p.stepSeconds(step)}
  for(i in left.indices){left[i]=left[i].coerceIn(-1f,1f);right[i]=right[i].coerceIn(-1f,1f)}
  return StereoMix(left,right)
 }
}
