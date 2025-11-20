package com.lasertrac.app

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.lasertrac.app.db.SavedSnapLocationEntity
import com.lasertrac.app.db.SnapDetail
import com.lasertrac.app.db.SnapLocationDao
import com.lasertrac.app.db.SnapStatus
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(private val snapLocationDao: SnapLocationDao) : ViewModel() {

    val allSnaps: LiveData<List<SnapDetail>> = snapLocationDao.getAllSnapLocations().map { entities ->
        entities.map { it.toSnapDetail() }
    }.asLiveData()

    fun deleteSnaps(snapIds: List<String>) {
        viewModelScope.launch {
            snapLocationDao.deleteSnapsByIds(snapIds)
        }
    }

    private fun SavedSnapLocationEntity.toSnapDetail(): SnapDetail {
        val currentDateTimeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(this.timestamp))
        val currentDateStr = currentDateTimeStr.substringBefore(" ")

        return SnapDetail(
            id = this.snapId,
            regNr = "", // Default empty, can be edited
            evidenceDate = currentDateStr,
            dateTime = currentDateTimeStr,
            status = SnapStatus.PENDING,
            speed = 0, // Placeholder
            deviceId = "", // Placeholder
            operatorId = "", // Placeholder
            speedLimit = 0, // Placeholder
            location = this.fullAddress ?: "N/A",
            violationDistance = "", // Placeholder
            recordNr = "REC-${this.snapId.take(4)}",
            latitude = this.latitude?.toString() ?: "N/A",
            longitude = this.longitude?.toString() ?: "N/A",
            district = this.district ?: "N/A",
            policeStation = this.selectedPoliceArea ?: "N/A",
            address = this.fullAddress ?: "N/A",
            uploadStatus = "Pending",
            mainImage = this.imageUri,
            licensePlateImage = this.imageUri,
            mapImage = null, // Placeholder
            violationSummary = "", // Placeholder
            violationManagementLink = "", // Placeholder
            accessLink = "", // Placeholder
            regNrStatus = "" // Placeholder
        )
    }
}

class MainViewModelFactory(private val snapLocationDao: SnapLocationDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(snapLocationDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
