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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
 Scaffold(snackbarHost={SnackbarHost(snack)},bottomBar={NavigationBar{listOf("PADS","SAMPLE","PISTAS","MIXER","PROJETO").forEachIndexed{i,n->NavigationBarItem(selected=s.page==i,onClick={vm.page(i)},icon={Text(listOf("▦","∿","▥","≋","BD")[i])},label={Text(n,fontSize=10.sp)})}}}){padding->
 if(s.page==0)PerformanceScreen(s,vm,Modifier.padding(padding),onKnob={i->knob=i;knobValue=listOf(volume,(s.pattern.bpm-40)/160f,s.gain/2,s.drive)[i]},onImport={importer.launch(arrayOf("audio/wav","audio/x-wav"))},onMicRecord={if(activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)vm.record() else permission.launch(Manifest.permission.RECORD_AUDIO)})
 else Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text("breda_dev",color=Purple,fontSize=12.sp);Text("BEATS / 04",fontWeight=FontWeight.Black,fontSize=26.sp)};Column(horizontalAlignment=Alignment.End){Text("SAMPLING WORKSTATION",fontSize=9.sp,color=Color.Gray);Text("${s.pattern.bpm} BPM",color=Amber,fontFamily=FontFamily.Monospace)}}
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={vm.undo()},enabled=s.canUndo && !s.busy && !s.recordPads && !s.recording,modifier=Modifier.weight(1f)){Text("↶ UNDO",fontSize=11.sp)};OutlinedButton(onClick={vm.redo()},enabled=s.canRedo && !s.busy && !s.recordPads && !s.recording,modifier=Modifier.weight(1f)){Text("↷ REDO",fontSize=11.sp)}}
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={vm.toggle()},enabled=!s.busy && !s.recording,modifier=Modifier.weight(1f)){Text(if(s.playing)"PAUSE" else "PLAY")};OutlinedButton(onClick={vm.stop()},modifier=Modifier.weight(1f)){Text("STOP")}}
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
@Composable private fun Control(text:String,on:Boolean,modifier:Modifier=Modifier,click:()->Unit){Button(onClick=click,modifier=modifier.defaultMinSize(minHeight=48.dp),shape=RoundedCornerShape(8.dp),colors=ButtonDefaults.buttonColors(containerColor=if(on)Amber else Panel,contentColor=if(on)Color.Black else Amber),contentPadding=PaddingValues(8.dp)){Text(text,fontSize=11.sp,fontWeight=FontWeight.Bold)}}
@Composable private fun Wave(sample:Sample,start:Float,end:Float){Canvas(Modifier.fillMaxWidth().height(88.dp)){val bars=100;val chunk=maxOf(1,sample.pcm.size/bars);for(i in 0 until bars){val from=i*chunk;val to=minOf(sample.pcm.size,from+chunk);val peak=if(from<to)(from until to).maxOf{abs(sample.pcm[it])}else 0f;val x=i*size.width/bars;val color=if(i.toFloat()/bars in start..end)Amber else Color.DarkGray;drawLine(color,Offset(x,size.height/2-peak*size.height/2),Offset(x,size.height/2+peak*size.height/2),2f)};drawLine(Purple,Offset(start*size.width,0f),Offset(start*size.width,size.height),2f);drawLine(Purple,Offset(end*size.width,0f),Offset(end*size.width,size.height),2f)}}

@Composable private fun Dial(name:String,value:Float,click:()->Unit){Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.clickable(onClick=click).padding(4.dp).semantics{contentDescription="Ajustar $name"}){Canvas(Modifier.size(56.dp)){drawCircle(Color(0xFF353942));drawCircle(Color(0xFF15171C),radius=size.minDimension*.38f);val angle=(-225+value*270)*kotlin.math.PI/180;val center=Offset(size.width/2,size.height/2);drawLine(Amber,center+Offset((kotlin.math.cos(angle)*size.width*.20).toFloat(),(kotlin.math.sin(angle)*size.height*.20).toFloat()),center+Offset((kotlin.math.cos(angle)*size.width*.37).toFloat(),(kotlin.math.sin(angle)*size.height*.37).toFloat()),3f)};Text(name,fontSize=9.sp,color=Amber)}}
