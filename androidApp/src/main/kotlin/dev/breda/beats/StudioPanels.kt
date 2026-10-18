package dev.breda.beats

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import dev.breda.beats.core.Channel
import dev.breda.beats.presentation.*


@Composable fun SampleEditorControls(s:StudioState,vm:StudioViewModel,onChop:()->Unit){
    val colors=LocalStudioColors.current

    var range by remember(s.pad,s.samples){mutableStateOf(s.start..s.end)}
    val sample=s.samples[s.pad]
    val selectedMs=(((range.endInclusive-range.start)*sample.pcm.size.toDouble()/sample.rate)*1000).roundToInt()
    val inMs=((range.start*sample.pcm.size.toDouble()/sample.rate)*1000).roundToInt()
    val outMs=((range.endInclusive*sample.pcm.size.toDouble()/sample.rate)*1000).roundToInt()
    val enabled=!s.busy && !s.recording && !s.recordPads && s.countdown==0
    val cardColors=CardDefaults.cardColors(containerColor=colors.panel)
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)){
        Card(colors=cardColors,border=BorderStroke(1.dp,colors.outline)){
            Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text("RECORTE",color=colors.accentText,fontSize=12.sp,fontWeight=FontWeight.Bold)
                        Text("PAD ${s.pad+1}  ·  ${selectedMs} ms",color=colors.muted,fontSize=11.sp)
                    }
                    Text("IN → OUT",color=colors.secondary,fontSize=10.sp,fontWeight=FontWeight.Bold)
                }
                RangeSlider(value=range,onValueChange={range=it;vm.trim(it.start,it.endInclusive)},valueRange=0f..1f,enabled=enabled)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Text("IN  ${range.start.times(100).toInt()}%  ·  ${inMs} ms",color=colors.secondary,fontSize=10.sp)
                    Text("OUT  ${range.endInclusive.times(100).toInt()}%  ·  ${outMs} ms",color=colors.secondary,fontSize=10.sp)
                }
            }
        }
        Card(colors=cardColors,border=BorderStroke(1.dp,colors.outline)){
            Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text("CARÁTER DO SAMPLE",color=colors.accentText,fontSize=12.sp,fontWeight=FontWeight.Bold)
                        Text("Nível e textura",color=colors.muted,fontSize=10.sp)
                    }
                    FilterChip(selected=s.reverse,onClick={vm.reverse()},enabled=enabled,label={Text(if(s.reverse)"REVERSO ON" else "REVERSO",fontSize=9.sp)})
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Column(Modifier.weight(1f).background(colors.inset,RoundedCornerShape(9.dp)).padding(horizontal=9.dp,vertical=6.dp)){
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                            Text("GAIN",color=colors.accentText,fontSize=10.sp,fontWeight=FontWeight.Bold)
                            Text("${(s.gain*100).toInt()}%",color=colors.muted,fontSize=10.sp)
                        }
                        Slider(value=s.gain,onValueChange=vm::gain,valueRange=0f..2f,enabled=enabled)
                    }
                    Column(Modifier.weight(1f).background(colors.inset,RoundedCornerShape(9.dp)).padding(horizontal=9.dp,vertical=6.dp)){
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                            Text("DRIVE",color=colors.accentText,fontSize=10.sp,fontWeight=FontWeight.Bold)
                            Text("${(s.drive*100).toInt()}%",color=colors.muted,fontSize=10.sp)
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
        Text("Aplique para confirmar. CHOP divide o trecho IN → OUT nos 16 pads; UNDO desfaz.",fontSize=10.sp,color=colors.muted)
    }
}

@Composable fun MicPanel(s:StudioState,vm:StudioViewModel,record:()->Unit){
    val colors=LocalStudioColors.current

    Card(colors=CardDefaults.cardColors(containerColor=if(s.recording)Color(0xFF4D2025) else colors.panel)){
        Column(Modifier.fillMaxWidth().padding(12.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){
                    Text(if(s.countdownMic)"PREPARAR ${s.countdown} · MIC AINDA FECHADO" else if(s.recording)"● MICROFONE CAPTURANDO" else if(s.micEnabled)"MIC HABILITADO · NÃO CAPTURANDO" else "MIC DESLIGADO",color=if(s.recording)Color(0xFFFF7777) else colors.secondary,fontSize=11.sp,fontWeight=FontWeight.Bold)
                    Text("REC PADS grava os toques, sem microfone",color=colors.muted,fontSize=10.sp)
                }
                Switch(checked=s.micEnabled,onCheckedChange={if(it!=s.micEnabled)vm.enableMic()})
            }
            if(s.micEnabled){
                Button(onClick=record,enabled=!s.busy && !s.recordPads && s.pendingTake==null,modifier=Modifier.fillMaxWidth()){
                    Text(if(s.countdownMic)"CANCELAR CONTAGEM" else if(s.recording)"ENCERRAR TAKE DO MICROFONE" else "REC SAMPLE · MICROFONE (ATÉ 60 s)",fontSize=10.sp)
                }
                Text("Gravação só substitui o sample após MANTER. Desligar MIC cancela a preparação/captura. Use fones para não captar os clicks do alto-falante.",fontSize=10.sp,color=colors.muted)
            }
        }
    }
}

private val TrackPalette=listOf(Color(0xFFF6B44E),Color(0xFFFF819D),Color(0xFF6EF5D0),Color(0xFFB197FC))

@Composable fun TrackOverview(s:StudioState,bar:Int,vm:StudioViewModel,onBarChange:(Int)->Unit){
    val colors=LocalStudioColors.current

    Card(modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=colors.panel),border=BorderStroke(1.dp,colors.raised)){
        Column(Modifier.fillMaxWidth().padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){
                    Text("PISTAS",color=colors.accentText,fontSize=15.sp,fontWeight=FontWeight.Black)
                    Text("16 canais · toque a faixa para selecionar",color=colors.muted,fontSize=9.sp)
                }
                Text("SEQUENCIADOR",color=colors.secondary,fontSize=9.sp,fontWeight=FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Text("COMPASSO",color=colors.muted,fontSize=9.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(end=5.dp))
                Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    for(index in 0 until s.pattern.steps.size/16){
                        FilterChip(selected=bar==index,onClick={onBarChange(index)},label={Text("${index+1}",fontSize=9.sp)},modifier=Modifier.height(34.dp))
                    }
                }
            }
            for(pad in 0..15){
                val tint=TrackPalette[pad%TrackPalette.size]
                val selected=s.pad==pad
                Row(Modifier.fillMaxWidth().height(29.dp).background(if(selected)colors.raised else Color.Transparent,RoundedCornerShape(5.dp)).padding(horizontal=2.dp),verticalAlignment=Alignment.CenterVertically){
                    Text(
                        "${(pad+1).toString().padStart(2,'0')} ${s.project.names[pad]}",
                        modifier=Modifier.width(69.dp).clickable{vm.select(pad)},
                        color=if(selected)tint else colors.muted,fontSize=8.sp,fontWeight=if(selected)FontWeight.Bold else FontWeight.Normal,
                        maxLines=1,overflow=TextOverflow.Ellipsis
                    )
                    for(i in 0..15){
                        val step=bar*16+i
                        val active=pad in s.pattern.steps[step]
                        Box(
                            Modifier.weight(1f).height(23.dp).padding(horizontal=1.dp,vertical=1.dp)
                                .background(if(active)tint else colors.raised,RoundedCornerShape(3.dp))
                                .border(if(s.step==step)1.dp else 0.dp,colors.secondary,RoundedCornerShape(3.dp))
                                .clickable(enabled=!s.busy && !s.recordPads && !s.recording && s.countdown==0){vm.select(pad);vm.step(step)}
                        )
                    }
                }
            }
            Text("Toque uma célula para ativar/desativar o passo; a borda violeta acompanha a execução.",fontSize=9.sp,color=colors.muted)
        }
    }
}

