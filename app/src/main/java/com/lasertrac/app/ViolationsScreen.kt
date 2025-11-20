package com.lasertrac.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lasertrac.app.db.Violation
import com.lasertrac.app.db.ViolationDao
import com.lasertrac.app.ui.theme.TextColorLight
import com.lasertrac.app.ui.theme.TopBarColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViolationsScreen(onNavigateBack: () -> Unit, violationDao: ViolationDao) {
    val violations by violationDao.getAllViolations().collectAsState(initial = emptyList())
    var showDialog by remember { mutableStateOf(false) }
    var editingViolation by remember { mutableStateOf<Violation?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Violations", color = TextColorLight) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextColorLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TopBarColor)
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sr Violations (Acts)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Button(
                    onClick = {
                        editingViolation = null
                        showDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1B5E20)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ADD NEW", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(violations) { index, violation ->
                    ViolationItem(
                        violation = violation,
                        index = index + 1,
                        onEditClick = {
                            editingViolation = violation
                            showDialog = true
                        }
                    )
                }
            }
        }

        if (showDialog) {
            ViolationDialog(
                violation = editingViolation,
                onDismiss = { showDialog = false },
                onConfirm = {
                    coroutineScope.launch {
                        if (editingViolation == null) {
                            violationDao.insert(it)
                        } else {
                            violationDao.update(it)
                        }
                    }
                },
                 onDelete = {
                    coroutineScope.launch {
                       violationDao.deleteViolation(it.id)
                    }
                }
            )
        }
    }
}

@Composable
fun ViolationItem(
    violation: Violation,
    index: Int,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A2A)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            Color(0xFF1B5E20),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = index.toString(),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "${violation.title} ${violation.description}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = Color(0xFF4A90E2),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ViolationDialog(
    violation: Violation?,
    onDismiss: () -> Unit,
    onConfirm: (Violation) -> Unit,
    onDelete: (Violation) -> Unit
) {
    var title by remember { mutableStateOf(violation?.title ?: "") }
    var description by remember { mutableStateOf(violation?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (violation != null) "Edit Act/Violation" else "Add Act/Violation",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Act ID", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF4A4A4A),
                        unfocusedBorderColor = Color(0xFF3A3A3A),
                        cursorColor = Color.White,
                        focusedLabelColor = Color.Gray,
                        unfocusedLabelColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Act Name", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF4A4A4A),
                        unfocusedBorderColor = Color(0xFF3A3A3A),
                        cursorColor = Color.White,
                        focusedLabelColor = Color.Gray,
                        unfocusedLabelColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (violation != null) {
                    Button(
                        onClick = {
                            onDelete(violation)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Delete")
                    }
                }

                Button(
                    onClick = {
                        val newViolation = violation?.copy(
                            title = title,
                            description = description
                        ) ?: Violation(
                            title = title,
                            description = description,
                            timestamp = System.currentTimeMillis()
                        )
                        onConfirm(newViolation)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1B5E20)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (violation != null) "Update" else "Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}
