package dev.kinetik

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.kinetik.ui.components.ringBox
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The Fold's cover screen is only 360 dp wide: the ring must shrink as a circle, never squash into an oval. */
@RunWith(AndroidJUnit4::class)
class RingSizeTest {
    @get:Rule val rule = createComposeRule()

    @Test fun ringStaysSquareWhenNarrowerThanItsMax() {
        rule.setContent { Box(Modifier.width(320.dp)) { Box(Modifier.ringBox(340.dp).testTag("ring")) } }
        rule.onNodeWithTag("ring").assertWidthIsEqualTo(320.dp).assertHeightIsEqualTo(320.dp)
    }

    @Test fun ringStaysSquareWhenShorterThanItsMax() {
        rule.setContent { Box(Modifier.height(260.dp).width(600.dp)) { Box(Modifier.ringBox(300.dp).testTag("ring")) } }
        rule.onNodeWithTag("ring").assertWidthIsEqualTo(260.dp).assertHeightIsEqualTo(260.dp)
    }

    @Test fun ringUsesItsMaxWhenThereIsRoom() {
        rule.setContent { Box(Modifier.width(412.dp)) { Box(Modifier.ringBox(340.dp).testTag("ring")) } }
        rule.onNodeWithTag("ring").assertWidthIsEqualTo(340.dp).assertHeightIsEqualTo(340.dp)
    }
}
