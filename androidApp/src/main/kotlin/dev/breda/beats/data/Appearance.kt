package dev.breda.beats.data

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File

data class StudioAppearance(val backgroundHex:String="101115",val accentHex:String="F6B44E") {
    init { require(validHex(backgroundHex) && validHex(accentHex)) { "Use uma cor com seis dígitos hexadecimais" } }
    companion object { fun validHex(value:String)=value.matches(Regex("[0-9a-fA-F]{6}")) }
}

interface AppearanceRepository {
    fun load():StudioAppearance
    fun save(value:StudioAppearance)
}

object AppearanceCodec {
    fun encode(value:StudioAppearance)=JSONObject().put("schema",1)
        .put("background",value.backgroundHex.uppercase()).put("accent",value.accentHex.uppercase()).toString()
    fun decode(text:String):StudioAppearance {
        val json=JSONObject(text)
        require(json.getInt("schema")==1) { "Versão de aparência não suportada" }
        return StudioAppearance(json.getString("background"),json.getString("accent"))
    }
}

class LocalAppearanceRepository(context:Context):AppearanceRepository {
    private val file=AtomicFile(File(context.filesDir,"appearance.json"))
    override fun load()=if(file.baseFile.exists())AppearanceCodec.decode(String(file.readFully())) else StudioAppearance()
    override fun save(value:StudioAppearance) {
        val stream=file.startWrite()
        try { stream.write(AppearanceCodec.encode(value).toByteArray());file.finishWrite(stream) }
        catch(e:Exception) { file.failWrite(stream);throw e }
    }
}
