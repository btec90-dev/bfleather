package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PmaRepository
import com.example.data.SyncedProtocolStep
import com.example.data.TaskItem
import com.example.i18n.AppLanguage
import com.example.ui.theme.ThemeColors
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun ProtocolScreen(
  protocolSteps: List<SyncedProtocolStep>,
  immediateTasks: List<TaskItem>,
  onSaveStep: (SyncedProtocolStep) -> Unit,
  onToggleStep: (SyncedProtocolStep) -> Unit,
  onDeleteStep: (String) -> Unit,
  onSaveImmediateTask: (TaskItem) -> Unit,
  onToggleImmediateTask: (TaskItem) -> Unit,
  onDeleteImmediateTask: (String) -> Unit,
  onResetProtocol: () -> Unit,
  theme: ThemeColors,
  language: AppLanguage
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val isFr = language == AppLanguage.FR

  // Modals state
  var showStepModal by remember { mutableStateOf(false) }
  var editingStep by remember { mutableStateOf<SyncedProtocolStep?>(null) }
  var modalInitialPhaseId by remember { mutableIntStateOf(2) }
  var modalInitialDate by remember { mutableStateOf("13 nov.") }

  var showImmediateTaskModal by remember { mutableStateOf(false) }
  var editingImmediateTask by remember { mutableStateOf<TaskItem?>(null) }

  var showResetConfirmDialog by remember { mutableStateOf(false) }

  // Phase tabs & collapsible state
  var selectedPhaseTab by remember { mutableIntStateOf(0) } // 0 = All Phases
  val expandedPhases = remember {
    mutableStateMapOf(
      1 to false,
      2 to true,
      3 to true,
      4 to false
    )
  }

  // Optional November 2026 quick calendar drawer (collapsed by default)
  var isCalendarExpanded by remember { mutableStateOf(false) }
  var selectedDayNumber by remember { mutableIntStateOf(13) }

  // Live countdown ticker
  var currentEpochTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(1000L)
      currentEpochTime = System.currentTimeMillis()
    }
  }

  fun formatLiveCountdown(targetMillis: Long): String {
    val diff = targetMillis - currentEpochTime
    if (diff <= 0) return if (isFr) "À prendre maintenant" else "Due now"
    val days = diff / (1000L * 60 * 60 * 24)
    val hours = (diff / (1000L * 60 * 60)) % 24
    val minutes = (diff / (1000L * 60)) % 60
    val seconds = (diff / 1000L) % 60
    return if (days > 0) "${days}j ${hours}h ${minutes}m ${seconds}s" else "${hours}h ${minutes}m ${seconds}s"
  }

  val nextEstraceMillis = remember(currentEpochTime) {
    val cal = Calendar.getInstance()
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    Calendar.getInstance().apply {
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      when {
        hour < 8 -> {
          set(Calendar.HOUR_OF_DAY, 8)
          set(Calendar.MINUTE, 0)
        }
        hour < 20 -> {
          set(Calendar.HOUR_OF_DAY, 20)
          set(Calendar.MINUTE, 0)
        }
        else -> {
          add(Calendar.DAY_OF_MONTH, 1)
          set(Calendar.HOUR_OF_DAY, 8)
          set(Calendar.MINUTE, 0)
        }
      }
    }.timeInMillis
  }

  val completedStepsCount = protocolSteps.count { it.isCompleted }
  val totalStepsCount = protocolSteps.size.coerceAtLeast(1)
  val progressFraction = (completedStepsCount.toFloat() / totalStepsCount.toFloat()).coerceIn(0f, 1f)

  val openAddStepModal: (Int, String) -> Unit = { phaseId, dateStr ->
    editingStep = null
    modalInitialPhaseId = phaseId
    modalInitialDate = dateStr
    showStepModal = true
  }

  SelectionContainer {
    Box(modifier = Modifier.fillMaxSize()) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .background(theme.background)
          .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 96.dp)
      ) {
        // -------------------------------------------------------------
        // HEADER CARD: Clinic, Doctor, Embryologist, Patient, Objective & Reset
        // -------------------------------------------------------------
        item {
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("protocol_header_card"),
            shape = RoundedCornerShape(20.dp),
            color = theme.cardSurface,
            border = BorderStroke(1.2.dp, theme.accent.copy(alpha = 0.35f)),
            shadowElevation = 3.dp
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(
                  Brush.verticalGradient(
                    colors = listOf(
                      theme.accent.copy(alpha = 0.10f),
                      theme.cardSurface
                    )
                  )
                )
                .padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = "📋 Protocole PMA & Étapes de Grossesse : Calendrier Directeur",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = theme.primaryText,
                lineHeight = 22.sp
              )

              HorizontalDivider(color = theme.border)

              ProtocolHeaderLine("🏢 Centre :", "${PmaRepository.clinicName} (${PmaRepository.clinicAddress})", theme)
              ProtocolHeaderLine("🩺 Médecin référent :", PmaRepository.doctorName, theme)
              ProtocolHeaderLine("👩‍⚕️ Équipe & Coordonnées :", "${PmaRepository.embryologistInfo}.", theme)
              ProtocolHeaderLine("👤 Patiente :", PmaRepository.patientName, theme)
              ProtocolHeaderLine("🎯 Objectif :", PmaRepository.objective, theme, highlightValue = true)

              // Progress Bar
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = if (isFr) {
                      "Progression : $completedStepsCount / ${protocolSteps.size} étapes complétées"
                    } else {
                      "Progress: $completedStepsCount / ${protocolSteps.size} steps completed"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.mutedText
                  )
                  Text(
                    text = "${(progressFraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = theme.accent
                  )
                }
                LinearProgressIndicator(
                  progress = { progressFraction },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(100.dp)),
                  color = theme.accent,
                  trackColor = theme.border
                )
              }

              // Reset & Quick Add Buttons
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(
                  onClick = { showResetConfirmDialog = true },
                  modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("protocol_reset_button"),
                  shape = RoundedCornerShape(12.dp),
                  border = BorderStroke(1.dp, theme.border),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                  Icon(Icons.Default.RestartAlt, contentDescription = null, tint = theme.mutedText, modifier = Modifier.size(17.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (isFr) "Réinitialiser" else "Reset",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.primaryText,
                    maxLines = 1
                  )
                }

                Button(
                  onClick = { openAddStepModal(2, "13 nov.") },
                  modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("protocol_header_add_button"),
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                  Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (isFr) "Ajouter Étape" else "Add Step",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                  )
                }
              }
            }
          }
        }

        // -------------------------------------------------------------
        // COMPACT LIVE ESTRACE COUNTDOWN + COLLAPSIBLE NOV 2026 CALENDAR
        // -------------------------------------------------------------
        item {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = theme.cardSurface,
            border = BorderStroke(1.dp, theme.accent.copy(alpha = 0.28f))
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  modifier = Modifier.weight(1f),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(text = "⏱️", fontSize = 18.sp)
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text(
                      text = if (isFr) "Prochaine prise Estrace 2 mg PO (08h & 20h)" else "Next Dose Estrace 2 mg PO (8 AM & 8 PM)",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = theme.primaryText
                    )
                    Text(
                      text = formatLiveCountdown(nextEstraceMillis),
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.ExtraBold,
                      color = theme.accent
                    )
                  }
                }

                TextButton(
                  onClick = { isCalendarExpanded = !isCalendarExpanded },
                  modifier = Modifier.heightIn(min = 48.dp)
                ) {
                  Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = theme.accent, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (isFr) "Nov. 2026" else "Nov 2026",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = theme.accent
                  )
                  Icon(
                    imageVector = if (isCalendarExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = theme.accent,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }

              AnimatedVisibility(visible = isCalendarExpanded) {
                NovemberCalendarGrid(
                  selectedDayNumber = selectedDayNumber,
                  theme = theme,
                  language = language,
                  onDayClick = { day ->
                    selectedDayNumber = day
                    openAddStepModal(2, "$day nov. 2026")
                  }
                )
              }
            }
          }
        }

        // -------------------------------------------------------------
        // SECTION 1: 📊 1. Tableau de Bord Immédiat (📌 Actions & Statuts Cliniques)
        // -------------------------------------------------------------
        item {
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("protocol_section_1_card"),
            shape = RoundedCornerShape(18.dp),
            color = theme.cardSurface,
            border = BorderStroke(1.2.dp, theme.accent.copy(alpha = 0.35f)),
            shadowElevation = 2.dp
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "📊 1. Tableau de Bord Immédiat",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = theme.primaryText
                  )
                  Text(
                    text = "📌 Actions & Statuts Cliniques",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.accent
                  )
                }

                FilledTonalButton(
                  onClick = {
                    editingImmediateTask = null
                    showImmediateTaskModal = true
                  },
                  modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("protocol_add_immediate_button"),
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = theme.accent.copy(alpha = 0.14f),
                    contentColor = theme.accent
                  ),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                  Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (isFr) "Ajouter" else "Add",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                  )
                }
              }

              if (immediateTasks.isEmpty()) {
                Text(
                  text = if (isFr) "Aucune action clinique enregistrée." else "No immediate clinical actions.",
                  style = MaterialTheme.typography.bodySmall,
                  color = theme.mutedText
                )
              } else {
                immediateTasks.forEach { task ->
                  ImmediateClinicalActionCard(
                    task = task,
                    theme = theme,
                    language = language,
                    onToggle = { onToggleImmediateTask(task) },
                    onEdit = {
                      editingImmediateTask = task
                      showImmediateTaskModal = true
                    },
                    onDelete = {
                      onDeleteImmediateTask(task.id)
                      Toast.makeText(context, if (isFr) "Action supprimée" else "Action deleted", Toast.LENGTH_SHORT).show()
                    }
                  )
                }
              }
            }
          }
        }

        // -------------------------------------------------------------
        // SECTION 2 HEADER & PHASE TABS: 🗓️ 2. Chronologie Complète du Parcours Médical
        // -------------------------------------------------------------
        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "🗓️ 2. Chronologie Complète du Parcours Médical",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = theme.primaryText,
                modifier = Modifier.weight(1f)
              )
              FilledTonalButton(
                onClick = { openAddStepModal(if (selectedPhaseTab in 1..4) selectedPhaseTab else 2, "13 nov.") },
                modifier = Modifier
                  .heightIn(min = 48.dp)
                  .testTag("protocol_add_step_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                  containerColor = theme.accent.copy(alpha = 0.14f),
                  contentColor = theme.accent
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isFr) "Étape" else "Step", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
              }
            }

            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              val tabs = listOf(0 to (if (isFr) "Toutes (4 Phases)" else "All Phases")) +
                protocolPhasesMeta.map { it.phaseId to "${it.emoji} Phase ${it.phaseId}" }
              items(tabs) { (phaseId, label) ->
                val isSel = selectedPhaseTab == phaseId
                FilterChip(
                  selected = isSel,
                  onClick = {
                    selectedPhaseTab = phaseId
                    if (phaseId in 1..4) {
                      expandedPhases[phaseId] = true
                    }
                  },
                  modifier = Modifier.testTag("protocol_phase_tab_$phaseId"),
                  label = { Text(label, fontSize = 11.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = theme.accent,
                    selectedLabelColor = Color.White
                  ),
                  border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSel,
                    borderColor = theme.border,
                    selectedBorderColor = theme.accent
                  )
                )
              }
            }
          }
        }

        // -------------------------------------------------------------
        // 4 COLLAPSIBLE PHASES
        // -------------------------------------------------------------
        val visiblePhases = if (selectedPhaseTab == 0) {
          protocolPhasesMeta
        } else {
          protocolPhasesMeta.filter { it.phaseId == selectedPhaseTab }
        }

        items(visiblePhases, key = { "phase_${it.phaseId}" }) { phaseMeta ->
          val phaseSteps = protocolSteps
            .filter { it.phaseId.toInt() == phaseMeta.phaseId }
            .sortedBy { it.sortOrder }
          val phaseDone = phaseSteps.count { it.isCompleted }
          val isExpanded = expandedPhases[phaseMeta.phaseId] ?: phaseMeta.defaultExpanded

          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("protocol_phase_card_${phaseMeta.phaseId}"),
            shape = RoundedCornerShape(18.dp),
            color = theme.cardSurface,
            border = BorderStroke(
              width = if (phaseMeta.phaseId == 2) 1.3.dp else 1.dp,
              color = if (phaseMeta.phaseId == 2) theme.accent.copy(alpha = 0.45f) else theme.border
            )
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              // Phase Collapsible Header
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { expandedPhases[phaseMeta.phaseId] = !isExpanded }
                  .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = phaseMeta.emoji, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = if (isFr) phaseMeta.titleFr else phaseMeta.titleEn,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = theme.primaryText,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                  Spacer(modifier = Modifier.height(3.dp))
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Text(
                      text = if (isFr) phaseMeta.dateRangeFr else phaseMeta.dateRangeEn,
                      style = MaterialTheme.typography.labelSmall,
                      color = theme.mutedText
                    )
                    Surface(
                      shape = RoundedCornerShape(100.dp),
                      color = if (phaseDone == phaseSteps.size && phaseSteps.isNotEmpty()) Color(0xFFF0FDF4) else theme.accent.copy(alpha = 0.12f)
                    ) {
                      Text(
                        text = "$phaseDone/${phaseSteps.size}",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (phaseDone == phaseSteps.size && phaseSteps.isNotEmpty()) Color(0xFF15803D) else theme.accent
                      )
                    }
                  }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(
                    onClick = { openAddStepModal(phaseMeta.phaseId, "13 nov.") },
                    modifier = Modifier
                      .size(48.dp)
                      .testTag("protocol_add_phase_${phaseMeta.phaseId}")
                  ) {
                    Icon(
                      imageVector = Icons.Default.AddCircleOutline,
                      contentDescription = if (isFr) "Ajouter à la phase ${phaseMeta.phaseId}" else "Add to phase ${phaseMeta.phaseId}",
                      tint = theme.accent
                    )
                  }
                  Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = theme.mutedText,
                    modifier = Modifier.size(22.dp)
                  )
                }
              }

              AnimatedVisibility(visible = isExpanded) {
                Column(
                  modifier = Modifier.padding(top = 8.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  if (phaseSteps.isEmpty()) {
                    Text(
                      text = if (isFr) "Aucune étape dans cette phase." else "No steps in this phase.",
                      style = MaterialTheme.typography.bodySmall,
                      color = theme.mutedText
                    )
                  } else {
                    phaseSteps.forEach { step ->
                      ProtocolTimelineStepCard(
                        step = step,
                        theme = theme,
                        language = language,
                        countdownText = if (step.targetEpochMillis > 0L && !step.isCompleted) {
                          formatLiveCountdown(step.targetEpochMillis)
                        } else null,
                        onToggle = { onToggleStep(step) },
                        onEdit = {
                          editingStep = step
                          modalInitialPhaseId = step.phaseId.toInt()
                          modalInitialDate = step.dateStr
                          showStepModal = true
                        },
                        onCopy = {
                          val copyText = "${step.dateStr} • ${step.stepName}\n${step.details}"
                          clipboardManager.setText(AnnotatedString(copyText))
                          Toast.makeText(context, if (isFr) "Étape copiée !" else "Copied!", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = {
                          onDeleteStep(step.id)
                          Toast.makeText(context, if (isFr) "Étape supprimée" else "Deleted", Toast.LENGTH_SHORT).show()
                        }
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Bottom FAB "+ Ajouter Étape"
      FloatingActionButton(
        onClick = { openAddStepModal(if (selectedPhaseTab in 1..4) selectedPhaseTab else 2, "13 nov.") },
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 16.dp, bottom = 16.dp)
          .testTag("protocol_fab_add_step"),
        containerColor = theme.accent,
        contentColor = Color.White,
        shape = RoundedCornerShape(16.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Add, contentDescription = if (isFr) "Ajouter Étape" else "Add Step", modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isFr) "Ajouter Étape" else "Add Step",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
        }
      }
    }
  }

  // Edit / Add Immediate Action Dialog (Section 1)
  if (showImmediateTaskModal) {
    ProtocolImmediateTaskDialog(
      existingTask = editingImmediateTask,
      theme = theme,
      language = language,
      onSave = { savedTask ->
        onSaveImmediateTask(savedTask)
        showImmediateTaskModal = false
        editingImmediateTask = null
        Toast.makeText(context, if (isFr) "Action enregistrée !" else "Action saved!", Toast.LENGTH_SHORT).show()
      },
      onDismiss = {
        showImmediateTaskModal = false
        editingImmediateTask = null
      }
    )
  }

  // Edit / Add Timeline Step Dialog (Section 2)
  if (showStepModal) {
    ProtocolStepEditDialog(
      existingStep = editingStep,
      initialPhaseId = modalInitialPhaseId,
      initialDateStr = modalInitialDate,
      nextSortOrder = (protocolSteps.maxOfOrNull { it.sortOrder } ?: 32L) + 1L,
      theme = theme,
      language = language,
      onSave = { savedStep ->
        onSaveStep(savedStep)
        expandedPhases[savedStep.phaseId.toInt()] = true
        showStepModal = false
        editingStep = null
        Toast.makeText(context, if (isFr) "Étape enregistrée !" else "Step saved!", Toast.LENGTH_SHORT).show()
      },
      onDismiss = {
        showStepModal = false
        editingStep = null
      }
    )
  }

  // Reset Confirmation Dialog
  if (showResetConfirmDialog) {
    ProtocolResetConfirmDialog(
      theme = theme,
      language = language,
      onConfirmReset = {
        onResetProtocol()
        showResetConfirmDialog = false
        Toast.makeText(
          context,
          if (isFr) "Protocole réinitialisé par défaut !" else "Protocol reset to default!",
          Toast.LENGTH_SHORT
        ).show()
      },
      onDismiss = { showResetConfirmDialog = false }
    )
  }
}

@Composable
private fun ProtocolHeaderLine(
  label: String,
  value: String,
  theme: ThemeColors,
  highlightValue: Boolean = false
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top
  ) {
    Text(
      text = "$label ",
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.Bold,
      color = theme.primaryText
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = if (highlightValue) FontWeight.Bold else FontWeight.Normal,
      color = if (highlightValue) theme.accent else theme.mutedText,
      modifier = Modifier.weight(1f),
      lineHeight = 18.sp
    )
  }
}

@Composable
private fun ImmediateClinicalActionCard(
  task: TaskItem,
  theme: ThemeColors,
  language: AppLanguage,
  onToggle: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  val isFr = language == AppLanguage.FR
  val bgColor = if (task.isCompleted) theme.background else theme.accent.copy(alpha = 0.08f)
  val borderColor = if (task.isCompleted) theme.border else theme.accent

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("protocol_immediate_item_${task.id}"),
    shape = RoundedCornerShape(14.dp),
    color = bgColor,
    border = BorderStroke(if (task.isCompleted) 0.9.dp else 1.5.dp, borderColor)
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onToggle,
          modifier = Modifier
            .size(48.dp)
            .testTag("protocol_toggle_immediate_${task.id}")
        ) {
          Surface(
            shape = RoundedCornerShape(7.dp),
            color = if (task.isCompleted) Color(0xFFF0FDF4) else theme.cardSurface,
            border = BorderStroke(1.2.dp, if (task.isCompleted) Color(0xFF16A34A) else theme.accent),
            modifier = Modifier.size(22.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              if (task.isCompleted) {
                Icon(Icons.Default.Check, contentDescription = "Validé", tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
              }
            }
          }
        }

        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (task.isCompleted) Color(0xFFF0FDF4) else theme.accent.copy(alpha = 0.14f)
            ) {
              Text(
                text = task.date,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (task.isCompleted) Color(0xFF15803D) else theme.accent
              )
            }
            if (task.category.isNotBlank()) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = theme.cardSurface,
                border = BorderStroke(0.5.dp, theme.border)
              ) {
                Text(
                  text = task.category,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 10.sp,
                  color = theme.mutedText
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = task.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = theme.primaryText
          )
        }
      }

      if (task.details.isNotBlank()) {
        Text(
          text = task.details,
          style = MaterialTheme.typography.bodySmall,
          color = theme.mutedText,
          lineHeight = 18.sp,
          modifier = Modifier.padding(start = 12.dp)
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        TextButton(
          onClick = onEdit,
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("protocol_edit_immediate_${task.id}")
        ) {
          Icon(Icons.Default.Edit, contentDescription = null, tint = theme.accent, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(if (isFr) "Modifier" else "Edit", style = MaterialTheme.typography.labelMedium, color = theme.accent, fontWeight = FontWeight.SemiBold)
        }

        IconButton(
          onClick = onDelete,
          modifier = Modifier
            .size(48.dp)
            .testTag("protocol_delete_immediate_${task.id}")
        ) {
          Icon(Icons.Default.DeleteOutline, contentDescription = if (isFr) "Supprimer" else "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
        }
      }
    }
  }
}

@Composable
private fun ProtocolTimelineStepCard(
  step: SyncedProtocolStep,
  theme: ThemeColors,
  language: AppLanguage,
  countdownText: String?,
  onToggle: () -> Unit,
  onEdit: () -> Unit,
  onCopy: () -> Unit,
  onDelete: () -> Unit
) {
  val isFr = language == AppLanguage.FR
  val isTodayOrActive = !step.isCompleted && (
    step.status.equals("Aujourd'hui", ignoreCase = true) ||
      step.dateStr.contains("Aujourd'hui", ignoreCase = true) ||
      step.status.equals("En cours", ignoreCase = true)
    )
  val isPriorityUpcoming = !step.isCompleted && !isTodayOrActive &&
    (step.status.equals("Prioritaire", ignoreCase = true) || step.isCriticalWindow)

  val cardBg = when {
    isTodayOrActive -> theme.accent.copy(alpha = 0.11f)
    isPriorityUpcoming -> Color(0xFFFFFBEB)
    step.isCompleted -> theme.background.copy(alpha = 0.72f)
    else -> theme.background
  }
  val cardBorder = when {
    isTodayOrActive -> theme.accent
    isPriorityUpcoming -> Color(0xFFD97706)
    else -> theme.border
  }

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("protocol_step_card_${step.id}"),
    shape = RoundedCornerShape(14.dp),
    color = cardBg,
    border = BorderStroke(if (isTodayOrActive) 2.dp else if (isPriorityUpcoming) 1.3.dp else 0.9.dp, cardBorder)
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      // Top Row: Checkbox + Date Pill + Status Badge + Medication Pill
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onToggle,
          modifier = Modifier
            .size(48.dp)
            .testTag("protocol_toggle_step_${step.id}")
        ) {
          Surface(
            shape = RoundedCornerShape(7.dp),
            color = if (step.isCompleted) Color(0xFFF0FDF4) else theme.cardSurface,
            border = BorderStroke(1.2.dp, if (step.isCompleted) Color(0xFF16A34A) else cardBorder),
            modifier = Modifier.size(22.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              if (step.isCompleted) {
                Icon(Icons.Default.Check, contentDescription = "Complété", tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
              }
            }
          }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Date pill
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (isTodayOrActive) theme.accent else theme.cardSurface,
              border = if (isTodayOrActive) null else BorderStroke(0.6.dp, theme.border)
            ) {
              Text(
                text = step.dateStr,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isTodayOrActive) Color.White else theme.primaryText
              )
            }

            // Status pill
            val (statusBg, statusFg, statusLabel) = when {
              step.isCompleted -> Triple(Color(0xFFF0FDF4), Color(0xFF15803D), if (isFr) "Complété" else "Completed")
              step.status.equals("Aujourd'hui", ignoreCase = true) || step.dateStr.contains("Aujourd'hui", ignoreCase = true) ->
                Triple(theme.accent.copy(alpha = 0.16f), theme.accent, "AUJOURD'HUI • EN COURS")
              step.status.equals("En cours", ignoreCase = true) ->
                Triple(theme.accent.copy(alpha = 0.14f), theme.accent, if (isFr) "En cours" else "In Progress")
              isPriorityUpcoming ->
                Triple(Color(0xFFFEF3C7), Color(0xFFB45309), if (isFr) "Prioritaire" else "Priority")
              else ->
                Triple(theme.cardSurface, theme.mutedText, step.status.ifBlank { if (isFr) "Planifié" else "Scheduled" })
            }
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = statusBg
            ) {
              Text(
                text = statusLabel,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = statusFg
              )
            }
          }

          if (step.medication.isNotBlank() || step.dosage.isNotBlank()) {
            Text(
              text = listOf(step.medication, step.dosage).filter { it.isNotBlank() }.joinToString(" • "),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = theme.accent
            )
          }
        }
      }

      // Step Title
      Text(
        text = step.stepName,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = theme.primaryText,
        lineHeight = 20.sp,
        modifier = Modifier.padding(start = 10.dp)
      )

      // Clinical Notes / Details
      if (step.details.isNotBlank()) {
        Text(
          text = step.details,
          style = MaterialTheme.typography.bodySmall,
          color = if (isTodayOrActive) theme.primaryText else theme.mutedText,
          lineHeight = 18.sp,
          modifier = Modifier.padding(start = 10.dp)
        )
      }

      // Live Countdown Banner if applicable
      if (countdownText != null) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFFFEF2F2),
          border = BorderStroke(0.6.dp, Color(0xFFFECACA)),
          modifier = Modifier.padding(start = 10.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = "${if (isFr) "Prochaine prise dans" else "Next dose in"} : $countdownText",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFDC2626)
            )
          }
        }
      }

      // Bottom Actions: Copy, Modifier, Supprimer
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onCopy,
          modifier = Modifier.size(48.dp)
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = if (isFr) "Copier" else "Copy", tint = theme.mutedText, modifier = Modifier.size(16.dp))
        }

        TextButton(
          onClick = onEdit,
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("protocol_edit_step_${step.id}")
        ) {
          Icon(Icons.Default.Edit, contentDescription = null, tint = theme.accent, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (isFr) "Modifier" else "Edit",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = theme.accent
          )
        }

        IconButton(
          onClick = onDelete,
          modifier = Modifier
            .size(48.dp)
            .testTag("protocol_delete_step_${step.id}")
        ) {
          Icon(Icons.Default.DeleteOutline, contentDescription = if (isFr) "Supprimer" else "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
        }
      }
    }
  }
}

@Composable
private fun NovemberCalendarGrid(
  selectedDayNumber: Int,
  theme: ThemeColors,
  language: AppLanguage,
  onDayClick: (Int) -> Unit
) {
  Column(modifier = Modifier.padding(top = 8.dp)) {
    val weekDays = if (language == AppLanguage.FR) {
      listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
    } else {
      listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      weekDays.forEach { wd ->
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
          Text(text = wd, style = MaterialTheme.typography.labelSmall, color = theme.mutedText, fontSize = 10.sp)
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    val totalLeadingDays = 6
    val daysInNov = 30
    val totalCells = ((totalLeadingDays + daysInNov + 6) / 7) * 7
    val highlightDays = setOf(5, 6, 13, 17, 18, 20, 27)

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
      for (weekRow in 0 until (totalCells / 7)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          for (col in 0 until 7) {
            val dayNum = weekRow * 7 + col - totalLeadingDays + 1
            if (dayNum in 1..daysInNov) {
              val isSel = selectedDayNumber == dayNum
              val isToday = dayNum == 13
              val hasEvent = dayNum in highlightDays
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = when {
                  isSel -> theme.accent
                  isToday -> theme.accent.copy(alpha = 0.16f)
                  else -> theme.background
                },
                border = BorderStroke(0.6.dp, if (isSel || isToday) theme.accent else theme.border),
                modifier = Modifier
                  .weight(1f)
                  .height(28.dp)
                  .padding(horizontal = 1.dp)
                  .clickable { onDayClick(dayNum) }
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = "$dayNum",
                      style = MaterialTheme.typography.labelSmall,
                      fontSize = 10.sp,
                      fontWeight = if (isSel || isToday) FontWeight.Bold else FontWeight.Normal,
                      color = if (isSel) Color.White else theme.primaryText
                    )
                    if (hasEvent && !isSel) {
                      Spacer(modifier = Modifier.width(2.dp))
                      Box(
                        modifier = Modifier
                          .size(4.dp)
                          .clip(CircleShape)
                          .background(theme.accent)
                      )
                    }
                  }
                }
              }
            } else {
              Spacer(modifier = Modifier.weight(1f).padding(horizontal = 1.dp))
            }
          }
        }
      }
    }
  }
}
