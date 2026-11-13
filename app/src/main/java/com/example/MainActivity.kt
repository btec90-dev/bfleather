package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PregnantWoman
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.FirestoreRepository
import com.example.i18n.AppLanguage
import com.example.i18n.Strings
import com.example.ui.*
import com.example.ui.theme.AppTheme
import com.example.ui.theme.AppThemePalette
import com.example.ui.theme.AppThemeProvider
import com.example.ui.theme.ThemeColors
import com.example.ui.theme.ThemeManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.launch

private var isFirestoreOfflineConfigured = false

private fun getOfflineEnabledFirestore(dbId: String): FirebaseFirestore {
  val db = FirebaseFirestore.getInstance(dbId)
  if (!isFirestoreOfflineConfigured) {
    isFirestoreOfflineConfigured = true
    runCatching {
      db.firestoreSettings = FirebaseFirestoreSettings.Builder()
        .setLocalCacheSettings(
          PersistentCacheSettings.newBuilder()
            .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        )
        .build()
    }
  }
  return db
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AppThemeProvider(themeManager = ThemeManager) {
        var showSplash by remember { mutableStateOf(true) }
        var isCheckingAutoSignIn by remember { mutableStateOf(true) }
        var currentLanguage by remember { mutableStateOf(AppLanguage.FR) }

        val context = LocalContext.current
        val activeTheme = AppTheme.colors
        val currentUser by Firebase.auth.authStateFlow()
          .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

        // Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          val notifPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
          ) { }

          LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
              notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
          }
        }

        // Attempt silent returning-user sign-in with Credential Manager on cold start
        LaunchedEffect(Unit) {
          attemptAutoSignIn(this@MainActivity)
          isCheckingAutoSignIn = false
        }

        if (showSplash) {
          SplashScreen(onTimeout = { showSplash = false })
        } else if (currentUser == null) {
          AuthScreen(
            theme = activeTheme,
            language = currentLanguage,
            isCheckingSession = isCheckingAutoSignIn
          )
        } else {
          val user = currentUser!!
          key(user.uid) {
            AuthenticatedAppContent(
              uid = user.uid,
              userEmail = user.email ?: user.uid,
              currentLanguage = currentLanguage,
              onLanguageChange = { currentLanguage = it },
              activeTheme = activeTheme
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AuthenticatedAppContent(
  uid: String,
  userEmail: String,
  currentLanguage: AppLanguage,
  onLanguageChange: (AppLanguage) -> Unit,
  activeTheme: ThemeColors
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var currentTab by remember { mutableIntStateOf(0) }
  val activePalette = AppTheme.palette

  val syncViewModel: PmaSyncViewModel = viewModel(
    key = uid,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val dbId = app.getString(R.string.firestore_database_id)
        val db = getOfflineEnabledFirestore(dbId)
        PmaSyncViewModel(FirestoreRepository(db, Firebase.auth), uid)
      }
    }
  )

  val profile by syncViewModel.profile.collectAsStateWithLifecycle()
  val tasksState by syncViewModel.tasks.collectAsStateWithLifecycle()
  val notesState by syncViewModel.notes.collectAsStateWithLifecycle()
  val appointmentsState by syncViewModel.appointments.collectAsStateWithLifecycle()
  val documentsState by syncViewModel.documents.collectAsStateWithLifecycle()
  val checklistsState by syncViewModel.checklists.collectAsStateWithLifecycle()
  val protocolStepsState by syncViewModel.protocolSteps.collectAsStateWithLifecycle()
  val careProvidersState by syncViewModel.careProviders.collectAsStateWithLifecycle()
  val voiceMemosState by syncViewModel.voiceMemos.collectAsStateWithLifecycle()
  val walletCardsState by syncViewModel.walletCards.collectAsStateWithLifecycle()
  val walletSyncStatus by syncViewModel.walletSyncStatus.collectAsStateWithLifecycle()
  val syncSummary by syncViewModel.syncSummary.collectAsStateWithLifecycle()
  val galleryViewModel: GalleryViewModel = viewModel()
  var showSyncStatusModal by remember { mutableStateOf(false) }

  // Apply synced theme & language preferences whenever remote profile updates
  LaunchedEffect(profile.themePaletteName, profile.languageCode) {
    val parsedPalette = runCatching { AppThemePalette.valueOf(profile.themePaletteName) }
      .getOrDefault(AppThemePalette.CARAMEL)
    if (parsedPalette != activePalette) {
      ThemeManager.setPalette(parsedPalette)
    }
    val syncedLang = if (profile.languageCode == "EN") AppLanguage.EN else AppLanguage.FR
    if (syncedLang != currentLanguage) {
      onLanguageChange(syncedLang)
    }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = activeTheme.background,
    bottomBar = {
      NavigationBar(
        containerColor = activeTheme.headerFooterBg,
        contentColor = activeTheme.primaryText,
        tonalElevation = 0.dp,
        modifier = Modifier
          .fillMaxWidth()
          .border(BorderStroke(1.dp, activeTheme.border))
      ) {
        NavigationBarItem(
          icon = {
            Icon(
              Icons.Default.Home,
              contentDescription = Strings.t("dashboard_tab", currentLanguage),
              modifier = Modifier.size(22.dp)
            )
          },
          label = {
            Text(
              text = Strings.t("dashboard_tab", currentLanguage),
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.sp),
              maxLines = 1
            )
          },
          alwaysShowLabel = true,
          selected = currentTab == 0,
          onClick = { currentTab = 0 },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = activeTheme.primaryText,
            selectedTextColor = activeTheme.primaryText,
            indicatorColor = activeTheme.accent.copy(alpha = 0.16f),
            unselectedIconColor = activeTheme.mutedText,
            unselectedTextColor = activeTheme.mutedText
          )
        )

        NavigationBarItem(
          icon = {
            Icon(
              Icons.Default.Timeline,
              contentDescription = Strings.t("protocol_tab", currentLanguage),
              modifier = Modifier.size(22.dp)
            )
          },
          label = {
            Text(
              text = Strings.t("protocol_tab", currentLanguage),
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.sp),
              maxLines = 1
            )
          },
          alwaysShowLabel = true,
          selected = currentTab == 1,
          onClick = { currentTab = 1 },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = activeTheme.primaryText,
            selectedTextColor = activeTheme.primaryText,
            indicatorColor = activeTheme.accent.copy(alpha = 0.16f),
            unselectedIconColor = activeTheme.mutedText,
            unselectedTextColor = activeTheme.mutedText
          )
        )

        NavigationBarItem(
          icon = {
            Icon(
              Icons.Default.PregnantWoman,
              contentDescription = Strings.t("pregnancy_tab", currentLanguage),
              modifier = Modifier.size(22.dp)
            )
          },
          label = {
            Text(
              text = Strings.t("pregnancy_tab", currentLanguage),
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.sp),
              maxLines = 1
            )
          },
          alwaysShowLabel = true,
          selected = currentTab == 2,
          onClick = { currentTab = 2 },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = activeTheme.primaryText,
            selectedTextColor = activeTheme.primaryText,
            indicatorColor = activeTheme.accent.copy(alpha = 0.16f),
            unselectedIconColor = activeTheme.mutedText,
            unselectedTextColor = activeTheme.mutedText
          )
        )

        NavigationBarItem(
          icon = {
            Icon(
              Icons.Default.AutoAwesome,
              contentDescription = if (currentLanguage == AppLanguage.FR) "Base Médicale" else "Medical DB",
              modifier = Modifier.size(22.dp)
            )
          },
          label = {
            Text(
              text = if (currentLanguage == AppLanguage.FR) "Base Médicale" else "Medical DB",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.sp),
              maxLines = 1
            )
          },
          alwaysShowLabel = true,
          selected = currentTab == 3,
          onClick = { currentTab = 3 },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = activeTheme.primaryText,
            selectedTextColor = activeTheme.primaryText,
            indicatorColor = activeTheme.accent.copy(alpha = 0.16f),
            unselectedIconColor = activeTheme.mutedText,
            unselectedTextColor = activeTheme.mutedText
          )
        )

        NavigationBarItem(
          icon = {
            Icon(
              Icons.Default.Settings,
              contentDescription = Strings.t("settings_tab", currentLanguage),
              modifier = Modifier.size(22.dp)
            )
          },
          label = {
            Text(
              text = Strings.t("settings_tab", currentLanguage),
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.sp),
              maxLines = 1
            )
          },
          alwaysShowLabel = true,
          selected = currentTab == 4,
          onClick = { currentTab = 4 },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = activeTheme.primaryText,
            selectedTextColor = activeTheme.primaryText,
            indicatorColor = activeTheme.accent.copy(alpha = 0.16f),
            unselectedIconColor = activeTheme.mutedText,
            unselectedTextColor = activeTheme.mutedText
          )
        )
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      CloudSyncStatusBanner(
        userEmail = userEmail,
        isSeeding = syncSummary.isSyncing,
        syncError = syncSummary.syncError,
        lastSyncedEpochMillis = syncSummary.lastSyncedEpochMillis,
        language = currentLanguage,
        theme = activeTheme,
        onRetry = { syncViewModel.initiateSync() },
        onOpenSyncDashboard = { showSyncStatusModal = true }
      )

      Box(modifier = Modifier.weight(1f)) {
        when (currentTab) {
          0 -> DashboardScreen(
            userId = uid,
            userEmail = userEmail,
            syncSummary = syncSummary,
            tasks = tasksState,
            profile = profile,
            notes = notesState,
            documents = documentsState.filter { it.source == "vault" },
            voiceMemos = voiceMemosState,
            walletCards = walletCardsState,
            walletSyncStatus = walletSyncStatus,
            onInitiateSync = { syncViewModel.initiateSync() },
            onTaskToggled = { syncViewModel.toggleTask(it) },
            onTaskAdded = { syncViewModel.saveTask(it) },
            onTaskDeleted = { syncViewModel.deleteTask(it) },
            onUpdateDashboardProgress = { dueDateText, weeks, daysRem, countdownDays, trimester, progress ->
              syncViewModel.updateDashboardProgress(dueDateText, weeks, daysRem, countdownDays, trimester, progress)
            },
            onSaveNote = { syncViewModel.saveNote(it) },
            onDeleteNote = { syncViewModel.deleteNote(it) },
            onSaveDocument = { syncViewModel.saveDocument(it) },
            onSaveVoiceMemo = { syncViewModel.saveVoiceMemo(it) },
            onDeleteVoiceMemo = { syncViewModel.deleteVoiceMemo(it) },
            onSaveWalletCard = { syncViewModel.saveWalletCard(it) },
            onDeleteWalletCard = { syncViewModel.deleteWalletCard(it) },
            onSeedSampleWalletCards = { syncViewModel.seedSampleWalletCards() },
            onNavigateToPregnancy = { currentTab = 2 },
            theme = activeTheme,
            language = currentLanguage
          )
          1 -> ProtocolScreen(
            protocolSteps = protocolStepsState,
            immediateTasks = tasksState,
            onSaveStep = { syncViewModel.saveProtocolStep(it) },
            onToggleStep = { syncViewModel.toggleProtocolStep(it) },
            onDeleteStep = { syncViewModel.deleteProtocolStep(it) },
            onSaveImmediateTask = { syncViewModel.saveTask(it) },
            onToggleImmediateTask = { syncViewModel.toggleTask(it) },
            onDeleteImmediateTask = { syncViewModel.deleteTask(it) },
            onResetProtocol = { syncViewModel.resetProtocolToDefault() },
            theme = activeTheme,
            language = currentLanguage
          )
          2 -> PregnancyHubScreen(
            userEmail = userEmail,
            profile = profile,
            appointments = appointmentsState,
            documents = documentsState.filter { it.source == "hub" },
            checklists = checklistsState,
            galleryViewModel = galleryViewModel,
            onUpdateProfile = { name, age, week, dueDate ->
              syncViewModel.updatePregnancyProfile(name, age, week, dueDate)
            },
            onUpdateWeek = { week -> syncViewModel.updateCurrentWeek(week) },
            onUpdateWater = { glasses -> syncViewModel.updateWaterGlasses(glasses) },
            onSaveHealthVitals = { mood, weight, water, bp, notesText ->
              syncViewModel.updateHealthVitals(mood, weight, water, bp, notesText)
            },
            onSaveAppointment = { syncViewModel.saveAppointment(it) },
            onToggleAppointment = { id, checked -> syncViewModel.toggleAppointment(id, checked) },
            onSaveDocument = { syncViewModel.saveDocument(it) },
            onSaveChecklistItem = { syncViewModel.saveChecklistItem(it) },
            onToggleChecklistItem = { id, checked -> syncViewModel.toggleChecklistItem(id, checked) },
            theme = activeTheme,
            language = currentLanguage
          )
          3 -> MedicalDatabaseScreen(
            theme = activeTheme,
            language = currentLanguage
          )
          4 -> SettingsScreen(
            userEmail = userEmail,
            syncSummary = syncSummary,
            momAvatar = profile.momAvatar,
            partnerAvatar = profile.partnerAvatar,
            babyAvatar = profile.babyAvatar,
            onAvatarsChanged = { mom, partner, baby ->
              syncViewModel.updatePreferences(
                momAvatar = mom,
                partnerAvatar = partner,
                babyAvatar = baby,
                languageCode = currentLanguage.name,
                themePaletteName = activePalette.name
              )
            },
            currentPalette = activePalette,
            onPaletteChanged = { newPalette ->
              ThemeManager.setPalette(newPalette)
              syncViewModel.updatePreferences(
                momAvatar = profile.momAvatar,
                partnerAvatar = profile.partnerAvatar,
                babyAvatar = profile.babyAvatar,
                languageCode = currentLanguage.name,
                themePaletteName = newPalette.name
              )
            },
            currentLanguage = currentLanguage,
            onLanguageChanged = { newLang ->
              onLanguageChange(newLang)
              syncViewModel.updatePreferences(
                momAvatar = profile.momAvatar,
                partnerAvatar = profile.partnerAvatar,
                babyAvatar = profile.babyAvatar,
                languageCode = newLang.name,
                themePaletteName = activePalette.name
              )
            },
            onInitiateSync = { syncViewModel.initiateSync() },
            onSignOut = {
              coroutineScope.launch {
                signOutWithGoogle(context)
              }
            },
            theme = activeTheme
          )
        }
      }
    }
  }

  if (showSyncStatusModal) {
    SyncStatusDashboardDialog(
      userEmail = userEmail,
      syncSummary = syncSummary,
      onInitiateSync = { syncViewModel.initiateSync() },
      onDismiss = { showSyncStatusModal = false },
      theme = activeTheme,
      language = currentLanguage
    )
  }
}

@Composable
private fun CloudSyncStatusBanner(
  userEmail: String,
  isSeeding: Boolean,
  syncError: String?,
  lastSyncedEpochMillis: Long?,
  language: AppLanguage,
  theme: ThemeColors,
  onRetry: () -> Unit,
  onOpenSyncDashboard: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp)
      .clickable { onOpenSyncDashboard() }
      .testTag("cloud_sync_status_banner"),
    shape = RoundedCornerShape(12.dp),
    color = when {
      syncError != null -> Color(0xFFFEF2F2)
      isSeeding -> theme.cardSurface
      else -> Color(0xFFF0FDF4)
    },
    border = BorderStroke(
      1.dp,
      when {
        syncError != null -> Color(0xFFFCA5A5)
        isSeeding -> theme.border
        else -> Color(0xFFDCFCE7)
      }
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
      ) {
        when {
          syncError != null -> Icon(
            Icons.Default.CloudOff,
            contentDescription = "Erreur de synchronisation",
            tint = Color(0xFFDC2626),
            modifier = Modifier.size(16.dp)
          )
          isSeeding -> CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            strokeWidth = 2.dp,
            color = theme.accent
          )
          else -> Icon(
            Icons.Default.CloudDone,
            contentDescription = "Synchronisé",
            tint = Color(0xFF16A34A),
            modifier = Modifier.size(16.dp)
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = when {
            syncError != null -> if (language == AppLanguage.FR) "Sync interrompue : $syncError" else "Sync issue: $syncError"
            isSeeding -> if (language == AppLanguage.FR) "Synchronisation Cloud ($userEmail)..." else "Cloud syncing ($userEmail)..."
            else -> if (language == AppLanguage.FR) {
              "Sync active • ${formatSyncShortTime(lastSyncedEpochMillis)} • $userEmail"
            } else {
              "Synced • ${formatSyncShortTime(lastSyncedEpochMillis)} • $userEmail"
            }
          },
          style = MaterialTheme.typography.labelSmall,
          color = when {
            syncError != null -> Color(0xFFB91C1C)
            isSeeding -> theme.primaryText
            else -> Color(0xFF15803D)
          },
          maxLines = 1
        )
      }
      if (syncError != null) {
        IconButton(
          onClick = onRetry,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            Icons.Default.Refresh,
            contentDescription = if (language == AppLanguage.FR) "Réessayer" else "Retry",
            tint = Color(0xFFDC2626),
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }
  }
}
