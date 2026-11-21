package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    REELS("reels_feed", "Reels", Icons.Default.Home, "nav_item_reels"),
    PROPERTIES("properties_search", "Explore", Icons.Default.Search, "nav_item_properties"),
    POST("create_post", "Post", Icons.Default.AddCircle, "nav_item_post"),
    LEADS("lead_inbox", "CRM", Icons.Default.Assignment, "nav_item_leads"),
    PROFILE("profile_hub", "Profile", Icons.Default.Person, "nav_item_profile")
}

sealed class Screen(val route: String) {
    object Reels : Screen("reels_feed")
    object Properties : Screen("properties_search")
    object PropertyDetail : Screen("property_detail/{propertyId}") {
        fun createRoute(propertyId: String) = "property_detail/$propertyId"
    }
    object LeadInbox : Screen("lead_inbox")
    object Profile : Screen("profile_hub")
    object CreateProperty : Screen("create_property")
    object CreateReel : Screen("create_reel")
    object SavedProperties : Screen("saved_properties")
}
