package dev.breda.beats.core
import org.junit.Test
import org.junit.Assert.*
class ProductionTest {
 private fun project(sample:Sample=Sample(floatArrayOf(.2f,.4f,.6f,.8f)))=StudioProject(samples=List(16){sample},pattern=Pattern().toggle(0,0),master=1f)
 @Test fun reverseAndPitchAreNonDestructive(){val p=project();val reversed=MixRenderer.render(p.channel(0,Channel(reverse=true)));assertEquals(.8f*.70710677f,reversed.left[0],.00001f);val pitched=MixRenderer.render(p.channel(0,Channel(pitch=12)));assertEquals(.6f*.70710677f,pitched.left[1],.00001f);assertEquals(0f,pitched.left[2],0f);assertEquals(.2f,p.samples[0].pcm[0],0f)}
 @Test fun loopRunsUntilNextGroupTrigger(){val p=project(Sample(FloatArray(100){.2f})).channel(0,Channel(loop=true,choke=1)).channel(1,Channel(choke=1));val pattern=p.pattern.toggle(4,1);val result=MixRenderer.render(p.copy(pattern=pattern),onlyPad=0,applyFx=false);val frame=(60.0/p.pattern.bpm*44100).toInt();assertTrue(result.left[1000]>0);assertEquals(0f,result.left[frame+1],0f)}
 @Test fun sequencesRetainNotesAndGlobalTempo(){val p=project().switchSequence(1);assertTrue(p.pattern.steps.all{it.isEmpty()});assertEquals(setOf(0),p.sequence(0).steps[0]);val q=p.copy(pattern=p.pattern.toggle(2,3).copy(bpm=100)).duplicateSequence(2);assertEquals(100,q.sequence(0).bpm);assertEquals(setOf(3),q.switchSequence(2).pattern.steps[2])}
 @Test fun copyAndSwapIncludeSettingsButNotNotes(){val p=project().channel(0,Channel(.7f,pitch=3));val copied=p.copyPad(0,3);assertSame(p.samples[0],copied.samples[3]);assertEquals(3,copied.channels[3].pitch);assertEquals(p.pattern,copied.pattern);assertEquals(p.names[3],p.copyPad(0,3,true).names[0])}
 @Test fun delayIsTimedAndClearRemovesTail(){val fx=FxProcessor();fx.configure(FxSettings(delay=1f),120);fx.process(.5f,0f);repeat(11024){fx.process(0f,0f)};fx.process(0f,0f);assertEquals(.5f,fx.left,.00001f);fx.clear();repeat(11026){fx.process(0f,0f);assertEquals(0f,fx.left,0f)}}
 @Test fun effectBypassMatchesDryMix(){val p=project();assertArrayEquals(MixRenderer.render(p,applyFx=false).left,MixRenderer.render(p).left,0f)}
 @Test fun deletingPadPreservesOtherSamples(){val p=project();val empty=p.deletePad(0);assertEquals("VAZIO",empty.names[0]);assertEquals(0f,empty.samples[0].pcm[0],0f);assertSame(p.samples[1],empty.samples[1])}
 @Test(expected=IllegalArgumentException::class) fun arrangementBoundsAreValidated(){project().copy(arrangement=List(17){0})}
 @Test(expected=IllegalArgumentException::class) fun invalidPitchIsRejected(){Channel(pitch=25)}
}
