package com.example.mictestprogram.data

class TestRepository(private val dao: TestResultDao) {
    fun observeHistory() = dao.observeAll()

    suspend fun saveResult(result: TestResultEntity) {
        dao.insert(result)
    }
}
