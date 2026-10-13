package dev.breda.beats.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.breda.beats.core.*
import dev.breda.beats.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class StudioState(
    val project:StudioProject=StudioProject(), val pad:Int=0, val playing:Boolean=false,
    val recording:Boolean=false, val recordPads:Boolean=false, val micEnabled:Boolean=false,
    val pendingTake:Sample?=null, val takePad:Int=0, val busy:Boolean=true,
    val countInMode:CountInMode=CountInMode.THREE_SECONDS,val countdown:Int=0,val countdownMic:Boolean=false,val metronome:Boolean=true,
    val error:String?=null, val message:String?=null, val step:Int=-1, val page:Int=0,
    val loop:Boolean=false, val gate:Boolean=false, val reverse:Boolean=false,
    val start:Float=0f, val end:Float=1f, val gain:Float=1f, val drive:Float=0f,
    val canUndo:Boolean=false,val canRedo:Boolean=false,val meters:List<Float> = List(16){0f},val clipped:Boolean=false
) {
    val samples get()=project.samples
    val pattern get()=project.pattern
}
class StudioViewModel(
    private val repository:StudioRepository,
    private val createEngine:((()->Unit)->AudioEngine),
    private val io:CoroutineDispatcher=Dispatchers.IO,
    private val cpu:CoroutineDispatcher=Dispatchers.Default
):ViewModel(){
    private val mutable=MutableStateFlow(StudioState())
    val state=mutable.asStateFlow()
    private var engine:AudioEngine?=null
    private val microphone=Microphone()
    private var recordTimeout:Job?=null
    private val history=EditHistory()
    private var recordBase:StudioProject?=null
    private var micGeneration=0L
    private var finishingMic=false
    private var countInJob:Job?=null
    init {
        viewModelScope.launch {
            try {
                val project=withContext(io){repository.load()}
                engine=withContext(cpu){createEngine {viewModelScope.launch {stop();message("Áudio interrompido por outro aplicativo")}}}
                applyAudio(project)
                mutable.value=mutable.value.copy(project=project,busy=false)
            } catch(e:Exception){update{it.copy(busy=false,error="Não foi possível abrir o estúdio: ${e.message}. Seus dados foram preservados.")}}
        }
        viewModelScope.launch {
            while(isActive){update{it.copy(step=engine?.step?:-1,meters=engine?.meters?:List(16){0f},clipped=engine?.clipped?:false)};delay(50)}
        }
    }
    private fun update(f:(StudioState)->StudioState)=mutable.update(f)
    private fun applyAudio(p:StudioProject){engine?.samples(p.samples);engine?.mix(p.channels);engine?.volume(p.master);engine?.metronome(state.value.metronome,p.pattern.bpm)}
    fun dismiss(){update{it.copy(error=null,message=null)}}
    fun message(text:String){update{it.copy(message=text)}}
    fun select(pad:Int){update{it.copy(pad=pad,start=0f,end=1f,reverse=false,gain=1f,drive=0f)}}
    fun hit(pad:Int){
        val s=state.value
        if(s.busy || engine==null || s.recording || s.countdown>0)return
        select(pad)
        if(s.recordPads){
            val index=(engine?.quantizedStep?:0).coerceIn(0,s.pattern.steps.lastIndex)
            val p=s.pattern.copy(steps=s.pattern.steps.mapIndexed{i,notes->if(i==index)notes+pad else notes})
            update{it.copy(project=it.project.copy(pattern=p))};engine?.updatePattern(p)
        }
        engine?.hit(pad,s.loop,s.gate)
    }
    fun release(pad:Int){engine?.release(pad)}
    fun page(page:Int){update{it.copy(page=page)}}
    fun loop(){update{it.copy(loop=!it.loop)}}
    fun gate(){update{it.copy(gate=!it.gate)}}
    fun reverse(){update{it.copy(reverse=!it.reverse)}}
    fun trim(start:Float,end:Float){update{it.copy(start=start,end=end)}}
    fun gain(gain:Float){update{it.copy(gain=gain)}}
    fun drive(drive:Float){update{it.copy(drive=drive)}}
    fun volume(v:Float){engine?.volume(v)}
    fun master(v:Float){val p=state.value.project.copy(master=v.coerceIn(0f,1f));if(state.value.recordPads){update{it.copy(project=p)};engine?.volume(p.master)}else commit(p)}
    private fun commit(next:StudioProject,kind:Int=0,previous:StudioProject=state.value.project,completed:()->Unit={}){
        if(state.value.busy || engine==null || state.value.recordPads || state.value.recording || state.value.countdown>0)return
        if(next==previous)return
        update{it.copy(busy=true)}
        viewModelScope.launch {
            try {
                withContext(io){repository.save(next)}
                when(kind){1->history.didUndo(previous);2->history.didRedo(previous);else->history.committed(previous)}
                applyAudio(next)
                completed()
                if(state.value.playing){if(next.pattern.steps.size==previous.pattern.steps.size)engine?.updatePattern(next.pattern) else engine?.transport(next.pattern)}
                update{it.copy(project=next,busy=false,canUndo=history.canUndo,canRedo=history.canRedo,message=when(kind){1->"Última ação desfeita";2->"Ação refeita";else->"Projeto salvo"})}
            } catch(e:Exception){applyAudio(previous);update{it.copy(project=previous,busy=false,error="Falha ao salvar; estado anterior preservado: ${e.message}")}}
        }
    }
    fun undo(){if(!state.value.recordPads)history.undoTarget()?.let{commit(it,1)}}
    fun redo(){if(!state.value.recordPads)history.redoTarget()?.let{commit(it,2)}}
    fun channel(pad:Int,value:Channel){val p=state.value.project.channel(pad,value);if(state.value.recordPads){update{it.copy(project=p)};engine?.mix(p.channels)}else commit(p)}
    fun previewChannel(pad:Int,value:Channel){engine?.mix(state.value.project.channel(pad,value).channels)}
    fun name(pad:Int,text:String){val name=text.trim().take(24);if(name.isNotEmpty())commit(state.value.project.copy(names=state.value.project.names.mapIndexed{i,n->if(i==pad)name else n}))}
    fun title(text:String){val t=text.trim().take(80);if(t.isNotEmpty())commit(state.value.project.copy(title=t))}
    fun eraseTrack(){commit(state.value.project.clearTrack(state.value.pad))}
    fun bars(count:Int){commit(state.value.project.resize(count))}
    fun step(index:Int){commit(state.value.project.copy(pattern=state.value.pattern.toggle(index,state.value.pad)))}
    fun tempo(bpm:Int){commit(state.value.project.copy(pattern=state.value.pattern.copy(bpm=bpm.coerceIn(40,200))))}
    fun swing(v:Float){commit(state.value.project.copy(pattern=state.value.pattern.copy(swing=v.coerceIn(0f,.45f))))}
    fun toggle(){
        if(state.value.recordPads){finishPads();return}
        val s=state.value;if(s.countdown>0){stop();return};if(s.busy || s.recording || engine==null)return
        engine?.transport(if(s.playing)null else s.pattern);update{it.copy(playing=!it.playing)}
    }
    fun metronome(){val enabled=!state.value.metronome;update{it.copy(metronome=enabled)};engine?.metronome(enabled,state.value.pattern.bpm,state.value.recording)}
    fun countIn(mode:CountInMode){if(state.value.countdown==0 && !state.value.recordPads && !state.value.recording)update{it.copy(countInMode=mode)}}
    private fun prepare(mic:Boolean,ready:()->Unit){
        val ticks=CountIn.ticks(state.value.countInMode,state.value.pattern.bpm)
        engine?.stopAll()
        if(ticks.isEmpty()){ready();return}
        update{it.copy(countdown=ticks.first().number,countdownMic=mic,playing=false)}
        countInJob=viewModelScope.launch{
            for(tick in ticks){update{it.copy(countdown=tick.number)};engine?.click(tick.accent);delay(tick.waitMillis)}
            update{it.copy(countdown=0,countdownMic=false)};countInJob=null;ready()
        }
    }
    fun recordPads(){
        if(state.value.countdown>0){stop();return}
        if(state.value.recordPads){finishPads();return}
        val s=state.value;if(s.busy || s.recording || engine==null || s.pendingTake!=null)return
        prepare(false){
            recordBase=state.value.project;engine?.stopAll();engine?.metronome(state.value.metronome,state.value.pattern.bpm);engine?.transport(state.value.pattern)
            update{it.copy(recordPads=true,playing=true,message="Overdub ligado: toque os pads. Microfone não é usado. STOP salva o take.")}
        }
    }
    private fun finishPads(){
        val before=recordBase?:return;recordBase=null
        val next=state.value.project;engine?.stopAll();update{it.copy(recordPads=false,playing=false)}
        commit(next,previous=before)
    }
    fun stop(){
        countInJob?.cancel();countInJob=null;update{it.copy(countdown=0,countdownMic=false)}
        if(state.value.recordPads)finishPads()
        engine?.stopAll();update{it.copy(playing=false)}
        if(state.value.recording){micGeneration++;microphone.cancel();recordTimeout?.cancel();update{it.copy(recording=false,message="Gravação descartada; sample anterior preservado")}}
    }
    fun enableMic(){
        val enabled=!state.value.micEnabled
        if(!enabled){micGeneration++;if(state.value.countdownMic)stop();if(state.value.recording){microphone.cancel();recordTimeout?.cancel();engine?.stopAll()}}
        update{it.copy(micEnabled=enabled,recording=if(enabled)it.recording else false,message=if(enabled)"Microfone habilitado. Só captura ao tocar REC SAMPLE." else "Microfone desligado. Nenhuma captura em andamento.")}
    }
    fun applyEdit(){
        val s=state.value
        try{val sample=s.samples[s.pad].edit(s.start,s.end,s.reverse,s.gain,s.drive);commit(s.project.copy(samples=s.samples.mapIndexed{i,v->if(i==s.pad)sample else v}));update{it.copy(start=0f,end=1f,reverse=false,gain=1f,drive=0f)}}catch(e:Exception){message(e.message?:"Corte inválido")}
    }
    fun chop(){
        val s=state.value
        try{val sample=s.samples[s.pad].edit(s.start,s.end,s.reverse,s.gain,s.drive);require(sample.pcm.size>=16){"Sample curto demais"};val parts=List(16){i->Sample(sample.pcm.copyOfRange(i*sample.pcm.size/16,(i+1)*sample.pcm.size/16),sample.rate)};commit(s.project.copy(samples=parts))}catch(e:Exception){message(e.message?:"Corte inválido")}
    }
    fun import(bytes:ByteArray){
        if(state.value.busy || state.value.recordPads || state.value.recording || state.value.countdown>0)return
        val pad=state.value.pad
        viewModelScope.launch{try{val sample=withContext(cpu){Wav.decode(bytes)};val s=state.value;commit(s.project.copy(samples=s.samples.mapIndexed{i,v->if(i==pad)sample else v}))}catch(e:Exception){update{it.copy(error=e.message)}}}
    }
    fun record(){
        if(state.value.countdown>0){stop();return}
        if(state.value.recording){finishRecording();return}
        val s=state.value
        if(!s.micEnabled){message("Microfone desligado. Habilite MIC antes de gravar um sample.");return}
        if(s.busy || s.recordPads || engine==null || s.pendingTake!=null || finishingMic)return
        val pad=s.pad
        prepare(true){
            if(state.value.micEnabled){
                try{
                    micGeneration++;microphone.start();engine?.metronome(state.value.metronome,state.value.pattern.bpm,true)
                    update{it.copy(recording=true,takePad=pad)}
                    recordTimeout=viewModelScope.launch{delay(10000);finishRecording()}
                }catch(e:Exception){microphone.cancel();engine?.stopAll();update{it.copy(error="Não foi possível gravar: ${e.message}")}}
            }
        }
    }
    private fun finishRecording(){
        if(finishingMic || !state.value.recording)return
        finishingMic=true;recordTimeout?.cancel();val generation=micGeneration
        viewModelScope.launch{
            try{
                val sample=withContext(io){microphone.finish()};engine?.metronome(state.value.metronome,state.value.pattern.bpm,false)
                if(generation==micGeneration && state.value.micEnabled)update{it.copy(recording=false,pendingTake=sample)}
            }catch(e:Exception){if(generation==micGeneration)update{it.copy(recording=false,error=e.message)}}
            finally{finishingMic=false}
        }
    }
    fun acceptTake(){val s=state.value;val take=s.pendingTake?:return;commit(s.project.copy(samples=s.samples.mapIndexed{i,v->if(i==s.takePad)take else v}),completed={update{it.copy(pendingTake=null)}})}
    fun discardTake(){if(state.value.busy)return;update{it.copy(pendingTake=null,message="Take descartado; sample anterior preservado")}}
    suspend fun export():ByteArray=withContext(cpu){MixRenderer.render(state.value.project).wav()}
    suspend fun exportStems(output:java.io.OutputStream)=withContext(io){
        val p=state.value.project
        ZipOutputStream(output).use{zip->
            for(pad in 0..15){
                val wav=withContext(cpu){MixRenderer.render(p,onlyPad=pad).wav()}
                zip.putNextEntry(ZipEntry("track-${pad+1}.wav"));zip.write(wav);zip.closeEntry()
            }
            zip.putNextEntry(ZipEntry("session.txt"));zip.write("${p.title}\nBPM ${p.pattern.bpm}; ${p.pattern.steps.size/16} bars; stereo PCM16 44100 Hz. Stems use channel levels, pan, mute/solo and master. Sum at unity gain.\n".toByteArray());zip.closeEntry()
        }
    }
    override fun onCleared(){microphone.cancel();engine?.close()}
}
