package com.example

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.screens.DesktopEnvironment

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DesktopEnvironment(
                        getRealRamInfo = { getRealRamStats() },
                        getRealStorageInfo = { getRealStorageStats() }
                    )
                }
            }
        }
    }

    private fun getRealRamStats(): Pair<Long, Long> {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return Pair(memoryInfo.totalMem, memoryInfo.totalMem - memoryInfo.availMem)
    }

    private fun getRealStorageStats(): Pair<Long, Long> {
        val stat = StatFs(filesDir.absolutePath)
        val totalBytes = stat.blockCountLong * stat.blockSizeLong
        val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
        val usedBytes = totalBytes - freeBytes
        return Pair(totalBytes, usedBytes)
    }
}
