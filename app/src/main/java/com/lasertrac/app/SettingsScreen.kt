package com.lasertrac.app

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lasertrac.app.ui.theme.Lasertac2Theme
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        containerColor = Color(0xFF0D0D0D),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Settings", color = Color.White, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A).copy(alpha = 0.9f)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Live Interval
            GlassCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Live Interval", color = Color.White, fontSize = 16.sp)
                    Spacer(Modifier.width(12.dp))
                    var text by remember { mutableStateOf("2") }

                    TextField(
                        value = text,
                        onValueChange = { newValue -> text = newValue },
                        textStyle = LocalTextStyle.current.copy(color = Color.White),
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = Color.Transparent,
                            focusedIndicatorColor = Color.White,
                            unfocusedIndicatorColor = Color.Gray
                        ),
                        modifier = Modifier.width(80.dp)
                    )
                    Text("(Sec)", color = Color.Gray, fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    GlassButton(text = "UPDATE")
                }
            }

            // Load Data (Date Wise) with DatePicker
            GlassCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    var selectedDate by remember { mutableStateOf("06-10-2025") }

                    val calendar = Calendar.getInstance()
                    val datePickerDialog = DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            selectedDate = "%02d-%02d-%04d".format(dayOfMonth, month + 1, year)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    )

                    // Disable future dates - set max date to today
                    datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

                    OutlinedButton(
                        onClick = { datePickerDialog.show() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(selectedDate, fontSize = 15.sp)
                    }

                    Spacer(Modifier.weight(1f))
                    GlassButton(text = "LOAD DATA DATE WISE")
                }
            }

            // Load Data (Month Wise) with Month-Year Picker Dialog
            GlassCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    var selectedMonthYear by remember { mutableStateOf("Oct 2025") }
                    var showMonthYearDialog by remember { mutableStateOf(false) }

                    OutlinedButton(
                        onClick = { showMonthYearDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(selectedMonthYear, fontSize = 15.sp)
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown",
                            tint = Color.White
                        )
                    }

                    if (showMonthYearDialog) {
                        MonthYearPickerDialog(
                            initialSelection = selectedMonthYear,
                            onDismiss = { showMonthYearDialog = false },
                            onConfirm = { monthYear ->
                                selectedMonthYear = monthYear
                                showMonthYearDialog = false
                            }
                        )
                    }

                    Spacer(Modifier.weight(1f))
                    GlassButton(text = "LOAD DATA MONTH WISE")
                }
            }

            // Delete Old Data (with dropdown)
            DeleteOldDataCard()
        }
    }
}

@Composable
fun MonthYearPickerDialog(
    initialSelection: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    val years = (2023..2030).toList()

    var selectedMonth by remember { mutableStateOf(initialSelection.split(" ")[0]) }
    var selectedYear by remember { mutableStateOf(initialSelection.split(" ")[1].toInt()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1A1A1A)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Select Month & Year", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Month Dropdown
                    var monthExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { monthExpanded = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Text(selectedMonth)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = monthExpanded,
                            onDismissRequest = { monthExpanded = false }
                        ) {
                            months.forEach { month ->
                                DropdownMenuItem(
                                    text = { Text(month) },
                                    onClick = {
                                        selectedMonth = month
                                        monthExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Year Dropdown
                    var yearExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { yearExpanded = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Text(selectedYear.toString())
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = yearExpanded,
                            onDismissRequest = { yearExpanded = false }
                        ) {
                            years.forEach { year ->
                                DropdownMenuItem(
                                    text = { Text(year.toString()) },
                                    onClick = {
                                        selectedYear = year
                                        yearExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = Color.Gray)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { onConfirm("$selectedMonth $selectedYear") }) {
                        Text("OK", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.06f))
                )
            )
            .padding(16.dp),
        content = content
    )
}

@Composable
fun GlassButton(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.08f))
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun GlassDangerButton(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color.Red.copy(alpha = 0.35f), Color.Red.copy(alpha = 0.15f))
                )
            )
            .border(
                width = 1.dp,
                color = Color.Red.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun DeleteOldDataCard() {
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf("3 Months") }
    val options = listOf(
        "3 Months","4 Months","5 Months","6 Months",
        "7 Months","8 Months","9 Months","10 Months",
        "11 Months","12 Months"
    )

    GlassCard {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Delete data older than:",
                    color = Color.White,
                    fontSize = 16.sp
                )
                Spacer(Modifier.width(8.dp))

                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            selectedOption,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown",
                            tint = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedOption = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                GlassDangerButton(text = "DELETE OLD DATA")
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun SettingsScreenPreview() {
    Lasertac2Theme {
        SettingsScreen(onNavigateBack = {})
    }
}
