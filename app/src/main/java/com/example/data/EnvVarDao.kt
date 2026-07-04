package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EnvVarDao {
    @Query("SELECT * FROM environment_variables")
    fun getAllEnvVarsFlow(): Flow<List<EnvVar>>

    @Query("SELECT * FROM environment_variables")
    suspend fun getAllEnvVars(): List<EnvVar>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnvVar(envVar: EnvVar)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnvVars(envVars: List<EnvVar>)

    @Query("DELETE FROM environment_variables WHERE `key` = :key")
    suspend fun deleteEnvVar(key: String)

    @Query("SELECT COUNT(*) FROM environment_variables")
    suspend fun getCount(): Int
}
