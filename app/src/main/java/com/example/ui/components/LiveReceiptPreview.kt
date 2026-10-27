package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReceiptData
import com.example.ui.theme.ReceiptPaperLight
import com.example.ui.theme.ReceiptTextDark

@Composable
fun LiveReceiptPreview(
    receiptData: ReceiptData,
    ditheredBitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_receipt_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Realistic Thermal Paper Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(ReceiptPaperLight)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Dithered Logo Preview
                    if (ditheredBitmap != null) {
                        Image(
                            bitmap = ditheredBitmap.asImageBitmap(),
                            contentDescription = "Dithered Practice Logo",
                            modifier = Modifier
                                .height(56.dp)
                                .padding(bottom = 10.dp)
                                .testTag("receipt_logo_img")
                        )
                    }

                    // Header Text
                    if (receiptData.headerText.isNotBlank()) {
                        receiptData.headerText.lines().forEach { line ->
                            if (line.isNotBlank()) {
                                Text(
                                    text = line.trim(),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ReceiptTextDark,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        ReceiptDivider()
                    }

                    // Clinic Name
                    Text(
                        text = "Clinic: ${receiptData.clinicName.ifEmpty { "_____" }}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ReceiptTextDark,
                        textAlign = TextAlign.Center
                    )

                    // Date & Time
                    val dt = if (receiptData.dateFormatted.isNotBlank()) {
                        "${receiptData.dateFormatted}  ${receiptData.timeFormatted}"
                    } else {
                        "-- -- --"
                    }
                    Text(
                        text = dt,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = ReceiptTextDark.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )

                    ReceiptDivider()

                    // Tests
                    if (!receiptData.rbsValue.isNullOrBlank()) {
                        Text(
                            text = "Random Blood Glucose Test (RBS)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = ReceiptTextDark,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Test Value: ${receiptData.rbsFormatted}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ReceiptTextDark,
                            textAlign = TextAlign.Center
                        )
                        ReceiptDivider()
                    }

                    if (!receiptData.hba1cValue.isNullOrBlank()) {
                        Text(
                            text = "HbA1c Test",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = ReceiptTextDark,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "HbA1c {%} : ${receiptData.hba1cFormatted}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ReceiptTextDark,
                            textAlign = TextAlign.Center
                        )
                        ReceiptDivider()
                    }

                    if (receiptData.bpFormatted != null) {
                        Text(
                            text = "Blood Pressure",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = ReceiptTextDark,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = receiptData.bpFormatted!!,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ReceiptTextDark,
                            textAlign = TextAlign.Center
                        )
                        ReceiptDivider()
                    }

                    if (!receiptData.hasAnyTest) {
                        Text(
                            text = "[ No test values entered yet ]",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        ReceiptDivider()
                    }

                    // Footer Stars
                    Text(
                        text = "********************************",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = ReceiptTextDark.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Thank you for your visit!",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = ReceiptTextDark,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Please follow up with your doctor.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = ReceiptTextDark.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "********************************",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = ReceiptTextDark.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ReceiptDivider() {
    Text(
        text = "--------------------------------",
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        color = ReceiptTextDark.copy(alpha = 0.6f),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
