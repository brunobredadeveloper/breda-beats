package dev.breda.beats

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import dev.breda.beats.core.*
import dev.breda.beats.presentation.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable fun PadPanel(pad:Int,s:StudioState,vm:StudioViewModel,close:()->Unit){
    val colors=LocalStudioColors.current

    val channel=s.project.channels[pad]
    var volume by remember(channel.level){mutableFloatStateOf(channel.level)}
    var pan by remember(channel.pan){mutableFloatStateOf(channel.pan)}
    var pitch by remember(channel.pitch){mutableFloatStateOf(channel.pitch.toFloat())}
    var name by remember(s.project.names[pad]){mutableStateOf(s.project.names[pad])}
    var copy by remember{mutableStateOf(false)}
    var swap by remember{mutableStateOf(false)}
    var delete by remember{mutableStateOf(false)}
    val enabled=!s.busy && !s.recording && !s.recordPads && s.countdown==0
    ModalBottomSheet(onDismissRequest=close){
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text("PAD ${pad+1} · ${s.project.names[pad]}",color=colors.secondary,fontSize=22.sp,fontWeight=FontWeight.Black)
            Text("Toque longo abre os controles. EDIT permite selecionar sem tocar o sample.",fontSize=12.sp,color=colors.muted)
            OutlinedTextField(name,{name=it.take(24)},label={Text("Nome do pad")},singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedButton(onClick={vm.name(pad,name)},enabled=enabled){Text("SALVAR NOME")}
            Text("VOLUME ${(volume*100).toInt()}%",color=colors.secondary)
            Slider(volume,{volume=it;vm.previewChannel(pad,channel.copy(level=it))},onValueChangeFinished={vm.channel(pad,channel.copy(level=volume))},enabled=enabled)
            Text("PAN ${if(pan==0f)"CENTRO" else if(pan<0)"L ${(-pan*100).toInt()}" else "R ${(pan*100).toInt()}"}")
            Slider(pan,{pan=it;vm.previewChannel(pad,channel.copy(pan=it))},onValueChangeFinished={vm.channel(pad,channel.copy(pan=pan))},valueRange=-1f..1f,enabled=enabled)
            Text("PITCH ${if(pitch>=0)"+" else ""}${pitch.toInt()} semitons",color=colors.secondary)
            Slider(pitch,{pitch=it},onValueChangeFinished={vm.channel(pad,channel.copy(pitch=pitch.toInt()))},valueRange=-24f..24f,steps=47,enabled=enabled)
            Text("Pitch também altera a duração. Não é time-stretch.",fontSize=11.sp,color=colors.muted)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                FilterChip(channel.reverse,{vm.channel(pad,channel.copy(reverse=!channel.reverse))},label={Text("REVERSE")},enabled=enabled)
                FilterChip(channel.loop,{vm.channel(pad,channel.copy(loop=!channel.loop))},label={Text("LOOP")},enabled=enabled)
                FilterChip(channel.gate,{vm.channel(pad,channel.copy(gate=!channel.gate))},label={Text("GATE")},enabled=enabled)
            }
            Text("GRUPO DE CORTE · um pad interrompe os outros do grupo",fontSize=11.sp,color=colors.secondary)
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){for(group in 0..4)FilterChip(channel.choke==group,{vm.channel(pad,channel.copy(choke=group))},label={Text(if(group==0)"OFF" else "$group")},enabled=enabled)}
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                OutlinedButton(onClick={vm.hit(pad);vm.release(pad)},enabled=enabled){Text("OUVIR")}
                OutlinedButton(onClick={close();vm.page(1)}){Text("EDITAR ONDA")}
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                OutlinedButton(onClick={vm.normalize()},enabled=enabled){Text("NORMALIZAR")}
                OutlinedButton(onClick={vm.trimSilence()},enabled=enabled){Text("TIRAR SILÊNCIO")}
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                OutlinedButton(onClick={copy=true;swap=false},enabled=enabled){Text("COPIAR")}
                OutlinedButton(onClick={copy=true;swap=true},enabled=enabled){Text("TROCAR")}
                OutlinedButton(onClick={delete=true},enabled=enabled){Text("EXCLUIR")}
            }
            Text("Alterações podem ser desfeitas com UNDO. GATE funciona enquanto o pad é segurado ao tocar ao vivo.",fontSize=11.sp,color=colors.muted)
            Spacer(Modifier.height(20.dp))
        }
    }
    if(copy)AlertDialog(onDismissRequest={copy=false},title={Text(if(swap)"Trocar com qual pad?" else "Copiar para qual pad?")},text={Column{Text("O destino será substituído. UNDO restaura a ação.");for(row in 0..3)Row{for(col in 0..3){val target=row*4+col;TextButton(onClick={vm.copyPad(target,swap);copy=false},enabled=target!=pad && enabled,modifier=Modifier.weight(1f)){Text("${target+1}")}}}}},confirmButton={TextButton(onClick={copy=false}){Text("CANCELAR")}})
    if(delete)AlertDialog(onDismissRequest={delete=false},title={Text("Excluir sample do pad ${pad+1}?")},text={Text("O pad fica vazio; UNDO restaura o sample. As notas do sequenciador permanecem.")},confirmButton={TextButton(onClick={vm.deletePad();delete=false}){Text("EXCLUIR")}},dismissButton={TextButton(onClick={delete=false}){Text("CANCELAR")}})
}

@OptIn(ExperimentalFoundationApi::class)
@Composable fun SequencePanel(s:StudioState,vm:StudioViewModel,onExportSong:()->Unit){
    val colors=LocalStudioColors.current

    var duplicate by remember{mutableIntStateOf(-1)}
    Text("SEQUÊNCIAS · ARRANJO",color=colors.secondary,fontWeight=FontWeight.Bold,fontSize=20.sp)
    Text("Toque para selecionar. Segure outra sequência para copiar a atual.",fontSize=11.sp,color=colors.muted)
    for(row in 0..1)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
        for(col in 0..3){val slot=row*4+col;val clip=s.project.sequence(slot);val used=clip.steps.any{it.isNotEmpty()}
            Card(Modifier.weight(1f).height(60.dp).combinedClickable(onClick={vm.sequence(slot)},onLongClick={if(slot!=s.project.slot)duplicate=slot}),colors=CardDefaults.cardColors(containerColor=if(slot==s.project.slot)lerp(colors.panel,colors.secondary,.24f) else colors.raised)){Column(Modifier.padding(8.dp)){Text("S${slot+1}",color=if(used)colors.secondary else colors.muted,fontWeight=FontWeight.Bold);Text("${clip.steps.size/16} compasso(s)",fontSize=8.sp)}}
        }
    }
    Text("MÚSICA: ${s.project.arrangement.joinToString(" → "){"S${it+1}"}}",color=colors.secondary,fontSize=12.sp)
    if(s.songPlaying)Text("TOCANDO PARTE ${s.songPosition+1}/${s.project.arrangement.size}",color=colors.secondary,fontSize=12.sp)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
        OutlinedButton(onClick={vm.appendSequence(s.project.slot)},enabled=!s.busy && !s.playing){Text("+ S${s.project.slot+1}")}
        OutlinedButton(onClick={vm.removeLastSequence()},enabled=!s.busy && !s.playing){Text("REMOVER ÚLTIMA")}
    }
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
        Button(onClick={vm.playSong()},enabled=!s.busy && !s.recording && !s.recordPads && s.countdown==0){Text(if(s.songPlaying)"PARAR MÚSICA" else "TOCAR MÚSICA")}
        OutlinedButton(onClick=onExportSong,enabled=!s.busy && !s.recording){Text("WAV MÚSICA")}
    }
    if(duplicate>=0)AlertDialog(onDismissRequest={duplicate=-1},title={Text("Copiar S${s.project.slot+1} para S${duplicate+1}?")},text={Text("O conteúdo do destino será substituído. UNDO restaura a cópia.")},confirmButton={TextButton(onClick={vm.duplicateSequence(duplicate);duplicate=-1}){Text("COPIAR")}},dismissButton={TextButton(onClick={duplicate=-1}){Text("CANCELAR")}})
}

@Composable fun EffectsPanel(s:StudioState,vm:StudioViewModel){
    val colors=LocalStudioColors.current

    var confirmResample by remember{mutableStateOf(false)}
    var filter by remember(s.project.fx.filter){mutableFloatStateOf(s.project.fx.filter)}
    var drive by remember(s.project.fx.drive){mutableFloatStateOf(s.project.fx.drive)}
    var delay by remember(s.project.fx.delay){mutableFloatStateOf(s.project.fx.delay)}
    var mode by remember(s.project.fx.mode){mutableStateOf(s.project.fx.mode)}
    var amount by remember(s.project.fx.amount){mutableFloatStateOf(s.project.fx.amount)}
    val enabled=!s.busy && !s.recording && s.countdown==0
    fun settings()=FxSettings(filter,drive,delay,mode,amount)
    Text("PERFORM FX · MASTER",color=colors.secondary,fontWeight=FontWeight.Bold,fontSize=20.sp)
    Text("FX DE PERFORMANCE",fontSize=12.sp,color=colors.secondary)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
        FxMode.entries.forEach{choice->
            FilterChip(selected=mode==choice,onClick={
                val next=settings().selectMode(choice)
                mode=choice;drive=next.drive;delay=next.delay;amount=next.amount
                vm.effect(next)
            },enabled=enabled,label={Text(choice.label,fontSize=10.sp)})
        }
    }
    if(mode==FxMode.DJFX_LOOPER){
        val division=listOf(1,2,4,8,16,32)[(amount*5).roundToInt().coerceIn(0,5)]
        Text("CTRL 1 · LOOP ${if(division==1)"1 tempo" else "1/$division do tempo"}",fontSize=12.sp,color=colors.secondary)
        Slider(amount,{amount=it;vm.previewEffect(settings())},onValueChangeFinished={vm.effect(settings())},enabled=enabled)
        Text("CTRL 2 · VELOCIDADE / DIREÇÃO  ${"%.1f".format(drive*2f-1f)}×",fontSize=12.sp,color=colors.secondary)
        Slider(drive,{drive=it;vm.previewEffect(settings())},onValueChangeFinished={vm.effect(settings())},enabled=enabled)
        Text("CTRL 3 · MIX DO LOOP  ${(delay*100).toInt()}%",fontSize=12.sp,color=colors.secondary)
        Slider(delay,{delay=it;vm.previewEffect(settings())},onValueChangeFinished={vm.effect(settings())},enabled=enabled)
        Text("Captura um trecho no BPM e repete para a frente, ao contrário ou parado.",fontSize=11.sp,color=colors.muted)
    }else{
        if(mode!=FxMode.STUDIO){
            val valueLabel=if(mode==FxMode.FILTER_SWEEP)"${(80+amount*amount*15920).toInt()} Hz" else "${(amount*100).toInt()}%"
            Text("${mode.label} · $valueLabel",fontSize=12.sp)
            Slider(amount,{amount=it;vm.previewEffect(settings())},onValueChangeFinished={vm.effect(settings())},enabled=enabled)
            Text(mode.description,fontSize=11.sp,color=colors.muted)
        }
        Text("LOW-PASS ${if(filter>=.999f)"BYPASS" else "${(40+filter*filter*17960).toInt()} Hz"}",fontSize=12.sp)
        Slider(filter,{filter=it;vm.previewEffect(settings())},onValueChangeFinished={vm.effect(settings())},enabled=enabled)
        Text("SATURATION ${(drive*100).toInt()}%",fontSize=12.sp)
        Slider(drive,{drive=it;vm.previewEffect(settings())},onValueChangeFinished={vm.effect(settings())},enabled=enabled)
        Text("${if(mode==FxMode.TAPE_ECHO)"TAPE ECHO · FEEDBACK" else "DELAY 1/8"} · ${(delay*100).toInt()}%",fontSize=12.sp)
        Slider(delay,{delay=it;vm.previewEffect(settings())},onValueChangeFinished={vm.effect(settings())},enabled=enabled)
    }
    OutlinedButton(onClick={filter=1f;drive=0f;delay=0f;mode=FxMode.STUDIO;amount=.55f;vm.effect(FxSettings())},enabled=enabled){Text("RESET FX")}
    Text("Os efeitos atuam na mix estéreo e na música. Stems são exportados sem o FX do master para mixagem no computador. Movimentos não são gravados como automação.",fontSize=11.sp,color=colors.muted)
    Button(onClick={confirmResample=true},enabled=enabled && !s.playing){Text("RESAMPLE S${s.project.slot+1} → PAD ${s.pad+1}")}
    if(confirmResample)AlertDialog(onDismissRequest={confirmResample=false},title={Text("Substituir pad ${s.pad+1} pelo resample?")},text={Text("A sequência será renderizada com sua mixagem e efeitos. Faça um backup para guardar o projeto atual.")},confirmButton={TextButton(onClick={vm.resample();confirmResample=false}){Text("RESAMPLE")}},dismissButton={TextButton(onClick={confirmResample=false}){Text("CANCELAR")}})
    Text("Resample gera áudio interno com o FX e a mixagem, sem usar o microfone. Substitui o pad selecionado; UNDO restaura. Pare o transporte antes de resamplear.",fontSize=11.sp,color=colors.muted)
}

