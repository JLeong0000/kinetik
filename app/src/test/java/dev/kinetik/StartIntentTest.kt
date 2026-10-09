package dev.kinetik

import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Review 2: a widget Start must not replay when Android recreates the activity from its original launch intent. */
class StartIntentTest {
    @Test fun freshLaunchStarts() = assertTrue(shouldStartFromIntent(restored = false, flags = 0))

    @Test fun recreatedActivityDoesNotStart() = assertFalse(shouldStartFromIntent(restored = true, flags = 0))

    @Test fun openedFromRecentsDoesNotStart() =
        assertFalse(shouldStartFromIntent(restored = false, flags = Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY))
}
