package com.example.ca2.ui.reports

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.ca2.data.model.Prediction
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExportHelper {

    fun downloadPdf(context: Context, prediction: Prediction) {
        val fileName = "plant-report-${System.currentTimeMillis()}.pdf"
        val pdfDocument = createPdfDocument(prediction)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: error("Could not create download entry")
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    pdfDocument.writeTo(output)
                }
            } else {
                val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: context.filesDir
                val file = File(directory, fileName)
                FileOutputStream(file).use { output -> pdfDocument.writeTo(output) }
            }
        } finally {
            pdfDocument.close()
        }
    }

    fun shareSummary(context: Context, prediction: Prediction) {
        val shareText = buildString {
            append("Plant disease report\n\n")
            append("Disease: ${prediction.diseaseName}\n")
            append("Confidence: ${(prediction.confidence * 100).toInt()}%\n\n")
            append("Description: ${prediction.description}\n\n")
            append("Prevention: ${prediction.prevention}\n\n")
            append("Treatment: ${prediction.pesticide}\n")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "Share report"))
    }

    private fun createPdfDocument(prediction: Prediction): PdfDocument {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.parseColor("#24355A")
            textSize = 24f
            isFakeBoldText = true
        }
        val headingPaint = Paint().apply {
            color = Color.parseColor("#D96C3F")
            textSize = 16f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#2A2A2A")
            textSize = 13f
        }

        var y = 50f
        canvas.drawText("Plant Health Report", 40f, y, titlePaint)
        y += 30f
        canvas.drawText("Generated: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(prediction.createdAt))}", 40f, y, bodyPaint)
        y += 28f
        canvas.drawText("Disease: ${prediction.diseaseName}", 40f, y, headingPaint)
        y += 22f
        canvas.drawText("Confidence: ${(prediction.confidence * 100).toInt()}%", 40f, y, bodyPaint)
        y += 32f

        y = drawSection(canvas, "Description", prediction.description, y, headingPaint, bodyPaint)
        y = drawSection(canvas, "Causes", prediction.causes, y, headingPaint, bodyPaint)
        y = drawSection(canvas, "Prevention", prediction.prevention, y, headingPaint, bodyPaint)
        y = drawSection(canvas, "Fertilizer", prediction.fertilizer, y, headingPaint, bodyPaint)
        y = drawSection(canvas, "Pesticide", prediction.pesticide, y, headingPaint, bodyPaint)
        y = drawSection(canvas, "Recovery", prediction.recoveryTime, y, headingPaint, bodyPaint)
        drawSection(canvas, "Extra Care Tips", prediction.extraCareTips, y, headingPaint, bodyPaint)

        document.finishPage(page)
        return document
    }

    private fun drawSection(
        canvas: Canvas,
        title: String,
        content: String,
        startY: Float,
        headingPaint: Paint,
        bodyPaint: Paint
    ): Float {
        var y = startY
        canvas.drawText(title, 40f, y, headingPaint)
        y += 18f
        wrapText(content.ifBlank { "Not available" }, 72).forEach { line ->
            canvas.drawText(line, 40f, y, bodyPaint)
            y += 18f
        }
        return y + 10f
    }

    private fun wrapText(text: String, maxChars: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        val current = StringBuilder()
        for (word in words) {
            if (current.isNotEmpty() && current.length + word.length + 1 > maxChars) {
                lines.add(current.toString().trim())
                current.clear()
            }
            current.append(word).append(' ')
        }
        if (current.isNotBlank()) lines.add(current.toString().trim())
        return lines
    }
}
