package com.lasertrac.app.db

import androidx.compose.ui.graphics.Color

enum class SnapStatus(val displayName: String, val color: Color) {
    UPLOADED("Uploaded", Color(0xFF4CAF50)),
    PENDING("Pending", Color(0xFFFFA500)),
    FAILED("Failed", Color(0xFFF44336)),
    UPDATED("Updated", Color(0xFF2196F3)),
    REJECTED("Rejected", Color(0xFF6c757d))
}
