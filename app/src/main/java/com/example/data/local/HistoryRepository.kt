package com.example.data.local

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val printHistoryDao: PrintHistoryDao) {
    val allHistory: Flow<List<PrintHistoryEntity>> = printHistoryDao.getAllHistory()

    suspend fun insertRecord(record: PrintHistoryEntity): Long {
        return printHistoryDao.insert(record)
    }

    suspend fun deleteRecord(id: Long) {
        printHistoryDao.deleteById(id)
    }

    suspend fun clearAll() {
        printHistoryDao.clearAll()
    }
}
