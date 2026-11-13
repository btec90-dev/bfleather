package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SyncedAppointmentItem
import com.example.data.SyncedChecklistItem
import com.example.data.SyncedMedicalDocItem
import com.example.data.SyncedUserProfile
import com.example.i18n.AppLanguage
import com.example.ui.theme.ThemeColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun formatHubShortDateFr(): String =
  SimpleDateFormat("dd MMM yyyy", Locale.FRENCH).format(Date())

private fun parseDayMonthYear(
  dateStr: String,
  fallbackDay: Long,
  fallbackMonth: Long,
  fallbackYear: Long
): Triple<Long, Long, Long> {
  val parts = dateStr.trim().split(Regex("\\s+"))
  val day = parts.getOrNull(0)?.toLongOrNull()?.coerceIn(1L, 31L) ?: fallbackDay
  val monthToken = parts.getOrNull(1)?.lowercase(Locale.FRENCH) ?: ""
  val month = when {
    monthToken.startsWith("jan") -> 0L
    monthToken.startsWith("fév") || monthToken.startsWith("fev") || monthToken.startsWith("feb") -> 1L
    monthToken.startsWith("mar") -> 2L
    monthToken.startsWith("avr") || monthToken.startsWith("apr") -> 3L
    monthToken.startsWith("mai") || monthToken.startsWith("may") -> 4L
    monthToken.startsWith("juin") || monthToken.startsWith("jun") -> 5L
    monthToken.startsWith("juil") || monthToken.startsWith("jul") -> 6L
    monthToken.startsWith("ao") || monthToken.startsWith("aug") -> 7L
    monthToken.startsWith("sep") -> 8L
    monthToken.startsWith("oct") -> 9L
    monthToken.startsWith("nov") -> 10L
    monthToken.startsWith("déc") || monthToken.startsWith("dec") -> 11L
    else -> fallbackMonth
  }
  val year = parts.getOrNull(2)?.toLongOrNull()?.coerceIn(2020L, 2035L) ?: fallbackYear
  return Triple(day, month, year)
}

@Composable
fun PregnancyHubScreen(
  userEmail: String,
  profile: SyncedUserProfile,
  appointments: List<SyncedAppointmentItem>,
  documents: List<SyncedMedicalDocItem>,
  checklists: List<SyncedChecklistItem>,
  galleryViewModel: GalleryViewModel,
  onUpdateProfile: (name: String, age: String, week: Int, dueDate: String) -> Unit,
  onUpdateWeek: (Int) -> Unit,
  onUpdateWater: (Int) -> Unit,
  onSaveHealthVitals: (mood: String, weight: String, water: Int, bp: String, notesText: String) -> Unit,
  onSaveAppointment: (SyncedAppointmentItem) -> Unit,
  onToggleAppointment: (aptId: String, currentChecked: Boolean) -> Unit,
  onSaveDocument: (SyncedMedicalDocItem) -> Unit,
  onSaveChecklistItem: (SyncedChecklistItem) -> Unit,
  onToggleChecklistItem: (itemId: String, currentChecked: Boolean) -> Unit,
  theme: ThemeColors,
  language: AppLanguage
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current

  // Synced Maternal Profile State
  val patientName = profile.patientName
  val patientAge = profile.patientAge
  val currentWeek = profile.currentWeek.toInt()
  val dueDate = profile.dueDate
  val daysRemaining = profile.daysRemaining.toInt()
  var showEditProfileDialog by remember { mutableStateOf(false) }

  // Monthly Calendar State (Default November 2026)
  var calendarYear by remember { mutableIntStateOf(2026) }
  var calendarMonth by remember { mutableIntStateOf(10) } // 0-based: 10 = November
  var selectedCalendarDay by remember { mutableIntStateOf(13) }
  var showAddCalendarEventDialog by remember { mutableStateOf(false) }

  // Daily Health & Vitals Draft State synced from profile
  var healthMoodInput by remember(profile.healthMood) { mutableStateOf(profile.healthMood) }
  var healthWeightInput by remember(profile.healthWeight) { mutableStateOf(profile.healthWeight) }
  var bloodPressureInput by remember(profile.bloodPressure) { mutableStateOf(profile.bloodPressure) }
  var personalNotesInput by remember(profile.personalNotesText) { mutableStateOf(profile.personalNotesText) }
  val waterGlasses = profile.waterGlasses.toInt()

  // Modals State
  var showAddDocDialog by remember { mutableStateOf(false) }
  var showCompleteArchiveDialog by remember { mutableStateOf(false) }
  var showAddAppointmentDialog by remember { mutableStateOf(false) }
  var showAddChecklistItemDialog by remember { mutableStateOf(false) }
  var targetChecklist by remember { mutableStateOf("Maman") }

  // Filtered Checklists from Firestore
  val momBagItems = remember(checklists) { checklists.filter { it.listKey == "mom_bag" } }
  val babyBagItems = remember(checklists) { checklists.filter { it.listKey == "baby_bag" } }
  val partnerBagItems = remember(checklists) { checklists.filter { it.listKey == "partner_bag" } }
  val beforeBirthItems = remember(checklists) { checklists.filter { it.listKey == "before_birth" } }
  val afterBirthItems = remember(checklists) { checklists.filter { it.listKey == "after_birth" } }

  // Dynamic calendar calculations for current month/year
  val monthCal = remember(calendarYear, calendarMonth) {
    Calendar.getInstance().apply {
      set(Calendar.YEAR, calendarYear)
      set(Calendar.MONTH, calendarMonth)
      set(Calendar.DAY_OF_MONTH, 1)
    }
  }
  val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
  val startOffset = ((monthCal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY) + 7) % 7

  val selectedDayAppointments = remember(appointments, selectedCalendarDay, calendarMonth, calendarYear) {
    appointments.filter {
      it.dayOfMonth.toInt() == selectedCalendarDay &&
        it.monthIndex.toInt() == calendarMonth &&
        it.yearNum.toInt() == calendarYear
    }
  }

  // -----------------------------------------------------------
  // MAIN SCREEN CONTENT (All on one screen - No left/right tabs)
  // -----------------------------------------------------------
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(theme.background)
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 36.dp)
  ) {
    // ---------------------------------------------------------
    // HEADER: Profile Summary & Direct Archive Access
    // ---------------------------------------------------------
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = theme.cardSurface,
        border = BorderStroke(1.dp, theme.border)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = CircleShape,
                color = theme.accent.copy(alpha = 0.16f),
                modifier = Modifier.size(46.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(profile.momAvatar.ifBlank { "🤰" }, fontSize = 24.sp)
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "$patientName ($patientAge ans)",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = theme.primaryText
                )
                Text(
                  text = "Semaine $currentWeek • DPA : $dueDate",
                  style = MaterialTheme.typography.bodySmall,
                  color = theme.mutedText
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(100.dp),
              color = theme.accent.copy(alpha = 0.15f),
              border = BorderStroke(0.5.dp, theme.accent)
            ) {
              Text(
                text = "J-$daysRemaining",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = theme.accent
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = { showEditProfileDialog = true },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
              modifier = Modifier
                .weight(1f)
                .testTag("pregnancy_edit_profile_button"),
              contentPadding = PaddingValues(vertical = 8.dp)
            ) {
              Icon(Icons.Default.Edit, contentDescription = "Modifier Profil", tint = Color.White, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (language == AppLanguage.FR) "Modifier Profil" else "Edit Profile", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }

            OutlinedButton(
              onClick = { showCompleteArchiveDialog = true },
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, theme.border),
              modifier = Modifier
                .weight(1f)
                .testTag("pregnancy_full_archive_button"),
              contentPadding = PaddingValues(vertical = 8.dp)
            ) {
              Icon(Icons.Default.FolderSpecial, contentDescription = "Archive Complète", tint = theme.accent, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (language == AppLanguage.FR) "Archive Complète" else "Full Archive", style = MaterialTheme.typography.labelSmall, color = theme.primaryText)
            }
          }
        }
      }
    }

    // ---------------------------------------------------------
    // SECTION: GALERIE PHOTO & ALBUM MAMAN & BÉBÉ (Moved to Top)
    // ---------------------------------------------------------
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = theme.cardSurface,
        border = BorderStroke(1.dp, theme.border)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = theme.accent, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (language == AppLanguage.FR) "Galerie Photo & Album Maman & Bébé" else "Mom & Baby Photo Gallery & Album",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = theme.primaryText
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(480.dp)
          ) {
            PhotoGalleryGrid(
              viewModel = galleryViewModel,
              theme = theme,
              language = language,
              modifier = Modifier.fillMaxSize()
            )
          }
        }
      }
    }

    // ---------------------------------------------------------
    // SECTION 1: CALENDRIER MENSUEL INTERACTIF
    // ---------------------------------------------------------
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = theme.cardSurface,
        border = BorderStroke(1.dp, theme.border)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.CalendarMonth, contentDescription = "Calendrier", tint = theme.accent, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "${getMonthName(calendarMonth, language)} $calendarYear",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = theme.primaryText
              )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              IconButton(
                onClick = {
                  if (calendarMonth == 0) {
                    calendarMonth = 11
                    calendarYear--
                  } else {
                    calendarMonth--
                  }
                  selectedCalendarDay = 1
                },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Mois précédent", tint = theme.primaryText)
              }
              IconButton(
                onClick = {
                  if (calendarMonth == 11) {
                    calendarMonth = 0
                    calendarYear++
                  } else {
                    calendarMonth++
                  }
                  selectedCalendarDay = 1
                },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Mois suivant", tint = theme.primaryText)
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          val dayHeaders = if (language == AppLanguage.FR) listOf("L", "M", "M", "J", "V", "S", "D") else listOf("M", "T", "W", "T", "F", "S", "S")
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            dayHeaders.forEach { h ->
              Text(h, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = theme.mutedText, textAlign = TextAlign.Center, modifier = Modifier.width(32.dp))
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          val totalCells = ((startOffset + daysInMonth + 6) / 7) * 7
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (week in 0 until totalCells / 7) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                for (dayIndex in 0 until 7) {
                  val dayNum = week * 7 + dayIndex - startOffset + 1
                  if (dayNum in 1..daysInMonth) {
                    val isSelected = selectedCalendarDay == dayNum
                    val hasAppointment = appointments.any {
                      it.dayOfMonth.toInt() == dayNum &&
                        it.monthIndex.toInt() == calendarMonth &&
                        it.yearNum.toInt() == calendarYear
                    }

                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = if (isSelected) theme.accent else if (hasAppointment) theme.accent.copy(alpha = 0.16f) else Color.Transparent,
                      border = if (isSelected) null else if (hasAppointment) BorderStroke(1.dp, theme.accent) else null,
                      modifier = Modifier
                        .size(36.dp)
                        .clickable { selectedCalendarDay = dayNum }
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                          Text(
                            text = dayNum.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected || hasAppointment) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else theme.primaryText
                          )
                          if (hasAppointment && !isSelected) {
                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(theme.accent))
                          }
                        }
                      }
                    }
                  } else {
                    Box(modifier = Modifier.size(36.dp))
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = theme.background,
            border = BorderStroke(1.dp, theme.border),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                  text = "Jour sélectionné : $selectedCalendarDay ${getMonthName(calendarMonth, language)} $calendarYear",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Bold,
                  color = theme.primaryText
                )
                Text(
                  text = if (selectedDayAppointments.isNotEmpty()) {
                    "${selectedDayAppointments.size} événement(s) : " +
                      selectedDayAppointments.joinToString(" • ") { it.title }
                  } else {
                    "Estrace 2 mg PO (Matin & Soir)"
                  },
                  style = MaterialTheme.typography.labelSmall,
                  color = theme.accent
                )
              }
              Button(
                onClick = { showAddCalendarEventDialog = true },
                modifier = Modifier.testTag("pregnancy_calendar_add_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter événement", tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+ Ajouter", style = MaterialTheme.typography.labelSmall, color = Color.White)
              }
            }
          }
        }
      }
    }

    // ---------------------------------------------------------
    // SECTION 2: MENU VISITES & RENDEZ-VOUS MÉDICAUX
    // ---------------------------------------------------------
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = theme.cardSurface,
        border = BorderStroke(1.dp, theme.border)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.MedicalServices, contentDescription = null, tint = theme.accent, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = if (language == AppLanguage.FR) "Menu Visites & Rendez-vous" else "Prenatal Visits & Appointments",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = theme.primaryText
                )
                Text(
                  text = "${appointments.size} visites programmées",
                  style = MaterialTheme.typography.labelSmall,
                  color = theme.mutedText
                )
              }
            }

            IconButton(
              onClick = { showAddAppointmentDialog = true },
              modifier = Modifier
                .size(34.dp)
                .background(theme.accent, CircleShape)
                .testTag("pregnancy_add_appointment_button")
            ) {
              Icon(Icons.Default.Add, contentDescription = "Ajouter visite", tint = Color.White, modifier = Modifier.size(18.dp))
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            appointments.forEach { apt ->
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = theme.background,
                border = BorderStroke(1.dp, if (apt.isCompleted) Color(0xFF16A34A).copy(alpha = 0.4f) else theme.border)
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = apt.title,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Bold,
                      color = theme.primaryText,
                      modifier = Modifier.weight(1f)
                    )
                    Surface(
                      shape = RoundedCornerShape(100.dp),
                      color = if (apt.isCompleted) Color(0xFFDCFCE7) else theme.accent.copy(alpha = 0.15f),
                      modifier = Modifier.clickable { onToggleAppointment(apt.id, apt.isCompleted) }
                    ) {
                      Text(
                        text = if (apt.isCompleted) "Effectué ✓" else "Prévu",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = if (apt.isCompleted) Color(0xFF16A34A) else theme.accent,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(4.dp))

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = theme.mutedText, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${apt.dateStr} à ${apt.timeStr}", style = MaterialTheme.typography.labelSmall, color = theme.mutedText)
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(Icons.Default.Person, contentDescription = null, tint = theme.mutedText, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = apt.doctor, style = MaterialTheme.typography.labelSmall, color = theme.mutedText)
                  }

                  if (apt.clinicalNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    SelectionContainer {
                      Text(
                        text = apt.clinicalNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.primaryText.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // ---------------------------------------------------------
    // SECTION 3: DÉVELOPPEMENT BÉBÉ & ÉCHOGRAPHIES
    // ---------------------------------------------------------
    item {
      SectionFetalDevelopmentOverview(
        currentWeek = currentWeek,
        onWeekCommitted = onUpdateWeek,
        theme = theme,
        language = language
      )
    }

    // ---------------------------------------------------------
    // SECTION 4: NUTRITION MAMAN & BÉBÉ
    // ---------------------------------------------------------
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = theme.cardSurface,
        border = BorderStroke(1.dp, theme.border)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Restaurant, contentDescription = null, tint = theme.accent, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (language == AppLanguage.FR) "Nutrition Maman & Bébé" else "Mom & Baby Nutrition",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = theme.primaryText
              )
            }
            Surface(
              shape = RoundedCornerShape(100.dp),
              color = theme.accent.copy(alpha = 0.15f)
            ) {
              Text(
                text = when {
                  currentWeek < 14 -> "Trimestre 1"
                  currentWeek < 28 -> "Trimestre 2"
                  else -> "Trimestre 3"
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = theme.accent,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Hydration tracker (Interactive Water glasses synced to Firestore)
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = theme.background,
            border = BorderStroke(1.dp, theme.border),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("Hydratation Quotidienne :", style = MaterialTheme.typography.labelSmall, color = theme.mutedText)
                Text("$waterGlasses verres (${waterGlasses * 250} ml)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = theme.primaryText)
              }
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                  onClick = { if (waterGlasses > 0) onUpdateWater(waterGlasses - 1) },
                  modifier = Modifier
                    .size(34.dp)
                    .background(theme.cardSurface, CircleShape)
                    .testTag("pregnancy_water_minus_button")
                ) {
                  Text("-", style = MaterialTheme.typography.titleMedium, color = theme.primaryText)
                }
                IconButton(
                  onClick = { onUpdateWater(waterGlasses + 1) },
                  modifier = Modifier
                    .size(34.dp)
                    .background(theme.accent, CircleShape)
                    .testTag("pregnancy_water_plus_button")
                ) {
                  Text("+", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          val foodPoints = listOf(
            "🌱 Folates (Vitamine B9) : Épinards, lentilles, pois chiches pour le tube neural.",
            "🥩 Fer & Vitamine C : Viandes maigres, agrumes, persil pour le volume sanguin.",
            "💧 Eau & Tisanes : Boire au minimum 1,5 à 2 litres par jour pour le liquide amniotique.",
            "🚫 À Proscrire : Aliments crus, fromages au lait cru, poissons prédateurs riches en mercure."
          )
          foodPoints.forEach { pt ->
            Text(pt, style = MaterialTheme.typography.bodySmall, color = theme.primaryText, modifier = Modifier.padding(vertical = 3.dp), lineHeight = 17.sp)
          }
        }
      }
    }

    // ---------------------------------------------------------
    // SECTION 5: OUTILS & SANTÉ MATERNELLE (Editable & Synced)
    // ---------------------------------------------------------
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = theme.cardSurface,
        border = BorderStroke(1.dp, theme.border)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (language == AppLanguage.FR) "Outils & Santé Maternelle" else "Health & Maternal Tools",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = theme.primaryText
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = healthWeightInput,
              onValueChange = { healthWeightInput = it },
              label = { Text("Poids (kg)", style = MaterialTheme.typography.labelSmall) },
              singleLine = true,
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
              value = bloodPressureInput,
              onValueChange = { bloodPressureInput = it },
              label = { Text("Tension", style = MaterialTheme.typography.labelSmall) },
              singleLine = true,
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp)
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Mood selector chips
          Text("Humeur :", style = MaterialTheme.typography.labelSmall, color = theme.mutedText)
          Spacer(modifier = Modifier.height(4.dp))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf("Calme", "Sereine", "Fatiguée", "Émotive", "En forme")) { moodOption ->
              FilterChip(
                selected = healthMoodInput == moodOption,
                onClick = { healthMoodInput = moodOption },
                label = { Text(moodOption, style = MaterialTheme.typography.labelSmall) }
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = personalNotesInput,
            onValueChange = { personalNotesInput = it },
            label = { Text("Notes personnelles & symptômes du jour", style = MaterialTheme.typography.bodySmall) },
            placeholder = { Text("Consignez vos ressentis ou observations...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          Button(
            onClick = {
              onSaveHealthVitals(
                healthMoodInput,
                healthWeightInput,
                waterGlasses,
                bloodPressureInput,
                personalNotesInput
              )
              Toast.makeText(context, "Paramètres santé & notes sauvegardés !", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("pregnancy_save_vitals_button"),
            contentPadding = PaddingValues(vertical = 8.dp)
          ) {
            Icon(Icons.Default.Check, contentDescription = "Enregistrer", tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Enregistrer les Paramètres Santé", style = MaterialTheme.typography.labelSmall, color = Color.White)
          }
        }
      }
    }

    // ---------------------------------------------------------
    // SECTION 6: COFFRE & DOCUMENTS MÉDICAUX
    // ---------------------------------------------------------
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = theme.cardSurface,
        border = BorderStroke(1.dp, theme.border)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Shield, contentDescription = null, tint = theme.accent, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (language == AppLanguage.FR) "Coffre & Documents Médicaux" else "Vault & Medical Documents",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = theme.primaryText
              )
            }
            TextButton(
              onClick = { showAddDocDialog = true },
              modifier = Modifier.testTag("pregnancy_add_doc_button")
            ) {
              Text("+ Ajouter", color = theme.accent, fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            documents.forEach { doc ->
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = theme.background,
                border = BorderStroke(1.dp, theme.border)
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    SelectionContainer {
                      Column {
                        Text(doc.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = theme.primaryText)
                        Text("${doc.category} • ${doc.dateStr}", style = MaterialTheme.typography.labelSmall, color = theme.mutedText)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(doc.summary, style = MaterialTheme.typography.bodySmall, color = theme.primaryText.copy(alpha = 0.8f))
                      }
                    }
                  }
                  IconButton(onClick = {
                    clipboardManager.setText(AnnotatedString("${doc.title} - ${doc.summary}"))
                    Toast.makeText(context, "Copié !", Toast.LENGTH_SHORT).show()
                  }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copier", tint = theme.accent, modifier = Modifier.size(16.dp))
                  }
                }
              }
            }
          }
        }
      }
    }



    // ---------------------------------------------------------
    // SECTION 7: GUIDE & ÉCHOGRAPHIES (Conseils & Jalons T1-T3)
    // ---------------------------------------------------------
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = theme.cardSurface,
        border = BorderStroke(1.dp, theme.border)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MenuBook, contentDescription = null, tint = theme.accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (language == AppLanguage.FR) "Guide & Échographies" else "Guides & Ultrasounds",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = theme.primaryText
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          val guidesList = listOf(
            "Échographie T1 (11-13 SA)" to "Mesure de la clarté nucale, datation précise et détection précoce.",
            "Échographie Morphologique T2 (20-24 SA)" to "Analyse détaillée des organes, biométrie et croissance.",
            "Échographie T3 (31-34 SA)" to "Position fœtale, estimation pondérale et bien-être placentaire.",
            "Préparation à la Naissance & Allaitement" to "Respiration, postures de travail et mise au sein précoce."
          )

          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            guidesList.forEach { (title, desc) ->
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = theme.background,
                border = BorderStroke(0.5.dp, theme.border)
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = theme.primaryText)
                  Text(desc, style = MaterialTheme.typography.labelSmall, color = theme.mutedText)
                }
              }
            }
          }
        }
      }
    }

    // ---------------------------------------------------------
    // SECTION 8: ORGANISATION & VALISE MATERNITÉ (Checklists)
    // ---------------------------------------------------------
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = theme.cardSurface,
        border = BorderStroke(1.dp, theme.border)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Luggage, contentDescription = null, tint = theme.accent, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (language == AppLanguage.FR) "Organisation & Valise Maternité" else "Checklists & Hospital Bag",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = theme.primaryText
              )
            }
            TextButton(
              onClick = { showAddChecklistItemDialog = true },
              modifier = Modifier.testTag("pregnancy_add_checklist_button")
            ) {
              Text("+ Ajouter Article", color = theme.accent, style = MaterialTheme.typography.labelSmall)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Text("Valise Maternité (Maman) :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = theme.primaryText)
          Spacer(modifier = Modifier.height(4.dp))
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            momBagItems.forEach { item ->
              ChecklistRow(item = item, theme = theme, onToggle = { onToggleChecklistItem(item.id, item.isChecked) })
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text("Valise Maternité (Bébé - Jour J) :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = theme.primaryText)
          Spacer(modifier = Modifier.height(4.dp))
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            babyBagItems.forEach { item ->
              ChecklistRow(item = item, theme = theme, onToggle = { onToggleChecklistItem(item.id, item.isChecked) })
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text("Valise Accompagnant (Papa) :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = theme.primaryText)
          Spacer(modifier = Modifier.height(4.dp))
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            partnerBagItems.forEach { item ->
              ChecklistRow(item = item, theme = theme, onToggle = { onToggleChecklistItem(item.id, item.isChecked) })
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text("À Acheter Avant la Naissance :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = theme.primaryText)
          Spacer(modifier = Modifier.height(4.dp))
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            beforeBirthItems.forEach { item ->
              ChecklistRow(item = item, theme = theme, onToggle = { onToggleChecklistItem(item.id, item.isChecked) })
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text("À Acheter Après la Naissance :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = theme.primaryText)
          Spacer(modifier = Modifier.height(4.dp))
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            afterBirthItems.forEach { item ->
              ChecklistRow(item = item, theme = theme, onToggle = { onToggleChecklistItem(item.id, item.isChecked) })
            }
          }
        }
      }
    }
  }

  // -----------------------------------------------------------
  // DIALOGS & EDITABLE MODALS
  // -----------------------------------------------------------

  // Edit Profile Modal
  if (showEditProfileDialog) {
    var editName by remember(patientName) { mutableStateOf(patientName) }
    var editAge by remember(patientAge) { mutableStateOf(patientAge) }
    var editWeek by remember(currentWeek) { mutableStateOf(currentWeek.toString()) }
    var editDueDate by remember(dueDate) { mutableStateOf(dueDate) }

    AlertDialog(
      onDismissRequest = { showEditProfileDialog = false },
      title = { Text(if (language == AppLanguage.FR) "Modifier le Profil Maman" else "Edit Maternal Profile", color = theme.primaryText) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Prénom") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = editAge, onValueChange = { editAge = it }, label = { Text("Âge") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = editWeek, onValueChange = { editWeek = it }, label = { Text("Semaine d'aménorrhée (SA)") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = editDueDate, onValueChange = { editDueDate = it }, label = { Text("Date d'accouchement estimée") }, modifier = Modifier.fillMaxWidth())
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onUpdateProfile(
              editName.ifBlank { patientName },
              editAge.ifBlank { patientAge },
              editWeek.toIntOrNull() ?: currentWeek,
              editDueDate.ifBlank { dueDate }
            )
            showEditProfileDialog = false
            Toast.makeText(context, "Profil mis à jour & synchronisé !", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
        ) {
          Text("Enregistrer")
        }
      },
      dismissButton = {
        TextButton(onClick = { showEditProfileDialog = false }) {
          Text("Annuler", color = theme.mutedText)
        }
      }
    )
  }

  // Add Appointment Modal (Menu Visite)
  if (showAddAppointmentDialog) {
    var newAptTitle by remember { mutableStateOf("") }
    var newAptDate by remember { mutableStateOf("13 Nov 2026") }
    var newAptTime by remember { mutableStateOf("09:00") }
    var newAptDoc by remember { mutableStateOf("Dr. Laurent (OB/GYN)") }
    var newAptLoc by remember { mutableStateOf("Maternité / Clinique") }
    var newAptNotes by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showAddAppointmentDialog = false },
      title = { Text("Ajouter un Rendez-vous Prénatal", color = theme.primaryText) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = newAptTitle, onValueChange = { newAptTitle = it }, label = { Text("Titre de la consultation") }, placeholder = { Text("ex: Échographie T2") }, modifier = Modifier.fillMaxWidth())
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(value = newAptDate, onValueChange = { newAptDate = it }, label = { Text("Date") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = newAptTime, onValueChange = { newAptTime = it }, label = { Text("Heure") }, modifier = Modifier.weight(1f))
          }
          OutlinedTextField(value = newAptDoc, onValueChange = { newAptDoc = it }, label = { Text("Médecin / Praticien") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = newAptLoc, onValueChange = { newAptLoc = it }, label = { Text("Lieu / Centre") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = newAptNotes, onValueChange = { newAptNotes = it }, label = { Text("Notes cliniques") }, modifier = Modifier.fillMaxWidth())
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newAptTitle.isNotBlank()) {
              val (parsedDay, parsedMonth, parsedYear) = parseDayMonthYear(
                dateStr = newAptDate,
                fallbackDay = selectedCalendarDay.toLong(),
                fallbackMonth = calendarMonth.toLong(),
                fallbackYear = calendarYear.toLong()
              )
              onSaveAppointment(
                SyncedAppointmentItem(
                  id = "apt_${System.currentTimeMillis()}",
                  title = newAptTitle,
                  dateStr = newAptDate,
                  timeStr = newAptTime,
                  doctor = newAptDoc,
                  location = newAptLoc,
                  clinicalNotes = newAptNotes,
                  eventType = "Rendez-vous",
                  dayOfMonth = parsedDay,
                  monthIndex = parsedMonth,
                  yearNum = parsedYear,
                  isCompleted = false
                )
              )
              showAddAppointmentDialog = false
              Toast.makeText(context, "Rendez-vous ajouté & synchronisé !", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
        ) {
          Text("Ajouter")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddAppointmentDialog = false }) {
          Text("Annuler", color = theme.mutedText)
        }
      }
    )
  }

  // Add Calendar Event Modal (Calendar day add)
  if (showAddCalendarEventDialog) {
    var eventTitle by remember { mutableStateOf("") }
    var eventType by remember { mutableStateOf("Rendez-vous") }

    AlertDialog(
      onDismissRequest = { showAddCalendarEventDialog = false },
      title = { Text("Ajouter pour le $selectedCalendarDay ${getMonthName(calendarMonth, language)}", color = theme.primaryText) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(value = eventTitle, onValueChange = { eventTitle = it }, label = { Text("Description") }, placeholder = { Text("ex: Prise Estrace ou Prise de sang") }, modifier = Modifier.fillMaxWidth())
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Rendez-vous", "Médicament", "Rappel").forEach { type ->
              FilterChip(
                selected = eventType == type,
                onClick = { eventType = type },
                label = { Text(type) }
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (eventTitle.isNotBlank()) {
              onSaveAppointment(
                SyncedAppointmentItem(
                  id = "apt_${System.currentTimeMillis()}",
                  title = eventTitle,
                  dateStr = "$selectedCalendarDay ${getMonthName(calendarMonth, language)} $calendarYear",
                  timeStr = "09:00",
                  doctor = "Dr. Laurent (OB/GYN)",
                  location = "Maternité",
                  clinicalNotes = "Ajouté depuis le calendrier mensuel ($eventType)",
                  eventType = eventType,
                  dayOfMonth = selectedCalendarDay.toLong(),
                  monthIndex = calendarMonth.toLong(),
                  yearNum = calendarYear.toLong(),
                  isCompleted = false
                )
              )
              showAddCalendarEventDialog = false
              Toast.makeText(context, "Événement planifié & synchronisé !", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
        ) {
          Text("Enregistrer")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddCalendarEventDialog = false }) {
          Text("Annuler", color = theme.mutedText)
        }
      }
    )
  }

  // Add Document Dialog
  if (showAddDocDialog) {
    var docTitle by remember { mutableStateOf("") }
    var docCat by remember { mutableStateOf("Ordonnance") }
    var docSummary by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showAddDocDialog = false },
      title = { Text("Nouveau Document Médical", color = theme.primaryText) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(value = docTitle, onValueChange = { docTitle = it }, label = { Text("Titre du document") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = docCat, onValueChange = { docCat = it }, label = { Text("Catégorie (Écho, Ordonnance, Bilan)") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = docSummary, onValueChange = { docSummary = it }, label = { Text("Résumé ou compte-rendu") }, modifier = Modifier.fillMaxWidth())
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (docTitle.isNotBlank()) {
              onSaveDocument(
                SyncedMedicalDocItem(
                  id = "doc_${System.currentTimeMillis()}",
                  title = docTitle,
                  category = docCat,
                  dateStr = formatHubShortDateFr(),
                  summary = docSummary,
                  source = "hub"
                )
              )
              showAddDocDialog = false
              Toast.makeText(context, "Document sauvegardé & synchronisé !", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
        ) {
          Text("Sauvegarder")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDocDialog = false }) {
          Text("Annuler", color = theme.mutedText)
        }
      }
    )
  }

  // Add Checklist Item Dialog
  if (showAddChecklistItemDialog) {
    var newChecklistLabel by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showAddChecklistItemDialog = false },
      title = { Text("Ajouter un article à la liste", color = theme.primaryText) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(value = newChecklistLabel, onValueChange = { newChecklistLabel = it }, label = { Text("Article / Équipement") }, modifier = Modifier.fillMaxWidth())
          Text("Sélectionnez la liste cible :", style = MaterialTheme.typography.labelSmall, color = theme.mutedText)
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf("Maman", "Bébé", "Papa", "Avant Naissance", "Après Naissance")) { target ->
              FilterChip(
                selected = targetChecklist == target,
                onClick = { targetChecklist = target },
                label = { Text(target) }
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newChecklistLabel.isNotBlank()) {
              val listKey = when (targetChecklist) {
                "Maman" -> "mom_bag"
                "Bébé" -> "baby_bag"
                "Papa" -> "partner_bag"
                "Avant Naissance" -> "before_birth"
                else -> "after_birth"
              }
              onSaveChecklistItem(
                SyncedChecklistItem(
                  id = "cl_${System.currentTimeMillis()}",
                  listKey = listKey,
                  label = newChecklistLabel,
                  isChecked = false,
                  sortOrder = checklists.size.toLong()
                )
              )
              showAddChecklistItemDialog = false
              Toast.makeText(context, "Article ajouté & synchronisé !", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
        ) {
          Text("Ajouter")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddChecklistItemDialog = false }) {
          Text("Annuler", color = theme.mutedText)
        }
      }
    )
  }

  // Complete Archive View Dialog (Dynamic Synced Summary)
  if (showCompleteArchiveDialog) {
    val checkedCount = checklists.count { it.isChecked }
    val archiveSummary = """
      MAMAN & BÉBÉ - ARCHIVE CLINIQUE COMPLÈTE
      Patiente : $patientName ($patientAge ans) • Compte : $userEmail
      Semaine $currentWeek • DPA : $dueDate ($daysRemaining jours restants)
      Hydratation : $waterGlasses verres • Poids : ${profile.healthWeight} kg • Tension : ${profile.bloodPressure}
      ${appointments.size} Visites Prénatales enregistrées • ${documents.size} Documents médicaux
      Checklists maternité : $checkedCount / ${checklists.size} éléments préparés
    """.trimIndent()

    AlertDialog(
      onDismissRequest = { showCompleteArchiveDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.FolderZip, contentDescription = null, tint = theme.accent)
          Spacer(modifier = Modifier.width(8.dp))
          Text("MAMAN & BÉBÉ - ARCHIVE CLINIQUE", color = theme.primaryText, style = MaterialTheme.typography.titleMedium)
        }
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = archiveSummary,
            style = MaterialTheme.typography.bodySmall,
            color = theme.mutedText
          )
          Spacer(modifier = Modifier.height(4.dp))
          Button(
            onClick = {
              clipboardManager.setText(AnnotatedString(archiveSummary))
              Toast.makeText(context, "Archive copiée dans le presse-papier !", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copier archive", tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Copier l'Archive Complète", color = Color.White)
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showCompleteArchiveDialog = false }) {
          Text("Fermer", color = theme.accent)
        }
      }
    )
  }
}

// -------------------------------------------------------------
// Interactive Checklist Item Row (Pure Reactive State)
// -------------------------------------------------------------
@Composable
private fun ChecklistRow(
  item: SyncedChecklistItem,
  theme: ThemeColors,
  onToggle: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onToggle() },
    shape = RoundedCornerShape(8.dp),
    color = theme.background,
    border = BorderStroke(0.5.dp, theme.border)
  ) {
    Row(
      modifier = Modifier.padding(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        if (item.isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
        contentDescription = if (item.isChecked) "Coché" else "Non coché",
        tint = if (item.isChecked) Color(0xFF16A34A) else theme.mutedText,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = item.label,
        style = MaterialTheme.typography.bodySmall,
        color = if (item.isChecked) theme.mutedText else theme.primaryText,
        fontSize = 12.sp,
        lineHeight = 16.sp
      )
    }
  }
}

// -------------------------------------------------------------
// Fetal Development Overview (Live Slider + Cloud Commit on Release)
// -------------------------------------------------------------
@Composable
private fun SectionFetalDevelopmentOverview(
  currentWeek: Int,
  onWeekCommitted: (Int) -> Unit,
  theme: ThemeColors,
  language: AppLanguage
) {
  var sliderWeek by remember(currentWeek) { mutableIntStateOf(currentWeek.coerceIn(1, 40)) }

  val (fruitName, fruitEmoji, sizeStr, weightStr, descStr) = when (sliderWeek) {
    in 1..4 -> Quintuple("Graine de pavot", "🌱", "0.2 cm", "< 1 g", "Nidation, formation du sac gestationnel et premières divisions cellulaires actives.")
    in 5..8 -> Quintuple("Framboise", "🍓", "1.6 cm", "1 g", "Battements cardiaques détectables, ébauche des membres et du tube neural.")
    in 9..12 -> Quintuple("Citron vert", "🍋", "5.4 cm", "14 g", "Réflexes embryonnaires, doigts et orteils séparés, déglutition amniotique.")
    in 13..16 -> Quintuple("Avocat", "🥑", "11.6 cm", "100 g", "Oreille interne formée, perception des voix, mouvements spontanés.")
    in 17..20 -> Quintuple("Mangue", "🥭", "16.4 cm", "300 g", "Premiers coups de pied ressentis (quickening), vernix caseosa protecteur.")
    in 21..24 -> Quintuple("Épi de maïs", "🌽", "29 cm", "550 g", "Empreintes digitales, audition affinée, cycles de sommeil et d'éveil.")
    in 25..28 -> Quintuple("Aubergine", "🍆", "36 cm", "1000 g", "Ouverture des yeux, développement du surfactant alvéolaire pulmonaire.")
    in 29..32 -> Quintuple("Noix de coco", "🥥", "42 cm", "1700 g", "Croissance pondérale rapide, os solides, maturation cérébrale.")
    in 33..36 -> Quintuple("Ananas", "🍍", "47 cm", "2600 g", "Position céphalique tête en bas, système immunitaire maternel actif.")
    else -> Quintuple("Pastèque", "🍉", "51 cm", "3400 g", "Bébé à terme, tous les organes prêts pour la venue au monde !")
  }

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    color = theme.cardSurface,
    border = BorderStroke(1.dp, theme.border)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = if (language == AppLanguage.FR) "Développement du Bébé" else "Baby Development",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = theme.primaryText
          )
          Text(
            text = "Semaine $sliderWeek • Taille : $sizeStr • Poids : $weightStr",
            style = MaterialTheme.typography.labelSmall,
            color = theme.mutedText
          )
        }

        Surface(
          shape = RoundedCornerShape(100.dp),
          color = theme.accent.copy(alpha = 0.15f)
        ) {
          Text(
            text = "$fruitEmoji $fruitName",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = theme.accent
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Slider(
        value = sliderWeek.toFloat(),
        onValueChange = { sliderWeek = it.toInt() },
        onValueChangeFinished = { onWeekCommitted(sliderWeek) },
        valueRange = 1f..40f,
        steps = 38,
        modifier = Modifier.testTag("pregnancy_week_slider"),
        colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent)
      )

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(180.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(
            Brush.verticalGradient(
              listOf(Color(0xFF1E293B), Color(0xFF0F172A))
            )
          ),
        contentAlignment = Alignment.Center
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val centerX = size.width / 2f
          val centerY = size.height / 2f

          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(Color(0x40E0E7FF), Color(0x103B82F6), Color.Transparent),
              center = Offset(centerX, centerY),
              radius = 80.dp.toPx()
            ),
            radius = 80.dp.toPx()
          )

          if (sliderWeek <= 5) {
            drawCircle(
              color = Color(0xFFFDE68A),
              radius = 12.dp.toPx(),
              center = Offset(centerX, centerY)
            )
            drawCircle(
              color = Color(0xFFD97706),
              radius = 5.dp.toPx(),
              center = Offset(centerX, centerY)
            )
          } else {
            drawCircle(
              color = Color(0xFFFBCFE8),
              radius = (sliderWeek * 1.5f).dp.toPx().coerceIn(16f, 60f),
              center = Offset(centerX, centerY)
            )
          }
        }

        Column(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 8.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "Modèle 3D fœtal interactif • $sizeStr",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.9f)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = descStr,
        style = MaterialTheme.typography.bodySmall,
        color = theme.primaryText,
        lineHeight = 18.sp
      )
    }
  }
}

private data class Quintuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

private fun getMonthName(monthIndex: Int, language: AppLanguage): String {
  val monthsFr = listOf("Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre")
  val monthsEn = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
  return if (language == AppLanguage.FR) monthsFr.getOrElse(monthIndex) { "Mois" } else monthsEn.getOrElse(monthIndex) { "Month" }
}
