package com.lasertrac.app

import android.os.Bundle
// import android.util.Log // Removed unused import
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler // IMPORT BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lasertrac.app.db.AppDatabase
// import com.lasertrac.app.db.PoliceStationDao // Removed unused import
import com.lasertrac.app.db.SnapLocationDao // Kept as snapLocationDao is used by LocationScreen
import com.lasertrac.app.ui.theme.Lasertac2Theme
import com.lasertrac.app.ui.theme.TextColorLight
import com.lasertrac.app.ui.theme.DashboardIconCircleBg
import kotlinx.coroutines.launch

// Screen identifiers for state-based navigation
enum class Screen {
    Dashboard,
    Settings,
    Snaps,
    Videos,
    FTP,
    DeviceId,
    Location,
    Violations,
    Reports
    // ImagePreview is NOT a top-level screen here, it's shown in a Dialog from SnapsScreen
}

// Updated to use DrawableRes for icons
data class FeatureGridItemData(
    val title: String,
    @DrawableRes val iconResId: Int,
    val iconBackgroundColor: Color,
    val onClick: () -> Unit
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appDb = AppDatabase.getDatabase(applicationContext)
        val snapLocationDao = appDb.snapLocationDao()

        setContent {
            Lasertac2Theme {
                var currentScreen by remember { mutableStateOf(Screen.Dashboard) }
                val context = LocalContext.current // For potential finishActivity call

                BackHandler(enabled = true) {
                    if (currentScreen != Screen.Dashboard) {
                        currentScreen = Screen.Dashboard
                    } else {
                        (context as? ComponentActivity)?.finishAffinity() // Exits the app
                    }
                }

                when (currentScreen) {
                    Screen.Dashboard -> MainDashboardScreen(
                        onNavigateToSettings = { currentScreen = Screen.Settings },
                        onNavigateToSnaps = { currentScreen = Screen.Snaps },
                        onNavigateToVideos = { currentScreen = Screen.Videos },
                        onNavigateToFTP = { currentScreen = Screen.FTP },
                        onNavigateToDeviceId = { currentScreen = Screen.DeviceId },
                        onNavigateToLocation = { currentScreen = Screen.Location },
                        onNavigateToViolations = { currentScreen = Screen.Violations },
                        onNavigateToReports = { currentScreen = Screen.Reports }
                    )
                    Screen.Settings -> SettingsScreen(
                        onNavigateBack = { currentScreen = Screen.Dashboard }
                    )
                    Screen.Snaps -> SnapsScreen(
                        onNavigateBack = { currentScreen = Screen.Dashboard },
                        snapLocationDao = snapLocationDao // Added snapLocationDao back
                    )
                    Screen.Videos -> VideosScreen(
                        onNavigateBack = { currentScreen = Screen.Dashboard }
                    )
                    Screen.FTP -> FTPScreen(
                        onNavigateBack = { currentScreen = Screen.Dashboard }
                    )
                    Screen.DeviceId -> DeviceIdScreen(
                        onNavigateBack = { currentScreen = Screen.Dashboard }
                    )
                    Screen.Location -> LocationScreen(
                        onNavigateBack = { currentScreen = Screen.Dashboard },
                        snapId = "current_device_location",
                        snapLocationDao = snapLocationDao
                    )
                    Screen.Violations -> ViolationsScreen(
                        onNavigateBack = { currentScreen = Screen.Dashboard }
                    )
                    Screen.Reports -> ReportsScreen(
                        onNavigateBack = { currentScreen = Screen.Dashboard }
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToSnaps: () -> Unit,
    onNavigateToVideos: () -> Unit,
    onNavigateToFTP: () -> Unit,
    onNavigateToDeviceId: () -> Unit,
    onNavigateToLocation: () -> Unit,
    onNavigateToViolations: () -> Unit,
    onNavigateToReports: () -> Unit
) {
    val features = listOf(
        FeatureGridItemData("Snaps", R.drawable.ic_snaps_custom, DashboardIconCircleBg) { onNavigateToSnaps() },
        FeatureGridItemData("Videos", R.drawable.ic_videos_custom, DashboardIconCircleBg) { onNavigateToVideos() },
        FeatureGridItemData("Settings", R.drawable.ic_settings_custom, DashboardIconCircleBg) { onNavigateToSettings() },
        FeatureGridItemData("FTP", R.drawable.ic_ftp_custom, DashboardIconCircleBg) { onNavigateToFTP() },
        FeatureGridItemData("Device ID", R.drawable.ic_device_id_custom, DashboardIconCircleBg) { onNavigateToDeviceId() },
        FeatureGridItemData("Location", R.drawable.ic_location_custom, DashboardIconCircleBg) { onNavigateToLocation() },
        FeatureGridItemData("Violations", R.drawable.ic_violations_custom, DashboardIconCircleBg) { onNavigateToViolations() },
        FeatureGridItemData("Reports", R.drawable.ic_reports_custom, DashboardIconCircleBg) { onNavigateToReports() }
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text("Home", color = TextColorLight, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(DashboardIconCircleBg),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = { /* TODO: Handle navigation drawer */ }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = TextColorLight)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.dashboard_background_device),
                contentDescription = "Dashboard Background",
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 4.dp),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.lasertrac_logo_banner),
                        contentDescription = "LaserTrac Logo Banner",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp),
                        contentScale = ContentScale.Crop
                    )
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(features) { feature ->
                        FeatureGridButton(item = feature)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.bottom_banner),
                            contentDescription = "Bottom Banner",
                            modifier = Modifier.height(40.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureGridButton(item: FeatureGridItemData) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .scale(scale.value)
            .clickable {
                scope.launch {
                     scale.animateTo(1.15f, animationSpec = tween(durationMillis = 100))
                    scale.animateTo(1f, animationSpec = tween(durationMillis = 100))
                    item.onClick()
                }
            }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .background(
                    color = item.iconBackgroundColor,
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.4f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = item.iconResId),
                contentDescription = item.title,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = item.title,
                color = TextColorLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainDashboardScreenPreview_New() {
    Lasertac2Theme {
        Box(modifier = Modifier.background(Color(0xFF42475A))) {
            MainDashboardScreen(
                onNavigateToSettings = {},
                onNavigateToSnaps = {},
                onNavigateToVideos = {},
                onNavigateToFTP = {},
                onNavigateToDeviceId = {},
                onNavigateToLocation = {},
                onNavigateToViolations = {},
                onNavigateToReports = {}
            )
        }
    }
}
