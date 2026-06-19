@file:Suppress("DEPRECATION")

package com.genzxid.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

// ── Brand palette ─────────────────────────────────────────
val darkPurple = Color(0xFF7C3AED)
val lightPurple = Color(0xFFA855F7)
val darkCyan = Color(0xFF06B6D4)
val lightCyan = Color(0xFF22D3EE)
val gradientBrush = Brush.horizontalGradient(listOf(darkPurple, darkCyan))

// Animated border gradient colors
val gradientPurple = Color(0xFF7C3AED)
val gradientViolet = Color(0xFFA855F7)
val gradientMagenta = Color(0xFF06B6D4)

// ── Aurora palette ────────────────────────────────────────
// Flowing aurora-borealis accent colors layered behind the UI. Tuned to read
// well over both the dark and light base backgrounds.
val auroraViolet = Color(0xFF8B5CF6)
val auroraIndigo = Color(0xFF6366F1)
val auroraCyan = Color(0xFF22D3EE)
val auroraTeal = Color(0xFF2DD4BF)
val auroraMagenta = Color(0xFFD946EF)
val auroraBlue = Color(0xFF3B82F6)

fun Modifier.handCursor() = pointerHoverIcon(PointerIcon.Hand, overrideDescendants = true)

// ──────────────────────────────────────────────────────────
// Color schemes
// ──────────────────────────────────────────────────────────
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB794FF),
    onPrimary = Color(0xFF1A0F2E),
    primaryContainer = Color(0xFF3A1D6E),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFF4DD7EE),
    onSecondary = Color(0xFF003640),
    secondaryContainer = Color(0xFF0E4A56),
    onSecondaryContainer = Color(0xFFB6ECF6),
    tertiary = Color(0xFFE879F9),
    onTertiary = Color(0xFF38003F),
    tertiaryContainer = Color(0xFF5C1366),
    onTertiaryContainer = Color(0xFFFAD7FF),
    surface = Color(0xFF15121F),
    onSurface = Color(0xFFF3EFFA),
    surfaceVariant = Color(0xFF2A2536),
    onSurfaceVariant = Color(0xFFCAC2DA),
    background = Color(0xFF0F0D17),
    onBackground = Color(0xFFF3EFFA),
    outline = Color(0xFF4A4458),
    outlineVariant = Color(0xFF332E40),
)

val LightColorScheme = lightColorScheme(
    primary = darkPurple,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDE0FF),
    onPrimaryContainer = Color(0xFF24005A),
    secondary = Color(0xFF0891B2),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC4EEF7),
    onSecondaryContainer = Color(0xFF00363F),
    tertiary = Color(0xFFC026D3),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFBD7FF),
    onTertiaryContainer = Color(0xFF3B0044),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFECE7F4),
    onSurfaceVariant = Color(0xFF4A4458),
    background = Color(0xFFFBFAFF),
    onBackground = Color(0xFF1C1B1F),
    outline = Color(0xFFCAC4D0),
    outlineVariant = Color(0xFFE3DEEC),
)

fun ColorScheme.withBlackBackground(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
)

val ColorScheme.isOledFlavor: Boolean get() = background == Color.Black

/** True when the active scheme paints onto a dark base (dark or OLED). */
val ColorScheme.isDarkSurface: Boolean get() = background.luminance() < 0.5f

// ──────────────────────────────────────────────────────────
// Aurora UI — animated flowing gradient background
// ──────────────────────────────────────────────────────────

/**
 * Paints an animated "aurora" backdrop: an opaque base ([ColorScheme.background])
 * with several softly drifting, semi-transparent colour glows layered on top.
 *
 * The base stays opaque so any content drawn above remains as readable as it would
 * be over the plain background — the glows only add a subtle wash of colour.
 *
 * @param animated set to false to render a static aurora (respects reduced-motion
 *   preferences and keeps screenshot/preview output deterministic). Animation is
 *   also disabled automatically inside Compose previews.
 * @param intensity multiplies the glow opacity (0f..1f-ish) for screens that want a
 *   more or less pronounced effect.
 */
@Composable
fun Modifier.auroraBackground(
    animated: Boolean = true,
    intensity: Float = 1f,
): Modifier {
    val scheme = MaterialTheme.colorScheme
    val inPreview = androidx.compose.ui.platform.LocalInspectionMode.current
    val shouldAnimate = animated && !inPreview

    val transition = rememberInfiniteTransition(label = "aurora")
    val phaseState = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22_000, easing = LinearEasing),
        ),
        label = "auroraPhase",
    )

    val dark = scheme.isDarkSurface
    val baseAlpha = (if (dark) 0.30f else 0.22f) * intensity.coerceIn(0f, 1.5f)

    // Aurora ribbons. Cyan/teal + violet + magenta echo the brand gradient.
    val c1 = auroraViolet.copy(alpha = baseAlpha)
    val c2 = auroraCyan.copy(alpha = baseAlpha * 0.95f)
    val c3 = auroraMagenta.copy(alpha = baseAlpha * 0.85f)
    val c4 = auroraBlue.copy(alpha = baseAlpha * 0.8f)

    val base = scheme.background

    return this.drawBehind {
        // Reading the animated state inside the draw scope re-runs only the draw
        // phase each frame — no recomposition of the wrapped content.
        val phase = if (shouldAnimate) phaseState.value else 0.12f
        drawRect(base)
        val w = size.width
        val h = size.height
        val maxDim = max(w, h)
        val a = phase * 2f * PI.toFloat()

        fun glow(color: Color, fx: Float, fy: Float, rFactor: Float) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(color, Color.Transparent),
                    center = Offset(w * fx, h * fy),
                    radius = maxDim * rFactor,
                ),
            )
        }

        glow(c1, 0.18f + 0.16f * sin(a), 0.12f + 0.10f * cos(a * 0.8f), 0.95f)
        glow(c2, 0.86f + 0.12f * cos(a * 1.1f), 0.22f + 0.12f * sin(a * 0.9f), 0.85f)
        glow(c3, 0.50f + 0.22f * sin(a * 0.7f + 1f), 0.88f + 0.10f * cos(a), 1.05f)
        glow(c4, 0.12f + 0.10f * cos(a * 1.3f), 0.78f + 0.10f * sin(a * 1.2f), 0.75f)
    }
}

// ──────────────────────────────────────────────────────────
// Glassmorphism + Soft UI helpers
// ──────────────────────────────────────────────────────────

/**
 * Frosted-glass surface: a translucent fill, a soft highlight wash and a subtle
 * light border. Designed to sit over [auroraBackground] so the colours bleed
 * gently through. Falls back to a transparent/outlined treatment on the OLED
 * flavor to keep pixels truly black.
 */
@Composable
fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(20.dp),
): Modifier {
    val scheme = MaterialTheme.colorScheme
    if (scheme.isOledFlavor) {
        return this
            .clip(shape)
            .background(Color.White.copy(alpha = 0.04f), shape)
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
    }
    val dark = scheme.isDarkSurface
    val tint = scheme.surface.copy(alpha = if (dark) 0.46f else 0.55f)
    val highlight = if (dark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.40f)
    val borderBrush = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = if (dark) 0.24f else 0.65f),
            Color.White.copy(alpha = if (dark) 0.04f else 0.18f),
        ),
    )
    return this
        .clip(shape)
        .background(tint, shape)
        .background(highlight, shape)
        .border(1.dp, borderBrush, shape)
}

/**
 * Soft, diffuse drop shadow for elevated surfaces (Soft UI). Optionally tints the
 * shadow with the primary colour for a gentle aurora glow under the element.
 */
@Composable
fun Modifier.softShadow(
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 12.dp,
    glow: Boolean = false,
): Modifier {
    val shadowColor = if (glow) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    } else {
        Color.Black.copy(alpha = 0.35f)
    }
    return this.shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = shadowColor,
        spotColor = shadowColor,
    )
}

// ── Adaptive card styling (glassy) ────────────────────────
@Composable
private fun kaiGlassContainerColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return when {
        scheme.isOledFlavor -> Color.Transparent
        scheme.isDarkSurface -> scheme.surfaceVariant.copy(alpha = 0.45f)
        else -> Color.White.copy(alpha = 0.55f)
    }
}

@Composable
private fun kaiGlassBorderColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return when {
        scheme.isOledFlavor -> scheme.outlineVariant
        scheme.isDarkSurface -> Color.White.copy(alpha = 0.12f)
        else -> Color.White.copy(alpha = 0.6f)
    }
}

@Composable
fun kaiAdaptiveCardColors(): CardColors = CardDefaults.cardColors(
    containerColor = kaiGlassContainerColor(),
)

@Composable
fun kaiAdaptiveCardBorder(): BorderStroke? = BorderStroke(1.dp, kaiGlassBorderColor())

@Composable
fun Modifier.kaiAdaptiveCardSurface(shape: Shape = CardDefaults.shape): Modifier = this
    .clip(shape)
    .background(kaiGlassContainerColor(), shape)
    .border(1.dp, kaiGlassBorderColor(), shape)

@Composable
fun outlineTextFieldColors() = OutlinedTextFieldDefaults.colors()

@Composable
fun KaiOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        label = label,
        placeholder = placeholder,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        shape = RoundedCornerShape(12.dp),
        colors = outlineTextFieldColors(),
    )
}

@Composable
fun KaiClearableTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    KaiOutlinedTextField(
        modifier = modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
        value = value,
        onValueChange = onValueChange,
        label = label,
        singleLine = singleLine,
        trailingIcon = {
            IconButton(
                onClick = { onValueChange("") },
                modifier = Modifier.handCursor()
                    .alpha(if (focused && value.isNotEmpty()) 1f else 0f),
                enabled = value.isNotEmpty(),
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
@Preview
fun Theme(
    colorScheme: ColorScheme,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = colorScheme,
    ) {
        content()
    }
}
