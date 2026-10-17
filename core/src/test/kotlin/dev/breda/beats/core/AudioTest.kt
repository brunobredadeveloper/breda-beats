package dev.breda.beats.core
import kotlin.test.*
class AudioTest {
 @Test fun wavRoundTrip(){val s=Kit.create(0);val d=Wav.decode(Wav.encode(s));assertEquals(s.pcm.size,d.pcm.size);assertTrue(kotlin.math.abs(s.pcm[100]-d.pcm[100])<.001)}
 @Test fun rejectMalformed(){assertFailsWith<IllegalArgumentException>{Wav.decode(ByteArray(44))}}
 @Test fun swingPreservesBar(){val p=Pattern(swing=.3f);assertEquals(60.0/88*4,(0..15).sumOf{p.stepSeconds(it)},.00001)}
 @Test fun toggleAndRender(){val p=Pattern().toggle(0,0);assertTrue(0 in p.steps[0]);assertTrue(p.toggle(0,0).steps[0].isEmpty());assertTrue(p.render(List(16){Kit.create(it)}).pcm.any{it!=0f})}
 @Test fun trimAndReverse(){val s=Sample(FloatArray(1000){it/1000f});assertEquals(500,s.edit(0f,.5f,true,1f,0f).pcm.size)}
 @Test fun rejectEmptyTrim(){assertFailsWith<IllegalArgumentException>{Kit.create(0).edit(1f,1f,false,1f,0f)}}
 @Test fun rejectInvalidPattern(){assertFailsWith<IllegalArgumentException>{Pattern(bpm=0)}}

 @Test fun defaultFxRemainsTransparent(){val fx=FxProcessor();fx.configure(FxSettings(),88);fx.process(.25f,-.5f);assertEquals(.25f,fx.left,.0001f);assertEquals(-.5f,fx.right,.0001f)}
 @Test fun roomReverbCreatesATail(){val fx=FxProcessor(8000);fx.configure(FxSettings(mode=FxMode.ROOM_REVERB,amount=1f),120);fx.process(1f,1f);var tail=0f;repeat(1200){fx.process(0f,0f);tail=maxOf(tail,kotlin.math.abs(fx.left))};assertTrue(tail>.0001f)}
 @Test fun beatRepeatLoopsAtTempo(){val fx=FxProcessor(8000);fx.configure(FxSettings(mode=FxMode.BEAT_REPEAT,amount=1f),120);repeat(1000){fx.process(.25f,.25f)};fx.process(0f,0f);assertEquals(.25f,fx.left,.0001f);assertEquals(.25f,fx.right,.0001f)}
 @Test fun loFiReducesResolution(){val fx=FxProcessor(8000);fx.configure(FxSettings(mode=FxMode.LO_FI,amount=1f),120);fx.process(.12345f,-.12345f);assertTrue(kotlin.math.abs(fx.left-.12345f)>.001f)}
 @Test fun rejectInvalidEffectAmount(){assertFailsWith<IllegalArgumentException>{FxSettings(amount=Float.NaN)}}
}
