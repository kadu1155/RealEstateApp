package com.example.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CoinEconomyConfig
import com.example.domain.model.CoinTransaction
import com.example.domain.model.CoinTransactionCategory
import com.example.domain.model.CoinTransactionType
import com.example.domain.model.CoinWalletSummary
import com.example.domain.model.RedemptionRule
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SunsetAmber
import com.example.ui.theme.SunsetOrange
import java.util.Date
import java.util.UUID

@Composable
fun CoinWalletSection(
    walletSummary: CoinWalletSummary,
    transactions: List<CoinTransaction>,
    redemptionRules: List<RedemptionRule>,
    onRedeemRule: (ruleId: String, idempotencyKey: String) -> Unit,
    onProcessQualifiedReward: (referralId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showRedeemDialog by remember { mutableStateOf(false) }
    var showSecurityNoticeDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SunsetAmber.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("section_coin_wallet")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. Header with Balance & Prototype Security Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Brush.linearGradient(listOf(SunsetOrange, SunsetAmber)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "PropReel Coin Wallet",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PropReel Coin Wallet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Authoritative ledger balance • In-app perks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .clickable { showSecurityNoticeDialog = true }
                        .testTag("chip_coin_ledger_security")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security architecture notice",
                            tint = SunsetAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Prototype Ledger",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Hero Live Balance Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, SunsetAmber.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "AVAILABLE BALANCE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SunsetOrange,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${walletSummary.authoritativeBalance}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("txt_coin_balance")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = CoinEconomyConfig.CURRENCY_CODE,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SunsetAmber,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    Button(
                        onClick = { showRedeemDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SunsetOrange),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_open_redeem_coins_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Redeem,
                            contentDescription = "Redeem Coins",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Redeem", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Separate Credited Rewards Breakdown: Referral Rewards vs Welcome Rewards vs Redeemed
            Text(
                text = "Credited Rewards Breakdown",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CoinBreakdownTile(
                    title = "Referral Rewards",
                    subtitle = "+50 / invite",
                    amount = walletSummary.referralRewardsCoins,
                    accentColor = EmeraldPrimary,
                    testTag = "txt_referral_rewards_coins",
                    modifier = Modifier.weight(1f)
                )
                CoinBreakdownTile(
                    title = "Welcome Bonus",
                    subtitle = "+25 / invitee",
                    amount = walletSummary.welcomeRewardsCoins,
                    accentColor = SunsetOrange,
                    testTag = "txt_welcome_rewards_coins",
                    modifier = Modifier.weight(1f)
                )
                CoinBreakdownTile(
                    title = "Redeemed",
                    subtitle = "In-app perks",
                    amount = walletSummary.totalRedeemedCoins,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    testTag = "txt_redeemed_coins",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Pending Rewards Section (Distinct from Credited Rewards)
            Surface(
                color = SunsetAmber.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SunsetAmber.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("section_pending_coin_rewards")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = SunsetAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pending Rewards (Not Yet Credited)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${walletSummary.totalPendingRewardsCoins} Coins",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = SunsetAmber,
                            modifier = Modifier.testTag("txt_pending_rewards_total")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "• Pending referrals awaiting qualification:",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${walletSummary.pendingReferralRewardsCoins}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (walletSummary.qualifiedAwaitingCreditCoins > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "• Qualified & ready for credit:",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "${walletSummary.qualifiedAwaitingCreditCoins}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }

                        walletSummary.qualifiedReferralsAwaitingId?.let { readyId ->
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onProcessQualifiedReward(readyId) },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, EmeraldPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_credit_qualified_reward")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Credit Qualified Reward (+50 / +25 Coins)",
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    if (walletSummary.pendingWelcomeRewardCoins > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "• Your pending welcome bonus:",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${walletSummary.pendingWelcomeRewardCoins}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Coin Transaction History
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Coin Ledger History (${transactions.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Idempotent • Atomic",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (transactions.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No coin transactions recorded yet. Refer a friend or qualify an invite to earn APP_COINS!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("list_coin_transactions")
                ) {
                    transactions.forEach { tx ->
                        CoinTransactionRow(transaction = tx)
                    }
                }
            }
        }
    }

    if (showRedeemDialog) {
        RedeemCoinsDialog(
            currentBalance = walletSummary.authoritativeBalance,
            redemptionRules = redemptionRules,
            onDismiss = { showRedeemDialog = false },
            onConfirmRedeem = { ruleId, idempotencyKey ->
                onRedeemRule(ruleId, idempotencyKey)
                showRedeemDialog = false
            }
        )
    }

    if (showSecurityNoticeDialog) {
        AlertDialog(
            onDismissRequest = { showSecurityNoticeDialog = false },
            title = { Text("Local Prototype vs Production Ledger") },
            text = {
                Text(
                    "This wallet currently uses a device-local Room SQLite Coin Ledger for offline/prototype verification.\n\n" +
                    "Important Security Architecture Notes:\n" +
                    "• Client-side SQLite/Room cannot prevent tampering on rooted or compromised devices.\n" +
                    "• In production, all referral qualification checks, 50/25 coin issuances, and redemptions are strictly executed server-side via authenticated, idempotent APIs (prepared in PropReelApiService).\n" +
                    "• APP_COINS are strictly for in-app PropReel discovery & lead perks and hold no cash or monetary value."
                )
            },
            confirmButton = {
                TextButton(onClick = { showSecurityNoticeDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }
}

@Composable
private fun CoinBreakdownTile(
    title: String,
    subtitle: String,
    amount: Int,
    accentColor: Color,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
        ) {
            Text(
                text = "$amount",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = accentColor,
                modifier = Modifier.testTag(testTag)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CoinTransactionRow(
    transaction: CoinTransaction,
    modifier: Modifier = Modifier
) {
    val isCredit = transaction.transactionType == CoinTransactionType.CREDIT
    val formattedDate = remember(transaction.timestamp) {
        DateFormat.format("dd MMM, hh:mm a", Date(transaction.timestamp)).toString()
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("coin_tx_${transaction.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        color = if (isCredit) EmeraldPrimary.copy(alpha = 0.15f) else SunsetOrange.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = transaction.transactionType.displayName,
                    tint = if (isCredit) EmeraldPrimary else SunsetOrange,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$formattedDate${transaction.referenceId?.let { " • Ref: $it" } ?: ""}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (isCredit) "+${transaction.amount}" else "-${transaction.amount}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = if (isCredit) EmeraldPrimary else SunsetOrange
            )
        }
    }
}

@Composable
fun RedeemCoinsDialog(
    currentBalance: Int,
    redemptionRules: List<RedemptionRule>,
    onDismiss: () -> Unit,
    onConfirmRedeem: (ruleId: String, idempotencyKey: String) -> Unit
) {
    // Stable single-session idempotency key per open dialog to prevent duplicate taps
    val requestKey = remember { "ui_redeem_${UUID.randomUUID().toString().take(12)}" }
    var selectedRule by remember(redemptionRules) {
        mutableStateOf(redemptionRules.firstOrNull { it.isEnabled })
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Redeem,
                    contentDescription = null,
                    tint = SunsetOrange,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Redeem APP_COINS")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text(
                            text = "Available Balance:",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "$currentBalance ${CoinEconomyConfig.CURRENCY_CODE}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = SunsetOrange
                        )
                    }
                }

                redemptionRules.filter { it.isEnabled }.forEach { rule ->
                    val isSelected = selectedRule?.id == rule.id
                    val canAfford = currentBalance >= rule.coinCost && currentBalance >= rule.minBalanceRequired
                    Surface(
                        color = if (isSelected) SunsetOrange.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) SunsetOrange else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedRule = rule
                                validationError = if (!canAfford) {
                                    "Insufficient balance: Need ${rule.coinCost} APP_COINS (you have $currentBalance)."
                                } else {
                                    null
                                }
                            }
                            .testTag("redeem_rule_${rule.id}")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = rule.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    color = if (canAfford) SunsetOrange else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${rule.coinCost} Coins",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (canAfford) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = rule.description,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!canAfford) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Need ${rule.coinCost - currentBalance} more APP_COINS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                validationError?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("txt_redeem_error")
                    )
                }

                HorizontalDivider()

                Text(
                    text = "In-app utility perks only. APP_COINS have no monetary or cash conversion value.",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val chosen = selectedRule
                    if (chosen == null) {
                        validationError = "Please select a perk to redeem."
                    } else if (currentBalance < chosen.coinCost || currentBalance < chosen.minBalanceRequired) {
                        validationError = "Insufficient APP_COINS balance (${currentBalance}/${chosen.coinCost})."
                    } else {
                        onConfirmRedeem(chosen.id, "${requestKey}_${chosen.id}")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SunsetOrange),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("btn_confirm_redeem_coins")
            ) {
                Text("Confirm Redeem", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
