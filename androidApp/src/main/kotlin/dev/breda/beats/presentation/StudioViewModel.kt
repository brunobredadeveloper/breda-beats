package dev.breda.beats.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.breda.beats.core.*
import dev.breda.beats.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class StudioState(
    val appearance:StudioAppearance=StudioAppearance(),
    val project:StudioProject=StudioProject(), val pad:Int=0, val playing:Boolean=false,
    val recording:Boolean=false, val recordPads:Boolean=false, val micEnabled:Boolean=false,
    val pendingTake:Sample?=null, val takePad:Int=0, val busy:Boolean=true,
    val countInMode:CountInMode=CountInMode.THREE_SECONDS,val countdown:Int=0,val countdownMic:Boolean=false,val metronome:Boolean=true,
    val pendingProject:StudioProject?=null,val songPlaying:Boolean=false,val songPosition:Int=0,val padEditMode:Boolean=false,
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
    private val cpu:CoroutineDispatcher=Dispatchers.Default,
    private val appearanceRepository:AppearanceRepository?=null
):ViewModel(){
    private val mutable=MutableStateFlow(StudioState())
    val state=mutable.asStateFlow()
    private var engine:AudioEngine?=null
    private val microphone=Microphone()
    private var recordTimeout:Job?=null
    private val history=EditHistory()
    private var recordBase:StudioProject?=null
    private var songBase:StudioProject?=null
    private var micGeneration=0L
    private var finishingMic=false
    private var countInJob:Job?=null
    private val appearanceWrites=Mutex()
    init {
        viewModelScope.launch {
            try { val value=withContext(io){appearanceRepository?.load()?:StudioAppearance()};update{it.copy(appearance=value)} }
            catch(e:Exception) { message("Não foi possível ler suas cores: ${e.message}") }
        }
        viewModelScope.launch {
            try {
                val project=withContext(io){repository.load()}
                engine=withContext(cpu){createEngine {viewModelScope.launch {stop();message("Áudio interrompido por outro aplicativo")}}}
                applyAudio(project)
                mutable.value=mutable.value.copy(project=project,busy=false,loop=project.channels[0].loop,gate=project.channels[0].gate)
            } catch(e:Exception){update{it.copy(busy=false,error="Não foi possível abrir o estúdio: ${e.message}. Seus dados foram preservados.")}}
        }
        viewModelScope.launch {
            while(isActive){update{it.copy(step=engine?.step?:-1,meters=engine?.meters?:List(16){0f},clipped=engine?.clipped?:false,songPosition=engine?.songIndex?:0)};delay(50)}
        }
    }
    private fun update(f:(StudioState)->StudioState)=mutable.update(f)
    private fun applyAudio(p:StudioProject){engine?.samples(p.samples);engine?.mix(p.channels);engine?.volume(p.master);engine?.metronome(state.value.metronome,p.pattern.bpm);engine?.effects(p.fx,p.pattern.bpm)}
    fun dismiss(){update{it.copy(error=null,message=null)}}
    fun message(text:String){update{it.copy(message=text)}}
    fun select(pad:Int){update{it.copy(pad=pad,start=0f,end=1f,reverse=false,gain=1f,drive=0f,loop=it.project.channels[pad].loop,gate=it.project.channels[pad].gate)}}
    fun editMode(){update{it.copy(padEditMode=!it.padEditMode)}}
    fun hit(pad:Int){
        val s=state.value
        if(s.busy || engine==null || s.recording || s.countdown>0)return
        select(pad)
        if(s.recordPads){
            val index=(engine?.quantizedStep?:0).coerceIn(0,s.pattern.steps.lastIndex)
            val p=s.pattern.copy(steps=s.pattern.steps.mapIndexed{i,notes->if(i==index)notes+pad else notes})
            update{it.copy(project=it.project.copy(pattern=p))};engine?.updatePattern(p)
        }
        val c=s.project.channels[pad];engine?.hit(pad,c.loop,c.gate)
    }
    fun release(pad:Int){engine?.release(pad)}
    fun page(page:Int){update{it.copy(page=page)}}
    fun appearance(value:StudioAppearance) {
        val previous=state.value.appearance
        update{it.copy(appearance=value)}
        viewModelScope.launch {
            appearanceWrites.withLock {
                try { withContext(io){appearanceRepository?.save(value)} }
                catch(e:Exception) { update{it.copy(appearance=if(it.appearance==value)previous else it.appearance,message="Falha ao salvar as cores: ${e.message}")} }
            }
        }
    }
    fun loop(){val s=state.value;channel(s.pad,s.project.channels[s.pad].copy(loop=!s.loop))}
    fun gate(){val s=state.value;channel(s.pad,s.project.channels[s.pad].copy(gate=!s.gate))}
    fun reverse(){update{it.copy(reverse=!it.reverse)}}
    fun trim(start:Float,end:Float){update{it.copy(start=start,end=end)}}
    fun gain(gain:Float){update{it.copy(gain=gain)}}
    fun drive(drive:Float){update{it.copy(drive=drive)}}
    fun volume(v:Float){engine?.volume(v)}
    fun master(v:Float){val p=state.value.project.copy(master=v.coerceIn(0f,1f));if(state.value.recordPads || state.value.songPlaying){update{it.copy(project=p,loop=p.channels[it.pad].loop,gate=p.channels[it.pad].gate)};engine?.volume(p.master)}else commit(p)}
    private fun commit(next:StudioProject,kind:Int=0,previous:StudioProject=state.value.project,completed:()->Unit={}){
        if(state.value.busy || state.value.songPlaying || engine==null || state.value.recordPads || state.value.recording || state.value.countdown>0)return
        if(next==previous)return
        update{it.copy(busy=true)}
        viewModelScope.launch {
            try {
                withContext(io){repository.save(next)}
                when(kind){1->history.didUndo(previous);2->history.didRedo(previous);else->history.committed(previous)}
                applyAudio(next)
                completed()
                if(state.value.playing && !state.value.songPlaying){if(next.pattern.steps.size==previous.pattern.steps.size)engine?.updatePattern(next.pattern) else engine?.transport(next.pattern)}
                update{it.copy(project=next,busy=false,loop=next.channels[it.pad].loop,gate=next.channels[it.pad].gate,canUndo=history.canUndo,canRedo=history.canRedo,message=when(kind){1->"Última ação desfeita";2->"Ação refeita";else->"Projeto salvo"})}
            } catch(e:Exception){applyAudio(previous);update{it.copy(project=previous,busy=false,error="Falha ao salvar; estado anterior preservado: ${e.message}")}}
        }
    }
    fun undo(){if(!state.value.recordPads)history.undoTarget()?.let{commit(it,1)}}
    fun redo(){if(!state.value.recordPads)history.redoTarget()?.let{commit(it,2)}}
    fun channel(pad:Int,value:Channel){val p=state.value.project.channel(pad,value);if(state.value.recordPads || state.value.songPlaying){update{it.copy(project=p,loop=p.channels[it.pad].loop,gate=p.channels[it.pad].gate)};engine?.mix(p.channels)}else commit(p)}
    fun previewChannel(pad:Int,value:Channel){engine?.mix(state.value.project.channel(pad,value).channels)}
    fun name(pad:Int,text:String){val name=text.trim().take(24);if(name.isNotEmpty())commit(state.value.project.copy(names=state.value.project.names.mapIndexed{i,n->if(i==pad)name else n}))}
    fun title(text:String){val t=text.trim().take(80);if(t.isNotEmpty())commit(state.value.project.copy(title=t))}
    fun eraseTrack(){commit(state.value.project.clearTrack(state.value.pad))}
    fun bars(count:Int){commit(state.value.project.resize(count))}
    fun step(index:Int){commit(state.value.project.copy(pattern=state.value.pattern.toggle(index,state.value.pad)))}
    fun tempo(bpm:Int){commit(state.value.project.copy(pattern=state.value.pattern.copy(bpm=bpm.coerceIn(40,200))))}
    fun swing(v:Float){commit(state.value.project.copy(pattern=state.value.pattern.copy(swing=v.coerceIn(0f,.45f))))}
    fun toggle(){
        if(state.value.songPlaying){stop();return}
        if(state.value.recordPads){finishPads();return}
        val s=state.value;if(s.countdown>0){stop();return};if(s.busy || s.recording || engine==null)return
        engine?.transport(if(s.playing)null else s.pattern);update{it.copy(playing=!it.playing)}
    }
    fun metronome(){val enabled=!state.value.metronome;update{it.copy(metronome=enabled)};engine?.metronome(enabled,state.value.pattern.bpm,state.value.recording)}
    fun countIn(mode:CountInMode){if(state.value.countdown==0 && !state.value.recordPads && !state.value.recording)update{it.copy(countInMode=mode)}}
    private fun prepare(mic:Boolean,ready:()->Unit){
        val ticks=CountIn.ticks(state.value.countInMode,state.value.pattern.bpm)
        engine?.stopAll();update{it.copy(playing=false,songPlaying=false)}
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
        val s=state.value;if(s.busy || s.recording || s.songPlaying || engine==null || s.pendingTake!=null)return
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
        if(state.value.songPlaying){val before=songBase;songBase=null;val next=state.value.project;update{it.copy(songPlaying=false,playing=false)};if(before!=null)commit(next,previous=before)}
        countInJob?.cancel();countInJob=null;update{it.copy(countdown=0,countdownMic=false)}
        if(state.value.recordPads)finishPads()
        engine?.stopAll();update{it.copy(playing=false,songPlaying=false)}
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
        if(s.busy || s.recordPads || s.songPlaying || engine==null || s.pendingTake!=null || finishingMic)return
        val pad=s.pad
        prepare(true){
            if(state.value.micEnabled){
                try{
                    micGeneration++;microphone.start();engine?.metronome(state.value.metronome,state.value.pattern.bpm,true)
                    update{it.copy(recording=true,takePad=pad)}
                    recordTimeout=viewModelScope.launch{delay(60000);finishRecording()}
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
                val wav=withContext(cpu){MixRenderer.render(p,onlyPad=pad,applyFx=false).wav()}
                zip.putNextEntry(ZipEntry("track-${pad+1}.wav"));zip.write(wav);zip.closeEntry()
            }
            zip.putNextEntry(ZipEntry("session.txt"));zip.write("${p.title}\nBPM ${p.pattern.bpm}; ${p.pattern.steps.size/16} bars; stereo PCM16 44100 Hz. Stems use channel levels, pan, mute/solo and master. Sum at unity gain.\n".toByteArray());zip.closeEntry()
        }
    }
    private fun operation(action:()->StudioProject){try{commit(action())}catch(e:Exception){message(e.message?:"Operação inválida")}}
    fun copyPad(target:Int,swap:Boolean=false)=operation{state.value.project.copyPad(state.value.pad,target,swap)}
    fun deletePad()=operation{state.value.project.deletePad(state.value.pad)}
    fun normalize()=operation{val s=state.value;val sample=s.samples[s.pad];val peak=sample.pcm.maxOf{kotlin.math.abs(it)};val normalized=if(peak>0f)Sample(FloatArray(sample.pcm.size){sample.pcm[it]/peak*.95f},sample.rate) else sample;s.project.copy(samples=s.samples.mapIndexed{i,v->if(i==s.pad)normalized else v})}
    fun trimSilence()=operation{val s=state.value;val sample=s.samples[s.pad];val a=sample.pcm.indexOfFirst{kotlin.math.abs(it)>.003f};val b=sample.pcm.indexOfLast{kotlin.math.abs(it)>.003f};require(a>=0){"Sample sem áudio detectável"};val trimmed=Sample(sample.pcm.copyOfRange((a-32).coerceAtLeast(0),(b+33).coerceAtMost(sample.pcm.size)),sample.rate);s.project.copy(samples=s.samples.mapIndexed{i,v->if(i==s.pad)trimmed else v})}
    fun sequence(slot:Int){if(state.value.playing){message("Pare o transporte para trocar a sequência");return};operation{state.value.project.switchSequence(slot)}}
    fun duplicateSequence(target:Int)=operation{state.value.project.duplicateSequence(target)}
    fun appendSequence(slot:Int)=operation{val p=state.value.project;p.copy(arrangement=p.arrangement+slot)}
    fun removeLastSequence()=operation{val p=state.value.project;p.copy(arrangement=p.arrangement.dropLast(1).ifEmpty{listOf(p.slot)})}
    fun playSong(){val s=state.value;if(s.songPlaying){stop();return};if(s.busy || s.recording || s.recordPads || s.countdown>0)return;songBase=s.project;engine?.stopAll();engine?.song(s.project.arrangement.map{s.project.sequence(it)});update{it.copy(playing=true,songPlaying=true)}}
    fun effect(settings:FxSettings){val p=state.value.project.copy(fx=settings);if(state.value.songPlaying || state.value.recordPads){update{it.copy(project=p)};engine?.effects(settings,p.pattern.bpm)}else operation{p}}
    fun previewEffect(settings:FxSettings){engine?.effects(settings,state.value.pattern.bpm)}
    fun resample(){
        val s=state.value;if(s.busy || s.playing || s.recording || s.countdown>0)return
        update{it.copy(busy=true)}
        viewModelScope.launch{try{val mix=withContext(cpu){MixRenderer.render(s.project)};val audio=Sample(FloatArray(mix.left.size){(mix.left[it]+mix.right[it])*.5f});val next=s.project.copy(samples=s.samples.mapIndexed{i,v->if(i==s.pad)audio else v},channels=s.project.channels.mapIndexed{i,v->if(i==s.pad)Channel() else v},names=s.project.names.mapIndexed{i,n->if(i==s.pad)"RESAMPLE S${s.project.slot+1}" else n});update{it.copy(busy=false)};commit(next)}catch(e:Exception){update{it.copy(busy=false,error=e.message)}}}
    }
    suspend fun backup(output:java.io.OutputStream)=withContext(io){ProjectArchive.write(state.value.project,output)}
    suspend fun readBackup(input:java.io.InputStream){try{val p=withContext(io){ProjectArchive.read(input)};update{it.copy(pendingProject=p)}}catch(e:Exception){message("Backup inválido: ${e.message}")}}
    fun acceptProject(){val p=state.value.pendingProject?:return;stop();commit(p,completed={update{it.copy(pendingProject=null)}})}
    fun discardProject(){if(!state.value.busy)update{it.copy(pendingProject=null)}}
    suspend fun exportSong(output:java.io.OutputStream)=withContext(io){
        val p=state.value.project;val patterns=p.arrangement.map{p.sequence(it)};val frames=patterns.sumOf{kotlin.math.round(60.0/it.bpm*it.steps.size/4*44100).toInt()}
        val h=java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN);h.put("RIFF".toByteArray());h.putInt(36+frames*4);h.put("WAVEfmt ".toByteArray());h.putInt(16);h.putShort(1);h.putShort(2);h.putInt(44100);h.putInt(176400);h.putShort(4);h.putShort(16);h.put("data".toByteArray());h.putInt(frames*4);output.write(h.array())
        val fx=FxProcessor();for(pattern in patterns){val bytes=withContext(cpu){MixRenderer.render(p.copy(pattern=pattern,arrangement=listOf(p.slot)),processor=fx).wav()};output.write(bytes,44,bytes.size-44)}
    }
    override fun onCleared(){microphone.cancel();engine?.close()}
}

