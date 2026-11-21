package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.model.Property
import com.example.domain.model.Reel
import com.example.domain.model.User
import com.example.ui.components.resolveVideoUri
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SunsetAmber
import com.example.ui.theme.SunsetOrange
import com.example.util.MediaUploadHelper
import com.example.util.PickedMediaResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

enum class ReelMediaTypeOption(val label: String, val mediaTypeKey: String) {
    VIDEO("Upload Video", "video"),
    IMAGE("Upload Image", "image")
}

data class DemoMediaSample(
    val title: String,
    val type: ReelMediaTypeOption,
    val mediaUrl: String,
    val thumbnailUrl: String,
    val durationSeconds: Int,
    val defaultLocality: String,
    val defaultCaption: String
)

val PRESET_DEMO_MEDIA = listOf(
    DemoMediaSample(
        title = "Skyline Luxury Penthouse",
        type = ReelMediaTypeOption.VIDEO,
        mediaUrl = "https://assets.propreel.in/videos/demo_reel_baner.mp4",
        thumbnailUrl = "https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?w=900&q=80",
        durationSeconds = 15,
        defaultLocality = "Baner",
        defaultCaption = "Breathtaking 4 BHK Penthouse tour with wrap-around infinity terrace and Pune skyline views! ✨ #LuxuryLiving #Baner"
    ),
    DemoMediaSample(
        title = "Modern Duplex Villa",
        type = ReelMediaTypeOption.VIDEO,
        mediaUrl = "https://assets.propreel.in/videos/demo_reel_kothrud.mp4",
        thumbnailUrl = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=900&q=80",
        durationSeconds = 15,
        defaultLocality = "Wakad",
        defaultCaption = "Private pool villa walkthrough in prime Wakad with smart home automation. 🏡🔑 #VillaTour #PuneHomes"
    ),
    DemoMediaSample(
        title = "Worli Sea View Residence",
        type = ReelMediaTypeOption.IMAGE,
        mediaUrl = "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=1080&q=85",
        thumbnailUrl = "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=1080&q=85",
        durationSeconds = 0,
        defaultLocality = "Worli",
        defaultCaption = "Unobstructed Arabian Sea panorama from the 38th floor in Worli South Mumbai. 🌊🏙️ #SeaFacing #WorliLuxury"
    ),
    DemoMediaSample(
        title = "Kharadi Tech Park Highrise",
        type = ReelMediaTypeOption.IMAGE,
        mediaUrl = "https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?w=1080&q=85",
        thumbnailUrl = "https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?w=1080&q=85",
        durationSeconds = 0,
        defaultLocality = "Kharadi",
        defaultCaption = "Contemporary 3 BHK Italian marble residence 5 mins from EON Free Zone Kharadi. 💼✨ #WalkToWork #Kharadi"
    )
)

/**
 * Modern, polished bottom sheet modal for uploading real estate video reels and photo reels.
 * Features:
 * - Image & Video media option toggle
 * - Gallery picker + 1-tap real estate demo media selector
 * - Live media preview with type badge
 * - Caption input with character counter and emoji/hashtag suggestions
 * - Locality selector (Pune & Mumbai hotspots)
 * - Optional Property link selector
 * - File format and size validation with error alerts
 * - Simulated upload progress indicator with live steps
 * - Immediate Room persistence & feed update
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UploadReelModal(
    currentUser: User,
    properties: List<Property>,
    onDismiss: () -> Unit,
    onReelCreated: (Reel) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    var selectedMediaType by remember { mutableStateOf(ReelMediaTypeOption.VIDEO) }
    var isUserPickedMedia by remember { mutableStateOf(false) }
    var isPreparingMedia by remember { mutableStateOf(false) }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var mediaUrl by remember { mutableStateOf(PRESET_DEMO_MEDIA[0].mediaUrl) }
    var thumbnailUrl by remember { mutableStateOf(PRESET_DEMO_MEDIA[0].thumbnailUrl) }
    var selectedMediaName by remember { mutableStateOf(PRESET_DEMO_MEDIA[0].title) }
    var selectedFileSizeMb by remember { mutableDoubleStateOf(14.2) }
    var selectedDurationSeconds by remember { mutableIntStateOf(PRESET_DEMO_MEDIA[0].durationSeconds) }

    var caption by remember { mutableStateOf(PRESET_DEMO_MEDIA[0].defaultCaption) }
    var locality by remember { mutableStateOf(PRESET_DEMO_MEDIA[0].defaultLocality) }
    var selectedProperty by remember { mutableStateOf<Property?>(null) }
    var isPropertyDropdownExpanded by remember { mutableStateOf(false) }

    // Validation & Error state
    var validationError by remember { mutableStateOf<String?>(null) }

    // Upload progress state
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var uploadStageText by remember { mutableStateOf("") }
    val animatedProgress by animateFloatAsState(targetValue = uploadProgress, label = "upload_progress")

    fun handlePickedDeviceUri(uri: Uri?, preferredType: ReelMediaTypeOption?) {
        if (uri == null) return
        isPreparingMedia = true
        isPreviewPlaying = false
        validationError = null
        coroutineScope.launch {
            when (val res = MediaUploadHelper.persistAndPreparePickedMedia(context, uri, preferredType)) {
                is PickedMediaResult.Success -> {
                    selectedMediaType = res.mediaType
                    isUserPickedMedia = true
                    mediaUrl = res.localMediaUri
                    thumbnailUrl = res.localThumbnailUri
                    selectedMediaName = res.displayName
                    selectedFileSizeMb = res.fileSizeMb
                    selectedDurationSeconds = res.durationSeconds
                    validationError = null
                    if (res.mediaType == ReelMediaTypeOption.VIDEO) {
                        isPreviewPlaying = true
                    }
                }
                is PickedMediaResult.Error -> {
                    validationError = res.message
                }
            }
            isPreparingMedia = false
        }
    }

    // Unified Photo Picker Launcher (supports VideoOnly, ImageOnly, or ImageAndVideo)
    val visualMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        handlePickedDeviceUri(uri, selectedMediaType)
    }

    // Zero-permission SAF File Browser Launcher (guarantees access to MP4/MOV in Downloads/Files/Drive)
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        handlePickedDeviceUri(uri, selectedMediaType)
    }

    val popularLocalities = remember {
        listOf("Baner", "Wakad", "Kharadi", "Hinjawadi", "Worli", "Bandra West", "Powai", "Andheri West")
    }

    val suggestedHashtags = remember {
        listOf("#LuxuryLiving", "#ReadyToMove", "#BanerHomes", "#WorliSeaView", "#ModernVilla", "#RERAVerified")
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isUploading) onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("upload_reel_modal")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Create Real Estate Reel",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Showcase verified homes with short vertical video or photo reels",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    enabled = !isUploading,
                    modifier = Modifier.testTag("btn_close_upload_modal")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 1: Media Option Segmented Cards (Video vs Image)
            Text(
                text = "1. CHOOSE MEDIA FORMAT",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = SunsetOrange
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Video Option Card
                Card(
                    onClick = {
                        if (!isUploading && !isPreparingMedia) {
                            selectedMediaType = ReelMediaTypeOption.VIDEO
                            validationError = null
                            isPreviewPlaying = false
                            if (!isUserPickedMedia) {
                                val defaultSample = PRESET_DEMO_MEDIA.first { it.type == ReelMediaTypeOption.VIDEO }
                                mediaUrl = defaultSample.mediaUrl
                                thumbnailUrl = defaultSample.thumbnailUrl
                                selectedMediaName = defaultSample.title
                                selectedDurationSeconds = defaultSample.durationSeconds
                                selectedFileSizeMb = 14.2
                            }
                        }
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedMediaType == ReelMediaTypeOption.VIDEO)
                            SunsetOrange.copy(alpha = 0.12f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = BorderStroke(
                        width = if (selectedMediaType == ReelMediaTypeOption.VIDEO) 2.dp else 1.dp,
                        color = if (selectedMediaType == ReelMediaTypeOption.VIDEO) SunsetOrange else Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("option_upload_video")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = if (selectedMediaType == ReelMediaTypeOption.VIDEO) SunsetOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Upload Video",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (selectedMediaType == ReelMediaTypeOption.VIDEO) SunsetOrange else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "MP4, MOV (Up to 45s)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Image Option Card
                Card(
                    onClick = {
                        if (!isUploading && !isPreparingMedia) {
                            selectedMediaType = ReelMediaTypeOption.IMAGE
                            validationError = null
                            isPreviewPlaying = false
                            if (!isUserPickedMedia) {
                                val defaultSample = PRESET_DEMO_MEDIA.first { it.type == ReelMediaTypeOption.IMAGE }
                                mediaUrl = defaultSample.mediaUrl
                                thumbnailUrl = defaultSample.thumbnailUrl
                                selectedMediaName = defaultSample.title
                                selectedDurationSeconds = 0
                                selectedFileSizeMb = 2.1
                            }
                        }
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedMediaType == ReelMediaTypeOption.IMAGE)
                            SunsetOrange.copy(alpha = 0.12f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = BorderStroke(
                        width = if (selectedMediaType == ReelMediaTypeOption.IMAGE) 2.dp else 1.dp,
                        color = if (selectedMediaType == ReelMediaTypeOption.IMAGE) SunsetOrange else Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("option_upload_image")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = if (selectedMediaType == ReelMediaTypeOption.IMAGE) SunsetOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Upload Image",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (selectedMediaType == ReelMediaTypeOption.IMAGE) SunsetOrange else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "JPG, PNG, WEBP (Hi-Res)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 2: Media Source & Live Interactive Preview
            Text(
                text = "2. MEDIA SOURCE & PREVIEW",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = SunsetOrange
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Choose device media buttons (Gallery Photo Picker + SAF Device Files Browser)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (isUploading || isPreparingMedia) return@OutlinedButton
                        val mediaFilter = if (selectedMediaType == ReelMediaTypeOption.VIDEO) {
                            ActivityResultContracts.PickVisualMedia.VideoOnly
                        } else {
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        }
                        try {
                            if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
                                visualMediaLauncher.launch(PickVisualMediaRequest(mediaFilter))
                            } else {
                                openDocumentLauncher.launch(
                                    if (selectedMediaType == ReelMediaTypeOption.VIDEO) {
                                        arrayOf("video/*")
                                    } else {
                                        arrayOf("image/*")
                                    }
                                )
                            }
                        } catch (_: ActivityNotFoundException) {
                            openDocumentLauncher.launch(arrayOf("video/*", "image/*"))
                        }
                    },
                    enabled = !isUploading && !isPreparingMedia,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_pick_media_gallery")
                ) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedMediaType == ReelMediaTypeOption.VIDEO) "Select Video" else "Select Photo",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = {
                        if (isUploading || isPreparingMedia) return@OutlinedButton
                        openDocumentLauncher.launch(
                            if (selectedMediaType == ReelMediaTypeOption.VIDEO) {
                                arrayOf("video/*", "image/*")
                            } else {
                                arrayOf("image/*", "video/*")
                            }
                        )
                    },
                    enabled = !isUploading && !isPreparingMedia,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_browse_media_files")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Browse Files",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Curated Quick-Select Chips (for instant testing in sandbox/emulator)
            Text(
                text = "Or pick curated sample:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PRESET_DEMO_MEDIA
                    .filter { it.type == selectedMediaType }
                    .forEach { sample ->
                        val isSelected = mediaUrl == sample.mediaUrl
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                isUserPickedMedia = false
                                isPreviewPlaying = false
                                mediaUrl = sample.mediaUrl
                                thumbnailUrl = sample.thumbnailUrl
                                selectedMediaName = sample.title
                                selectedDurationSeconds = sample.durationSeconds
                                selectedFileSizeMb = if (sample.type == ReelMediaTypeOption.VIDEO) 14.2 else 2.1
                                caption = sample.defaultCaption
                                locality = sample.defaultLocality
                                validationError = null
                            },
                            label = { Text(sample.title, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SunsetOrange,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Media Preview Card (supports live inline ExoPlayer preview for picked/demo video)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable(enabled = !isPreparingMedia && selectedMediaType == ReelMediaTypeOption.VIDEO) {
                        isPreviewPlaying = !isPreviewPlaying
                    }
                    .testTag("media_preview_container")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (thumbnailUrl.isNotBlank()) {
                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = "Media Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (selectedMediaType == ReelMediaTypeOption.VIDEO && isPreviewPlaying && mediaUrl.isNotBlank()) {
                        UploadVideoInlinePreview(
                            mediaUrl = mediaUrl,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Subtle Dark Gradient Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = if (isPreviewPlaying) 0.15f else 0.35f))
                    )

                    // Top Badge: Media Type & Format
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (selectedMediaType == ReelMediaTypeOption.VIDEO) Icons.Default.Movie else Icons.Default.Image,
                                contentDescription = null,
                                tint = SunsetAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (selectedMediaType == ReelMediaTypeOption.VIDEO) {
                                    "VIDEO TOUR • ${selectedDurationSeconds.coerceAtLeast(1)}s"
                                } else {
                                    "PHOTO REEL • 9:16"
                                },
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Center Preparing Spinner or Play/Pause Toggle for Video
                    if (isPreparingMedia) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            CircularProgressIndicator(
                                color = SunsetOrange,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Preparing media...",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else if (selectedMediaType == ReelMediaTypeOption.VIDEO) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = CircleShape,
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Icon(
                                imageVector = if (isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPreviewPlaying) "Pause Preview" else "Play Preview",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(28.dp)
                            )
                        }
                    }

                    // Bottom info bar
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.72f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedMediaName,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${"%.1f".format(selectedFileSizeMb)} MB • Ready",
                            color = EmeraldPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 3: Caption Input with Suggestions
            Text(
                text = "3. CAPTION & HASHTAGS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = SunsetOrange
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = caption,
                onValueChange = {
                    if (it.length <= 300) {
                        caption = it
                        validationError = null
                    }
                },
                label = { Text("Reel Caption") },
                placeholder = { Text("Tell home seekers about the key highlights, views, or amenities...") },
                supportingText = {
                    Text(
                        text = "${caption.length}/300 characters",
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_reel_caption")
            )

            // Hashtag quick suggestions
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                suggestedHashtags.forEach { tag ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable {
                            if (!caption.contains(tag)) {
                                caption = if (caption.isBlank()) tag else "$caption $tag"
                            }
                        }
                    ) {
                        Text(
                            text = tag,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 4: Locality Selection
            Text(
                text = "4. LOCALITY",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = SunsetOrange
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = locality,
                onValueChange = {
                    locality = it
                    validationError = null
                },
                label = { Text("Locality / Micro-Market") },
                placeholder = { Text("e.g. Baner, Wakad, Worli") },
                leadingIcon = {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = SunsetOrange)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_reel_locality")
            )

            // Popular Locality Chips
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                popularLocalities.forEach { loc ->
                    val isSelected = locality.equals(loc, ignoreCase = true)
                    Surface(
                        color = if (isSelected) SunsetOrange else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable {
                            locality = loc
                            validationError = null
                        }
                    ) {
                        Text(
                            text = loc,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 5: Optional Property Link
            Text(
                text = "5. LINK PROPERTY (OPTIONAL)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = SunsetOrange
            )
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("property_link_selector")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (selectedProperty != null) Icons.Default.AddLink else Icons.Default.LinkOff,
                                contentDescription = null,
                                tint = if (selectedProperty != null) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedProperty != null) "Attached Property" else "No Property Attached",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }

                        if (selectedProperty != null) {
                            TextButton(onClick = { selectedProperty = null }) {
                                Text("Remove", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                    }

                    if (selectedProperty != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            AsyncImage(
                                model = selectedProperty?.imageUrls?.firstOrNull() ?: "",
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedProperty?.title ?: "",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${selectedProperty?.priceDisplay} • ${selectedProperty?.locality}",
                                    fontSize = 11.sp,
                                    color = SunsetOrange,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Link an existing property listing to display price, BHK, and direct enquiry CTAs on this reel.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Property selection dropdown box
                        ExposedDropdownMenuBox(
                            expanded = isPropertyDropdownExpanded,
                            onExpandedChange = { isPropertyDropdownExpanded = !isPropertyDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedProperty?.title ?: "Select from available listings...",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPropertyDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                                    .testTag("dropdown_select_property")
                            )

                            ExposedDropdownMenu(
                                expanded = isPropertyDropdownExpanded,
                                onDismissRequest = { isPropertyDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("None (Standalone Reel)") },
                                    onClick = {
                                        selectedProperty = null
                                        isPropertyDropdownExpanded = false
                                    }
                                )
                                properties.take(6).forEach { prop ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = prop.title,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${prop.priceDisplay} • ${prop.locality}",
                                                    fontSize = 11.sp,
                                                    color = SunsetOrange
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedProperty = prop
                                            locality = prop.locality
                                            isPropertyDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Error display
            if (validationError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("upload_validation_error")
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = validationError ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Upload Progress Animation
            AnimatedVisibility(
                visible = isUploading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .testTag("upload_progress_container")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uploadStageText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SunsetOrange
                        )
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = SunsetOrange
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        color = SunsetOrange,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .testTag("upload_progress_bar")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons: Publish Reel & Cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isUploading,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_cancel_upload")
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        // Validate inputs
                        if (caption.trim().isBlank()) {
                            validationError = "Please enter a caption for your reel."
                            return@Button
                        }
                        if (locality.trim().isBlank()) {
                            validationError = "Please specify a locality (e.g., Baner, Wakad, Worli)."
                            return@Button
                        }
                        if (mediaUrl.isBlank()) {
                            validationError = "Please select a valid image or video media file."
                            return@Button
                        }

                        // Start simulated upload progress with realistic stages
                        isUploading = true
                        validationError = null

                        coroutineScope.launch {
                            uploadStageText = "Verifying ${selectedMediaType.mediaTypeKey} format & compression..."
                            uploadProgress = 0.25f
                            delay(300)

                            uploadStageText = "Transcoding vertical 9:16 stream..."
                            uploadProgress = 0.60f
                            delay(400)

                            uploadStageText = "Generating HD poster thumbnail..."
                            uploadProgress = 0.85f
                            delay(300)

                            uploadStageText = "Saving to PropReel database..."
                            uploadProgress = 1.0f
                            delay(200)

                            // Construct new Reel record
                            val newId = "reel_${System.currentTimeMillis()}"
                            val detectedCity = if (
                                locality.contains("Worli", ignoreCase = true) ||
                                locality.contains("Bandra", ignoreCase = true) ||
                                locality.contains("Powai", ignoreCase = true) ||
                                locality.contains("Andheri", ignoreCase = true) ||
                                locality.contains("Mumbai", ignoreCase = true)
                            ) "Mumbai" else "Pune"

                            val newReel = Reel(
                                id = newId,
                                userId = currentUser.id,
                                creatorName = currentUser.fullName,
                                creatorBadge = currentUser.verificationBadge,
                                creatorRole = currentUser.role,
                                propertyId = selectedProperty?.id,
                                propertyTitle = selectedProperty?.title,
                                propertyPriceDisplay = selectedProperty?.priceDisplay,
                                propertyLocality = selectedProperty?.locality ?: locality,
                                propertyBhk = selectedProperty?.bhk,
                                caption = caption.trim(),
                                videoUrl = mediaUrl,
                                thumbnailUrl = thumbnailUrl,
                                durationSeconds = if (selectedMediaType == ReelMediaTypeOption.VIDEO) selectedDurationSeconds.coerceAtLeast(1) else 0,
                                city = detectedCity,
                                locality = locality.trim(),
                                isSponsored = false,
                                likesCount = 0,
                                commentsCount = 0,
                                savesCount = 0,
                                sharesCount = 0,
                                isLikedByMe = false,
                                isSavedByMe = false,
                                mediaType = selectedMediaType.mediaTypeKey
                            )

                            isPreviewPlaying = false
                            onReelCreated(newReel)
                        }
                    },
                    enabled = !isUploading && !isPreparingMedia,
                    colors = ButtonDefaults.buttonColors(containerColor = SunsetOrange),
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("btn_publish_reel")
                ) {
                    if (isUploading) {
                        Text("Publishing...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Publish Reel")
                    }
                }
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun UploadVideoInlinePreview(
    mediaUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewUri = remember(mediaUrl, context) {
        resolveVideoUri(context, mediaUrl, "preview_${mediaUrl.hashCode()}")
    }
    val exoPlayer = remember(previewUri, context) {
        try {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(previewUri))
                repeatMode = Player.REPEAT_MODE_ONE
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    false
                )
                volume = 0f
                playWhenReady = true
                prepare()
            }
        } catch (_: Exception) {
            null
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    exoPlayer?.playWhenReady = true
                    exoPlayer?.play()
                }
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_DESTROY -> {
                    exoPlayer?.playWhenReady = false
                    exoPlayer?.pause()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer?.stop()
            exoPlayer?.release()
        }
    }

    if (exoPlayer != null) {
        AndroidView(
            factory = { ctx ->
                (LayoutInflater.from(ctx).inflate(R.layout.reel_player_view, null, false) as PlayerView).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    isClickable = false
                    isFocusable = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                if (view.player != exoPlayer) view.player = exoPlayer
            },
            modifier = modifier
        )
    }
}
