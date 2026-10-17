package dev.breda.beats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.breda.beats.core.*
import dev.breda.beats.presentation.*
import kotlin.math.*

private val Gold=Color(0xFFF6B44E)
private val Violet=Color(0xFFB197FC)
private val DarkPanel=Color(0xFF22252C)
private val PadColors=listOf(Color(0xFFF6B44E),Color(0xFFFF819D),Color(0xFF6EF5D0),Color(0xFFB197FC))

@OptIn(ExperimentalMaterial3Api::class,ExperimentalFoundationApi::class)
@Composable fun PerformanceScreen(s:StudioState,vm:StudioViewModel,modifier:Modifier=Modifier,onKnob:(Int)->Unit,onImport:()->Unit,onMicRecord:()->Unit){
    var settings by remember{mutableStateOf(false)};var editingPad by remember{mutableIntStateOf(-1)}
    fun edit(pad:Int){vm.select(pad);editingPad=pad}
    BoxWithConstraints(modifier.fillMaxSize().padding(8.dp)){
        val landscape=maxWidth>maxHeight*1.3f
        val compact=maxHeight<600.dp
        if(landscape){
            Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                Column(Modifier.widthIn(max=220.dp).weight(.37f).fillMaxHeight().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)){
                    Display(s,compact=true)
                    Utility(s,vm,{settings=true},{edit(s.pad)})
                }
                PadGrid(s,vm,Modifier.weight(.63f).fillMaxHeight(),::edit)
            }
        }else{
            Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(6.dp)){
                Display(s,compact)
                PadGrid(s,vm,Modifier.fillMaxWidth().weight(1f),::edit)
                Utility(s,vm,{settings=true},{edit(s.pad)})
            }
        }
    }
    if(editingPad>=0)PadPanel(editingPad,s,vm){editingPad=-1;vm.previewChannel(s.pad,s.project.channels[s.pad])}
    if(settings)ModalBottomSheet(onDismissRequest={settings=false}){
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Text("PREPARAÇÃO E GRAVAÇÃO",fontWeight=FontWeight.Bold,color=Violet)
            CountInControls(s,vm)
            Row(verticalAlignment=Alignment.CenterVertically){Text("Metrônomo no BPM",Modifier.weight(1f));Switch(s.metronome,{if(it!=s.metronome)vm.metronome()})}
            Text("O clique acompanha o BPM ao tocar/gravar. É excluído da mixagem e dos stems exportados. Use fones ao gravar microfone para evitar capturar o som dos alto-falantes.",fontSize=12.sp,color=Color.Gray)
            MicPanel(s,vm){settings=false;onMicRecord()}
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                OutlinedButton(onClick={settings=false;onImport()},enabled=!s.busy && s.countdown==0){Text("IMPORTAR WAV")}
                OutlinedButton(onClick={settings=false;vm.page(1)}){Text("EDITAR SAMPLE")}
            }
            OutlinedButton(onClick={settings=false;onKnob(0)},modifier=Modifier.fillMaxWidth()){Text("AJUSTAR MASTER")}
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable fun CountInControls(s:StudioState,vm:StudioViewModel){
    Text("CONTAGEM ANTES DE REC",fontSize=11.sp,color=Violet)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
        listOf(CountInMode.THREE_SECONDS to "3 s",CountInMode.ONE_BAR to "1 compasso",CountInMode.NONE to "Sem").forEach{(mode,label)->
            FilterChip(selected=s.countInMode==mode,onClick={vm.countIn(mode)},label={Text(label,fontSize=11.sp)},enabled=s.countdown==0 && !s.recordPads && !s.recording)
        }
    }
    Text(if(s.countInMode==CountInMode.ONE_BAR)"4 tempos: ${"%.2f".format(240.0/s.pattern.bpm)} segundos no BPM atual." else if(s.countInMode==CountInMode.THREE_SECONDS)"3 → 2 → 1. A gravação só começa após os 3 segundos." else "REC inicia imediatamente.",fontSize=11.sp,color=Color.Gray)
}

@Composable private fun Header(s:StudioState,vm:StudioViewModel,onKnob:(Int)->Unit){
    Row(Modifier.fillMaxWidth().height(40.dp),verticalAlignment=Alignment.CenterVertically){
        Column(Modifier.weight(1f)){Text("breda_dev",fontSize=8.sp,color=Violet);Text("BEATS / 04",fontSize=19.sp,fontWeight=FontWeight.Black)}
        TextButton(onClick={onKnob(1)},contentPadding=PaddingValues(4.dp)){Text("${s.pattern.bpm} BPM",color=Gold,fontSize=12.sp)}
        IconButton(onClick={vm.undo()},enabled=s.canUndo && !s.busy && !s.recordPads && !s.recording && s.countdown==0,modifier=Modifier.size(40.dp).semantics{contentDescription="Desfazer"}){Text("↶",fontSize=22.sp)}
        IconButton(onClick={vm.redo()},enabled=s.canRedo && !s.busy && !s.recordPads && !s.recording && s.countdown==0,modifier=Modifier.size(40.dp).semantics{contentDescription="Refazer"}){Text("↷",fontSize=22.sp)}
    }
}
@Composable private fun Display(s:StudioState,compact:Boolean){
    val status=when{ s.countdown>0->"PREPARAR ${s.countdown}${if(s.countInMode==CountInMode.THREE_SECONDS)" s" else ""}";s.recording->"● REC MIC";s.recordPads->"● REC PADS";s.playing->if(s.songPlaying)"SONG ${s.songPosition+1}" else "▶ ${s.step+1}";else->"READY" }
    Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF090C10)),border=BorderStroke(1.dp,Color(0xFF424651))){
        Column(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=6.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){Text("${s.pad+1} · ${s.project.names[s.pad]}",Modifier.weight(1f),color=Gold,fontSize=10.sp,maxLines=1,overflow=TextOverflow.Ellipsis);Text(status,color=if(s.countdown>0)Violet else Gold,fontSize=11.sp,fontWeight=FontWeight.Bold,modifier=Modifier.testTag("record-status"))}
            if(!compact)MiniWave(s.samples[s.pad],Modifier.fillMaxWidth().height(30.dp))
            Text("${if(s.micEnabled)"MIC ARMADO" else "MIC OFF"} · ${if(s.countInMode==CountInMode.THREE_SECONDS)"PRE 3 s" else if(s.countInMode==CountInMode.ONE_BAR)"PRE 1 BAR" else "PRE OFF"} · ${if(s.metronome)"CLICK ON" else "CLICK OFF"}",fontSize=8.sp,color=Color.Gray,maxLines=1)
            if(s.busy)LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
        }
    }
}
@Composable private fun Transport(s:StudioState,vm:StudioViewModel){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){
        SmallButton(if(s.playing)"PAUSE" else "PLAY",s.playing,Modifier.weight(1f)){vm.toggle()}
        SmallButton(if(s.countdown>0)"CANCEL" else if(s.recordPads)"END REC" else "REC PADS",s.recordPads || s.countdown>0,Modifier.weight(1.3f)){vm.recordPads()}
        SmallButton("STOP",false,Modifier.weight(1f)){vm.stop()}
    }
}
@Composable private fun Utility(s:StudioState,vm:StudioViewModel,settings:()->Unit,edit:()->Unit){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){
        SmallButton("LOOP",s.loop,Modifier.weight(1f)){vm.loop()}
        SmallButton("EDIT",s.padEditMode,Modifier.weight(1f)){vm.editMode()}
        SmallButton("METRO",s.metronome,Modifier.weight(1.1f)){vm.metronome()}
        SmallButton("AJUSTES",false,Modifier.weight(1.25f),settings)
    }
}
@Composable private fun SmallButton(text:String,on:Boolean,modifier:Modifier,click:()->Unit){
    Button(onClick=click,modifier=modifier.height(44.dp),shape=RoundedCornerShape(8.dp),contentPadding=PaddingValues(3.dp),colors=ButtonDefaults.buttonColors(containerColor=if(on)Gold else DarkPanel,contentColor=if(on)Color.Black else Gold)){Text(text,fontSize=9.sp,fontWeight=FontWeight.Bold,maxLines=1)}
}
@Composable private fun PadGrid(s:StudioState,vm:StudioViewModel,modifier:Modifier,edit:(Int)->Unit){
    Column(modifier.testTag("fixed-pad-grid"),verticalArrangement=Arrangement.spacedBy(6.dp)){
        for(row in 0..3)Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            for(col in 0..3){val pad=row*4+col;FixedPad(pad,s.project.names[pad],s.pad==pad,!s.busy && s.countdown==0 && !s.recording,Modifier.weight(1f).fillMaxHeight(),vm,s.padEditMode,s.project.channels[pad],s.recordPads,{edit(pad)})}
        }
    }
}
@Composable private fun FixedPad(pad:Int,name:String,selected:Boolean,enabled:Boolean,modifier:Modifier,vm:StudioViewModel,editMode:Boolean,channel:Channel,recording:Boolean,edit:()->Unit){
    val accent=PadColors[pad/4]
    var pressed by remember{mutableStateOf(false)};val scale by animateFloatAsState(if(pressed).95f else 1f,label="Pad press")
    BoxWithConstraints(modifier.scale(scale).testTag("pad-${pad+1}").background(if(pressed)accent else Color(0xFF191D25),RoundedCornerShape(10.dp)).border(if(selected)2.dp else 1.dp,if(selected)accent else accent.copy(alpha=.25f),RoundedCornerShape(10.dp)).semantics{contentDescription="Pad ${pad+1}, $name";role=Role.Button;onClick{if(enabled){if(editMode)edit() else{vm.hit(pad);vm.release(pad)}};enabled};onLongClick("Editar pad"){if(enabled && !recording)edit();enabled && !recording}}.pointerInput(pad,enabled,editMode,recording){detectTapGestures(onTap={if(enabled && editMode)edit()},onLongPress={if(enabled && !recording){pressed=false;vm.release(pad);edit()}},onPress={if(enabled && !editMode){pressed=true;vm.hit(pad);try{tryAwaitRelease()}finally{pressed=false;vm.release(pad)}}})}.padding(7.dp)){
        Text("${pad+1}",fontSize=if(maxHeight<48.dp)18.sp else 26.sp,fontWeight=FontWeight.Black,color=if(pressed)Color.Black else accent,modifier=Modifier.align(Alignment.TopStart),maxLines=1)
        if(channel.pitch!=0)Text("${if(channel.pitch>0)"+" else ""}${channel.pitch}",fontSize=9.sp,color=Violet,modifier=Modifier.align(Alignment.TopEnd))
        if(maxHeight>=48.dp)Text("$name · ${(channel.level*100).toInt()}%",fontSize=8.sp,color=if(pressed)Color.Black else Color.LightGray,modifier=Modifier.align(Alignment.BottomStart),maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}
@Composable private fun MiniWave(sample:Sample,modifier:Modifier){
    val peaks=remember(sample){val chunk=max(1,sample.pcm.size/80);FloatArray(80){i->val a=i*chunk;val b=min(sample.pcm.size,a+chunk);if(a<b)(a until b).maxOf{abs(sample.pcm[it])}else 0f}}
    Canvas(modifier){peaks.forEachIndexed{i,v->val x=i*size.width/peaks.size;drawLine(Gold,Offset(x,size.height/2-v*size.height/2),Offset(x,size.height/2+v*size.height/2),2f)}}
}
@Composable private fun SmallDial(name:String,value:Float,click:()->Unit){
    Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.clickable(onClick=click).semantics{contentDescription="Ajustar $name"}){
        Canvas(Modifier.size(40.dp)){drawCircle(Color(0xFF353942));drawCircle(Color(0xFF14161C),radius=size.minDimension*.38f);val angle=(-225+value*270)*PI/180;val center=Offset(size.width/2,size.height/2);drawLine(Gold,center,center+Offset((cos(angle)*size.width*.36).toFloat(),(sin(angle)*size.height*.36).toFloat()),3f)}
        Text(name,fontSize=8.sp,color=Gold)
    }
}
