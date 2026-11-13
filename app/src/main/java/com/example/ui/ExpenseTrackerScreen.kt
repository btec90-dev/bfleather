package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.AppLanguage
import com.example.ui.theme.ThemeColors
import java.text.SimpleDateFormat
import java.util.*

data class ExpenseItem(
  val id: String,
  val title: String,
  val amount: Double,
  val category: String, // "Transport", "Médicaments", "Affaires Bébé", "Affaires Maman"
  val phase: String, // "Grossesse" or "Bébé"
  val dateStr: String,
  val receiptPath: String? = null
)

@Composable
fun ExpenseTrackerScreen(
  theme: ThemeColors,
  language: AppLanguage
) {
  val context = LocalContext.current
  var expenses by remember {
    mutableStateOf(
      listOf(
        ExpenseItem("e1", "Consultation Dr. Laurent", 120.0, "Transport & Consultations", "Grossesse", "06 nov. 2026"),
        ExpenseItem("e2", "Estrace 2 mg & Progestérone", 48.50, "Médicaments", "Grossesse", "05 nov. 2026"),
        ExpenseItem("e3", "Poussette & Nacelle", 580.0, "Affaires Bébé", "Bébé", "01 nov. 2026"),
        ExpenseItem("e4", "Vêtements de maternité", 95.0, "Affaires Maman", "Grossesse", "28 oct. 2026")
      )
    )
  }

  var selectedFilter by remember { mutableStateOf("Tous") }
  var showAddExpenseDialog by remember { mutableStateOf(false) }

  var titleInput by remember { mutableStateOf("") }
  var amountInput by remember { mutableStateOf("") }
  var categoryInput by remember { mutableStateOf("Médicaments") }
  var phaseInput by remember { mutableStateOf("Grossesse") }

  val categories = listOf("Tous", "Médicaments", "Transport & Consultations", "Affaires Bébé", "Affaires Maman")

  val totalSpent = expenses.sumOf { it.amount }

  val filteredExpenses = expenses.filter {
    selectedFilter == "Tous" || it.category == selectedFilter
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(theme.background)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Header & Add Button
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = if (language == AppLanguage.FR) "Dépenses Grossesse & Bébé" else "Pregnancy & Baby Expenses",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = theme.primaryText
        )
        Text(
          text = if (language == AppLanguage.FR) "Suivi budgétaire complet" else "Comprehensive budget tracker",
          style = MaterialTheme.typography.bodySmall,
          color = theme.mutedText
        )
      }

      Button(
        onClick = { showAddExpenseDialog = true },
        colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = if (language == AppLanguage.FR) "Ajouter" else "Add", color = Color.White, fontSize = 13.sp)
      }
    }

    // Summary Card
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(18.dp),
      color = theme.cardSurface,
      border = BorderStroke(1.dp, theme.border)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = if (language == AppLanguage.FR) "Total Dépensé" else "Total Spent",
          style = MaterialTheme.typography.labelSmall,
          color = theme.mutedText
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "%.2f €".format(totalSpent),
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = theme.accent
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
          progress = { (totalSpent / 2000.0).toFloat().coerceIn(0f, 1f) },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
          color = theme.accent,
          trackColor = theme.border
        )
      }
    }

    // Category Filter Chips
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      categories.forEach { cat ->
        val isSelected = selectedFilter == cat
        Surface(
          onClick = { selectedFilter = cat },
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

    // Expenses List
    if (filteredExpenses.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(54.dp), tint = theme.mutedText.copy(alpha = 0.5f))
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = if (language == AppLanguage.FR) "Aucune dépense enregistrée" else "No expenses recorded yet",
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
        items(filteredExpenses, key = { it.id }) { exp ->
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = theme.cardSurface,
            border = BorderStroke(1.dp, theme.border)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                  shape = CircleShape,
                  color = theme.accent.copy(alpha = 0.15f),
                  modifier = Modifier.size(40.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      when (exp.category) {
                        "Médicaments" -> Icons.Default.MedicalServices
                        "Transport & Consultations" -> Icons.Default.LocalTaxi
                        "Affaires Bébé" -> Icons.Default.ChildCare
                        else -> Icons.Default.ShoppingBag
                      },
                      contentDescription = null,
                      tint = theme.accent,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(exp.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = theme.primaryText)
                  Text("${exp.category} • ${exp.dateStr}", style = MaterialTheme.typography.labelSmall, color = theme.mutedText)
                }
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "%.2f €".format(exp.amount),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = theme.accent
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                  onClick = {
                    expenses = expenses.filter { it.id != exp.id }
                    Toast.makeText(context, "Dépense supprimée", Toast.LENGTH_SHORT).show()
                  },
                  modifier = Modifier.size(28.dp)
                ) {
                  Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }
      }
    }
  }

  // Add Expense Dialog
  if (showAddExpenseDialog) {
    AlertDialog(
      onDismissRequest = { showAddExpenseDialog = false },
      title = { Text(if (language == AppLanguage.FR) "Ajouter une dépense" else "Add Expense", color = theme.primaryText) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = titleInput,
            onValueChange = { titleInput = it },
            label = { Text("Description / Article") },
            placeholder = { Text("ex. Vitamines prénatales / Échographie") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = amountInput,
            onValueChange = { amountInput = it },
            label = { Text("Montant (€)") },
            placeholder = { Text("ex. 45.00") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = categoryInput,
            onValueChange = { categoryInput = it },
            label = { Text("Catégorie") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val amount = amountInput.toDoubleOrNull() ?: 0.0
            if (titleInput.isNotBlank() && amount > 0) {
              val newExp = ExpenseItem(
                id = "e_${System.currentTimeMillis()}",
                title = titleInput,
                amount = amount,
                category = categoryInput,
                phase = phaseInput,
                dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
              )
              expenses = listOf(newExp) + expenses
              showAddExpenseDialog = false
              titleInput = ""
              amountInput = ""
              Toast.makeText(context, "Dépense enregistrée !", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
        ) {
          Text("Enregistrer")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddExpenseDialog = false }) {
          Text("Annuler", color = theme.mutedText)
        }
      }
    )
  }
}
