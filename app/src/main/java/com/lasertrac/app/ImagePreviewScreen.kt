package com.lasertrac.app

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lasertrac.app.ui.theme.Lasertac2Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ActionStatus { IDLE, IN_PROGRESS, SUCCESS, FAILURE }

fun generateChallanPdf(context: Context, snapDetail: SnapDetail) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(792, 1120, 1).create()
    val layoutManager = PdfLayoutManager(pdfDocument, pageInfo, context)

    val laserTracLogoBitmap = try {
        BitmapFactory.decodeResource(context.resources, R.drawable.lasertrac_logo_banner)
    } catch (e: Exception) { null }

    val integraLogoBitmap = try {
        BitmapFactory.decodeResource(context.resources, R.drawable.bottom_banner)
    } catch (e: Exception) { null }

    layoutManager.drawWatermark(laserTracLogoBitmap)
    val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    layoutManager.drawHeader(currentDate, laserTracLogoBitmap)

    val titlePaint = TextPaint().apply { this.color = android.graphics.Color.BLACK; textSize = 16f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
    val valueBoldPaint = TextPaint().apply { this.color = android.graphics.Color.BLACK; textSize = 9f; isFakeBoldText = true }
    val redValuePaint = TextPaint(valueBoldPaint).apply { this.color = android.graphics.Color.RED }
    val linkPaint = TextPaint().apply { this.color = 0xFF1f8ef1.toInt(); textSize = 9f }

    layoutManager.drawCenteredText("Challan Violation Report", titlePaint, forceSinglePageCompact = true)

    val mainBitmap = loadBitmapFromAny(context, snapDetail.mainImage)
    val plateBitmap = loadBitmapFromAny(context, snapDetail.licensePlateImage)
    val mapBitmap = loadBitmapFromAny(context, snapDetail.mapImage)

    if (mainBitmap != null) {
        layoutManager.drawImage(mainBitmap, 150f, forceSinglePageCompact = true)
    } else {
        layoutManager.drawGrid(listOf("Main Image" to "[Image Not Available]"), paints = listOf(redValuePaint), forceSinglePageCompact = true)
    }

    val contextualBitmaps = listOfNotNull(plateBitmap, mapBitmap)
    if (contextualBitmaps.isNotEmpty()) {
        layoutManager.drawImageRow(contextualBitmaps, 60f, forceSinglePageCompact = true)
    }

    layoutManager.addSpacer(5f, forceSinglePageCompact = true)

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
    
    layoutManager.drawFooter(integraLogoBitmap)

    layoutManager.finish()
    savePdfToDownloads(context, pdfDocument, "challan_${snapDetail.recordNr}.pdf")
}

private fun loadBitmapFromAny(context: Context, imageSource: Any): Bitmap? {
    return when (imageSource) {
        is Int -> {
            try {
                BitmapFactory.decodeResource(context.resources, imageSource)
            } catch (e: Exception) {
                null
            }
        }
        is Uri -> {
            try {
                context.contentResolver.openInputStream(imageSource)?.use {
                    BitmapFactory.decodeStream(it)
                }
            } catch (e: Exception) {
                null
            }
        }
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePreviewScreen(
    snapDetail: SnapDetail,
    onClose: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    isPrevEnabled: Boolean,
    isNextEnabled: Boolean,
    onStatusChange: (SnapDetail) -> Unit
) {
    var actionStatus by remember { mutableStateOf(ActionStatus.IDLE) }
    var statusText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    LaunchedEffect(actionStatus) {
        if (actionStatus == ActionStatus.SUCCESS || actionStatus == ActionStatus.FAILURE) {
            delay(1500)
            actionStatus = ActionStatus.IDLE
        }
    }

    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
        Scaffold {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(it)
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PageHeader(snapDetail = snapDetail, onClose = onClose)

                ActionButtonsSection(
                    snapDetail = snapDetail,
                    onUpdateClick = {
                        scope.launch {
                            statusText = "Updating..."
                            actionStatus = ActionStatus.IN_PROGRESS
                            delay(800)
                            onStatusChange(snapDetail.copy(status = SnapStatus.UPDATED))
                            statusText = "Updated"
                            actionStatus = ActionStatus.SUCCESS
                        }
                    },
                    onUploadClick = {
                        scope.launch {
                            statusText = "Uploading..."
                            actionStatus = ActionStatus.IN_PROGRESS
                            delay(1200)
                            onStatusChange(snapDetail.copy(status = SnapStatus.UPLOADED))
                            statusText = "Uploaded"
                            actionStatus = ActionStatus.SUCCESS
                        }
                    },
                    isUploadEnabled = snapDetail.status == SnapStatus.UPDATED,
                    onRejectClick = {
                        scope.launch {
                            statusText = "Rejecting..."
                            actionStatus = ActionStatus.IN_PROGRESS
                            delay(800)
                            onStatusChange(snapDetail.copy(status = SnapStatus.REJECTED))
                            statusText = "Rejected"
                            actionStatus = ActionStatus.SUCCESS
                        }
                    },
                    onPrintClick = { generateChallanPdf(context, snapDetail) }
                )

                MainImageSection(snapDetail, onPrev, onNext, isPrevEnabled, isNextEnabled)
                ContextualImagesSection(snapDetail)
                ExcelDetailsGrid(snapDetail)
                AdditionalDummyDetailsSection()
                Spacer(modifier = Modifier.height(60.dp))
            }
        }

        AnimatedVisibility(
            visible = actionStatus != ActionStatus.IDLE,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                AnimatedStatusLogo(status = actionStatus, text = statusText)
            }
        }
    }
}

@Composable
private fun PageHeader(snapDetail: SnapDetail, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Challan Details", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(8.dp))
            StatusChip(status = snapDetail.status)
        }
        Spacer(modifier = Modifier.width(48.dp))
    }
}

@Composable
fun StatusChip(status: SnapStatus) {
    Box(
        modifier = Modifier
            .background(status.color, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.name,
            color = androidx.compose.ui.graphics.Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AnimatedStatusLogo(status: ActionStatus, text: String) {
    Box(
        modifier = Modifier
            .size(160.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Crossfade(targetState = status, animationSpec = tween(300)) { currentStatus ->
                when (currentStatus) {
                    ActionStatus.IN_PROGRESS -> CircularProgressIndicator(modifier = Modifier.size(60.dp))
                    ActionStatus.SUCCESS -> Icon(Icons.Default.Check, "Success", modifier = Modifier.size(60.dp), tint = androidx.compose.ui.graphics.Color.Green)
                    ActionStatus.FAILURE -> Icon(Icons.Default.Close, "Failure", modifier = Modifier.size(60.dp), tint = androidx.compose.ui.graphics.Color.Red)
                    ActionStatus.IDLE -> {}
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun MainImageSection(
    snapDetail: SnapDetail,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    isPrevEnabled: Boolean,
    isNextEnabled: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onPrev, enabled = isPrevEnabled) { Text("Prev") }
            Text(snapDetail.dateTime, style = MaterialTheme.typography.bodySmall)
            Button(onClick = onNext, enabled = isNextEnabled) { Text("Next") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.fillMaxWidth()) {
            AsyncImage(model = snapDetail.mainImage, contentDescription = "Main preview", modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
            Card(modifier = Modifier.padding(8.dp), colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)), shape = RoundedCornerShape(8.dp)) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Speed: ", color = androidx.compose.ui.graphics.Color.White, fontSize = 12.sp)
                    Text("${snapDetail.speed}", color = androidx.compose.ui.graphics.Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(" / ${snapDetail.speedLimit} km/h", color = androidx.compose.ui.graphics.Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Dist: ${snapDetail.violationDistance}", color = androidx.compose.ui.graphics.Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ContextualImagesSection(snapDetail: SnapDetail) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.weight(1.5f).height(80.dp).clip(RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            AsyncImage(model = snapDetail.licensePlateImage, contentDescription = "License plate", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f)))
            Text(text = snapDetail.regNr, color = androidx.compose.ui.graphics.Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        AsyncImage(model = snapDetail.mapImage, contentDescription = "Map preview", modifier = Modifier.weight(1f).height(80.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, androidx.compose.ui.graphics.Color.Gray, RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
    }
}

@Composable
private fun ExcelDetailsGrid(snapDetail: SnapDetail) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(all = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Violation Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoCell("Device Id", snapDetail.deviceId, Modifier.weight(1f))
                InfoCell("Record Nr", snapDetail.recordNr, Modifier.weight(1f))
                InfoCell("Operator Id", snapDetail.operatorId, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoCell("Speed Limit", "${snapDetail.speedLimit} km/h", Modifier.weight(1f))
                InfoCell("Speed", "${snapDetail.speed} km/h", Modifier.weight(1f), valueColor = if (snapDetail.speed > snapDetail.speedLimit) MaterialTheme.colorScheme.error else androidx.compose.ui.graphics.Color.Unspecified)
                InfoCell("Reg Nr", snapDetail.regNr, Modifier.weight(1f), valueColor = if(snapDetail.regNrStatus == "Valid") Color(0xFF008800) else MaterialTheme.colorScheme.error)
            }
            InfoCell("Evidence Date", snapDetail.evidenceDate)
            InfoCell("Full Address", snapDetail.address)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoCell("District", snapDetail.district, Modifier.weight(1f))
                InfoCell("Police Station", snapDetail.policeStation, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoCell("Latitude", snapDetail.latitude, Modifier.weight(1f))
                InfoCell("Longitude", snapDetail.longitude, Modifier.weight(1f))
            }
            InfoCell("Violation", snapDetail.violationSummary, highlight = true)
        }
    }
}

@Composable
private fun AdditionalDummyDetailsSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(all = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Additional Details (Dummy)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoCell("Officer Name", "Inspector Sharma", Modifier.weight(1f))
                InfoCell("Challan Amount", "₹ 1,000", Modifier.weight(1f), valueColor = Color(0xFFAA0000))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoCell("Vehicle Type", "Sedan", Modifier.weight(1f))
                InfoCell("Color", "White", Modifier.weight(1f))
            }
            InfoCell("Remarks", "Sample data for preview and layout testing.")
        }
    }
}

@Composable
private fun InfoCell(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Color.Unspecified, highlight: Boolean = false) {
    Column(modifier = modifier.border(0.5.dp, Color.LightGray).padding(horizontal = 4.dp, vertical = 2.dp)) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.7f))
        Text(value.ifEmpty { "N/A" }, fontSize = 12.sp, color = if(highlight) MaterialTheme.colorScheme.error else valueColor, fontWeight = if(highlight) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun ActionButtonsSection(
    snapDetail: SnapDetail,
    onUpdateClick: () -> Unit,
    onUploadClick: () -> Unit,
    isUploadEnabled: Boolean,
    onPrintClick: () -> Unit,
    onRejectClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        Button(onClick = onUpdateClick, enabled = snapDetail.status == SnapStatus.PENDING, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("Update") }
        Button(onClick = onUploadClick, enabled = isUploadEnabled, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("Upload") }
        Button(onClick = onPrintClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))) { Text("Print") }
        Button(onClick = onRejectClick, enabled = snapDetail.status != SnapStatus.REJECTED, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Reject") }
    }
}

private class PdfLayoutManager(private val document: PdfDocument, private val pageInfo: PdfDocument.PageInfo, private val context: Context) {
    private var currentPage: PdfDocument.Page = document.startPage(pageInfo)
    private var canvas: android.graphics.Canvas = currentPage.canvas
    private var yPos: Float = 20f
    private val leftMargin = 25f
    private val rightMargin = pageInfo.pageWidth - 25f
    private val contentWidth = rightMargin - leftMargin
    private val bottomMargin = 25f
    private val pageEffectiveHeight = pageInfo.pageHeight - bottomMargin

    private val borderPaint = Paint().apply { this.color = 0xFFEAEAEA.toInt(); style = Paint.Style.STROKE; strokeWidth = 0.5f }
    private val headerFooterTextPaint = TextPaint().apply { this.color = android.graphics.Color.DKGRAY; textSize = 8f }

    private fun checkNewPage(requiredHeight: Float, forceSinglePage: Boolean = false) {
        if (forceSinglePage && yPos + requiredHeight > pageEffectiveHeight) {
            // Content overflows, do nothing for now
        }
    }

    fun drawWatermark(watermarkBitmap: Bitmap?) {
        watermarkBitmap?.let {
            val paint = Paint().apply { alpha = 30 }
            val aspectRatio = it.width.toFloat() / it.height.toFloat()
            val desiredWidth = pageInfo.pageWidth * 0.6f
            val scaledHeight = (desiredWidth / aspectRatio).toInt()
            val scaledWidth = desiredWidth.toInt()
            val centerX = (pageInfo.pageWidth - scaledWidth) / 2f
            val centerY = (pageInfo.pageHeight - scaledHeight) / 2f
            val scaledWatermark = Bitmap.createScaledBitmap(it, scaledWidth, scaledHeight, true)
            canvas.drawBitmap(scaledWatermark, centerX, centerY, paint)
        }
    }

    fun drawHeader(dateText: String, logoBitmap: Bitmap?) {
        val headerHeight = 40f
        checkNewPage(headerHeight, true)
        canvas.drawText(dateText, leftMargin, yPos + headerFooterTextPaint.textSize, headerFooterTextPaint)
        logoBitmap?.let {
            val logoHeight = 30f
            val aspectRatio = it.width.toFloat() / it.height.toFloat()
            val scaledWidth = (logoHeight * aspectRatio).toInt()
            val scaledLogo = Bitmap.createScaledBitmap(it, scaledWidth, logoHeight.toInt(), true)
            canvas.drawBitmap(scaledLogo, rightMargin - scaledWidth, yPos, null)
        }
        yPos += headerHeight
        canvas.drawLine(leftMargin, yPos - 5f, rightMargin, yPos - 5f, borderPaint)
        addSpacer(5f, true)
    }

    fun drawFooter(logoBitmap: Bitmap?) {
        val footerYPos = pageInfo.pageHeight - bottomMargin - 30f
        canvas.drawLine(leftMargin, footerYPos - 5f, rightMargin, footerYPos - 5f, borderPaint)
        logoBitmap?.let {
            val logoHeight = 25f
            val aspectRatio = it.width.toFloat() / it.height.toFloat()
            val scaledWidth = (logoHeight * aspectRatio).toInt()
            val scaledLogo = Bitmap.createScaledBitmap(it, scaledWidth, logoHeight.toInt(), true)
            val x = (pageInfo.pageWidth - scaledWidth) / 2f
            canvas.drawBitmap(scaledLogo, x, footerYPos, null)
        }
    }

    fun drawCenteredText(text: String, paint: TextPaint, forceSinglePageCompact: Boolean = false) {
        val textBounds = Rect()
        paint.getTextBounds(text, 0, text.length, textBounds)
        val textHeight = textBounds.height().toFloat()
        val spacer = if (forceSinglePageCompact) 1f else 5f
        checkNewPage(textHeight + spacer, forceSinglePageCompact)
        canvas.drawText(text, (pageInfo.pageWidth / 2).toFloat(), yPos + textBounds.exactCenterY() - textBounds.centerY(), paint)
        yPos += textHeight + spacer
    }

    fun drawImage(bitmap: Bitmap, fixedHeight: Float, forceSinglePageCompact: Boolean = false) {
        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val scaledHeight = fixedHeight.toInt()
        val scaledWidth = (scaledHeight * aspectRatio).toInt()
        val spacer = if (forceSinglePageCompact) 1f else 5f
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
        val imageSpacing = if (forceSinglePageCompact) 2f else 5f
        val spacingBetweenImages = if (scaledBitmaps.size > 1) imageSpacing * (scaledBitmaps.size - 1) else 0f
        val totalRowWidth = totalWidthOfImages + spacingBetweenImages
        val spacer = if (forceSinglePageCompact) 1f else 5f
        checkNewPage(fixedHeight + spacer, forceSinglePageCompact)
        var currentX = (pageInfo.pageWidth - totalRowWidth) / 2f
        scaledBitmaps.forEachIndexed { index, bitmap ->
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
        val cellPadding = if (forceSinglePageCompact) 1f else 3f
        val labelValueSpacing = if (forceSinglePageCompact) 0f else 1f
        for (i in details.indices step columns) {
            var maxHeightInRow = 0f
            val layoutsInRow = mutableListOf<Pair<StaticLayout, StaticLayout>>()
            for (j in 0 until columns) {
                val detailIndex = i + j
                if (detailIndex < details.size) {
                    val (label, value) = details[detailIndex]
                    val valuePaint = (paints?.getOrNull(detailIndex) as? TextPaint ?: TextPaint().apply { this.color = android.graphics.Color.BLACK }).apply {
                        textSize = if (forceSinglePageCompact) 8f else 9f
                        isFakeBoldText = (paints?.getOrNull(detailIndex) as? TextPaint)?.isFakeBoldText ?: false
                        if ((paints?.getOrNull(detailIndex) as? TextPaint)?.color == android.graphics.Color.RED) this.color = android.graphics.Color.RED
                    }
                    val labelPaint = TextPaint().apply{ this.color = android.graphics.Color.DKGRAY; textSize = if (forceSinglePageCompact) 7f else 8f }
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

@Preview(showBackground = true)
@Composable
private fun ImagePreviewScreenPreview() {
    val dummySnap = SnapDetail(
        id = "prev_mock1",
        regNr = "PREV1234",
        dateTime = "2023-10-27 10:30:00",
        speed = 100,
        speedLimit = 80,
        location = "Preview Highway",
        evidenceDate = "2023-10-27",
        district = "Preview District",
        policeStation = "Preview PS",
        address = "123 Preview Lane",
        latitude = "12.345",
        longitude = "67.890",
        recordNr = "PREC-123",
        deviceId = "PDEV-01",
        operatorId = "POP-01",
        violationDistance = "120m",
        uploadStatus = "Pending",
        status = SnapStatus.PENDING,
        mainImage = R.drawable.ic_snaps_custom,
        licensePlateImage = R.drawable.ic_snaps_custom,
        mapImage = R.drawable.ic_snaps_custom,
        violationSummary = "Overspeeding",
        violationManagementLink = "",
        accessLink = "",
        regNrStatus = "Valid"
    )
    Lasertac2Theme {
        ImagePreviewScreen(
            snapDetail = dummySnap,
            onClose = {},
            onPrev = {},
            onNext = {},
            isPrevEnabled = true,
            isNextEnabled = true,
            onStatusChange = {}
        )
    }
}
