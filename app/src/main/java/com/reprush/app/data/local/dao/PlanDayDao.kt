package com.reprush.app.data.local.dao

import androidx.room.*
import com.reprush.app.data.local.entity.PlanDayEntity

@Dao
interface PlanDayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanDay(planDay: PlanDayEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(planDays: List<PlanDayEntity>)

    @Query("SELECT * FROM plan_days WHERE planId = :planId ORDER BY dayNumber ASC")
    suspend fun getDaysForPlan(planId: String): List<PlanDayEntity>
}
