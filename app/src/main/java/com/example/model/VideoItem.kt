package com.example.model

data class VideoItem(
  val id: Long,
  val title: String,
  val durationMs: Long,
  val sizeBytes: Long,
  val path: String,
  val uri: String,
  val thumbnailUri: String? = null,
  val dateAdded: Long = 0,
  val isDemo: Boolean = false
) {
  // Helper to get formatted duration like 05:24 or 01:24:10
  fun getFormattedDuration(): String {
    if (durationMs <= 0) return "00:00"
    val totalSeconds = durationMs / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600

    return if (hours > 0) {
      String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
      String.format("%02d:%02d", minutes, seconds)
    }
  }

  // Helper to format file size dynamically (e.g. 1.2 GB, 350 MB)
  fun getFormattedSize(): String {
    if (sizeBytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = sizeBytes.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.size - 1) {
      size /= 1024
      unitIndex++
    }
    return String.format("%.2f %s", size, units[unitIndex])
  }
}
