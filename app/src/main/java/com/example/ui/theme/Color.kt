package com.example.ui.theme

import androidx.compose.ui.graphics.Color

enum class AppThemePalette(val displayNameEn: String, val displayNameFr: String) {
  CARAMEL("1. Caramel (Warm & Sweet)", "1. Caramel (Doux & Chaleureux)"),
  MARRON("2. Chestnut (Deep Earth)", "2. Marron (Terre Riche)"),
  ROSE("3. Rose (Pearl & Blush)", "3. Rose (Perle & Poudré)"),
  SAUGE("4. Sage (Botanical Calm)", "4. Sauge (Végétal & Sérénité)"),
  LAVANDE("5. Lavender (Soft Floral)", "5. Lavande (Apaisant & Doux)"),
  SUNSET("6. Sunset (Coral Glow)", "6. Coucher de Soleil (Corail Chaud)"),
  SLATE("7. Slate (Clean Minimal)", "7. Ardoise (Moderne & Épuré)"),
  OCEAN("8. Ocean (Breeze & Sky)", "8. Océan (Brise & Azur)")
}

data class ThemeColors(
  val background: Color,
  val cardSurface: Color,
  val headerFooterBg: Color,
  val primaryText: Color,
  val mutedText: Color,
  val border: Color,
  val accent: Color
)

fun getThemeColors(palette: AppThemePalette): ThemeColors {
  return when (palette) {
    AppThemePalette.CARAMEL -> ThemeColors(
      background = Color(0xFFFDFBF7),
      cardSurface = Color(0xFFFAF4EB),
      headerFooterBg = Color(0xFFFAF1E4),
      primaryText = Color(0xFF5C4736),
      mutedText = Color(0xFF9E8775),
      border = Color(0xFFEFE4D6),
      accent = Color(0xFFC67D34) // Warm honey caramel
    )
    AppThemePalette.MARRON -> ThemeColors(
      background = Color(0xFFFAF8F6),
      cardSurface = Color(0xFFF3ECE6),
      headerFooterBg = Color(0xFFEFE5DC),
      primaryText = Color(0xFF4E382A),
      mutedText = Color(0xFF8F7666),
      border = Color(0xFFE5D7CC),
      accent = Color(0xFF8D532F) // Rich chestnut brown
    )
    AppThemePalette.ROSE -> ThemeColors(
      background = Color(0xFFFCF9F9),
      cardSurface = Color(0xFFF7EFF0),
      headerFooterBg = Color(0xFFF4E7E8),
      primaryText = Color(0xFF5B454A),
      mutedText = Color(0xFFA1858C),
      border = Color(0xFFEEDADB),
      accent = Color(0xFFD66D81) // Soft rosewood blush
    )
    AppThemePalette.SAUGE -> ThemeColors(
      background = Color(0xFFF8FAF7),
      cardSurface = Color(0xFFEFF3EE),
      headerFooterBg = Color(0xFFE7ECE6),
      primaryText = Color(0xFF3F4E40),
      mutedText = Color(0xFF7F9081),
      border = Color(0xFFDCE3DC),
      accent = Color(0xFF5F8261) // Soft olive sage
    )
    AppThemePalette.LAVANDE -> ThemeColors(
      background = Color(0xFFF9F8FD),
      cardSurface = Color(0xFFF1EFFF),
      headerFooterBg = Color(0xFFEAE7FC),
      primaryText = Color(0xFF4A4562),
      mutedText = Color(0xFF8A84A6),
      border = Color(0xFFE0DDF7),
      accent = Color(0xFF7E72B5) // Calming lavender
    )
    AppThemePalette.SUNSET -> ThemeColors(
      background = Color(0xFFFDF9F7),
      cardSurface = Color(0xFFFBF0EA),
      headerFooterBg = Color(0xFFF7E7DE),
      primaryText = Color(0xFF5A4138),
      mutedText = Color(0xFFA17F72),
      border = Color(0xFFEED8CD),
      accent = Color(0xFFD46949) // Soft terracotta sunset
    )
    AppThemePalette.SLATE -> ThemeColors(
      background = Color(0xFFF8FAFC),
      cardSurface = Color(0xFFF1F5F9),
      headerFooterBg = Color(0xFFE7ECF3),
      primaryText = Color(0xFF334155),
      mutedText = Color(0xFF64748B),
      border = Color(0xFFE2E8F0),
      accent = Color(0xFF475569) // Modern slate
    )
    AppThemePalette.OCEAN -> ThemeColors(
      background = Color(0xFFF6FAFC),
      cardSurface = Color(0xFFEAF3F7),
      headerFooterBg = Color(0xFFE0EDF3),
      primaryText = Color(0xFF324A56),
      mutedText = Color(0xFF6F8997),
      border = Color(0xFFD6E6EE),
      accent = Color(0xFF3B88A8) // Ocean breeze
    )
  }
}

// Backward compatibility objects for legacy screens
object SoftChalkTheme {
  val background = Color(0xFFFFFFFF)
  val cardSurface = Color(0xFFF8FAFC)
  val primaryText = Color(0xFF475569)
  val mutedText = Color(0xFF94A3B8)
  val border = Color(0xFFE2E8F0)
  val accent = Color(0xFF38BDF8)
}

object CoolTitaniumTheme {
  val background = Color(0xFFF3F4F6)
  val cardSurface = Color(0xFFFFFFFF)
  val primaryText = Color(0xFF4B5563)
  val mutedText = Color(0xFF9CA3AF)
  val border = Color(0xFFE5E7EB)
  val accent = Color(0xFF818CF8)
}

