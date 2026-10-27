package com.example.data.model

import android.graphics.Bitmap

data class ReceiptData(
    val clinicName: String = "Central Medical Centre",
    val headerText: String = "123 Health Ave.\nTel: 555-0198",
    val rbsValue: String? = null,
    val hba1cValue: String? = null,
    val bpSystolic: String? = null,
    val bpDiastolic: String? = null,
    val dateFormatted: String = "",
    val timeFormatted: String = "",
    val logoBitmap: Bitmap? = null,
    val footerMessage: String = "Thank you for your visit!\nPlease follow up with your doctor."
) {
    val rbsFormatted: String?
        get() = rbsValue?.takeIf { it.isNotBlank() }?.let { "$it mg/dL" }

    val hba1cFormatted: String?
        get() = hba1cValue?.takeIf { it.isNotBlank() }?.let { "$it %" }

    val bpFormatted: String?
        get() = when {
            bpSystolic.isNullOrBlank() && bpDiastolic.isNullOrBlank() -> null
            bpSystolic.isNullOrBlank() -> "?/${bpDiastolic} mmHg"
            bpDiastolic.isNullOrBlank() -> "${bpSystolic}/? mmHg"
            else -> "$bpSystolic/$bpDiastolic mmHg"
        }

    val hasAnyTest: Boolean
        get() = !rbsValue.isNullOrBlank() || !hba1cValue.isNullOrBlank() || !bpSystolic.isNullOrBlank() || !bpDiastolic.isNullOrBlank()
}
