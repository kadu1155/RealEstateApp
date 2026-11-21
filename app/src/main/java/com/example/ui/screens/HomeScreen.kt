package com.example.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.domain.model.Reel
import com.example.ui.components.PropertyReelsFeed

@Composable
fun HomeScreen(
    reels: List<Reel>,
    onLikeClick: (String) -> Unit,
    onSaveClick: (String) -> Unit,
    onViewPropertyClick: (String) -> Unit,
    onEnquireClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isHostActive: Boolean = true,
    focusReelId: String? = null,
    onFocusReelConsumed: () -> Unit = {},
    onUploadClick: () -> Unit = {}
) {
    PropertyReelsFeed(
        reels = reels,
        onLikeClick = onLikeClick,
        onSaveClick = onSaveClick,
        onViewPropertyClick = onViewPropertyClick,
        onEnquireClick = onEnquireClick,
        isHostActive = isHostActive,
        focusReelId = focusReelId,
        onFocusReelConsumed = onFocusReelConsumed,
        onUploadClick = onUploadClick,
        modifier = modifier.fillMaxSize()
    )
}
