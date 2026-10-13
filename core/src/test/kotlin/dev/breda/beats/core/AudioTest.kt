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
}
