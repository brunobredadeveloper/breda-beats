package dev.breda.beats.core
import kotlin.math.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class Channel(val level:Float=1f,val pan:Float=0f,val mute:Boolean=false,val solo:Boolean=false,
    val pitch:Int=0,val reverse:Boolean=false,val choke:Int=0,val loop:Boolean=false,val gate:Boolean=false) {
    init {require(level.isFinite() && level in 0f..1f);require(pan.isFinite() && pan in -1f..1f);require(pitch in -24..24 && choke in 0..4)}
    val speed get()=2.0.pow(pitch/12.0)
    fun gains(anySolo:Boolean):Pair<Float,Float>{
        if(mute || (anySolo && !solo))return 0f to 0f
        val angle=(pan+1)*PI/4
        return (cos(angle)*level).toFloat() to (sin(angle)*level).toFloat()
    }
}
data class StudioProject(
    val samples:List<Sample> = List(16){Kit.create(it)},val pattern:Pattern=Pattern().toggle(0,0).toggle(4,1).toggle(8,0).toggle(12,1),
    val channels:List<Channel> = List(16){Channel()},val title:String="Minha primeira beat",val master:Float=.7f,val names:List<String> = Kit.names,
    val fx:FxSettings=FxSettings(),val slots:List<Pattern?> = List(8){null},val slot:Int=0,val arrangement:List<Int> = listOf(0)
){
    init {
        require(samples.size==16 && channels.size==16);require(title.isNotBlank() && title.length<=80);require(master in 0f..1f)
        require(names.size==16 && names.all{it.isNotBlank() && it.length<=24});require(slots.size==8 && slot in 0..7)
        require(arrangement.size in 1..16 && arrangement.all{it in 0..7});require(samples.sumOf{it.pcm.size.toDouble()/it.rate}<=180.0){"Limite do projeto: 3 minutos de áudio nos pads"}
        require(arrangement.sumOf{sequence(it).steps.size/16}<=64){"Limite: 64 compassos no arranjo"}
    }
    fun sequence(index:Int):Pattern {require(index in 0..7);return (if(index==slot)pattern else slots[index]?:Pattern()).copy(bpm=pattern.bpm)}
    fun switchSequence(index:Int):StudioProject {require(index in 0..7);if(index==slot)return this;val saved=slots.mapIndexed{i,p->if(i==slot)pattern else p};return copy(pattern=sequence(index),slots=saved,slot=index)}
    fun duplicateSequence(target:Int):StudioProject {require(target in 0..7);return copy(slots=slots.mapIndexed{i,p->if(i==target)pattern else p})}
    fun channel(pad:Int,channel:Channel)=copy(channels=channels.mapIndexed{i,c->if(i==pad)channel else c})
    fun clearTrack(pad:Int)=copy(pattern=pattern.copy(steps=pattern.steps.map{it-pad}))
    fun resize(bars:Int):StudioProject {require(bars in 1..8);return copy(pattern=pattern.copy(steps=List(bars*16){i->pattern.steps.getOrNull(i)?:emptySet()}))}
    fun copyPad(from:Int,to:Int,swap:Boolean=false):StudioProject {
        require(from in 0..15 && to in 0..15)
        return copy(samples=samples.mapIndexed{i,s->if(i==to)samples[from] else if(swap && i==from)samples[to] else s},
            channels=channels.mapIndexed{i,c->if(i==to)channels[from] else if(swap && i==from)channels[to] else c},
            names=names.mapIndexed{i,n->if(i==to)names[from] else if(swap && i==from)names[to] else n})
    }
    fun deletePad(pad:Int)=copy(samples=samples.mapIndexed{i,s->if(i==pad)Sample(floatArrayOf(0f)) else s},channels=channels.mapIndexed{i,c->if(i==pad)Channel() else c},names=names.mapIndexed{i,n->if(i==pad)"VAZIO" else n})
}
class EditHistory(private val capacity:Int=20) {
 private val past=ArrayDeque<StudioProject>();private val future=ArrayDeque<StudioProject>()
 val canUndo get()=past.isNotEmpty();val canRedo get()=future.isNotEmpty()
 fun committed(previous:StudioProject){past.addLast(previous);future.clear();trim()}
 private fun trim(){
  fun frames():Long{val seen=java.util.IdentityHashMap<FloatArray,Boolean>();var total=0L;for(p in past)for(s in p.samples)if(seen.put(s.pcm,true)==null)total+=s.pcm.size;return total}
  while(past.isNotEmpty() && (past.size>capacity || frames()>12_000_000L))past.removeFirst()
 }
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
    fun render(project:StudioProject,master:Float=project.master,onlyPad:Int?=null,applyFx:Boolean=true,processor:FxProcessor?=null):StereoMix {
        require(master in 0f..1f);val rate=44100;val p=project.pattern;val size=(60.0/p.bpm*p.steps.size/4*rate).roundToInt()
        val left=FloatArray(size);val right=FloatArray(size);val solo=project.channels.any{it.solo};var time=0.0
        val events=mutableListOf<Pair<Int,Int>>()
        p.steps.forEachIndexed{step,pads->val offset=(time*rate).roundToInt();pads.sorted().forEach{events.add(offset to it)};time+=p.stepSeconds(step)}
        events.forEachIndexed{event,(offset,pad)->if(onlyPad==null || onlyPad==pad){
            val channel=project.channels[pad];val (l,r)=channel.gains(solo);val sample=project.samples[pad]
            val speed=sample.rate/44100.0*channel.speed;val natural=ceil(sample.pcm.size/speed).toInt()
            val cut=events.drop(event+1).firstOrNull{(_,other)->(other==pad && channel.loop) || (channel.choke!=0 && project.channels[other].choke==channel.choke)}?.first?:size
            val duration=min(if(channel.loop)size-offset else natural,min(size-offset,cut-offset)).coerceAtLeast(0)
            for(i in 0 until duration){
                val pos=if(channel.loop)(i*speed)%sample.pcm.size else i*speed;val a=pos.toInt().coerceAtMost(sample.pcm.lastIndex);val b=(a+1).coerceAtMost(sample.pcm.lastIndex)
                val va=sample.pcm[if(channel.reverse)sample.pcm.lastIndex-a else a];val vb=sample.pcm[if(channel.reverse)sample.pcm.lastIndex-b else b]
                val v=(va+(vb-va)*(pos-a).toFloat())*master;left[offset+i]+=v*l;right[offset+i]+=v*r
            }
        }}
        val fx=if(applyFx)(processor?:FxProcessor()).also{it.configure(project.fx,p.bpm)} else null
        for(i in left.indices){fx?.process(left[i],right[i]);left[i]=(fx?.left?:left[i]).coerceIn(-1f,1f);right[i]=(fx?.right?:right[i]).coerceIn(-1f,1f)}
        return StereoMix(left,right)
    }
}
