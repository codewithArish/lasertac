package com.lasertrac.app

import androidx.compose.ui.graphics.Color

data class SnapDetail(
    val id: String,
    val regNr: String,
    val dateTime: String,
    val speed: Int,
    val speedLimit: Int,
    val location: String,
    val evidenceDate: String,
    val district: String,
    val policeStation: String,
    val address: String,
    val latitude: String,
    val longitude: String,
    val recordNr: String,
    val deviceId: String,
    val operatorId: String,
    val violationDistance: String,
    val uploadStatus: String,
    val status: SnapStatus,
    val mainImage: Any,
    val licensePlateImage: Any,
    val mapImage: Any,
    val violationSummary: String,
    val violationManagementLink: String,
    val accessLink: String,
    val regNrStatus: String
)

data class Violation(
    val actId: String,
    val actName: String
)

object ViolationRepository {
    private val defaultViolations = listOf(
        Violation("1110", "Overspeeding Two/Three Wheeler"),
        Violation("1111", "Overspeeding LMV"),
        Violation("1112", "Overspeeding HMV"),
        Violation("1114", "Wrong Side Two / Three Wheeler"),
        Violation("1114", "Wrong Side LMV"),
        Violation("1114", "Wrong Side HMV"),
        Violation("1115", "No Helmet")
    )

    private val _violations = androidx.compose.runtime.mutableStateListOf<Violation>().apply {
        addAll(defaultViolations)
    }

    val violations: List<Violation> get() = _violations

    fun addViolation(violation: Violation) {
        _violations.add(violation)
    }

    fun updateViolation(index: Int, violation: Violation) {
        if (index in _violations.indices) _violations[index] = violation
    }

    fun removeViolation(index: Int) {
        if (index in _violations.indices) _violations.removeAt(index)
    }
}

enum class SnapStatus(val displayName: String, val color: Color) {
    PENDING("Pending", Color(0xFFFFA726)),
    UPLOADED("Uploaded", Color(0xFF66BB6A)),
    REJECTED("Rejected", Color(0xFFEF5350)),
    UPDATED("Updated", Color(0xFF42A5F5))
}
