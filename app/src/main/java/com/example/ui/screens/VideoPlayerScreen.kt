package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.model.VideoItem
import com.example.viewmodel.VideoViewModel
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerScreen(
  video: VideoItem,
  viewModel: VideoViewModel,
  onNavigateBack: () -> Unit
) {
  val context = LocalContext.current
  val activity = context as? Activity

  // Keep screen on continuously during movie time
  DisposableEffect(Unit) {
    activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    onDispose {
      activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      // Restore orientation safely on exit
      activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
  }

  // Handle back button specifically inside player
  BackHandler {
    viewModel.selectVideo(null)
    onNavigateBack()
  }

  // Create & manage the single lifecycle of ExoPlayer
  val exoPlayer = remember {
    ExoPlayer.Builder(context).build().apply {
      repeatMode = Player.REPEAT_MODE_OFF
    }
  }

  var isPlaying by remember { mutableStateOf(viewModel.savedPlayWhenReady.value) }
  var currentPosition by remember { mutableLongStateOf(viewModel.savedPlaybackPosition.value) }
  var duration by remember { mutableLongStateOf(video.durationMs) }
  var isMuted by remember { mutableStateOf(false) }
  var currentSpeed by remember { mutableFloatStateOf(1.0f) }
  var showControls by remember { mutableStateOf(true) }
  var isLandscape by remember { mutableStateOf(false) }

  // Set the source and dynamic states
  LaunchedEffect(video.uri) {
    val mediaItem = MediaItem.fromUri(video.uri)
    exoPlayer.setMediaItem(mediaItem)
    exoPlayer.seekTo(viewModel.savedPlaybackPosition.value)
    exoPlayer.playWhenReady = viewModel.savedPlayWhenReady.value
    exoPlayer.prepare()

    // Add Player listener to update loading parameters
    val listener = object : Player.Listener {
      override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY) {
          duration = exoPlayer.duration.coerceAtLeast(0L)
        }
      }

      override fun onIsPlayingChanged(isPlayingParam: Boolean) {
        isPlaying = isPlayingParam
      }
    }
    exoPlayer.addListener(listener)
  }

  // Save progress dynamically into viewmodel state so rotation remains unbroken
  LaunchedEffect(isPlaying, currentPosition) {
    viewModel.updatePlaybackState(currentPosition, isPlaying)
  }

  // Polling coroutine loop for high accuracy slider tracking (runs on main)
  LaunchedEffect(exoPlayer, isPlaying) {
    while (true) {
      if (exoPlayer.isPlaying) {
        currentPosition = exoPlayer.currentPosition
        duration = exoPlayer.duration.coerceAtLeast(0L)
      }
      delay(250)
    }
  }

  // Auto-hide controls timeout
  LaunchedEffect(showControls) {
    if (showControls) {
      delay(4000)
      showControls = false
    }
  }

  // Main immersive surface box
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
      .testTag("video_player_box")
  ) {
    // 1. Core ExoPlayer Android component
    AndroidView(
      factory = { ctx ->
        PlayerView(ctx).apply {
          player = exoPlayer
          useController = false // Hide native gray UI controls
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
          )
        }
      },
      modifier = Modifier
        .fillMaxSize()
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null
        ) {
          showControls = !showControls
        }
    )

    // 2. Clear invisible doubletap areas to fast rewind/forward
    Row(modifier = Modifier.fillMaxSize()) {
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .weight(1f)
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
          ) {
            // Triple click or double-tap left
            val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0L)
            exoPlayer.seekTo(target)
            currentPosition = target
            showControls = true
          }
      )
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .weight(1f)
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
          ) {
            // Double-tap right
            val target = (exoPlayer.currentPosition + 10000).coerceAtMost(duration)
            exoPlayer.seekTo(target)
            currentPosition = target
            showControls = true
          }
      )
    }

    // 3. Cinematic controls overlay
    AnimatedVisibility(
      visible = showControls,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.fillMaxSize()
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.5f))
      ) {

        // HEADER: Back Arrow & Active Name
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = {
              viewModel.selectVideo(null)
              onNavigateBack()
            },
            modifier = Modifier
              .size(48.dp)
              .testTag("player_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "الرجوع",
              tint = Color.White
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Text(
            text = video.title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )

          // Speed regulator indicator
          var speedMenuExpanded by remember { mutableStateOf(false) }
          Box {
            IconButton(
              onClick = { speedMenuExpanded = true },
              modifier = Modifier.size(48.dp)
            ) {
              Icon(
                imageVector = Icons.Default.SlowMotionVideo,
                contentDescription = "سرعة التشغيل",
                tint = Color.White
              )
            }

            DropdownMenu(
              expanded = speedMenuExpanded,
              onDismissRequest = { speedMenuExpanded = false },
              modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
              listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { rate ->
                DropdownMenuItem(
                  text = { Text("${rate}x", color = Color.White) },
                  onClick = {
                    currentSpeed = rate
                    exoPlayer.playbackParameters = PlaybackParameters(rate)
                    speedMenuExpanded = false
                  }
                )
              }
            }
          }
        }

        // CENTER: Large control row (Rewind, Play/Pause, Forward)
        Row(
          modifier = Modifier.align(Alignment.Center),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = {
              val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0L)
              exoPlayer.seekTo(target)
              currentPosition = target
            },
            modifier = Modifier
              .size(56.dp)
              .clip(CircleShape)
              .background(Color.Black.copy(alpha = 0.4f))
          ) {
            Icon(
              imageVector = Icons.Default.FastRewind,
              contentDescription = "تأخير 10 ثواني",
              tint = Color.White,
              modifier = Modifier.size(32.dp)
            )
          }

          Spacer(modifier = Modifier.width(28.dp))

          IconButton(
            onClick = {
              if (isPlaying) {
                exoPlayer.pause()
              } else {
                exoPlayer.play()
              }
              isPlaying = !isPlaying
            },
            modifier = Modifier
              .size(72.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary)
              .testTag("player_play_pause_button")
          ) {
            Icon(
              imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
              tint = Color.Black,
              modifier = Modifier.size(42.dp)
            )
          }

          Spacer(modifier = Modifier.width(28.dp))

          IconButton(
            onClick = {
              val target = (exoPlayer.currentPosition + 10000).coerceAtMost(duration)
              exoPlayer.seekTo(target)
              currentPosition = target
            },
            modifier = Modifier
              .size(56.dp)
              .clip(CircleShape)
              .background(Color.Black.copy(alpha = 0.4f))
          ) {
            Icon(
              imageVector = Icons.Default.FastForward,
              contentDescription = "تقديم 10 ثواني",
              tint = Color.White,
              modifier = Modifier.size(32.dp)
            )
          }
        }

        // FOOTER: SeekBar, Timing displays, Volume mute, fullscreen toggle
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
          // Time Labels Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Elapsed helper
            Text(
              text = formatDuration(currentPosition),
              color = Color.White,
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium
            )

            // Total / Limit Helper
            Text(
              text = formatDuration(duration),
              color = Color.White,
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium
            )
          }

          Spacer(modifier = Modifier.height(4.dp))

          // Material Cyan seek bar
          Slider(
            value = currentPosition.toFloat(),
            onValueChange = { newValue ->
              currentPosition = newValue.toLong()
              exoPlayer.seekTo(currentPosition)
            },
            valueRange = 0f..(duration.toFloat().coerceAtLeast(1f)),
            colors = SliderDefaults.colors(
              thumbColor = MaterialTheme.colorScheme.primary,
              activeTrackColor = MaterialTheme.colorScheme.primary,
              inactiveTrackColor = Color.White.copy(alpha = 0.24f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("player_seekbar")
          )

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Mute / Speaker Controls
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = {
                  isMuted = !isMuted
                  exoPlayer.volume = if (isMuted) 0f else 1f
                }
              ) {
                Icon(
                  imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                  contentDescription = "كتم الصوت",
                  tint = Color.White
                )
              }

              Text(
                text = if (isMuted) "مكتوم" else "صوت مرتفع",
                color = Color.White,
                fontSize = 12.sp
              )
            }

            // Speed & orientation info
            Row(verticalAlignment = Alignment.CenterVertically) {
              if (currentSpeed != 1.0f) {
                Surface(
                  color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                  shape = RoundedCornerShape(4.dp),
                  modifier = Modifier.padding(end = 8.dp)
                ) {
                  Text(
                    text = "سرعة ${currentSpeed}x",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 11.sp
                  )
                }
              }

              IconButton(
                onClick = {
                  isLandscape = !isLandscape
                  if (isLandscape) {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                  } else {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                  }
                },
                modifier = Modifier.testTag("toggle_fullscreen")
              ) {
                Icon(
                  imageVector = if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                  contentDescription = "ملء الشاشة تدوير",
                  tint = Color.White
                )
              }
            }
          }
        }
      }
    }
  }

  // Release resource of ExoPlayer exactly when leaving screen context
  DisposableEffect(Unit) {
    onDispose {
      exoPlayer.release()
    }
  }
}

// Global scope formatting helper
private fun formatDuration(millis: Long): String {
  if (millis <= 0) return "00:00"
  val totalSeconds = millis / 1000
  val seconds = totalSeconds % 60
  val minutes = (totalSeconds / 60) % 60
  val hours = totalSeconds / 3600

  return if (hours > 0) {
    String.format("%d:%02d:%02d", hours, minutes, seconds)
  } else {
    String.format("%02d:%02d", minutes, seconds)
  }
}
