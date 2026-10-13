package dev.breda.beats.core
import kotlin.test.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
class MixerTest {
 @Test fun undoRedoAndBranch(){val h=EditHistory();val a=StudioProject();val b=a.channel(0,Channel(.7f));h.committed(a);assertEquals(a,h.undoTarget());h.didUndo(b);assertEquals(b,h.redoTarget());h.didRedo(a);h.committed(b);assertFalse(h.canRedo)}
 @Test fun historyBound(){val h=EditHistory(2);val p=StudioProject();repeat(3){h.committed(p.copy(title="$it"))};h.didUndo(p);h.didUndo(p);assertFalse(h.canUndo)}
 @Test fun channelLevels(){val a=Channel(.7f).gains(false);val b=Channel().gains(false);assertEquals(.7f,a.first/b.first,.00001f)}
 @Test fun panMuteSolo(){assertEquals(0f,Channel(mute=true).gains(false).first);assertEquals(0f,Channel().gains(true).first);assertEquals(0f,Channel(pan=1f).gains(false).first,.00001f);assertEquals(1f,Channel(pan=1f).gains(false).second,.00001f)}
 @Test fun resizePreservesNotes(){val p=StudioProject().resize(4);assertEquals(64,p.pattern.steps.size);assertEquals(setOf(0),p.pattern.steps[0]);assertTrue(p.pattern.steps[63].isEmpty())}
 @Test fun eraseTrackOnly(){val p=StudioProject().clearTrack(0);assertTrue(p.pattern.steps[0].isEmpty());assertEquals(setOf(1),p.pattern.steps[4])}
 @Test fun silenceMutedExport(){val p=StudioProject(channels=List(16){Channel(mute=true)});assertTrue(MixRenderer.render(p).left.all{it==0f})}
 @Test fun stereoHeader(){val bytes=MixRenderer.render(StudioProject()).wav();assertEquals(2,ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getShort(22).toInt())}
 @Test fun stemsAddToMix(){val p=StudioProject();val mix=MixRenderer.render(p,.1f);val a=MixRenderer.render(p,.1f,0);val b=MixRenderer.render(p,.1f,1);for(i in mix.left.indices)assertEquals(mix.left[i],a.left[i]+b.left[i],.00001f)}
 @Test fun eightBarsRender(){val p=StudioProject().resize(8);val mix=MixRenderer.render(p);assertTrue(mix.left.size>441000)}
 @Test fun invalidChannel(){assertFailsWith<IllegalArgumentException>{Channel(Float.NaN)};assertFailsWith<IllegalArgumentException>{Channel(pan=2f)}}
}
