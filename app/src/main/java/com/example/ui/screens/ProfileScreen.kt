package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AttributionSource
import com.example.domain.model.CoinTransaction
import com.example.domain.model.CoinWalletSummary
import com.example.domain.model.Property
import com.example.domain.model.RedemptionRule
import com.example.domain.model.Reel
import com.example.domain.model.Referral
import com.example.domain.model.ReferralStats
import com.example.domain.model.User
import com.example.domain.model.UserRole
import com.example.ui.components.CoinWalletSection
import com.example.ui.components.ManagePostsSection
import com.example.ui.components.ReferralSection
import com.example.ui.components.VerifiedBadgeChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SunsetAmber
import com.example.ui.theme.SunsetOrange

@Composable
fun ProfileScreen(
    user: User,
    savedProperties: List<Property>,
    leadsCount: Int,
    onSwitchRole: (UserRole) -> Unit,
    onPropertyClick: (String) -> Unit,
    onPostClick: () -> Unit,
    onAuthClick: () -> Unit,
    modifier: Modifier = Modifier,
    referralStats: ReferralStats? = null,
    referrals: List<Referral> = emptyList(),
    onApplyReferralCode: ((code: String, source: AttributionSource) -> Unit)? = null,
    coinWalletSummary: CoinWalletSummary? = null,
    coinTransactions: List<CoinTransaction> = emptyList(),
    redemptionRules: List<RedemptionRule> = emptyList(),
    onRedeemCoins: ((ruleId: String, idempotencyKey: String) -> Unit)? = null,
    onProcessQualifiedReward: ((referralId: String) -> Unit)? = null,
    userReels: List<Reel> = emptyList(),
    userProperties: List<Property> = emptyList(),
    onDeleteReel: ((String) -> Unit)? = null,
    onDeleteProperty: ((String) -> Unit)? = null
) {
    var showPromotionModal by remember { mutableStateOf(false) }

    if (showPromotionModal) {
        PromotionModal(
            onDismiss = { showPromotionModal = false },
            onBoostActivated = { _, _ -> },
            onVerificationSubmitted = { _, _ -> }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // User Profile Card
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(SunsetOrange, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = user.fullName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${user.mobile} • ${user.city}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        VerifiedBadgeChip(badge = user.verificationBadge)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Role Switcher Panel
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = SunsetOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Current Active Role: ${user.role.displayName}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            UserRole.values().take(4).forEach { role ->
                                val isSelected = user.role == role
                                OutlinedButton(
                                    onClick = { onSwitchRole(role) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) SunsetOrange else Color.Transparent,
                                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.weight(1f).testTag("btn_switch_role_${role.name}")
                                ) {
                                    Text(
                                        text = role.displayName.split(" ").first(),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats Overview Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            StatCard(title = "Saved Properties", count = "${savedProperties.size}", modifier = Modifier.weight(1f))
            StatCard(title = "CRM Leads", count = "$leadsCount", modifier = Modifier.weight(1f))
            StatCard(title = "My City", count = user.city, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Actions
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Button(
                onClick = onPostClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SunsetOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_profile_post_property")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("List New Property (Free)", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { showPromotionModal = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SunsetAmber),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_open_promotion_modal")
            ) {
                Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("🚀 Boost Listings & MahaRERA Verify", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onAuthClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_profile_auth_modal")
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign In with Mobile OTP / Switch Account")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Manage My Posts & Reels Section (Delete Reels / Listings)
        if (onDeleteReel != null || onDeleteProperty != null) {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                ManagePostsSection(
                    currentUserId = user.id,
                    reels = userReels,
                    properties = userProperties,
                    onDeleteReel = { reelId -> onDeleteReel?.invoke(reelId) },
                    onDeleteProperty = { propId -> onDeleteProperty?.invoke(propId) },
                    onPropertyClick = onPropertyClick
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Phase 4: Coin Economy Wallet & Ledger Section
        if (coinWalletSummary != null) {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                CoinWalletSection(
                    walletSummary = coinWalletSummary,
                    transactions = coinTransactions,
                    redemptionRules = redemptionRules,
                    onRedeemRule = { ruleId, key ->
                        onRedeemCoins?.invoke(ruleId, key)
                    },
                    onProcessQualifiedReward = { referralId ->
                        onProcessQualifiedReward?.invoke(referralId)
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Phase 3: Refer & Earn Section
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            ReferralSection(
                referralStats = referralStats,
                referrals = referrals,
                onApplyReferralCode = { inputCode, source ->
                    onApplyReferralCode?.invoke(inputCode, source)
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Saved Properties Section
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = SunsetAmber,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Saved Properties (${savedProperties.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (savedProperties.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No saved properties yet. Tap the bookmark icon on any reel or property to save.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                savedProperties.forEach { property ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        onClick = { onPropertyClick(property.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = property.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${property.locality} • ${property.priceDisplay}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SunsetOrange
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
fun StatCard(
    title: String,
    count: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = count,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = SunsetOrange
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
