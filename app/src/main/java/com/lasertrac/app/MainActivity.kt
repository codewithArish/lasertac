package com.lasertrac.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lasertrac.app.db.AppDatabase
import com.lasertrac.app.ui.AuthScreen
import com.lasertrac.app.LocationScreen // Corrected Import
import com.lasertrac.app.ui.theme.DashboardIconCircleBg
import com.lasertrac.app.ui.theme.Lasertac2Theme
import com.lasertrac.app.ui.theme.TextColorLight
import com.lasertrac.app.util.SessionManager
import kotlinx.coroutines.launch

enum class Screen {
    Login, Dashboard, Settings, Videos, FTP, DeviceId, Violations, Reports, Snaps, Location // Added Location
}

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
        val sessionManager = SessionManager(applicationContext)

        setContent {
            Lasertac2Theme {
                val startScreen = if (sessionManager.isLoggedIn()) Screen.Dashboard else Screen.Login
                var currentScreen by remember { mutableStateOf(startScreen) }

                when (currentScreen) {
                    Screen.Login -> AuthScreen(onLoginSuccess = { currentScreen = Screen.Dashboard })
                    else -> {
                        val logout = {
                            sessionManager.clearSession()
                            currentScreen = Screen.Login
                        }
                        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                        val scope = rememberCoroutineScope()

                        BackHandler(enabled = drawerState.isOpen) {
                            scope.launch { drawerState.close() }
                        }

                        ModalNavigationDrawer(
                            drawerState = drawerState,
                            drawerContent = {
                                ModalDrawerSheet {
                                    Spacer(Modifier.height(12.dp))
                                    NavigationDrawerItem(
                                        icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout") },
                                        label = { Text("Logout") },
                                        selected = false,
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            logout()
                                        }
                                    )
                                }
                            },
                            content = {
                                when (currentScreen) {
                                    Screen.Dashboard -> MainDashboardScreen(
                                        onMenuClick = { scope.launch { drawerState.open() } },
                                        onNavigateTo = { screen -> currentScreen = screen }
                                    )
                                    Screen.Settings -> SettingsScreen(onNavigateBack = { currentScreen = Screen.Dashboard })
                                    Screen.Videos -> VideosScreen(onNavigateBack = { currentScreen = Screen.Dashboard })
                                    Screen.FTP -> {
                                        val ftpViewModel: FTPViewModel = viewModel(factory = FTPViewModelFactory(appDb.snapLocationDao()))
                                        FTPScreen(
                                            onNavigateBack = { currentScreen = Screen.Dashboard },
                                            ftpViewModel = ftpViewModel
                                        )
                                    }
                                    Screen.DeviceId -> DeviceIdScreen(onNavigateBack = { currentScreen = Screen.Dashboard })
                                    Screen.Violations -> ViolationsScreen(
                                        onNavigateBack = { currentScreen = Screen.Dashboard },
                                        violationDao = appDb.violationDao()
                                    )
                                    Screen.Reports -> ReportsScreen(onNavigateBack = { currentScreen = Screen.Dashboard })
                                    Screen.Snaps -> SnapsScreen(
                                        onNavigateBack = { currentScreen = Screen.Dashboard },
                                        snapLocationDao = appDb.snapLocationDao()
                                    )
                                    // Added navigation for LocationScreen
                                    Screen.Location -> LocationScreen(
                                        onNavigateBack = { currentScreen = Screen.Dashboard },
                                        snapId = "", // Pass a default or selected snapId
                                        snapLocationDao = appDb.snapLocationDao()
                                    )

                                    Screen.Login -> {}
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    onMenuClick: () -> Unit,
    onNavigateTo: (Screen) -> Unit
) {
    val features = listOf(
        FeatureGridItemData("Snaps", R.drawable.ic_snaps_custom, DashboardIconCircleBg) { onNavigateTo(Screen.Snaps) },
        FeatureGridItemData("Videos", R.drawable.ic_videos_custom, DashboardIconCircleBg) { onNavigateTo(Screen.Videos) },
        FeatureGridItemData("Settings", R.drawable.ic_settings_custom, DashboardIconCircleBg) { onNavigateTo(Screen.Settings) },
        FeatureGridItemData("FTP", R.drawable.ic_ftp_custom, DashboardIconCircleBg) { onNavigateTo(Screen.FTP) },
        FeatureGridItemData("Device ID", R.drawable.ic_device_id_custom, DashboardIconCircleBg) { onNavigateTo(Screen.DeviceId) },
        FeatureGridItemData("Violations", R.drawable.ic_violations_custom, DashboardIconCircleBg) { onNavigateTo(Screen.Violations) },
        FeatureGridItemData("Reports", R.drawable.ic_reports_custom, DashboardIconCircleBg) { onNavigateTo(Screen.Reports) },
        // Added Location button to the dashboard
        FeatureGridItemData("Location", R.drawable.ic_location_custom, DashboardIconCircleBg) { onNavigateTo(Screen.Location) }
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(DashboardIconCircleBg),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = TextColorLight)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.dashboard_background_device),
                contentDescription = "Dashboard Background",
                modifier = Modifier.fillMaxSize(),
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
                        modifier = Modifier.fillMaxWidth().height(72.dp),
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
            .clickable {                scope.launch {
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
                .background(color = item.iconBackgroundColor, shape = CircleShape)
                .border(width = 1.dp, color = Color.White.copy(alpha = 0.4f), shape = CircleShape),
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
                onMenuClick = {},
                onNavigateTo = {}
            )
        }
    }
}
