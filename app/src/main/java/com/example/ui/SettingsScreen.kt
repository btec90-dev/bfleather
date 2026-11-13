package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CloudSyncSummary
import com.example.i18n.AppLanguage
import com.example.i18n.Strings
import com.example.ui.theme.AppThemePalette
import com.example.ui.theme.ThemeColors
import com.example.ui.theme.getThemeColors

@Composable
fun SettingsScreen(
  userEmail: String,
  syncSummary: CloudSyncSummary,
  momAvatar: String,
  partnerAvatar: String,
  babyAvatar: String,
  onAvatarsChanged: (mom: String, partner: String, baby: String) -> Unit,
  currentPalette: AppThemePalette,
  onPaletteChanged: (AppThemePalette) -> Unit,
  currentLanguage: AppLanguage,
  onLanguageChanged: (AppLanguage) -> Unit,
  onInitiateSync: () -> Unit,
  onSignOut: () -> Unit,
  theme: ThemeColors
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(theme.background)
      .padding(horizontal = 20.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
    contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp)
  ) {
    item {
      Column {
        Text(
          text = Strings.t("settings_tab", currentLanguage),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.SemiBold,
          color = theme.primaryText
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = if (currentLanguage == AppLanguage.FR)
            "Personnalisation, Famille & Synchronisation Cloud"
          else
            "Customization, Family & Cloud Sync",
          style = MaterialTheme.typography.bodySmall,
          color = theme.mutedText
        )
      }
    }

    // 1. Live Sync Status Dashboard & Account Sign-Out
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SyncStatusDashboardCard(
          userEmail = userEmail,
          syncSummary = syncSummary,
          onInitiateSync = onInitiateSync,
          theme = theme,
          language = currentLanguage
        )

        OutlinedButton(
          onClick = onSignOut,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("sign_out_button"),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, theme.border),
          colors = ButtonDefaults.outlinedButtonColors(containerColor = theme.cardSurface)
        ) {
          Text(
            text = "${Strings.t("sign_out", currentLanguage)} ($userEmail)",
            style = MaterialTheme.typography.bodySmall,
            color = theme.primaryText,
            fontWeight = FontWeight.Medium
          )
        }
      }
    }

    // 2. Family & Profile Avatars Card (Synced via Firestore)
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = theme.cardSurface,
        border = BorderStroke(1.dp, theme.border)
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Face, contentDescription = null, tint = theme.accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = Strings.t("profile_avatars", currentLanguage),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = theme.primaryText
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Text(
            text = "${Strings.t("mom_avatar", currentLanguage)} : $momAvatar",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = theme.primaryText
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf("🌸", "🤰", "👑", "🌿", "✨").forEach { avatar ->
              val isSel = momAvatar == avatar
              Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = if (isSel) theme.accent.copy(alpha = 0.2f) else theme.background,
                border = BorderStroke(1.dp, if (isSel) theme.accent else theme.border),
                onClick = { onAvatarsChanged(avatar, partnerAvatar, babyAvatar) }
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(text = avatar, fontSize = 20.sp)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = "${Strings.t("partner_avatar", currentLanguage)} : $partnerAvatar",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = theme.primaryText
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf("💙", "👔", "🌟", "🕊️", "☕").forEach { avatar ->
              val isSel = partnerAvatar == avatar
              Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = if (isSel) theme.accent.copy(alpha = 0.2f) else theme.background,
                border = BorderStroke(1.dp, if (isSel) theme.accent else theme.border),
                onClick = { onAvatarsChanged(momAvatar, avatar, babyAvatar) }
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(text = avatar, fontSize = 20.sp)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = "${Strings.t("baby_avatar", currentLanguage)} : $babyAvatar",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = theme.primaryText
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf("👶", "🍼", "🧸", "🐣", "⭐").forEach { avatar ->
              val isSel = babyAvatar == avatar
              Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = if (isSel) theme.accent.copy(alpha = 0.2f) else theme.background,
                border = BorderStroke(1.dp, if (isSel) theme.accent else theme.border),
                onClick = { onAvatarsChanged(momAvatar, partnerAvatar, avatar) }
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(text = avatar, fontSize = 20.sp)
                }
              }
            }
          }
        }
      }
    }

    // 3. Language Selection Card
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = theme.cardSurface,
        border = BorderStroke(1.dp, theme.border)
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Translate, contentDescription = null, tint = theme.accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = Strings.t("language_selection", currentLanguage),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = theme.primaryText
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
              selected = currentLanguage == AppLanguage.FR,
              onClick = { onLanguageChanged(AppLanguage.FR) },
              label = { Text("Français (FR)") },
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = theme.accent.copy(alpha = 0.15f),
                selectedLabelColor = theme.primaryText
              ),
              border = FilterChipDefaults.filterChipBorder(
                borderColor = theme.border,
                selectedBorderColor = theme.accent,
                enabled = true,
                selected = currentLanguage == AppLanguage.FR
              )
            )
            FilterChip(
              selected = currentLanguage == AppLanguage.EN,
              onClick = { onLanguageChanged(AppLanguage.EN) },
              label = { Text("English (EN)") },
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = theme.accent.copy(alpha = 0.15f),
                selectedLabelColor = theme.primaryText
              ),
              border = FilterChipDefaults.filterChipBorder(
                borderColor = theme.border,
                selectedBorderColor = theme.accent,
                enabled = true,
                selected = currentLanguage == AppLanguage.EN
              )
            )
          }
        }
      }
    }

    // 4. Theme Selection Card
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = theme.cardSurface,
        border = BorderStroke(1.dp, theme.border)
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Palette, contentDescription = null, tint = theme.accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = Strings.t("theme_selection", currentLanguage),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = theme.primaryText
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppThemePalette.entries.forEach { palette ->
              val isSelected = currentPalette == palette
              val paletteName = if (currentLanguage == AppLanguage.FR) palette.displayNameFr else palette.displayNameEn
              val pColors = getThemeColors(palette)

              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) theme.accent.copy(alpha = 0.12f) else theme.background,
                border = BorderStroke(1.dp, if (isSelected) theme.accent else theme.border),
                onClick = { onPaletteChanged(palette) }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                      modifier = Modifier.size(16.dp),
                      shape = CircleShape,
                      color = pColors.accent,
                      border = BorderStroke(0.5.dp, pColors.border)
                    ) {}
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                      modifier = Modifier.size(16.dp),
                      shape = CircleShape,
                      color = pColors.cardSurface,
                      border = BorderStroke(0.5.dp, pColors.border)
                    ) {}
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                      text = paletteName,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                      color = theme.primaryText
                    )
                  }

                  if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = theme.accent, modifier = Modifier.size(16.dp))
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
