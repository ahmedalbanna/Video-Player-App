package com.example.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.example.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoRepository(private val context: Context) {

  // Standard Creative Commons video streams for testing and demonstration
  val demoVideos = listOf(
    VideoItem(
      id = -1,
      title = "فيديو تجريبي: Big Buck Bunny",
      durationMs = 596000, // 9:56
      sizeBytes = 158 * 1024 * 1024,
      path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
      uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
      thumbnailUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg",
      dateAdded = System.currentTimeMillis() - 10000,
      isDemo = true
    ),
    VideoItem(
      id = -2,
      title = "فيديو تجريبي: Sintel (فيلم رسوم متحركة)",
      durationMs = 528000, // 8:48
      sizeBytes = 120 * 1024 * 1024,
      path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
      uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
      thumbnailUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/Sintel.jpg",
      dateAdded = System.currentTimeMillis() - 20000,
      isDemo = true
    ),
    VideoItem(
      id = -3,
      title = "فيديو تجريبي: Tears of Steel (خيال علمي)",
      durationMs = 734000, // 12:14
      sizeBytes = 145 * 1024 * 1024,
      path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
      uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
      thumbnailUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg",
      dateAdded = System.currentTimeMillis() - 30000,
      isDemo = true
    ),
    VideoItem(
      id = -4,
      title = "فيديو تجريبي: Elephant's Dream",
      durationMs = 653000, // 10:53
      sizeBytes = 190 * 1024 * 1024,
      path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
      uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
      thumbnailUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ElephantsDream.jpg",
      dateAdded = System.currentTimeMillis() - 40000,
      isDemo = true
    )
  )

  /**
   * Scans local MediaStore cache for videos. Runs on IO Dispatcher for extreme smoothness.
   */
  suspend fun fetchLocalVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
    val videosList = mutableListOf<VideoItem>()
    val contentResolver: ContentResolver = context.contentResolver

    val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

    val projection = arrayOf(
      MediaStore.Video.Media._ID,
      MediaStore.Video.Media.DISPLAY_NAME,
      MediaStore.Video.Media.DURATION,
      MediaStore.Video.Media.SIZE,
      MediaStore.Video.Media.DATA,
      MediaStore.Video.Media.DATE_ADDED
    )

    // Sort by added date in descending order
    val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

    try {
      contentResolver.query(
        collection,
        projection,
        null,
        null,
        sortOrder
      )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
        val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

        while (cursor.moveToNext()) {
          val id = cursor.getLong(idColumn)
          val name = cursor.getString(nameColumn) ?: "فيديو غير معروف"
          val duration = cursor.getLong(durationColumn)
          val size = cursor.getLong(sizeColumn)
          val path = cursor.getString(pathColumn) ?: ""
          val dateAdded = cursor.getLong(dateAddedColumn)

          // Generate Content Uri for this video
          val contentUri = ContentUris.withAppendedId(collection, id).toString()

          // Thumbnail loaded via contentResolver.loadThumbnail on request or Uri standard representation
          val thumbnailSavedUri = contentUri

          videosList.add(
            VideoItem(
              id = id,
              title = name,
              durationMs = duration,
              sizeBytes = size,
              path = path,
              uri = contentUri,
              thumbnailUri = thumbnailSavedUri,
              dateAdded = dateAdded,
              isDemo = false
            )
          )
        }
      }
    } catch (e: Exception) {
      Log.e("VideoRepository", "Error reading local videos", e)
    }

    return@withContext videosList
  }

  /**
   * Delete a local video from MediaStore. On newer Android versions, this could throw a RecoverableSecurityException
   * which may require permission prompt. We do a general delete check here.
   */
  suspend fun deleteVideo(video: VideoItem): Boolean = withContext(Dispatchers.IO) {
    if (video.isDemo) return@withContext false // Cannot delete sample video
    try {
      val contentUri = UriParser.parse(video.uri)
      val rowsDeleted = context.contentResolver.delete(contentUri, null, null)
      return@withContext rowsDeleted > 0
    } catch (e: Exception) {
      Log.e("VideoRepository", "Error delete file: ${video.title}", e)
      return@withContext false
    }
  }
}

// Simple internal helper to safely handle Uri parsing
object UriParser {
  fun parse(uriString: String): android.net.Uri {
    return android.net.Uri.parse(uriString)
  }
}
