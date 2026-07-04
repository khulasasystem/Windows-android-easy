package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VirtualFileDao {
    @Query("SELECT * FROM virtual_files")
    fun getAllFilesFlow(): Flow<List<VirtualFile>>

    @Query("SELECT * FROM virtual_files")
    suspend fun getAllFiles(): List<VirtualFile>

    @Query("SELECT * FROM virtual_files WHERE parentPath = :parentPath")
    suspend fun getFilesByParent(parentPath: String): List<VirtualFile>

    @Query("SELECT * FROM virtual_files WHERE path = :path LIMIT 1")
    suspend fun getFileByPath(path: String): VirtualFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: VirtualFile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<VirtualFile>)

    @Query("DELETE FROM virtual_files WHERE path = :path")
    suspend fun deleteFileByPath(path: String)

    @Query("DELETE FROM virtual_files WHERE path LIKE :pathPrefix || '%'")
    suspend fun deleteFilesByPrefix(pathPrefix: String)

    @Query("SELECT COUNT(*) FROM virtual_files")
    suspend fun getFileCount(): Int
}
