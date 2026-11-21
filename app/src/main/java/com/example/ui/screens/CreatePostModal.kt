package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.FurnishingStatus
import com.example.domain.model.PossessionStatus
import com.example.domain.model.Property
import com.example.domain.model.PropertyCategory
import com.example.domain.model.Reel
import com.example.domain.model.User
import com.example.ui.theme.SunsetOrange
import java.util.UUID

/**
 * Dedicated modal for creating and publishing verified property listings.
 * Reel creation is exclusively handled via the Reels Feed Plus (+) button.
 */
@Composable
fun CreatePostModal(
    currentUser: User,
    onDismiss: () -> Unit,
    onPropertyCreated: (Property) -> Unit,
    onReelCreated: ((Reel) -> Unit)? = null
) {
    // Property Form State
    var title by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var locality by remember { mutableStateOf("Baner") }
    var bhkText by remember { mutableStateOf("2") }
    var carpetAreaText by remember { mutableStateOf("850") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.AddHome,
                contentDescription = null,
                tint = SunsetOrange
            )
        },
        title = {
            Text(
                text = "List New Property (Free)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Add verified listing details to reach genuine home buyers in Pune & Mumbai.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Property Title") },
                    placeholder = { Text("e.g. 2 BHK in Pristine Meadows") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_prop_title")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Price and BHK
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Price (₹)") },
                        placeholder = { Text("7500000") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_prop_price")
                    )
                    OutlinedTextField(
                        value = bhkText,
                        onValueChange = { bhkText = it },
                        label = { Text("BHK") },
                        placeholder = { Text("2") },
                        modifier = Modifier
                            .weight(0.5f)
                            .testTag("input_prop_bhk")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Locality and Carpet Area
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = locality,
                        onValueChange = { locality = it },
                        label = { Text("Locality") },
                        placeholder = { Text("Wakad, Baner, Worli") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_prop_locality")
                    )
                    OutlinedTextField(
                        value = carpetAreaText,
                        onValueChange = { carpetAreaText = it },
                        label = { Text("Carpet Area (sqft)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_prop_carpet")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description & Amenities") },
                    placeholder = { Text("Modular kitchen, clubhouse, reserved parking...") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_prop_desc")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && priceText.isNotBlank()) {
                        val price = priceText.toDoubleOrNull() ?: 5000000.0
                        val priceFormatted = if (price >= 10000000) "₹%.2f Cr".format(price / 10000000) else "₹%.0f Lakh".format(price / 100000)
                        val propId = "prop_" + UUID.randomUUID().toString().take(8)
                        val detectedCity = if (
                            locality.contains("Worli", ignoreCase = true) ||
                            locality.contains("Bandra", ignoreCase = true) ||
                            locality.contains("Powai", ignoreCase = true) ||
                            locality.contains("Andheri", ignoreCase = true) ||
                            locality.contains("Mumbai", ignoreCase = true)
                        ) "Mumbai" else "Pune"

                        val newProp = Property(
                            id = propId,
                            userId = currentUser.id,
                            sellerName = currentUser.fullName,
                            sellerRole = currentUser.role,
                            sellerPhone = currentUser.mobile,
                            sellerBadge = currentUser.verificationBadge,
                            title = title.trim(),
                            description = if (description.isNotBlank()) description.trim() else "Newly listed property in $locality $detectedCity.",
                            category = PropertyCategory.RESIDENTIAL_BUY,
                            price = price,
                            priceDisplay = priceFormatted,
                            isNegotiable = true,
                            city = detectedCity,
                            locality = locality.trim(),
                            landmark = "$detectedCity, Maharashtra",
                            bhk = bhkText.toIntOrNull() ?: 2,
                            bathrooms = 2,
                            carpetAreaSqft = carpetAreaText.toDoubleOrNull() ?: 800.0,
                            floorNumber = 5,
                            totalFloors = 12,
                            furnishing = FurnishingStatus.SEMI_FURNISHED,
                            possession = PossessionStatus.READY_TO_MOVE,
                            amenities = listOf("Lift", "Security", "Parking", "Power Backup", "Clubhouse"),
                            imageUrls = listOf("https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=1000&q=80")
                        )
                        onPropertyCreated(newProp)
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank() && priceText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SunsetOrange),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_confirm_post")
            ) {
                Text("Publish Property")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
