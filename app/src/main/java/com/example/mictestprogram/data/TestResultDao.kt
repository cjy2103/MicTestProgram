package com.example.mictestprogram.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TestResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: TestResultEntity)

    @Query("SELECT * FROM test_results ORDER BY testDateMillis DESC")
    fun observeAll(): Flow<List<TestResultEntity>>
}
