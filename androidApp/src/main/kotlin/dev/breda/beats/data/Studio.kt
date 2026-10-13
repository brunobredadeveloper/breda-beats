package dev.breda.beats.data
import android.content.Context
import android.media.*
import dev.breda.beats.core.*
import org.json.*
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*
interface StudioRepository { fun load():StudioProject; fun save(project:StudioProject) }
class LocalStudioRepository(context:Context):StudioRepository {
    private val file=android.util.AtomicFile(File(context.filesDir,"studio.json"))
    private val audioDir=File(context.filesDir,"samples")
    private val identities=java.util.IdentityHashMap<Sample,String>()
    private fun hash(bytes:ByteArray)=java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
    override fun load():StudioProject {
        if(!file.baseFile.exists())return StudioProject()
        val j=JSONObject(String(file.readFully()));val version=j.getInt("schema")
        require(version in 1..4){"Versão de projeto não suportada"}
        if(version==4)return ProjectCodec.decode(j){name->require(ProjectCodec.validName(name));val f=File(audioDir,name);require(f.length() in 44..24_000_000);val bytes=f.readBytes();require(hash(bytes)+".wav"==name){"Sample corrompido"};Wav.decode(bytes).also{identities[it]=name}}
        val samples=j.getJSONArray(if(version==3)"sampleFiles" else "samples")
        val steps=j.getJSONArray("steps");require(samples.length()==16)
        val audio=List(16){i->
            if(version<3)Wav.decode(android.util.Base64.decode(samples.getString(i),android.util.Base64.NO_WRAP))
            else {
                val name=samples.getString(i);require(name.matches(Regex("[a-f0-9]{64}\\.wav"))){"Referência de sample inválida"}
                val f=File(audioDir,name);require(f.length() in 44..24_000_000){"Sample ausente ou corrompido"}
                val bytes=f.readBytes();require(hash(bytes)+".wav"==name){"Sample corrompido; arquivo preservado"}
                Wav.decode(bytes).also{identities[it]=name}
            }
        }
        val pattern=Pattern(j.getInt("bpm"),j.getDouble("swing").toFloat(),List(steps.length()){i->val a=steps.getJSONArray(i);(0 until a.length()).map{a.getInt(it)}.toSet()})
        if(version==1)return StudioProject(audio,pattern)
        val c=j.getJSONArray("channels");require(c.length()==16);val names=j.getJSONArray("names");require(names.length()==16)
        return StudioProject(audio,pattern,List(16){i->val v=c.getJSONObject(i);Channel(v.getDouble("level").toFloat(),v.getDouble("pan").toFloat(),v.getBoolean("mute"),v.getBoolean("solo"))},j.getString("title"),j.getDouble("master").toFloat(),List(16){names.getString(it)})
    }
    private fun saveSample(sample:Sample):String {
        identities[sample]?.let{return it}
        require(audioDir.isDirectory || audioDir.mkdirs()){"Armazenamento indisponível"}
        val bytes=Wav.encode(sample);val name=hash(bytes)+".wav";val f=File(audioDir,name)
        if(f.exists()){require(hash(f.readBytes())+".wav"==name){"Sample existente corrompido; arquivo preservado"}}
        else {val target=android.util.AtomicFile(f);val stream=target.startWrite();try{stream.write(bytes);target.finishWrite(stream)}catch(e:Exception){target.failWrite(stream);throw e}}
        identities[sample]=name;return name
    }
    override fun save(project:StudioProject) {
        val names=project.samples.map{saveSample(it)}
        val j=ProjectCodec.encode(project,names)
        val stream=file.startWrite();try{stream.write(j.toString().toByteArray());file.finishWrite(stream)}catch(e:Exception){file.failWrite(stream);throw e}
        if(identities.size>32){val iterator=identities.keys.iterator();while(iterator.hasNext()){val sample=iterator.next();if(project.samples.none{it===sample})iterator.remove()}}
    }
}
interface AudioEngine:AutoCloseable {
 fun samples(value:List<Sample>);fun mix(channels:List<Channel>);fun hit(pad:Int,loop:Boolean=false,gate:Boolean=false);fun release(pad:Int)
 fun transport(pattern:Pattern?);fun updatePattern(pattern:Pattern);fun stopAll();fun volume(v:Float)
 fun effects(settings:FxSettings,bpm:Int);fun song(patterns:List<Pattern>);val songIndex:Int
 fun click(accent:Boolean);fun metronome(enabled:Boolean,bpm:Int,standalone:Boolean=false)
 val step:Int;val quantizedStep:Int;val meters:List<Float>;val clipped:Boolean
}
class PcmAudioEngine(context:Context,private val interrupted:()->Unit):AudioEngine {
 private val manager=context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
 private var focused=false
 private val focus=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setOnAudioFocusChangeListener{change->if(change<0){stopAll();interrupted()}}.build()
 private fun acquire():Boolean {if(focused)return true;focused=manager.requestAudioFocus(focus)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;if(!focused)interrupted();return focused}
 private data class Voice(val pad:Int,val sample:Sample,var position:Double=0.0,val loop:Boolean=false,val gate:Boolean=false,val speed:Double=1.0,val reverse:Boolean=false)
 private val lock=Any();private var kit=List(16){Kit.create(it)};private val voices=mutableListOf<Voice>();private var pattern:Pattern?=null;private var nextFrame=0L;private var lastFrame=0L;private var frame=0L;private var index=0;@Volatile override var step=-1;private var master=.7f
 private var channels=List(16){Channel()};private var gains=channels.map{it.gains(false)};private val peaks=FloatArray(16)
 @Volatile override var clipped=false
 override val meters get()=synchronized(lock){peaks.toList()}
 override val quantizedStep get()=synchronized(lock){if(step<0)0 else if(frame-lastFrame >= (nextFrame-lastFrame)/2)index else step}
 private var metroEnabled=true;private var metroStandalone=false;private var metroBpm=88;private var nextClickFrame=0L;private val clickAccent=ClickSound.pcm(true);private val clickNormal=ClickSound.pcm(false);private var clickAudio=clickNormal;private var clickPosition=clickNormal.size
 private val fx=FxProcessor();private var songPatterns:List<Pattern> = emptyList();private var advanceSong=false;@Volatile override var songIndex=0
 private val alive=AtomicBoolean(true);private val track:AudioTrack;private val worker:Thread
 init {
  val min=AudioTrack.getMinBufferSize(44100,AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_16BIT);require(min>0)
  track=AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setAudioFormat(AudioFormat.Builder().setSampleRate(44100).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build()).setTransferMode(AudioTrack.MODE_STREAM).setBufferSizeInBytes(max(min,4096)).setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY).build()
  require(track.state==AudioTrack.STATE_INITIALIZED);track.play()
  worker=Thread({android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);val buffer=ShortArray(512)
   while(alive.get()) {
    synchronized(lock){peaks.fill(0f);clipped=false;for(i in 0 until buffer.size/2){pattern?.let{if(frame>=nextFrame){if(advanceSong && songPatterns.isNotEmpty()){songIndex=(songIndex+1)%songPatterns.size;pattern=songPatterns[songIndex];voices.clear();advanceSong=false};val p=pattern!!;lastFrame=frame;step=index;if(metroEnabled && index%4==0)triggerClick(index%16==0);p.steps[index].sorted().forEach{add(it,channels[it].loop,false)};nextFrame+=max(1,(p.stepSeconds(index)*44100).roundToInt());index=(index+1)%p.steps.size;if(index==0 && songPatterns.isNotEmpty())advanceSong=true}}
     if(pattern==null && metroEnabled && metroStandalone && frame>=nextClickFrame){triggerClick(false);nextClickFrame=frame+(60.0/metroBpm*44100).roundToLong()}
     var left=0.0;var right=0.0;val iterator=voices.iterator();while(iterator.hasNext()){val v=iterator.next();var at=v.position.toInt();if(at>=v.sample.pcm.size){if(v.loop){v.position%=v.sample.pcm.size;at=v.position.toInt()}else{iterator.remove();continue}}
      val next=(at+1).coerceAtMost(v.sample.pcm.lastIndex);val a=v.sample.pcm[if(v.reverse)v.sample.pcm.lastIndex-at else at];val b=v.sample.pcm[if(v.reverse)v.sample.pcm.lastIndex-next else next];val value=a+(b-a)*(v.position-at).toFloat();val gain=gains[v.pad];left+=value*gain.first;right+=value*gain.second;peaks[v.pad]=max(peaks[v.pad],abs(value)*max(gain.first,gain.second));v.position+=v.sample.rate/44100.0*v.speed}
     fx.process((left*master).toFloat(),(right*master).toFloat());left=fx.left.toDouble();right=fx.right.toDouble();if(clickPosition<clickAudio.size){val c=clickAudio[clickPosition++];left+=c;right+=c};if(abs(left)>1 || abs(right)>1)clipped=true
     buffer[i*2]=(left*32767).roundToInt().coerceIn(-32767,32767).toShort();buffer[i*2+1]=(right*32767).roundToInt().coerceIn(-32767,32767).toShort();frame++}}
    val written=track.write(buffer,0,buffer.size,AudioTrack.WRITE_BLOCKING);if(written<0)break
   }
  },"BredaPcm").also{it.start()}
 }
 private fun triggerClick(accent:Boolean){clickAudio=if(accent)clickAccent else clickNormal;clickPosition=0}
 override fun click(accent:Boolean){if(acquire())synchronized(lock){triggerClick(accent)}}
 override fun metronome(enabled:Boolean,bpm:Int,standalone:Boolean){if(standalone && enabled && !acquire())return;synchronized(lock){metroEnabled=enabled;metroBpm=bpm.coerceIn(40,200);metroStandalone=standalone;if(standalone)nextClickFrame=frame}}
 override fun effects(settings:FxSettings,bpm:Int)=synchronized(lock){fx.configure(settings,bpm)}
 override fun song(patterns:List<Pattern>){require(patterns.isNotEmpty());transport(patterns.first());synchronized(lock){songPatterns=patterns;songIndex=0;advanceSong=false}}
 private fun add(pad:Int,loop:Boolean,gate:Boolean){val c=channels[pad];if(c.choke>0)voices.removeAll{channels[it.pad].choke==c.choke};if(voices.size>=32)voices.removeAt(0);voices.removeAll{it.pad==pad && (it.loop || it.gate)};voices.add(Voice(pad,kit[pad],loop=loop,gate=gate,speed=c.speed,reverse=c.reverse))}
 override fun samples(value:List<Sample>)=synchronized(lock){require(value.size==16);kit=value}
 override fun mix(channels:List<Channel>)=synchronized(lock){require(channels.size==16);this.channels=channels;val solo=channels.any{it.solo};gains=channels.map{it.gains(solo)}}
 override fun hit(pad:Int,loop:Boolean,gate:Boolean){if(acquire())synchronized(lock){add(pad,loop,gate)}}
 override fun release(pad:Int)=synchronized(lock){voices.removeAll{it.pad==pad && it.gate};Unit}
 override fun transport(pattern:Pattern?){if(pattern!=null && !acquire())return;synchronized(lock){this.pattern=pattern;songPatterns=emptyList();songIndex=0;advanceSong=false;fx.clear();index=0;nextFrame=frame;lastFrame=frame;step=-1}}
 override fun updatePattern(pattern:Pattern)=synchronized(lock){if(this.pattern!=null){require(this.pattern!!.steps.size==pattern.steps.size);this.pattern=pattern}}
 override fun stopAll(){synchronized(lock){pattern=null;songPatterns=emptyList();songIndex=0;advanceSong=false;fx.clear();metroStandalone=false;clickPosition=clickAudio.size;voices.clear();peaks.fill(0f);step=-1};if(focused){focused=false;manager.abandonAudioFocusRequest(focus)}}
 override fun volume(v:Float)=synchronized(lock){master=v.coerceIn(0f,1f)}
 override fun close(){stopAll();alive.set(false);track.pause();track.flush();worker.join(1000);track.release()}
}
class Microphone {
 @Volatile private var running=false;private var worker:Thread?=null;private var recorder:AudioRecord?=null;private val values=ArrayList<Float>();@Volatile var error:String?=null
 @android.annotation.SuppressLint("MissingPermission") @Synchronized fun start(){check(!running);error=null;values.clear();val min=AudioRecord.getMinBufferSize(44100,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);require(min>0)
  val r=AudioRecord(MediaRecorder.AudioSource.MIC,44100,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,max(min,4096));require(r.state==AudioRecord.STATE_INITIALIZED){"Microfone indisponível"};recorder=r;r.startRecording();require(r.recordingState==AudioRecord.RECORDSTATE_RECORDING);running=true
  worker=Thread({val b=ShortArray(1024);while(running && values.size<2646000){val n=r.read(b,0,min(b.size,2646000-values.size));if(n<=0){if(running)error="Falha ao ler microfone";break};for(i in 0 until n)values.add(b[i]/32768f)};running=false},"BredaMic").also{it.start()}
 }
 @Synchronized fun finish():Sample {running=false;recorder?.let{runCatching{it.stop()}};worker?.join(1000);recorder?.release();recorder=null;worker=null;error?.let{throw IllegalStateException(it)};require(values.isNotEmpty()){"Nenhum áudio capturado"};return Sample(values.toFloatArray())}
 @Synchronized fun cancel(){runCatching{finish()};values.clear()}
}
