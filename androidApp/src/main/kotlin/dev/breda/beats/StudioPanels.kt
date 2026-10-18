package dev.breda.beats

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import dev.breda.beats.core.Channel
import dev.breda.beats.presentation.*

private val Gold=Color(0xFFF6B44E)
private val Violet=Color(0xFFB197FC)

@Composable fun SampleEditorControls(s:StudioState,vm:StudioViewModel,onChop:()->Unit){
    var range by remember(s.pad,s.samples){mutableStateOf(s.start..s.end)}
    val sample=s.samples[s.pad]
    val selectedMs=(((range.endInclusive-range.start)*sample.pcm.size.toDouble()/sample.rate)*1000).roundToInt()
    val inMs=((range.start*sample.pcm.size.toDouble()/sample.rate)*1000).roundToInt()
    val outMs=((range.endInclusive*sample.pcm.size.toDouble()/sample.rate)*1000).roundToInt()
    val enabled=!s.busy && !s.recording && !s.recordPads && s.countdown==0
    val cardColors=CardDefaults.cardColors(containerColor=Color(0xFF191C23))
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)){
        Card(colors=cardColors,border=BorderStroke(1.dp,Color(0xFF373B46))){
            Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text("RECORTE",color=Gold,fontSize=12.sp,fontWeight=FontWeight.Bold)
                        Text("PAD ${s.pad+1}  ·  ${selectedMs} ms",color=Color.LightGray,fontSize=11.sp)
                    }
                    Text("IN → OUT",color=Violet,fontSize=10.sp,fontWeight=FontWeight.Bold)
                }
                RangeSlider(value=range,onValueChange={range=it;vm.trim(it.start,it.endInclusive)},valueRange=0f..1f,enabled=enabled)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Text("IN  ${range.start.times(100).toInt()}%  ·  ${inMs} ms",color=Violet,fontSize=10.sp)
                    Text("OUT  ${range.endInclusive.times(100).toInt()}%  ·  ${outMs} ms",color=Violet,fontSize=10.sp)
                }
            }
        }
        Card(colors=cardColors,border=BorderStroke(1.dp,Color(0xFF373B46))){
            Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text("CARÁTER DO SAMPLE",color=Gold,fontSize=12.sp,fontWeight=FontWeight.Bold)
                        Text("Nível e textura",color=Color.LightGray,fontSize=10.sp)
                    }
                    FilterChip(selected=s.reverse,onClick={vm.reverse()},enabled=enabled,label={Text(if(s.reverse)"REVERSO ON" else "REVERSO",fontSize=9.sp)})
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Column(Modifier.weight(1f).background(Color(0xFF101217),RoundedCornerShape(9.dp)).padding(horizontal=9.dp,vertical=6.dp)){
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                            Text("GAIN",color=Gold,fontSize=10.sp,fontWeight=FontWeight.Bold)
                            Text("${(s.gain*100).toInt()}%",color=Color.LightGray,fontSize=10.sp)
                        }
                        Slider(value=s.gain,onValueChange=vm::gain,valueRange=0f..2f,enabled=enabled)
                    }
                    Column(Modifier.weight(1f).background(Color(0xFF101217),RoundedCornerShape(9.dp)).padding(horizontal=9.dp,vertical=6.dp)){
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                            Text("DRIVE",color=Gold,fontSize=10.sp,fontWeight=FontWeight.Bold)
                            Text("${(s.drive*100).toInt()}%",color=Color.LightGray,fontSize=10.sp)
                        }
                        Slider(value=s.drive,onValueChange=vm::drive,valueRange=0f..1f,enabled=enabled)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Button(onClick={vm.applyEdit()},enabled=enabled && range.start<range.endInclusive,modifier=Modifier.weight(1.15f).heightIn(min=50.dp),contentPadding=PaddingValues(horizontal=8.dp,vertical=8.dp)){
                Text("APLICAR · SALVAR",fontSize=10.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
            OutlinedButton(onClick=onChop,enabled=enabled && range.start<range.endInclusive,modifier=Modifier.weight(1f).heightIn(min=50.dp),contentPadding=PaddingValues(horizontal=8.dp,vertical=8.dp)){
                Text("CHOP · 16 PADS",fontSize=10.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
        }
        Text("Aplique para confirmar. CHOP divide o trecho IN → OUT nos 16 pads; UNDO desfaz.",fontSize=10.sp,color=Color.Gray)
    }
}

@Composable fun MicPanel(s:StudioState,vm:StudioViewModel,record:()->Unit){
    Card(colors=CardDefaults.cardColors(containerColor=if(s.recording)Color(0xFF4D2025) else Color(0xFF20252E))){
        Column(Modifier.fillMaxWidth().padding(12.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){
                    Text(if(s.countdownMic)"PREPARAR ${s.countdown} · MIC AINDA FECHADO" else if(s.recording)"● MICROFONE CAPTURANDO" else if(s.micEnabled)"MIC HABILITADO · NÃO CAPTURANDO" else "MIC DESLIGADO",color=if(s.recording)Color(0xFFFF7777) else Violet,fontSize=11.sp,fontWeight=FontWeight.Bold)
                    Text("REC PADS grava os toques, sem microfone",color=Color.LightGray,fontSize=10.sp)
                }
                Switch(checked=s.micEnabled,onCheckedChange={if(it!=s.micEnabled)vm.enableMic()})
            }
            if(s.micEnabled){
                Button(onClick=record,enabled=!s.busy && !s.recordPads && s.pendingTake==null,modifier=Modifier.fillMaxWidth()){
                    Text(if(s.countdownMic)"CANCELAR CONTAGEM" else if(s.recording)"ENCERRAR TAKE DO MICROFONE" else "REC SAMPLE · MICROFONE (ATÉ 60 s)",fontSize=10.sp)
                }
                Text("Gravação só substitui o sample após MANTER. Desligar MIC cancela a preparação/captura. Use fones para não captar os clicks do alto-falante.",fontSize=10.sp,color=Color.Gray)
            }
        }
    }
}

@Composable fun MixerScreen(s:StudioState,vm:StudioViewModel){
    Text("MESA DE MIXAGEM",color=Gold,fontWeight=FontWeight.Black,fontSize=24.sp)
    Text("16 canais estéreo · volume, panorama, mute e solo",color=Violet,fontSize=12.sp)
    var master by remember(s.project.master){mutableFloatStateOf(s.project.master)}
    Row(verticalAlignment=Alignment.CenterVertically){Text("MASTER ${(master*100).toInt()}%",Modifier.weight(1f));Text(if(s.clipped)"CLIP — BAIXE O MASTER" else "OUTPUT L / R",color=if(s.clipped)Color.Red else Gold,fontSize=10.sp)}
    Slider(master,{master=it;vm.volume(it)},onValueChangeFinished={vm.master(master)},enabled=!s.busy && !s.recording && s.countdown==0)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(10.dp)){
        for(pad in 0..15){
            val c=s.project.channels[pad]
            var level by remember(c.level){mutableFloatStateOf(c.level)}
            var pan by remember(c.pan){mutableFloatStateOf(c.pan)}
            var rename by remember{mutableStateOf(false)}
            var name by remember(s.project.names[pad]){mutableStateOf(s.project.names[pad])}
            Card(modifier=Modifier.width(118.dp),border=BorderStroke(1.dp,if(s.pad==pad)Violet else Color(0xFF444753))){
                Column(Modifier.padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)){
                    Text("CH ${pad+1}",fontSize=11.sp,color=Violet)
                    Text(s.project.names[pad],fontSize=10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.clickable{rename=true})
                    Text("${(level*100).toInt()}%",fontSize=20.sp,color=Gold,fontWeight=FontWeight.Black)
                    LinearProgressIndicator(progress={s.meters[pad].coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(6.dp),color=Gold)
                    Box(Modifier.height(160.dp).fillMaxWidth(),contentAlignment=Alignment.Center){
                        Slider(level,{level=it;vm.previewChannel(pad,c.copy(level=it))},onValueChangeFinished={vm.channel(pad,c.copy(level=level))},enabled=!s.busy && !s.recording && s.countdown==0,modifier=Modifier.requiredWidth(150.dp).rotate(-90f))
                    }
                    Text(if(pan==0f)"PAN C" else if(pan<0)"L ${(-pan*100).toInt()}" else "R ${(pan*100).toInt()}",fontSize=10.sp,color=Violet)
                    Slider(pan,{pan=it;vm.previewChannel(pad,c.copy(pan=it))},onValueChangeFinished={vm.channel(pad,c.copy(pan=pan))},valueRange=-1f..1f,enabled=!s.busy && !s.recording && s.countdown==0)
                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                        FilterChip(selected=c.mute,onClick={vm.channel(pad,c.copy(mute=!c.mute))},label={Text("M",fontSize=11.sp)},enabled=!s.busy && !s.recording && s.countdown==0)
                        FilterChip(selected=c.solo,onClick={vm.channel(pad,c.copy(solo=!c.solo))},label={Text("S",fontSize=11.sp)},enabled=!s.busy && !s.recording && s.countdown==0)
                    }
                    OutlinedButton(onClick={vm.hit(pad);vm.release(pad)},enabled=!s.busy && !s.recording && s.countdown==0){Text("CUE",fontSize=10.sp)}
                }
            }
            if(rename)AlertDialog(onDismissRequest={rename=false},title={Text("Nome da pista ${pad+1}")},text={OutlinedTextField(name,{name=it.take(24)},singleLine=true)},confirmButton={TextButton(onClick={vm.name(pad,name);rename=false}){Text("SALVAR")}},dismissButton={TextButton(onClick={rename=false}){Text("CANCELAR")}})
        }
    }
    Text("Deslize a mesa para acessar todos os canais. M = mute; S = solo. O master e a mixagem também são aplicados ao WAV e às pistas exportadas.",fontSize=12.sp,color=Color.Gray)
}

private val TrackPalette=listOf(Color(0xFFF6B44E),Color(0xFFFF819D),Color(0xFF6EF5D0),Color(0xFFB197FC))

@Composable fun TrackOverview(s:StudioState,bar:Int,vm:StudioViewModel,onBarChange:(Int)->Unit){
    Card(modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Color(0xFF17191F)),border=BorderStroke(1.dp,Color(0xFF30333D))){
        Column(Modifier.fillMaxWidth().padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){
                    Text("PISTAS",color=Gold,fontSize=15.sp,fontWeight=FontWeight.Black)
                    Text("16 canais · toque a faixa para selecionar",color=Color.LightGray,fontSize=9.sp)
                }
                Text("SEQUENCIADOR",color=Violet,fontSize=9.sp,fontWeight=FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Text("COMPASSO",color=Color.Gray,fontSize=9.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(end=5.dp))
                Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    for(index in 0 until s.pattern.steps.size/16){
                        FilterChip(selected=bar==index,onClick={onBarChange(index)},label={Text("${index+1}",fontSize=9.sp)},modifier=Modifier.height(34.dp))
                    }
                }
            }
            for(pad in 0..15){
                val tint=TrackPalette[pad%TrackPalette.size]
                val selected=s.pad==pad
                Row(Modifier.fillMaxWidth().height(29.dp).background(if(selected)Color(0xFF22252C) else Color.Transparent,RoundedCornerShape(5.dp)).padding(horizontal=2.dp),verticalAlignment=Alignment.CenterVertically){
                    Text(
                        "${(pad+1).toString().padStart(2,'0')} ${s.project.names[pad]}",
                        modifier=Modifier.width(69.dp).clickable{vm.select(pad)},
                        color=if(selected)tint else Color.LightGray,fontSize=8.sp,fontWeight=if(selected)FontWeight.Bold else FontWeight.Normal,
                        maxLines=1,overflow=TextOverflow.Ellipsis
                    )
                    for(i in 0..15){
                        val step=bar*16+i
                        val active=pad in s.pattern.steps[step]
                        Box(
                            Modifier.weight(1f).height(23.dp).padding(horizontal=1.dp,vertical=1.dp)
                                .background(if(active)tint else Color(0xFF30333D),RoundedCornerShape(3.dp))
                                .border(if(s.step==step)1.dp else 0.dp,Violet,RoundedCornerShape(3.dp))
                                .clickable(enabled=!s.busy && !s.recordPads && !s.recording && s.countdown==0){vm.select(pad);vm.step(step)}
                        )
                    }
                }
            }
            Text("Toque uma célula para ativar/desativar o passo; a borda violeta acompanha a execução.",fontSize=9.sp,color=Color.Gray)
        }
    }
}
