package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {

    fun exportPdfReport(
        context: Context,
        reportTitle: String,
        subtitle: String,
        sections: List<Pair<String, List<String>>>
    ): Result<File> {
        return try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val outFile = File(exportDir, "StudioFlow360_Report_$timestamp.pdf")

            val pdf = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            var page = pdf.startPage(pageInfo)
            var canvas = page.canvas

            val headerPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 20f
                isFakeBoldText = true
            }
            val subPaint = Paint().apply {
                color = Color.rgb(71, 85, 105)
                textSize = 11f
            }
            val sectionPaint = Paint().apply {
                color = Color.rgb(37, 99, 235)
                textSize = 14f
                isFakeBoldText = true
            }
            val bodyPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 11f
            }

            var y = 52f
            canvas.drawText(reportTitle, 40f, y, headerPaint)
            y += 20f
            canvas.drawText(subtitle, 40f, y, subPaint)
            y += 26f

            for ((sectionName, lines) in sections) {
                if (y > 760f) {
                    pdf.finishPage(page)
                    page = pdf.startPage(pageInfo)
                    canvas = page.canvas
                    y = 50f
                }
                canvas.drawText(sectionName, 40f, y, sectionPaint)
                y += 18f
                for (line in lines) {
                    if (y > 790f) {
                        pdf.finishPage(page)
                        page = pdf.startPage(pageInfo)
                        canvas = page.canvas
                        y = 50f
                    }
                    canvas.drawText(line.take(92), 48f, y, bodyPaint)
                    y += 15f
                }
                y += 12f
            }

            pdf.finishPage(page)
            FileOutputStream(outFile).use { pdf.writeTo(it) }
            pdf.close()
            Result.success(outFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportCsvReport(
        context: Context,
        fileNamePrefix: String,
        headers: List<String>,
        rows: List<List<String>>
    ): Result<File> {
        return try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val outFile = File(exportDir, "${fileNamePrefix}_$timestamp.csv")
            val sb = StringBuilder()
            sb.appendLine(headers.joinToString(",") { escapeCsv(it) })
            rows.forEach { row ->
                sb.appendLine(row.joinToString(",") { escapeCsv(it) })
            }
            outFile.writeText(sb.toString(), Charsets.UTF_8)
            Result.success(outFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Report"))
    }
}
