package dev.breda.beats.presentation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.breda.beats.core.*
import dev.breda.beats.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
data class StudioState(val samples:List<Sample> = List(16){Kit.create(it)},val pattern:Pattern=Pattern(),val pad:Int=0,val playing:Boolean=false,val recording:Boolean=false,val busy:Boolean=true,val error:String?=null,val message:String?=null,val step:Int=-1,val page:Int=0,val loop:Boolean=false,val gate:Boolean=false,val reverse:Boolean=false,val start:Float=0f,val end:Float=1f,val gain:Float=1f,val drive:Float=0f)
class StudioViewModel(private val repository:StudioRepository):ViewModel(){
 private val mutable=MutableStateFlow(StudioState());val state=mutable.asStateFlow();private var engine:AudioEngine?=null;private val microphone=Microphone();private var recordTimeout:Job?=null;private val mutex=kotlinx.coroutines.sync.Mutex()
 init {viewModelScope.launch {try{val pair=withContext(Dispatchers.IO){repository.load()};engine=withContext(Dispatchers.Default){PcmAudioEngine().also{it.samples(pair.first)}};mutable.value=mutable.value.copy(samples=pair.first,pattern=pair.second,busy=false)}catch(e:Exception){mutable.value=mutable.value.copy(busy=false,error="Não foi possível abrir o estúdio: ${e.message}. Seus dados foram preservados.")}}
  viewModelScope.launch{while(isActive){mutable.update{it.copy(step=engine?.step?:-1)};delay(50)}}
 }
 private fun update(f:(StudioState)->StudioState)=mutable.update(f)
 fun dismiss(){update{it.copy(error=null,message=null)}}
 fun select(pad:Int){update{it.copy(pad=pad,start=0f,end=1f,reverse=false,gain=1f,drive=0f)}}
 fun hit(pad:Int){if(state.value.busy || engine==null)return;select(pad);engine?.hit(pad,state.value.loop,state.value.gate)}
 fun release(pad:Int){engine?.release(pad)}
 fun page(page:Int){update{it.copy(page=page)}}
 fun loop(){update{it.copy(loop=!it.loop)}}
 fun gate(){update{it.copy(gate=!it.gate)}}
 fun reverse(){update{it.copy(reverse=!it.reverse)}}
 fun trim(start:Float,end:Float){update{it.copy(start=start,end=end)}}
 fun gain(gain:Float){update{it.copy(gain=gain)}}
 fun drive(drive:Float){update{it.copy(drive=drive)}}
 fun volume(v:Float){engine?.volume(v)}
 fun toggle(){val s=state.value;if(s.busy)return;engine?.transport(if(s.playing)null else s.pattern);update{it.copy(playing=!it.playing)}}
 fun stop(){engine?.stopAll();update{it.copy(playing=false)};if(state.value.recording){microphone.cancel();recordTimeout?.cancel();update{it.copy(recording=false,message="Gravação interrompida; sample anterior preservado")}}}
 private fun save(samples:List<Sample> = state.value.samples, pattern:Pattern=state.value.pattern){
  if(state.value.busy || engine==null)return
  update{it.copy(busy=true)};viewModelScope.launch{mutex.lock();try{withContext(Dispatchers.IO){repository.save(samples,pattern)};engine?.samples(samples);if(state.value.playing)engine?.transport(pattern);update{it.copy(samples=samples,pattern=pattern,busy=false,message="Projeto salvo no aparelho")}}catch(e:Exception){update{it.copy(busy=false,error="Falha ao salvar: ${e.message}")}}finally{mutex.unlock()}}
 }
 fun step(index:Int){save(pattern=state.value.pattern.toggle(index,state.value.pad))}
 fun tempo(bpm:Int){save(pattern=state.value.pattern.copy(bpm=bpm.coerceIn(40,200)))}
 fun swing(v:Float){save(pattern=state.value.pattern.copy(swing=v.coerceIn(0f,.45f)))}
 fun applyEdit(){val s=state.value;val sample=s.samples[s.pad].edit(s.start,s.end,s.reverse,s.gain,s.drive);save(s.samples.mapIndexed{i,v->if(i==s.pad)sample else v});update{it.copy(start=0f,end=1f,reverse=false,gain=1f,drive=0f)}}
 fun chop(){val s=state.value;val sample=s.samples[s.pad].edit(s.start,s.end,s.reverse,s.gain,s.drive);if(sample.pcm.size<16){update{it.copy(error="Sample curto demais")};return};val parts=List(16){i->Sample(sample.pcm.copyOfRange(i*sample.pcm.size/16,(i+1)*sample.pcm.size/16),sample.rate)};save(parts)}
 fun import(bytes:ByteArray){try{val sample=Wav.decode(bytes);val s=state.value;save(s.samples.mapIndexed{i,v->if(i==s.pad)sample else v})}catch(e:Exception){update{it.copy(error=e.message)}}}
 fun record(){if(state.value.recording){finishRecording();return};if(state.value.busy || engine==null)return;stop();try{microphone.start();update{it.copy(recording=true)};recordTimeout=viewModelScope.launch{delay(10000);finishRecording()}}catch(e:Exception){microphone.cancel();update{it.copy(error="Não foi possível gravar: ${e.message}")}}}
 private fun finishRecording(){recordTimeout?.cancel();viewModelScope.launch{try{val sample=withContext(Dispatchers.IO){microphone.finish()};val s=state.value;update{it.copy(recording=false)};save(s.samples.mapIndexed{i,v->if(i==s.pad)sample else v})}catch(e:Exception){update{it.copy(recording=false,error=e.message)}}}}
 suspend fun export():ByteArray=withContext(Dispatchers.Default){val s=state.value;Wav.encode(s.pattern.render(s.samples))}
 fun message(text:String){update{it.copy(message=text)}}
 override fun onCleared(){microphone.cancel();engine?.close()}
}
