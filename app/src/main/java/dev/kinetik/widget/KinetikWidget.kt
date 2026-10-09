package dev.kinetik.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.kinetik.MainActivity
import dev.kinetik.app
import dev.kinetik.session.Control
import dev.kinetik.ui.theme.K
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KinetikWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = KinetikWidget()
}

class KinetikWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.app
        provideContent {
            val s by app.session.state.collectAsState()
            val lib by app.store.library.collectAsState()
            WidgetContent(widgetModel(s, lib))
        }
    }
}

private val ControlKey = ActionParameters.Key<String>("control")

/** Done / Pause / Resume / Skip from the widget go straight to the running session. */
class ControlAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val control = parameters[ControlKey]?.let { name -> Control.entries.firstOrNull { it.name == name } } ?: return
        withContext(Dispatchers.Main) { context.app.session.send(control.event) }
    }
}

private fun color(c: Color) = ColorProvider(c)

@Composable
private fun WidgetContent(m: WidgetModel) {
    val context = LocalContext.current
    Column(
        GlanceModifier.fillMaxSize().cornerRadius(26.dp).background(color(K.Card)).padding(16.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
    ) {
        Text(m.headline.uppercase(), style = TextStyle(color = color(K.Muted), fontSize = 11.sp, fontWeight = FontWeight.Bold), maxLines = 1)
        Spacer(GlanceModifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (m.big.isNotEmpty()) {
                Text(m.big, style = TextStyle(color = color(if (m.isRest) K.Rest else K.TealHi), fontSize = 34.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.width(12.dp))
            }
            Column {
                Text(m.title, style = TextStyle(color = color(K.Text), fontSize = 18.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                if (m.sub.isNotEmpty()) Text(m.sub, style = TextStyle(color = color(K.Muted), fontSize = 12.sp), maxLines = 1)
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        Row(GlanceModifier.fillMaxWidth()) {
            m.startWorkoutId?.let { id ->
                WidgetButton(
                    "▶ Start", primary = true, modifier = GlanceModifier.defaultWeight(),
                    action = actionStartActivity(Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_START, id)),
                )
            }
            m.controls.forEachIndexed { i, c ->
                if (i > 0) Spacer(GlanceModifier.width(8.dp))
                WidgetButton(
                    c.label, primary = c == Control.DONE, modifier = GlanceModifier.defaultWeight(),
                    action = actionRunCallback<ControlAction>(actionParametersOf(ControlKey to c.name)),
                )
            }
        }
    }
}

@Composable
private fun WidgetButton(label: String, primary: Boolean, modifier: GlanceModifier, action: Action) {
    Box(
        modifier.height(44.dp).cornerRadius(12.dp).background(color(if (primary) K.Teal else K.Card2)).clickable(action),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = TextStyle(color = color(if (primary) K.OnTeal else K.Text), fontSize = 14.sp, fontWeight = FontWeight.Bold))
    }
}
