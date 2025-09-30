package com.lasertrac.app

// import androidx.compose.foundation.layout.Arrangement // Removed unused import
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GraphicEq // For Audio
import androidx.compose.material.icons.filled.MyLocation // For Target
import androidx.compose.material.icons.filled.Storage // For Storage
import androidx.compose.material.icons.filled.Tune // For System/General Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lasertrac.app.ui.theme.Lasertac2Theme
import com.lasertrac.app.ui.theme.PrimaryBlue
import com.lasertrac.app.ui.theme.LightGrayBackground
import com.lasertrac.app.ui.theme.DarkGrayText
import com.lasertrac.app.ui.theme.CardBackground

data class SettingItem(
    val title: String,
    val currentValue: String? = null, // Optional: display current value
    val onClick: () -> Unit
)

data class SettingsCategory(
    val title: String,
    val icon: ImageVector,
    val items: List<SettingItem>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val settingsCategories = listOf(
        SettingsCategory(
            title = "Target Settings",
            icon = Icons.Filled.MyLocation,
            items = listOf(
                SettingItem("Direction") { /* TODO */ },
                SettingItem("Speed") { /* TODO */ },
                SettingItem("Diff. Speed") { /* TODO */ },
                SettingItem("Min. Speed") { /* TODO */ },
                SettingItem("Max. Speed") { /* TODO */ },
                SettingItem("Vehicle Class") { /* TODO */ }
            )
        ),
        SettingsCategory(
            title = "Audio Settings",
            icon = Icons.Filled.GraphicEq,
            items = listOf(
                SettingItem("Buzzer") { /* TODO */ },
                SettingItem("Voice") { /* TODO */ }
            )
        ),
        SettingsCategory(
            title = "Storage Settings",
            icon = Icons.Filled.Storage,
            items = listOf(
                SettingItem("Keep Full Rec.") { /* TODO */ },
                SettingItem("FTP Auto Upload") { /* TODO */ },
                SettingItem("File Format") { /* TODO */ }
            )
        ),
        SettingsCategory(
            title = "System Settings",
            icon = Icons.Filled.Tune,
            items = listOf(
                SettingItem("GPS") { /* TODO */ },
                SettingItem("Speed Unit") { /* TODO */ },
                SettingItem("Date Time") { /* TODO */ },
                SettingItem("Language") { /* TODO */ }
            )
        )
    )

    Scaffold(
        containerColor = LightGrayBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = PrimaryBlue
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(vertical = 16.dp) // Add padding for top/bottom of the scrollable list
                .verticalScroll(rememberScrollState()) // Make the column scrollable
        ) {
            settingsCategories.forEach { category ->
                SettingsCategoryCard(category = category)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) // Ensures Experimental API for Card is handled
@Composable
fun SettingsCategoryCard(category: SettingsCategory) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp), // Add horizontal padding for cards
        colors = CardDefaults.cardColors(containerColor = CardBackground), // Changed to cardColors
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.title,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = category.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkGrayText
                )
            }
            HorizontalDivider(color = LightGrayBackground, thickness = 1.dp)

            category.items.forEachIndexed { index, settingItem ->
                SettingRowItem(settingItem = settingItem)
                if (index < category.items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp), // Indent divider
                        color = LightGrayBackground.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) // Ensures Experimental API for ListItem is handled
@Composable
fun SettingRowItem(settingItem: SettingItem) {
    ListItem(
        headlineContent = {
            Text(
                text = settingItem.title,
                fontSize = 16.sp,
                color = DarkGrayText
            )
        },
        trailingContent = {
             Row(verticalAlignment = Alignment.CenterVertically) {
                settingItem.currentValue?.let {
                    Text(it, fontSize = 14.sp, color = DarkGrayText.copy(alpha = 0.7f))
                    Spacer(Modifier.width(8.dp))
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "Select ${settingItem.title}",
                    tint = DarkGrayText.copy(alpha = 0.5f)
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
        // onClick = settingItem.onClick // ListItem itself can be clickable
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740) // Corrected Preview annotation
@Composable
fun SettingsScreenPreview() {
    Lasertac2Theme {
        SettingsScreen(onNavigateBack = {})
    }
}
