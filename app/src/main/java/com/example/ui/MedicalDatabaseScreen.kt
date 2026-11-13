package com.example.ui

import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.AppLanguage
import com.example.ui.theme.ThemeColors
import java.util.*

data class MedicalKnowledgeItem(
  val id: String,
  val titleFr: String,
  val titleEn: String,
  val categoryFr: String,
  val categoryEn: String,
  val contentFr: String,
  val contentEn: String,
  val icon: String
)

@Composable
fun MedicalDatabaseScreen(
  theme: ThemeColors,
  language: AppLanguage
) {
  val context = LocalContext.current
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("Tous") }

  val knowledgeBase = remember {
    listOf(
      MedicalKnowledgeItem(
        id = "m_1",
        titleFr = "Estrace (Étradiol 2 mg)",
        titleEn = "Estrace (Estradiol 2 mg)",
        categoryFr = "Médicaments",
        categoryEn = "Medications",
        contentFr = "Indication : Soutien endométrial en protocole FIV/TEC. Posologie usuelle : 1 comprimé matin et soir (AM & PM) par voie orale ou selon prescription stricte du gynécologue. Ne jamais interrompre sans avis médical.",
        contentEn = "Indication: Endometrial preparation for IVF/FET cycles. Usual dose: 1 tablet morning and evening (AM & PM) orally or per strict OB/GYN instructions. Do not stop without clinical guidance.",
        icon = "💊"
      ),
      MedicalKnowledgeItem(
        id = "m_2",
        titleFr = "Progestérone (PIO / Ovules)",
        titleEn = "Progesterone (PIO / Suppositories)",
        categoryFr = "Médicaments",
        categoryEn = "Medications",
        contentFr = "Indication : Maintien de la phase lutéale et prévention des fausses couches précoces. Voie intramusculaire (PIO) ou vaginale selon protocole de la clinique. Conserver à température ambiante.",
        contentEn = "Indication: Luteal phase support and early pregnancy maintenance. Intramuscular (PIO) or vaginal route per clinic protocol. Store at room temperature.",
        icon = "💉"
      ),
      MedicalKnowledgeItem(
        id = "m_3",
        titleFr = "Acide Folique & Vitamines Prénatales",
        titleEn = "Folic Acid & Prenatal Vitamins",
        categoryFr = "Nutrition",
        categoryEn = "Nutrition",
        contentFr = "Indication : Prévention des anomalies du tube neural (spina bifida). Recommandation 400 mcg à 1 mg par jour dès la pré-conception et tout au long du premier trimestre.",
        contentEn = "Indication: Prevention of neural tube defects. Recommended 400 mcg to 1 mg daily starting pre-conception and throughout the first trimester.",
        icon = "🥗"
      ),
      MedicalKnowledgeItem(
        id = "m_4",
        titleFr = "Échographie T1 (11–13 SA)",
        titleEn = "First Trimester Scan (11–13 Wks)",
        categoryFr = "Échographies",
        categoryEn = "Ultrasounds",
        contentFr = "Objectif : Datation précise de la grossesse, mesure de la clarté nucale (dépistage combiné de la trisomie 21) et observation morphologique précoce.",
        contentEn = "Objective: Precise gestational dating, nuchal translucency measurement (Down syndrome screening), and early structural survey.",
        icon = "👶"
      ),
      MedicalKnowledgeItem(
        id = "m_5",
        titleFr = "Signes d'Alerte & Urgences Obstétricales",
        titleEn = "Warning Signs & Emergency Guidance",
        categoryFr = "Urgences",
        categoryEn = "Emergencies",
        contentFr = "Consulter immédiatement ou appeler le 15 / 911 en cas de : saignements vaginaux abondants, douleurs pelviennes intenses unilatérales, fièvre > 38.5°C, maux de tête violents avec troubles visuels.",
        contentEn = "Seek immediate care or call 911 / emergency services if experiencing: heavy vaginal bleeding, severe unilateral pelvic pain, fever > 38.5°C, severe headaches with visual changes.",
        icon = "🚨"
      ),
      MedicalKnowledgeItem(
        id = "m_6",
        titleFr = "Hydratation & Besoins en Eau",
        titleEn = "Hydration & Water Requirements",
        categoryFr = "Nutrition",
        categoryEn = "Nutrition",
        contentFr = "Recommandation : 1.5 à 2.5 litres d'eau par jour pour soutenir l'augmentation du volume sanguin maternel et la production de liquide amniotique.",
        contentEn = "Recommendation: 1.5 to 2.5 liters of water daily to support expanded maternal blood volume and amniotic fluid maintenance.",
        icon = "💧"
      )
    )
  }

  val speechRecognizerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == android.app.Activity.RESULT_OK) {
      val data = result.data
      val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
      if (!results.isNullOrEmpty()) {
        searchQuery = results[0]
        Toast.makeText(context, "Recherche vocale : ${searchQuery}", Toast.LENGTH_SHORT).show()
      }
    }
  }

  val categories = if (language == AppLanguage.FR) {
    listOf("Tous", "Médicaments", "Nutrition", "Échographies", "Urgences")
  } else {
    listOf("All", "Medications", "Nutrition", "Ultrasounds", "Emergencies")
  }

  val filteredItems = knowledgeBase.filter { item ->
    val matchesCategory = selectedCategory == "Tous" || selectedCategory == "All" ||
      item.categoryFr.equals(selectedCategory, true) || item.categoryEn.equals(selectedCategory, true)
    val matchesSearch = searchQuery.isBlank() ||
      item.titleFr.contains(searchQuery, true) ||
      item.titleEn.contains(searchQuery, true) ||
      item.contentFr.contains(searchQuery, true) ||
      item.contentEn.contains(searchQuery, true)
    matchesCategory && matchesSearch
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(theme.background)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = if (language == AppLanguage.FR) "Base Médicale & Santé" else "Medical & Health Database",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = theme.primaryText
        )
        Text(
          text = if (language == AppLanguage.FR) "Référentiel certifié (ACOG / OMS)" else "Certified Reference (ACOG / WHO)",
          style = MaterialTheme.typography.bodySmall,
          color = theme.mutedText
        )
      }
    }

    // Search bar with Voice Input (Microphone)
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      color = theme.cardSurface,
      border = BorderStroke(1.dp, theme.border)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = theme.mutedText)
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = {
            Text(
              if (language == AppLanguage.FR) "Rechercher médicament, symptôme..." else "Search medication, symptom...",
              style = MaterialTheme.typography.bodyMedium
            )
          },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent
          ),
          modifier = Modifier.weight(1f)
        )
        if (searchQuery.isNotBlank()) {
          IconButton(onClick = { searchQuery = "" }) {
            Icon(Icons.Default.Clear, contentDescription = "Effacer", tint = theme.mutedText, modifier = Modifier.size(18.dp))
          }
        }
        IconButton(
          onClick = {
            try {
              val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (language == AppLanguage.FR) "fr-FR" else "en-US")
                putExtra(RecognizerIntent.EXTRA_PROMPT, if (language == AppLanguage.FR) "Parlez pour rechercher..." else "Speak to search...")
              }
              speechRecognizerLauncher.launch(intent)
            } catch (_: Exception) {
              Toast.makeText(context, "Recherche vocale non disponible", Toast.LENGTH_SHORT).show()
            }
          },
          modifier = Modifier
            .size(36.dp)
            .background(theme.accent, CircleShape)
        ) {
          Icon(Icons.Default.Mic, contentDescription = "Recherche vocale", tint = Color.White, modifier = Modifier.size(18.dp))
        }
      }
    }

    // Category Filter Chips
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      categories.forEach { cat ->
        val isSelected = selectedCategory == cat
        Surface(
          onClick = { selectedCategory = cat },
          shape = RoundedCornerShape(20.dp),
          color = if (isSelected) theme.accent else theme.cardSurface,
          border = BorderStroke(1.dp, if (isSelected) theme.accent else theme.border),
          modifier = Modifier.height(34.dp)
        ) {
          Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(
              text = cat,
              style = MaterialTheme.typography.labelMedium,
              color = if (isSelected) Color.White else theme.primaryText,
              fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
          }
        }
      }
    }

    // Results List
    if (filteredItems.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.LocalHospital, contentDescription = null, modifier = Modifier.size(54.dp), tint = theme.mutedText.copy(alpha = 0.5f))
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = if (language == AppLanguage.FR) "Aucun résultat trouvé" else "No matching medical records found",
            style = MaterialTheme.typography.bodyMedium,
            color = theme.mutedText
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
      ) {
        items(filteredItems, key = { it.id }) { item ->
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = theme.cardSurface,
            border = BorderStroke(1.dp, theme.border)
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(item.icon, fontSize = 22.sp)
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text(
                      text = if (language == AppLanguage.FR) item.titleFr else item.titleEn,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      color = theme.primaryText
                    )
                    Text(
                      text = if (language == AppLanguage.FR) item.categoryFr else item.categoryEn,
                      style = MaterialTheme.typography.labelSmall,
                      color = theme.accent
                    )
                  }
                }
              }
              Spacer(modifier = Modifier.height(8.dp))
              SelectionContainer {
                Text(
                  text = if (language == AppLanguage.FR) item.contentFr else item.contentEn,
                  style = MaterialTheme.typography.bodySmall,
                  color = theme.primaryText.copy(alpha = 0.85f),
                  lineHeight = 18.sp
                )
              }
            }
          }
        }
      }
    }
  }
}
