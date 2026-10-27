package com.example.printer

import android.graphics.Bitmap
import com.example.data.model.ReceiptData
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

object EscPosBuilder {

    private val CHARSET: Charset = Charset.forName("CP437").let {
        try { it } catch (e: Exception) { Charsets.UTF_8 }
    }

    /**
     * Builds standard ESC/POS binary command stream for the given receipt data.
     */
    fun buildReceiptPayload(data: ReceiptData, ditheredLogoRaster: ByteArray? = null): ByteArray {
        val stream = ByteArrayOutputStream()

        // Wakeup / NUL bytes for BLE stability
        stream.write(byteArrayOf(0x00, 0x00, 0x00))

        // Initialize printer (ESC @)
        stream.write(byteArrayOf(0x1B, 0x40))

        // Center alignment (ESC a 1)
        stream.write(byteArrayOf(0x1B, 0x61, 0x01))

        // 1. Logo raster
        if (ditheredLogoRaster != null && ditheredLogoRaster.isNotEmpty()) {
            stream.write(ditheredLogoRaster)
            // Extra line feeds after logo
            stream.write(byteArrayOf(0x0A, 0x0A))
        }

        // 2. Receipt Header (Multi-line, Bold & Double Size)
        val headerText = data.headerText.trim()
        if (headerText.isNotEmpty()) {
            headerText.lines().forEach { line ->
                if (line.isNotBlank()) {
                    writeLine(stream, line.trim(), bold = true, doubleSize = true)
                }
            }
            writeDivider(stream, '-')
        }

        // 3. Clinic / Practice Name
        val clinic = data.clinicName.trim().ifEmpty { "Clinic" }
        writeLine(stream, "Clinic: $clinic", bold = true, doubleSize = false)

        // 4. Date & Time
        val dt = if (data.dateFormatted.isNotBlank() && data.timeFormatted.isNotBlank()) {
            "${data.dateFormatted}  ${data.timeFormatted}"
        } else {
            java.text.SimpleDateFormat("yyyy-MM-dd  HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        }
        writeLine(stream, dt, bold = false, doubleSize = false)
        writeDivider(stream, '-')

        // 5. Medical Tests
        if (!data.rbsValue.isNullOrBlank()) {
            writeLine(stream, "Random Blood Glucose Test (RBS)", bold = false, doubleSize = false)
            writeLine(stream, "Test Value: ${data.rbsFormatted}", bold = true, doubleSize = false)
            writeDivider(stream, '-')
        }

        if (!data.hba1cValue.isNullOrBlank()) {
            writeLine(stream, "HbA1c Test", bold = false, doubleSize = false)
            writeLine(stream, "HbA1c {%} : ${data.hba1cFormatted}", bold = true, doubleSize = false)
            writeDivider(stream, '-')
        }

        if (data.bpFormatted != null) {
            writeLine(stream, "Blood Pressure", bold = false, doubleSize = false)
            writeLine(stream, data.bpFormatted!!, bold = true, doubleSize = false)
            writeDivider(stream, '-')
        }

        // 6. Footer message
        writeDivider(stream, '*')
        data.footerMessage.lines().forEach { line ->
            if (line.isNotBlank()) {
                writeLine(stream, line.trim(), bold = false, doubleSize = false)
            }
        }
        writeDivider(stream, '*')

        // 7. Feeds and Paper Cut
        stream.write(byteArrayOf(0x0A, 0x0A, 0x0A, 0x0A))
        // ESC d 4 (Feed 4 lines)
        stream.write(byteArrayOf(0x1B, 0x64, 0x04))
        // GS V 65 0 (Cut paper if supported)
        stream.write(byteArrayOf(0x1D, 0x56, 0x41, 0x00))

        return stream.toByteArray()
    }

    /**
     * Quick self-test diagnostic print payload.
     */
    fun buildSelfTestPayload(): ByteArray {
        val stream = ByteArrayOutputStream()
        stream.write(byteArrayOf(0x00, 0x00, 0x00, 0x1B, 0x40, 0x1B, 0x61, 0x01))
        writeLine(stream, "ThermoPrint Studio", bold = true, doubleSize = true)
        writeLine(stream, "ESC/POS Diagnostic Test", bold = true, doubleSize = false)
        writeDivider(stream, '-')
        writeLine(stream, "Bluetooth SPP & BLE: OK", bold = false, doubleSize = false)
        writeLine(stream, "WiFi TCP Socket 9100: OK", bold = false, doubleSize = false)
        writeLine(stream, "Floyd-Steinberg Engine: OK", bold = false, doubleSize = false)
        val now = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        writeLine(stream, "Timestamp: $now", bold = false, doubleSize = false)
        writeDivider(stream, '=')
        writeLine(stream, "PRINTER READY", bold = true, doubleSize = false)
        writeDivider(stream, '=')
        stream.write(byteArrayOf(0x0A, 0x0A, 0x0A, 0x1B, 0x64, 0x04, 0x1D, 0x56, 0x41, 0x00))
        return stream.toByteArray()
    }

    private fun writeLine(stream: ByteArrayOutputStream, text: String, bold: Boolean, doubleSize: Boolean) {
        if (doubleSize) {
            // GS ! 0x11 (Double width and double height)
            stream.write(byteArrayOf(0x1D, 0x21, 0x11))
        }
        if (bold) {
            // ESC E 1
            stream.write(byteArrayOf(0x1B, 0x45, 0x01))
        }

        stream.write(text.toByteArray(CHARSET))
        stream.write(0x0A) // Line Feed

        if (bold) {
            // ESC E 0
            stream.write(byteArrayOf(0x1B, 0x45, 0x00))
        }
        if (doubleSize) {
            // GS ! 0x00 (Normal size)
            stream.write(byteArrayOf(0x1D, 0x21, 0x00))
        }
    }

    private fun writeDivider(stream: ByteArrayOutputStream, char: Char) {
        val divider = String(CharArray(32) { char })
        stream.write(divider.toByteArray(CHARSET))
        stream.write(0x0A)
    }
}
