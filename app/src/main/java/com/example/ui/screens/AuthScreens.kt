package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.UserRole
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ReraBlue
import com.example.ui.theme.SunsetAmber
import com.example.ui.theme.SunsetOrange
import kotlinx.coroutines.delay

@Composable
fun AuthModal(
    onDismiss: () -> Unit,
    onSendOtp: (mobile: String, onSent: (isSandbox: Boolean) -> Unit) -> Unit,
    onVerifyOtp: (mobile: String, otp: String, onVerified: () -> Unit) -> Unit,
    onRoleSelected: (role: UserRole, reraNumber: String?, agencyName: String?) -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: Mobile, 2: OTP, 3: Role
    var mobile by remember { mutableStateOf("9823045678") }
    var otp by remember { mutableStateOf("999999") }
    var isSandbox by remember { mutableStateOf(true) }
    var selectedRole by remember { mutableStateOf(UserRole.BROKER) }
    var reraNumber by remember { mutableStateOf("A52100034190") }
    var agencyName by remember { mutableStateOf("Apex Prime Realty") }
    var termsAccepted by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var resendTimer by remember { mutableIntStateOf(30) }

    LaunchedEffect(step) {
        if (step == 2) {
            resendTimer = 30
            while (resendTimer > 0) {
                delay(1000)
                resendTimer--
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(SunsetOrange, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (step == 1) Icons.Default.Lock else if (step == 2) Icons.Default.Key else Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = when (step) {
                        1 -> "Sign In to PropReel"
                        2 -> "Verify Mobile OTP"
                        else -> "Select Your Role"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                when (step) {
                    1 -> {
                        // Mobile Input Step
                        Text(
                            text = "India's Free Real Estate Discovery & Reels Platform. Enter your mobile number to get started.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = mobile,
                            onValueChange = {
                                if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                                    mobile = it
                                }
                            },
                            label = { Text("Mobile Number") },
                            leadingIcon = {
                                Text(
                                    text = "+91 ",
                                    fontWeight = FontWeight.Bold,
                                    color = SunsetOrange,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_auth_mobile")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Sandbox Helper Hint
                        Surface(
                            color = SunsetAmber.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Test Sandbox: Use '+91 98230 45678' (Broker) or '+91 98901 23456' (Owner) with instant OTP 999999.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = termsAccepted,
                                onCheckedChange = { termsAccepted = it },
                                colors = CheckboxDefaults.colors(checkedColor = SunsetOrange)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "I agree to PropReel's Terms of Service, Anti-Spam Policy & Privacy Policy.",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    2 -> {
                        // OTP Verification Step
                        Text(
                            text = "Enter 6-digit OTP sent to +91 $mobile",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = otp,
                            onValueChange = {
                                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                    otp = it
                                }
                            },
                            label = { Text("6-Digit OTP Code") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_auth_otp")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (resendTimer > 0) "Resend OTP in ${resendTimer}s" else "Didn't receive code?",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (resendTimer == 0) {
                                TextButton(
                                    onClick = {
                                        resendTimer = 30
                                        onSendOtp("+91$mobile") {}
                                    }
                                ) {
                                    Text("Resend OTP", color = SunsetOrange, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    3 -> {
                        // Role Selection Step
                        Text(
                            text = "How do you plan to use PropReel?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Role Cards
                        val roles = listOf(
                            Triple(UserRole.CUSTOMER, "Buyer / Tenant", "Discover properties through reels, enquire directly for free"),
                            Triple(UserRole.OWNER, "Property Owner", "List up to 2 free properties directly with zero brokerage"),
                            Triple(UserRole.BROKER, "Real Estate Agent / Broker", "Publish unlimited listings & video reels, track CRM leads"),
                            Triple(UserRole.BUILDER, "Builder / Developer", "Showcase projects, manage high-volume site visit campaigns")
                        )

                        roles.forEach { (role, title, desc) ->
                            val isSelected = selectedRole == role
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) SunsetAmber.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, SunsetOrange) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedRole = role }
                                    .testTag("role_option_${role.name}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Apartment,
                                        contentDescription = null,
                                        tint = if (isSelected) SunsetOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) SunsetOrange else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Broker / Builder RERA Fields
                        if (selectedRole == UserRole.BROKER || selectedRole == UserRole.BUILDER) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = agencyName,
                                onValueChange = { agencyName = it },
                                label = { Text("Agency / Company Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("input_agency_name")
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = reraNumber,
                                onValueChange = { reraNumber = it },
                                label = { Text("MahaRERA Registration Number") },
                                placeholder = { Text("e.g. A52100034190") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("input_rera_number")
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (step == 1 && mobile.length >= 10 && termsAccepted) {
                        isLoading = true
                        onSendOtp("+91$mobile") { sandbox ->
                            isLoading = false
                            isSandbox = sandbox
                            step = 2
                        }
                    } else if (step == 2 && otp.length == 6) {
                        isLoading = true
                        onVerifyOtp("+91$mobile", otp) {
                            isLoading = false
                            step = 3
                        }
                    } else if (step == 3) {
                        onRoleSelected(selectedRole, reraNumber.takeIf { it.isNotBlank() }, agencyName.takeIf { it.isNotBlank() })
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SunsetOrange),
                enabled = !isLoading && (step != 1 || termsAccepted),
                modifier = Modifier.testTag("btn_auth_confirm")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = when (step) {
                            1 -> "Send OTP"
                            2 -> "Verify OTP"
                            else -> "Get Started"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
