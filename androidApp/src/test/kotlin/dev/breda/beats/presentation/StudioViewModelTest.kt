package dev.breda.beats.presentation
import androidx.lifecycle.ViewModelStore
import dev.breda.beats.core.*
import dev.breda.beats.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StudioViewModelTest {
    private class Repository:StudioRepository {
        var project=StudioProject(pattern=Pattern())
        var fail=false
        override fun load()=project
        override fun save(project:StudioProject){check(!fail){"disk full"};this.project=project}
    }
    private class Engine:AudioEngine {
        override val songIndex=0
        override fun effects(settings:FxSettings,bpm:Int){}
        override fun song(patterns:List<Pattern>){}
        override val step=0
        override val quantizedStep=2
        override val meters=List(16){0f}
        override val clipped=false
        override fun samples(value:List<Sample>){}
        override fun mix(channels:List<Channel>){}
        override fun hit(pad:Int,loop:Boolean,gate:Boolean){}
        override fun release(pad:Int){}
        override fun transport(pattern:Pattern?){}
        override fun updatePattern(pattern:Pattern){}
        override fun stopAll(){}
        override fun volume(v:Float){}
        override fun click(accent:Boolean){}
        override fun metronome(enabled:Boolean,bpm:Int,standalone:Boolean){}
        override fun close(){}
    }
    private fun scenario(body:TestScope.(StudioViewModel,Repository)->Unit)=runTest {
        val dispatcher=StandardTestDispatcher(testScheduler);Dispatchers.setMain(dispatcher)
        val store=ViewModelStore()
        try{
            val repository=Repository();val vm=StudioViewModel(repository,{Engine()},dispatcher,dispatcher)
            store.put("studio",vm);runCurrent();assertFalse(vm.state.value.busy)
            body(vm,repository)
        } finally {store.clear();Dispatchers.resetMain()}
    }
    @Test fun undoRedoRestoresIndependentVolumes()=scenario {vm,repo->
        vm.channel(0,Channel(.7f));runCurrent();assertEquals(.7f,repo.project.channels[0].level,0f);assertEquals(1f,repo.project.channels[1].level,0f)
        vm.undo();runCurrent();assertEquals(1f,repo.project.channels[0].level,0f)
        vm.redo();runCurrent();assertEquals(.7f,repo.project.channels[0].level,0f)
    }
    @Test fun failedSaveDoesNotCreateUndo()=scenario {vm,repo->
        repo.fail=true;vm.channel(0,Channel(.2f));runCurrent()
        assertEquals(1f,vm.state.value.project.channels[0].level,0f);assertFalse(vm.state.value.canUndo);assertNotNull(vm.state.value.error)
    }
    @Test fun microphoneIsOffAndCannotRecordByDefault()=scenario {vm,_->
        assertFalse(vm.state.value.micEnabled);vm.record();runCurrent();assertFalse(vm.state.value.recording)
        vm.enableMic();assertTrue(vm.state.value.micEnabled);assertFalse(vm.state.value.recording)
        vm.enableMic();assertFalse(vm.state.value.micEnabled)
    }
    @Test fun padOverdubIsOneUndoableTakeWithoutMicrophone()=scenario {vm,repo->
        vm.recordPads();advanceTimeBy(3000);runCurrent();vm.hit(0);vm.hit(1);assertFalse(vm.state.value.recording);assertFalse(vm.state.value.micEnabled)
        vm.stop();runCurrent();assertEquals(setOf(0,1),repo.project.pattern.steps[2]);assertTrue(vm.state.value.canUndo)
        vm.undo();runCurrent();assertTrue(repo.project.pattern.steps[2].isEmpty())
    }
    @Test fun padsDoNotRecordBeforeThreeSeconds()=scenario {vm,repo->
        vm.recordPads();runCurrent();assertEquals(3,vm.state.value.countdown);vm.hit(0)
        advanceTimeBy(1000);runCurrent();assertEquals(2,vm.state.value.countdown);assertFalse(vm.state.value.recordPads)
        advanceTimeBy(1999);runCurrent();assertFalse(vm.state.value.recordPads)
        advanceTimeBy(1);runCurrent();assertTrue(vm.state.value.recordPads);assertEquals(0,vm.state.value.countdown)
        assertTrue(repo.project.pattern.steps.all{it.isEmpty()})
    }
    @Test fun stopCancelsWithoutLaterRecording()=scenario {vm,_->
        vm.recordPads();runCurrent();advanceTimeBy(1000);vm.stop();runCurrent();advanceTimeBy(4000);runCurrent()
        assertFalse(vm.state.value.recordPads);assertFalse(vm.state.value.playing);assertEquals(0,vm.state.value.countdown)
    }
    @Test fun countInBarAndNoneAreSelectable()=scenario {vm,_->
        vm.countIn(CountInMode.NONE);vm.recordPads();assertTrue(vm.state.value.recordPads);vm.stop();runCurrent()
        vm.countIn(CountInMode.ONE_BAR);vm.recordPads();runCurrent();assertEquals(4,vm.state.value.countdown);assertFalse(vm.state.value.recordPads)
    }
    @Test fun micOffDuringPreparationNeverStartsCapture()=scenario {vm,_->
        vm.enableMic();vm.record();runCurrent();assertTrue(vm.state.value.countdownMic);assertFalse(vm.state.value.recording)
        advanceTimeBy(1000);vm.enableMic();runCurrent();advanceTimeBy(4000);runCurrent()
        assertFalse(vm.state.value.micEnabled);assertFalse(vm.state.value.recording);assertEquals(0,vm.state.value.countdown);assertNull(vm.state.value.pendingTake);assertNull(vm.state.value.error)
    }
}
