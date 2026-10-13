package dev.breda.beats.data
import dev.breda.beats.core.*
import org.junit.Test
import org.junit.Assert.*
import java.io.*
import java.util.zip.*
class ProjectArchiveTest {
 @Test fun backupRestoresSamplesSequencesAndMix(){val p=StudioProject().channel(0,Channel(.7f,pitch=-5,reverse=true,choke=2)).copy(fx=FxSettings(.6f,.2f,.3f)).duplicateSequence(2).copy(arrangement=listOf(0,2,0));val out=ByteArrayOutputStream();ProjectArchive.write(p,out);val restored=ProjectArchive.read(ByteArrayInputStream(out.toByteArray()));assertEquals(p.channels,restored.channels);assertEquals(p.fx,restored.fx);assertEquals(p.arrangement,restored.arrangement);assertEquals(p.sequence(2),restored.sequence(2));assertEquals(p.samples[0].pcm[20],restored.samples[0].pcm[20],.0001f)}
 @Test(expected=IllegalArgumentException::class) fun archiveRejectsTraversal(){val out=ByteArrayOutputStream();ZipOutputStream(out).use{it.putNextEntry(ZipEntry("../evil"));it.write(byteArrayOf(1));it.closeEntry()};ProjectArchive.read(ByteArrayInputStream(out.toByteArray()))}
 @Test(expected=IllegalArgumentException::class) fun codecRejectsMissingPads(){val p=StudioProject();val j=ProjectCodec.encode(p,List(16){"a".repeat(64)+".wav"});j.put("channels",org.json.JSONArray());ProjectCodec.decode(j){p.samples[0]}}
}
