package dev.kinetik

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.kinetik.model.Exercise
import dev.kinetik.model.Workout
import dev.kinetik.session.SessionState
import dev.kinetik.ui.components.ringBox
import dev.kinetik.ui.live.RingBlock
import dev.kinetik.ui.live.liveModel
import dev.kinetik.ui.theme.KinetikTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.hypot

/** Long exercise names must stay inside the ring, never on or over it. */
@RunWith(AndroidJUnit4::class)
class RingTextFitTest {
    @get:Rule val rule = createComposeRule()

    private val longName = "Alternating chin up and pull up negatives with pause"

    private fun DpRect.cornersInsideCircle(ring: DpRect, stroke: Dp): Boolean {
        val cx = (ring.left + ring.right).value / 2
        val cy = (ring.top + ring.bottom).value / 2
        val inner = (ring.right - ring.left).value / 2 - stroke.value * 2
        return listOf(left to top, right to top, left to bottom, right to bottom)
            .all { (x, y) -> hypot(x.value - cx, y.value - cy) <= inner }
    }

    @Test fun longNameFitsInsideTheRingOnTheCoverScreen() {
        val w = Workout(
            name = "W", circuits = 1, repDrop = 0, restExerciseSec = 60, restCircuitSec = 60,
            exercises = listOf(Exercise(name = longName, startReps = 12)),
        )
        val m = liveModel(SessionState.start(w))
        rule.setContent {
            KinetikTheme {
                Box(Modifier.width(320.dp)) {
                    RingBlock(m, Modifier.ringBox(340.dp).testTag("ring"), stroke = 18.dp, bigSize = 116.sp, nameSize = 24.sp)
                }
            }
        }
        val ring = rule.onNodeWithTag("ring").getBoundsInRoot()
        val name = rule.onNodeWithText(longName).getBoundsInRoot()
        val reps = rule.onNodeWithText("12").getBoundsInRoot()
        assertTrue("name $name pokes out of ring $ring", name.cornersInsideCircle(ring, 18.dp))
        assertTrue("reps $reps pokes out of ring $ring", reps.cornersInsideCircle(ring, 18.dp))
    }
}
