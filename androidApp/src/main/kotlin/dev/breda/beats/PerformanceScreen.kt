package dev.breda.beats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
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

private val PadColors=listOf(Color(0xFFF6B44E),Color(0xFFFF819D),Color(0xFF6EF5D0),Color(0xFFB197FC))

@OptIn(ExperimentalMaterial3Api::class,ExperimentalFoundationApi::class)
@Composable fun PerformanceScreen(s:StudioState,vm:StudioViewModel,modifier:Modifier=Modifier,onKnob:(Int)->Unit,onImport:()->Unit,onMicRecord:()->Unit){
    val colors=LocalStudioColors.current

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
            Text("PREPARAÇÃO E GRAVAÇÃO",fontWeight=FontWeight.Bold,color=colors.secondary)
            CountInControls(s,vm)
            Row(verticalAlignment=Alignment.CenterVertically){Text("Metrônomo no BPM",Modifier.weight(1f));Switch(s.metronome,{if(it!=s.metronome)vm.metronome()})}
            Text("O clique acompanha o BPM ao tocar/gravar. É excluído da mixagem e dos stems exportados. Use fones ao gravar microfone para evitar capturar o som dos alto-falantes.",fontSize=12.sp,color=colors.muted)
            MicPanel(s,vm){settings=false;onMicRecord()}
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                OutlinedButton(onClick={settings=false;onImport()},enabled=!s.busy && s.countdown==0){Text("IMPORTAR WAV")}
                OutlinedButton(onClick={settings=false;vm.page(1)}){Text("EDITAR SAMPLE")}
            }
            OutlinedButton(onClick={settings=false;vm.page(4)},modifier=Modifier.fillMaxWidth()){Text("CORES DO ESTÚDIO")}
            OutlinedButton(onClick={settings=false;onKnob(0)},modifier=Modifier.fillMaxWidth()){Text("AJUSTAR MASTER")}
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable fun CountInControls(s:StudioState,vm:StudioViewModel){
    val colors=LocalStudioColors.current

    Text("CONTAGEM ANTES DE REC",fontSize=11.sp,color=colors.secondary)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
        listOf(CountInMode.THREE_SECONDS to "3 s",CountInMode.ONE_BAR to "1 compasso",CountInMode.NONE to "Sem").forEach{(mode,label)->
            FilterChip(selected=s.countInMode==mode,onClick={vm.countIn(mode)},label={Text(label,fontSize=11.sp)},enabled=s.countdown==0 && !s.recordPads && !s.recording)
        }
    }
    Text(if(s.countInMode==CountInMode.ONE_BAR)"4 tempos: ${"%.2f".format(240.0/s.pattern.bpm)} segundos no BPM atual." else if(s.countInMode==CountInMode.THREE_SECONDS)"3 → 2 → 1. A gravação só começa após os 3 segundos." else "REC inicia imediatamente.",fontSize=11.sp,color=colors.muted)
}

@Composable private fun Header(s:StudioState,vm:StudioViewModel,onKnob:(Int)->Unit){
    val colors=LocalStudioColors.current

    Row(Modifier.fillMaxWidth().height(40.dp),verticalAlignment=Alignment.CenterVertically){
        Column(Modifier.weight(1f)){Text("breda_dev",fontSize=8.sp,color=colors.secondary);Text("BEATS / 04",fontSize=19.sp,fontWeight=FontWeight.Black)}
        TextButton(onClick={onKnob(1)},contentPadding=PaddingValues(4.dp)){Text("${s.pattern.bpm} BPM",color=colors.accentText,fontSize=12.sp)}
        IconButton(onClick={vm.undo()},enabled=s.canUndo && !s.busy && !s.recordPads && !s.recording && s.countdown==0,modifier=Modifier.size(40.dp).semantics{contentDescription="Desfazer"}){Text("↶",fontSize=22.sp)}
        IconButton(onClick={vm.redo()},enabled=s.canRedo && !s.busy && !s.recordPads && !s.recording && s.countdown==0,modifier=Modifier.size(40.dp).semantics{contentDescription="Refazer"}){Text("↷",fontSize=22.sp)}
    }
}
@Composable private fun Display(s:StudioState,compact:Boolean){
    val colors=LocalStudioColors.current

    val status=when{ s.countdown>0->"PREPARAR ${s.countdown}${if(s.countInMode==CountInMode.THREE_SECONDS)" s" else ""}";s.recording->"● REC MIC";s.recordPads->"● REC PADS";s.playing->if(s.songPlaying)"SONG ${s.songPosition+1}" else "▶ ${s.step+1}";else->"READY" }
    Card(colors=CardDefaults.cardColors(containerColor=colors.inset),border=BorderStroke(1.dp,colors.outline)){
        Column(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=6.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){Text("${s.pad+1} · ${s.project.names[s.pad]}",Modifier.weight(1f),color=colors.accentText,fontSize=10.sp,maxLines=1,overflow=TextOverflow.Ellipsis);Text(status,color=if(s.countdown>0)colors.secondary else colors.accentText,fontSize=11.sp,fontWeight=FontWeight.Bold,modifier=Modifier.testTag("record-status"))}
            if(!compact)MiniWave(s.samples[s.pad],Modifier.fillMaxWidth().height(30.dp))
            Text("${if(s.micEnabled)"MIC ARMADO" else "MIC OFF"} · ${if(s.countInMode==CountInMode.THREE_SECONDS)"PRE 3 s" else if(s.countInMode==CountInMode.ONE_BAR)"PRE 1 BAR" else "PRE OFF"} · ${if(s.metronome)"CLICK ON" else "CLICK OFF"}",fontSize=8.sp,color=colors.muted,maxLines=1)
            if(s.busy)LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
        }
    }
}
@Composable private fun Transport(s:StudioState,vm:StudioViewModel){
    val colors=LocalStudioColors.current

    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){
        SmallButton(if(s.playing)"PAUSE" else "PLAY",s.playing,Modifier.weight(1f)){vm.toggle()}
        SmallButton(if(s.countdown>0)"CANCEL" else if(s.recordPads)"END REC" else "REC PADS",s.recordPads || s.countdown>0,Modifier.weight(1.3f)){vm.recordPads()}
        SmallButton("STOP",false,Modifier.weight(1f)){vm.stop()}
    }
}
@Composable private fun Utility(s:StudioState,vm:StudioViewModel,settings:()->Unit,edit:()->Unit){
    val colors=LocalStudioColors.current

    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){
        SmallButton("LOOP",s.loop,Modifier.weight(1f)){vm.loop()}
        SmallButton("EDIT",s.padEditMode,Modifier.weight(1f)){vm.editMode()}
        SmallButton("METRO",s.metronome,Modifier.weight(1.1f)){vm.metronome()}
        SmallButton("AJUSTES",false,Modifier.weight(1.25f),settings)
    }
}
@Composable private fun SmallButton(text:String,on:Boolean,modifier:Modifier,click:()->Unit){
    val colors=LocalStudioColors.current

    Button(onClick=click,modifier=modifier.height(44.dp),shape=RoundedCornerShape(8.dp),contentPadding=PaddingValues(3.dp),colors=ButtonDefaults.buttonColors(containerColor=if(on)colors.accent else colors.raised,contentColor=if(on)colors.onAccent else colors.accentText)){Text(text,fontSize=9.sp,fontWeight=FontWeight.Bold,maxLines=1)}
}
@Composable private fun PadGrid(s:StudioState,vm:StudioViewModel,modifier:Modifier,edit:(Int)->Unit){
    val colors=LocalStudioColors.current

    val playbackPattern=if(s.songPlaying)s.project.sequence(s.project.arrangement.getOrNull(s.songPosition)?:s.project.slot) else s.pattern
    val activePads=if(s.playing && s.step>=0)playbackPattern.steps.getOrNull(s.step).orEmpty() else emptySet()
    Column(modifier.testTag("fixed-pad-grid"),verticalArrangement=Arrangement.spacedBy(6.dp)){
        for(row in 0..3)Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            for(col in 0..3){
                val pad=row*4+col
                FixedPad(pad,s.project.names[pad],s.pad==pad,!s.busy && s.countdown==0 && !s.recording,Modifier.weight(1f).fillMaxHeight(),vm,s.padEditMode,s.project.channels[pad],s.recordPads,pad in activePads){edit(pad)}
            }
        }
    }
}
@Composable private fun FixedPad(pad:Int,name:String,selected:Boolean,enabled:Boolean,modifier:Modifier,vm:StudioViewModel,editMode:Boolean,channel:Channel,recording:Boolean,sequenceActive:Boolean,edit:()->Unit){
    val colors=LocalStudioColors.current

    val accent=PadColors[pad/4]
    var pressed by remember{mutableStateOf(false)}
    var touchIntensity by remember{mutableFloatStateOf(.68f)}
    val lit=pressed || sequenceActive
    val light=when{pressed->(.38f+touchIntensity*.62f).coerceIn(0f,1f);sequenceActive->.84f;else->0f}
    val padBackground by animateColorAsState(
        targetValue=when{
            pressed->lerp(colors.inset,accent,.36f+.64f*touchIntensity)
            sequenceActive->lerp(colors.inset,accent,.68f)
            else->colors.inset
        },
        animationSpec=tween(durationMillis=70),
        label="Pad RGB"
    )
    val glow by animateDpAsState(
        targetValue=if(lit)(5f+17f*light).dp else 0.dp,
        animationSpec=tween(durationMillis=65),
        label="Pad glow"
    )
    val scale by animateFloatAsState(if(pressed).96f else 1f,label="Pad press")
    val borderColor=when{pressed->accent.copy(alpha=.7f+.3f*touchIntensity);sequenceActive->accent;selected->accent;else->accent.copy(alpha=.25f)}
    BoxWithConstraints(
        modifier
            .scale(scale)
            .shadow(glow,RoundedCornerShape(10.dp),clip=false,ambientColor=accent.copy(alpha=if(lit).42f else 0f),spotColor=accent.copy(alpha=if(lit).72f else 0f))
            .testTag("pad-${pad+1}")
            .background(padBackground,RoundedCornerShape(10.dp))
            .border(if(lit || selected)2.dp else 1.dp,borderColor,RoundedCornerShape(10.dp))
            .semantics{
                contentDescription="Pad ${pad+1}, $name"
                stateDescription=when{pressed->"Tocado, intensidade ${(touchIntensity*100).toInt()}%";sequenceActive->"Acionado pela pista";selected->"Selecionado";else->"Aguardando"}
                role=Role.Button
                onClick{
                    if(enabled){
                        if(editMode)edit() else {vm.hit(pad);vm.release(pad)}
                    }
                    enabled
                }
                onLongClick("Editar pad"){
                    if(enabled && !recording){pressed=false;vm.release(pad);edit()}
                    enabled && !recording
                }
            }
            .pointerInput(pad,enabled,editMode,recording){
                detectTapGestures(
                    onTap={if(enabled && editMode)edit()},
                    onLongPress={if(enabled && !recording){pressed=false;vm.release(pad);edit()}},
                    onPress={
                        if(enabled && !editMode){
                            pressed=true
                            vm.hit(pad)
                            try{tryAwaitRelease()}finally{pressed=false;vm.release(pad)}
                        }
                    }
                )
            }
            .pointerInput(pad,enabled,editMode,recording){
                awaitEachGesture{
                    val down=awaitFirstDown(requireUnconsumed=false,pass=PointerEventPass.Initial)
                    if(enabled && !editMode && !recording){
                        try{
                            touchIntensity=normalizeTouchPressure(down.pressure)
                            while(true){
                                val change=awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull{it.id==down.id}?:break
                                if(!change.pressed)break
                                touchIntensity=normalizeTouchPressure(change.pressure)
                            }
                        }finally{
                            touchIntensity=.68f
                        }
                    }
                }
            }
            .padding(7.dp)
    ){
        Text("${pad+1}",fontSize=if(maxHeight<48.dp)18.sp else 26.sp,fontWeight=FontWeight.Black,color=if(lit)Color.Black else colors.text,modifier=Modifier.align(Alignment.TopStart),maxLines=1)
        if(channel.pitch!=0)Text("${if(channel.pitch>0)"+" else ""}${channel.pitch}",fontSize=9.sp,color=colors.secondary,modifier=Modifier.align(Alignment.TopEnd))
        if(maxHeight>=48.dp)Text("$name · ${(channel.level*100).toInt()}%",fontSize=8.sp,color=if(lit)Color.Black else colors.muted,modifier=Modifier.align(Alignment.BottomStart),maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}
private fun normalizeTouchPressure(value:Float):Float=if(value.isFinite() && value>.01f)value.coerceIn(.18f,1f) else .68f
@Composable private fun MiniWave(sample:Sample,modifier:Modifier){
    val colors=LocalStudioColors.current

    val peaks=remember(sample){val chunk=max(1,sample.pcm.size/80);FloatArray(80){i->val a=i*chunk;val b=min(sample.pcm.size,a+chunk);if(a<b)(a until b).maxOf{abs(sample.pcm[it])}else 0f}}
    Canvas(modifier){peaks.forEachIndexed{i,v->val x=i*size.width/peaks.size;drawLine(colors.accentText,Offset(x,size.height/2-v*size.height/2),Offset(x,size.height/2+v*size.height/2),2f)}}
}
@Composable private fun SmallDial(name:String,value:Float,click:()->Unit){
    val colors=LocalStudioColors.current

    Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.clickable(onClick=click).semantics{contentDescription="Ajustar $name"}){
        Canvas(Modifier.size(40.dp)){drawCircle(colors.raised);drawCircle(colors.inset,radius=size.minDimension*.38f);val angle=(-225+value*270)*PI/180;val center=Offset(size.width/2,size.height/2);drawLine(colors.accentText,center,center+Offset((cos(angle)*size.width*.36).toFloat(),(sin(angle)*size.height*.36).toFloat()),3f)}
        Text(name,fontSize=8.sp,color=colors.accentText)
    }
}

