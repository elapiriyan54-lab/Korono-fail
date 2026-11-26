package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import android.net.Uri
import com.example.R
import com.example.model.FileKind
import com.example.model.KronoFileItem
import com.example.ui.theme.AbyssalTeal
import com.example.ui.theme.DeepSapphire
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.FrostedPearl
import com.example.ui.theme.GlassBorderAmber
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.GlassShadowDeep
import com.example.ui.theme.GlassSlateBottom
import com.example.ui.theme.GlassSlateMid
import com.example.ui.theme.GlassSlateTop
import com.example.ui.theme.IceCyan
import com.example.ui.theme.LiquidEmerald
import com.example.ui.theme.MoltenAmber
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PrismaticViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmGold
import com.example.util.FormatUtils
import java.io.File

enum class BottomNavTab(val label: String, val tag: String) {
  HOME("Home", "nav_home"),
  ALL_FILES("All Files", "nav_all_files"),
  SEARCH("Search", "nav_search"),
  VAULT("Vault", "nav_vault"),
}

/**
 * High-contrast Emerald & Abyssal-Teal Liquid Wave + Caustic Light-Streak fluid backdrop
 * across all screens, framed with subtle edge readability vignetting and ambient caustic glows.
 */
@Composable
fun KronoObsidianBackdrop(
  modifier: Modifier = Modifier,
  cyanIntensity: Float = 0.28f,
  violetIntensity: Float = 0.14f,
  amberIntensity: Float = 0.16f,
  content: @Composable BoxScope.() -> Unit,
) {
  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(ObsidianBg),
  ) {
    Image(
      painter = painterResource(id = R.drawable.img_luminous_liquid_bg_1795589225319),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      // Gentle abyssal-teal framing for sharp white text legibility without dimming mid-screen liquid caustics
      drawRect(
        brush =
          Brush.verticalGradient(
            colors =
              listOf(
                Color(0xFF041820).copy(alpha = 0.26f),
                Color(0xFF041C24).copy(alpha = 0.08f),
                Color(0xFF041A22).copy(alpha = 0.10f),
                Color(0xFF04141C).copy(alpha = 0.30f),
              ),
          )
      )
      // Luminous emerald-cyan upper caustic reinforcement
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                LiquidEmerald.copy(alpha = cyanIntensity * 0.52f),
                IceCyan.copy(alpha = cyanIntensity * 0.34f),
                Color.Transparent,
              ),
            center = Offset(w * 0.20f, h * 0.22f),
            radius = size.maxDimension * 0.54f,
          ),
        radius = size.maxDimension * 0.54f,
        center = Offset(w * 0.20f, h * 0.22f),
      )
      // Prismatic Violet & Electric Blue upper-right shimmer
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                PrismaticViolet.copy(alpha = violetIntensity * 0.85f),
                ElectricBlue.copy(alpha = violetIntensity * 0.52f),
                Color.Transparent,
              ),
            center = Offset(w * 0.86f, h * 0.24f),
            radius = size.maxDimension * 0.44f,
          ),
        radius = size.maxDimension * 0.44f,
        center = Offset(w * 0.86f, h * 0.24f),
      )
      // Molten Amber lower-right caustic glow
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                MoltenAmber.copy(alpha = amberIntensity * 0.72f),
                WarmGold.copy(alpha = amberIntensity * 0.30f),
                Color.Transparent,
              ),
            center = Offset(w * 0.84f, h * 0.82f),
            radius = size.maxDimension * 0.44f,
          ),
        radius = size.maxDimension * 0.44f,
        center = Offset(w * 0.84f, h * 0.82f),
      )
    }
    content()
  }
}

/**
 * Multi-layered 3D Liquid Glass surface with soft floating drop shadow, translucent depth fill,
 * inner top-left Frosted Pearl specular highlight, prismatic rim refraction, and gentle press feedback.
 */
@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 20.dp,
  borderAccentStart: Color = GlassBorderCyan,
  borderAccentEnd: Color = GlassBorderAmber,
  fillAlphaTop: Float = 0.28f,
  fillAlphaBottom: Float = 0.16f,
  elevationShadowAlpha: Float = 0.52f,
  onClick: (() -> Unit)? = null,
  content: @Composable BoxScope.() -> Unit,
) {
  val shape = RoundedCornerShape(cornerRadius)
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val pressScale by animateFloatAsState(
    targetValue = if (onClick != null && isPressed) 0.975f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    label = "glass_card_scale",
  )
  val pressBoost by animateFloatAsState(
    targetValue = if (onClick != null && isPressed) 1f else 0f,
    animationSpec = tween(140),
    label = "glass_card_boost",
  )

  val borderBrush =
    Brush.linearGradient(
      colors =
        listOf(
          FrostedPearl.copy(alpha = 0.86f + 0.14f * pressBoost),
          borderAccentStart.copy(alpha = 0.96f),
          LiquidEmerald.copy(alpha = 0.58f),
          borderAccentEnd.copy(alpha = 0.92f),
        ),
      start = Offset.Zero,
      end = Offset(900f, 900f),
    )

  val fillBrush =
    Brush.linearGradient(
      colors =
        listOf(
          GlassSlateTop.copy(alpha = (fillAlphaTop * 0.92f + 0.08f * pressBoost).coerceIn(0.16f, 0.42f)),
          GlassSlateMid.copy(alpha = (fillAlphaTop * 0.58f).coerceIn(0.10f, 0.28f)),
          GlassSlateBottom.copy(alpha = (fillAlphaBottom + 0.10f).coerceIn(0.16f, 0.38f)),
        ),
      start = Offset.Zero,
      end = Offset(650f, 950f),
    )

  Box(
    modifier =
      modifier
        .graphicsLayer {
          scaleX = pressScale
          scaleY = pressScale
        }
        .drawBehind {
          val cr = cornerRadius.toPx()
          // 1. Floating drop shadow beneath the glass card
          drawRoundRect(
            brush =
              Brush.verticalGradient(
                listOf(
                  GlassShadowDeep.copy(alpha = elevationShadowAlpha * 0.38f),
                  GlassShadowDeep.copy(alpha = elevationShadowAlpha * 0.85f),
                )
              ),
            topLeft = Offset(0f, 6.dp.toPx() * (1f - 0.3f * pressBoost)),
            size = Size(size.width, size.height + 2.dp.toPx()),
            cornerRadius = CornerRadius(cr + 2.dp.toPx(), cr + 2.dp.toPx()),
          )
          // 2. Outer thick prismatic refraction halo
          drawRoundRect(
            brush =
              Brush.linearGradient(
                listOf(
                  FrostedPearl.copy(alpha = 0.14f + 0.08f * pressBoost),
                  borderAccentStart.copy(alpha = 0.24f + 0.08f * pressBoost),
                  borderAccentEnd.copy(alpha = 0.20f + 0.08f * pressBoost),
                )
              ),
            cornerRadius = CornerRadius(cr + 3.dp.toPx(), cr + 3.dp.toPx()),
            style = Stroke(width = 5.5.dp.toPx()),
          )
        }
        .clip(shape)
        .background(fillBrush)
        .drawBehind {
          // 3. Inner beveled glass wall & top Frosted Pearl specular sheen
          val cr = cornerRadius.toPx().coerceAtMost(size.minDimension / 2f)
          // Thick translucent inner glass bezel ring (refractive rim thickness)
          drawRoundRect(
            brush =
              Brush.linearGradient(
                listOf(
                  FrostedPearl.copy(alpha = 0.22f + 0.10f * pressBoost),
                  borderAccentStart.copy(alpha = 0.14f),
                  Color.Transparent,
                  borderAccentEnd.copy(alpha = 0.16f),
                )
              ),
            topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
            size = Size((size.width - 4.dp.toPx()).coerceAtLeast(1f), (size.height - 4.dp.toPx()).coerceAtLeast(1f)),
            cornerRadius = CornerRadius((cr - 2.dp.toPx()).coerceAtLeast(4f), (cr - 2.dp.toPx()).coerceAtLeast(4f)),
            style = Stroke(width = 3.6.dp.toPx()),
          )
          drawRoundRect(
            brush =
              Brush.verticalGradient(
                colors =
                  listOf(
                    FrostedPearl.copy(alpha = 0.32f + 0.14f * pressBoost),
                    FrostedPearl.copy(alpha = 0.08f),
                    Color.Transparent,
                  ),
                startY = 0f,
                endY = size.height * 0.38f,
              ),
            cornerRadius = CornerRadius(cr, cr),
          )
          // Subtle inner bottom caustic tint
          drawRoundRect(
            brush =
              Brush.linearGradient(
                colors =
                  listOf(
                    Color.Transparent,
                    borderAccentEnd.copy(alpha = 0.08f + 0.06f * pressBoost),
                  ),
                start = Offset(0f, size.height * 0.58f),
                end = Offset(size.width, size.height),
              ),
            cornerRadius = CornerRadius(cr, cr),
          )
          // Thick beveled top-left Frosted Pearl + Cyan corner meniscus highlight
          val arcBox = (cr * 2f).coerceAtLeast(16.dp.toPx())
          drawArc(
            brush =
              Brush.linearGradient(
                listOf(FrostedPearl.copy(alpha = 0.90f + 0.10f * pressBoost), borderAccentStart.copy(alpha = 0.85f))
              ),
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(2.2.dp.toPx(), 2.2.dp.toPx()),
            size = Size(arcBox, arcBox),
            style = Stroke(width = 3.0.dp.toPx(), cap = StrokeCap.Round),
          )
          // Bottom-right exit-light bevel arc
          drawArc(
            brush =
              Brush.linearGradient(
                listOf(borderAccentEnd.copy(alpha = 0.68f), FrostedPearl.copy(alpha = 0.72f))
              ),
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(size.width - arcBox - 2.2.dp.toPx(), size.height - arcBox - 2.2.dp.toPx()),
            size = Size(arcBox, arcBox),
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round),
          )
        }
        .border(width = 1.5.dp, brush = borderBrush, shape = shape)
        .then(
          if (onClick != null) {
            Modifier.clickable(
              interactionSource = interactionSource,
              indication = LocalIndication.current,
              onClick = onClick,
            )
          } else Modifier
        ),
    content = content,
  )
}

/**
 * 3D Liquid Glass Orb Button:
 * Floating drop shadow + convex off-center translucent dome + Frosted Pearl top-left specular crescent +
 * bottom-right caustic edge bounce + prismatic rim refraction + spring press feedback.
 */
@Composable
fun GlassBubbleButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = 48.dp,
  iconSize: Dp = 22.dp,
  tint: Color = TextPrimary,
  accentStart: Color = IceCyan,
  accentEnd: Color = WarmGold,
  isActive: Boolean = false,
  badgeCount: Int = 0,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val pressScale by animateFloatAsState(
    targetValue = if (isPressed) 0.91f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
    label = "orb_scale",
  )
  val pressHighlight by animateFloatAsState(
    targetValue = if (isPressed) 1f else 0f,
    animationSpec = tween(120),
    label = "orb_highlight",
  )

  val rimBrush =
    Brush.linearGradient(
      colors =
        listOf(
          FrostedPearl.copy(alpha = if (isActive) 0.85f else 0.58f),
          accentStart.copy(alpha = if (isActive) 0.96f else 0.78f),
          PrismaticViolet.copy(alpha = 0.45f),
          accentEnd.copy(alpha = if (isActive) 0.96f else 0.76f),
        ),
      start = Offset.Zero,
      end = Offset(180f, 180f),
    )

  Box(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .size(size)
        .graphicsLayer {
          scaleX = pressScale
          scaleY = pressScale
        }
        .drawBehind {
          val radius = this.size.minDimension / 2f
          val center = Offset(this.size.width / 2f, this.size.height / 2f)
          // 1. Floating soft drop shadow offset slightly downward
          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    Color.Black.copy(alpha = 0.82f),
                    Color.Black.copy(alpha = 0.38f),
                    Color.Transparent,
                  ),
                center = Offset(center.x, center.y + 4.5.dp.toPx() * (1f - 0.35f * pressHighlight)),
                radius = radius * 1.24f,
              ),
            radius = radius * 1.24f,
            center = Offset(center.x, center.y + 4.5.dp.toPx() * (1f - 0.35f * pressHighlight)),
          )
          // 2. Ambient luminous caustic halo
          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    accentStart.copy(alpha = (if (isActive) 0.36f else 0.18f) + 0.12f * pressHighlight),
                    accentEnd.copy(alpha = (if (isActive) 0.22f else 0.10f) + 0.08f * pressHighlight),
                    Color.Transparent,
                  ),
                center = center,
                radius = radius * 1.32f,
              ),
            radius = radius * 1.32f,
            center = center,
          )
        }
        .clip(CircleShape)
        .background(Color(0xFF0A2B36).copy(alpha = 0.28f))
        .drawBehind {
          val r = this.size.minDimension / 2f
          val center = Offset(this.size.width / 2f, this.size.height / 2f)
          val specCenter = Offset(this.size.width * 0.33f, this.size.height * 0.28f)

          // 3. Convex 3D Liquid Glass dome radial fill (luminous translucent crystal)
          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  if (isActive) {
                    listOf(
                      FrostedPearl.copy(alpha = 0.42f + 0.14f * pressHighlight),
                      accentStart.copy(alpha = 0.44f),
                      DeepSapphire.copy(alpha = 0.48f),
                      Color(0xFF093140).copy(alpha = 0.62f),
                    )
                  } else {
                    listOf(
                      FrostedPearl.copy(alpha = 0.32f + 0.14f * pressHighlight),
                      accentStart.copy(alpha = 0.26f),
                      Color(0xFF1A4B5C).copy(alpha = 0.46f),
                      Color(0xFF092530).copy(alpha = 0.58f),
                    )
                  },
                center = specCenter,
                radius = r * 1.45f,
              ),
            radius = r,
            center = center,
          )

          // 4. Inner luminous core glow when active or pressed
          if (isActive || pressHighlight > 0.05f) {
            drawCircle(
              brush =
                Brush.radialGradient(
                  colors =
                    listOf(
                      accentEnd.copy(alpha = 0.24f * (if (isActive) 1f else pressHighlight)),
                      accentStart.copy(alpha = 0.14f),
                      Color.Transparent,
                    ),
                  center = Offset(this.size.width * 0.62f, this.size.height * 0.66f),
                  radius = r * 0.95f,
                ),
              radius = r,
              center = center,
            )
          }

          // 5. Crisp top-left Frosted Pearl glass specular meniscus arc + soft upper droplet sheen
          drawArc(
            brush =
              Brush.linearGradient(
                colors =
                  listOf(
                    FrostedPearl.copy(alpha = 0.68f + 0.18f * pressHighlight),
                    FrostedPearl.copy(alpha = 0.08f),
                  ),
                start = Offset(0f, 0f),
                end = Offset(this.size.width * 0.65f, this.size.height * 0.65f),
              ),
            startAngle = 195f,
            sweepAngle = 92f,
            useCenter = false,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round),
            topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
            size = Size(this.size.width - 6.dp.toPx(), this.size.height - 6.dp.toPx()),
          )

          // Soft specular pill highlight inside upper dome
          drawOval(
            brush =
              Brush.verticalGradient(
                listOf(
                  FrostedPearl.copy(alpha = 0.26f + 0.10f * pressHighlight),
                  Color.Transparent,
                )
              ),
            topLeft = Offset(this.size.width * 0.22f, this.size.height * 0.10f),
            size = Size(this.size.width * 0.56f, this.size.height * 0.30f),
          )

          // 6. Subtle bottom-right inner rim bounce (light exiting the glass sphere)
          drawArc(
            color = accentEnd.copy(alpha = if (isActive) 0.55f else 0.32f),
            startAngle = 18f,
            sweepAngle = 72f,
            useCenter = false,
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round),
            topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
            size = Size(this.size.width - 6.dp.toPx(), this.size.height - 6.dp.toPx()),
          )
        }
        .border(
          width = if (isActive) 1.6.dp else 1.2.dp,
          brush = rimBrush,
          shape = CircleShape,
        )
        .clickable(
          interactionSource = interactionSource,
          indication = LocalIndication.current,
          onClick = onClick,
        ),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = tint,
      modifier = Modifier.size(iconSize),
    )
    if (badgeCount > 0) {
      Box(
        modifier =
          Modifier.align(Alignment.TopEnd)
            .padding(2.dp)
            .size(18.dp)
            .drawBehind {
              drawCircle(
                color = MoltenAmber.copy(alpha = 0.55f),
                radius = this.size.minDimension * 0.68f,
              )
            }
            .clip(CircleShape)
            .background(
              Brush.radialGradient(
                colors = listOf(FrostedPearl.copy(alpha = 0.55f), WarmGold, MoltenAmber),
                center = Offset(4f, 4f),
              )
            )
            .border(1.dp, FrostedPearl.copy(alpha = 0.75f), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = if (badgeCount > 9) "9+" else badgeCount.toString(),
          style = MaterialTheme.typography.labelSmall,
          color = ObsidianDeep,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

/**
 * 3D Liquid Glass Icon Vessel replacing flat tinted boxes across Category Tiles, Notifications,
 * Audio Playlists, Vault items, and Create Action Rows.
 */
@Composable
fun LiquidGlassIconOrb(
  icon: ImageVector,
  contentDescription: String?,
  accent: Color,
  modifier: Modifier = Modifier,
  boxSize: Dp = 44.dp,
  iconSize: Dp = boxSize * 0.52f,
  cornerRadius: Dp = 14.dp,
  secondaryAccent: Color = ElectricBlue,
) {
  val shape = RoundedCornerShape(cornerRadius)
  Box(
    modifier =
      modifier
        .size(boxSize)
        .drawBehind {
          val cr = cornerRadius.toPx()
          // Floating drop shadow
          drawRoundRect(
            color = Color.Black.copy(alpha = 0.72f),
            topLeft = Offset(0f, 3.dp.toPx()),
            size = size,
            cornerRadius = CornerRadius(cr, cr),
          )
          // Ambient tinted glow
          drawRoundRect(
            brush =
              Brush.radialGradient(
                listOf(accent.copy(alpha = 0.28f), Color.Transparent),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.maxDimension * 0.78f,
              ),
            cornerRadius = CornerRadius(cr + 3.dp.toPx(), cr + 3.dp.toPx()),
          )
        }
        .clip(shape)
        .background(Color(0xFF0A2A34).copy(alpha = 0.42f))
        .drawBehind {
          val cr = cornerRadius.toPx()
          // Translucent liquid glass body with off-center sheen
          drawRoundRect(
            brush =
              Brush.linearGradient(
                colors =
                  listOf(
                    FrostedPearl.copy(alpha = 0.30f),
                    accent.copy(alpha = 0.34f),
                    secondaryAccent.copy(alpha = 0.24f),
                    Color(0xFF0B2F3A).copy(alpha = 0.56f),
                  ),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
              ),
            cornerRadius = CornerRadius(cr, cr),
          )
          // Inner radial luminous core
          drawCircle(
            brush =
              Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.46f), Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 0.55f),
                radius = size.maxDimension * 0.62f,
              )
          )
          // Top-left Frosted Pearl glass glint
          drawRoundRect(
            brush =
              Brush.verticalGradient(
                listOf(FrostedPearl.copy(alpha = 0.38f), Color.Transparent),
                startY = 0f,
                endY = size.height * 0.44f,
              ),
            cornerRadius = CornerRadius(cr, cr),
          )
        }
        .border(
          width = 1.15.dp,
          brush =
            Brush.linearGradient(
              listOf(
                FrostedPearl.copy(alpha = 0.65f),
                accent.copy(alpha = 0.82f),
                secondaryAccent.copy(alpha = 0.45f),
              ),
              start = Offset.Zero,
              end = Offset(140f, 140f),
            ),
          shape = shape,
        ),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = accent,
      modifier = Modifier.size(iconSize),
    )
  }
}

/**
 * 3D Liquid Glass Capsule Action Button (replacing flat Material Buttons for focal CTAs).
 */
@Composable
fun LiquidGlassActionButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  accentStart: Color = IceCyan,
  accentEnd: Color = DeepSapphire,
  contentColor: Color = TextPrimary,
  contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
  content: @Composable RowScope.() -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val pressScale by animateFloatAsState(
    targetValue = if (enabled && isPressed) 0.95f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    label = "action_btn_scale",
  )
  val pressBoost by animateFloatAsState(
    targetValue = if (enabled && isPressed) 1f else 0f,
    animationSpec = tween(120),
    label = "action_btn_boost",
  )
  val shape = RoundedCornerShape(50)

  val effectiveStart = if (enabled) accentStart else TextMuted.copy(alpha = 0.35f)
  val effectiveEnd = if (enabled) accentEnd else ObsidianSurface

  Row(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .heightIn(min = 42.dp)
        .graphicsLayer {
          scaleX = pressScale
          scaleY = pressScale
          alpha = if (enabled) 1f else 0.55f
        }
        .drawBehind {
          val cr = size.height / 2f
          if (enabled) {
            // Floating shadow
            drawRoundRect(
              color = Color.Black.copy(alpha = 0.68f),
              topLeft = Offset(0f, 4.dp.toPx() * (1f - 0.3f * pressBoost)),
              size = size,
              cornerRadius = CornerRadius(cr, cr),
            )
            // Soft outer neon refraction glow
            drawRoundRect(
              brush =
                Brush.horizontalGradient(
                  listOf(
                    effectiveStart.copy(alpha = 0.24f + 0.14f * pressBoost),
                    effectiveEnd.copy(alpha = 0.24f + 0.14f * pressBoost),
                  )
                ),
              cornerRadius = CornerRadius(cr + 3.dp.toPx(), cr + 3.dp.toPx()),
              style = Stroke(width = 4.dp.toPx()),
            )
          }
        }
        .clip(shape)
        .background(Color(0xFF092A35).copy(alpha = 0.48f))
        .drawBehind {
          val cr = size.height / 2f
          // Luminous liquid gradient fill
          drawRoundRect(
            brush =
              Brush.linearGradient(
                colors =
                  listOf(
                    effectiveStart.copy(alpha = 0.56f + 0.14f * pressBoost),
                    effectiveEnd.copy(alpha = 0.48f + 0.12f * pressBoost),
                    Color(0xFF0C3644).copy(alpha = 0.54f),
                  ),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
              ),
            cornerRadius = CornerRadius(cr, cr),
          )
          // Top Frosted Pearl specular highlight
          drawRoundRect(
            brush =
              Brush.verticalGradient(
                listOf(
                  FrostedPearl.copy(alpha = 0.42f + 0.12f * pressBoost),
                  Color.Transparent,
                ),
                startY = 0f,
                endY = size.height * 0.52f,
              ),
            cornerRadius = CornerRadius(cr, cr),
          )
        }
        .border(
          width = 1.25.dp,
          brush =
            Brush.linearGradient(
              listOf(
                FrostedPearl.copy(alpha = 0.75f),
                effectiveStart,
                effectiveEnd,
              )
            ),
          shape = shape,
        )
        .clickable(
          enabled = enabled,
          interactionSource = interactionSource,
          indication = LocalIndication.current,
          onClick = onClick,
        )
        .padding(contentPadding),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
    content = {
      androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.material3.LocalContentColor provides if (enabled) contentColor else TextMuted
      ) {
        content()
      }
    },
  )
}

/**
 * Milky Frosted 3D Liquid Glass secondary capsule button (matching the translucent frosted
 * "Dismiss" / "Cancel" pills in the reference Notifications & Dialog designs).
 */
@Composable
fun LiquidGlassFrostedButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  contentColor: Color = FrostedPearl,
  contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
  content: @Composable RowScope.() -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val pressScale by animateFloatAsState(
    targetValue = if (isPressed) 0.95f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    label = "frosted_btn_scale",
  )
  val pressBoost by animateFloatAsState(
    targetValue = if (isPressed) 1f else 0f,
    animationSpec = tween(120),
    label = "frosted_btn_boost",
  )
  val shape = RoundedCornerShape(50)

  Row(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .heightIn(min = 40.dp)
        .graphicsLayer {
          scaleX = pressScale
          scaleY = pressScale
        }
        .drawBehind {
          val cr = size.height / 2f
          drawRoundRect(
            color = Color.Black.copy(alpha = 0.56f),
            topLeft = Offset(0f, 3.dp.toPx()),
            size = size,
            cornerRadius = CornerRadius(cr, cr),
          )
        }
        .clip(shape)
        .background(Color(0xFF144252).copy(alpha = 0.38f))
        .drawBehind {
          val cr = size.height / 2f
          drawRoundRect(
            brush =
              Brush.verticalGradient(
                listOf(
                  FrostedPearl.copy(alpha = 0.42f + 0.14f * pressBoost),
                  FrostedPearl.copy(alpha = 0.22f + 0.08f * pressBoost),
                  Color(0xFF427386).copy(alpha = 0.26f),
                )
              ),
            cornerRadius = CornerRadius(cr, cr),
          )
          drawRoundRect(
            brush =
              Brush.verticalGradient(
                listOf(FrostedPearl.copy(alpha = 0.44f), Color.Transparent),
                startY = 0f,
                endY = size.height * 0.5f,
              ),
            cornerRadius = CornerRadius(cr, cr),
          )
        }
        .border(
          width = 1.15.dp,
          brush = Brush.linearGradient(listOf(FrostedPearl.copy(alpha = 0.78f), FrostedPearl.copy(alpha = 0.28f), IceCyan.copy(alpha = 0.42f))),
          shape = shape,
        )
        .clickable(
          interactionSource = interactionSource,
          indication = LocalIndication.current,
          onClick = onClick,
        )
        .padding(contentPadding),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
    content = {
      androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.material3.LocalContentColor provides contentColor
      ) {
        content()
      }
    },
  )
}

/**
 * Floating 3D Liquid Glass status / micro-badge pill (replaces flat sticker badges like NOW PLAYING / Keep).
 */
@Composable
fun LiquidGlassBadgePill(
  text: String,
  accent: Color = IceCyan,
  secondaryAccent: Color = WarmGold,
  leadingIcon: ImageVector? = null,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(50)
  Row(
    modifier =
      modifier
        .clip(shape)
        .background(Color(0xFF0A2E3A).copy(alpha = 0.44f))
        .drawBehind {
          val cr = size.height / 2f
          drawRoundRect(
            brush =
              Brush.linearGradient(
                listOf(
                  accent.copy(alpha = 0.38f),
                  secondaryAccent.copy(alpha = 0.24f),
                )
              ),
            cornerRadius = CornerRadius(cr, cr),
          )
          drawRoundRect(
            brush =
              Brush.verticalGradient(
                listOf(FrostedPearl.copy(alpha = 0.36f), Color.Transparent),
                startY = 0f,
                endY = size.height * 0.55f,
              ),
            cornerRadius = CornerRadius(cr, cr),
          )
        }
        .border(
          width = 1.dp,
          brush = Brush.linearGradient(listOf(FrostedPearl.copy(alpha = 0.65f), accent, secondaryAccent)),
          shape = shape,
        )
        .padding(horizontal = 9.dp, vertical = 3.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    if (leadingIcon != null) {
      Icon(leadingIcon, contentDescription = null, tint = accent, modifier = Modifier.size(12.dp))
    }
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = TextPrimary,
      fontWeight = FontWeight.SemiBold,
    )
  }
}

@Composable
fun GlassChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  leadingIcon: ImageVector? = null,
  accentColor: Color = IceCyan,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val pressScale by animateFloatAsState(
    targetValue = if (isPressed) 0.94f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    label = "chip_scale",
  )
  val pressBoost by animateFloatAsState(
    targetValue = if (isPressed) 1f else 0f,
    animationSpec = tween(120),
    label = "chip_boost",
  )

  val secondaryGlow = if (accentColor == WarmAmber || accentColor == WarmGold) WarmGold else DeepSapphire
  val borderBrush =
    if (selected) {
      Brush.linearGradient(listOf(FrostedPearl.copy(alpha = 0.72f), accentColor, WarmGold.copy(alpha = 0.85f)))
    } else {
      Brush.linearGradient(listOf(FrostedPearl.copy(alpha = 0.42f), accentColor.copy(alpha = 0.45f), PrismaticViolet.copy(alpha = 0.35f)))
    }
  val shape = RoundedCornerShape(50)

  Row(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .graphicsLayer {
          scaleX = pressScale
          scaleY = pressScale
        }
        .drawBehind {
          val cr = size.height / 2f
          drawRoundRect(
            color = Color.Black.copy(alpha = 0.56f),
            topLeft = Offset(0f, 3.dp.toPx() * (1f - 0.3f * pressBoost)),
            size = size,
            cornerRadius = CornerRadius(cr, cr),
          )
          if (selected) {
            drawRoundRect(
              brush = Brush.horizontalGradient(listOf(accentColor.copy(alpha = 0.24f), secondaryGlow.copy(alpha = 0.18f))),
              cornerRadius = CornerRadius(cr + 2.dp.toPx(), cr + 2.dp.toPx()),
              style = Stroke(width = 3.dp.toPx()),
            )
          }
        }
        .clip(shape)
        .background(Color(0xFF0B303C).copy(alpha = 0.28f))
        .drawBehind {
          val cr = size.height / 2f
          if (selected) {
            drawRoundRect(
              brush =
                Brush.linearGradient(
                  listOf(
                    accentColor.copy(alpha = 0.38f + 0.10f * pressBoost),
                    secondaryGlow.copy(alpha = 0.28f),
                    Color(0xFF0E3C4A).copy(alpha = 0.36f),
                  )
                ),
              cornerRadius = CornerRadius(cr, cr),
            )
          } else {
            drawRoundRect(
              brush =
                Brush.verticalGradient(
                  listOf(
                    GlassSlateTop.copy(alpha = 0.38f + 0.14f * pressBoost),
                    GlassSlateBottom.copy(alpha = 0.34f),
                  )
                ),
              cornerRadius = CornerRadius(cr, cr),
            )
          }
          // Top-edge Frosted Pearl liquid specular glint
          drawRoundRect(
            brush =
              Brush.verticalGradient(
                listOf(FrostedPearl.copy(alpha = (if (selected) 0.36f else 0.24f) + 0.10f * pressBoost), Color.Transparent),
                startY = 0f,
                endY = size.height * 0.48f,
              ),
            cornerRadius = CornerRadius(cr, cr),
          )
        }
        .border(if (selected) 1.4.dp else 1.1.dp, borderBrush, shape)
        .clickable(
          interactionSource = interactionSource,
          indication = LocalIndication.current,
          onClick = onClick,
        )
        .padding(horizontal = 14.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    if (leadingIcon != null) {
      Icon(
        imageVector = leadingIcon,
        contentDescription = null,
        tint = if (selected) accentColor else TextSecondary,
        modifier = Modifier.size(16.dp),
      )
    }
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = if (selected) TextPrimary else TextSecondary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
fun GlassSegmentedToggle(
  options: List<String>,
  selectedIndex: Int,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      modifier
        .drawBehind {
          val cr = size.height / 2f
          drawRoundRect(
            color = Color.Black.copy(alpha = 0.42f),
            topLeft = Offset(0f, 4.dp.toPx()),
            size = size,
            cornerRadius = CornerRadius(cr, cr),
          )
        }
        .clip(RoundedCornerShape(50))
        .background(Color(0xFF0B2F3B).copy(alpha = 0.36f))
        .background(Brush.verticalGradient(listOf(GlassSlateTop.copy(alpha = 0.34f), GlassSlateBottom.copy(alpha = 0.28f))))
        .border(
          1.35.dp,
          Brush.linearGradient(
            listOf(
              FrostedPearl.copy(alpha = 0.52f),
              GlassBorderCyan.copy(alpha = 0.72f),
              GlassBorderAmber.copy(alpha = 0.58f),
            )
          ),
          RoundedCornerShape(50),
        )
        .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    options.forEachIndexed { idx, title ->
      val active = idx == selectedIndex
      val interactionSource = remember { MutableInteractionSource() }
      val isPressed by interactionSource.collectIsPressedAsState()
      val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "seg_scale",
      )
      val bg by animateColorAsState(
        targetValue = if (active) IceCyan.copy(alpha = 0.26f) else Color.Transparent,
        animationSpec = tween(180),
        label = "seg_bg",
      )
      Box(
        modifier =
          Modifier.weight(1f)
            .minimumInteractiveComponentSize()
            .graphicsLayer {
              scaleX = pressScale
              scaleY = pressScale
            }
            .clip(RoundedCornerShape(50))
            .background(bg)
            .drawBehind {
              if (active) {
                val cr = size.height / 2f
                drawRoundRect(
                  brush =
                    Brush.verticalGradient(
                      listOf(FrostedPearl.copy(alpha = 0.24f), Color.Transparent),
                      startY = 0f,
                      endY = size.height * 0.55f,
                    ),
                  cornerRadius = CornerRadius(cr, cr),
                )
              }
            }
            .then(
              if (active) {
                Modifier.border(
                  1.15.dp,
                  Brush.linearGradient(
                    listOf(FrostedPearl.copy(alpha = 0.72f), IceCyan.copy(alpha = 0.9f), WarmGold.copy(alpha = 0.75f))
                  ),
                  RoundedCornerShape(50),
                )
              } else Modifier
            )
            .clickable(
              interactionSource = interactionSource,
              indication = LocalIndication.current,
              onClick = { onSelect(idx) },
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge,
          color = if (active) TextPrimary else TextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
fun FileThumbnailOrIcon(
  item: KronoFileItem,
  modifier: Modifier = Modifier,
  boxSize: Dp = 46.dp,
) {
  val accent = FormatUtils.accentForKind(item.kind)
  if (item.kind == FileKind.IMAGE && !item.isDirectory) {
    val shape = RoundedCornerShape(14.dp)
    Box(
      modifier =
        modifier
          .size(boxSize)
          .drawBehind {
            val cr = 14.dp.toPx()
            drawRoundRect(
              color = Color.Black.copy(alpha = 0.65f),
              topLeft = Offset(0f, 3.dp.toPx()),
              size = size,
              cornerRadius = CornerRadius(cr, cr),
            )
          }
          .clip(shape)
          .border(
            1.15.dp,
            Brush.linearGradient(listOf(FrostedPearl.copy(alpha = 0.65f), IceCyan.copy(alpha = 0.7f), WarmGold.copy(alpha = 0.55f))),
            shape,
          ),
      contentAlignment = Alignment.Center,
    ) {
      val imageModel: Any = item.contentUri?.let { Uri.parse(it) } ?: File(item.absolutePath)
      AsyncImage(
        model = imageModel,
        contentDescription = item.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().clip(shape),
      )
      // Subtle top glass sheen over thumbnail
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(FrostedPearl.copy(alpha = 0.16f), Color.Transparent),
              )
            )
      )
    }
  } else {
    LiquidGlassIconOrb(
      icon = FormatUtils.iconForKind(item.kind),
      contentDescription = item.kind.name,
      accent = accent,
      secondaryAccent = if (accent == WarmAmber) WarmGold else ElectricBlue,
      boxSize = boxSize,
      iconSize = boxSize * 0.52f,
      cornerRadius = 14.dp,
      modifier = modifier,
    )
  }
}

@Composable
fun FloatingGlassBottomBar(
  selectedTab: BottomNavTab,
  onSelectTab: (BottomNavTab) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 10.dp),
    contentAlignment = Alignment.Center,
  ) {
    GlassCard(
      modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
      cornerRadius = 30.dp,
      borderAccentStart = IceCyan,
      borderAccentEnd = MoltenAmber,
      fillAlphaTop = 0.30f,
      fillAlphaBottom = 0.18f,
      elevationShadowAlpha = 0.80f,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        BottomNavTab.entries.forEach { tab ->
          val active = tab == selectedTab
          val interactionSource = remember { MutableInteractionSource() }
          val isPressed by interactionSource.collectIsPressedAsState()
          val pressScale by animateFloatAsState(
            targetValue = if (isPressed) 0.92f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "nav_tab_scale",
          )
          val icon =
            when (tab) {
              BottomNavTab.HOME -> if (active) Icons.Filled.Home else Icons.Outlined.Home
              BottomNavTab.ALL_FILES -> if (active) Icons.Filled.Folder else Icons.Outlined.Folder
              BottomNavTab.SEARCH -> if (active) Icons.Filled.Search else Icons.Outlined.Search
              BottomNavTab.VAULT -> if (active) Icons.Filled.Lock else Icons.Outlined.Lock
            }
          val itemTint = if (active) IceCyan else TextMuted
          val pillShape = RoundedCornerShape(22.dp)

          Box(
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .graphicsLayer {
                  scaleX = pressScale
                  scaleY = pressScale
                }
                .clip(pillShape)
                .then(
                  if (active) {
                    Modifier.drawBehind {
                      val cr = 22.dp.toPx()
                      drawRoundRect(
                        brush =
                          Brush.linearGradient(
                            listOf(
                              IceCyan.copy(alpha = 0.28f),
                              DeepSapphire.copy(alpha = 0.24f),
                              WarmGold.copy(alpha = 0.16f),
                            )
                          ),
                        cornerRadius = CornerRadius(cr, cr),
                      )
                      drawRoundRect(
                        brush =
                          Brush.verticalGradient(
                            listOf(FrostedPearl.copy(alpha = 0.26f), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.5f,
                          ),
                        cornerRadius = CornerRadius(cr, cr),
                      )
                    }.border(
                      1.15.dp,
                      Brush.linearGradient(
                        listOf(FrostedPearl.copy(alpha = 0.72f), IceCyan.copy(alpha = 0.88f), WarmGold.copy(alpha = 0.72f))
                      ),
                      pillShape,
                    )
                  } else Modifier
                )
                .clickable(
                  interactionSource = interactionSource,
                  indication = LocalIndication.current,
                  onClick = { onSelectTab(tab) },
                )
                .testTag(tab.tag)
                .padding(vertical = 7.dp),
            contentAlignment = Alignment.Center,
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              Icon(
                imageVector = icon,
                contentDescription = tab.label,
                tint = itemTint,
                modifier = Modifier.size(22.dp),
              )
              Text(
                text = tab.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) TextPrimary else TextMuted,
                maxLines = 1,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun GlassNavigationRail(
  selectedTab: BottomNavTab,
  onSelectTab: (BottomNavTab) -> Unit,
  modifier: Modifier = Modifier,
) {
  GlassCard(
    modifier = modifier.fillMaxHeight().padding(12.dp).width(92.dp),
    cornerRadius = 28.dp,
    borderAccentStart = IceCyan,
    borderAccentEnd = MoltenAmber,
    elevationShadowAlpha = 0.78f,
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(vertical = 18.dp, horizontal = 8.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      BottomNavTab.entries.forEach { tab ->
        val active = tab == selectedTab
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val pressScale by animateFloatAsState(
          targetValue = if (isPressed) 0.93f else 1f,
          animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
          label = "rail_tab_scale",
        )
        val icon =
          when (tab) {
            BottomNavTab.HOME -> if (active) Icons.Filled.Home else Icons.Outlined.Home
            BottomNavTab.ALL_FILES -> if (active) Icons.Filled.Folder else Icons.Outlined.Folder
            BottomNavTab.SEARCH -> if (active) Icons.Filled.Search else Icons.Outlined.Search
            BottomNavTab.VAULT -> if (active) Icons.Filled.Lock else Icons.Outlined.Lock
          }
        val pillShape = RoundedCornerShape(18.dp)
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
              }
              .clip(pillShape)
              .then(
                if (active) {
                  Modifier.drawBehind {
                    val cr = 18.dp.toPx()
                    drawRoundRect(
                      brush = Brush.linearGradient(listOf(IceCyan.copy(alpha = 0.28f), WarmGold.copy(alpha = 0.16f))),
                      cornerRadius = CornerRadius(cr, cr),
                    )
                    drawRoundRect(
                      brush = Brush.verticalGradient(listOf(FrostedPearl.copy(alpha = 0.24f), Color.Transparent), startY = 0f, endY = size.height * 0.5f),
                      cornerRadius = CornerRadius(cr, cr),
                    )
                  }.border(
                    1.15.dp,
                    Brush.linearGradient(listOf(FrostedPearl.copy(alpha = 0.72f), IceCyan, WarmGold.copy(alpha = 0.7f))),
                    pillShape,
                  )
                } else Modifier
              )
              .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = { onSelectTab(tab) },
              )
              .testTag(tab.tag)
              .padding(vertical = 10.dp, horizontal = 4.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = icon,
            contentDescription = tab.label,
            tint = if (active) IceCyan else TextMuted,
            modifier = Modifier.size(22.dp),
          )
          Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) TextPrimary else TextMuted,
            maxLines = 1,
          )
        }
      }
    }
  }
}

/**
 * 3D Liquid Glass Orb Checkbox (minimum 48dp accessible touch target).
 */
@Composable
fun LiquidGlassCheckboxOrb(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  accent: Color = IceCyan,
  contentDescription: String = if (checked) "Checked" else "Unchecked",
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.88f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "checkbox_orb_scale",
  )

  Box(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .size(48.dp)
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
        .clickable(
          interactionSource = interactionSource,
          indication = LocalIndication.current,
          onClick = { onCheckedChange(!checked) },
        ),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
        Modifier.size(26.dp)
          .drawBehind {
            drawCircle(
              color = Color.Black.copy(alpha = 0.6f),
              radius = size.minDimension * 0.56f,
              center = Offset(size.width / 2f, size.height / 2f + 2.dp.toPx()),
            )
            if (checked) {
              drawCircle(
                brush = Brush.radialGradient(listOf(accent.copy(alpha = 0.36f), Color.Transparent)),
                radius = size.minDimension * 0.82f,
              )
            }
          }
          .clip(CircleShape)
          .background(
            if (checked) {
              Brush.radialGradient(
                colors = listOf(FrostedPearl.copy(alpha = 0.35f), accent.copy(alpha = 0.55f), DeepSapphire.copy(alpha = 0.72f)),
                center = Offset(10f, 8f),
              )
            } else {
              Brush.radialGradient(
                colors = listOf(FrostedPearl.copy(alpha = 0.22f), Color(0xFF0E3744).copy(alpha = 0.42f)),
                center = Offset(10f, 8f),
              )
            }
          )
          .border(
            width = if (checked) 1.5.dp else 1.15.dp,
            brush =
              Brush.linearGradient(
                if (checked) listOf(FrostedPearl.copy(alpha = 0.88f), accent, LiquidEmerald)
                else listOf(FrostedPearl.copy(alpha = 0.45f), accent.copy(alpha = 0.35f))
              ),
            shape = CircleShape,
          ),
      contentAlignment = Alignment.Center,
    ) {
      if (checked) {
        Icon(
          imageVector = Icons.Rounded.Check,
          contentDescription = contentDescription,
          tint = FrostedPearl,
          modifier = Modifier.size(16.dp),
        )
      }
    }
  }
}

/**
 * Custom 3D Liquid Glass Capsule Seekbar with glowing luminous fill & Frosted Pearl spherical orb thumb.
 */
@Composable
fun LiquidGlassScrubBar(
  valueFraction: Float,
  onSeekFraction: (Float) -> Unit,
  modifier: Modifier = Modifier,
  accentStart: Color = IceCyan,
  accentEnd: Color = WarmGold,
) {
  val clamped = valueFraction.coerceIn(0f, 1f)
  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .height(34.dp)
        .pointerInput(Unit) {
          detectTapGestures { offset ->
            if (size.width > 0) {
              onSeekFraction((offset.x / size.width.toFloat()).coerceIn(0f, 1f))
            }
          }
        }
        .pointerInput(Unit) {
          detectHorizontalDragGestures { change, _ ->
            change.consume()
            if (size.width > 0) {
              onSeekFraction((change.position.x / size.width.toFloat()).coerceIn(0f, 1f))
            }
          }
        },
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val trackHeight = 8.dp.toPx()
      val cy = size.height / 2f
      val thumbRadius = 9.5.dp.toPx()
      val padX = thumbRadius
      val usableWidth = (size.width - padX * 2f).coerceAtLeast(1f)
      val fillWidth = usableWidth * clamped
      val thumbX = padX + fillWidth

      // 1. Translucent beveled glass capsule track
      drawRoundRect(
        color = FrostedPearl.copy(alpha = 0.22f),
        topLeft = Offset(padX, cy - trackHeight / 2f),
        size = Size(usableWidth, trackHeight),
        cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f),
      )
      drawRoundRect(
        brush = Brush.linearGradient(listOf(FrostedPearl.copy(alpha = 0.45f), accentStart.copy(alpha = 0.25f))),
        topLeft = Offset(padX, cy - trackHeight / 2f),
        size = Size(usableWidth, trackHeight),
        cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f),
        style = Stroke(width = 1.dp.toPx()),
      )

      // 2. Active luminous liquid fill
      if (fillWidth > 1f) {
        drawRoundRect(
          brush = Brush.horizontalGradient(listOf(accentStart, LiquidEmerald, accentEnd)),
          topLeft = Offset(padX, cy - trackHeight / 2f),
          size = Size(fillWidth, trackHeight),
          cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f),
        )
        // Top specular glint strip along active liquid
        drawRoundRect(
          color = FrostedPearl.copy(alpha = 0.35f),
          topLeft = Offset(padX, cy - trackHeight / 2f),
          size = Size(fillWidth, trackHeight * 0.42f),
          cornerRadius = CornerRadius(trackHeight / 4f, trackHeight / 4f),
        )
      }

      // 3. Floating drop shadow + convex Frosted Pearl thumb orb
      drawCircle(
        color = Color.Black.copy(alpha = 0.65f),
        radius = thumbRadius * 1.15f,
        center = Offset(thumbX, cy + 2.dp.toPx()),
      )
      drawCircle(
        brush = Brush.radialGradient(listOf(accentEnd.copy(alpha = 0.45f), Color.Transparent)),
        radius = thumbRadius * 1.75f,
        center = Offset(thumbX, cy),
      )
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(FrostedPearl, accentEnd, MoltenAmber),
            center = Offset(thumbX - thumbRadius * 0.32f, cy - thumbRadius * 0.32f),
            radius = thumbRadius * 1.3f,
          ),
        radius = thumbRadius,
        center = Offset(thumbX, cy),
      )
      drawCircle(
        color = FrostedPearl.copy(alpha = 0.85f),
        radius = thumbRadius,
        center = Offset(thumbX, cy),
        style = Stroke(width = 1.2.dp.toPx()),
      )
    }
  }
}
