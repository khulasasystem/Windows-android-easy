package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "virtual_files")
data class VirtualFile(
    @PrimaryKey val path: String, // e.g. "C:\Users\Administrator\Desktop\README.txt"
    val name: String,
    val isDirectory: Boolean,
    val content: String = "",
    val parentPath: String // e.g. "C:\Users\Administrator\Desktop"
)
