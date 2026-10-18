package dev.breda.beats.data

import dev.breda.beats.core.*
import org.json.*
import java.io.*
import java.util.zip.*
import java.security.MessageDigest

object ProjectCodec {
    fun pattern(p:Pattern)=JSONObject().put("bpm",p.bpm).put("swing",p.swing.toDouble()).put("steps",JSONArray(p.steps.map{JSONArray(it.sorted())}))
    private fun readPattern(j:JSONObject):Pattern {val steps=j.getJSONArray("steps");require(steps.length() in 16..128);return Pattern(j.getInt("bpm"),j.getDouble("swing").toFloat(),List(steps.length()){i->val a=steps.getJSONArray(i);require(a.length()<=16);(0 until a.length()).map{a.getInt(it)}.toSet()})}
    fun encode(p:StudioProject,files:List<String>):JSONObject=JSONObject().put("schema",4).put("pattern",pattern(p.pattern)).put("sampleFiles",JSONArray(files)).put("title",p.title).put("master",p.master.toDouble()).put("names",JSONArray(p.names))
        .put("channels",JSONArray(p.channels.map{c->JSONObject().put("level",c.level.toDouble()).put("pan",c.pan.toDouble()).put("mute",c.mute).put("solo",c.solo).put("pitch",c.pitch).put("reverse",c.reverse).put("choke",c.choke).put("loop",c.loop).put("gate",c.gate)}))
        .put("fx",JSONObject().put("filter",p.fx.filter.toDouble()).put("drive",p.fx.drive.toDouble()).put("delay",p.fx.delay.toDouble()).put("mode",p.fx.mode.name).put("amount",p.fx.amount.toDouble()))
        .put("slots",JSONArray(p.slots.map{it?.let{v->pattern(v)}?:JSONObject.NULL})).put("slot",p.slot).put("arrangement",JSONArray(p.arrangement))
    fun decode(j:JSONObject,resolve:(String)->Sample):StudioProject {
        require(j.getInt("schema")==4);val refs=j.getJSONArray("sampleFiles");val channels=j.getJSONArray("channels");val names=j.getJSONArray("names");val slots=j.getJSONArray("slots");val order=j.getJSONArray("arrangement")
        require(refs.length()==16 && channels.length()==16 && names.length()==16 && slots.length()==8 && order.length() in 1..16)
        val fx=j.getJSONObject("fx")
        return StudioProject(List(16){resolve(refs.getString(it))},readPattern(j.getJSONObject("pattern")),List(16){i->val c=channels.getJSONObject(i);Channel(c.getDouble("level").toFloat(),c.getDouble("pan").toFloat(),c.getBoolean("mute"),c.getBoolean("solo"),c.getInt("pitch"),c.getBoolean("reverse"),c.getInt("choke"),c.getBoolean("loop"),c.getBoolean("gate"))},j.getString("title"),j.getDouble("master").toFloat(),List(16){names.getString(it)},FxSettings(fx.getDouble("filter").toFloat(),fx.getDouble("drive").toFloat(),fx.getDouble("delay").toFloat(),FxMode.valueOf(fx.optString("mode",FxMode.STUDIO.name)),fx.optDouble("amount",.55).toFloat()),List(8){if(slots.isNull(it))null else readPattern(slots.getJSONObject(it))},j.getInt("slot"),List(order.length()){order.getInt(it)})
    }
    fun hash(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
    fun validName(name:String)=name.matches(Regex("[a-f0-9]{64}\\.wav"))
}
object ProjectArchive {
    fun write(project:StudioProject,output:OutputStream){
        ZipOutputStream(output).use{zip->val files=mutableListOf<String>();val written=mutableSetOf<String>()
            for(sample in project.samples){val bytes=Wav.encode(sample);val name=ProjectCodec.hash(bytes)+".wav";files.add(name);if(written.add(name)){zip.putNextEntry(ZipEntry("samples/$name"));zip.write(bytes);zip.closeEntry()}}
            zip.putNextEntry(ZipEntry("manifest.json"));zip.write(ProjectCodec.encode(project,files).toString().toByteArray());zip.closeEntry()
        }
    }
    fun read(input:InputStream):StudioProject {
        val entries=mutableMapOf<String,ByteArray>();var total=0
        ZipInputStream(input).use{zip->while(true){val entry=zip.nextEntry?:break;val name=entry.name
            require(name=="manifest.json" || (name.startsWith("samples/") && ProjectCodec.validName(name.removePrefix("samples/")))){"Backup contém um caminho inválido"}
            require(entries.size<17 && !entries.containsKey(name)){"Backup contém entradas duplicadas ou extras"}
            val out=ByteArrayOutputStream();val buffer=ByteArray(8192)
            while(true){val n=zip.read(buffer);if(n<0)break;total+=n;require(total<=40_000_000 && out.size()+n<=if(name=="manifest.json")256_000 else 24_000_000){"Backup excede o limite"};out.write(buffer,0,n)}
            entries[name]=out.toByteArray();zip.closeEntry()
        }}
        val manifest=requireNotNull(entries["manifest.json"]){"Backup sem projeto"};val cache=mutableMapOf<String,Sample>()
        return ProjectCodec.decode(JSONObject(String(manifest))){name->require(ProjectCodec.validName(name));cache.getOrPut(name){val bytes=requireNotNull(entries["samples/$name"]){"Sample ausente no backup"};require(ProjectCodec.hash(bytes)+".wav"==name){"Sample corrompido"};Wav.decode(bytes)}}
    }
}
