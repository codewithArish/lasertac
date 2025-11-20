package com.lasertrac.app.repository

import com.lasertrac.app.db.Violation
import com.lasertrac.app.db.ViolationDao
import kotlinx.coroutines.flow.Flow

class ViolationRepository(private val violationDao: ViolationDao) {

    fun getAllViolations(): Flow<List<Violation>> = violationDao.getAllViolations()

    suspend fun insert(violation: Violation) {
        violationDao.insert(violation)
    }

    suspend fun update(violation: Violation) {
        violationDao.update(violation)
    }

    suspend fun deleteViolation(violationId: Int) {
        violationDao.deleteViolation(violationId)
    }
}
