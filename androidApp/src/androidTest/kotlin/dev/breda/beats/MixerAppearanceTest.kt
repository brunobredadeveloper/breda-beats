package dev.breda.beats

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsActions
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
class MixerAppearanceTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun ready(landscape:Boolean=false) {
        compose.runOnUiThread{compose.activity.requestedOrientation=if(landscape)ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}
        compose.waitUntil(15000){compose.activity.resources.configuration.orientation==(if(landscape)Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT) && compose.onAllNodesWithTag("nav-mixer").fetchSemanticsNodes().isNotEmpty()}
    }
    private fun open() {
        compose.onNodeWithTag("nav-mixer").performClick()
        compose.onNodeWithTag("mixer-channel-16").assertIsDisplayed()
    }
    private fun capture(name:String) {
        val screenshot=requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        File(compose.activity.getExternalFilesDir(null),name).outputStream().use{screenshot.compress(Bitmap.CompressFormat.PNG,100,it)}
        screenshot.recycle()
    }
    private fun verifyChannels() {
        val root=compose.onRoot().fetchSemanticsNode().boundsInRoot
        for(pad in 1..16) {
            val bounds=compose.onNodeWithTag("mixer-fader-$pad").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue("Canal $pad fora da tela",bounds.left>=root.left && bounds.right<=root.right && bounds.top>=root.top && bounds.bottom<=root.bottom)
            assertTrue("Fader $pad sem espaço para ajustar",bounds.height>20f)
        }
    }
    @Test fun sixteenChannelsFitPortraitAndTransportRemainsInteractive() {
        ready();open();verifyChannels()
        compose.onNodeWithTag("mixer-fader-16").performTouchInput{swipe(bottomCenter,center,300)}
        compose.onNodeWithTag("mixer-channel-16").assertIsDisplayed()
        val value=compose.onNodeWithTag("mixer-fader-16").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current
        assertTrue("Arrastar o fader deve alterar o canal sem recolher a mesa",value>=.25f && value<=.75f)
        compose.onNodeWithText("CTRL 1").assertIsDisplayed()
        compose.waitUntil(15000){!compose.onNodeWithText("PLAY").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)}
        compose.onNodeWithText("PLAY").performClick()
        compose.onNodeWithText("PAUSE").assertIsDisplayed()
        capture("ui-mixer-portrait.png")
        compose.onNodeWithText("STOP").performClick()
    }
    @Test fun sixteenChannelsFitLandscape() {
        ready(true)
        try { open();capture("ui-mixer-landscape.png");verifyChannels() }
        finally { compose.runOnUiThread{compose.activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT} }
    }
    @Test fun mixerOverlaysCurrentPageAndChannelsRemainIndependent() {
        ready();compose.onNodeWithTag("nav-pistas").performClick();open()
        val untouched=compose.onNodeWithTag("mixer-fader-14").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        compose.onNodeWithTag("mixer-fader-13").performSemanticsAction(SemanticsActions.SetProgress){it(.35f)}
        compose.onNodeWithTag("mixer-fader-13").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"35%"))
        compose.onNodeWithTag("mixer-fader-14").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,untouched))
        compose.onNodeWithTag("mixer-selected-name").assertTextContains("CH 13",substring=true)
        compose.waitUntil(15000){!compose.onNodeWithTag("mixer-mute").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)}
        compose.onNodeWithTag("mixer-mute").performClick()
        compose.waitUntil(15000){compose.onNodeWithTag("mixer-mute").fetchSemanticsNode().config[SemanticsProperties.StateDescription]=="Ligado"}
        compose.onNodeWithTag("mixer-mute").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"Ligado"))
        compose.waitUntil(15000){!compose.onNodeWithTag("mixer-mute").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)}
        compose.onNodeWithTag("mixer-mute").performClick()
        compose.onNodeWithTag("mixer-close").performClick()
        compose.onNodeWithText("SEQUENCIADOR").assertIsDisplayed()
        open()
        compose.runOnUiThread{compose.activity.onBackPressedDispatcher.onBackPressed()}
        compose.onNodeWithText("SEQUENCIADOR").assertIsDisplayed()
    }
    @Test fun appearanceSupportsIndependentColorsHexLightAndRecreation() {
        ready();compose.onNodeWithTag("nav-projeto").performClick()
        try {
            compose.onNodeWithTag("background-102B40").performClick()
            compose.onNodeWithTag("accent-6EF5D0").performClick()
            compose.onNodeWithTag("appearance-settings").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"Fundo #102B40; controles #6EF5D0"))
            capture("ui-appearance.png")
            compose.activityRule.scenario.recreate()
            compose.waitUntil(15000){compose.onAllNodesWithTag("appearance-settings").fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithTag("appearance-settings").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"Fundo #102B40; controles #6EF5D0"))
            compose.onNodeWithTag("custom-accent").performTextReplacement("ZZZZZZ")
            compose.onNodeWithTag("appearance-apply").assertIsNotEnabled()
            compose.onNodeWithTag("custom-accent").performTextReplacement("82E0FF")
            compose.onNodeWithTag("appearance-apply").performClick()
            compose.onNodeWithTag("background-F0EEE7").performClick()
            capture("ui-appearance-light.png")
        } finally {
            compose.onNodeWithTag("appearance-reset").performClick()
        }
    }
}
