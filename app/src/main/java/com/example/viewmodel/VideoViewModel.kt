package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.VideoItem
import com.example.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class VideoSortType {
  DATE, SIZE, NAME
}

class VideoViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = VideoRepository(application.applicationContext)

  // System permissions state
  val permissionsGranted = MutableStateFlow(false)

  // Source videos: combination of local MediaStore files and beautiful fallbacks
  private val _localVideos = MutableStateFlow<List<VideoItem>>(emptyList())
  private val _isLoading = MutableStateFlow(false)

  // Search query state
  val searchQuery = MutableStateFlow("")

  // Sorting state
  val sortType = MutableStateFlow(VideoSortType.DATE)

  // Filter local & demo streams list
  val showDemoVideos = MutableStateFlow(true)

  // Combine local videos, search query, sorting type, and show demo videos flag
  val filteredVideos: StateFlow<List<VideoItem>> = combine(
    _localVideos,
    searchQuery,
    sortType,
    showDemoVideos
  ) { locals, query, sort, showDemo ->
    var resultList = mutableListOf<VideoItem>()
    resultList.addAll(locals)
    if (showDemo || locals.isEmpty()) {
      resultList.addAll(repository.demoVideos)
    }

    // Filter by query if present
    if (query.isNotBlank()) {
      resultList = resultList.filter {
        it.title.contains(query, ignoreCase = true) ||
          it.path.contains(query, ignoreCase = true)
      }.toMutableList()
    }

    // Apply sorting
    when (sort) {
      VideoSortType.DATE -> resultList.sortByDescending { it.dateAdded }
      VideoSortType.SIZE -> resultList.sortByDescending { it.sizeBytes }
      VideoSortType.NAME -> resultList.sortBy { it.title.lowercase() }
    }

    resultList.toList()
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val isLoading: StateFlow<Boolean> = _isLoading

  // Video currently playing
  private val _currentPlayingVideo = MutableStateFlow<VideoItem?>(null)
  val currentPlayingVideo: StateFlow<VideoItem?> = _currentPlayingVideo

  // Saved playback states for seamless rotation recovery
  var savedPlaybackPosition = MutableStateFlow(0L)
  var savedPlayWhenReady = MutableStateFlow(true)

  init {
    loadVideos()
  }

  fun setPermissionsGranted(granted: Boolean) {
    permissionsGranted.value = granted
    if (granted) {
      loadVideos()
    }
  }

  fun loadVideos() {
    viewModelScope.launch {
      _isLoading.value = true
      try {
        if (permissionsGranted.value) {
          val locals = repository.fetchLocalVideos()
          _localVideos.value = locals
        } else {
          _localVideos.value = emptyList()
        }
      } catch (e: Exception) {
        _localVideos.value = emptyList()
      } finally {
        _isLoading.value = false
      }
    }
  }

  fun selectVideo(video: VideoItem?) {
    if (_currentPlayingVideo.value?.uri != video?.uri) {
      _currentPlayingVideo.value = video
      savedPlaybackPosition.value = 0L // Reset position for different video
      savedPlayWhenReady.value = true
    }
  }

  fun updatePlaybackState(position: Long, playWhenReady: Boolean) {
    savedPlaybackPosition.value = position
    savedPlayWhenReady.value = playWhenReady
  }

  fun deleteVideo(video: VideoItem) {
    viewModelScope.launch {
      val success = repository.deleteVideo(video)
      if (success) {
        loadVideos()
      }
    }
  }
}
