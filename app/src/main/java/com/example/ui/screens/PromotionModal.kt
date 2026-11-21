package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ReraBlue
import com.example.ui.theme.SunsetAmber
import com.example.ui.theme.SunsetOrange

@Composable
fun PromotionModal(
    onDismiss: () -> Unit,
    onBoostActivated: (packageType: String, amount: Double) -> Unit,
    onVerificationSubmitted: (reraId: String, withPhysicalInspection: Boolean) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Boost Reel, 1: RERA Verification
    var selectedPackage by remember { mutableStateOf("FEATURED_REEL") }
    var reraNumberInput by remember { mutableStateOf("P52100028912") }
    var requestPhysicalCrew by remember { mutableStateOf(true) }
    var showSuccessMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxSize(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Header Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(SunsetOrange.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = null,
                                tint = SunsetOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Growth & Monetization",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Boost visibility & establish buyer trust",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_close_promotion_modal")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tabs: Boost Reel vs RERA Verification
                TabRow(
                    selectedTabIndex = selectedTab,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = SunsetOrange
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("🚀 Boost Listing", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        modifier = Modifier.testTag("tab_boost_listing")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("🛡️ MahaRERA Verify", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        modifier = Modifier.testTag("tab_verify_listing")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (showSuccessMessage != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Success!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = showSuccessMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (selectedTab == 0) {
                    // TAB 0: BOOST LISTINGS & REELS
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Select a Promotion Package",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Package 1: Featured Reel
                        PromotionPackageCard(
                            title = "Featured Reel Boost",
                            price = "₹999",
                            duration = "7 Days",
                            tag = "Most Popular",
                            features = listOf("3x vertical reel impressions", "Pune buyer algorithm priority", "Featured badge on video overlay"),
                            isSelected = selectedPackage == "FEATURED_REEL",
                            onClick = { selectedPackage = "FEATURED_REEL" },
                            tagModifier = Modifier.testTag("pkg_featured_reel")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Package 2: Locality Boost
                        PromotionPackageCard(
                            title = "Locality Micro-Market Dominance",
                            price = "₹1,999",
                            duration = "14 Days",
                            tag = "High ROI",
                            features = listOf("Pinned top in Baner / Wakad search", "Instant WhatsApp notification on search", "Direct callback routing"),
                            isSelected = selectedPackage == "LOCALITY_BOOST",
                            onClick = { selectedPackage = "LOCALITY_BOOST" },
                            tagModifier = Modifier.testTag("pkg_locality_boost")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Package 3: Developer Spotlight
                        PromotionPackageCard(
                            title = "Developer Spotlight & Elite Badging",
                            price = "₹4,999",
                            duration = "30 Days",
                            tag = "Maximum Exposure",
                            features = listOf("Full city-wide spotlight placement", "PropReel Inspected badge inclusion", "Zero lead caps"),
                            isSelected = selectedPackage == "PREMIUM_SPOTLIGHT",
                            onClick = { selectedPackage = "PREMIUM_SPOTLIGHT" },
                            tagModifier = Modifier.testTag("pkg_developer_spotlight")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val amount = when (selectedPackage) {
                                "FEATURED_REEL" -> 999.0
                                "LOCALITY_BOOST" -> 1999.0
                                else -> 4999.0
                            }
                            onBoostActivated(selectedPackage, amount)
                            showSuccessMessage = "Promotion boost activated! Your reel is now highlighted with priority delivery to Pune buyers."
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SunsetOrange),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_confirm_boost")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Activate Boost Now (${if (selectedPackage == "FEATURED_REEL") "₹999" else if (selectedPackage == "LOCALITY_BOOST") "₹1,999" else "₹4,999"})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // TAB 1: MAHARERA VERIFICATION & ONSITE INSPECTION
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Get Verified to Increase Inquiries by 4x",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Properties verified with MahaRERA receive verified trust badges and 4x more site visit requests.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = reraNumberInput,
                            onValueChange = { reraNumberInput = it },
                            label = { Text("MahaRERA Registration Number") },
                            placeholder = { Text("e.g. P52100028912") },
                            leadingIcon = {
                                Icon(Icons.Default.Security, contentDescription = null, tint = ReraBlue)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_rera_number")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Checkbox(
                                    checked = requestPhysicalCrew,
                                    onCheckedChange = { requestPhysicalCrew = it },
                                    colors = CheckboxDefaults.colors(checkedColor = SunsetOrange)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Request PropReel On-Site Video Crew",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Our verified videographer visits your property in Pune, films 4K vertical drone/reels & inspects the site.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            onVerificationSubmitted(reraNumberInput, requestPhysicalCrew)
                            showSuccessMessage = "MahaRERA verification details submitted! Badge will be granted upon registry check."
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ReraBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_submit_verification")
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Submit for MahaRERA Badge", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PromotionPackageCard(
    title: String,
    price: String,
    duration: String,
    tag: String,
    features: List<String>,
    isSelected: Boolean,
    onClick: () -> Unit,
    tagModifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) SunsetOrange else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SunsetOrange.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface
        ),
        modifier = tagModifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(text = "$price for $duration", color = SunsetOrange, fontWeight = FontWeight.Black, fontSize = 15.sp)
                }

                Surface(
                    color = SunsetAmber.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = tag,
                        color = Color(0xFFB45309),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            features.forEach { feature ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = feature, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
