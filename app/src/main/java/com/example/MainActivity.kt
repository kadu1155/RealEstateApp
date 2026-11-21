package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.PropReelRepository
import com.example.domain.model.CoinOperationResult
import com.example.domain.model.CoinRewardProcessResult
import com.example.domain.model.EnquiryIntent
import com.example.domain.model.Property
import com.example.domain.model.ReferralApplyResult
import com.example.domain.model.UserRole
import com.example.ui.components.PropReelBottomBar
import com.example.ui.components.PropReelTopBar
import com.example.ui.navigation.BottomNavItem
import com.example.ui.navigation.Screen
import com.example.ui.screens.AuthModal
import com.example.ui.screens.CreatePostModal
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LeadInboxScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.PropertyDetailScreen
import com.example.ui.screens.PropertySearchScreen
import com.example.ui.screens.UploadReelModal
import com.example.ui.theme.PropReelTheme
import com.example.ui.theme.SunsetOrange
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: PropReelRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = PropReelRepository(applicationContext)
        enableEdgeToEdge()

        setContent {
            PropReelTheme {
                PropReelApp(repository = repository)
            }
        }
    }
}

@Composable
fun PropReelApp(repository: PropReelRepository) {
    var currentRoute by remember { mutableStateOf(BottomNavItem.REELS.route) }
    var selectedPropertyId by remember { mutableStateOf<String?>(null) }
    var showCreatePostModal by remember { mutableStateOf(false) }
    var showUploadReelModal by remember { mutableStateOf(false) }
    var showCityDialog by remember { mutableStateOf(false) }
    var showAuthModal by remember { mutableStateOf(false) }
    var pendingFocusReelId by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Repository Flows
    val reels by repository.getReelsStream().collectAsStateWithLifecycle(initialValue = emptyList())
    val properties by repository.getPropertiesStream().collectAsStateWithLifecycle(initialValue = emptyList())
    val savedProperties by repository.getSavedPropertiesStream().collectAsStateWithLifecycle(initialValue = emptyList())
    val leads by repository.getLeadsStream().collectAsStateWithLifecycle(initialValue = emptyList())
    val currentUser by repository.currentUser.collectAsStateWithLifecycle()
    val currentCity by repository.selectedCity.collectAsStateWithLifecycle()
    val referralStats by repository.getReferralStatsStream(currentUser.id).collectAsStateWithLifecycle(initialValue = null)
    val referrals by repository.getReferralsStream(currentUser.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val coinWalletSummary by repository.getCoinWalletSummaryStream(currentUser.id).collectAsStateWithLifecycle(initialValue = null)
    val coinTransactions by repository.getCoinTransactionsStream(currentUser.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val redemptionRules by repository.redemptionRules.collectAsStateWithLifecycle()

    val savedPropertyIds = remember(savedProperties) {
        savedProperties.map { it.id }.toSet()
    }

    val isReelsFeedActive = currentRoute == BottomNavItem.REELS.route &&
        selectedPropertyId == null &&
        !showUploadReelModal &&
        !showCreatePostModal &&
        !showAuthModal &&
        !showCityDialog

    // Handle System Back Button
    BackHandler(enabled = selectedPropertyId != null || currentRoute != BottomNavItem.REELS.route) {
        if (selectedPropertyId != null) {
            selectedPropertyId = null
        } else {
            currentRoute = BottomNavItem.REELS.route
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (selectedPropertyId == null && currentRoute != BottomNavItem.REELS.route) {
                PropReelTopBar(
                    currentCity = currentCity,
                    onCityClick = { showCityDialog = true }
                )
            }
        },
        bottomBar = {
            if (selectedPropertyId == null) {
                PropReelBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route == BottomNavItem.POST.route) {
                            showCreatePostModal = true
                        } else {
                            currentRoute = route
                            selectedPropertyId = null
                        }
                    }
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = if (selectedPropertyId != null || currentRoute == BottomNavItem.REELS.route) 0.dp else innerPadding.calculateTopPadding(),
                    bottom = if (selectedPropertyId != null) 0.dp else innerPadding.calculateBottomPadding()
                )
        ) {
            if (selectedPropertyId != null) {
                val detailProp = properties.find { it.id == selectedPropertyId }
                PropertyDetailScreen(
                    property = detailProp,
                    isSaved = savedPropertyIds.contains(selectedPropertyId!!),
                    onBackClick = { selectedPropertyId = null },
                    onSaveToggle = {
                        coroutineScope.launch {
                            val saved = repository.toggleSaveProperty(selectedPropertyId!!)
                            snackbarHostState.showSnackbar(if (saved) "Property bookmarked!" else "Removed from saved.")
                        }
                    },
                    onSubmitEnquiry = { name, phone, msg, intent ->
                        coroutineScope.launch {
                            detailProp?.let { prop ->
                                repository.submitEnquiry(prop, name, phone, msg, intent)
                                snackbarHostState.showSnackbar("Enquiry sent to ${prop.sellerName}! Lead created in CRM.")
                            }
                        }
                    }
                )
            } else {
                when (currentRoute) {
                    BottomNavItem.REELS.route -> {
                        HomeScreen(
                            reels = reels,
                            isHostActive = isReelsFeedActive,
                            focusReelId = pendingFocusReelId,
                            onFocusReelConsumed = { pendingFocusReelId = null },
                            onLikeClick = { reelId ->
                                coroutineScope.launch { repository.toggleLikeReel(reelId) }
                            },
                            onSaveClick = { reelId ->
                                coroutineScope.launch {
                                    repository.toggleSaveReel(reelId)
                                    snackbarHostState.showSnackbar("Reel bookmark updated")
                                }
                            },
                            onViewPropertyClick = { propId ->
                                selectedPropertyId = propId
                            },
                            onEnquireClick = { propId ->
                                selectedPropertyId = propId
                            },
                            onUploadClick = {
                                showUploadReelModal = true
                            }
                        )
                    }

                    BottomNavItem.PROPERTIES.route -> {
                        PropertySearchScreen(
                            properties = properties,
                            savedPropertyIds = savedPropertyIds,
                            currentCity = currentCity,
                            onCityChange = { newCity ->
                                repository.setCity(newCity)
                            },
                            onPropertyClick = { propId ->
                                selectedPropertyId = propId
                            },
                            onSaveToggle = { propId ->
                                coroutineScope.launch {
                                    repository.toggleSaveProperty(propId)
                                }
                            },
                            onReelClick = { reelId ->
                                pendingFocusReelId = reelId
                                currentRoute = BottomNavItem.REELS.route
                            }
                        )
                    }

                    BottomNavItem.LEADS.route -> {
                        LeadInboxScreen(
                            leads = leads,
                            onUpdateStage = { leadId, newStage, notes ->
                                coroutineScope.launch {
                                    repository.updateLeadStage(leadId, newStage, notes)
                                    snackbarHostState.showSnackbar("Lead updated to ${newStage.label}")
                                }
                            }
                        )
                    }

                    BottomNavItem.PROFILE.route -> {
                        ProfileScreen(
                            user = currentUser,
                            savedProperties = savedProperties,
                            leadsCount = leads.size,
                            referralStats = referralStats,
                            referrals = referrals,
                            coinWalletSummary = coinWalletSummary,
                            coinTransactions = coinTransactions,
                            redemptionRules = redemptionRules,
                            onRedeemCoins = { ruleId, idempotencyKey ->
                                coroutineScope.launch {
                                    when (val result = repository.redeemCoins(
                                        userId = currentUser.id,
                                        ruleId = ruleId,
                                        idempotencyKey = idempotencyKey
                                    )) {
                                        is CoinOperationResult.Success -> {
                                            snackbarHostState.showSnackbar(result.message)
                                        }
                                        is CoinOperationResult.DuplicateRejected -> {
                                            snackbarHostState.showSnackbar(result.reason)
                                        }
                                        is CoinOperationResult.Error -> {
                                            snackbarHostState.showSnackbar(result.message)
                                        }
                                    }
                                }
                            },
                            onProcessQualifiedReward = { referralId ->
                                coroutineScope.launch {
                                    when (val res = repository.processReferralReward(referralId)) {
                                        is CoinRewardProcessResult.Success -> {
                                            snackbarHostState.showSnackbar("Credited +50 APP_COINS (and +25 welcome bonus to invitee)!")
                                        }
                                        is CoinRewardProcessResult.AlreadyProcessed -> {
                                            snackbarHostState.showSnackbar(res.reason)
                                        }
                                        is CoinRewardProcessResult.Rejected -> {
                                            snackbarHostState.showSnackbar(res.reason)
                                        }
                                    }
                                }
                            },
                            onApplyReferralCode = { inputCode, source ->
                                coroutineScope.launch {
                                    val result = repository.applyReferralCode(
                                        referredUserId = currentUser.id,
                                        code = inputCode,
                                        source = source
                                    )
                                    when (result) {
                                        is ReferralApplyResult.Success -> {
                                            snackbarHostState.showSnackbar("Referral code applied! Attributed via ${source.displayName}")
                                        }
                                        is ReferralApplyResult.Error -> {
                                            snackbarHostState.showSnackbar(result.message)
                                        }
                                    }
                                }
                            },
                            onSwitchRole = { role ->
                                repository.switchRole(role)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Role switched to ${role.displayName}")
                                }
                            },
                            onPropertyClick = { propId ->
                                selectedPropertyId = propId
                            },
                            onPostClick = {
                                showCreatePostModal = true
                            },
                            onAuthClick = {
                                showAuthModal = true
                            },
                            userReels = reels,
                            userProperties = properties,
                            onDeleteReel = { reelId ->
                                coroutineScope.launch {
                                    repository.deleteReel(reelId)
                                    snackbarHostState.showSnackbar("Reel post deleted permanently")
                                }
                            },
                            onDeleteProperty = { propId ->
                                coroutineScope.launch {
                                    repository.deleteProperty(propId)
                                    snackbarHostState.showSnackbar("Property listing deleted permanently")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal: Create Property (Post Page - Exclusively for property listings)
    if (showCreatePostModal) {
        CreatePostModal(
            currentUser = currentUser,
            onDismiss = { showCreatePostModal = false },
            onPropertyCreated = { newProp ->
                coroutineScope.launch {
                    repository.createProperty(newProp)
                    showCreatePostModal = false
                    snackbarHostState.showSnackbar("Property published for free in ${newProp.locality}!")
                    currentRoute = BottomNavItem.PROPERTIES.route
                }
            }
        )
    }

    // Modal: Upload Real Estate Reel (Triggered by Upper-Right Plus Button)
    if (showUploadReelModal) {
        UploadReelModal(
            currentUser = currentUser,
            properties = properties,
            onDismiss = { showUploadReelModal = false },
            onReelCreated = { newReel ->
                coroutineScope.launch {
                    repository.createReel(newReel)
                    showUploadReelModal = false
                    pendingFocusReelId = newReel.id
                    currentRoute = BottomNavItem.REELS.route
                    snackbarHostState.showSnackbar("Reel published successfully in ${newReel.locality}!")
                }
            }
        )
    }

    // Modal: Mobile OTP Authentication & Role Setup
    if (showAuthModal) {
        AuthModal(
            onDismiss = { showAuthModal = false },
            onSendOtp = { mobile, onSent ->
                coroutineScope.launch {
                    val (success, isSandbox) = repository.sendOtp(mobile)
                    if (success) {
                        snackbarHostState.showSnackbar("OTP sent to $mobile")
                        onSent(isSandbox)
                    } else {
                        snackbarHostState.showSnackbar("Failed to send OTP. Please check number.")
                    }
                }
            },
            onVerifyOtp = { mobile, otp, onVerified ->
                coroutineScope.launch {
                    val verified = repository.verifyOtp(mobile, otp)
                    if (verified) {
                        snackbarHostState.showSnackbar("OTP verified successfully!")
                        onVerified()
                    } else {
                        snackbarHostState.showSnackbar("Invalid OTP code. Please retry.")
                    }
                }
            },
            onRoleSelected = { role, reraNum, agencyName ->
                repository.updateUserRoleWithDetails(role, reraNum, agencyName)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Profile updated as ${role.displayName}")
                }
            }
        )
    }

    // Modal: City Selection
    if (showCityDialog) {
        val cities = listOf("Pune", "Mumbai", "Bangalore", "Delhi-NCR", "Hyderabad")
        AlertDialog(
            onDismissRequest = { showCityDialog = false },
            title = { Text("Select Discovery City") },
            text = {
                Column {
                    for (city in cities) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = currentCity == city,
                                onClick = {
                                    repository.setCity(city)
                                    showCityDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = SunsetOrange)
                            )
                            Text(text = city, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCityDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
