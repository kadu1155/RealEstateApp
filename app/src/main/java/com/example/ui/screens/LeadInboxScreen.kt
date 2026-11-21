package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Lead
import com.example.domain.model.LeadStage
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ReraBlue
import com.example.ui.theme.SunsetAmber
import com.example.ui.theme.SunsetOrange

@Composable
fun LeadInboxScreen(
    leads: List<Lead>,
    onUpdateStage: (leadId: String, newStage: LeadStage, notes: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedStageFilter by remember { mutableStateOf<LeadStage?>(null) }
    var leadToEdit by remember { mutableStateOf<Lead?>(null) }
    val context = LocalContext.current

    val filteredLeads = leads.filter { lead ->
        selectedStageFilter == null || lead.stage == selectedStageFilter
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Leads & CRM Pipeline",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Track enquiries from reels and property discovery in real-time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Pipeline Stage Filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedStageFilter == null,
                onClick = { selectedStageFilter = null },
                label = { Text("All Leads (${leads.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SunsetOrange,
                    selectedLabelColor = Color.White
                )
            )

            LeadStage.values().filter { it != LeadStage.NOT_RELEVANT }.forEach { stage ->
                val count = leads.count { it.stage == stage }
                FilterChip(
                    selected = selectedStageFilter == stage,
                    onClick = { selectedStageFilter = if (selectedStageFilter == stage) null else stage },
                    label = { Text("${stage.label} ($count)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SunsetOrange,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (filteredLeads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No leads in this stage yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("leads_list_view")
            ) {
                items(filteredLeads, key = { it.id }) { lead ->
                    LeadCard(
                        lead = lead,
                        onCallClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${lead.customerPhone}"))
                            context.startActivity(intent)
                        },
                        onEditClick = { leadToEdit = lead }
                    )
                }
            }
        }
    }

    if (leadToEdit != null) {
        LeadStatusDialog(
            lead = leadToEdit!!,
            onDismiss = { leadToEdit = null },
            onSave = { newStage, notes ->
                onUpdateStage(leadToEdit!!.id, newStage, notes)
                leadToEdit = null
            }
        )
    }
}

@Composable
fun LeadCard(
    lead: Lead,
    onCallClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("lead_card_${lead.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Customer Header & Stage Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(SunsetOrange.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = SunsetOrange,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lead.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = lead.customerPhone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StageBadge(stage = lead.stage)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Property Reference
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = lead.propertyTitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${lead.locality} • ${lead.propertyPriceDisplay}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SunsetOrange
                    )
                }
            }

            if (lead.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notes: ${lead.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (lead.scheduledVisitDate != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Visit Scheduled: ${lead.scheduledVisitDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Call, WhatsApp & Update Status
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onCallClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).testTag("btn_call_lead_${lead.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Call", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        val cleanPhone = lead.customerPhone.replace("+", "").replace(" ", "").replace("-", "")
                        val msg = Uri.encode("Hello ${lead.customerName}, regarding your inquiry for ${lead.propertyTitle} on PropReel...")
                        val uri = Uri.parse("https://wa.me/$cleanPhone?text=$msg")
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).testTag("btn_whatsapp_lead_${lead.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "WhatsApp", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).testTag("btn_update_lead_${lead.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Stage", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun StageBadge(stage: LeadStage) {
    val (bgColor, textColor) = when (stage) {
        LeadStage.NEW -> Pair(SunsetAmber.copy(alpha = 0.15f), SunsetOrange)
        LeadStage.CONTACTED -> Pair(ReraBlue.copy(alpha = 0.15f), ReraBlue)
        LeadStage.SITE_VISIT -> Pair(EmeraldPrimary.copy(alpha = 0.15f), EmeraldPrimary)
        LeadStage.CLOSED -> Pair(EmeraldPrimary, Color.White)
        else -> Pair(Color.LightGray.copy(alpha = 0.2f), Color.DarkGray)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = stage.label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun LeadStatusDialog(
    lead: Lead,
    onDismiss: () -> Unit,
    onSave: (newStage: LeadStage, notes: String) -> Unit
) {
    var selectedStage by remember { mutableStateOf(lead.stage) }
    var notes by remember { mutableStateOf(lead.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Lead Status") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Customer: ${lead.customerName}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Select Stage:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                LeadStage.values().forEach { stage ->
                    Surface(
                        color = if (selectedStage == stage) SunsetAmber.copy(alpha = 0.2f) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        TextButton(
                            onClick = { selectedStage = stage },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stage.label,
                                color = if (selectedStage == stage) SunsetOrange else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (selectedStage == stage) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Follow-up Notes") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedStage, notes) },
                colors = ButtonDefaults.buttonColors(containerColor = SunsetOrange)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
