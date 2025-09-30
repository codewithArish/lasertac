package com.lasertrac.app

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates a professional, well-structured, single-page PDF for the challan report,
 * including header, footer, and watermark.
 */
fun generateChallanPdf(context: Context, snapDetail: SnapDetail) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(792, 1120, 1).create()
    val layoutManager = PdfLayoutManager(pdfDocument, pageInfo, context) // Pass context for resource loading

    // --- Load Logos ---
    var laserTracLogoBitmap: Bitmap? = null
    try {
        laserTracLogoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.lasertrac_logo_banner)
    } catch (e: Exception) { /* Log error */ }

    var integraLogoBitmap: Bitmap? = null
    try {
        integraLogoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.bottom_banner)
    } catch (e: Exception) { /* Log error */ }

    // --- Draw Watermark (before any other content on the page) ---
    layoutManager.drawWatermark(laserTracLogoBitmap)

    // --- Draw Header ---
    val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    layoutManager.drawHeader(currentDate, laserTracLogoBitmap)

    // --- Define Paints (Reduced Sizes) ---
    val titlePaint = TextPaint().apply { color = Color.BLACK; textSize = 16f; isFakeBoldText = true; textAlign = Paint.Align.CENTER } // Further reduced
    val valueBoldPaint = TextPaint().apply { color = Color.BLACK; textSize = 9f; isFakeBoldText = true } // Further reduced
    val redValuePaint = TextPaint(valueBoldPaint).apply { color = Color.RED }
    val linkPaint = TextPaint().apply { color = 0xFF1f8ef1.toInt(); textSize = 9f }

    // --- Draw Main Content (Compacted) ---
    layoutManager.drawCenteredText("Challan Violation Report", titlePaint, forceSinglePageCompact = true)
    // layoutManager.addSpacer(5f, forceSinglePageCompact = true) // Very small spacer if needed

    var mainBitmap: Bitmap? = null
    try {
        mainBitmap = BitmapFactory.decodeResource(context.resources, snapDetail.mainImageResId)
    } catch (e: Exception) { /* Consider logging */ }

    var plateBitmap: Bitmap? = null
    try {
        plateBitmap = BitmapFactory.decodeResource(context.resources, snapDetail.licensePlateImageResId)
    } catch (e: Exception) { /* Consider logging */ }

    var mapBitmap: Bitmap? = null
    try {
        mapBitmap = BitmapFactory.decodeResource(context.resources, snapDetail.mapImageResId)
    } catch (e: Exception) { /* Consider logging */ }

    if (mainBitmap != null) {
        layoutManager.drawImage(mainBitmap, 150f, forceSinglePageCompact = true) // Further Reduced height
    } else {
        layoutManager.drawGrid(listOf("Main Image" to "[Image Not Available]"), paints = listOf(redValuePaint), forceSinglePageCompact = true)
    }

    val contextualBitmaps = mutableListOf<Bitmap>()
    if (plateBitmap != null) contextualBitmaps.add(plateBitmap)
    if (mapBitmap != null) contextualBitmaps.add(mapBitmap)

    if (contextualBitmaps.isNotEmpty()) {
        layoutManager.drawImageRow(contextualBitmaps, 60f, forceSinglePageCompact = true) // Further Reduced height
    } else {
        if (mainBitmap != null) {
             layoutManager.drawGrid(listOf("Contextual Images" to "[Plate & Map Not Available]"), columns = 1, paints = listOf(redValuePaint), forceSinglePageCompact = true)
        }
    }
    layoutManager.addSpacer(5f, forceSinglePageCompact = true) // Reduced spacer

    val details = listOf(
        "Record ID" to snapDetail.id,
        "Record Number" to snapDetail.recordNr,
        "Date & Time" to snapDetail.dateTime,
        "Operator ID" to snapDetail.operatorId,
        "Device ID" to snapDetail.deviceId,
        "Violation Distance" to snapDetail.violationDistance,
        "Speed Limit" to "${snapDetail.speedLimit} km/h",
        "Vehicle Speed" to "${snapDetail.speed} km/h",
        "Registration Number" to snapDetail.regNr,
        "Registration Status" to snapDetail.regNrStatus,
        "Location" to snapDetail.location,
        "GPS" to "${snapDetail.latitude}, ${snapDetail.longitude}",
        "District" to snapDetail.district,
        "Police Station" to snapDetail.policeStation,
        "Challan ID" to snapDetail.uploadStatus
    )
    // Paints will be further reduced by forceSinglePageCompact in drawGrid
    val detailPaints = listOf(
        valueBoldPaint, valueBoldPaint, valueBoldPaint, valueBoldPaint, valueBoldPaint, valueBoldPaint, valueBoldPaint, redValuePaint,
        if (snapDetail.regNrStatus == "Valid") valueBoldPaint else redValuePaint,
        if (snapDetail.regNrStatus == "Valid") valueBoldPaint else redValuePaint, 
        valueBoldPaint, valueBoldPaint, valueBoldPaint, valueBoldPaint, valueBoldPaint
    )
    layoutManager.drawGrid(details, paints = detailPaints, forceSinglePageCompact = true)

    layoutManager.drawGrid(listOf("Address" to snapDetail.address), columns = 1, forceSinglePageCompact = true)
    layoutManager.drawGrid(listOf("Violation Summary" to snapDetail.violationSummary), columns = 1, paints = listOf(redValuePaint), forceSinglePageCompact = true)
    layoutManager.drawGrid(listOf("Violation Management Link" to snapDetail.violationManagementLink), columns = 1, paints = listOf(linkPaint), forceSinglePageCompact = true)
    layoutManager.drawGrid(listOf("Access Link" to snapDetail.accessLink), columns = 1, paints = listOf(linkPaint), forceSinglePageCompact = true)
    
    // --- Draw Footer (before finishing the page) ---
    layoutManager.drawFooter(integraLogoBitmap)

    // --- Finish and Save ---
    layoutManager.finish()
    savePdfToDownloads(context, pdfDocument, "challan_${snapDetail.recordNr}.pdf")
}

private class PdfLayoutManager(private val document: PdfDocument, private val pageInfo: PdfDocument.PageInfo, private val context: Context) {
    private var currentPage: PdfDocument.Page = document.startPage(pageInfo)
    private var canvas: android.graphics.Canvas = currentPage.canvas
    private var yPos: Float = 20f // Reduced initial Y position, to make space for header
    private val leftMargin = 25f // Further Reduced margin
    private val rightMargin = pageInfo.pageWidth - 25f // Further Reduced margin
    private val contentWidth = rightMargin - leftMargin
    private val bottomMargin = 25f // Further Reduced margin, to make space for footer
    private val pageEffectiveHeight = pageInfo.pageHeight - bottomMargin // Usable height before footer

    private val borderPaint = Paint().apply { color = 0xFFEAEAEA.toInt(); style = Paint.Style.STROKE; strokeWidth = 0.5f } // Lighter, thinner border
    private val headerFooterTextPaint = TextPaint().apply { color = Color.DKGRAY; textSize = 8f }

    // When forcing single page, page breaks are effectively disabled by using a very large height for checks.
    private fun checkNewPage(requiredHeight: Float, forceSinglePage: Boolean = false) {
        if (forceSinglePage) {
            // If we are forcing a single page, we don't create a new page.
            // However, we should check if content will overflow past the effective height (before footer).
            if (yPos + requiredHeight > pageEffectiveHeight) {
                // Content will overflow. Ideally, we'd scale down even more or indicate an issue.
                // For now, it will just draw past the footer line if it's too much.
            }
            return
        }
        if (yPos + requiredHeight > pageInfo.pageHeight - bottomMargin) { // Check against actual bottom margin for multi-page
            document.finishPage(currentPage)
            currentPage = document.startPage(pageInfo)
            canvas = currentPage.canvas
            drawWatermark(BitmapFactory.decodeResource(context.resources, R.drawable.lasertrac_logo_banner)) // Redraw watermark on new page
            yPos = 20f // Reset Y to top margin for new page
            // Potentially redraw header on new page if it were multi-page scenario
        }
    }

    fun drawWatermark(watermarkBitmap: Bitmap?) {
        watermarkBitmap?.let {
            val paint = Paint().apply { alpha = 30 } // Semi-transparent
            val aspectRatio = it.width.toFloat() / it.height.toFloat()
            val desiredWidth = pageInfo.pageWidth * 0.6f // Watermark covers 60% of page width
            val scaledHeight = (desiredWidth / aspectRatio).toInt()
            val scaledWidth = desiredWidth.toInt()

            val centerX = (pageInfo.pageWidth - scaledWidth) / 2f
            val centerY = (pageInfo.pageHeight - scaledHeight) / 2f
            
            val scaledWatermark = Bitmap.createScaledBitmap(it, scaledWidth, scaledHeight, true)
            canvas.drawBitmap(scaledWatermark, centerX, centerY, paint)
        }
    }

    fun drawHeader(dateText: String, logoBitmap: Bitmap?) {
        val headerHeight = 40f // Allocate space for header
        checkNewPage(headerHeight, true) // Force on current page, check against yPos

        canvas.drawText(dateText, leftMargin, yPos + headerFooterTextPaint.textSize, headerFooterTextPaint)

        logoBitmap?.let {
            val logoHeight = 30f
            val aspectRatio = it.width.toFloat() / it.height.toFloat()
            val scaledWidth = (logoHeight * aspectRatio).toInt()
            val scaledLogo = Bitmap.createScaledBitmap(it, scaledWidth, logoHeight.toInt(), true)
            canvas.drawBitmap(scaledLogo, rightMargin - scaledWidth, yPos, null)
        }
        yPos += headerHeight // Move yPos down past the header area
        canvas.drawLine(leftMargin, yPos -5f, rightMargin, yPos-5f, borderPaint) // Optional: line below header
        addSpacer(5f, true)
    }

    fun drawFooter(logoBitmap: Bitmap?) {
        // Footer is drawn at a fixed position from the bottom of the pageInfo area
        val footerYPos = pageInfo.pageHeight - bottomMargin - 30f // 30f for logo height
        canvas.drawLine(leftMargin, footerYPos - 5f, rightMargin, footerYPos - 5f, borderPaint) // Optional: line above footer

        logoBitmap?.let {
            val logoHeight = 25f
            val aspectRatio = it.width.toFloat() / it.height.toFloat()
            val scaledWidth = (logoHeight * aspectRatio).toInt()
            val scaledLogo = Bitmap.createScaledBitmap(it, scaledWidth, logoHeight.toInt(), true)
            val x = (pageInfo.pageWidth - scaledWidth) / 2f // Center the logo
            canvas.drawBitmap(scaledLogo, x, footerYPos, null)
        }
    }

    fun drawCenteredText(text: String, paint: TextPaint, forceSinglePageCompact: Boolean = false) {
        val textBounds = Rect()
        paint.getTextBounds(text, 0, text.length, textBounds)
        val textHeight = textBounds.height().toFloat()
        val spacer = if (forceSinglePageCompact) 1f else 5f // Reduced spacer
        checkNewPage(textHeight + spacer, forceSinglePageCompact)
        canvas.drawText(text, (pageInfo.pageWidth / 2).toFloat(), yPos + textBounds.exactCenterY() - textBounds.centerY(), paint)
        yPos += textHeight + spacer
    }

    fun drawImage(bitmap: Bitmap, fixedHeight: Float, forceSinglePageCompact: Boolean = false) {
        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val scaledHeight = fixedHeight.toInt()
        val scaledWidth = (scaledHeight * aspectRatio).toInt()
        val spacer = if (forceSinglePageCompact) 1f else 5f // Reduced spacer
        checkNewPage(scaledHeight.toFloat() + spacer, forceSinglePageCompact)
        val x = (pageInfo.pageWidth - scaledWidth) / 2f
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        canvas.drawBitmap(scaledBitmap, x, yPos, null)
        yPos += scaledHeight + spacer
    }

    fun drawImageRow(bitmaps: List<Bitmap>, fixedHeight: Float, forceSinglePageCompact: Boolean = false) {
        if (bitmaps.isEmpty()) return
        val scaledBitmaps = bitmaps.map { 
            val aspectRatio = it.width.toFloat() / it.height.toFloat()
            Bitmap.createScaledBitmap(it, (fixedHeight * aspectRatio).toInt(), fixedHeight.toInt(), true)
        }
        val totalWidthOfImages = scaledBitmaps.sumOf { it.width }
        val imageSpacing = if (forceSinglePageCompact) 2f else 5f // Reduced image spacing
        val spacingBetweenImages = if (scaledBitmaps.size > 1) imageSpacing * (scaledBitmaps.size - 1) else 0f
        val totalRowWidth = totalWidthOfImages + spacingBetweenImages
        val spacer = if (forceSinglePageCompact) 1f else 5f // Reduced spacer
        
        checkNewPage(fixedHeight + spacer, forceSinglePageCompact)
        var currentX = (pageInfo.pageWidth - totalRowWidth) / 2f

        for ((index, bitmap) in scaledBitmaps.withIndex()) {
            canvas.drawBitmap(bitmap, currentX, yPos, null)
            currentX += bitmap.width
            if (index < scaledBitmaps.size - 1) {
                currentX += imageSpacing
            }
        }
        yPos += fixedHeight + spacer
    }

    fun drawGrid(details: List<Pair<String, String>>, columns: Int = 2, paints: List<Paint>? = null, forceSinglePageCompact: Boolean = false) {
        if (details.isEmpty()) return

        val columnWidth = contentWidth / columns
        val cellPadding = if (forceSinglePageCompact) 1f else 3f // Reduced cell padding
        val labelValueSpacing = if (forceSinglePageCompact) 0f else 1f // Reduced spacing

        for (i in details.indices step columns) {
            var maxHeightInRow = 0f
            val layoutsInRow = mutableListOf<Pair<StaticLayout, StaticLayout>>()

            for (j in 0 until columns) {
                val detailIndex = i + j
                if (detailIndex < details.size) {
                    val (label, value) = details[detailIndex]
                    // Apply further reduced font size if compact mode, overriding passed paints' sizes
                    val valuePaint = (paints?.getOrNull(detailIndex) as? TextPaint ?: TextPaint().apply { color = Color.BLACK }).apply {
                        textSize = if (forceSinglePageCompact) 8f else 9f
                        isFakeBoldText = (paints?.getOrNull(detailIndex) as? TextPaint)?.isFakeBoldText ?: false
                        if ((paints?.getOrNull(detailIndex) as? TextPaint)?.color == Color.RED) color = Color.RED
                    }
                    val labelPaint = TextPaint().apply{ color = Color.DKGRAY; textSize = if (forceSinglePageCompact) 7f else 8f }
                    
                    val labelLayout = StaticLayout.Builder.obtain(label, 0, label.length, labelPaint, (columnWidth - 2 * cellPadding).toInt()).build()
                    val valueLayout = StaticLayout.Builder.obtain(value, 0, value.length, valuePaint, (columnWidth - 2 * cellPadding).toInt()).build()
                    layoutsInRow.add(labelLayout to valueLayout)
                    val cellHeight = labelLayout.height + valueLayout.height + 2 * cellPadding + labelValueSpacing 
                    if (cellHeight > maxHeightInRow) {
                        maxHeightInRow = cellHeight
                    }
                }
            }

            checkNewPage(maxHeightInRow, forceSinglePageCompact)
            var currentX = leftMargin
            for ((labelLayout, valueLayout) in layoutsInRow) {
                canvas.drawRect(currentX, yPos, currentX + columnWidth, yPos + maxHeightInRow, borderPaint)
                canvas.save()
                canvas.translate(currentX + cellPadding, yPos + cellPadding)
                labelLayout.draw(canvas)
                canvas.translate(0f, labelLayout.height.toFloat() + labelValueSpacing)
                valueLayout.draw(canvas)
                canvas.restore()
                currentX += columnWidth
            }
            yPos += maxHeightInRow
        }
    }

    fun addSpacer(height: Float, forceSinglePageCompact: Boolean = false) {
        // Spacers are significantly reduced in compact mode
        val actualHeight = if (forceSinglePageCompact) height / 2f else height 
        checkNewPage(actualHeight, forceSinglePageCompact)
        yPos += actualHeight
    }

    fun finish() {
        document.finishPage(currentPage)
    }
}

private fun savePdfToDownloads(context: Context, document: PdfDocument, fileName: String) {
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

    uri?.let {
        try {
            resolver.openOutputStream(it)?.use { outputStream ->
                document.writeTo(outputStream)
                Toast.makeText(context, "PDF saved to Downloads", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
        }
    } ?: Toast.makeText(context, "Failed to create PDF file", Toast.LENGTH_SHORT).show()
}
