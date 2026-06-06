package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.example.model.VideoItem
import com.example.viewmodel.VideoSortType
import com.example.viewmodel.VideoViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VideoListScreen(
  viewModel: VideoViewModel,
  onVideoSelected: (VideoItem) -> Unit
) {
  val videos by viewModel.filteredVideos.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val permissionGrantedState by viewModel.permissionsGranted.collectAsState()
  val query by viewModel.searchQuery.collectAsState()
  val activeSortType by viewModel.sortType.collectAsState()
  val showDemos by viewModel.showDemoVideos.collectAsState()

  // Standard Media permission selector
  val storagePermissionState = rememberPermissionState(
    permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      Manifest.permission.READ_MEDIA_VIDEO
    } else {
      Manifest.permission.READ_EXTERNAL_STORAGE
    }
  )

  LaunchedEffect(storagePermissionState.status) {
    viewModel.setPermissionsGranted(storagePermissionState.status.isGranted)
  }

  var isSearchExpanded by remember { mutableStateOf(false) }
  var sortMenuExpanded by remember { mutableStateOf(false) }
  var detailDialogVideo by remember { mutableStateOf<VideoItem?>(null) }

  Scaffold(
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF0F0F0F)) // Immersive dark Background color
          .statusBarsPadding()
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          if (!isSearchExpanded) {
            Column {
              Text(
                text = "فيديوهاتي",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("app_title_text")
              )
              Text(
                text = "${videos.size} فيديو متاح",
                color = Color(0xFF94A3B8), // slate-400
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp)
              )
            }

            Row {
              // Search button
              IconButton(
                onClick = { isSearchExpanded = true },
                modifier = Modifier.testTag("search_trigger_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Search,
                  contentDescription = "بحث",
                  tint = Color.White
                )
              }

              // Sort menu indicator
              Box {
                IconButton(
                  onClick = { sortMenuExpanded = true },
                  modifier = Modifier.testTag("sort_menu_button")
                ) {
                  Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = "ترتيب",
                    tint = Color.White
                  )
                }

                DropdownMenu(
                  expanded = sortMenuExpanded,
                  onDismissRequest = { sortMenuExpanded = false },
                  modifier = Modifier.background(Color(0xFF1E1E1E))
                ) {
                  DropdownMenuItem(
                    text = { Text("ترتيب حسب التاريخ", color = Color.White) },
                    onClick = {
                      viewModel.sortType.value = VideoSortType.DATE
                      sortMenuExpanded = false
                    }
                  )
                  DropdownMenuItem(
                    text = { Text("ترتيب حسب الحجم", color = Color.White) },
                    onClick = {
                      viewModel.sortType.value = VideoSortType.SIZE
                      sortMenuExpanded = false
                    }
                  )
                  DropdownMenuItem(
                    text = { Text("ترتيب حسب الاسم", color = Color.White) },
                    onClick = {
                      viewModel.sortType.value = VideoSortType.NAME
                      sortMenuExpanded = false
                    }
                  )
                }
              }

              // Force Refresh
              IconButton(
                onClick = { viewModel.loadVideos() },
                modifier = Modifier.testTag("refresh_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Refresh,
                  contentDescription = "تحديث",
                  tint = Color.White
                )
              }
            }
          } else {
            // Expand Search Field with custom themed outline
            OutlinedTextField(
              value = query,
              onValueChange = { viewModel.searchQuery.value = it },
              placeholder = { Text("ابحث عن فيديو...", color = Color.Gray) },
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.DarkGray,
                cursorColor = MaterialTheme.colorScheme.primary
              ),
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Search,
                  contentDescription = null,
                  tint = Color.Gray
                )
              },
              trailingIcon = {
                IconButton(
                  onClick = {
                    viewModel.searchQuery.value = ""
                    isSearchExpanded = false
                  }
                ) {
                  Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "إغلاق البحث",
                    tint = Color.Gray
                  )
                }
              },
              modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("search_text_input")
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Toggle Live Demo Streamings
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "إظهار الفديوهات التجريبية (بث مباشر)",
            color = Color(0xFF94A3B8),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
          )

          Switch(
            checked = showDemos,
            onCheckedChange = { viewModel.showDemoVideos.value = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = MaterialTheme.colorScheme.primary,
              checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
              uncheckedThumbColor = Color.LightGray,
              uncheckedTrackColor = Color.DarkGray
            ),
            modifier = Modifier.testTag("demo_streams_switch")
          )
        }
      }
    },
    bottomBar = {
      // Sleek Navigation Bar matching design HTML
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF161616))
          .padding(bottom = 24.dp, top = 12.dp)
          .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Tab 1: Fedyohaty (Active)
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.clickable { }
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(16.dp))
              .background(Color(0xFF8B5CF6).copy(alpha = 0.2f)) // bg-violet-500/20 in html
              .padding(horizontal = 20.dp, vertical = 6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Movie,
              contentDescription = null,
              tint = Color(0xFF8B5CF6),
              modifier = Modifier.size(24.dp)
            )
          }
          Text(
            text = "الفيديوهات",
            color = Color(0xFF8B5CF6),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }

        // Tab 2: Playlists (Mock)
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.clickable { }
        ) {
          Icon(
            imageVector = Icons.Default.Sort,
            contentDescription = null,
            tint = Color(0xFF64748B),
            modifier = Modifier.size(24.dp)
          )
          Text(
            text = "القوائم",
            color = Color(0xFF64748B),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }

        // Tab 3: Settings (Mock)
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.clickable { }
        ) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = Color(0xFF64748B),
            modifier = Modifier.size(24.dp)
          )
          Text(
            text = "الإعدادات",
            color = Color(0xFF64748B),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    },
    modifier = Modifier.fillMaxSize()
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF0F0F0F))
        .padding(innerPadding)
    ) {
      if (!permissionGrantedState) {
        // PERMISSION NOT RESOLVED
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .align(Alignment.Center)
            .testTag("permission_dialog_box"),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = Icons.Default.Movie,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
          )

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = "مطلوب إذن الوصول للملفات",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = "التطبيق يحتاج إلى صلاحية الوصول للفيديوهات والملفات المخزنة في ذكرة الهاتف حتى يتمكن من استعراض واجهات مشغل الموفرة لك.",
            color = Color.LightGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
          )

          Spacer(modifier = Modifier.height(24.dp))

          Button(
            onClick = { storagePermissionState.launchPermissionRequest() },
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("grant_permission_button")
          ) {
            Text(
              text = "منح الصلاحية الآن",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )
          }

          if (storagePermissionState.status.shouldShowRationale) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "لقد قمت برفض هذا الإذن مسبقاً، يرجى الانتقال إلى إعدادات الهاتف لتفعيل الحصول على المعاينة.",
              color = MaterialTheme.colorScheme.error,
              fontSize = 12.sp,
              textAlign = TextAlign.Center
            )
          }
        }
      } else {
        // PERMISSION SUCCESS: LOAD LAZY STREAM
        if (isLoading) {
          CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Center)
          )
        } else if (videos.isEmpty()) {
          Column(
            modifier = Modifier
              .align(Alignment.Center)
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Default.Movie,
              contentDescription = null,
              tint = Color.Gray,
              modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "لا توجد فيديوهات مطابقة",
              color = Color.White,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "تأكد من وجود ملفات فيديو على جهازك أو قم بتفعيل خيار الفيديوهات التجريبية في الأعلى للتجربة الفورية.",
              color = Color.Gray,
              fontSize = 13.sp,
              textAlign = TextAlign.Center,
              lineHeight = 20.sp
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            // RENDERING STUNNING FEATURED SHOWCASE AT THE TOP
            item {
              val firstVideo = videos.firstOrNull()
              if (firstVideo != null) {
                FeaturedVideoBlock(
                  video = firstVideo,
                  onPlay = { onVideoSelected(firstVideo) }
                )
              }
            }

            // SECTION HEADER
            item {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "جميع الفيديوهات",
                  color = Color(0xFFCBD5E1), // slate-300
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                )
                Text(
                  text = "عرض الكل",
                  color = Color(0xFF8B5CF6), // violet-400
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            // ALL OTHER VIDEOS
            items(videos, key = { it.uri }) { video ->
              VideoRowItem(
                video = video,
                onClicked = { onVideoSelected(video) },
                onInfoRequested = { detailDialogVideo = video },
                onDeleteRequested = { viewModel.deleteVideo(video) }
              )
            }
          }
        }
      }
    }
  }

  // Info Dialog Picker
  detailDialogVideo?.let { targetVideo ->
    AlertDialog(
      onDismissRequest = { detailDialogVideo = null },
      title = {
        Text(
          text = "تفاصيل الفيديو",
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "الاسم: ${targetVideo.title}",
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            fontSize = 14.sp
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "المسار: ${targetVideo.path}",
            color = Color.LightGray,
            fontSize = 13.sp
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "المدة: ${targetVideo.getFormattedDuration()}",
            color = Color.LightGray,
            fontSize = 13.sp
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "الحجم: ${targetVideo.getFormattedSize()}",
            color = Color.LightGray,
            fontSize = 13.sp
          )
          Spacer(modifier = Modifier.height(8.dp))
          val addedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(targetVideo.dateAdded * 1000))
          Text(
            text = "تاريخ الإضافة: $addedDate",
            color = Color.LightGray,
            fontSize = 13.sp
          )
          if (targetVideo.isDemo) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "نوع الفيديو: ملف بث سحابي عبر الإنترنت",
              color = MaterialTheme.colorScheme.primary,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { detailDialogVideo = null }) {
          Text("موافق", color = MaterialTheme.colorScheme.primary)
        }
      },
      containerColor = Color(0xFF1E1E1E)
    )
  }
}

/**
 * Beautiful featured video component with dual layers, neon colored shadow lines and backdrop play triggers.
 */
@Composable
fun FeaturedVideoBlock(
  video: VideoItem,
  onPlay: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    // Elegant background shadow glow overlay mimicking -inset-1 blur effect inside HTML
    Box(
      modifier = Modifier
        .matchParentSize()
        .padding(1.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(
          Brush.linearGradient(
            colors = listOf(
              Color(0xFF7C3AED), // violet-600
              Color(0xFF4F46E5)  // indigo-600
            )
          ),
          alpha = 0.3f
        )
    )

    // Main Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clickable { onPlay() },
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(
        containerColor = Color(0xFF1A1A1A)
      ),
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        // Thumbnail AspectRatio Video block
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color(0xFF1E293B))
        ) {
          if (!video.thumbnailUri.isNullOrEmpty()) {
            AsyncImage(
              model = video.thumbnailUri,
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
              alpha = 0.7f // subtle low opacity look matching HTML template "opacity-60"
            )
          }

          // Bottom timing pill overlay
          Box(
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(12.dp)
              .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = video.getFormattedDuration(),
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }

          // Blurry backdrop play button ring
          Box(
            modifier = Modifier
              .size(56.dp)
              .align(Alignment.Center)
              .clip(CircleShape)
              .background(Color.White.copy(alpha = 0.2f))
              .clickable { onPlay() },
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = "تشغيل",
              tint = Color.White,
              modifier = Modifier.size(36.dp)
            )
          }
        }

        // Card Labels
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Text(
            text = video.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "الاستمرار في المشاهدة • 00:00",
            color = Color(0xFF8B5CF6), // violet-500
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp
          )

          Spacer(modifier = Modifier.height(10.dp))

          // 2/3 filled progress tracking bar
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(4.dp)
              .clip(CircleShape)
              .background(Color.White.copy(alpha = 0.1f))
          ) {
            Box(
              modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.66f) // exactly 2/3 progress
                .background(Color(0xFF8B5CF6))
            )
          }
        }
      }
    }
  }
}

/**
 * Standard feed row cards styled exactly to the design layout specs.
 */
@Composable
fun VideoRowItem(
  video: VideoItem,
  onClicked: () -> Unit,
  onInfoRequested: () -> Unit,
  onDeleteRequested: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
      .clickable { onClicked() }
      .testTag("video_item_${video.id}"),
    colors = CardDefaults.cardColors(
      containerColor = Color(0x801A1A1A) // bg-[#1A1A1A]/50 from design HTML
    ),
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), // border-white/5
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Thumbnail Box
      Box(
        modifier = Modifier
          .size(width = 96.dp, height = 64.dp) // w-24 h-16 in HTML
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF334155))
      ) {
        if (!video.thumbnailUri.isNullOrEmpty()) {
          AsyncImage(
            model = video.thumbnailUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.8f // opacity-80 in html
          )
        }

        // Overlay transparent play icon
        Box(
          modifier = Modifier
            .size(24.dp)
            .align(Alignment.Center)
            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
          Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
              .size(14.dp)
              .align(Alignment.Center)
          )
        }

        // Bottom right duration pill
        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(4.dp)
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
          Text(
            text = video.getFormattedDuration(),
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Text Labels
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.Center
      ) {
        Text(
          text = video.title,
          color = Color.White,
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = video.getFormattedSize(),
            color = Color(0xFF64748B), // slate-500
            fontSize = 11.sp
          )

          Spacer(modifier = Modifier.width(6.dp))

          Text(
            text = "•",
            color = Color(0xFF475569), // slate-600
            fontSize = 11.sp
          )

          Spacer(modifier = Modifier.width(6.dp))

          if (video.isDemo) {
            Text(
              text = "فيديو سحابي",
              color = Color(0xFF8B5CF6), // violet gradient themed accent
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          } else {
            Text(
              text = "محلي",
              color = Color(0xFF94A3B8),
              fontSize = 11.sp
            )
          }
        }
      }

      // Action Row
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
      ) {
        IconButton(onClick = onInfoRequested) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "تفاصيل وتاريخ ومسار الفيديو",
            tint = Color(0xFF64748B),
            modifier = Modifier.size(18.dp)
          )
        }

        if (!video.isDemo) {
          IconButton(onClick = onDeleteRequested) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "حذف الفيديو",
              tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }
  }
}
