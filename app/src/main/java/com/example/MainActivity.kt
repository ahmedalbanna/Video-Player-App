package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.VideoListScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.VideoViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize()
        ) {
          val viewModel: VideoViewModel = viewModel()
          val currentVideo by viewModel.currentPlayingVideo.collectAsState()

          // Slide-in slide-out transitions for cinema-quality visual fidelity!
          AnimatedContent(
            targetState = currentVideo,
            transitionSpec = {
              if (targetState != null) {
                // Navigating to the player: slide from right to left
                slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
              } else {
                // Navigating back to the list: slide from left to right
                slideInHorizontally { width -> -width } togetherWith slideOutHorizontally { width -> width }
              }
            },
            label = "ScreenNavigation"
          ) { activeVideo ->
            if (activeVideo == null) {
              VideoListScreen(
                viewModel = viewModel,
                onVideoSelected = { video ->
                  viewModel.selectVideo(video)
                }
              )
            } else {
              VideoPlayerScreen(
                video = activeVideo,
                viewModel = viewModel,
                onNavigateBack = {
                  viewModel.selectVideo(null)
                }
              )
            }
          }
        }
      }
    }
  }
}
