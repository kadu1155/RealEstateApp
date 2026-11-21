package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.Reel
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SunsetAmber
import com.example.ui.theme.SunsetOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A standalone, high-performance vertical scrolling feed UI component in Jetpack Compose
 * for displaying property video reels with rich overlays (price, location, BHK, creator info,
 * interactive CTA cards, engagement metrics, mute toggle, and double-tap like heart animation).
 */
@Composable
fun PropertyReelsFeed(
    reels: List<Reel>,
    onLikeClick: (String) -> Unit,
    onSaveClick: (String) -> Unit,
    onViewPropertyClick: (String) -> Unit,
    onEnquireClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialPageIndex: Int = 0,
    isHostActive: Boolean = true,
    focusReelId: String? = null,
    onFocusReelConsumed: () -> Unit = {},
    onShareClick: ((Reel) -> Unit)? = null,
    onCommentClick: ((Reel) -> Unit)? = null,
    onUploadClick: () -> Unit = {}
) {
    if (reels.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = SunsetOrange,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading Property Reels...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = initialPageIndex.coerceIn(0, reels.lastIndex),
        pageCount = { reels.size }
    )
    val coroutineScope = rememberCoroutineScope()

    val flingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        pagerSnapDistance = PagerSnapDistance.atMost(1),
        snapPositionalThreshold = 0.25f,
        snapAnimationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    // Immediately focus newly published/uploaded reel when it lands in the feed
    LaunchedEffect(focusReelId, reels) {
        if (!focusReelId.isNullOrBlank()) {
            val targetIdx = reels.indexOfFirst { it.id == focusReelId }
            if (targetIdx >= 0) {
                pagerState.animateScrollToPage(targetIdx)
                onFocusReelConsumed()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("property_reels_vertical_feed")
    ) {
        VerticalPager(
            state = pagerState,
            key = { index -> reels[index].id },
            flingBehavior = flingBehavior,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxSize()
                .testTag("reels_vertical_pager")
        ) { page ->
            val reel = reels[page]
            val isCurrentPage = pagerState.currentPage == page

            PropertyReelItem(
                reel = reel,
                isActive = isCurrentPage,
                isHostActive = isHostActive,
                onLikeClick = { onLikeClick(reel.id) },
                onSaveClick = { onSaveClick(reel.id) },
                onViewPropertyClick = { reel.propertyId?.let { onViewPropertyClick(it) } },
                onEnquireClick = { reel.propertyId?.let { onEnquireClick(it) } },
                onShareClick = { onShareClick?.invoke(reel) },
                onCommentClick = { onCommentClick?.invoke(reel) },
                onAutoAdvanceNext = {
                    if (page < reels.size - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(page + 1)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Floating Minimal City & Page Counter Pill
        val currentReel = reels.getOrNull(pagerState.currentPage)
        val cityLabel = currentReel?.city ?: "Properties"
        Surface(
            color = Color.Black.copy(alpha = 0.55f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 8.dp)
                .testTag("reel_counter_pill")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = SunsetOrange,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$cityLabel • ${pagerState.currentPage + 1}/${reels.size}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Floating Upper-Right Plus (+) Button for Quick Reel Upload (Positioned with safe insets and zero overlap)
        Surface(
            onClick = onUploadClick,
            color = SunsetOrange,
            shape = CircleShape,
            shadowElevation = 8.dp,
            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.5f)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 16.dp, top = 8.dp)
                .size(42.dp)
                .testTag("btn_reels_create_plus")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Upload Reel",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Individual full-screen vertical reel item with media container and overlay HUD.
 */
@Composable
fun PropertyReelItem(
    reel: Reel,
    isActive: Boolean,
    onLikeClick: () -> Unit,
    onSaveClick: () -> Unit,
    onViewPropertyClick: () -> Unit,
    onEnquireClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHostActive: Boolean = true,
    onShareClick: (() -> Unit)? = null,
    onCommentClick: (() -> Unit)? = null,
    onAutoAdvanceNext: () -> Unit = {}
) {
    var isMuted by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("reel_item_${reel.id}")
    ) {
        // 1. Unified Media Renderer for Video Walkthroughs and Photo Tours
        ReelMediaRenderer(
            reel = reel,
            isActive = isActive,
            isPlaying = isPlaying,
            isMuted = isMuted,
            isHostActive = isHostActive,
            onTogglePlay = { isPlaying = !isPlaying },
            onDoubleTapLike = {
                if (!reel.isLikedByMe) {
                    onLikeClick()
                }
            },
            onAutoAdvanceNext = onAutoAdvanceNext,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top Controls: Sponsored Pill & Mute Button (Mute button only for videos with sound)
        if (reel.isSponsored) {
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SunsetAmber.copy(alpha = 0.4f)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = if (reel.mediaType == "image") 76.dp else 48.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Sponsored",
                        tint = SunsetAmber,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SPONSORED",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Mute Button: For videos, positioned at end = 68.dp with statusBarsPadding (zero overlap with Plus button at end = 16.dp)
        if (reel.mediaType != "image") {
            IconButton(
                onClick = { isMuted = !isMuted },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 68.dp, top = 9.dp)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)), CircleShape)
                    .testTag("btn_reel_mute_toggle")
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = if (isMuted) "Unmute Reel" else "Mute Reel",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 6. Right Side Action Engagement Column (Like, Comment, Save, Share)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 110.dp)
        ) {
            // Like
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onLikeClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .testTag("reel_like_button_${reel.id}")
                ) {
                    Icon(
                        imageVector = if (reel.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like Reel",
                        tint = if (reel.isLikedByMe) Color(0xFFEF4444) else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${reel.likesCount}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Comments
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { onCommentClick?.invoke() },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .testTag("reel_comments_button_${reel.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${reel.commentsCount}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Save / Bookmark
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onSaveClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .testTag("reel_save_button_${reel.id}")
                ) {
                    Icon(
                        imageVector = if (reel.isSavedByMe) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Save Reel",
                        tint = if (reel.isSavedByMe) SunsetAmber else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${reel.savesCount}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Share
            IconButton(
                onClick = { onShareClick?.invoke() },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    .testTag("reel_share_button_${reel.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 7. Bottom Property Overlay Information Card & Funnel Actions
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 74.dp, bottom = 18.dp)
        ) {
            // Price & Location Highlight Chips
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                // Price Highlight Badge
                val priceText = reel.propertyPriceDisplay
                if (!priceText.isNullOrBlank()) {
                    Surface(
                        color = SunsetOrange,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = priceText,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // BHK / Property Type Badge
                if (reel.propertyBhk != null && reel.propertyBhk > 0) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "${reel.propertyBhk} BHK",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                // Location Tag
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = SunsetAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = reel.propertyLocality ?: reel.locality,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Creator Name & Verification
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Text(
                    text = reel.creatorName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                VerifiedBadgeChip(badge = reel.creatorBadge)
            }

            // Caption Text (2-line expandable)
            Text(
                text = reel.caption,
                color = Color.White.copy(alpha = 0.92f),
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Linked Property Card with Direct CTA
            if (reel.propertyId != null) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F172A).copy(alpha = 0.92f)
                    ),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onViewPropertyClick)
                        .testTag("reel_property_card_${reel.propertyId}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = reel.propertyTitle ?: "Featured Listing",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${reel.propertyLocality ?: reel.locality} • ${reel.propertyPriceDisplay ?: ""}",
                                color = SunsetAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // In-Reel Enquire Button
                        OutlinedButton(
                            onClick = onEnquireClick,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, SunsetOrange),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("btn_reel_enquire_${reel.id}")
                        ) {
                            Text(
                                text = "Enquire",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SunsetOrange
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // In-Reel View Details Button
                        Button(
                            onClick = onViewPropertyClick,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SunsetOrange,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("btn_view_property_${reel.propertyId}")
                        ) {
                            Text(
                                text = "View",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
