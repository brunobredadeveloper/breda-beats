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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.breda.beats.core.Channel
import dev.breda.beats.presentation.*

private val Gold=Color(0xFFF6B44E)
private val Violet=Color(0xFFB197FC)

@Composable fun MicPanel(s:StudioState,vm:StudioViewModel,record:()->Unit){
    Card(colors=CardDefaults.cardColors(containerColor=if(s.recording)Color(0xFF4D2025) else Color(0xFF20252E))){
        Column(Modifier.fillMaxWidth().padding(12.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){
                    Text(if(s.recording)"● MICROFONE CAPTURANDO" else if(s.micEnabled)"MIC HABILITADO · NÃO CAPTURANDO" else "MIC DESLIGADO",color=if(s.recording)Color(0xFFFF7777) else Violet,fontSize=11.sp,fontWeight=FontWeight.Bold)
                    Text("REC PADS grava os toques, sem microfone",color=Color.LightGray,fontSize=10.sp)
                }
                Switch(checked=s.micEnabled,onCheckedChange={if(it!=s.micEnabled)vm.enableMic()})
            }
            if(s.micEnabled){
                Button(onClick=record,enabled=!s.busy && !s.recordPads && s.pendingTake==null,modifier=Modifier.fillMaxWidth()){
                    Text(if(s.recording)"ENCERRAR TAKE DO MICROFONE" else "REC SAMPLE · MICROFONE (ATÉ 10 s)",fontSize=10.sp)
                }
                Text("Gravação só substitui o sample após MANTER. Desligar MIC descarta a captura em andamento.",fontSize=10.sp,color=Color.Gray)
            }
        }
    }
}

@Composable fun MixerScreen(s:StudioState,vm:StudioViewModel){
    Text("MESA DE MIXAGEM",color=Gold,fontWeight=FontWeight.Black,fontSize=24.sp)
    Text("16 canais estéreo · volume, panorama, mute e solo",color=Violet,fontSize=12.sp)
    var master by remember(s.project.master){mutableFloatStateOf(s.project.master)}
    Row(verticalAlignment=Alignment.CenterVertically){Text("MASTER ${(master*100).toInt()}%",Modifier.weight(1f));Text(if(s.clipped)"CLIP — BAIXE O MASTER" else "OUTPUT L / R",color=if(s.clipped)Color.Red else Gold,fontSize=10.sp)}
    Slider(master,{master=it;vm.volume(it)},onValueChangeFinished={vm.master(master)},enabled=!s.busy && !s.recording)
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
                        Slider(level,{level=it;vm.previewChannel(pad,c.copy(level=it))},onValueChangeFinished={vm.channel(pad,c.copy(level=level))},enabled=!s.busy && !s.recording,modifier=Modifier.requiredWidth(150.dp).rotate(-90f))
                    }
                    Text(if(pan==0f)"PAN C" else if(pan<0)"L ${(-pan*100).toInt()}" else "R ${(pan*100).toInt()}",fontSize=10.sp,color=Violet)
                    Slider(pan,{pan=it;vm.previewChannel(pad,c.copy(pan=it))},onValueChangeFinished={vm.channel(pad,c.copy(pan=pan))},valueRange=-1f..1f,enabled=!s.busy && !s.recording)
                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                        FilterChip(selected=c.mute,onClick={vm.channel(pad,c.copy(mute=!c.mute))},label={Text("M",fontSize=11.sp)},enabled=!s.busy && !s.recording)
                        FilterChip(selected=c.solo,onClick={vm.channel(pad,c.copy(solo=!c.solo))},label={Text("S",fontSize=11.sp)},enabled=!s.busy && !s.recording)
                    }
                    OutlinedButton(onClick={vm.hit(pad);vm.release(pad)},enabled=!s.busy && !s.recording){Text("CUE",fontSize=10.sp)}
                }
            }
            if(rename)AlertDialog(onDismissRequest={rename=false},title={Text("Nome da pista ${pad+1}")},text={OutlinedTextField(name,{name=it.take(24)},singleLine=true)},confirmButton={TextButton(onClick={vm.name(pad,name);rename=false}){Text("SALVAR")}},dismissButton={TextButton(onClick={rename=false}){Text("CANCELAR")}})
        }
    }
    Text("Deslize a mesa para acessar todos os canais. M = mute; S = solo. O master e a mixagem também são aplicados ao WAV e às pistas exportadas.",fontSize=12.sp,color=Color.Gray)
}

@Composable fun TrackOverview(s:StudioState,bar:Int,vm:StudioViewModel){
    Column(Modifier.fillMaxWidth().background(Color(0xFF17191F),RoundedCornerShape(8.dp)).padding(8.dp)){
        Text("COMPASSO ${bar+1} · TOQUE NA CÉLULA PARA EDITAR",color=Violet,fontSize=9.sp)
        for(pad in 0..15){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Text("${pad+1}",modifier=Modifier.width(24.dp).clickable{vm.select(pad)},color=if(s.pad==pad)Gold else Color.Gray,fontSize=10.sp)
                for(i in 0..15){
                    val step=bar*16+i;val active=pad in s.pattern.steps[step]
                    Box(Modifier.weight(1f).height(24.dp).padding(1.dp).background(if(active)Gold else Color(0xFF30333D),RoundedCornerShape(3.dp)).border(if(s.step==step)1.dp else 0.dp,Violet,RoundedCornerShape(3.dp)).clickable(enabled=!s.busy && !s.recordPads && !s.recording){vm.select(pad);vm.step(step)})
                }
            }
        }
    }
}
