package com.example.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import com.example.data.local.VaultItemEntity
import com.example.data.storage.KronoStorageRepository
import com.example.model.BiometricVisualState
import com.example.model.KronoFileItem
import com.example.ui.components.GlassBubbleButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlassTextInputDialog
import com.example.ui.components.LiquidGlassActionButton
import com.example.ui.components.LiquidGlassFrostedButton
import com.example.ui.components.LiquidGlassIconOrb
import com.example.ui.theme.DeepSapphire
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.FrostedPearl
import com.example.ui.theme.IceCyan
import com.example.ui.theme.MoltenAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PrismaticViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmGold
import com.example.util.FormatUtils
import com.example.viewmodel.VaultUiState

import kotlinx.coroutines.delay

@Composable
fun SafeVaultScreen(
  vaultState: VaultUiState,
  vaultItems: List<VaultItemEntity>,
  availableStorageFiles: List<KronoFileItem>,
  exitOnAuthFail: Boolean,
  onPinDigit: (Char) -> Unit,
  onPinBackspace: () -> Unit,
  onBiometricSuccess: () -> Unit,
  onBiometricFailure: (String, Boolean) -> Unit,
  onResetBiometricVisual: () -> Unit,
  onExitOnFailToggle: (Boolean) -> Unit,
  onLockVaultNow: () -> Unit,
  onLockFileIntoVault: (KronoFileItem) -> Unit,
  onUnlockVaultItem: (VaultItemEntity) -> Unit,
  onDeleteVaultItem: (VaultItemEntity) -> Unit,
  onOpenVaultItem: (VaultItemEntity) -> Unit,
  onUpdatePin: (String) -> Unit,
  onConsumeExitOnAuthFail: () -> Unit,
  onCloseVaultFlowOnAuthFail: () -> Unit,
  hasBottomBar: Boolean = true,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var showAddFilePicker by remember { mutableStateOf(false) }
  var showChangePinDialog by remember { mutableStateOf(false) }
  var autoPromptTriggered by remember { mutableStateOf(false) }

  fun launchBiometricPrompt() {
    val activity = context as? FragmentActivity ?: return
    val manager = BiometricManager.from(activity)
    val canAuth =
      manager.canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
      )
    if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
      onResetBiometricVisual()
      return
    }
    onResetBiometricVisual()
    val executor = ContextCompat.getMainExecutor(activity)
    var promptInstance: BiometricPrompt? = null
    val prompt =
      BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
          override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onBiometricSuccess()
          }

          override fun onAuthenticationFailed() {
            if (exitOnAuthFail) {
              promptInstance?.cancelAuthentication()
              onBiometricFailure("Fingerprint rejected — Exiting KronoFiles", true)
            } else {
              onBiometricFailure("Fingerprint mismatch — try side sensor again", false)
            }
          }

          override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            val userFallback =
              errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                errorCode == BiometricPrompt.ERROR_CANCELED ||
                errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS ||
                errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT ||
                errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE
            if (userFallback) {
              onResetBiometricVisual()
            } else {
              val shouldExit = exitOnAuthFail
              onBiometricFailure(errString.toString(), shouldExit)
            }
          }
        },
      )
    promptInstance = prompt
    val info =
      BiometricPrompt.PromptInfo.Builder()
        .setTitle("KronoFiles Safe Vault")
        .setSubtitle("Touch the side-mounted fingerprint sensor to unlock")
        .setNegativeButtonText("Use PIN Keypad")
        .build()
    prompt.authenticate(info)
  }

  LaunchedEffect(vaultState.isUnlocked, vaultState.hasPinConfigured) {
    if (!vaultState.isUnlocked) {
      onResetBiometricVisual()
      if (!autoPromptTriggered && vaultState.hasPinConfigured) {
        autoPromptTriggered = true
        launchBiometricPrompt()
      }
    } else {
      autoPromptTriggered = false
    }
  }

  LaunchedEffect(vaultState.exitRequestedOnAuthFail) {
    if (vaultState.exitRequestedOnAuthFail) {
      delay(650L)
      onConsumeExitOnAuthFail()
      onCloseVaultFlowOnAuthFail()
    }
  }

  val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

  if (!vaultState.isUnlocked) {
    // 1. LOCKED / AUTHENTICATION VIEW
    Column(
      modifier =
        modifier
          .fillMaxSize()
          .statusBarsPadding()
          .verticalScroll(rememberScrollState())
          .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = navBottom + if (hasBottomBar) 136.dp else 28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Security Header
      Row(
        modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          GlassBubbleButton(
            icon = Icons.Rounded.Shield,
            contentDescription = "Safe Vault Shield",
            onClick = {},
            tint = NeonCyan,
            accentStart = NeonCyan,
            accentEnd = NeonViolet,
          )
          Column {
            Text("Safe Vault", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
            Text("Hardware Biometric & PIN Lock", style = MaterialTheme.typography.bodySmall, color = NeonViolet)
          }
        }
        GlassChip(
          label = if (exitOnAuthFail) "Auto-Exit: ON" else "Retry: ON",
          selected = exitOnAuthFail,
          onClick = { onExitOnFailToggle(!exitOnAuthFail) },
          accentColor = WarmAmber,
        )
      }

      // Side-Sensor Biometric Visual Indicator on Display
      SideSensorFingerprintIndicator(
        visualState = vaultState.biometricState,
        statusMessage = vaultState.statusMessage,
        onTapTriggerSensor = { launchBiometricPrompt() },
        modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
      )

      // Glassmorphism PIN Keypad
      GlassCard(
        modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
        cornerRadius = 24.dp,
        borderAccentStart = NeonViolet.copy(alpha = 0.55f),
        borderAccentEnd = NeonCyan.copy(alpha = 0.45f),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text =
              when {
                !vaultState.hasPinConfigured && vaultState.pinSetupFirstEntry == null ->
                  "Create 4-Digit Vault PIN"
                !vaultState.hasPinConfigured && vaultState.pinSetupFirstEntry != null ->
                  "Confirm 4-Digit Vault PIN"
                else -> "Enter 4-Digit Vault PIN"
              },
            style = MaterialTheme.typography.titleSmall,
            color = if (!vaultState.hasPinConfigured) IceCyan else TextSecondary,
          )
          // 3D Liquid Glass PIN Dot Orbs
          Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(4) { idx ->
              val filled = idx < vaultState.pinInput.length
              Box(
                modifier =
                  Modifier.size(18.dp)
                    .drawBehind {
                      if (filled) {
                        drawCircle(
                          brush = Brush.radialGradient(listOf(IceCyan.copy(alpha = 0.45f), Color.Transparent)),
                          radius = size.minDimension * 0.9f,
                        )
                      }
                    }
                    .clip(CircleShape)
                    .background(
                      if (filled) {
                        Brush.radialGradient(
                          listOf(FrostedPearl, IceCyan, DeepSapphire),
                          center = Offset(5f, 5f),
                        )
                      } else {
                        Brush.radialGradient(
                          listOf(FrostedPearl.copy(alpha = 0.20f), Color(0xFF0E3744).copy(alpha = 0.48f)),
                          center = Offset(5f, 5f),
                        )
                      }
                    )
                    .border(
                      1.4.dp,
                      if (filled) Brush.linearGradient(listOf(FrostedPearl, IceCyan))
                      else Brush.linearGradient(listOf(FrostedPearl.copy(alpha = 0.48f), IceCyan.copy(alpha = 0.35f))),
                      CircleShape,
                    )
              )
            }
          }

          val rows = listOf(listOf('1', '2', '3'), listOf('4', '5', '6'), listOf('7', '8', '9'))
          rows.forEach { row ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
              row.forEach { digit ->
                PinGlassKey(label = digit.toString(), onClick = { onPinDigit(digit) })
              }
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            GlassBubbleButton(
              icon = Icons.Rounded.Fingerprint,
              contentDescription = "Scan Fingerprint",
              onClick = { launchBiometricPrompt() },
              size = 58.dp,
              iconSize = 26.dp,
              tint = NeonCyan,
              modifier = Modifier.testTag("vault_biometric_trigger"),
            )
            PinGlassKey(label = "0", onClick = { onPinDigit('0') })
            GlassBubbleButton(
              icon = Icons.AutoMirrored.Rounded.Backspace,
              contentDescription = "Backspace PIN",
              onClick = onPinBackspace,
              size = 58.dp,
              iconSize = 24.dp,
              tint = WarmAmber,
              modifier = Modifier.testTag("vault_pin_backspace"),
            )
          }
          Text(
            text =
              if (vaultState.hasPinConfigured) "PIN is hashed offline with SHA-256"
              else "Set your personal 4-digit PIN to protect encrypted files",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            textAlign = TextAlign.Center,
          )
        }
      }
    }
  } else {
    // 2. UNLOCKED VAULT VIEW
    Column(
      modifier =
        modifier
          .fillMaxSize()
          .statusBarsPadding()
          .widthIn(max = 980.dp)
          .padding(top = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text("Safe Vault Unlocked", style = MaterialTheme.typography.headlineSmall, color = NeonGreen)
          Text("${vaultItems.size} private file(s) isolated", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          GlassBubbleButton(
            icon = Icons.Rounded.Pin,
            contentDescription = "Change PIN",
            onClick = { showChangePinDialog = true },
            tint = NeonCyan,
          )
          GlassBubbleButton(
            icon = Icons.Rounded.Add,
            contentDescription = "Add File to Vault",
            onClick = { showAddFilePicker = true },
            tint = WarmAmber,
            modifier = Modifier.testTag("vault_add_file"),
          )
          GlassBubbleButton(
            icon = Icons.Rounded.Lock,
            contentDescription = "Lock Vault Immediately",
            onClick = onLockVaultNow,
            tint = NeonRed,
            accentStart = NeonRed,
            accentEnd = NeonViolet,
            modifier = Modifier.testTag("vault_lock_now"),
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      if (vaultItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
          GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(28.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(Icons.Rounded.LockOpen, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(40.dp))
              Text("Vault is Empty", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
              Text(
                "Tap + above or choose 'Lock to Safe Vault' from any file's menu.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
              )
              LiquidGlassActionButton(
                onClick = { showAddFilePicker = true },
                accentStart = IceCyan,
                accentEnd = DeepSapphire,
              ) {
                Text("Add Files Now", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = navBottom + if (hasBottomBar) 136.dp else 28.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(vaultItems, key = { it.id }) { item ->
            val kind = KronoStorageRepository.classifyExtension(item.extension)
            val accent = FormatUtils.accentForKind(kind)
            GlassCard(
              modifier = Modifier.fillMaxWidth(),
              cornerRadius = 18.dp,
              borderAccentStart = accent.copy(alpha = 0.65f),
              borderAccentEnd = PrismaticViolet.copy(alpha = 0.45f),
              onClick = { onOpenVaultItem(item) },
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                LiquidGlassIconOrb(
                  icon = FormatUtils.iconForKind(kind),
                  contentDescription = null,
                  accent = accent,
                  secondaryAccent = if (accent == WarmAmber) WarmGold else ElectricBlue,
                  boxSize = 46.dp,
                  iconSize = 24.dp,
                  cornerRadius = 14.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    item.fileName,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    "${FormatUtils.formatBytes(item.sizeBytes)} • From ${item.originalRelativePath}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                GlassBubbleButton(
                  icon = Icons.Rounded.LockOpen,
                  contentDescription = "Unlock & Export back to Storage",
                  onClick = { onUnlockVaultItem(item) },
                  tint = NeonCyan,
                )
                GlassBubbleButton(
                  icon = Icons.Rounded.Delete,
                  contentDescription = "Delete from Vault",
                  onClick = { onDeleteVaultItem(item) },
                  tint = NeonRed,
                )
              }
            }
          }
        }
      }
    }
  }

  if (showAddFilePicker) {
    Dialog(onDismissRequest = { showAddFilePicker = false }) {
      GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        fillAlphaTop = 0.22f,
        fillAlphaBottom = 0.1f,
      ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Lock File into Safe Vault", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
          LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(availableStorageFiles, key = { it.id }) { file ->
              val accent = FormatUtils.accentForKind(file.kind)
              GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                borderAccentStart = accent.copy(alpha = 0.55f),
                borderAccentEnd = IceCyan.copy(alpha = 0.4f),
                fillAlphaTop = 0.16f,
                fillAlphaBottom = 0.08f,
                onClick = {
                  showAddFilePicker = false
                  onLockFileIntoVault(file)
                },
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  LiquidGlassIconOrb(
                    icon = FormatUtils.iconForKind(file.kind),
                    contentDescription = null,
                    accent = accent,
                    boxSize = 38.dp,
                    iconSize = 20.dp,
                    cornerRadius = 12.dp,
                  )
                  Column(modifier = Modifier.weight(1f)) {
                    Text(file.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(file.relativePath, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  }
                }
              }
            }
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            LiquidGlassFrostedButton(onClick = { showAddFilePicker = false }) {
              Text("Close", color = TextPrimary)
            }
          }
        }
      }
    }
  }

  if (showChangePinDialog) {
    GlassTextInputDialog(
      title = "Update 4-Digit Vault PIN",
      label = "4 Digits",
      initialValue = "",
      confirmLabel = "Save PIN",
      onConfirm = { pin ->
        showChangePinDialog = false
        onUpdatePin(pin)
      },
      onDismiss = { showChangePinDialog = false },
    )
  }
}

@Composable
private fun SideSensorFingerprintIndicator(
  visualState: BiometricVisualState,
  statusMessage: String,
  onTapTriggerSensor: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val infinite = rememberInfiniteTransition(label = "bio_pulse")
  val pulse by infinite.animateFloat(
    initialValue = 0.75f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
    label = "pulse",
  )

  val activeColor by animateColorAsState(
    targetValue =
      when (visualState) {
        BiometricVisualState.IDLE_SCANNING -> NeonCyan
        BiometricVisualState.SUCCESS -> NeonGreen
        BiometricVisualState.FAILURE -> NeonRed
      },
    label = "bio_color",
  )

  val displayIcon =
    when (visualState) {
      BiometricVisualState.IDLE_SCANNING -> Icons.Rounded.Fingerprint
      BiometricVisualState.SUCCESS -> Icons.Rounded.Check
      BiometricVisualState.FAILURE -> Icons.Rounded.Close
    }

  GlassCard(
    modifier = modifier.testTag("vault_biometric_card"),
    cornerRadius = 24.dp,
    borderAccentStart = activeColor.copy(alpha = 0.75f),
    borderAccentEnd = NeonViolet.copy(alpha = 0.5f),
    onClick = onTapTriggerSensor,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Box(
        modifier = Modifier.size(104.dp),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val radius = size.minDimension / 2f
          val center = Offset(size.width / 2f, size.height / 2f)
          val domeRadius = radius * 0.84f
          // Floating drop shadow
          drawCircle(
            brush = Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.76f), Color.Transparent)),
            radius = domeRadius * 1.18f,
            center = Offset(center.x, center.y + 4.dp.toPx()),
          )
          // Pulsing outer refraction halo
          drawCircle(
            brush = Brush.radialGradient(listOf(activeColor.copy(alpha = 0.34f * pulse), Color.Transparent)),
            radius = radius,
            center = center,
          )
          // Convex 3D Liquid Glass orb fill
          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    FrostedPearl.copy(alpha = 0.36f),
                    activeColor.copy(alpha = 0.36f),
                    Color(0xFF194B5C).copy(alpha = 0.50f),
                    Color(0xFF0A2B36).copy(alpha = 0.56f),
                  ),
                center = Offset(size.width * 0.34f, size.height * 0.30f),
                radius = domeRadius * 1.4f,
              ),
            radius = domeRadius,
            center = center,
          )
          // Crisp top-left Frosted Pearl specular crescent
          drawArc(
            brush = Brush.linearGradient(listOf(FrostedPearl.copy(alpha = 0.78f), FrostedPearl.copy(alpha = 0.08f))),
            startAngle = 192f,
            sweepAngle = 92f,
            useCenter = false,
            topLeft = Offset(center.x - domeRadius * 0.88f, center.y - domeRadius * 0.88f),
            size = androidx.compose.ui.geometry.Size(domeRadius * 1.76f, domeRadius * 1.76f),
            style = Stroke(width = 2.6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
          )
          // Outer double-bevel refractive rim
          drawCircle(
            brush = Brush.linearGradient(listOf(FrostedPearl.copy(alpha = 0.72f), activeColor, NeonViolet)),
            radius = domeRadius,
            center = center,
            style = Stroke(width = 2.8.dp.toPx()),
          )
        }
        Icon(
          imageVector = displayIcon,
          contentDescription = "Side Fingerprint Sensor Status",
          tint = activeColor,
          modifier = Modifier.size(50.dp),
        )
      }
      Text(
        text =
          when (visualState) {
            BiometricVisualState.IDLE_SCANNING -> "SIDE SENSOR ACTIVE"
            BiometricVisualState.SUCCESS -> "BIOMETRIC VERIFIED"
            BiometricVisualState.FAILURE -> "AUTHENTICATION FAILED"
          },
        style = MaterialTheme.typography.labelLarge,
        color = activeColor,
      )
      Text(
        text = statusMessage,
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun PinGlassKey(label: String, onClick: () -> Unit) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val pressScale by animateFloatAsState(
    targetValue = if (isPressed) 0.90f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
    label = "pin_key_scale",
  )
  val pressHighlight by animateFloatAsState(
    targetValue = if (isPressed) 1f else 0f,
    animationSpec = tween(110),
    label = "pin_key_highlight",
  )

  Box(
    modifier =
      Modifier.minimumInteractiveComponentSize()
        .size(58.dp)
        .graphicsLayer {
          scaleX = pressScale
          scaleY = pressScale
        }
        .drawBehind {
          val radius = size.minDimension / 2f
          val center = Offset(size.width / 2f, size.height / 2f)
          // Floating drop shadow
          drawCircle(
            brush = Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)),
            radius = radius * 1.2f,
            center = Offset(center.x, center.y + 4.dp.toPx() * (1f - 0.35f * pressHighlight)),
          )
          // Subtle cyan-amber refraction aura on press
          drawCircle(
            brush =
              Brush.radialGradient(
                listOf(
                  IceCyan.copy(alpha = 0.14f + 0.22f * pressHighlight),
                  WarmGold.copy(alpha = 0.08f + 0.12f * pressHighlight),
                  Color.Transparent,
                )
              ),
            radius = radius * 1.24f,
            center = center,
          )
        }
        .clip(CircleShape)
        .background(Color(0xFF0A2B36).copy(alpha = 0.42f))
        .drawBehind {
          val r = size.minDimension / 2f
          val center = Offset(size.width / 2f, size.height / 2f)
          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    FrostedPearl.copy(alpha = 0.34f + 0.16f * pressHighlight),
                    IceCyan.copy(alpha = 0.24f),
                    Color(0xFF194B5C).copy(alpha = 0.50f),
                    Color(0xFF092834).copy(alpha = 0.62f),
                  ),
                center = Offset(size.width * 0.33f, size.height * 0.28f),
                radius = r * 1.4f,
              ),
            radius = r,
            center = center,
          )
          // Top-left Frosted Pearl specular meniscus
          drawArc(
            color = FrostedPearl.copy(alpha = 0.65f + 0.2f * pressHighlight),
            startAngle = 198f,
            sweepAngle = 86f,
            useCenter = false,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
            size = Size(size.width - 6.dp.toPx(), size.height - 6.dp.toPx()),
          )
          // Bottom-right warm gold edge bounce
          drawArc(
            color = WarmGold.copy(alpha = 0.30f + 0.25f * pressHighlight),
            startAngle = 20f,
            sweepAngle = 68f,
            useCenter = false,
            style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round),
            topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
            size = Size(size.width - 6.dp.toPx(), size.height - 6.dp.toPx()),
          )
        }
        .border(
          1.2.dp,
          Brush.linearGradient(listOf(FrostedPearl.copy(alpha = 0.65f), IceCyan.copy(alpha = 0.78f), WarmGold.copy(alpha = 0.65f))),
          CircleShape,
        )
        .clickable(
          interactionSource = interactionSource,
          indication = LocalIndication.current,
          onClick = onClick,
        )
        .testTag("pin_key_$label"),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.headlineSmall,
      color = TextPrimary,
    )
  }
}
