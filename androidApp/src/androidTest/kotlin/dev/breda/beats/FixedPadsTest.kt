package dev.breda.beats

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FixedPadsTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun verify(orientation:Int,filename:String){
        compose.runOnUiThread{compose.activity.requestedOrientation=if(orientation==Configuration.ORIENTATION_PORTRAIT)ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}
        compose.waitUntil(15000){compose.activity.resources.configuration.orientation==orientation && compose.onAllNodesWithTag("fixed-pad-grid").fetchSemanticsNodes().isNotEmpty()}
        val root=compose.onRoot().fetchSemanticsNode().boundsInRoot
        val grid=compose.onNodeWithTag("fixed-pad-grid").fetchSemanticsNode()
        assertFalse(grid.config.contains(SemanticsProperties.VerticalScrollAxisRange))
        assertFalse(grid.config.contains(SemanticsProperties.HorizontalScrollAxisRange))
        for(pad in 1..16){
            val node=compose.onNodeWithTag("pad-$pad").assertIsDisplayed().fetchSemanticsNode()
            val bounds=node.boundsInRoot
            assertTrue("Pad $pad extends past screen",bounds.left>=root.left-.5f && bounds.top>=root.top-.5f && bounds.right<=root.right+.5f && bounds.bottom<=root.bottom+.5f)
        }
        val screenshot=requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        File(compose.activity.getExternalFilesDir(null),filename).outputStream().use{screenshot.compress(Bitmap.CompressFormat.PNG,100,it)}
        screenshot.recycle()
    }
    @Test fun sixteenPadsStayVisiblePortrait(){verify(Configuration.ORIENTATION_PORTRAIT,"ui-portrait.png")}
    @Test fun sixteenPadsStayVisibleLandscape(){verify(Configuration.ORIENTATION_LANDSCAPE,"ui-landscape.png")}
}
