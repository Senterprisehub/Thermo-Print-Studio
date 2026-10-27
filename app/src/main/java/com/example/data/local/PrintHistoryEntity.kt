package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "print_history")
data class PrintHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val clinicName: String,
    val headerText: String,
    val rbsValue: String?,
    val hba1cValue: String?,
    val bpSystolic: String?,
    val bpDiastolic: String?,
    val dateTimeFormatted: String,
    val timestamp: Long = System.currentTimeMillis(),
    val connectionType: String,
    val printerName: String
)
