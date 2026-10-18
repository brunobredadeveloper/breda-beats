package dev.breda.beats

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.breda.beats.data.StudioAppearance

data class StudioColors(
    val background:Color,val panel:Color,val raised:Color,val inset:Color,
    val text:Color,val muted:Color,val outline:Color,val accent:Color,
    val accentText:Color,val onAccent:Color,val secondary:Color
)

private fun hexColor(hex:String)=Color(0xFF000000 or hex.toLong(16))
private fun contrast(a:Color,b:Color):Float {
    val x=a.luminance();val y=b.luminance()
    return (maxOf(x,y)+.05f)/(minOf(x,y)+.05f)
}
private fun readable(color:Color,background:Color):Color {
    if(contrast(color,background)>=4.5f)return color
    val target=if(background.luminance()>.4f)Color.Black else Color.White
    return (1..20).map{lerp(color,target,it/20f)}.firstOrNull{contrast(it,background)>=4.5f}?:target
}
private fun palette(appearance:StudioAppearance):StudioColors {
    val bg=hexColor(appearance.backgroundHex);val accent=hexColor(appearance.accentHex)
    val light=bg.luminance()>.4f
    val ink=if(light)Color(0xFF1A1C22) else Color(0xFFF4F1E8)
    val panel=lerp(bg,if(light)Color.Black else Color.White,.045f)
    val raised=lerp(bg,ink,.08f)
    return StudioColors(bg,panel,raised,lerp(bg,if(light)Color.White else Color.Black,.2f),
        ink,lerp(bg,ink,.72f),lerp(bg,ink,.3f),accent,readable(accent,panel),
        if(contrast(Color.Black,accent)>contrast(Color.White,accent))Color.Black else Color.White,
        readable(Color(0xFFB197FC),panel))
}

val LocalStudioColors=staticCompositionLocalOf { palette(StudioAppearance()) }

@Composable fun StudioTheme(appearance:StudioAppearance,content:@Composable ()->Unit) {
    val colors=remember(appearance){palette(appearance)}
    val scheme=if(colors.background.luminance()>.4f)lightColorScheme() else darkColorScheme()
    CompositionLocalProvider(LocalStudioColors provides colors) {
        MaterialTheme(colorScheme=scheme.copy(
            primary=colors.accentText,onPrimary=if(contrast(Color.Black,colors.accentText)>contrast(Color.White,colors.accentText))Color.Black else Color.White,secondary=colors.secondary,
            onSecondary=if(colors.background.luminance()>.4f)Color.White else Color.Black,
            background=colors.background,onBackground=colors.text,surface=colors.panel,onSurface=colors.text,
            surfaceVariant=colors.raised,onSurfaceVariant=colors.muted,outline=colors.outline,
            surfaceContainer=colors.panel,surfaceContainerHigh=colors.raised,surfaceContainerHighest=colors.raised,
            surfaceContainerLow=colors.panel,surfaceContainerLowest=colors.inset,
            secondaryContainer=lerp(colors.panel,colors.secondary,.2f),onSecondaryContainer=colors.text
        ),content=content)
    }
}

private val BackgroundOptions=listOf("Carvão" to "101115","Grafite" to "30343B","Oceano" to "102B40","Ameixa" to "302038","Floresta" to "15342F","Claro" to "F0EEE7")
private val AccentOptions=listOf("Âmbar" to "F6B44E","Coral" to "FF819D","Violeta" to "B197FC","Azul" to "75B8FF","Menta" to "6EF5D0","Lima" to "C7EF72","Laranja" to "FF9B62","Branco" to "F4F1E8")

@Composable fun AppearancePanel(value:StudioAppearance,onChange:(StudioAppearance)->Unit) {
    val colors=LocalStudioColors.current
    var background by remember(value.backgroundHex){mutableStateOf(value.backgroundHex)}
    var accent by remember(value.accentHex){mutableStateOf(value.accentHex)}
    val valid=StudioAppearance.validHex(background) && StudioAppearance.validHex(accent)
    Card(Modifier.fillMaxWidth().testTag("appearance-settings").semantics{stateDescription="Fundo #${value.backgroundHex}; controles #${value.accentHex}"}) {
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("CORES DO ESTÚDIO",fontWeight=FontWeight.Bold,color=colors.accentText)
            Text("Escolha o fundo e os controles. Sua aparência é mantida ao reabrir o app.",fontSize=11.sp,color=colors.muted)
            ColorOptions("FUNDO",BackgroundOptions,value.backgroundHex,"background"){onChange(value.copy(backgroundHex=it))}
            ColorOptions("CONTROLES",AccentOptions,value.accentHex,"accent"){onChange(value.copy(accentHex=it))}
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(background,{background=it.removePrefix("#").take(6).uppercase()},Modifier.weight(1f).testTag("custom-background"),label={Text("Fundo · HEX",fontSize=11.sp)},prefix={Text("#")},singleLine=true,isError=!StudioAppearance.validHex(background))
                OutlinedTextField(accent,{accent=it.removePrefix("#").take(6).uppercase()},Modifier.weight(1f).testTag("custom-accent"),label={Text("Controles · HEX",fontSize=11.sp)},prefix={Text("#")},singleLine=true,isError=!StudioAppearance.validHex(accent))
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                TextButton(onClick={onChange(StudioAppearance())},modifier=Modifier.testTag("appearance-reset")){Text("RESTAURAR",fontSize=11.sp)}
                Button(onClick={onChange(StudioAppearance(background,accent))},enabled=valid,modifier=Modifier.testTag("appearance-apply")){Text("APLICAR CORES",fontSize=11.sp)}
            }
        }
    }
}

@Composable private fun ColorOptions(title:String,options:List<Pair<String,String>>,selected:String,tag:String,onSelect:(String)->Unit) {
    val colors=LocalStudioColors.current
    Text(title,fontSize=10.sp,color=colors.muted)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        options.forEach{(name,hex)->
            Column(Modifier.width(48.dp).selectable(selected.equals(hex,true),onClick={onSelect(hex)})
                .testTag("$tag-$hex").semantics{contentDescription="$title $name"}.padding(vertical=4.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                Box(Modifier.size(27.dp).background(hexColor(hex),CircleShape).border(if(selected.equals(hex,true))3.dp else 1.dp,if(selected.equals(hex,true))colors.text else colors.outline,CircleShape))
                Text(name,fontSize=8.sp,color=colors.text,maxLines=1)
            }
        }
    }
}
