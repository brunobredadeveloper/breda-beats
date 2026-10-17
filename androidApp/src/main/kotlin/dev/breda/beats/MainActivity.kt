package dev.breda.beats
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import dev.breda.beats.core.*
import dev.breda.beats.data.*
import dev.breda.beats.presentation.*
import kotlinx.coroutines.*
import kotlin.math.abs
private val Amber=Color(0xFFF6B44E);private val Purple=Color(0xFFB197FC);private val Panel=Color(0xFF22252C)
class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{MaterialTheme(colorScheme=darkColorScheme(primary=Amber,secondary=Purple,background=Color(0xFF101115),surface=Panel)){val vm:StudioViewModel=viewModel(factory=viewModelFactory{initializer{StudioViewModel(LocalStudioRepository(applicationContext),{callback->PcmAudioEngine(applicationContext,callback)})}});Studio(vm,this)}}}}
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun Studio(vm:StudioViewModel,activity:ComponentActivity){
 val s by vm.state.collectAsStateWithLifecycle();val scope=rememberCoroutineScope();var volume by remember{mutableFloatStateOf(.7f)};var confirmChop by remember{mutableStateOf(false)};var splash by remember{mutableStateOf(true)};var knob by remember{mutableIntStateOf(-1)};var knobValue by remember{mutableFloatStateOf(0f)};var bar by rememberSaveable{mutableIntStateOf(0)};var erase by remember{mutableStateOf(false)}
 LaunchedEffect(s.project.master){volume=s.project.master}
 LaunchedEffect(s.pattern.steps.size){bar=bar.coerceAtMost(s.pattern.steps.size/16-1)}
 LaunchedEffect(Unit){delay(1200);splash=false}
 DisposableEffect(activity){val o=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_STOP)vm.stop()};activity.lifecycle.addObserver(o);onDispose{activity.lifecycle.removeObserver(o)}}
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->if(granted)vm.record()else vm.message("Permita o microfone nas configurações para gravar. Os pads continuam disponíveis.")}
 val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)scope.launch{try{val bytes=withContext(Dispatchers.IO){activity.contentResolver.openInputStream(uri)?.use{val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);while(true){val n=it.read(buffer);if(n<0)break;require(output.size()+n<=24_000_000){"Arquivo grande demais"};output.write(buffer,0,n)};output.toByteArray()}?:error("Não foi possível abrir")};vm.import(bytes)}catch(e:Exception){vm.message(e.message?:"Falha na importação")}}}
 val exporter=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/wav")){uri->if(uri!=null)scope.launch{try{val bytes=vm.export();withContext(Dispatchers.IO){activity.contentResolver.openOutputStream(uri)?.use{it.write(bytes)}?:error("Destino indisponível")};vm.message("Mix estéreo exportado com ${s.pattern.steps.size/16} compassos") }catch(e:Exception){vm.message("Exportação falhou: ${e.message}")}}}
 val stemsExporter=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")){uri->if(uri!=null)scope.launch{try{withContext(Dispatchers.IO){activity.contentResolver.openOutputStream(uri)?.use{vm.exportStems(it)}?:error("Destino indisponível")};vm.message("16 pistas WAV exportadas em ZIP") }catch(e:Exception){vm.message("Exportação falhou: ${e.message}")}}}
 val backupExporter=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")){uri->if(uri!=null)scope.launch{try{withContext(Dispatchers.IO){activity.contentResolver.openOutputStream(uri)?.use{vm.backup(it)}?:error("Destino indisponível")};vm.message("Projeto editável salvo com samples e configurações") }catch(e:Exception){vm.message("Backup falhou: ${e.message}")}}}
 val backupImporter=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)scope.launch{try{withContext(Dispatchers.IO){activity.contentResolver.openInputStream(uri)?.use{vm.readBackup(it)}?:error("Backup indisponível")}}catch(e:Exception){vm.message(e.message?:"Falha no backup")}}}
 val songExporter=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/wav")){uri->if(uri!=null)scope.launch{try{withContext(Dispatchers.IO){activity.contentResolver.openOutputStream(uri)?.use{vm.exportSong(it)}?:error("Destino indisponível")};vm.message("Arranjo exportado em WAV estéreo") }catch(e:Exception){vm.message("Exportação falhou: ${e.message}")}}}
 val snack=remember{SnackbarHostState()};LaunchedEffect(s.message){s.message?.let{snack.showSnackbar(it);vm.dismiss()}}
 if(splash){Box(Modifier.fillMaxSize().background(Color(0xFF101115)),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Image(painterResource(R.drawable.ic_brand),contentDescription="breda_dev",modifier=Modifier.size(108.dp));LinearProgressIndicator(Modifier.width(160.dp));Text("BREDA BEATS",color=Amber,fontSize=24.sp);Text("Do sample à sua próxima batida",color=Color.LightGray)}};return}
 Scaffold(snackbarHost={SnackbarHost(snack)},topBar={FixedPerformanceDeck(s,vm)},bottomBar={NavigationBar{listOf("PADS","SAMPLE","PISTAS","MIXER","PROJETO").forEachIndexed{i,n->NavigationBarItem(selected=s.page==i,onClick={vm.page(i)},icon={Text(listOf("▦","∿","▥","≋","BD")[i])},label={Text(n,fontSize=10.sp)})}}}){padding->
 if(s.page==0)PerformanceScreen(s,vm,Modifier.padding(padding),onKnob={i->knob=i;knobValue=listOf(volume,(s.pattern.bpm-40)/160f,s.gain/2,s.drive)[i]},onImport={importer.launch(arrayOf("audio/wav","audio/x-wav"))},onMicRecord={if(activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)vm.record() else permission.launch(Manifest.permission.RECORD_AUDIO)})
 else Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){



 if(s.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
 if(s.page==1)Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF090C10)),border=BorderStroke(1.dp,Color(0xFF41444D))){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("A • ${s.pad+1}  ${s.project.names[s.pad]}",color=Amber,fontFamily=FontFamily.Monospace);Text(if(s.recording)"● REC" else if(s.playing)"▶ ${s.step+1}/${s.pattern.steps.size}" else "READY",color=if(s.recording)Color.Red else Purple)}
 Wave(s.samples[s.pad],s.start,s.end)
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${"%.2f".format(s.samples[s.pad].pcm.size.toDouble()/s.samples[s.pad].rate)} s  /  PCM",fontSize=10.sp,color=Color.Gray);Text("${if(s.loop)"LOOP" else "ONE SHOT"}  ${if(s.gate)"GATE" else "TRIGGER"}",fontSize=10.sp,color=Amber)}
 }}
 when(s.page){1->{
 Text("SAMPLE EDIT",color=Amber,fontWeight=FontWeight.Bold);Text("Edite o pad ${s.pad+1}. Alterações são aplicadas ao confirmar.",color=Color.LightGray,fontSize=12.sp)
 var range by remember(s.pad,s.samples){mutableStateOf(s.start..s.end)}
 RangeSlider(value=range,onValueChange={range=it;vm.trim(it.start,it.endInclusive)},valueRange=0f..1f)
 Row{Text("START ${"%.0f".format(s.start*100)}%",Modifier.weight(1f),color=Purple);Text("END ${"%.0f".format(s.end*100)}%",color=Purple)}
 Control("REVERSE",s.reverse){vm.reverse()};Text("GAIN",color=Amber);Slider(s.gain,{vm.gain(it)},valueRange=0f..2f)
 Text("DRIVE / SATURATION",color=Amber);Slider(s.drive,{vm.drive(it)},valueRange=0f..1f)
 Button(onClick={vm.applyEdit()},enabled=!s.busy && s.end>s.start){Text("APLICAR E SALVAR")}
 OutlinedButton(onClick={confirmChop=true},enabled=!s.busy){Text("CHOP → 16 PADS")}
 Text("CHOP substitui todos os pads por 16 cortes iguais. WAV PCM 16 bits, mono/estéreo, até 60 segundos por pad.",fontSize=12.sp,color=Color.Gray)
 };2->{
 CountInControls(s,vm)
 SequencePanel(s,vm){songExporter.launch("breda-beats-song.wav")}
 Text("GRAVAÇÃO MULTIPISTAS",color=Amber,fontWeight=FontWeight.Bold)
 Text("16 pistas • ${s.pattern.steps.size/16} compassos • overdub quantizado em 1/16",fontSize=12.sp,color=Purple)
 Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){Control(if(s.recordPads)"END TAKE" else "REC PADS",s.recordPads,Modifier.weight(1f)){vm.recordPads()};Control("ERASE PISTA",false,Modifier.weight(1f)){erase=true}}
 Row(verticalAlignment=Alignment.CenterVertically){OutlinedButton(onClick={vm.bars((s.pattern.steps.size/16-1).coerceAtLeast(1))},enabled=!s.busy && !s.recordPads){Text("−")};Text(" ${s.pattern.steps.size/16} compassos ",Modifier.weight(1f));OutlinedButton(onClick={vm.bars((s.pattern.steps.size/16+1).coerceAtMost(8))},enabled=!s.busy && !s.recordPads){Text("+")}}
 Row(Modifier.horizontalScroll(rememberScrollState())){for(b in 0 until s.pattern.steps.size/16){OutlinedButton(onClick={bar=b},modifier=Modifier.padding(end=4.dp)){Text("${b+1}",color=if(bar==b)Amber else Color.Gray)}}}
 TrackOverview(s,bar.coerceAtMost(s.pattern.steps.size/16-1),vm)
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Control("− BPM",false,Modifier.weight(1f)){vm.tempo(s.pattern.bpm-1)};Control("+ BPM",false,Modifier.weight(1f)){vm.tempo(s.pattern.bpm+1)};Control(if(s.playing)"STOP" else "PLAY",s.playing,Modifier.weight(1f)){vm.toggle()}}
 Text("PAD ${s.pad+1} • ${s.project.names[s.pad]} • passos em semicolcheias",fontSize=12.sp,color=Purple)
 Row(Modifier.horizontalScroll(rememberScrollState())){for(p in 0..15){OutlinedButton(onClick={vm.select(p)},modifier=Modifier.padding(end=4.dp)){Text("${p+1}")}}}
 for(row in 0..3){Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){for(col in 0..3){val i=bar.coerceAtMost(s.pattern.steps.size/16-1)*16+row*4+col;Control("${i%16+1}",s.pad in s.pattern.steps[i],Modifier.weight(1f).border(if(s.step==i)2.dp else 0.dp,Purple,RoundedCornerShape(8.dp))){vm.step(i)}}}}
 var swing by remember(s.pattern.swing){mutableFloatStateOf(s.pattern.swing)};Text("SWING ${"%.0f".format(swing*100)}%",color=Amber);Slider(swing,{swing=it},onValueChangeFinished={vm.swing(swing)},valueRange=0f..0.45f)
 Button(onClick={exporter.launch("breda-beats-${s.pattern.bpm}bpm.wav")},enabled=!s.busy){Text("EXPORTAR BATIDA WAV")}
 };3->{EffectsPanel(s,vm);MixerScreen(s,vm)};4->{
 Button(onClick={backupExporter.launch("breda-beats-project.zip")},enabled=!s.busy && !s.recording && !s.playing){Text("BACKUP PROJETO EDITÁVEL")}
 OutlinedButton(onClick={backupImporter.launch(arrayOf("application/zip"))},enabled=!s.busy && !s.recording && !s.playing){Text("ABRIR BACKUP DE PROJETO")}
 CountInControls(s,vm)
 MicPanel(s,vm){if(activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)vm.record() else permission.launch(Manifest.permission.RECORD_AUDIO)}
 Text("SEU ESTÚDIO, SEU SOM",fontWeight=FontWeight.Black,fontSize=24.sp,color=Purple)
 Text("${s.project.title}\n16 pistas • ${s.pattern.steps.size/16} compassos • mix estéreo\nSalvamento automático, UNDO/REDO e stems",color=Color.LightGray)
 var title by remember(s.project.title){mutableStateOf(s.project.title)};OutlinedTextField(title,{title=it.take(80)},label={Text("Nome do projeto")});OutlinedButton(onClick={vm.title(title)},enabled=!s.busy){Text("SALVAR NOME")}
 Text("Kit original sintetizado pela breda_dev. Importe samples que você tenha direito de usar. Grave o microfone por até 60 segundos; ao sair do app a gravação é descartada.",fontSize=13.sp)
 Button(onClick={exporter.launch("breda-beats-${s.pattern.bpm}bpm.wav")},enabled=!s.busy){Text("EXPORTAR WAV")}
 OutlinedButton(onClick={importer.launch(arrayOf("audio/wav","audio/x-wav"))}){Text("IMPORTAR PARA PAD ${s.pad+1}")}
 Button(onClick={stemsExporter.launch("breda-beats-${s.pattern.bpm}bpm-stems.zip")},enabled=!s.busy){Text("EXPORTAR 16 PISTAS WAV (ZIP)")}
 Text("Inspirado no fluxo de samplers de mesa. Produto independente breda_dev. Use fones com fio para gravar e tocar; Bluetooth pode acrescentar atraso. Latência depende do aparelho. Não há login, nuvem, compras ou anúncios nesta versão.",fontSize=12.sp,color=Color.Gray)
 }}
 }}
 if(knob>=0)AlertDialog(onDismissRequest={knob=-1},title={Text(listOf("VOLUME","BPM","SAMPLE GAIN","SAMPLE DRIVE")[knob])},text={Column{Slider(knobValue,{knobValue=it});Text(if(knob==1)"${40+(knobValue*160).toInt()} BPM" else "${(knobValue*100).toInt()}%")}},confirmButton={TextButton(onClick={when(knob){0->{volume=knobValue;vm.master(volume)};1->vm.tempo(40+(knobValue*160).toInt());2->{vm.gain(knobValue*2);vm.page(1)};3->{vm.drive(knobValue);vm.page(1)}};knob=-1}){Text("AJUSTAR")}})
 if(erase)AlertDialog(onDismissRequest={erase=false},title={Text("Apagar notas da pista ${s.pad+1}?")},text={Text("O sample permanece. UNDO restaura as notas desta pista em todos os compassos.")},confirmButton={TextButton(onClick={vm.eraseTrack();erase=false}){Text("ERASE")}},dismissButton={TextButton(onClick={erase=false}){Text("CANCELAR")}})
 s.pendingTake?.let{take->AlertDialog(onDismissRequest={vm.discardTake()},title={Text("Manter gravação no pad ${s.takePad+1}?")},text={Column{Wave(take,0f,1f);Text("${"%.2f".format(take.pcm.size.toDouble()/take.rate)} segundos. Nada será substituído sem sua confirmação.")}},confirmButton={TextButton(onClick={vm.acceptTake()},enabled=!s.busy){Text("MANTER")}},dismissButton={TextButton(onClick={vm.discardTake()}){Text("DESCARTAR")}})}
 s.pendingProject?.let{p->AlertDialog(onDismissRequest={vm.discardProject()},title={Text("Abrir ${p.title}?")},text={Text("Substitui o projeto atual. Faça backup antes. UNDO pode restaurar enquanto o histórico estiver disponível.")},confirmButton={TextButton(onClick={vm.acceptProject()},enabled=!s.busy){Text("ABRIR")}},dismissButton={TextButton(onClick={vm.discardProject()}){Text("CANCELAR")}})}
 s.error?.let{AlertDialog(onDismissRequest={vm.dismiss()},title={Text("Estúdio indisponível")},text={Text(it)},confirmButton={TextButton(onClick={vm.dismiss()}){Text("FECHAR")}})}
 if(confirmChop)AlertDialog(onDismissRequest={confirmChop=false},title={Text("Substituir os 16 pads?")},text={Text("O sample atual será dividido em 16 cortes. Os samples anteriores serão substituídos e o padrão será preservado.")},confirmButton={TextButton(onClick={vm.chop();confirmChop=false}){Text("DIVIDIR")}},dismissButton={TextButton(onClick={confirmChop=false}){Text("CANCELAR")}})
}

@Composable private fun FixedPerformanceDeck(s:StudioState,vm:StudioViewModel){
    val fx=s.project.fx
    val config=LocalConfiguration.current
    val landscape=config.screenWidthDp>config.screenHeightDp
    var master by remember(s.project.master){mutableFloatStateOf(s.project.master)}
    var ctrl1 by remember(fx.mode,fx.filter,fx.amount){mutableFloatStateOf(if(fx.mode==FxMode.STUDIO)fx.filter else fx.amount)}
    var ctrl2 by remember(fx.drive){mutableFloatStateOf(fx.drive)}
    var ctrl3 by remember(fx.delay){mutableFloatStateOf(fx.delay)}
    var fxMenu by remember{mutableStateOf(false)}
    val enabled=!s.busy && !s.recording && s.countdown==0
    val status=when{
        s.countdown>0->"PRE ${s.countdown}"
        s.recording->"REC MIC"
        s.recordPads->"REC PADS"
        s.songPlaying->"SONG ${s.songPosition+1}"
        s.playing->"PLAYING"
        else->"READY"
    }
    fun settings(c1:Float,c2:Float,c3:Float):FxSettings=
        if(fx.mode==FxMode.STUDIO)fx.copy(filter=c1,drive=c2,delay=c3)
        else fx.copy(amount=c1,drive=c2,delay=c3)
    val ctrl1Name=when(fx.mode){
        FxMode.STUDIO->"FILTER"
        FxMode.FILTER_SWEEP->"SWEEP"
        FxMode.VINYL_COLOR->"COLOR"
        FxMode.LO_FI->"LO-FI"
        FxMode.ROOM_REVERB->"ROOM"
        FxMode.BEAT_REPEAT->"REPEAT"
    }
    val ctrl1Value=when(fx.mode){
        FxMode.STUDIO->if(ctrl1>=.999f)"BYPASS" else "${(40+ctrl1*ctrl1*17960).toInt()} Hz"
        FxMode.FILTER_SWEEP->"${(80+ctrl1*ctrl1*15920).toInt()} Hz"
        else->"${(ctrl1*100).toInt()}%"
    }
    Surface(color=Color(0xFF17191F),shadowElevation=6.dp){
        Column(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=4.dp),verticalArrangement=Arrangement.spacedBy(2.dp)){
            Row(Modifier.fillMaxWidth().height(34.dp),verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){
                    Text("BREDA BEATS / 0.6",fontSize=12.sp,fontWeight=FontWeight.Black,color=Amber,maxLines=1,overflow=TextOverflow.Ellipsis)
                    Text("${s.pattern.bpm} BPM · $status",fontSize=8.sp,color=Color.LightGray,maxLines=1)
                }
                Box{
                    TextButton(onClick={fxMenu=true},contentPadding=PaddingValues(horizontal=5.dp,vertical=0.dp)){
                        Column(horizontalAlignment=Alignment.End){
                            Text("FX",fontSize=8.sp,color=Color.Gray)
                            Text(fx.mode.label,fontSize=9.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                        }
                    }
                    DropdownMenu(expanded=fxMenu,onDismissRequest={fxMenu=false}){
                        FxMode.entries.forEach{choice->
                            DropdownMenuItem(text={Text(choice.label)},onClick={
                                fxMenu=false
                                if(choice!=fx.mode)vm.effect(fx.copy(mode=choice))
                            })
                        }
                    }
                }
                IconButton(onClick={vm.undo()},enabled=s.canUndo && !s.busy && !s.recordPads && !s.recording && s.countdown==0,modifier=Modifier.size(34.dp).semantics{contentDescription="Desfazer"}){
                    Text("↶",fontSize=20.sp,color=if(s.canUndo)Amber else Color.Gray)
                }
                IconButton(onClick={vm.redo()},enabled=s.canRedo && !s.busy && !s.recordPads && !s.recording && s.countdown==0,modifier=Modifier.size(34.dp).semantics{contentDescription="Refazer"}){
                    Text("↷",fontSize=20.sp,color=if(s.canRedo)Amber else Color.Gray)
                }
            }
            if(landscape){
                Row(Modifier.fillMaxWidth().height(72.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Row(Modifier.weight(1f),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically){
                        PerformanceRotary("VOL","MASTER",master,"${(master*100).toInt()}%",enabled,Modifier.weight(1f),onValueChange={master=it;vm.volume(it)},onFinished={vm.master(master)})
                        PerformanceRotary("CTRL 1",ctrl1Name,ctrl1,ctrl1Value,enabled,Modifier.weight(1f),onValueChange={ctrl1=it;vm.previewEffect(settings(it,ctrl2,ctrl3))},onFinished={vm.effect(settings(ctrl1,ctrl2,ctrl3))})
                        PerformanceRotary("CTRL 2","DRIVE",ctrl2,"${(ctrl2*100).toInt()}%",enabled,Modifier.weight(1f),onValueChange={ctrl2=it;vm.previewEffect(settings(ctrl1,it,ctrl3))},onFinished={vm.effect(settings(ctrl1,ctrl2,ctrl3))})
                        PerformanceRotary("CTRL 3","DELAY",ctrl3,"${(ctrl3*100).toInt()}%",enabled,Modifier.weight(1f),onValueChange={ctrl3=it;vm.previewEffect(settings(ctrl1,ctrl2,it))},onFinished={vm.effect(settings(ctrl1,ctrl2,ctrl3))})
                    }
                    FixedTransport(s,vm,Modifier.widthIn(min=232.dp,max=286.dp))
                }
            }else{
                Row(Modifier.fillMaxWidth().height(70.dp),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically){
                    PerformanceRotary("VOL","MASTER",master,"${(master*100).toInt()}%",enabled,Modifier.weight(1f),onValueChange={master=it;vm.volume(it)},onFinished={vm.master(master)})
                    PerformanceRotary("CTRL 1",ctrl1Name,ctrl1,ctrl1Value,enabled,Modifier.weight(1f),onValueChange={ctrl1=it;vm.previewEffect(settings(it,ctrl2,ctrl3))},onFinished={vm.effect(settings(ctrl1,ctrl2,ctrl3))})
                    PerformanceRotary("CTRL 2","DRIVE",ctrl2,"${(ctrl2*100).toInt()}%",enabled,Modifier.weight(1f),onValueChange={ctrl2=it;vm.previewEffect(settings(ctrl1,it,ctrl3))},onFinished={vm.effect(settings(ctrl1,ctrl2,ctrl3))})
                    PerformanceRotary("CTRL 3","DELAY",ctrl3,"${(ctrl3*100).toInt()}%",enabled,Modifier.weight(1f),onValueChange={ctrl3=it;vm.previewEffect(settings(ctrl1,ctrl2,it))},onFinished={vm.effect(settings(ctrl1,ctrl2,ctrl3))})
                }
                FixedTransport(s,vm)
            }
        }
    }
}
@Composable private fun FixedTransport(s:StudioState,vm:StudioViewModel,modifier:Modifier=Modifier){
    val primary=when{
        s.recordPads->"REC ON"
        s.songPlaying->"STOP SONG"
        s.countdown>0->"CANCEL"
        s.playing->"PAUSE"
        else->"PLAY"
    }
    Row(modifier.fillMaxWidth().height(36.dp),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically){
        Button(onClick={vm.toggle()},enabled=!s.busy && !s.recording && !s.recordPads,modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=4.dp,vertical=0.dp)){
            Text(primary,fontSize=9.sp,fontWeight=FontWeight.Bold,maxLines=1)
        }
        Button(onClick={vm.recordPads()},enabled=!s.busy && !s.recording && (!s.songPlaying || s.recordPads) && (s.pendingTake==null || s.recordPads || s.countdown>0),modifier=Modifier.weight(1.2f),contentPadding=PaddingValues(horizontal=4.dp,vertical=0.dp),colors=ButtonDefaults.buttonColors(containerColor=if(s.recordPads)Color(0xFFB33A48) else Panel,contentColor=if(s.recordPads)Color.White else Amber)){
            Text(if(s.countdown>0)"CANCEL" else if(s.recordPads)"END REC" else "REC PADS",fontSize=9.sp,fontWeight=FontWeight.Bold,maxLines=1)
        }
        OutlinedButton(onClick={vm.stop()},modifier=Modifier.weight(.8f),contentPadding=PaddingValues(horizontal=4.dp,vertical=0.dp)){
            Text("STOP",fontSize=9.sp,fontWeight=FontWeight.Bold,maxLines=1)
        }
    }
}
@Composable private fun PerformanceRotary(title:String,parameter:String,value:Float,valueText:String,enabled:Boolean,modifier:Modifier=Modifier,onValueChange:(Float)->Unit,onFinished:()->Unit){
    val currentValue by rememberUpdatedState(value)
    val change by rememberUpdatedState(onValueChange)
    val finish by rememberUpdatedState(onFinished)
    Column(modifier.semantics{
        contentDescription="$title $parameter"
        stateDescription=valueText
        progressBarRangeInfo=ProgressBarRangeInfo(value,0f..1f)
        setProgress{target->if(enabled){change(target.coerceIn(0f,1f));finish();true}else false}
    },horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(0.dp)){
        Canvas(Modifier.size(40.dp).pointerInput(enabled){
            if(enabled)detectDragGestures(onDragEnd={finish()},onDragCancel={finish()}){changeEvent,dragAmount->
                changeEvent.consume()
                change((currentValue-dragAmount.y/120f).coerceIn(0f,1f))
            }
        }){
            val thickness=size.minDimension*.075f
            drawCircle(Color(0xFF14161C))
            drawArc(Color(0xFF3A3E47),135f,270f,false,style=Stroke(thickness))
            drawArc(if(enabled)Amber else Color.DarkGray,135f,270f*value,false,style=Stroke(thickness))
            drawCircle(Color(0xFF30343D),radius=size.minDimension*.31f)
            val angle=(-225.0+value*270.0)*kotlin.math.PI/180.0
            val center=androidx.compose.ui.geometry.Offset(size.width/2,size.height/2)
            val tip=androidx.compose.ui.geometry.Offset((kotlin.math.cos(angle)*size.width*.34).toFloat(),(kotlin.math.sin(angle)*size.height*.34).toFloat())
            drawLine(Color(0xFFF4F1E8),center,center+tip,strokeWidth=2.dp.toPx(),cap=androidx.compose.ui.graphics.StrokeCap.Round)
        }
        Text(title,fontSize=8.sp,fontWeight=FontWeight.Bold,color=if(enabled)Amber else Color.Gray,maxLines=1)
        Text(parameter,fontSize=7.sp,color=Color.LightGray,maxLines=1)
        Text(valueText,fontSize=7.sp,color=if(enabled)Color.White else Color.Gray,maxLines=1)
    }
}
@Composable private fun Control(text:String,on:Boolean,modifier:Modifier=Modifier,click:()->Unit){Button(onClick=click,modifier=modifier.defaultMinSize(minHeight=48.dp),shape=RoundedCornerShape(8.dp),colors=ButtonDefaults.buttonColors(containerColor=if(on)Amber else Panel,contentColor=if(on)Color.Black else Amber),contentPadding=PaddingValues(8.dp)){Text(text,fontSize=11.sp,fontWeight=FontWeight.Bold)}}
@Composable private fun Wave(sample:Sample,start:Float,end:Float){
 val peaks=remember(sample){val bars=100;val chunk=maxOf(1,sample.pcm.size/bars);FloatArray(bars){i->val from=i*chunk;val to=minOf(sample.pcm.size,from+chunk);if(from<to)(from until to).maxOf{abs(sample.pcm[it])}else 0f}}
 Canvas(Modifier.fillMaxWidth().height(88.dp)){for(i in peaks.indices){val peak=peaks[i];val x=i*size.width/peaks.size;val color=if(i.toFloat()/peaks.size in start..end)Amber else Color.DarkGray;drawLine(color,Offset(x,size.height/2-peak*size.height/2),Offset(x,size.height/2+peak*size.height/2),2f)};drawLine(Purple,Offset(start*size.width,0f),Offset(start*size.width,size.height),2f);drawLine(Purple,Offset(end*size.width,0f),Offset(end*size.width,size.height),2f)}
}

@Composable private fun Dial(name:String,value:Float,click:()->Unit){Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.clickable(onClick=click).padding(4.dp).semantics{contentDescription="Ajustar $name"}){Canvas(Modifier.size(56.dp)){drawCircle(Color(0xFF353942));drawCircle(Color(0xFF15171C),radius=size.minDimension*.38f);val angle=(-225+value*270)*kotlin.math.PI/180;val center=Offset(size.width/2,size.height/2);drawLine(Amber,center+Offset((kotlin.math.cos(angle)*size.width*.20).toFloat(),(kotlin.math.sin(angle)*size.height*.20).toFloat()),center+Offset((kotlin.math.cos(angle)*size.width*.37).toFloat(),(kotlin.math.sin(angle)*size.height*.37).toFloat()),3f)};Text(name,fontSize=9.sp,color=Amber)}}
