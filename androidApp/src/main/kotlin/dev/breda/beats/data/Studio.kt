package dev.breda.beats.data
import android.content.Context
import android.media.*
import dev.breda.beats.core.*
import org.json.*
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*
interface StudioRepository { fun load():Pair<List<Sample>,Pattern>; fun save(samples:List<Sample>,pattern:Pattern) }
class LocalStudioRepository(private val context:Context):StudioRepository {
 private val file=android.util.AtomicFile(File(context.filesDir,"studio.json"))
 override fun load():Pair<List<Sample>,Pattern> {
  if(!file.baseFile.exists())return List(16){Kit.create(it)} to Pattern().toggle(0,0).toggle(4,1).toggle(8,0).toggle(12,1)
  val j=JSONObject(String(file.readFully()));require(j.getInt("schema")==1)
  val samples=j.getJSONArray("samples");val steps=j.getJSONArray("steps")
  require(samples.length()==16 && steps.length()==16)
  return List(16){Wav.decode(android.util.Base64.decode(samples.getString(it),android.util.Base64.NO_WRAP))} to Pattern(j.getInt("bpm"),j.getDouble("swing").toFloat(),List(16){i->val a=steps.getJSONArray(i);(0 until a.length()).map{a.getInt(it)}.toSet()})
 }
 override fun save(samples:List<Sample>,pattern:Pattern) {
  val j=JSONObject().put("schema",1).put("bpm",pattern.bpm).put("swing",pattern.swing).put("samples",JSONArray(samples.map{android.util.Base64.encodeToString(Wav.encode(it),android.util.Base64.NO_WRAP)})).put("steps",JSONArray(pattern.steps.map{JSONArray(it.sorted())}))
  val stream=file.startWrite();try {stream.write(j.toString().toByteArray());file.finishWrite(stream)}catch(e:Exception){file.failWrite(stream);throw e}
 }
}
interface AudioEngine:AutoCloseable { fun samples(value:List<Sample>); fun hit(pad:Int,loop:Boolean=false,gate:Boolean=false); fun release(pad:Int); fun transport(pattern:Pattern?); fun stopAll(); fun volume(v:Float); val step:Int }
class PcmAudioEngine(context:Context,private val interrupted:()->Unit):AudioEngine {
 private val manager=context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
 private var focused=false
 private val focus=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setOnAudioFocusChangeListener{change->if(change<0){stopAll();interrupted()}}.build()
 private fun acquire():Boolean {if(focused)return true;focused=manager.requestAudioFocus(focus)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;if(!focused)interrupted();return focused}
 private data class Voice(val pad:Int,val sample:Sample,var position:Double=0.0,val loop:Boolean=false,val gate:Boolean=false)
 private val lock=Any();private var kit=List(16){Kit.create(it)};private val voices=mutableListOf<Voice>();private var pattern:Pattern?=null;private var nextFrame=0L;private var frame=0L;private var index=0;@Volatile override var step=-1;private var master=.7f
 private val alive=AtomicBoolean(true);private val track:AudioTrack
 private val worker:Thread
 init {
  val min=AudioTrack.getMinBufferSize(44100,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);require(min>0)
  track=AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setAudioFormat(AudioFormat.Builder().setSampleRate(44100).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build()).setTransferMode(AudioTrack.MODE_STREAM).setBufferSizeInBytes(max(min,2048)).setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY).build()
  require(track.state==AudioTrack.STATE_INITIALIZED);track.play()
  worker=Thread({android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);val buffer=ShortArray(256)
   while(alive.get()) {
    synchronized(lock){for(i in buffer.indices){pattern?.let{p->if(frame>=nextFrame){step=index;p.steps[index].forEach{add(it,false,false)};nextFrame+=max(1,(p.stepSeconds(index)*44100).roundToInt());index=(index+1)%16}}
     var value=0.0;val iterator=voices.iterator();while(iterator.hasNext()){val v=iterator.next();var at=v.position.toInt();if(at>=v.sample.pcm.size){if(v.loop){v.position%=v.sample.pcm.size;at=v.position.toInt()}else{iterator.remove();continue}};value+=v.sample.pcm[at];v.position+=v.sample.rate/44100.0}
     buffer[i]=(value*master*32767).roundToInt().coerceIn(-32767,32767).toShort();frame++}}
    val written=track.write(buffer,0,buffer.size,AudioTrack.WRITE_BLOCKING);if(written<0)break
   }
  },"BredaPcm").also{it.start()}
 }
 private fun add(pad:Int,loop:Boolean,gate:Boolean){if(voices.size>=32)voices.removeAt(0);voices.removeAll{it.pad==pad && (it.loop || it.gate)};voices.add(Voice(pad,kit[pad],loop=loop,gate=gate))}
 override fun samples(value:List<Sample>)=synchronized(lock){require(value.size==16);kit=value}
 override fun hit(pad:Int,loop:Boolean,gate:Boolean){if(acquire())synchronized(lock){add(pad,loop,gate)}}
 override fun release(pad:Int)=synchronized(lock){voices.removeAll{it.pad==pad && it.gate};Unit}
 override fun transport(pattern:Pattern?){if(pattern!=null && !acquire())return;synchronized(lock){this.pattern=pattern;index=0;nextFrame=frame;step=-1}}
 override fun stopAll(){synchronized(lock){pattern=null;voices.clear();step=-1};if(focused){focused=false;manager.abandonAudioFocusRequest(focus)}}
 override fun volume(v:Float)=synchronized(lock){master=v.coerceIn(0f,1f)}
 override fun close(){stopAll();alive.set(false);track.pause();track.flush();worker.join(1000);track.release()}
}
class Microphone {
 @Volatile private var running=false;private var worker:Thread?=null;private var recorder:AudioRecord?=null;private val values=ArrayList<Float>();@Volatile var error:String?=null
 @android.annotation.SuppressLint("MissingPermission") fun start(){check(!running);error=null;values.clear();val min=AudioRecord.getMinBufferSize(44100,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);require(min>0)
  val r=AudioRecord(MediaRecorder.AudioSource.MIC,44100,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,max(min,4096));require(r.state==AudioRecord.STATE_INITIALIZED){"Microfone indisponível"};recorder=r;r.startRecording();require(r.recordingState==AudioRecord.RECORDSTATE_RECORDING);running=true
  worker=Thread({val b=ShortArray(1024);while(running && values.size<441000){val n=r.read(b,0,min(b.size,441000-values.size));if(n<=0){if(running)error="Falha ao ler microfone";break};for(i in 0 until n)values.add(b[i]/32768f)};running=false},"BredaMic").also{it.start()}
 }
 fun finish():Sample {running=false;recorder?.let{runCatching{it.stop()}};worker?.join(1000);recorder?.release();recorder=null;worker=null;error?.let{throw IllegalStateException(it)};require(values.isNotEmpty()){"Nenhum áudio capturado"};return Sample(values.toFloatArray())}
 fun cancel(){runCatching{finish()};values.clear()}
}
