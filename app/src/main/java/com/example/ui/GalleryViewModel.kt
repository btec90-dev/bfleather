package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

data class GalleryPhotoItem(
  val id: String,
  val title: String,
  val subtitle: String,
  val filePath: String?,
  val timestamp: Long,
  val category: String = "Souvenir • 2026"
)

class GalleryViewModel : ViewModel() {

  private val _photos = MutableStateFlow<List<GalleryPhotoItem>>(
    listOf(
      GalleryPhotoItem(
        id = "p_1",
        title = "Échographie T1 — Clarté nucale (1.2 mm)",
        subtitle = "05 Nov 2026 • Dr. Brauer RMA",
        filePath = null,
        timestamp = System.currentTimeMillis() - 86400000L * 2,
        category = "👶 Échographies"
      ),
      GalleryPhotoItem(
        id = "p_2",
        title = "Ordonnance Estrace 2 mg PO (AM & PM)",
        subtitle = "06 Nov 2026 • RMA New York",
        filePath = null,
        timestamp = System.currentTimeMillis() - 86400000L,
        category = "💊 Ordonnances"
      ),
      GalleryPhotoItem(
        id = "p_3",
        title = "Photo Ventre Semaine 16",
        subtitle = "06 Nov 2026 • Suivi Maman",
        filePath = null,
        timestamp = System.currentTimeMillis(),
        category = "🤰 Ventre & Souvenirs"
      )
    )
  )
  val photos: StateFlow<List<GalleryPhotoItem>> = _photos.asStateFlow()

  fun addPhoto(context: Context, title: String, subtitle: String, bitmap: Bitmap?, uri: Uri?, category: String) {
    var savedPath: String? = null
    try {
      val filename = "photo_${System.currentTimeMillis()}.jpg"
      val file = File(context.filesDir, filename)
      val fos = FileOutputStream(file)
      if (bitmap != null) {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
        savedPath = file.absolutePath
      } else if (uri != null) {
        val inputStream = context.contentResolver.openInputStream(uri)
        inputStream?.use { input ->
          fos.use { output ->
            input.copyTo(output)
          }
        }
        savedPath = file.absolutePath
      }
      fos.close()
    } catch (_: Exception) {}

    val newItem = GalleryPhotoItem(
      id = "p_${System.currentTimeMillis()}",
      title = title,
      subtitle = subtitle,
      filePath = savedPath,
      timestamp = System.currentTimeMillis(),
      category = category
    )

    _photos.value = listOf(newItem) + _photos.value
  }

  fun deletePhoto(id: String) {
    _photos.value = _photos.value.filter { it.id != id }
  }
}
