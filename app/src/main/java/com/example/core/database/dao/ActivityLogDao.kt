package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ActivityLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityLogDao {

  @Query("SELECT * FROM activity_logs ORDER BY createdAt DESC LIMIT :limit")
  fun getRecentActivities(limit: Int = 50): Flow<List<ActivityLogEntity>>

  @Query("SELECT * FROM activity_logs WHERE entityId = :entityId OR details LIKE '%' || :entityId || '%' ORDER BY createdAt DESC LIMIT 50")
  fun getActivitiesForEntity(entityId: String): Flow<List<ActivityLogEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertActivityLog(log: ActivityLogEntity)
}
