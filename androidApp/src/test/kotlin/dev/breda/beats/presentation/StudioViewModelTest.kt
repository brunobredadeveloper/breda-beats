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
        vm.recordPads();vm.hit(0);vm.hit(1);assertFalse(vm.state.value.recording);assertFalse(vm.state.value.micEnabled)
        vm.stop();runCurrent();assertEquals(setOf(0,1),repo.project.pattern.steps[2]);assertTrue(vm.state.value.canUndo)
        vm.undo();runCurrent();assertTrue(repo.project.pattern.steps[2].isEmpty())
    }
}
