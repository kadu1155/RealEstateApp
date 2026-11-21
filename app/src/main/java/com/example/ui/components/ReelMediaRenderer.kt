package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.model.Reel
import com.example.ui.theme.SunsetAmber
import com.example.ui.theme.SunsetOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "ReelVideoPlayer"

/**
 * Resolves a reliable playback Uri for video reels.
 * Handles local raw bundled resources (android.resource://), content URIs,
 * file URIs, HTTPS streams, and falls back from obsolete 403 Google demo URLs
 * to bundled vertical HD video assets.
 */
fun resolveVideoUri(context: Context, videoUrl: String, reelId: String): Uri {
    val trimmed = videoUrl.trim()
    Log.d(TAG, "[$reelId] Resolving video URI: rawUrl='$trimmed'")

    val pkg = context.packageName

    val isBundledBaner4Bhk = trimmed.contains("demo_reel_baner_4bhk") ||
            reelId == "reel_05_video"

    val isBundledBaner = !isBundledBaner4Bhk && (
            trimmed.contains("demo_reel_baner") ||
            trimmed.contains("ForBiggerBlazes") ||
            reelId == "reel_01_video"
            )

    val isBundledKothrud = trimmed.contains("demo_reel_kothrud") ||
            trimmed.contains("TearsOfSteel") ||
            trimmed.contains("ForBiggerJoyBlazes") ||
            reelId == "reel_03_video"

    return when {
        isBundledBaner4Bhk -> {
            val resolved = Uri.parse("android.resource://$pkg/raw/demo_reel_baner_4bhk")
            Log.d(TAG, "[$reelId] Mapped to bundled Baner 4 BHK video asset: $resolved")
            resolved
        }
        isBundledBaner -> {
            val resolved = Uri.parse("android.resource://$pkg/raw/demo_reel_baner")
            Log.d(TAG, "[$reelId] Mapped to bundled Baner video asset: $resolved")
            resolved
        }
        isBundledKothrud -> {
            val resolved = Uri.parse("android.resource://$pkg/raw/demo_reel_kothrud")
            Log.d(TAG, "[$reelId] Mapped to bundled Kothrud video asset: $resolved")
            resolved
        }
        trimmed.startsWith("android.resource://") -> {
            Uri.parse(trimmed)
        }
        trimmed.startsWith("content://") || trimmed.startsWith("file://") -> {
            Uri.parse(trimmed)
        }
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> {
            Uri.parse(trimmed)
        }
        trimmed.isNotEmpty() -> {
            Uri.parse(trimmed)
        }
        else -> {
            Uri.parse("android.resource://$pkg/raw/demo_reel_baner")
        }
    }
}

/**
 * Reusable media rendering engine for mixed-media real estate reels.
 * Fully supports:
 * - High-definition Video walkthroughs with ExoPlayer playback and mute controls
 * - Full-screen Image photo tours with "PHOTO TOUR" indicator
 * - Smooth configurable auto-advance timer for image reels (5s per slide)
 * - Gesture-compatible pinch-to-zoom for detailed architectural inspection
 * - Double-tap heart burst animation & single-tap play/pause state flash
 * - Graceful fallback and loading state handling for invalid/offline media
 */
@OptIn(UnstableApi::class)
@Composable
fun ReelMediaRenderer(
    reel: Reel,
    isActive: Boolean,
    isPlaying: Boolean,
    isMuted: Boolean,
    onTogglePlay: () -> Unit,
    onDoubleTapLike: () -> Unit,
    onAutoAdvanceNext: () -> Unit,
    modifier: Modifier = Modifier,
    isHostActive: Boolean = true,
    autoAdvanceDurationMs: Long = 5000L
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val isImageReel = reel.mediaType.trim().equals("image", ignoreCase = true)

    var isLifecycleResumed by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }

    val shouldPlay = isActive && isPlaying && isHostActive && isLifecycleResumed

    // Double-tap heart burst state
    var showBigHeart by remember { mutableStateOf(false) }
    val heartScale = remember { Animatable(0f) }

    // Pinch-to-zoom state (Image reels)
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var zoomOffset by remember { mutableStateOf(Offset.Zero) }

    // Video stream playback progress (0.0 to 1.0)
    var videoPlaybackProgress by remember { mutableFloatStateOf(0f) }

    // Image auto-advance progress (0.0 to 1.0)
    var imageAdvanceProgress by remember { mutableFloatStateOf(0f) }

    // Reset zoom and offsets whenever the reel changes or moves out of the viewport
    LaunchedEffect(isActive, reel.id) {
        if (!isActive) {
            zoomScale = 1f
            zoomOffset = Offset.Zero
            videoPlaybackProgress = 0f
            imageAdvanceProgress = 0f
        }
    }

    // Video Player State (Initialized ONLY when this video reel is the active page to keep pager swipes 60fps+)
    var isBuffering by remember(reel.id, isActive) { mutableStateOf(isActive && !isImageReel) }
    var playbackError by remember(reel.id, isActive) { mutableStateOf<String?>(null) }
    var hasRenderedFirstFrame by remember(reel.id, isActive) { mutableStateOf(false) }

    val resolvedVideoUri = remember(reel.id, reel.videoUrl, isImageReel) {
        if (!isImageReel) {
            resolveVideoUri(context, reel.videoUrl, reel.id)
        } else {
            Uri.EMPTY
        }
    }

    val player: ExoPlayer? = remember(reel.id, resolvedVideoUri, context, isImageReel, isActive) {
        if (!isImageReel && isActive) {
            try {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(resolvedVideoUri))
                    repeatMode = Player.REPEAT_MODE_ONE
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                            .setUsage(C.USAGE_MEDIA)
                            .build(),
                        true
                    )
                    setHandleAudioBecomingNoisy(true)
                    volume = if (isMuted) 0f else 1f
                    playWhenReady = isPlaying && isHostActive && isLifecycleResumed
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_BUFFERING -> isBuffering = true
                                Player.STATE_READY -> {
                                    isBuffering = false
                                    playbackError = null
                                }
                                Player.STATE_ENDED -> isBuffering = false
                                Player.STATE_IDLE -> {}
                            }
                        }

                        override fun onRenderedFirstFrame() {
                            hasRenderedFirstFrame = true
                            isBuffering = false
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            Log.e(TAG, "[${reel.id}] ExoPlayer error: ${error.message} (code=${error.errorCodeName})", error)
                            playbackError = error.message ?: "Video playback failed"
                            isBuffering = false
                        }
                    })
                    prepare()
                }
            } catch (e: Exception) {
                Log.e(TAG, "[${reel.id}] Exception initializing ExoPlayer: ${e.message}", e)
                playbackError = e.message
                null
            }
        } else {
            null
        }
    }

    // Immediate Activity / App background & screen-lock audio stop observer
    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    isLifecycleResumed = true
                }
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_DESTROY -> {
                    isLifecycleResumed = false
                    player?.playWhenReady = false
                    player?.pause()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Lifecycle Synchronization: Mute State
    LaunchedEffect(isMuted, player) {
        player?.let {
            it.volume = if (isMuted) 0f else 1f
        }
    }

    // Lifecycle Synchronization: Viewport Active + In-App Focus + OS Resumed
    LaunchedEffect(shouldPlay, player) {
        player?.let {
            if (shouldPlay) {
                it.playWhenReady = true
                it.play()
            } else {
                it.playWhenReady = false
                it.pause()
            }
        }
    }

    // Progress updates from ExoPlayer
    LaunchedEffect(shouldPlay, isActive, isImageReel, player) {
        if (!isImageReel && player != null && shouldPlay) {
            while (shouldPlay) {
                val duration = player.duration
                if (duration > 0) {
                    val pos = player.currentPosition
                    videoPlaybackProgress = (pos.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                }
                delay(150L)
            }
        } else if (!isActive) {
            videoPlaybackProgress = 0f
        }
    }

    // Release player immediately when Composable leaves composition or page becomes inactive
    DisposableEffect(reel.id, player) {
        onDispose {
            player?.playWhenReady = false
            player?.stop()
            player?.release()
        }
    }

    // Image Auto-Advance Lifecycle: Runs 5s countdown when active, focused, and resumed; cancelled otherwise
    LaunchedEffect(shouldPlay, isActive, isImageReel, reel.id) {
        if (isImageReel && shouldPlay) {
            imageAdvanceProgress = 0f
            val stepIntervalMs = 50L
            val totalSteps = (autoAdvanceDurationMs / stepIntervalMs).toInt()
            var currentStep = 0
            while (currentStep < totalSteps && shouldPlay) {
                delay(stepIntervalMs)
                currentStep++
                imageAdvanceProgress = (currentStep.toFloat() / totalSteps).coerceIn(0f, 1f)
            }
            if (currentStep >= totalSteps && shouldPlay) {
                onAutoAdvanceNext()
            }
        } else if (!isActive) {
            imageAdvanceProgress = 0f
        }
    }

    // Gesture handling: non-blocking pinch-to-zoom for image reels + tap gestures
    // Crucial: Single-finger drags when zoomScale <= 1.02f are NOT consumed so VerticalPager swiping remains 100% fluid!
    val gestureModifier = if (isImageReel) {
        Modifier
            .pointerInput(reel.id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }
                        if (pressedCount >= 2 || zoomScale > 1.02f) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val nextScale = (zoomScale * zoomChange).coerceIn(1f, 3.5f)
                            zoomScale = nextScale
                            if (nextScale > 1.02f) {
                                zoomOffset = Offset(
                                    x = (zoomOffset.x + panChange.x).coerceIn(-400f, 400f),
                                    y = (zoomOffset.y + panChange.y).coerceIn(-600f, 600f)
                                )
                                event.changes.forEach {
                                    if (it.positionChanged()) it.consume()
                                }
                            } else {
                                zoomOffset = Offset.Zero
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(reel.id) {
                detectTapGestures(
                    onDoubleTap = {
                        if (zoomScale > 1.05f) {
                            zoomScale = 1f
                            zoomOffset = Offset.Zero
                        } else {
                            onDoubleTapLike()
                            coroutineScope.launch {
                                showBigHeart = true
                                heartScale.snapTo(0.2f)
                                heartScale.animateTo(
                                    targetValue = 1.25f,
                                    animationSpec = spring(dampingRatio = 0.45f, stiffness = 400f)
                                )
                                delay(300)
                                showBigHeart = false
                                heartScale.snapTo(0f)
                            }
                        }
                    },
                    onTap = {
                        onTogglePlay()
                    }
                )
            }
            .graphicsLayer(
                scaleX = zoomScale,
                scaleY = zoomScale,
                translationX = zoomOffset.x,
                translationY = zoomOffset.y
            )
    } else {
        Modifier.pointerInput(reel.id) {
            detectTapGestures(
                onDoubleTap = {
                    onDoubleTapLike()
                    coroutineScope.launch {
                        showBigHeart = true
                        heartScale.snapTo(0.2f)
                        heartScale.animateTo(
                            targetValue = 1.25f,
                            animationSpec = spring(dampingRatio = 0.45f, stiffness = 400f)
                        )
                        delay(300)
                        showBigHeart = false
                        heartScale.snapTo(0f)
                    }
                },
                onTap = {
                    onTogglePlay()
                }
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag(if (isImageReel) "reel_image_renderer_${reel.id}" else "reel_video_renderer_${reel.id}")
    ) {
        val posterUrl = reel.thumbnailUrl
        val posterModel: Any? = when {
            posterUrl.contains("poster_baner_4bhk") ||
                posterUrl.contains("demo_reel_baner_4bhk") ||
                reel.id == "reel_05_video" -> R.drawable.poster_baner_4bhk
            posterUrl.isNotBlank() -> posterUrl
            else -> null
        }

        if (isImageReel) {
            // --- IMAGE REEL RENDERING ---
            val mediaDisplayUrl: Any = if (reel.videoUrl.isNotBlank()) reel.videoUrl else (posterModel ?: "")

            AsyncImage(
                model = mediaDisplayUrl,
                contentDescription = reel.caption,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(gestureModifier)
                    .testTag("reel_media_content_${reel.id}")
            )
        } else {
            // --- VIDEO REEL RENDERING ---
            // 1. Poster image layer (renders instantly at 60/120fps during swipes and buffering)
            if (posterModel != null) {
                AsyncImage(
                    model = posterModel,
                    contentDescription = reel.caption,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("reel_media_content_${reel.id}")
                )
            }

            // 2. Real ExoPlayer Video Player View (Attached only for active page; crossfades smoothly on first frame)
            val playerAlpha by animateFloatAsState(
                targetValue = if (hasRenderedFirstFrame) 1f else 0f,
                animationSpec = tween(durationMillis = 120),
                label = "video_first_frame_fade"
            )
            if (player != null && playbackError == null && isActive) {
                AndroidView(
                    factory = { ctx ->
                        (LayoutInflater.from(ctx).inflate(R.layout.reel_player_view, null, false) as PlayerView).apply {
                            this.player = player
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
                    update = { playerView ->
                        if (playerView.player != player) {
                            playerView.player = player
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(playerAlpha)
                        .testTag("reel_video_player_${reel.id}")
                )
            }

            // 3. Gesture overlay on top of video player to capture taps and double-tap like
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(gestureModifier)
            )

            // 4. Buffering indicator
            if (isActive && isBuffering && !hasRenderedFirstFrame && playbackError == null) {
                CircularProgressIndicator(
                    color = SunsetOrange,
                    strokeWidth = 3.dp,
                    modifier = Modifier
                        .size(38.dp)
                        .align(Alignment.Center)
                )
            }
        }

        // 2. Cinematic Gradient Overlay (Cached Brush avoids per-recomposition allocation)
        val gradientBrush = remember {
            Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.45f),
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.25f),
                    Color.Black.copy(alpha = 0.88f),
                    Color.Black.copy(alpha = 0.98f)
                ),
                startY = 0f
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
        )

        // 3. Media Type Indicator Badges
        if (isImageReel) {
            // Photo Tour Badge
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SunsetAmber.copy(alpha = 0.45f)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 48.dp)
                    .testTag("photo_tour_indicator")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Photo Tour",
                        tint = SunsetAmber,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PHOTO TOUR",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Zoom indicator when user pinches to inspect architectural details
            if (zoomScale > 1.1f) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 8.dp)
                        .testTag("zoom_scale_indicator")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "%.1fx Zoom (Double-tap to reset)".format(zoomScale),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 4. Progress Bars: Top Auto-Advance bar for Images, Bottom Video bar for Videos
        if (isImageReel) {
            LinearProgressIndicator(
                progress = { imageAdvanceProgress },
                color = SunsetAmber,
                trackColor = Color.White.copy(alpha = 0.15f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .align(Alignment.TopCenter)
                    .testTag("image_auto_advance_progress")
            )
        } else {
            LinearProgressIndicator(
                progress = { videoPlaybackProgress },
                color = SunsetOrange,
                trackColor = Color.White.copy(alpha = 0.15f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .align(Alignment.BottomCenter)
                    .testTag("video_playback_progress")
            )
        }

        // 5. Floating Play/Pause State Flash
        AnimatedVisibility(
            visible = !isPlaying,
            enter = fadeIn(tween(150)) + scaleIn(tween(150)),
            exit = fadeOut(tween(150)) + scaleOut(tween(150)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Paused",
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
        }

        // 6. Double-tap Heart Burst
        if (showBigHeart) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(96.dp)
                    .scale(heartScale.value)
            )
        }
    }
}
