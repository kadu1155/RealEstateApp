package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.Property
import com.example.domain.model.PropertyCategory
import com.example.domain.model.VerificationBadge
import com.example.ui.components.VerifiedBadgeChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ReraBlue
import com.example.ui.theme.SunsetAmber
import com.example.ui.theme.SunsetOrange

val CITY_LOCALITIES_MAP: Map<String, List<String>> = mapOf(
    "Pune" to listOf("All Pune", "Baner", "Wakad", "Kharadi", "Hinjawadi", "Kothrud", "Kalyani Nagar", "Balewadi", "Viman Nagar"),
    "Mumbai" to listOf("All Mumbai", "Worli", "Bandra West", "Andheri West", "Juhu", "Powai", "Malad", "Thane", "Navi Mumbai"),
    "Bangalore" to listOf("All Bangalore", "Whitefield", "Indiranagar", "Koramangala", "HSR Layout", "Electronic City", "Bellandur"),
    "Hyderabad" to listOf("All Hyderabad", "Gachibowli", "Hitec City", "Madhapur", "Jubilee Hills", "Banjara Hills", "Kondapur"),
    "Delhi-NCR" to listOf("All Delhi-NCR", "Gurgaon Golf Course", "Cyber City", "Noida Sector 62", "Greater Kailash", "Dwarka")
)

val AVAILABLE_CITIES = listOf("Pune", "Mumbai", "Bangalore", "Hyderabad", "Delhi-NCR")

@Composable
fun PropertySearchScreen(
    properties: List<Property>,
    savedPropertyIds: Set<String>,
    currentCity: String = "Pune",
    onCityChange: (String) -> Unit = {},
    onPropertyClick: (String) -> Unit,
    onSaveToggle: (String) -> Unit,
    onReelClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var activeCity by remember(currentCity) { mutableStateOf(currentCity) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<PropertyCategory?>(null) }
    var selectedLocality by remember { mutableStateOf<String?>(null) }
    var verifiedOnly by remember { mutableStateOf(false) }
    var showSeoPreview by remember { mutableStateOf(false) }

    // Sync activeCity and reset localities whenever currentCity changes
    LaunchedEffect(currentCity) {
        activeCity = currentCity
        selectedLocality = null
    }

    LaunchedEffect(activeCity) {
        selectedLocality = null
    }

    // Dynamic localities list based on active city
    val availableLocalities = remember(activeCity) {
        CITY_LOCALITIES_MAP[activeCity] ?: listOf("All $activeCity")
    }

    // Filter properties strictly matching active city, locality, search query, category, and verification
    val filteredList = properties.filter { p ->
        val matchesCity = activeCity.equals("All", ignoreCase = true) || p.city.equals(activeCity, ignoreCase = true)
        val matchesLocality = selectedLocality == null ||
                selectedLocality!!.startsWith("All") ||
                p.locality.equals(selectedLocality, ignoreCase = true)
        val matchesCategory = selectedCategory == null || p.category == selectedCategory
        val matchesVerified = !verifiedOnly || p.sellerBadge != VerificationBadge.NONE

        val query = searchQuery.trim().lowercase()
        val matchesQuery = query.isBlank() ||
                p.title.lowercase().contains(query) ||
                p.locality.lowercase().contains(query) ||
                p.city.lowercase().contains(query) ||
                (p.reraId != null && p.reraId.lowercase().contains(query)) ||
                (p.bhk != null && "${p.bhk} bhk".contains(query)) ||
                (p.bhk != null && "${p.bhk}bhk".contains(query)) ||
                p.landmark.lowercase().contains(query) ||
                p.sellerName.lowercase().contains(query) ||
                p.priceDisplay.lowercase().contains(query)

        matchesCity && matchesLocality && matchesCategory && matchesVerified && matchesQuery
    }

    // Dynamic SEO Metadata Elements
    val effectiveLocality = if (!selectedLocality.isNullOrBlank() && selectedLocality != "All") selectedLocality!! else activeCity
    val categoryName = selectedCategory?.title ?: "Flats & Properties"
    val seoTitle = "${if (searchQuery.isNotBlank()) "$searchQuery - " else ""}$categoryName in $effectiveLocality | PropReel Video Tours"
    val seoSlug = "https://propreel.in/${activeCity.lowercase()}/${effectiveLocality.lowercase().replace(" ", "-")}"
    val seoMetaDescription = "Explore ${filteredList.size}+ verified vertical video walk-throughs & direct listings in $effectiveLocality. Real-time pricing, floor plans, MahaRERA approvals, and 1-tap WhatsApp broker connect."

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. City Switcher Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location",
                tint = SunsetOrange,
                modifier = Modifier.size(16.dp)
            )

            AVAILABLE_CITIES.forEach { city ->
                val isSelectedCity = activeCity.equals(city, ignoreCase = true)
                Surface(
                    onClick = {
                        activeCity = city
                        selectedLocality = null // Reset locality filter on city switch
                        onCityChange(city)
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelectedCity) SunsetOrange else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        1.dp,
                        if (isSelectedCity) SunsetOrange else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.testTag("city_filter_$city")
                ) {
                    Text(
                        text = city,
                        color = if (isSelectedCity) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelectedCity) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // 2. Search Input Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by locality, BHK, RERA ID, or landmark in $activeCity...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = SunsetOrange
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SunsetOrange,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("property_search_input")
        )

        // 3. Trending SEO Search Chips
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = "Trending",
                tint = SunsetAmber,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "SEO Trends:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val trendingSeoTags = listOf(
                "Ready to Move",
                "Luxury 3 BHK",
                "Under ₹1 Cr",
                "MahaRERA Approved",
                "Direct Owner"
            )

            trendingSeoTags.forEach { tag ->
                Surface(
                    onClick = { searchQuery = if (searchQuery == tag) "" else tag },
                    shape = RoundedCornerShape(12.dp),
                    color = if (searchQuery == tag) SunsetAmber.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (searchQuery == tag) SunsetAmber else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.testTag("seo_chip_$tag")
                ) {
                    Text(
                        text = tag,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (searchQuery == tag) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 4. Dynamic Localities Filter Chips (Specific to active city)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 2.dp)
        ) {
            Text(
                text = "📍 Localities in $activeCity:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = SunsetOrange
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = verifiedOnly,
                onClick = { verifiedOnly = !verifiedOnly },
                label = { Text("Verified Only") },
                leadingIcon = {
                    if (verifiedOnly) Icon(Icons.Default.Check, contentDescription = null)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldPrimary,
                    selectedLabelColor = Color.White
                )
            )

            availableLocalities.forEach { loc ->
                val isSelected = (selectedLocality == null && loc.startsWith("All")) || (selectedLocality == loc)
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedLocality = if (loc.startsWith("All")) null else loc },
                    label = { Text(loc) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SunsetOrange,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("locality_chip_$loc")
                )
            }
        }

        // 5. Category Filter Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("All Categories") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SunsetOrange,
                    selectedLabelColor = Color.White
                )
            )
            PropertyCategory.values().forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                    label = { Text(cat.shortCode) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SunsetOrange,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // 6. Header with Results Count & Interactive SEO Snippet Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Showing ${filteredList.size} properties in $activeCity",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                onClick = { showSeoPreview = !showSeoPreview },
                shape = RoundedCornerShape(12.dp),
                color = if (showSeoPreview) ReraBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, if (showSeoPreview) ReraBlue else Color.Transparent),
                modifier = Modifier.testTag("btn_toggle_seo_preview")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = ReraBlue,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showSeoPreview) "Hide SEO SERP" else "View Google SEO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ReraBlue
                    )
                    Icon(
                        imageVector = if (showSeoPreview) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = ReraBlue,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // 7. Expandable Google SERP & SEO Snippet Preview Card
        AnimatedVisibility(
            visible = showSeoPreview,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ReraBlue.copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("seo_serp_preview_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Google Breadcrumb
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = SunsetOrange,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.size(14.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("P", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = seoSlug,
                            color = Color(0xFF475569),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Google Clickable Title in Royal Blue
                    Text(
                        text = seoTitle,
                        color = Color(0xFF1D4ED8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Stars & Rating Rich Snippet
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "4.9 · 1,840+ reviews · MahaRERA Verified",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF059669)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Meta Description Snippet
                    Text(
                        text = seoMetaDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // SEO Keywords Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        listOf("#$activeCity", "#$effectiveLocality", "#MahaRERA", "#VideoReels", "#ZeroBrokerage").forEach { keyword ->
                            Surface(
                                color = ReraBlue.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = keyword,
                                    color = ReraBlue,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 8. Properties List / Empty State
        if (filteredList.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No properties found in $effectiveLocality",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Try clearing filters, selecting 'All' localities, or exploring another city.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("property_results_list")
            ) {
                items(filteredList, key = { it.id }) { property ->
                    PropertyCard(
                        property = property,
                        isSaved = savedPropertyIds.contains(property.id),
                        onClick = { onPropertyClick(property.id) },
                        onSaveToggle = { onSaveToggle(property.id) },
                        onReelClick = { property.linkedReelId?.let { onReelClick?.invoke(it) } }
                    )
                }
            }
        }
    }
}

@Composable
fun PropertyCard(
    property: Property,
    isSaved: Boolean,
    onClick: () -> Unit,
    onSaveToggle: () -> Unit,
    onReelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("property_card_${property.id}")
    ) {
        Column {
            // Image Section with Badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                AsyncImage(
                    model = property.imageUrls.firstOrNull(),
                    contentDescription = property.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Left: Seller Badge & Category
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    VerifiedBadgeChip(badge = property.sellerBadge)
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = property.category.shortCode,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Top Right: Save Bookmark Icon
                IconButton(
                    onClick = onSaveToggle,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .testTag("btn_save_property_${property.id}")
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Save",
                        tint = if (isSaved) SunsetAmber else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Bottom Left: Linked Reel Badge (if exists)
                if (property.linkedReelId != null) {
                    Surface(
                        onClick = onReelClick,
                        color = SunsetOrange,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .testTag("btn_view_linked_reel_${property.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleOutline,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Watch Reel Tour",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Property Details Content
            Column(modifier = Modifier.padding(14.dp)) {
                // Price & Area
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = property.priceDisplay,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = SunsetOrange
                    )
                    Text(
                        text = "${property.carpetAreaSqft.toInt()} sq.ft",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Title
                Text(
                    text = property.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Location & BHK
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = SunsetAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${property.locality}, ${property.city}${if (property.bhk != null) " • ${property.bhk} BHK" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Amenities Tags
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    property.amenities.take(3).forEach { amenity ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = amenity,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
