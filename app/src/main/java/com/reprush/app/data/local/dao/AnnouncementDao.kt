package com.reprush.app.data.local.dao

import androidx.room.*
import com.reprush.app.data.local.entity.AnnouncementEntity

@Dao
interface AnnouncementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: AnnouncementEntity)

    @Query("SELECT * FROM announcements WHERE isActive = 1 ORDER BY createdAt DESC")
    suspend fun getActiveAnnouncements(): List<AnnouncementEntity>

    @Update
    suspend fun updateAnnouncement(announcement: AnnouncementEntity)
}
