package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.AttachFile
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.core.content.ContextCompat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.i18n.AppLanguage
import com.example.ui.theme.ThemeColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PhotoGalleryGrid(
  viewModel: GalleryViewModel,
  theme: ThemeColors,
  language: AppLanguage,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val photos by viewModel.photos.collectAsState()
  var selectedCategory by remember { mutableStateOf("Tous") }
  var selectedPhoto by remember { mutableStateOf<GalleryPhotoItem?>(null) }
  var showAddDialog by remember { mutableStateOf(false) }

  var titleInput by remember { mutableStateOf("") }
  var categoryInput by remember { mutableStateOf("🤰 Ventre & Souvenirs") }

  val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
    if (bitmap != null) {
      val subtitle = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault()).format(Date())
      viewModel.addPhoto(
        context = context,
        title = titleInput.ifBlank { "Photo Caméra" },
        subtitle = subtitle,
        bitmap = bitmap,
        uri = null,
        category = categoryInput
      )
      showAddDialog = false
      Toast.makeText(context, "📷 Photo de caméra enregistrée !", Toast.LENGTH_SHORT).show()
    }
  }

  val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
    if (isGranted) {
      cameraLauncher.launch(null)
    } else {
      Toast.makeText(context, "Permission caméra refusée", Toast.LENGTH_SHORT).show()
    }
  }

  val galleryPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri != null) {
      val subtitle = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault()).format(Date())
      viewModel.addPhoto(
        context = context,
        title = titleInput.ifBlank { "Pièce jointe Galerie" },
        subtitle = subtitle,
        bitmap = null,
        uri = uri,
        category = categoryInput
      )
      showAddDialog = false
      Toast.makeText(context, "📁 Fichier/photo joint enregistré !", Toast.LENGTH_SHORT).show()
    }
  }

  val categories = listOf("Tous", "👶 Échographies", "💊 Ordonnances", "🤰 Ventre & Souvenirs")

  val filteredPhotos = if (selectedCategory == "Tous") {
    photos
  } else {
    photos.filter { it.category == selectedCategory }
  }

  Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
    // Header & Action Button (No camera/gallery attachments)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = if (language == AppLanguage.FR) "Galerie & Album Maman & Bébé" else "Mom & Baby Photo Gallery",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = theme.primaryText
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = if (language == AppLanguage.FR) "Chronologie visuelle et documents sécurisés" else "Visual timeline & secure documents",
          style = MaterialTheme.typography.bodySmall,
          color = theme.mutedText
        )
      }

      Button(
        onClick = { showAddDialog = true },
        colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = if (language == AppLanguage.FR) "Ajouter" else "Add", color = Color.White, fontSize = 13.sp)
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

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

    Spacer(modifier = Modifier.height(16.dp))

    // Photo Grid
    if (filteredPhotos.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(54.dp), tint = theme.mutedText.copy(alpha = 0.5f))
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = if (language == AppLanguage.FR) "Aucune photo dans cette catégorie" else "No photos in this category",
            style = MaterialTheme.typography.bodyMedium,
            color = theme.mutedText
          )
        }
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().weight(1f),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
      ) {
        items(filteredPhotos, key = { it.id }) { photo ->
          PhotoGridCard(photo = photo, theme = theme, onClick = { selectedPhoto = photo }, onDelete = { viewModel.deletePhoto(photo.id) })
        }
      }
    }
  }

  // Add Item Dialog (No camera dependency)
  if (showAddDialog) {
    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text(text = if (language == AppLanguage.FR) "Ajouter un souvenir, photo ou document" else "Add Memory, Photo or Document", color = theme.primaryText) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = titleInput,
            onValueChange = { titleInput = it },
            label = { Text(if (language == AppLanguage.FR) "Titre / Description" else "Title / Description") },
            placeholder = { Text("ex: Échographie T1 / Ordonnance Estrace") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = categoryInput,
            onValueChange = { categoryInput = it },
            label = { Text(if (language == AppLanguage.FR) "Catégorie" else "Category") },
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(4.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                  cameraLauncher.launch(null)
                } else {
                  cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
              },
              modifier = Modifier.weight(1f),
              colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
            ) {
              Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (language == AppLanguage.FR) "Caméra" else "Camera", fontSize = 12.sp, color = Color.White)
            }

            OutlinedButton(
              onClick = {
                galleryPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              modifier = Modifier.weight(1f),
              border = BorderStroke(1.dp, theme.accent)
            ) {
              Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = theme.accent)
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (language == AppLanguage.FR) "Galerie" else "Gallery", fontSize = 12.sp, color = theme.primaryText)
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (titleInput.isNotBlank()) {
              val subtitle = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault()).format(Date())
              viewModel.addPhoto(
                context = context,
                title = titleInput,
                subtitle = subtitle,
                bitmap = null,
                uri = null,
                category = categoryInput
              )
              showAddDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
        ) {
          Text(text = if (language == AppLanguage.FR) "Enregistrer" else "Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDialog = false }) {
          Text(text = if (language == AppLanguage.FR) "Annuler" else "Cancel", color = theme.mutedText)
        }
      }
    )
  }

  // Full Screen Photo Detail Dialog
  selectedPhoto?.let { photo ->
    AlertDialog(
      onDismissRequest = { selectedPhoto = null },
      title = { Text(text = photo.title, color = theme.primaryText) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(260.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(theme.background),
            contentAlignment = Alignment.Center
          ) {
            if (photo.filePath != null) {
              AsyncImage(
                model = File(photo.filePath),
                contentDescription = photo.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
              )
            } else {
              Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(64.dp), tint = theme.mutedText)
            }
          }
          Text(text = photo.subtitle, style = MaterialTheme.typography.bodySmall, color = theme.mutedText)
          Text(text = "Catégorie: ${photo.category}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = theme.accent)
        }
      },
      confirmButton = {
        TextButton(onClick = { selectedPhoto = null }) {
          Text(text = if (language == AppLanguage.FR) "Fermer" else "Close")
        }
      }
    )
  }
}

@Composable
private fun PhotoGridCard(
  photo: GalleryPhotoItem,
  theme: ThemeColors,
  onClick: () -> Unit,
  onDelete: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .height(190.dp)
      .clickable { onClick() },
    shape = RoundedCornerShape(16.dp),
    color = theme.cardSurface,
    border = BorderStroke(1.dp, theme.border)
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      Column(modifier = Modifier.fillMaxSize()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(theme.background),
          contentAlignment = Alignment.Center
        ) {
          if (photo.filePath != null) {
            AsyncImage(
              model = File(photo.filePath),
              contentDescription = photo.title,
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop
            )
          } else {
            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(40.dp), tint = theme.mutedText.copy(alpha = 0.6f))
          }
        }

        Column(modifier = Modifier.padding(10.dp)) {
          Text(
            text = photo.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = theme.primaryText,
            maxLines = 1
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = photo.subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = theme.mutedText,
            maxLines = 1
          )
        }
      }

      // Delete Button Top Right
      IconButton(
        onClick = onDelete,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(4.dp)
          .size(30.dp)
          .background(Color.Black.copy(alpha = 0.4f), CircleShape)
      ) {
        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(14.dp))
      }
    }
  }
}
