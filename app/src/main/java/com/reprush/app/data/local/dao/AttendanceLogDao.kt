package com.reprush.app.data.local.dao

import androidx.room.*
import com.reprush.app.data.local.entity.AttendanceLogEntity

@Dao
interface AttendanceLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceLogEntity)

    @Query("SELECT * FROM attendance_logs WHERE memberId = :memberId ORDER BY date DESC")
    suspend fun getAttendanceForMember(memberId: String): List<AttendanceLogEntity>

    @Query("SELECT * FROM attendance_logs WHERE date = :date")
    suspend fun getAttendanceForDate(date: String): List<AttendanceLogEntity>

    @Query("SELECT * FROM attendance_logs WHERE memberId = :memberId AND date = :date LIMIT 1")
    suspend fun getAttendanceForMemberOnDate(memberId: String, date: String): AttendanceLogEntity?
}
