package dev.kinetik.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.kinetik.ui.theme.K
import dev.kinetik.ui.theme.KShape
import dev.kinetik.ui.theme.KText
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors

@Composable
fun Label(text: String, modifier: Modifier = Modifier, color: Color = K.Muted) =
    Text(text.uppercase(), modifier, style = KText.label.copy(color = color))

@Composable
fun Tag(text: String, accent: Boolean = false, modifier: Modifier = Modifier) = Text(
    text,
    modifier
        .background(if (accent) K.Teal.copy(alpha = 0.14f) else K.Card2, KShape.Chip)
        .padding(horizontal = 9.dp, vertical = 4.dp),
    style = KText.body.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (accent) K.TealHi else K.Muted),
)

@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(K.Card)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(11.dp),
    ) {
        Text(value, style = KText.mono(19.sp).copy(color = K.TealHi))
        Label(label)
    }
}

/** Flat buttons: no glow on DONE or START. */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    height: Dp = 84.dp,
    textSize: TextUnit = 26.sp,
) {
    Box(
        modifier
            .height(height)
            .clip(KShape.Big)
            .background(if (primary) K.Teal else K.Card2)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 12.dp),
            style = KText.display(textSize, 900).copy(color = if (primary) K.OnTeal else K.Text, letterSpacing = 1.sp),
            maxLines = 1,
        )
    }
}

/** One bar per circuit: done = dim teal, current = teal with glow, upcoming = grey. */
@Composable
fun ProgressBars(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            val color = when {
                i < current -> K.TealDim
                i == current -> K.Teal
                else -> K.Card2
            }
            Box(
                Modifier
                    .size(26.dp, 6.dp)
                    .then(if (i == current) Modifier.drawBehind { drawRoundRect(K.Teal.copy(alpha = 0.35f), topLeft = Offset(-3f, -3f), size = size.copy(size.width + 6f, size.height + 6f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f)) } else Modifier)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = KText.body.copy(fontSize = 15.sp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = K.Teal, checkedThumbColor = K.OnTeal),
        )
    }
}

/** Diagonal teal stripes, used for overridden cells. */
fun Modifier.striped(): Modifier = drawBehind {
    drawRect(K.Teal.copy(alpha = 0.06f))
    val step = 16.dp.toPx()
    val w = 8.dp.toPx()
    clipRect {
        var x = -size.height
        while (x < size.width) {
            drawLine(K.Teal.copy(alpha = 0.18f), Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = w)
            x += step
        }
    }
}

/** Lays text out sideways (reading bottom to top) in a tall, narrow box. */
fun Modifier.verticalText(): Modifier = layout { measurable, constraints ->
    val p = measurable.measure(
        constraints.copy(
            minWidth = constraints.minHeight, maxWidth = constraints.maxHeight,
            minHeight = constraints.minWidth, maxHeight = constraints.maxWidth,
        ),
    )
    layout(p.height, p.width) { p.place((p.height - p.width) / 2, (p.width - p.height) / 2) }
}.graphicsLayer { rotationZ = -90f }

/** Text fields with grey labels and placeholders, teal when focused. */
@Composable
fun kFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = K.Teal,
    unfocusedBorderColor = K.Line,
    focusedLabelColor = K.TealHi,
    unfocusedLabelColor = K.Muted,
    focusedPlaceholderColor = K.Muted,
    unfocusedPlaceholderColor = K.Muted,
    cursorColor = K.Teal,
)
