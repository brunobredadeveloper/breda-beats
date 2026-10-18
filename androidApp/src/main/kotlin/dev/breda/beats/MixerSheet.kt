package dev.breda.beats

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.breda.beats.presentation.*
import kotlin.math.roundToInt

/** A persistent sheet keeps the transport outside the overlay and preserves the current page. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun MixerHost(s:StudioState,vm:StudioViewModel,open:Boolean,onDismiss:()->Unit,onEffects:()->Unit,modifier:Modifier=Modifier,content:@Composable ()->Unit) {
    val colors=LocalStudioColors.current
    val sheet=rememberStandardBottomSheetState(initialValue=if(open)SheetValue.Expanded else SheetValue.Hidden,skipHiddenState=false)
    val scaffold=rememberBottomSheetScaffoldState(bottomSheetState=sheet)
    val currentOpen by rememberUpdatedState(open)
    val dismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(open) { if(open)sheet.expand() else if(sheet.currentValue!=SheetValue.Hidden)sheet.hide() }
    LaunchedEffect(sheet) {
        snapshotFlow{sheet.currentValue}.collect{if(it!=SheetValue.Expanded && currentOpen)dismiss()}
    }
    BackHandler(open,onBack=onDismiss)
    BoxWithConstraints(modifier.fillMaxSize()) {
        val panelHeight=(maxHeight-26.dp).coerceAtLeast(0.dp)
        BottomSheetScaffold(
            scaffoldState=scaffold,sheetPeekHeight=0.dp,sheetMaxWidth=Dp.Unspecified,
            sheetContainerColor=colors.panel,sheetContentColor=colors.text,
            sheetShape=RoundedCornerShape(topStart=20.dp,topEnd=20.dp),sheetShadowElevation=12.dp,
            containerColor=colors.background,contentColor=colors.text,
            sheetDragHandle={Box(Modifier.fillMaxWidth().height(12.dp),contentAlignment=Alignment.Center){Box(Modifier.width(32.dp).height(3.dp).background(colors.outline,RoundedCornerShape(3.dp)))}},
            sheetContent={MixerPanel(s,vm,onDismiss,onEffects,Modifier.height(panelHeight))}
        ) { content() }
    }
}

@Composable private fun MixerPanel(s:StudioState,vm:StudioViewModel,onDismiss:()->Unit,onEffects:()->Unit,modifier:Modifier) {
    val colors=LocalStudioColors.current
    var selected by rememberSaveable{mutableIntStateOf(s.pad)}
    var master by remember(s.project.master){mutableFloatStateOf(s.project.master)}
    val enabled=!s.busy && !s.recording && s.countdown==0
    BoxWithConstraints(modifier.fillMaxWidth().testTag("mixer-sheet").padding(horizontal=10.dp)) {
        val columns=if(maxWidth>600.dp)16 else 8
        Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth().height(30.dp),verticalAlignment=Alignment.CenterVertically) {
                Text("MESA DE MIXAGEM",fontWeight=FontWeight.Black,fontSize=15.sp,color=colors.accentText)
                Text("  16 CANAIS",Modifier.weight(1f),fontSize=9.sp,color=colors.muted)
                TextButton(onClick=onEffects,contentPadding=PaddingValues(horizontal=8.dp),modifier=Modifier.testTag("mixer-fx")){Text("FX",fontSize=11.sp)}
                IconButton(onClick=onDismiss,modifier=Modifier.size(30.dp).testTag("mixer-close").semantics{contentDescription="Fechar mixer"}){Text("×",fontSize=24.sp)}
            }
            Row(Modifier.fillMaxWidth().height(32.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("MASTER ${(master*100).roundToInt()}%",fontSize=10.sp,color=colors.accentText)
                Slider(master,{master=it;vm.volume(it)},onValueChangeFinished={vm.master(master)},enabled=enabled,modifier=Modifier.weight(1f).testTag("mixer-master"))
                Text(if(s.clipped)"CLIP" else "L / R",fontSize=9.sp,color=if(s.clipped)MaterialTheme.colorScheme.error else colors.muted)
            }
            Column(Modifier.weight(1f).fillMaxWidth().testTag("mixer-channel-grid"),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                for(row in 0 until 16/columns)Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                    for(col in 0 until columns) {
                        val pad=row*columns+col
                        MixerStrip(pad,s,vm,pad==selected,{selected=pad},Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
            MixerSelectedChannel(selected,s,vm)
            Spacer(Modifier.height(3.dp))
        }
    }
}

@Composable private fun MixerStrip(pad:Int,s:StudioState,vm:StudioViewModel,selected:Boolean,onSelect:()->Unit,modifier:Modifier) {
    val colors=LocalStudioColors.current
    val channel=s.project.channels[pad]
    val currentChannel by rememberUpdatedState(channel)
    var level by remember(channel.level){mutableFloatStateOf(channel.level)}
    val select by rememberUpdatedState(onSelect)
    val tint=listOf(colors.accent,Color(0xFFFF819D),Color(0xFF6EF5D0),colors.secondary)[pad/4]
    val enabled=!s.busy && !s.recording && s.countdown==0
    val meter=s.meters[pad].coerceIn(0f,1f)
    val soloed=s.project.channels.any{it.solo}
    val silent=channel.mute || (soloed && !channel.solo)
    Column(modifier.background(if(selected)colors.raised else colors.inset,RoundedCornerShape(7.dp))
        .border(if(selected)1.5.dp else .5.dp,if(selected)colors.accentText else colors.outline,RoundedCornerShape(7.dp))
        .testTag("mixer-channel-${pad+1}").padding(horizontal=3.dp,vertical=3.dp),
        horizontalAlignment=Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().clickable(onClick=onSelect),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
            Text("${pad+1}",fontSize=12.sp,fontWeight=FontWeight.Bold,color=colors.text)
            if(channel.mute || channel.solo)Text(if(channel.mute)" M" else " S",fontSize=8.sp,color=colors.accentText)
        }
        Text(s.project.names[pad],fontSize=7.sp,maxLines=1,overflow=TextOverflow.Ellipsis,color=colors.muted)
        Canvas(Modifier.weight(1f).fillMaxWidth().testTag("mixer-fader-${pad+1}").semantics {
            contentDescription="Volume canal ${pad+1}, ${s.project.names[pad]}"
            stateDescription="${(level*100).roundToInt()}%"
            progressBarRangeInfo=ProgressBarRangeInfo(level,0f..1f)
            setProgress{target->
                if(enabled){select();level=target.coerceIn(0f,1f);vm.channel(pad,currentChannel.copy(level=level));true}else false
            }
        }.pointerInput(pad,enabled) {
            if(enabled)detectDragGestures(
                onDragStart={position->select();level=(1f-(position.y-8.dp.toPx())/(size.height-16.dp.toPx()).coerceAtLeast(1f)).coerceIn(0f,1f);vm.previewChannel(pad,currentChannel.copy(level=level))},
                onDragEnd={vm.channel(pad,currentChannel.copy(level=level))},
                onDragCancel={level=currentChannel.level;vm.previewChannel(pad,currentChannel)}
            ){change,amount->change.consume();level=(level-amount.y/(size.height-16.dp.toPx()).coerceAtLeast(1f)).coerceIn(0f,1f);vm.previewChannel(pad,currentChannel.copy(level=level))}
        }.pointerInput(pad,enabled) {
            if(enabled)detectTapGestures{position->select();level=(1f-(position.y-8.dp.toPx())/(size.height-16.dp.toPx()).coerceAtLeast(1f)).coerceIn(0f,1f);vm.channel(pad,currentChannel.copy(level=level))}
        }) {
            val top=8.dp.toPx();val bottom=(size.height-top).coerceAtLeast(top)
            val x=size.width*.42f;val y=bottom-(bottom-top)*level
            drawLine(colors.outline,Offset(x,top),Offset(x,bottom),3.dp.toPx(),StrokeCap.Round)
            drawLine(if(silent)colors.muted else tint,Offset(x,bottom),Offset(x,y),3.dp.toPx(),StrokeCap.Round)
            drawLine(colors.outline,Offset(size.width-2.dp.toPx(),top),Offset(size.width-2.dp.toPx(),bottom),2.dp.toPx())
            if(meter>0f)drawLine(if(silent)colors.muted else tint,Offset(size.width-2.dp.toPx(),bottom),Offset(size.width-2.dp.toPx(),bottom-(bottom-top)*meter),2.dp.toPx())
            drawRoundRect(colors.text,Offset(1.dp.toPx(),y-4.dp.toPx()),androidx.compose.ui.geometry.Size(size.width-7.dp.toPx(),8.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
            drawLine(colors.background,Offset(4.dp.toPx(),y),Offset(size.width-9.dp.toPx(),y),1.dp.toPx())
        }
        Text("${(level*100).roundToInt()}%",fontSize=9.sp,color=colors.text)
    }
}

@Composable private fun MixerSelectedChannel(pad:Int,s:StudioState,vm:StudioViewModel) {
    val colors=LocalStudioColors.current
    val channel=s.project.channels[pad]
    var pan by remember(pad,channel.pan){mutableFloatStateOf(channel.pan)}
    var rename by remember{mutableStateOf(false)}
    var name by remember(pad,s.project.names[pad]){mutableStateOf(s.project.names[pad])}
    val enabled=!s.busy && !s.recording && s.countdown==0
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(32.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            Text("CH ${pad+1} · ${s.project.names[pad]}",Modifier.weight(1f).clickable{rename=true}.testTag("mixer-selected-name"),fontSize=11.sp,fontWeight=FontWeight.Bold,color=colors.text,maxLines=1,overflow=TextOverflow.Ellipsis)
            MixerToggle("M",channel.mute,enabled,"mixer-mute"){vm.channel(pad,channel.copy(mute=!channel.mute))}
            MixerToggle("S",channel.solo,enabled,"mixer-solo"){vm.channel(pad,channel.copy(solo=!channel.solo))}
            TextButton(onClick={vm.hit(pad);vm.release(pad)},enabled=enabled,contentPadding=PaddingValues(4.dp),modifier=Modifier.height(32.dp).testTag("mixer-cue")){Text("CUE",fontSize=10.sp)}
        }
        Row(Modifier.fillMaxWidth().height(30.dp),verticalAlignment=Alignment.CenterVertically) {
            Text(if(pan==0f)"PAN C" else if(pan<0)"PAN L ${(-pan*100).roundToInt()}" else "PAN R ${(pan*100).roundToInt()}",Modifier.width(78.dp),fontSize=9.sp,color=colors.muted)
            Slider(pan,{pan=it;vm.previewChannel(pad,channel.copy(pan=it))},onValueChangeFinished={vm.channel(pad,channel.copy(pan=pan))},valueRange=-1f..1f,enabled=enabled,modifier=Modifier.weight(1f).testTag("mixer-pan"))
            TextButton(onClick={pan=0f;vm.channel(pad,channel.copy(pan=0f))},enabled=enabled,contentPadding=PaddingValues(2.dp),modifier=Modifier.height(30.dp)){Text("C",fontSize=10.sp)}
        }
    }
    if(rename)AlertDialog(onDismissRequest={rename=false},title={Text("Nome da pista ${pad+1}")},text={OutlinedTextField(name,{name=it.take(24)},singleLine=true)},confirmButton={TextButton(onClick={vm.name(pad,name);rename=false}){Text("SALVAR")}},dismissButton={TextButton(onClick={rename=false}){Text("CANCELAR")}})
}

@Composable private fun MixerToggle(label:String,selected:Boolean,enabled:Boolean,tag:String,onClick:()->Unit) {
    val colors=LocalStudioColors.current
    Surface(onClick=onClick,enabled=enabled,modifier=Modifier.size(width=36.dp,height=30.dp).testTag(tag).semantics{stateDescription=if(selected)"Ligado" else "Desligado";contentDescription=if(label=="M")"Mute" else "Solo"},
        color=if(selected)colors.accent else colors.raised,contentColor=if(selected)colors.onAccent else colors.text,shape=RoundedCornerShape(5.dp)) {
        Box(contentAlignment=Alignment.Center){Text(label,fontSize=11.sp,fontWeight=FontWeight.Bold)}
    }
}
