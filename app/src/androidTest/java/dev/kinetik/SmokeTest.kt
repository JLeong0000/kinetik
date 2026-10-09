package dev.kinetik

import android.Manifest
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmokeTest {
    @get:Rule(order = 0) val notifications: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
    @get:Rule(order = 1) val rule = createAndroidComposeRule<MainActivity>()

    @After fun stopSession() = rule.runOnUiThread { rule.activity.app.session.stop() }

    @Test fun startThenDoneStartsARest() {
        rule.onAllNodesWithTag("start").onFirst().performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("done").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("done").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("REST").fetchSemanticsNodes().isNotEmpty() }
    }
}
