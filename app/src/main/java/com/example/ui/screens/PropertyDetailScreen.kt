package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.EnquiryIntent
import com.example.domain.model.Property
import com.example.ui.components.VerifiedBadgeChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SunsetAmber
import com.example.ui.theme.SunsetOrange

@Composable
fun PropertyDetailScreen(
    property: Property?,
    isSaved: Boolean,
    onBackClick: () -> Unit,
    onSaveToggle: () -> Unit,
    onSubmitEnquiry: (customerName: String, customerPhone: String, message: String, intent: EnquiryIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showEnquiryDialog by remember { mutableStateOf(false) }

    if (property == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Property not found", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    Scaffold(
        bottomBar = {
            // Sticky Bottom CTA Bar with Instant WhatsApp & Enquiry
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = property.priceDisplay,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = SunsetOrange
                        )
                        Text(
                            text = "${property.carpetAreaSqft.toInt()} sq.ft Carpet",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // 1-Tap WhatsApp Direct Chat
                    IconButton(
                        onClick = {
                            val cleanPhone = property.sellerPhone.replace("+", "").replace(" ", "").replace("-", "")
                            val msg = Uri.encode(
                                "Hello ${property.sellerName}, I saw ${property.title} (${property.priceDisplay}, ${property.locality}) on PropReel. Please share brochure and details."
                            )
                            val uri = Uri.parse("https://wa.me/$cleanPhone?text=$msg")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF25D366), RoundedCornerShape(14.dp))
                            .testTag("btn_whatsapp_property_detail")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Chat on WhatsApp",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Primary Enquiry Action
                    Button(
                        onClick = { showEnquiryDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SunsetOrange,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("btn_enquire_now")
                    ) {
                        Text(
                            text = "Enquire Now",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Hero Image with Back & Save buttons
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                AsyncImage(
                    model = property.imageUrls.firstOrNull(),
                    contentDescription = property.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Navigation Icons
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(40.dp)
                            .testTag("btn_back_detail")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                val shareText = "Check out ${property.title} in ${property.locality}, Pune for ${property.priceDisplay} on PropReel!\nhttps://propreel.in/properties/${property.id}"
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share Property")
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .size(40.dp)
                                .testTag("btn_share_detail")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Property",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = onSaveToggle,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .size(40.dp)
                                .testTag("btn_save_detail")
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Save",
                                tint = if (isSaved) SunsetAmber else Color.White
                            )
                        }
                    }
                }
            }

            // Property Heading & Location
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = property.priceDisplay,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = SunsetOrange
                        )
                        if (property.carpetAreaSqft > 0) {
                            val perSqft = (property.price / property.carpetAreaSqft).toInt()
                            Text(
                                text = "₹$perSqft / sq.ft Carpet",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    VerifiedBadgeChip(badge = property.sellerBadge)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = property.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = SunsetAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${property.locality}, ${property.city} • ${property.landmark}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Key Specs Matrix
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Key Specifications",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    SpecGridItem(title = "BHK", value = "${property.bhk ?: "-"} BHK", modifier = Modifier.weight(1f))
                    SpecGridItem(title = "Carpet Area", value = "${property.carpetAreaSqft.toInt()} sq.ft", modifier = Modifier.weight(1f))
                    SpecGridItem(title = "Floor", value = "${property.floorNumber ?: "-"}/${property.totalFloors ?: "-"}", modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    SpecGridItem(title = "Furnishing", value = property.furnishing.label, modifier = Modifier.weight(1f))
                    SpecGridItem(title = "Possession", value = property.possession.label, modifier = Modifier.weight(1f))
                    SpecGridItem(title = "Bathrooms", value = "${property.bathrooms ?: "-"}", modifier = Modifier.weight(1f))
                }

                if (property.reraId != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MahaRERA ID: ${property.reraId}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Interactive Home Loan EMI Calculator
            if (property.price > 1000000) {
                EmiCalculatorCard(propertyPrice = property.price)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }

            // Seller Contact / Broker Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(SunsetOrange.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = property.sellerName.take(1),
                            color = SunsetOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = property.sellerName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${property.sellerRole.displayName} • Verified on PropReel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Amenities Section
            if (property.amenities.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = "Amenities & Features",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        property.amenities.take(4).forEach { amenity ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = amenity,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Description
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "About This Property",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = property.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    // Interactive Enquiry Intent Modal
    if (showEnquiryDialog) {
        EnquiryDialog(
            propertyTitle = property.title,
            sellerName = property.sellerName,
            onDismiss = { showEnquiryDialog = false },
            onSubmit = { name, phone, msg, intent ->
                onSubmitEnquiry(name, phone, msg, intent)
                showEnquiryDialog = false
            }
        )
    }
}

@Composable
fun SpecGridItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun EnquiryDialog(
    propertyTitle: String,
    sellerName: String,
    onDismiss: () -> Unit,
    onSubmit: (name: String, phone: String, message: String, intent: EnquiryIntent) -> Unit
) {
    var name by remember { mutableStateOf("Rahul Verma") }
    var phone by remember { mutableStateOf("+91 98220 12345") }
    var selectedIntent by remember { mutableStateOf(EnquiryIntent.SITE_VISIT) }
    var consentChecked by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Enquire to $sellerName",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = propertyTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SunsetOrange,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_enquiry_name")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_enquiry_phone")
                )

                Text(
                    text = "Select Intent:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                EnquiryIntent.values().forEach { intent ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedIntent == intent,
                            onClick = { selectedIntent = intent },
                            colors = RadioButtonDefaults.colors(selectedColor = SunsetOrange)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = intent.label, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = consentChecked,
                        onCheckedChange = { consentChecked = it },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "I consent to share my contact with verified seller for this property only.",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (consentChecked && name.isNotBlank() && phone.isNotBlank()) {
                        onSubmit(name, phone, "Intent: ${selectedIntent.label}", selectedIntent)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SunsetOrange),
                enabled = consentChecked,
                modifier = Modifier.testTag("btn_submit_enquiry_dialog")
            ) {
                Text("Send Enquiry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EmiCalculatorCard(
    propertyPrice: Double,
    modifier: Modifier = Modifier
) {
    var loanPercentage by remember { mutableStateOf(0.80f) }
    var interestRate by remember { mutableStateOf(8.5f) }
    var tenureYears by remember { mutableStateOf(20f) }

    val loanAmount = propertyPrice * loanPercentage
    val monthlyRate = (interestRate / (12 * 100)).toDouble()
    val months = (tenureYears * 12).toInt()

    val emi = remember(loanAmount, monthlyRate, months) {
        if (monthlyRate > 0 && months > 0) {
            val pow = Math.pow(1 + monthlyRate, months.toDouble())
            (loanAmount * monthlyRate * pow / (pow - 1)).toInt()
        } else {
            0
        }
    }

    val totalPayment = emi.toLong() * months
    val totalInterest = (totalPayment - loanAmount).coerceAtLeast(0.0).toLong()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = SunsetOrange,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Home Loan EMI Calculator",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Result Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = SunsetAmber.copy(alpha = 0.12f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Estimated Monthly EMI",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${String.format("%,d", emi)} / mo",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = SunsetOrange
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Principal: ₹${String.format("%.1f", loanAmount / 100000)} Lakh",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Interest: ₹${String.format("%.1f", totalInterest.toDouble() / 100000)} Lakh",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Loan Amount Slider
        Text(
            text = "Loan Amount: ${(loanPercentage * 100).toInt()}% (₹${String.format("%.1f", loanAmount / 100000)} Lakh)",
            style = MaterialTheme.typography.labelMedium
        )
        Slider(
            value = loanPercentage,
            onValueChange = { loanPercentage = it },
            valueRange = 0.20f..0.90f,
            colors = SliderDefaults.colors(
                thumbColor = SunsetOrange,
                activeTrackColor = SunsetOrange
            )
        )

        // Tenure Slider
        Text(
            text = "Tenure: ${tenureYears.toInt()} Years",
            style = MaterialTheme.typography.labelMedium
        )
        Slider(
            value = tenureYears,
            onValueChange = { tenureYears = it },
            valueRange = 5f..30f,
            steps = 24,
            colors = SliderDefaults.colors(
                thumbColor = SunsetOrange,
                activeTrackColor = SunsetOrange
            )
        )

        // Interest Rate Slider
        Text(
            text = "Interest Rate: ${String.format("%.1f", interestRate)}% p.a.",
            style = MaterialTheme.typography.labelMedium
        )
        Slider(
            value = interestRate,
            onValueChange = { interestRate = it },
            valueRange = 7.0f..12.0f,
            steps = 9,
            colors = SliderDefaults.colors(
                thumbColor = SunsetOrange,
                activeTrackColor = SunsetOrange
            )
        )
    }
}
