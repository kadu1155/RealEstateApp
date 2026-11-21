package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.domain.model.Property
import com.example.domain.model.Reel
import com.example.ui.theme.SunsetOrange

private enum class PostManagementScope(val label: String) {
    MY_POSTS("My Posts"),
    ALL_POSTS("All Feed Posts")
}

private sealed class DeleteTarget {
    data class ReelTarget(val reel: Reel) : DeleteTarget()
    data class PropertyTarget(val property: Property) : DeleteTarget()
}

@Composable
fun ManagePostsSection(
    currentUserId: String,
    reels: List<Reel>,
    properties: List<Property>,
    onDeleteReel: (String) -> Unit,
    onDeleteProperty: (String) -> Unit,
    onPropertyClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var scope by remember { mutableStateOf(PostManagementScope.MY_POSTS) }
    var pendingDelete by remember { mutableStateOf<DeleteTarget?>(null) }

    val myReels = remember(reels, currentUserId) {
        reels.filter { it.userId == currentUserId }
    }
    val myProperties = remember(properties, currentUserId) {
        properties.filter { it.userId == currentUserId }
    }

    val displayedReels = if (scope == PostManagementScope.MY_POSTS) myReels else reels
    val displayedProperties = if (scope == PostManagementScope.MY_POSTS) myProperties else properties

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("section_manage_posts")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.VideoLibrary,
                contentDescription = null,
                tint = SunsetOrange,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "My Posts & Reels",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = scope == PostManagementScope.MY_POSTS,
                onClick = { scope = PostManagementScope.MY_POSTS },
                label = { Text("My Posts (${myReels.size + myProperties.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SunsetOrange,
                    selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("filter_my_posts")
            )
            FilterChip(
                selected = scope == PostManagementScope.ALL_POSTS,
                onClick = { scope = PostManagementScope.ALL_POSTS },
                label = { Text("All Posts (${reels.size + properties.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SunsetOrange,
                    selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("filter_all_posts")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Reels Sub-section
        Text(
            text = "Reels (${displayedReels.size})",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (displayedReels.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (scope == PostManagementScope.MY_POSTS) {
                            "No reels posted under this account yet."
                        } else {
                            "No reels available in the feed."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (scope == PostManagementScope.MY_POSTS && reels.isNotEmpty()) {
                        TextButton(
                            onClick = { scope = PostManagementScope.ALL_POSTS },
                            modifier = Modifier.testTag("btn_switch_to_all_posts")
                        ) {
                            Text("View All Feed Reels (${reels.size})", color = SunsetOrange)
                        }
                    }
                }
            }
        } else {
            displayedReels.forEach { reel ->
                ManageReelRowCard(
                    reel = reel,
                    onDeleteClick = { pendingDelete = DeleteTarget.ReelTarget(reel) },
                    onCardClick = {
                        reel.propertyId?.let { onPropertyClick(it) }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Property Listings Sub-section
        Text(
            text = "Property Listings (${displayedProperties.size})",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (displayedProperties.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (scope == PostManagementScope.MY_POSTS) {
                        "No property listings under this account."
                    } else {
                        "No property listings available."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp)
                )
            }
        } else {
            displayedProperties.forEach { property ->
                ManagePropertyRowCard(
                    property = property,
                    onClick = { onPropertyClick(property.id) },
                    onDeleteClick = { pendingDelete = DeleteTarget.PropertyTarget(property) }
                )
            }
        }
    }

    pendingDelete?.let { target ->
        val (title, body) = when (target) {
            is DeleteTarget.ReelTarget -> {
                val label = target.reel.propertyTitle?.takeIf { it.isNotBlank() }
                    ?: target.reel.locality
                "Delete Reel?" to "Remove \"$label\" (${target.reel.mediaType.uppercase()}) permanently from the Reels feed?"
            }
            is DeleteTarget.PropertyTarget -> {
                "Delete Property Listing?" to "Remove \"${target.property.title}\" permanently from Explore listings?"
            }
        }

        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = { Text(body, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (target) {
                            is DeleteTarget.ReelTarget -> onDeleteReel(target.reel.id)
                            is DeleteTarget.PropertyTarget -> onDeleteProperty(target.property.id)
                        }
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("btn_confirm_delete_post")
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingDelete = null },
                    modifier = Modifier.testTag("btn_cancel_delete_post")
                ) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("dialog_confirm_delete_post")
        )
    }
}

@Composable
private fun ManageReelRowCard(
    reel: Reel,
    onDeleteClick: () -> Unit,
    onCardClick: () -> Unit
) {
    val context = LocalContext.current
    val isVideo = reel.mediaType.equals("video", ignoreCase = true)
    val thumbModel: Any = remember(reel.id, reel.thumbnailUrl, reel.videoUrl, isVideo) {
        when {
            reel.id == "reel_05_video" || reel.thumbnailUrl.contains("poster_baner_4bhk") ->
                R.drawable.poster_baner_4bhk
            reel.thumbnailUrl.isNotBlank() -> reel.thumbnailUrl
            !isVideo && reel.videoUrl.isNotBlank() -> reel.videoUrl
            else -> R.drawable.poster_baner_4bhk
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onCardClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("card_manage_reel_${reel.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 78.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.08f))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(thumbModel)
                        .crossfade(true)
                        .build(),
                    contentDescription = reel.propertyTitle ?: reel.caption,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (isVideo) Icons.Default.PlayCircleOutline else Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isVideo) "Video" else "Photo",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reel.propertyTitle?.takeIf { it.isNotBlank() } ?: reel.caption,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${reel.locality}, ${reel.city}" +
                        (reel.propertyPriceDisplay?.let { " • $it" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = SunsetOrange,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = reel.caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${reel.likesCount} likes • by ${reel.creatorName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("btn_delete_reel_${reel.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete reel ${reel.propertyTitle ?: reel.locality}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ManagePropertyRowCard(
    property: Property,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val thumbModel: Any = remember(property.id, property.imageUrls) {
        val firstImg = property.imageUrls.firstOrNull().orEmpty()
        when {
            firstImg.contains("poster_baner_4bhk") -> R.drawable.poster_baner_4bhk
            firstImg.isNotBlank() -> firstImg
            else -> R.drawable.poster_baner_4bhk
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("card_manage_property_${property.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.08f))
            ) {
                if (property.imageUrls.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(thumbModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = property.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Apartment,
                        contentDescription = null,
                        tint = SunsetOrange,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = property.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${property.locality}, ${property.city} • ${property.priceDisplay}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SunsetOrange,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${property.category.shortCode} • ${property.sellerName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("btn_delete_property_${property.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete property ${property.title}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
