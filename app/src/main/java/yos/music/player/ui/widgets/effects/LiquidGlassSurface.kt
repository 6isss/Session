package yos.music.player.ui.widgets.effects

import android.annotation.SuppressLint
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * RayMusic-style liquid glass. Haze captures the real screen behind this
 * surface; Android 13+ then refracts that captured layer with AGSL while the
 * controls stay crisp above it. Older Android versions keep the blurred,
 * tinted glass and optical rim without attempting unsupported refraction.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun LiquidGlassSurface(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(percent = 50),
    tint: Color,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    val rim = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = 0.68f),
            Color.White.copy(alpha = 0.08f),
            Color.Black.copy(alpha = 0.14f),
            Color.White.copy(alpha = 0.34f)
        )
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.shape = shape
            clip = false
            shadowElevation = 14.dp.toPx()
            ambientShadowColor = Color.Black.copy(alpha = 0.18f)
            spotShadowColor = Color.Black.copy(alpha = 0.28f)
        },
        contentAlignment = contentAlignment
    ) {
        LiquidBackdrop(hazeState, tint, shape)
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.11f),
                            tint.copy(alpha = 0.05f),
                            Color.Black.copy(alpha = 0.04f)
                        )
                    ),
                    shape
                )
                .border(0.9.dp, rim, shape)
                .clip(shape)
        )
        content()
    }
}

@SuppressLint("NewApi")
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun BoxScope.LiquidBackdrop(hazeState: HazeState, tint: Color, shape: Shape) {
    val shader = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        remember { RuntimeShader(LIQUID_GLASS_SHADER) }
    } else null

    Box(
        Modifier
            .matchParentSize()
            .clip(shape)
            .graphicsLayer {
                if (shader != null) {
                    shader.setFloatUniform("resolution", size.width, size.height)
                    renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "contents")
                        .asComposeRenderEffect()
                }
            }
            .hazeChild(
                state = hazeState,
                style = HazeMaterials.thick(tint).copy(blurRadius = 22.dp)
            )
            .background(tint.copy(alpha = 0.18f), shape)
    )
}

private const val LIQUID_GLASS_SHADER = """
uniform shader contents;
uniform float2 resolution;

float roundedBox(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

half4 main(float2 fragCoord) {
    float2 center = resolution * 0.5;
    float2 halfSize = max(center, float2(1.0));
    float radius = min(halfSize.y, 30.0);
    float2 local = fragCoord - center;
    float sdf = roundedBox(local, halfSize - 1.0, radius);
    float edge = 1.0 - smoothstep(-18.0, -2.0, sdf);
    float2 normalized = local / halfSize;
    float bulge = (1.0 - min(dot(normalized, normalized), 1.0)) * 0.018;
    float2 refracted = center + local * (1.0 - bulge - edge * 0.045);

    half4 color = contents.eval(refracted);
    color += contents.eval(refracted + float2(2.5, 0.0)) * half4(0.025, 0.0, 0.0, 0.0);
    color += contents.eval(refracted - float2(2.5, 0.0)) * half4(0.0, 0.0, 0.025, 0.0);
    half highlight = half(edge * (1.0 - smoothstep(-12.0, 8.0, local.y)) * 0.18);
    color.rgb += highlight;
    return color;
}
"""
