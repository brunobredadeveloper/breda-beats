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
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{MaterialTheme(colorScheme=darkColorScheme(primary=Amber,secondary=Purple,background=Color(0xFF101115),surface=Panel)){val vm:StudioViewModel=viewModel(factory=viewModelFactory{initializer{StudioViewModel(LocalStudioRepository(applicationContext))}});Studio(vm,this)}}}}
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun Studio(vm:StudioViewModel,activity:ComponentActivity){
 val s by vm.state.collectAsStateWithLifecycle();val scope=rememberCoroutineScope();var volume by remember{mutableFloatStateOf(.7f)};var confirmChop by remember{mutableStateOf(false)};var splash by remember{mutableStateOf(true)}
 LaunchedEffect(Unit){delay(1200);splash=false}
 DisposableEffect(activity){val o=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_STOP)vm.stop()};activity.lifecycle.addObserver(o);onDispose{activity.lifecycle.removeObserver(o)}}
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->if(granted)vm.record()else vm.message("Permita o microfone nas configurações para gravar. Os pads continuam disponíveis.")}
 val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)scope.launch{try{val bytes=withContext(Dispatchers.IO){activity.contentResolver.openInputStream(uri)?.use{val a=it.readNBytes(4_000_001);require(a.size<=4_000_000){"Arquivo grande demais"};a}?:error("Não foi possível abrir")};vm.import(bytes)}catch(e:Exception){vm.message(e.message?:"Falha na importação")}}}
 val exporter=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/wav")){uri->if(uri!=null)scope.launch{try{val bytes=vm.export();withContext(Dispatchers.IO){activity.contentResolver.openOutputStream(uri)?.use{it.write(bytes)}?:error("Destino indisponível")};vm.message("WAV exportado — um compasso de 4 tempos") }catch(e:Exception){vm.message("Exportação falhou: ${e.message}")}}}
 val snack=remember{SnackbarHostState()};LaunchedEffect(s.message){s.message?.let{snack.showSnackbar(it);vm.dismiss()}}
 if(splash){Box(Modifier.fillMaxSize().background(Color(0xFF101115)),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Text("BD",color=Purple,fontSize=60.sp,fontWeight=FontWeight.Black);LinearProgressIndicator(Modifier.width(160.dp));Text("BREDA BEATS",color=Amber,fontSize=24.sp);Text("Do sample à sua próxima batida",color=Color.LightGray)}};return}
 Scaffold(snackbarHost={SnackbarHost(snack)},bottomBar={NavigationBar{listOf("PADS","SAMPLE","PATTERN","PROJETO").forEachIndexed{i,n->NavigationBarItem(selected=s.page==i,onClick={vm.page(i)},icon={Text(listOf("▦","∿","▥","BD")[i])},label={Text(n,fontSize=10.sp)})}}}){padding->
 Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text("breda_dev",color=Purple,fontSize=12.sp);Text("BEATS / 01",fontWeight=FontWeight.Black,fontSize=26.sp)};Column(horizontalAlignment=Alignment.End){Text("SAMPLING WORKSTATION",fontSize=9.sp,color=Color.Gray);Text("${s.pattern.bpm} BPM",color=Amber,fontFamily=FontFamily.Monospace)}}
 if(s.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
 Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF090C10)),border=BorderStroke(1.dp,Color(0xFF41444D))){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("A • ${s.pad+1}  ${Kit.names[s.pad]}",color=Amber,fontFamily=FontFamily.Monospace);Text(if(s.recording)"● REC" else if(s.playing)"▶ ${s.step+1}/16" else "READY",color=if(s.recording)Color.Red else Purple)}
 Wave(s.samples[s.pad],s.start,s.end)
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${"%.2f".format(s.samples[s.pad].pcm.size.toDouble()/s.samples[s.pad].rate)} s  /  PCM",fontSize=10.sp,color=Color.Gray);Text("${if(s.loop)"LOOP" else "ONE SHOT"}  ${if(s.gate)"GATE" else "TRIGGER"}",fontSize=10.sp,color=Amber)}
 }}
 when(s.page){0->{
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Control(if(s.playing)"STOP" else "PLAY",s.playing,Modifier.weight(1f)){vm.toggle()};Control(if(s.recording)"END REC" else "REC",s.recording,Modifier.weight(1f)){if(activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)vm.record() else permission.launch(Manifest.permission.RECORD_AUDIO)};Control("IMPORT",false,Modifier.weight(1f)){importer.launch(arrayOf("audio/wav","audio/x-wav"))}}
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Control("LOOP",s.loop,Modifier.weight(1f)){vm.loop()};Control("GATE",s.gate,Modifier.weight(1f)){vm.gate()};Control("STOP ALL",false,Modifier.weight(1f)){vm.stop()}}
 Text("BANK A  /  16 PADS",fontSize=11.sp,color=Purple,fontWeight=FontWeight.Bold)
 for(row in 0..3){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){for(col in 0..3){val pad=row*4+col;Pad(pad,s.pad==pad,Modifier.weight(1f),vm)}}}
 Text("Toque para disparar • segure em GATE • toque STOP ALL para encerrar loops",fontSize=11.sp,color=Color.Gray)
 Text("MASTER",fontSize=10.sp,color=Amber);Slider(volume,onValueChange={volume=it;vm.volume(it)})
 };1->{
 Text("SAMPLE EDIT",color=Amber,fontWeight=FontWeight.Bold);Text("Edite o pad ${s.pad+1}. Alterações são aplicadas ao confirmar.",color=Color.LightGray,fontSize=12.sp)
 var range by remember(s.pad,s.samples){mutableStateOf(s.start..s.end)}
 RangeSlider(value=range,onValueChange={range=it;vm.trim(it.start,it.endInclusive)},valueRange=0f..1f)
 Row{Text("START ${"%.0f".format(s.start*100)}%",Modifier.weight(1f),color=Purple);Text("END ${"%.0f".format(s.end*100)}%",color=Purple)}
 Control("REVERSE",s.reverse){vm.reverse()};Text("GAIN",color=Amber);Slider(s.gain,{vm.gain(it)},valueRange=0f..2f)
 Text("DRIVE / SATURATION",color=Amber);Slider(s.drive,{vm.drive(it)},valueRange=0f..1f)
 Button(onClick={vm.applyEdit()},enabled=!s.busy && s.end>s.start){Text("APLICAR E SALVAR")}
 OutlinedButton(onClick={confirmChop=true},enabled=!s.busy){Text("CHOP → 16 PADS")}
 Text("CHOP substitui todos os pads por 16 cortes iguais. WAV PCM 16 bits, mono/estéreo, até 10 segundos por pad.",fontSize=12.sp,color=Color.Gray)
 };2->{
 Text("PATTERN SEQUENCER",color=Amber,fontWeight=FontWeight.Bold)
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Control("− BPM",false,Modifier.weight(1f)){vm.tempo(s.pattern.bpm-1)};Control("+ BPM",false,Modifier.weight(1f)){vm.tempo(s.pattern.bpm+1)};Control(if(s.playing)"STOP" else "PLAY",s.playing,Modifier.weight(1f)){vm.toggle()}}
 Text("PAD ${s.pad+1} • ${Kit.names[s.pad]} • passos em semicolcheias",fontSize=12.sp,color=Purple)
 Row(Modifier.horizontalScroll(rememberScrollState())){for(p in 0..15){OutlinedButton(onClick={vm.select(p)},modifier=Modifier.padding(end=4.dp)){Text("${p+1}")}}}
 for(row in 0..3){Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){for(col in 0..3){val i=row*4+col;Control("${i+1}",s.pad in s.pattern.steps[i],Modifier.weight(1f).border(if(s.step==i)2.dp else 0.dp,Purple,RoundedCornerShape(8.dp))){vm.step(i)}}}}
 var swing by remember(s.pattern.swing){mutableFloatStateOf(s.pattern.swing)};Text("SWING ${"%.0f".format(swing*100)}%",color=Amber);Slider(swing,{swing=it},onValueChangeFinished={vm.swing(swing)},valueRange=0f..0.45f)
 Button(onClick={exporter.launch("breda-beats-${s.pattern.bpm}bpm.wav")},enabled=!s.busy){Text("EXPORTAR BATIDA WAV")}
 };3->{
 Text("SEU ESTÚDIO, SEU SOM",fontWeight=FontWeight.Black,fontSize=24.sp,color=Purple)
 Text("Um projeto local • Banco A • 16 pads • 1 compasso\nSalvamento automático de samples e padrão",color=Color.LightGray)
 Text("Kit original sintetizado pela breda_dev. Importe samples que você tenha direito de usar. Grave o microfone por até 10 segundos; ao sair do app a gravação é descartada.",fontSize=13.sp)
 Button(onClick={exporter.launch("breda-beats-${s.pattern.bpm}bpm.wav")},enabled=!s.busy){Text("EXPORTAR WAV")}
 OutlinedButton(onClick={importer.launch(arrayOf("audio/wav","audio/x-wav"))}){Text("IMPORTAR PARA PAD ${s.pad+1}")}
 Text("Inspirado no fluxo de samplers de mesa. Produto independente breda_dev. Use fones com fio para gravar e tocar; Bluetooth pode acrescentar atraso. Latência depende do aparelho. Não há login, nuvem, compras ou anúncios nesta versão.",fontSize=12.sp,color=Color.Gray)
 }}
 }}
 s.error?.let{AlertDialog(onDismissRequest={vm.dismiss()},title={Text("Estúdio indisponível")},text={Text(it)},confirmButton={TextButton(onClick={vm.dismiss()}){Text("FECHAR")}})}
 if(confirmChop)AlertDialog(onDismissRequest={confirmChop=false},title={Text("Substituir os 16 pads?")},text={Text("O sample atual será dividido em 16 cortes. Os samples anteriores serão substituídos e o padrão será preservado.")},confirmButton={TextButton(onClick={vm.chop();confirmChop=false}){Text("DIVIDIR")}},dismissButton={TextButton(onClick={confirmChop=false}){Text("CANCELAR")}})
}
@Composable private fun Control(text:String,on:Boolean,modifier:Modifier=Modifier,click:()->Unit){Button(onClick=click,modifier=modifier.defaultMinSize(minHeight=48.dp),shape=RoundedCornerShape(8.dp),colors=ButtonDefaults.buttonColors(containerColor=if(on)Amber else Panel,contentColor=if(on)Color.Black else Amber),contentPadding=PaddingValues(8.dp)){Text(text,fontSize=11.sp,fontWeight=FontWeight.Bold)}}
@Composable private fun Pad(pad:Int,selected:Boolean,modifier:Modifier,vm:StudioViewModel){var pressed by remember{mutableStateOf(false)};val scale by animateFloatAsState(if(pressed).94f else 1f,label="Pad touch")
 Box(modifier.aspectRatio(1f).scale(scale).background(if(pressed)Amber else Panel,RoundedCornerShape(12.dp)).border(if(selected)2.dp else 1.dp,if(selected)Purple else Color(0xFF484B55),RoundedCornerShape(12.dp)).pointerInput(pad){detectTapGestures(onPress={pressed=true;vm.hit(pad);try{tryAwaitRelease()}finally{pressed=false;vm.release(pad)}})}.padding(10.dp)){
 Text("${pad+1}",fontSize=26.sp,fontWeight=FontWeight.Black,color=if(pressed)Color.Black else Amber,modifier=Modifier.align(Alignment.TopStart));Text(Kit.names[pad],fontSize=8.sp,color=if(pressed)Color.Black else Color.LightGray,modifier=Modifier.align(Alignment.BottomStart))
 }}
@Composable private fun Wave(sample:Sample,start:Float,end:Float){Canvas(Modifier.fillMaxWidth().height(88.dp)){val bars=100;val chunk=maxOf(1,sample.pcm.size/bars);for(i in 0 until bars){val from=i*chunk;val to=minOf(sample.pcm.size,from+chunk);val peak=if(from<to)(from until to).maxOf{abs(sample.pcm[it])}else 0f;val x=i*size.width/bars;val color=if(i.toFloat()/bars in start..end)Amber else Color.DarkGray;drawLine(color,Offset(x,size.height/2-peak*size.height/2),Offset(x,size.height/2+peak*size.height/2),2f)};drawLine(Purple,Offset(start*size.width,0f),Offset(start*size.width,size.height),2f);drawLine(Purple,Offset(end*size.width,0f),Offset(end*size.width,size.height),2f)}}
