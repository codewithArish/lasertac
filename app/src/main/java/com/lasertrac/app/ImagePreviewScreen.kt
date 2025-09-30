package com.lasertrac.app

import android.content.Intent // ADDED IMPORT
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lasertrac.app.ui.theme.Lasertac2Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ActionStatus { IDLE, IN_PROGRESS, SUCCESS, FAILURE }
enum class SnapStatus(val color: Color) {
    PENDING(Color.Red),
    UPDATED(Color.Green),
    UPLOADED(Color.Blue),
    REJECTED(Color.Gray)
}

data class SnapDetail(
    val id: String,
    @DrawableRes val mainImageResId: Int,
    @DrawableRes val licensePlateImageResId: Int,
    @DrawableRes val mapImageResId: Int,
    val dateTime: String,
    val deviceId: String,
    val operatorId: String,
    val speed: Int,
    val speedLimit: Int,
    val location: String,
    val violationDistance: String,
    val recordNr: String,
    val regNr: String,
    val regNrStatus: String,
    val latitude: String,
    val longitude: String,
    val evidenceDate: String,
    val district: String,
    val policeStation: String,
    val address: String,
    val violationSummary: String,
    val violationManagementLink: String,
    val accessLink: String,
    val uploadStatus: String,
    val status: SnapStatus
)

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
    val context = LocalContext.current // Context can be retrieved here or passed down
    val scrollState = rememberScrollState()

    LaunchedEffect(actionStatus) {
        if (actionStatus == ActionStatus.SUCCESS || actionStatus == ActionStatus.FAILURE) {
            delay(200) // Adjusted delay
            actionStatus = ActionStatus.IDLE
        }
    }

    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
        Scaffold {
            scaffoldPaddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(scaffoldPaddingValues) 
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PageHeader(snapDetail = snapDetail, onClose = onClose)

                ActionButtonsSection(
                    snapDetail = snapDetail, // Pass snapDetail here
                    onUpdateClick = {
                        scope.launch {
                            statusText = "Updating..."
                            actionStatus = ActionStatus.IN_PROGRESS
                            delay(50)
                            onStatusChange(snapDetail.copy(status = SnapStatus.UPDATED))
                            statusText = "Updated"
                            actionStatus = ActionStatus.SUCCESS
                        }
                    },
                    onUploadClick = {
                        scope.launch {
                            statusText = "Uploading..."
                            actionStatus = ActionStatus.IN_PROGRESS
                            delay(50)
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
                            delay(50)
                            onStatusChange(snapDetail.copy(status = SnapStatus.REJECTED))
                            statusText = "Rejected"
                            actionStatus = ActionStatus.SUCCESS
                        }
                    },
                    // onShareClick removed, handled internally by ActionButtonsSection
                    onPrintClick = {
                        generateChallanPdf(context, snapDetail)
                    }
                )

                MainImageSection(snapDetail, onPrev, onNext, isPrevEnabled, isNextEnabled)
                ContextualImagesSection(snapDetail)
                ExcelDetailsGrid(snapDetail)
                 Spacer(modifier = Modifier.height(60.dp)) // Add some space at the bottom
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
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                AnimatedStatusLogo(status = actionStatus, text = statusText)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        Spacer(modifier = Modifier.width(48.dp)) // To balance the IconButton on the left
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
            color = Color.White,
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
                    ActionStatus.SUCCESS -> Icon(Icons.Default.Check, "Success", modifier = Modifier.size(60.dp), tint = Color.Green)
                    ActionStatus.FAILURE -> Icon(Icons.Default.Close, "Failure", modifier = Modifier.size(60.dp), tint = Color.Red)
                    ActionStatus.IDLE -> { /* Do nothing */ }
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
            Image(
                painter = painterResource(id = snapDetail.mainImageResId),
                contentDescription = "Main preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Card(
                modifier = Modifier.padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Speed: ", color = Color.White, fontSize = 12.sp)
                    Text("${snapDetail.speed}", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(" / ${snapDetail.speedLimit} km/h", color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Dist: ${snapDetail.violationDistance}", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ContextualImagesSection(snapDetail: SnapDetail) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Image(
            painter = painterResource(id = snapDetail.licensePlateImageResId),
            contentDescription = "License plate",
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        Image(
            painter = painterResource(id = snapDetail.mapImageResId),
            contentDescription = "Map preview",
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun ExcelDetailsGrid(snapDetail: SnapDetail) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(all = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Violation Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                InfoCell("Device Id", snapDetail.deviceId, Modifier.weight(1f))
                InfoCell("Record Nr", snapDetail.recordNr, Modifier.weight(1f))
                InfoCell("Operator Id", snapDetail.operatorId, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                InfoCell("Speed Limit", "${snapDetail.speedLimit} km/h", Modifier.weight(1f))
                InfoCell("Speed", "${snapDetail.speed} km/h", Modifier.weight(1f), valueColor = if (snapDetail.speed > snapDetail.speedLimit) Color.Red else Color.Unspecified)
                InfoCell("Reg Nr", snapDetail.regNr, Modifier.weight(1f), valueColor = if(snapDetail.regNrStatus == "Valid") Color.Green else Color.Red)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                InfoCell("Location", snapDetail.location, Modifier.weight(1f))
                InfoCell("Latitude", snapDetail.latitude, Modifier.weight(1f))
                InfoCell("Longitude", snapDetail.longitude, Modifier.weight(1f))
            }
            InfoCell("Evidence Date", snapDetail.evidenceDate)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                InfoCell("Districts", snapDetail.district, Modifier.weight(1f))
                InfoCell("Police Station", snapDetail.policeStation, Modifier.weight(1f))
            }
            InfoCell("Address", snapDetail.address)
            InfoCell("Violation", snapDetail.violationSummary, highlight = true)
            Column(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
                Text("Management Links", fontSize = 9.sp, color = Color.Gray)
                Text(snapDetail.violationManagementLink, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { /*TODO*/ }, fontSize = 11.sp)
                Text(snapDetail.accessLink, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { /*TODO*/ }, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun InfoCell(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Color.Unspecified, highlight: Boolean = false) {
    Column(modifier = modifier.border(0.5.dp, Color.LightGray).padding(horizontal = 3.dp, vertical = 1.dp)) {
        Text(label, fontSize = 9.sp, color = Color.Gray)
        Text(value, fontSize = 11.sp, color = if(highlight) Color.Red else valueColor, fontWeight = if(highlight) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun ActionButtonsSection(
    snapDetail: SnapDetail, // ADDED snapDetail parameter
    onUpdateClick: () -> Unit,
    onUploadClick: () -> Unit,
    isUploadEnabled: Boolean,
    // onShareClick: () -> Unit, // REMOVED onShareClick parameter
    onPrintClick: () -> Unit,
    onRejectClick: () -> Unit
) {
    val context = LocalContext.current // Get context for the share intent

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(text = "Update", onClick = onUpdateClick, modifier = Modifier.weight(1f))
            ActionButton(text = "Reject", onClick = onRejectClick, buttonColor = Color.Red, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(text = "Upload", onClick = onUploadClick, enabled = isUploadEnabled, modifier = Modifier.weight(1f))
            ActionButton(
                text = "Share", 
                onClick = {
                    val shareText = """
                        Challan Details:
                        Record Nr: ${snapDetail.recordNr}
                        Reg Nr: ${snapDetail.regNr} (${snapDetail.regNrStatus})
                        Date & Time: ${snapDetail.dateTime}
                        Violation: ${snapDetail.violationSummary}
                        Speed: ${snapDetail.speed} km/h (Limit: ${snapDetail.speedLimit} km/h)
                        Location: ${snapDetail.location} (${snapDetail.latitude}, ${snapDetail.longitude})
                        Address: ${snapDetail.address}
                        Device ID: ${snapDetail.deviceId}
                        Operator ID: ${snapDetail.operatorId}
                        District: ${snapDetail.district}
                        Police Station: ${snapDetail.policeStation}
                        Evidence Date: ${snapDetail.evidenceDate}
                        Status: ${snapDetail.status.name}
                        Upload Info: ${snapDetail.uploadStatus}
                        Management Link: ${snapDetail.violationManagementLink}
                        Access Link: ${snapDetail.accessLink}
                    """.trimIndent()

                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Snap Violation Details: ${snapDetail.regNr}")
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Snap Details"))
                }, 
                modifier = Modifier.weight(1f)
            )
            ActionButton(text = "Print", onClick = onPrintClick, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ActionButton(text: String, onClick: () -> Unit, buttonColor: Color? = null, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor ?: MaterialTheme.colorScheme.primary),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 10.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun ImagePreviewScreenPreview() {
    val dummySnap = remember {
        SnapDetail(
            id = "1",
            mainImageResId = R.drawable.ic_snaps_custom, // Ensure this drawable exists
            licensePlateImageResId = R.drawable.ic_snaps_custom, // Ensure this drawable exists
            mapImageResId = R.drawable.ic_snaps_custom, // Ensure this drawable exists
            dateTime = "2024-05-20 10:30:15",
            deviceId = "LT-12345",
            operatorId = "OP-6789",
            speed = 92,
            speedLimit = 80,
            location = "Highway A1",
            violationDistance = "150 m",
            recordNr = "REC-001",
            regNr = "HR26AS0001",
            regNrStatus = "Valid",
            latitude = "28.6139",
            longitude = "77.2090",
            evidenceDate = "2024-05-20",
            district = "New Delhi",
            policeStation = "Central Delhi PS",
            address = "Near India Gate, New Delhi, India, 110001",
            violationSummary = "Overspeeding",
            violationManagementLink = "http://example.com/violation/1",
            accessLink = "http://example.com/access/1",
            uploadStatus = "Uploaded: KL250728125150TP2AA0263AI00010",
            status = SnapStatus.UPDATED
        )
    }
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
