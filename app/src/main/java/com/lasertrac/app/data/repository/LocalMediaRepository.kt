package com.lasertrac.app.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.lasertrac.app.db.SnapDetail
import com.lasertrac.app.db.SnapStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocalMediaRepository(private val context: Context) {

    suspend fun getLocalMediaSnaps(): Result<List<SnapDetail>> = withContext(Dispatchers.IO) {
        val snaps = mutableListOf<SnapDetail>()
        try {
            val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_MODIFIED
            )
            val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf("%Pictures/com.lasertrac%")
            val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val dateModified = cursor.getLong(dateModifiedColumn) * 1000 // Convert to milliseconds

                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    val dateTimeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(dateModified))
                    val dateStr = dateTimeStr.substringBefore(" ")

                    snaps.add(
                        SnapDetail(
                            id = "local_${id}",
                            regNr = "", // Local media has no registration number
                            evidenceDate = dateStr,
                            dateTime = dateTimeStr,
                            status = SnapStatus.PENDING, // Default status for local media
                            speed = 0,
                            deviceId = "",
                            operatorId = "",
                            speedLimit = 0,
                            location = name, // Use file name as location
                            violationDistance = "",
                            recordNr = "",
                            latitude = "",
                            longitude = "",
                            district = "",
                            policeStation = "",
                            address = "",
                            uploadStatus = "Pending",
                            mainImage = contentUri.toString(),
                            licensePlateImage = null,
                            mapImage = null,
                            violationSummary = "",
                            violationManagementLink = "",
                            accessLink = "",
                            regNrStatus = ""
                        )
                    )
                }
            }
            Result.success(snaps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
