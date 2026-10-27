package com.example.printer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.example.data.model.ReceiptData
import java.io.FileOutputStream

object SystemPrintHelper {

    fun printReceipt(context: Context, receiptData: ReceiptData) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "Receipt_${receiptData.clinicName.replace(" ", "_")}"

        printManager.print(jobName, object : PrintDocumentAdapter() {
            private var pdfDocument: PdfDocument? = null

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }

                val info = PrintDocumentInfo.Builder("medical_receipt.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()

                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                val pdf = PdfDocument()
                pdfDocument = pdf

                val pageInfo = PdfDocument.PageInfo.Builder(384, 700, 1).create()
                val page = pdf.startPage(pageInfo)
                val canvas: Canvas = page.canvas

                // Draw receipt paper background
                val bgPaint = Paint().apply { color = Color.WHITE }
                canvas.drawRect(0f, 0f, 384f, 700f, bgPaint)

                var y = 40f
                val textPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 14f
                    typeface = Typeface.MONOSPACE
                    textAlign = Paint.Align.CENTER
                }

                // Draw logo if available
                if (receiptData.logoBitmap != null) {
                    try {
                        val dithered = FloydSteinbergDitherer.createDitheredPreview(receiptData.logoBitmap, 240)
                        val xPos = (384f - dithered.width) / 2f
                        canvas.drawBitmap(dithered, xPos, y, null)
                        y += dithered.height + 20f
                    } catch (e: Exception) {}
                }

                // Header
                if (receiptData.headerText.isNotBlank()) {
                    textPaint.textSize = 15f
                    textPaint.isFakeBoldText = true
                    receiptData.headerText.lines().forEach { line ->
                        canvas.drawText(line, 192f, y, textPaint)
                        y += 22f
                    }
                    textPaint.isFakeBoldText = false
                    textPaint.textSize = 13f
                    canvas.drawText("--------------------------------", 192f, y, textPaint)
                    y += 20f
                }

                // Clinic
                textPaint.textSize = 16f
                textPaint.isFakeBoldText = true
                canvas.drawText("Clinic: ${receiptData.clinicName}", 192f, y, textPaint)
                y += 24f

                // Date/Time
                textPaint.textSize = 12f
                textPaint.isFakeBoldText = false
                val dt = "${receiptData.dateFormatted}  ${receiptData.timeFormatted}"
                canvas.drawText(dt, 192f, y, textPaint)
                y += 18f

                canvas.drawText("--------------------------------", 192f, y, textPaint)
                y += 20f

                // Tests
                if (!receiptData.rbsValue.isNullOrBlank()) {
                    canvas.drawText("Random Blood Glucose Test (RBS)", 192f, y, textPaint)
                    y += 18f
                    textPaint.isFakeBoldText = true
                    canvas.drawText("Test Value: ${receiptData.rbsFormatted}", 192f, y, textPaint)
                    textPaint.isFakeBoldText = false
                    y += 18f
                    canvas.drawText("--------------------------------", 192f, y, textPaint)
                    y += 20f
                }

                if (!receiptData.hba1cValue.isNullOrBlank()) {
                    canvas.drawText("HbA1c Test", 192f, y, textPaint)
                    y += 18f
                    textPaint.isFakeBoldText = true
                    canvas.drawText("HbA1c {%} : ${receiptData.hba1cFormatted}", 192f, y, textPaint)
                    textPaint.isFakeBoldText = false
                    y += 18f
                    canvas.drawText("--------------------------------", 192f, y, textPaint)
                    y += 20f
                }

                if (receiptData.bpFormatted != null) {
                    canvas.drawText("Blood Pressure", 192f, y, textPaint)
                    y += 18f
                    textPaint.isFakeBoldText = true
                    canvas.drawText(receiptData.bpFormatted!!, 192f, y, textPaint)
                    textPaint.isFakeBoldText = false
                    y += 18f
                    canvas.drawText("--------------------------------", 192f, y, textPaint)
                    y += 20f
                }

                // Footer
                canvas.drawText("********************************", 192f, y, textPaint)
                y += 18f
                receiptData.footerMessage.lines().forEach { line ->
                    canvas.drawText(line, 192f, y, textPaint)
                    y += 18f
                }
                canvas.drawText("********************************", 192f, y, textPaint)

                pdf.finishPage(page)

                try {
                    val out = FileOutputStream(destination?.fileDescriptor)
                    pdf.writeTo(out)
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                } finally {
                    pdf.close()
                }
            }
        }, null)
    }
}
