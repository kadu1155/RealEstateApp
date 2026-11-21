package com.example.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AttributionSource
import com.example.domain.model.Referral
import com.example.domain.model.ReferralStats
import com.example.domain.model.ReferralStatus
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SunsetAmber
import com.example.ui.theme.SunsetOrange
import java.util.Date

@Composable
fun ReferralSection(
    referralStats: ReferralStats?,
    referrals: List<Referral>,
    onApplyReferralCode: (code: String, source: AttributionSource) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showEnterCodeDialog by remember { mutableStateOf(false) }
    var showDemoDisclaimerDialog by remember { mutableStateOf(false) }

    val code = referralStats?.referralCode ?: "PROPREEL"
    val link = referralStats?.referralLink ?: "https://propreel.in/refer/$code"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SunsetOrange.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("section_refer_and_earn")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row with Gift Icon and Demo Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(SunsetOrange.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = "Refer & Earn",
                        tint = SunsetOrange,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Refer & Earn",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Invite agents, builders & owners",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Demo / Device-Local Mode Badge
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .clickable { showDemoDisclaimerDialog = true }
                        .testTag("chip_referral_demo_mode")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SunsetAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Local Mode",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Referral Code Display Box
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SunsetOrange.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "YOUR REFERRAL CODE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SunsetOrange,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = code,
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("txt_user_referral_code")
                        )

                        Button(
                            onClick = {
                                ReferralShareHelper.copyToClipboard(context, code, "Referral Code")
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SunsetOrange),
                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                            modifier = Modifier.testTag("btn_copy_referral_code")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Code", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Link row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = link,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("txt_user_referral_link")
                        )
                        TextButton(
                            onClick = {
                                ReferralShareHelper.copyToClipboard(context, link, "Referral Link")
                            },
                            modifier = Modifier.testTag("btn_copy_referral_link")
                        ) {
                            Text("Copy Link", fontSize = 11.sp, color = SunsetOrange, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: WhatsApp Share, Native Share, and Enter Code
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // WhatsApp Button
                Button(
                    onClick = {
                        ReferralShareHelper.shareViaWhatsApp(context, code, link)
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_share_whatsapp")
                ) {
                    Text("💬 WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Native Share Button
                Button(
                    onClick = {
                        ReferralShareHelper.shareNative(context, code, link)
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SunsetOrange),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_share_native")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Enter Code Button
                OutlinedButton(
                    onClick = { showEnterCodeDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("btn_open_enter_referral_dialog")
                ) {
                    Text("Enter Code", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Summary Stats Counters Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ReferralStatPill(
                    title = "Total Invites",
                    count = "${referralStats?.totalReferrals ?: 0}",
                    modifier = Modifier.weight(1f),
                    testTag = "txt_referrals_total_count"
                )
                ReferralStatPill(
                    title = "Pending",
                    count = "${referralStats?.pendingCount ?: 0}",
                    modifier = Modifier.weight(1f),
                    testTag = "txt_referrals_pending_count"
                )
                ReferralStatPill(
                    title = "Qualified",
                    count = "${referralStats?.qualifiedCount ?: 0}",
                    modifier = Modifier.weight(1f),
                    testTag = "txt_referrals_qualified_count"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Phase 4 Reward Teaser
            Surface(
                color = SunsetAmber.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SunsetAmber.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = SunsetAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Coin Rewards Active: Earn 50 APP_COINS per qualified referral, and your invitee receives 25 APP_COINS!",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Referral History List
            Text(
                text = "Referral Activity (${referrals.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (referrals.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No referral activity yet. Share your code to start tracking invites!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    referrals.forEach { referral ->
                        ReferralHistoryCard(referral = referral)
                    }
                }
            }
        }
    }

    // Enter Referral Code Dialog
    if (showEnterCodeDialog) {
        EnterReferralCodeDialog(
            onDismiss = { showEnterCodeDialog = false },
            onSubmit = { inputCode, source ->
                onApplyReferralCode(inputCode, source)
                showEnterCodeDialog = false
            }
        )
    }

    // Demo / Local Mode Disclaimer Dialog
    if (showDemoDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showDemoDisclaimerDialog = false },
            title = { Text("Local Referral Mode Notice") },
            text = {
                Text(
                    "This build operates in Demo / Device-Local attribution mode.\n\n" +
                    "• True app installation verification requires server-side Play Install Referrer API / token exchange.\n" +
                    "• Client-side code and link attribution operates locally using Room persistence with anti-fraud safeguards (self-referral prevention, duplicate attribution checks, and 30-day window limits)."
                )
            },
            confirmButton = {
                TextButton(onClick = { showDemoDisclaimerDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }
}

@Composable
fun ReferralStatPill(
    title: String,
    count: String,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            Text(
                text = count,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = SunsetOrange,
                modifier = Modifier.testTag(testTag)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ReferralHistoryCard(
    referral: Referral,
    modifier: Modifier = Modifier
) {
    val formattedDate = remember(referral.createdAt) {
        DateFormat.format("dd MMM yyyy, hh:mm a", Date(referral.createdAt)).toString()
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("referral_item_${referral.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        when (referral.attributionSource) {
                            AttributionSource.WHATSAPP -> Color(0xFF25D366).copy(alpha = 0.15f)
                            AttributionSource.DIRECT_LINK -> SunsetAmber.copy(alpha = 0.15f)
                            AttributionSource.COPY_LINK -> SunsetOrange.copy(alpha = 0.15f)
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (referral.attributionSource) {
                        AttributionSource.WHATSAPP -> Icons.Default.Share
                        AttributionSource.DIRECT_LINK -> Icons.Default.Link
                        AttributionSource.COPY_LINK -> Icons.Default.ContentCopy
                    },
                    contentDescription = null,
                    tint = when (referral.attributionSource) {
                        AttributionSource.WHATSAPP -> Color(0xFF25D366)
                        AttributionSource.DIRECT_LINK -> SunsetAmber
                        AttributionSource.COPY_LINK -> SunsetOrange
                    },
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Referred User: ${referral.referredUserId.takeLast(8)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Via ${referral.attributionSource.displayName} • $formattedDate",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!referral.notes.isNullOrBlank()) {
                    Text(
                        text = referral.notes,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status Chip
            Surface(
                color = when (referral.status) {
                    ReferralStatus.PENDING -> SunsetAmber.copy(alpha = 0.15f)
                    ReferralStatus.QUALIFIED -> EmeraldPrimary.copy(alpha = 0.15f)
                    ReferralStatus.REWARDED -> Color(0xFF8B5CF6).copy(alpha = 0.15f)
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    when (referral.status) {
                        ReferralStatus.PENDING -> SunsetAmber
                        ReferralStatus.QUALIFIED -> EmeraldPrimary
                        ReferralStatus.REWARDED -> Color(0xFF8B5CF6)
                    }
                ),
                modifier = Modifier.testTag("referral_status_${referral.status.name}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = when (referral.status) {
                            ReferralStatus.PENDING -> Icons.Default.Schedule
                            ReferralStatus.QUALIFIED -> Icons.Default.CheckCircle
                            ReferralStatus.REWARDED -> Icons.Default.Star
                        },
                        contentDescription = null,
                        tint = when (referral.status) {
                            ReferralStatus.PENDING -> SunsetAmber
                            ReferralStatus.QUALIFIED -> EmeraldPrimary
                            ReferralStatus.REWARDED -> Color(0xFF8B5CF6)
                        },
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = referral.status.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (referral.status) {
                            ReferralStatus.PENDING -> SunsetAmber
                            ReferralStatus.QUALIFIED -> EmeraldPrimary
                            ReferralStatus.REWARDED -> Color(0xFF8B5CF6)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EnterReferralCodeDialog(
    onDismiss: () -> Unit,
    onSubmit: (code: String, source: AttributionSource) -> Unit
) {
    var inputCode by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf(AttributionSource.COPY_LINK) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Have a Referral Code?") },
        text = {
            Column {
                Text(
                    text = "Enter the 8-character invite code shared by your friend or broker to bind attribution.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = inputCode,
                    onValueChange = {
                        inputCode = it.uppercase().take(12)
                        errorMessage = null
                    },
                    label = { Text("Referral Code (e.g. PROP9823)") },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = {
                        errorMessage?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_referral_code")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "How did you receive this code?",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AttributionSource.values().forEach { source ->
                        val isSelected = selectedSource == source
                        OutlinedButton(
                            onClick = { selectedSource = source },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) SunsetOrange else Color.Transparent,
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("selector_source_${source.name}")
                        ) {
                            Text(
                                text = source.displayName.split(" ").first(),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (inputCode.isBlank()) {
                        errorMessage = "Please enter a valid referral code"
                    } else {
                        onSubmit(inputCode.trim(), selectedSource)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SunsetOrange),
                modifier = Modifier.testTag("btn_submit_referral_code")
            ) {
                Text("Apply Code", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
