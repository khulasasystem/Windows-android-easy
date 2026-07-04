package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "environment_variables")
data class EnvVar(
    @PrimaryKey val key: String,
    val value: String,
    val isSystem: Boolean = false
)
