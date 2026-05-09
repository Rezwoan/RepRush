package com.reprush.app.data.local.dao

import androidx.room.*
import com.reprush.app.data.local.entity.MembershipPackageEntity

@Dao
interface MembershipPackageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackage(pkg: MembershipPackageEntity)

    @Query("SELECT * FROM membership_packages")
    suspend fun getAllPackages(): List<MembershipPackageEntity>

    @Query("SELECT * FROM membership_packages WHERE id = :packageId")
    suspend fun getPackageById(packageId: String): MembershipPackageEntity?

    @Update
    suspend fun updatePackage(pkg: MembershipPackageEntity)
}
