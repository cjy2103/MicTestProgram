package com.example.mictestprogram.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TestResultEntity::class], version = 1, exportSchema = false)
abstract class TestDatabase : RoomDatabase() {

    abstract fun testResultDao(): TestResultDao

    companion object {
        @Volatile
        private var INSTANCE: TestDatabase? = null

        fun getDatabase(context: Context): TestDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    TestDatabase::class.java,
                    "mic_test_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
