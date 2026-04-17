package com.example.mictestprogram.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test_results")
data class TestResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val testDateMillis: Long,
    val totalCount: Int,
    val correctCount: Int,
    val accuracy: Double,
    val details: String
)
