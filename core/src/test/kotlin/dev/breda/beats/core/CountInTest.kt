package dev.breda.beats.core
import kotlin.test.*
class CountInTest {
    @Test fun secondsAreExactlyThreeAtAnyBpm(){for(bpm in listOf(40,88,120,200)){val ticks=CountIn.ticks(CountInMode.THREE_SECONDS,bpm);assertEquals(listOf(3,2,1),ticks.map{it.number});assertEquals(3000,ticks.sumOf{it.waitMillis}.toInt())}}
    @Test fun barFollowsTempo(){assertEquals(2000,CountIn.ticks(CountInMode.ONE_BAR,120).sumOf{it.waitMillis}.toInt());assertEquals(6000,CountIn.ticks(CountInMode.ONE_BAR,40).sumOf{it.waitMillis}.toInt())}
    @Test fun roundingDoesNotAccumulate(){val ticks=CountIn.ticks(CountInMode.ONE_BAR,88);assertEquals(2727,ticks.sumOf{it.waitMillis}.toInt());assertTrue(ticks.first().accent);assertTrue(ticks.drop(1).none{it.accent})}
    @Test fun noCountInIsImmediate(){assertTrue(CountIn.ticks(CountInMode.NONE,88).isEmpty())}
    @Test fun clickIsBoundedAndOriginal(){val a=ClickSound.pcm(true);val b=ClickSound.pcm(false);assertEquals(1103,a.size);assertTrue(a.any{it!=0f});assertTrue(a.all{it in -.35f.. .35f});assertFalse(a.contentEquals(b))}
}
