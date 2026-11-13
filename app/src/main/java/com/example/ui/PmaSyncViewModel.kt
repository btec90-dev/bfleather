package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CloudSyncSummary
import com.example.data.FirestoreRepository
import com.example.data.SyncedAppointmentItem
import com.example.data.SyncedCareProvider
import com.example.data.SyncedChecklistItem
import com.example.data.SyncedExpenseRecord
import com.example.data.SyncedGroceryItem
import com.example.data.SyncedHealthIssue
import com.example.data.SyncedMedReminder
import com.example.data.SyncedMedicalDocItem
import com.example.data.SyncedNoteItem
import com.example.data.SyncedNutritionFood
import com.example.data.SyncedPregnancyMed
import com.example.data.SyncedProtocolStep
import com.example.data.SyncedTaskItem
import com.example.data.SyncedUserProfile
import com.example.data.SyncedVoiceMemo
import com.example.data.SyncedWalletCard
import com.example.data.TaskItem
import com.example.data.WalletSyncSnapshot
import com.example.data.WalletSyncStatus
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PmaSyncViewModel(
  private val repository: FirestoreRepository,
  private val currentUserId: String
) : ViewModel() {

  private val _isSeeding = MutableStateFlow(true)
  val isSeeding: StateFlow<Boolean> = _isSeeding.asStateFlow()

  private val _isManualSyncing = MutableStateFlow(false)
  val isManualSyncing: StateFlow<Boolean> = _isManualSyncing.asStateFlow()

  private val _syncError = MutableStateFlow<String?>(null)
  val syncError: StateFlow<String?> = _syncError.asStateFlow()

  private val _lastSyncedEpoch = MutableStateFlow<Long?>(null)
  val lastSyncedEpoch: StateFlow<Long?> = _lastSyncedEpoch.asStateFlow()

  private val _lastSyncTrigger = MutableStateFlow("LIVE")

  val profile: StateFlow<SyncedUserProfile> = repository.observeProfile(currentUserId)
    .map { it ?: SyncedUserProfile(userId = currentUserId) }
    .trackSync("profile")
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = SyncedUserProfile(userId = currentUserId)
    )

  val tasks: StateFlow<List<TaskItem>> = repository.observeTasks(currentUserId)
    .map { list -> list.map(SyncedTaskItem::toTaskItem) }
    .trackSync("tasks")
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList()
    )

  val notes: StateFlow<List<SyncedNoteItem>> = repository.observeNotes(currentUserId)
    .trackSync("notes")
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList()
    )

  val appointments: StateFlow<List<SyncedAppointmentItem>> = repository.observeAppointments(currentUserId)
    .trackSync("appointments")
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList()
    )

  val documents: StateFlow<List<SyncedMedicalDocItem>> = repository.observeDocuments(currentUserId)
    .trackSync("documents")
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList()
    )

  val checklists: StateFlow<List<SyncedChecklistItem>> = repository.observeChecklists(currentUserId)
    .trackSync("checklists")
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList()
    )

  val protocolSteps: StateFlow<List<SyncedProtocolStep>> = repository.observeProtocolSteps(currentUserId)
    .trackSync("protocol_steps")
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList()
    )

  val careProviders: StateFlow<List<SyncedCareProvider>> = repository.observeCareProviders(currentUserId)
    .trackSync("care_providers")
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList()
    )

  val voiceMemos: StateFlow<List<SyncedVoiceMemo>> = repository.observeVoiceMemos(currentUserId)
    .trackSync("voice_memos")
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList()
    )

  private val walletSnapshot: StateFlow<WalletSyncSnapshot> = repository.observeWalletCards(currentUserId)
    .trackSync("wallet_cards")
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = WalletSyncSnapshot(emptyList(), WalletSyncStatus.SYNCING)
    )

  val walletCards: StateFlow<List<SyncedWalletCard>> = walletSnapshot
    .map { it.cards }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList()
    )

  val walletSyncStatus: StateFlow<WalletSyncStatus> = combine(
    walletSnapshot,
    _isManualSyncing,
    _syncError
  ) { snap, manualSyncing, error ->
    when {
      error != null -> WalletSyncStatus.ERROR
      manualSyncing -> WalletSyncStatus.SYNCING
      else -> snap.syncStatus
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
    initialValue = WalletSyncStatus.SYNCING
  )

  val medReminders: StateFlow<List<SyncedMedReminder>> = repository.observeMedReminders(currentUserId)
    .trackSync("med_reminders")
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList()
    )

  val pregnancyMeds: StateFlow<List<SyncedPregnancyMed>> = repository.observePregnancyMeds(currentUserId)
    .trackSync("preg_medications")
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList()
    )

  val nutritionFoods: StateFlow<List<SyncedNutritionFood>> = repository.observeNutritionFoods(currentUserId)
    .trackSync("nutrition_foods")
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList()
    )

  val groceryItems: StateFlow<List<SyncedGroceryItem>> = repository.observeGroceryItems(currentUserId)
    .trackSync("grocery_items")
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList()
    )

  val healthIssues: StateFlow<List<SyncedHealthIssue>> = repository.observeHealthIssues(currentUserId)
    .trackSync("health_issues")
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList()
    )

  val expenses: StateFlow<List<SyncedExpenseRecord>> = repository.observeExpenses(currentUserId)
    .trackSync("expenses")
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList()
    )

  private data class SyncStatusMeta(
    val lastSyncedEpochMillis: Long?,
    val isSyncing: Boolean,
    val syncError: String?,
    val trigger: String
  )

  private data class PrimaryRecordCounts(
    val profileCount: Int,
    val tasksCount: Int,
    val notesCount: Int,
    val appointmentsCount: Int,
    val documentsCount: Int
  )

  private data class SecondaryRecordCounts(
    val checklistsCount: Int,
    val protocolStepsCount: Int,
    val careProvidersCount: Int,
    val voiceMemosCount: Int,
    val walletCardsCount: Int
  )

  private data class HealthHubRecordCounts(
    val medRemindersCount: Int,
    val healthHubRecordsCount: Int
  )

  private val healthHubCountsFlow: Flow<HealthHubRecordCounts> = combine(
    medReminders,
    pregnancyMeds,
    nutritionFoods,
    groceryItems,
    combine(healthIssues, expenses) { hi, exp -> hi.size + exp.size }
  ) { mr, pm, nf, gr, hiExpCount ->
    HealthHubRecordCounts(
      medRemindersCount = mr.size,
      healthHubRecordsCount = pm.size + nf.size + gr.size + hiExpCount
    )
  }

  val syncSummary: StateFlow<CloudSyncSummary> = combine(
    combine(_lastSyncedEpoch, _isSeeding, _isManualSyncing, _syncError, _lastSyncTrigger) { epoch, seeding, manual, error, trigger ->
      SyncStatusMeta(epoch, seeding || manual, error, trigger)
    },
    combine(profile, tasks, notes, appointments, documents) { p, t, n, a, d ->
      PrimaryRecordCounts(
        profileCount = if (p.userId.isNotBlank()) 1 else 0,
        tasksCount = t.size,
        notesCount = n.size,
        appointmentsCount = a.size,
        documentsCount = d.size
      )
    },
    combine(checklists, protocolSteps, careProviders, voiceMemos, walletCards) { c, ps, cp, vm, wc ->
      SecondaryRecordCounts(
        checklistsCount = c.size,
        protocolStepsCount = ps.size,
        careProvidersCount = cp.size,
        voiceMemosCount = vm.size,
        walletCardsCount = wc.size
      )
    },
    healthHubCountsFlow
  ) { meta, primary, secondary, hub ->
    CloudSyncSummary(
      lastSyncedEpochMillis = meta.lastSyncedEpochMillis,
      isSyncing = meta.isSyncing,
      syncError = meta.syncError,
      lastSyncTrigger = meta.trigger,
      profileCount = primary.profileCount,
      tasksCount = primary.tasksCount,
      notesCount = primary.notesCount,
      appointmentsCount = primary.appointmentsCount,
      documentsCount = primary.documentsCount,
      checklistsCount = secondary.checklistsCount,
      protocolStepsCount = secondary.protocolStepsCount,
      careProvidersCount = secondary.careProvidersCount,
      voiceMemosCount = secondary.voiceMemosCount,
      walletCardsCount = secondary.walletCardsCount,
      medRemindersCount = hub.medRemindersCount,
      healthHubRecordsCount = hub.healthHubRecordsCount
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
    initialValue = CloudSyncSummary(isSyncing = true)
  )

  init {
    seedInitialData()
  }

  fun initiateSync() {
    if (_isManualSyncing.value) return
    viewModelScope.launch {
      _isManualSyncing.value = true
      _syncError.value = null
      try {
        val fetchedSummary = repository.forceFetchFromCloud(currentUserId)
        _lastSyncTrigger.value = "MANUAL"
        _lastSyncedEpoch.value = fetchedSummary.lastSyncedEpochMillis ?: System.currentTimeMillis()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val root = unwrapFirestoreError(e)
        Log.e(TAG, "Manual cloud sync failed", root)
        _syncError.value = root.localizedMessage ?: "Cloud fetch failed"
      } finally {
        _isManualSyncing.value = false
      }
    }
  }

  fun retrySync() {
    initiateSync()
  }

  fun clearSyncError() {
    _syncError.value = null
  }

  private fun seedInitialData() {
    viewModelScope.launch {
      _isSeeding.value = true
      _syncError.value = null
      try {
        repository.ensureInitialSeedData(currentUserId)
        _lastSyncTrigger.value = "LIVE"
        _lastSyncedEpoch.value = System.currentTimeMillis()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val root = unwrapFirestoreError(e)
        Log.e(TAG, "Initial seed failed", root)
        _syncError.value = root.localizedMessage ?: "Cloud sync initialization failed"
      } finally {
        _isSeeding.value = false
      }
    }
  }

  private fun <T> Flow<T>.trackSync(label: String): Flow<T> =
    onEach {
      _lastSyncedEpoch.value = System.currentTimeMillis()
    }.catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val root = firestoreError ?: error
      Log.w(TAG, "Error observing $label", root)
      _syncError.value = root.localizedMessage ?: "Failed to sync $label"
    }

  private fun runWrite(label: String, block: suspend () -> Unit) {
    viewModelScope.launch {
      try {
        _syncError.value = null
        block()
        _lastSyncTrigger.value = "LIVE"
        _lastSyncedEpoch.value = System.currentTimeMillis()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val root = unwrapFirestoreError(e)
        Log.e(TAG, "Write error ($label)", root)
        _syncError.value = root.localizedMessage ?: "Sync write failed ($label)"
      }
    }
  }

  private fun unwrapFirestoreError(error: Throwable): Throwable =
    generateSequence(error) { it.cause }
      .firstOrNull { it is FirebaseFirestoreException }
      ?: error

  // Tasks
  fun toggleTask(task: TaskItem) = runWrite("toggleTask") { repository.toggleTask(task) }
  fun saveTask(task: TaskItem) = runWrite("saveTask") { repository.saveTask(task) }
  fun deleteTask(taskId: String) = runWrite("deleteTask") { repository.deleteTask(taskId) }

  // Profile & Preferences
  fun updatePregnancyProfile(patientName: String, patientAge: String, currentWeek: Int, dueDate: String) =
    runWrite("updatePregnancyProfile") {
      repository.updatePregnancyProfile(patientName, patientAge, currentWeek, dueDate)
    }

  fun updateCurrentWeek(week: Int) = runWrite("updateCurrentWeek") { repository.updateCurrentWeek(week) }

  fun updateDashboardProgress(
    dueDateText: String,
    gestationalWeeks: Int,
    gestationalDaysRemainder: Int,
    remainingCountdownDays: Int,
    currentTrimester: String,
    progressPercent: Float
  ) = runWrite("updateDashboardProgress") {
    repository.updateDashboardProgress(
      dueDateText, gestationalWeeks, gestationalDaysRemainder,
      remainingCountdownDays, currentTrimester, progressPercent
    )
  }

  fun updateWaterGlasses(glasses: Int) = runWrite("updateWaterGlasses") {
    repository.updateWaterGlasses(glasses)
  }

  fun updateHealthVitals(
    healthMood: String,
    healthWeight: String,
    waterGlasses: Int,
    bloodPressure: String,
    personalNotesText: String
  ) = runWrite("updateHealthVitals") {
    repository.updateHealthVitals(healthMood, healthWeight, waterGlasses, bloodPressure, personalNotesText)
  }

  fun updatePreferences(
    momAvatar: String,
    partnerAvatar: String,
    babyAvatar: String,
    languageCode: String,
    themePaletteName: String
  ) = runWrite("updatePreferences") {
    repository.updatePreferences(momAvatar, partnerAvatar, babyAvatar, languageCode, themePaletteName)
  }

  // Notes
  fun saveNote(note: SyncedNoteItem) = runWrite("saveNote") { repository.saveNote(note) }
  fun deleteNote(noteId: String) = runWrite("deleteNote") { repository.deleteNote(noteId) }

  // Appointments & Calendar Events
  fun saveAppointment(apt: SyncedAppointmentItem) = runWrite("saveAppointment") { repository.saveAppointment(apt) }
  fun toggleAppointment(aptId: String, currentCompleted: Boolean) = runWrite("toggleAppointment") {
    repository.toggleAppointment(aptId, currentCompleted)
  }
  fun deleteAppointment(aptId: String) = runWrite("deleteAppointment") { repository.deleteAppointment(aptId) }

  // Medical Documents
  fun saveDocument(doc: SyncedMedicalDocItem) = runWrite("saveDocument") { repository.saveDocument(doc) }
  fun deleteDocument(docId: String) = runWrite("deleteDocument") { repository.deleteDocument(docId) }

  // Checklists
  fun saveChecklistItem(item: SyncedChecklistItem) = runWrite("saveChecklistItem") { repository.saveChecklistItem(item) }
  fun toggleChecklistItem(itemId: String, currentChecked: Boolean) = runWrite("toggleChecklistItem") {
    repository.toggleChecklistItem(itemId, currentChecked)
  }

  // Protocol Steps
  fun saveProtocolStep(step: SyncedProtocolStep) = runWrite("saveProtocolStep") { repository.saveProtocolStep(step) }
  fun toggleProtocolStep(step: SyncedProtocolStep) = runWrite("toggleProtocolStep") { repository.toggleProtocolStep(step) }
  fun deleteProtocolStep(stepId: String) = runWrite("deleteProtocolStep") { repository.deleteProtocolStep(stepId) }
  fun resetProtocolToDefault() = runWrite("resetProtocolToDefault") { repository.resetProtocolToDefault(currentUserId) }

  // Care Providers
  fun saveCareProvider(provider: SyncedCareProvider) = runWrite("saveCareProvider") {
    repository.saveCareProvider(provider)
  }
  fun deleteCareProvider(providerId: String) = runWrite("deleteCareProvider") {
    repository.deleteCareProvider(providerId)
  }

  // Voice Memos
  fun saveVoiceMemo(memo: SyncedVoiceMemo) = runWrite("saveVoiceMemo") { repository.saveVoiceMemo(memo) }
  fun deleteVoiceMemo(memoId: String) = runWrite("deleteVoiceMemo") { repository.deleteVoiceMemo(memoId) }

  // Digital Wallet Cards
  fun saveWalletCard(card: SyncedWalletCard) = runWrite("saveWalletCard") { repository.saveWalletCard(card) }
  fun deleteWalletCard(cardId: String) = runWrite("deleteWalletCard") { repository.deleteWalletCard(cardId) }
  fun seedSampleWalletCards() = runWrite("seedSampleWalletCards") { repository.seedSampleWalletCards(currentUserId) }

  // Medication Reminders
  fun saveMedReminder(reminder: SyncedMedReminder) = runWrite("saveMedReminder") { repository.saveMedReminder(reminder) }
  fun toggleMedReminder(reminderId: String, currentEnabled: Boolean) = runWrite("toggleMedReminder") {
    repository.toggleMedReminder(reminderId, currentEnabled)
  }
  fun deleteMedReminder(reminderId: String) = runWrite("deleteMedReminder") { repository.deleteMedReminder(reminderId) }

  // Pregnancy Health Hub: Medications Database
  fun savePregnancyMed(med: SyncedPregnancyMed) = runWrite("savePregnancyMed") { repository.savePregnancyMed(med) }
  fun deletePregnancyMed(medId: String) = runWrite("deletePregnancyMed") { repository.deletePregnancyMed(medId) }

  // Pregnancy Health Hub: Nutrition & Grocery
  fun saveNutritionFood(food: SyncedNutritionFood) = runWrite("saveNutritionFood") { repository.saveNutritionFood(food) }
  fun deleteNutritionFood(foodId: String) = runWrite("deleteNutritionFood") { repository.deleteNutritionFood(foodId) }
  fun saveGroceryItem(item: SyncedGroceryItem) = runWrite("saveGroceryItem") { repository.saveGroceryItem(item) }
  fun toggleGroceryItem(itemId: String, currentChecked: Boolean) = runWrite("toggleGroceryItem") {
    repository.toggleGroceryItem(itemId, currentChecked)
  }
  fun deleteGroceryItem(itemId: String) = runWrite("deleteGroceryItem") { repository.deleteGroceryItem(itemId) }

  // Pregnancy Health Hub: Health Issues
  fun saveHealthIssue(issue: SyncedHealthIssue) = runWrite("saveHealthIssue") { repository.saveHealthIssue(issue) }
  fun toggleHealthIssue(issueId: String, currentResolved: Boolean) = runWrite("toggleHealthIssue") {
    repository.toggleHealthIssue(issueId, currentResolved)
  }
  fun deleteHealthIssue(issueId: String) = runWrite("deleteHealthIssue") { repository.deleteHealthIssue(issueId) }

  // Pregnancy Health Hub: Multi-Currency Expenses
  fun saveExpense(expense: SyncedExpenseRecord) = runWrite("saveExpense") { repository.saveExpense(expense) }
  fun deleteExpense(expenseId: String) = runWrite("deleteExpense") { repository.deleteExpense(expenseId) }

  private companion object {
    const val TAG = "PmaSyncVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
