package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// --- Models & Enums ---
enum class AppType {
    CHROME,
    SETTINGS,
    DIAGNOSTICS,
    TERMINAL,
    EXPLORER,
    PAINT,
    NOTEPAD,
    EXE_RUNNER,
    AI_AGENT
}

data class WindowState(
    val id: String,
    val title: String,
    val appType: AppType,
    val isMinimized: Boolean = false,
    val isMaximized: Boolean = false,
    val x: Float = 100f,
    val y: Float = 100f,
    val width: Float = 500f,
    val height: Float = 400f,
    val webTabs: List<String> = listOf("https://google.com"),
    val activeTabIndex: Int = 0,
    val filePath: String = "",
    val fileContent: String = ""
)

data class DesktopIcon(
    val title: String,
    val appType: AppType,
    val icon: ImageVector,
    val tint: Color
)

data class VirtualFile(
    val name: String,
    val isDirectory: Boolean,
    val content: String = "",
    val children: List<VirtualFile> = emptyList()
)

// --- Virtual File System Tree Helpers ---
fun VirtualFile.updateFileOrFolder(folderPath: List<String>, file: VirtualFile): VirtualFile {
    if (folderPath.isEmpty()) {
        val newChildren = this.children.filter { it.name != file.name } + file
        return this.copy(children = newChildren)
    }
    val nextDir = folderPath.first()
    val updatedChildren = this.children.map { child ->
        if (child.name == nextDir && child.isDirectory) {
            child.updateFileOrFolder(folderPath.drop(1), file)
        } else {
            child
        }
    }
    return this.copy(children = updatedChildren)
}

fun VirtualFile.deleteFileOrFolder(folderPath: List<String>, nameToDelete: String): VirtualFile {
    if (folderPath.isEmpty()) {
        return this.copy(children = this.children.filter { it.name != nameToDelete })
    }
    val nextDir = folderPath.first()
    val updatedChildren = this.children.map { child ->
        if (child.name == nextDir && child.isDirectory) {
            child.deleteFileOrFolder(folderPath.drop(1), nameToDelete)
        } else {
            child
        }
    }
    return this.copy(children = updatedChildren)
}

fun VirtualFile.resolvePath(path: List<String>): VirtualFile {
    var current = this
    for (segment in path) {
        val found = current.children.find { it.name == segment && it.isDirectory }
        if (found != null) {
            current = found
        } else {
            break
        }
    }
    return current
}

fun createDefaultFileSystem(): VirtualFile {
    return VirtualFile(
        name = "Root",
        isDirectory = true,
        children = listOf(
            VirtualFile(
                name = "Desktop",
                isDirectory = true,
                children = listOf(
                    VirtualFile("MatrixRain.exe", false, "SYSTEM_APP:MATRIX"),
                    VirtualFile("SnakeGame.exe", false, "SYSTEM_APP:SNAKE"),
                    VirtualFile("RetroMaze.exe", false, "SYSTEM_APP:MAZE"),
                    VirtualFile("DemoScript.exe", false, "print \"Starting test procedure...\"\ncolor cyan\nwait 500\nprint \"Platform: Android JVM Sandbox\"\nwait 800\nprint \"Preparing UI popup...\"\nwait 1000\nmsgbox \"Greetings from Notepad compiler! Your custom .EXE ran beautifully!\"\nprint \"Finished task successfully!\"\nbeep\ncolor green"),
                    VirtualFile("Readme.txt", false, "Welcome to OS Simulator 11!\n\nTo run EXE programs:\n1. Click 'File Explorer' and go to 'Desktop'. Double-click on any file ending with '.exe'.\n2. Open 'Notepad' from the start menu, write a custom sequence of actions, save as '.exe', and run it!\n3. Open 'OS Terminal' and type './MatrixRain.exe' or './SnakeGame.exe' to execute programs in command line mode.")
                )
            ),
            VirtualFile(
                name = "Documents",
                isDirectory = true,
                children = listOf(
                    VirtualFile("project_ideas.txt", false, "1. Build a customized VM interpreter.\n2. Design retro game engines.\n3. Implement local Room database for simulated filesystem."),
                    VirtualFile("todo_list.txt", false, "Optimize full-screen window tiles.\nIntegrate custom launcher icon.")
                )
            ),
            VirtualFile(
                name = "Downloads",
                isDirectory = true,
                children = listOf(
                    VirtualFile("chrome_installer.msi", false, "Binary stream data (simulated)"),
                    VirtualFile("wallpaper_aurora.png", false, "RGBA image pixel stream")
                )
            ),
            VirtualFile("system_check.log", false, "All physical cores running successfully. RAM synchronized. Local thread ID: 0x7FFA")
        )
    )
}

// --- Primary Entry Composable ---
@Composable
fun DesktopEnvironment(
    getRealRamInfo: () -> Pair<Long, Long>,
    getRealStorageInfo: () -> Pair<Long, Long>
) {
    // Systems States
    var isDarkTheme by remember { mutableStateOf(true) }
    var scaleFactor by remember { mutableFloatStateOf(1.0f) }
    var selectedWallpaper by remember { mutableStateOf("Aurora Blue") }
    var systemVolume by remember { mutableFloatStateOf(70f) }
    
    // Shared Virtual File System State
    var virtualRoot by remember { mutableStateOf(createDefaultFileSystem()) }
    
    // Window management
    var windows by remember { mutableStateOf(listOf<WindowState>()) }
    var activeWindowId by remember { mutableStateOf("") }

    // Helper to launch or bring window to front, supporting optional arguments
    val launchOrFocusApp = { appType: AppType, title: String, filePath: String, fileContent: String ->
        // For EXE_RUNNER, launch a separate window for different executable files
        val existing = windows.find {
            it.appType == appType && (appType != AppType.EXE_RUNNER || it.filePath == filePath)
        }
        if (existing != null) {
            windows = windows.map {
                if (it.id == existing.id) it.copy(isMinimized = false) else it
            }
            activeWindowId = existing.id
        } else {
            val newId = UUID.randomUUID().toString()
            val baseWidth = when (appType) {
                AppType.EXE_RUNNER -> 480f
                AppType.NOTEPAD -> 450f
                else -> 500f
            }
            val baseHeight = when (appType) {
                AppType.EXE_RUNNER -> 420f
                AppType.NOTEPAD -> 480f
                else -> 400f
            }
            windows = windows + WindowState(
                id = newId,
                title = title,
                appType = appType,
                filePath = filePath,
                fileContent = fileContent,
                width = baseWidth,
                height = baseHeight,
                x = 100f + (windows.size * 30f) % 200f,
                y = 100f + (windows.size * 30f) % 200f
            )
            activeWindowId = newId
        }
    }
    
    // UI Panels Toggles
    var isStartMenuOpen by remember { mutableStateOf(false) }
    var isQuickSettingsOpen by remember { mutableStateOf(false) }
    var isBiosScreen by remember { mutableStateOf(false) }
    
    // Virtual touchpad state (simulated internal mouse)
    var isTouchpadEnabled by remember { mutableStateOf(false) }
    var virtualCursorPosition by remember { mutableStateOf(Offset(500f, 400f)) }

    // Color definitions
    val desktopBgColor = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val panelBgColor = if (isDarkTheme) Color(0xDD1E293B) else Color(0xDDF1F5F9)
    val textColor = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val borderTint = if (isDarkTheme) Color(0x33FFFFFF) else Color(0x33000000)

    // Dynamic scale wrapper
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(desktopBgColor)
    ) {
        // --- Wallpaper Render ---
        WallpaperRenderer(selectedWallpaper, isDarkTheme)

        // --- Simulated BIOS Setup Utility ---
        if (isBiosScreen) {
            BiosSetupScreen(
                onExit = { isBiosScreen = false },
                isDarkTheme = isDarkTheme
            )
            return
        }

        // --- Main Desktop Surface ---
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val workspaceWidth = maxWidth
            val workspaceHeight = maxHeight - 48.dp // Space for Taskbar

            // Virtual cursor pointer tracking (Physical & External Mouse visualization)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (isTouchpadEnabled) {
                                    virtualCursorPosition = Offset(
                                        (virtualCursorPosition.x + dragAmount.x).coerceIn(0f, size.width.toFloat()),
                                        (virtualCursorPosition.y + dragAmount.y).coerceIn(0f, size.height.toFloat())
                                    )
                                }
                            }
                        )
                    }
            ) {
                // Desktop Shortcut Icons Grid
                val desktopIcons = listOf(
                    DesktopIcon("Google Chrome", AppType.CHROME, Icons.Default.Language, Color(0xFF4CAF50)),
                    DesktopIcon("Settings Panel", AppType.SETTINGS, Icons.Default.Settings, Color(0xFF9E9E9E)),
                    DesktopIcon("Diagnostics", AppType.DIAGNOSTICS, Icons.Default.Analytics, Color(0xFF2196F3)),
                    DesktopIcon("OS Terminal", AppType.TERMINAL, Icons.Default.Terminal, Color(0xFFE91E63)),
                    DesktopIcon("File Explorer", AppType.EXPLORER, Icons.Default.Folder, Color(0xFFFFC107)),
                    DesktopIcon("Canvas Paint", AppType.PAINT, Icons.Default.Brush, Color(0xFF9C27B0)),
                    DesktopIcon("Notepad Editor", AppType.NOTEPAD, Icons.Default.Description, Color(0xFF03A9F4)),
                    DesktopIcon("EXE Runner", AppType.EXE_RUNNER, Icons.Default.PlayArrow, Color(0xFF4CAF50)),
                    DesktopIcon("AI Copilot", AppType.AI_AGENT, Icons.Default.Face, Color(0xFF8E24AA))
                )

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(80.dp),
                    modifier = Modifier
                        .widthIn(max = 260.dp)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(desktopIcons) { item ->
                        DesktopIconWidget(
                            icon = item,
                            onClick = {
                                launchOrFocusApp(item.appType, item.title, "", "")
                            },
                            textColor = textColor
                        )
                    }
                }

                // --- Windows Workspace Layer ---
                windows.filter { !it.isMinimized }.forEach { window ->
                    WindowFrame(
                        window = window,
                        isActive = activeWindowId == window.id,
                        panelBgColor = panelBgColor,
                        textColor = textColor,
                        borderTint = borderTint,
                        onClose = {
                            windows = windows.filter { it.id != window.id }
                            if (activeWindowId == window.id) {
                                activeWindowId = windows.lastOrNull()?.id ?: ""
                            }
                        },
                        onMinimize = {
                            windows = windows.map {
                                if (it.id == window.id) it.copy(isMinimized = true) else it
                            }
                            if (activeWindowId == window.id) {
                                activeWindowId = windows.lastOrNull { !it.isMinimized }?.id ?: ""
                            }
                        },
                        onMaximizeToggle = {
                            windows = windows.map {
                                if (it.id == window.id) it.copy(isMaximized = !it.isMaximized) else it
                            }
                        },
                        onMove = { dx, dy ->
                            windows = windows.map {
                                if (it.id == window.id) {
                                    it.copy(
                                        x = (it.x + dx).coerceAtLeast(0f),
                                        y = (it.y + dy).coerceAtLeast(0f)
                                    )
                                } else it
                            }
                        },
                        onResize = { dw, dh ->
                            windows = windows.map {
                                if (it.id == window.id) {
                                    it.copy(
                                        width = (it.width + dw).coerceAtLeast(300f),
                                        height = (it.height + dh).coerceAtLeast(200f)
                                    )
                                } else it
                            }
                        },
                        onBringToFront = {
                            activeWindowId = window.id
                        },
                        onTileLeft = {
                            windows = windows.map {
                                if (it.id == window.id) {
                                    it.copy(
                                        isMaximized = false,
                                        x = 0f,
                                        y = 0f,
                                        width = 800f,
                                        height = 1200f
                                    )
                                } else it
                            }
                        },
                        onTileRight = {
                            windows = windows.map {
                                if (it.id == window.id) {
                                    it.copy(
                                        isMaximized = false,
                                        x = 800f,
                                        y = 0f,
                                        width = 800f,
                                        height = 1200f
                                    )
                                } else it
                            }
                        },
                        content = {
                            when (window.appType) {
                                AppType.CHROME -> ChromeAppContent(
                                    windowState = window,
                                    onUpdateTabs = { updated ->
                                        windows = windows.map {
                                            if (it.id == window.id) updated else it
                                        }
                                    }
                                )
                                AppType.SETTINGS -> SettingsAppContent(
                                    isDarkTheme = isDarkTheme,
                                    onThemeToggle = { isDarkTheme = !isDarkTheme },
                                    scaleFactor = scaleFactor,
                                    onScaleChange = { scaleFactor = it },
                                    selectedWallpaper = selectedWallpaper,
                                    onWallpaperChange = { selectedWallpaper = it },
                                    volume = systemVolume,
                                    onVolumeChange = { systemVolume = it },
                                    onEnterBios = { isBiosScreen = true },
                                    textColor = textColor
                                )
                                AppType.DIAGNOSTICS -> DiagnosticsAppContent(
                                    getRealRamInfo = getRealRamInfo,
                                    getRealStorageInfo = getRealStorageInfo,
                                    textColor = textColor
                                )
                                AppType.TERMINAL -> TerminalAppContent(
                                    virtualRoot = virtualRoot,
                                    onLaunchWindow = launchOrFocusApp,
                                    textColor = textColor
                                )
                                AppType.EXPLORER -> ExplorerAppContent(
                                    virtualRoot = virtualRoot,
                                    onUpdateFileSystem = { virtualRoot = it },
                                    onLaunchWindow = launchOrFocusApp,
                                    textColor = textColor
                                )
                                AppType.PAINT -> PaintAppContent()
                                AppType.AI_AGENT -> AIAgentAppContent(
                                    virtualRoot = virtualRoot,
                                    onUpdateFileSystem = { virtualRoot = it },
                                    onLaunchWindow = launchOrFocusApp,
                                    textColor = textColor
                                )
                                AppType.NOTEPAD -> NotepadAppContent(
                                    virtualRoot = virtualRoot,
                                    onUpdateFileSystem = { virtualRoot = it },
                                    onLaunchWindow = launchOrFocusApp,
                                    textColor = textColor
                                )
                                AppType.EXE_RUNNER -> ExeRunnerAppContent(
                                    fileName = window.filePath.ifEmpty { "unnamed.exe" },
                                    fileContent = window.fileContent,
                                    getRealRamInfo = getRealRamInfo,
                                    getRealStorageInfo = getRealStorageInfo,
                                    onCloseWindow = {
                                        windows = windows.filter { it.id != window.id }
                                        if (activeWindowId == window.id) {
                                            activeWindowId = windows.lastOrNull()?.id ?: ""
                                        }
                                    }
                                )
                            }
                        }
                    )
                }

                // Virtual Touchpad Cursor Visualization
                if (isTouchpadEnabled) {
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    virtualCursorPosition.x.roundToInt(),
                                    virtualCursorPosition.y.roundToInt()
                                )
                            }
                            .size(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Simulated Cursor",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // --- Start Menu Dialog Overlay ---
        if (isStartMenuOpen) {
            StartMenuDialog(
                onClose = { isStartMenuOpen = false },
                panelBgColor = panelBgColor,
                textColor = textColor,
                onLaunchApp = { appType, title ->
                    isStartMenuOpen = false
                    launchOrFocusApp(appType, title, "", "")
                }
            )
        }

        // --- Quick Settings Pane Dialog Overlay ---
        if (isQuickSettingsOpen) {
            QuickSettingsPane(
                onClose = { isQuickSettingsOpen = false },
                panelBgColor = panelBgColor,
                textColor = textColor,
                volume = systemVolume,
                onVolumeChange = { systemVolume = it },
                isTouchpadEnabled = isTouchpadEnabled,
                onToggleTouchpad = { isTouchpadEnabled = !isTouchpadEnabled }
            )
        }

        // --- Persistent Bottom Taskbar ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(48.dp)
                .background(panelBgColor)
                .border(1.dp, borderTint)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Side: Start Button & Launcher Utilities
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { isStartMenuOpen = !isStartMenuOpen },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Window,
                            contentDescription = "Start Button",
                            tint = Color(0xFF03A9F4),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Quick launch icon indicators
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        windows.forEach { win ->
                            val indicatorColor = if (activeWindowId == win.id) Color(0xFF03A9F4) else Color(0x889E9E9E)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (activeWindowId == win.id) Color(0x3303A9F4) else Color.Transparent)
                                    .clickable {
                                        if (win.isMinimized) {
                                            windows = windows.map {
                                                if (it.id == win.id) it.copy(isMinimized = false) else it
                                            }
                                            activeWindowId = win.id
                                        } else if (activeWindowId == win.id) {
                                            windows = windows.map {
                                                if (it.id == win.id) it.copy(isMinimized = true) else it
                                            }
                                            activeWindowId = windows.lastOrNull { !it.isMinimized }?.id ?: ""
                                        } else {
                                            activeWindowId = win.id
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = when (win.appType) {
                                            AppType.CHROME -> Icons.Default.Language
                                            AppType.SETTINGS -> Icons.Default.Settings
                                            AppType.DIAGNOSTICS -> Icons.Default.Analytics
                                            AppType.TERMINAL -> Icons.Default.Terminal
                                            AppType.EXPLORER -> Icons.Default.Folder
                                            AppType.PAINT -> Icons.Default.Brush
                                            AppType.NOTEPAD -> Icons.Default.Description
                                            AppType.EXE_RUNNER -> Icons.Default.PlayArrow
                                            AppType.AI_AGENT -> Icons.Default.Face
                                        },
                                        contentDescription = win.title,
                                        tint = when (win.appType) {
                                            AppType.CHROME -> Color(0xFF4CAF50)
                                            AppType.SETTINGS -> Color(0xFF9E9E9E)
                                            AppType.DIAGNOSTICS -> Color(0xFF2196F3)
                                            AppType.TERMINAL -> Color(0xFFE91E63)
                                            AppType.EXPLORER -> Color(0xFFFFC107)
                                            AppType.PAINT -> Color(0xFF9C27B0)
                                            AppType.NOTEPAD -> Color(0xFF03A9F4)
                                            AppType.EXE_RUNNER -> Color(0xFF4CAF50)
                                            AppType.AI_AGENT -> Color(0xFF8E24AA)
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(width = 12.dp, height = 3.dp)
                                            .clip(CircleShape)
                                            .background(indicatorColor)
                                    )
                                }
                            }
                        }
                    }
                }

                // Right Side: Quick Settings & Taskbar Clock Widget
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Quick stats pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDarkTheme) Color(0x33FFFFFF) else Color(0x11000000))
                            .clickable { isQuickSettingsOpen = !isQuickSettingsOpen }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Wi-Fi Status",
                            tint = textColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Icon(
                            imageVector = if (systemVolume == 0f) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = "Volume Status",
                            tint = textColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = "Battery Status",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Clock widget
                    TaskbarClock(textColor = textColor)
                }
            }
        }
    }
}

// --- Custom Wallpaper Canvas Background ---
@Composable
fun WallpaperRenderer(selectedWallpaper: String, isDarkTheme: Boolean) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        when (selectedWallpaper) {
            "Aurora Blue" -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = if (isDarkTheme) {
                            listOf(Color(0xFF0F172A), Color(0xFF1E3A8A), Color(0xFF0F172A))
                        } else {
                            listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE), Color(0xFFEFF6FF))
                        }
                    )
                )
                // Draw some dynamic aurora fluid waves
                val path1 = Path().apply {
                    moveTo(0f, height * 0.4f)
                    cubicTo(width * 0.3f, height * 0.3f, width * 0.7f, height * 0.6f, width, height * 0.5f)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                drawPath(
                    path = path1,
                    color = if (isDarkTheme) Color(0x1E0284C7) else Color(0x1E38BDF8)
                )
            }
            "Cosmic Neon" -> {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF3B0764), Color(0xFF0F051D)),
                        center = Offset(width * 0.5f, height * 0.5f),
                        radius = width * 0.8f
                    )
                )
                drawCircle(
                    color = Color(0x15EC4899),
                    radius = width * 0.3f,
                    center = Offset(width * 0.8f, height * 0.2f)
                )
            }
            "Minimalist Slate" -> {
                drawRect(
                    color = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                )
            }
        }
    }
}

// --- Desktop Icon Widget ---
@Composable
fun DesktopIconWidget(
    icon: DesktopIcon,
    onClick: () -> Unit,
    textColor: Color
) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(icon.tint.copy(alpha = 0.2f))
                .border(1.dp, icon.tint.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon.icon,
                contentDescription = icon.title,
                tint = icon.tint,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = icon.title,
            color = textColor,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium
        )
    }
}

// --- Floating Window Component ---
@Composable
fun WindowFrame(
    window: WindowState,
    isActive: Boolean,
    panelBgColor: Color,
    textColor: Color,
    borderTint: Color,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onMaximizeToggle: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onResize: (Float, Float) -> Unit,
    onBringToFront: () -> Unit,
    onTileLeft: () -> Unit,
    onTileRight: () -> Unit,
    content: @Composable () -> Unit
) {
    val cornerRadius = if (window.isMaximized) 0.dp else 12.dp
    val elevation = if (isActive) 16.dp else 4.dp
    val windowModifier = if (window.isMaximized) {
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(bottom = 48.dp) // Avoid covering taskbar
    } else {
        Modifier
            .offset { IntOffset(window.x.roundToInt(), window.y.roundToInt()) }
            .size(width = window.width.dp, height = window.height.dp)
    }

    Card(
        modifier = windowModifier
            .shadow(elevation, RoundedCornerShape(cornerRadius))
            .border(
                width = 1.dp,
                color = if (isActive) Color(0xFF03A9F4) else borderTint,
                shape = RoundedCornerShape(cornerRadius)
            )
            .clickable { onBringToFront() },
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = panelBgColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Window Header (Title Bar) with Drag gesture
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(if (isActive) Color(0x1503A9F4) else Color(0x05000000))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { onBringToFront() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (!window.isMaximized) {
                                    onMove(dragAmount.x, dragAmount.y)
                                }
                            }
                        )
                    }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Side: Window icon & title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (window.appType) {
                            AppType.CHROME -> Icons.Default.Language
                            AppType.SETTINGS -> Icons.Default.Settings
                            AppType.DIAGNOSTICS -> Icons.Default.Analytics
                            AppType.TERMINAL -> Icons.Default.Terminal
                            AppType.EXPLORER -> Icons.Default.Folder
                            AppType.PAINT -> Icons.Default.Brush
                            AppType.NOTEPAD -> Icons.Default.Description
                            AppType.EXE_RUNNER -> Icons.Default.PlayArrow
                            AppType.AI_AGENT -> Icons.Default.Face
                        },
                        contentDescription = "Window Icon",
                        tint = when (window.appType) {
                            AppType.CHROME -> Color(0xFF4CAF50)
                            AppType.SETTINGS -> Color(0xFF9E9E9E)
                            AppType.DIAGNOSTICS -> Color(0xFF2196F3)
                            AppType.TERMINAL -> Color(0xFFE91E63)
                            AppType.EXPLORER -> Color(0xFFFFC107)
                            AppType.PAINT -> Color(0xFF9C27B0)
                            AppType.NOTEPAD -> Color(0xFF03A9F4)
                            AppType.EXE_RUNNER -> Color(0xFF4CAF50)
                            AppType.AI_AGENT -> Color(0xFF8E24AA)
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = window.title,
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Right Side: Control action buttons (minimize, maximize, tile options, close)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Split screen tiling helper controls
                    IconButton(
                        onClick = onTileLeft,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AlignHorizontalLeft,
                            contentDescription = "Tile Left",
                            tint = textColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    IconButton(
                        onClick = onTileRight,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AlignHorizontalRight,
                            contentDescription = "Tile Right",
                            tint = textColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    
                    // Standard window actions
                    IconButton(
                        onClick = onMinimize,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Minimize,
                            contentDescription = "Minimize",
                            tint = textColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    IconButton(
                        onClick = onMaximizeToggle,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (window.isMaximized) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "Maximize",
                            tint = textColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Red,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Client Content Window Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(if (textColor == Color(0xFFF8FAFC)) Color(0xFF0F172A) else Color(0xFFFFFFFF))
            ) {
                content()

                // Corner resizing handle (only if not maximized)
                if (!window.isMaximized) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    onResize(dragAmount.x, dragAmount.y)
                                }
                            }
                    ) {
                        // Resizing grip visually
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            drawLine(
                                color = textColor.copy(alpha = 0.4f),
                                start = Offset(w * 0.5f, h),
                                end = Offset(w, h * 0.5f),
                                strokeWidth = 2f
                            )
                            drawLine(
                                color = textColor.copy(alpha = 0.4f),
                                start = Offset(w * 0.2f, h),
                                end = Offset(w, h * 0.2f),
                                strokeWidth = 2f
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Start Menu Dialog Dialog ---
@Composable
fun StartMenuDialog(
    onClose: () -> Unit,
    panelBgColor: Color,
    textColor: Color,
    onLaunchApp: (AppType, String) -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .width(420.dp)
                .height(480.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = panelBgColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Search Box
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = { Text("Search for apps, settings, documents...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    leadingIcon = { Icon(Icons.Default.Search, "Search") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Pinned Applications",
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                val apps = listOf(
                    Triple(AppType.CHROME, "Google Chrome", Icons.Default.Language),
                    Triple(AppType.SETTINGS, "Settings Panel", Icons.Default.Settings),
                    Triple(AppType.DIAGNOSTICS, "Diagnostics", Icons.Default.Analytics),
                    Triple(AppType.TERMINAL, "OS Terminal", Icons.Default.Terminal),
                    Triple(AppType.EXPLORER, "File Explorer", Icons.Default.Folder),
                    Triple(AppType.PAINT, "Canvas Paint", Icons.Default.Brush),
                    Triple(AppType.NOTEPAD, "Notepad Editor", Icons.Default.Description),
                    Triple(AppType.EXE_RUNNER, "EXE Runner", Icons.Default.PlayArrow),
                    Triple(AppType.AI_AGENT, "AI Copilot", Icons.Default.Face)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(apps) { (appType, name, icon) ->
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onLaunchApp(appType, name) }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = name,
                                tint = when (appType) {
                                    AppType.CHROME -> Color(0xFF4CAF50)
                                    AppType.SETTINGS -> Color(0xFF9E9E9E)
                                    AppType.DIAGNOSTICS -> Color(0xFF2196F3)
                                    AppType.TERMINAL -> Color(0xFFE91E63)
                                    AppType.EXPLORER -> Color(0xFFFFC107)
                                    AppType.PAINT -> Color(0xFF9C27B0)
                                    AppType.NOTEPAD -> Color(0xFF03A9F4)
                                    AppType.EXE_RUNNER -> Color(0xFF4CAF50)
                                    AppType.AI_AGENT -> Color(0xFF8E24AA)
                                },
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = name,
                                color = textColor,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Bottom strip: Profile avatar & Power Button
                Divider(color = textColor.copy(alpha = 0.1f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF03A9F4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text("Administrator", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.PowerSettingsNew, "Power Button", tint = Color.Red)
                    }
                }
            }
        }
    }
}

// --- Quick Settings Pane Composable ---
@Composable
fun QuickSettingsPane(
    onClose: () -> Unit,
    panelBgColor: Color,
    textColor: Color,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    isTouchpadEnabled: Boolean,
    onToggleTouchpad: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .width(360.dp)
                .height(280.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = panelBgColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Quick Control Settings",
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                // Quick buttons grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickToggleButton(
                        label = "Wi-Fi Connected",
                        icon = Icons.Default.Wifi,
                        isActive = true,
                        textColor = textColor,
                        onClick = {}
                    )
                    QuickToggleButton(
                        label = "Bluetooth On",
                        icon = Icons.Default.Bluetooth,
                        isActive = true,
                        textColor = textColor,
                        onClick = {}
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickToggleButton(
                        label = if (isTouchpadEnabled) "Virtual Mouse ON" else "Virtual Mouse OFF",
                        icon = Icons.Default.TouchApp,
                        isActive = isTouchpadEnabled,
                        textColor = textColor,
                        onClick = onToggleTouchpad
                    )
                    QuickToggleButton(
                        label = "Airplane Mode",
                        icon = Icons.Default.AirplaneTicket,
                        isActive = false,
                        textColor = textColor,
                        onClick = {}
                    )
                }

                // Volume slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Volume Level", color = textColor, fontSize = 12.sp)
                        Text("${volume.toInt()}%", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = volume,
                        onValueChange = onVolumeChange,
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.QuickToggleButton(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) Color(0xFF03A9F4) else Color(0x11888888))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) Color.White else textColor.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = if (isActive) Color.White else textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// --- Taskbar Clock Widget ---
@Composable
fun TaskbarClock(textColor: Color) {
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val cal = Calendar.getInstance()
            currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(cal.time)
            currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
            delay(1000)
        }
    }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = currentTime,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = currentDate,
            color = textColor.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
    }
}

// --- Real Chrome WebView Multi-Tab Browser ---
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ChromeAppContent(
    windowState: WindowState,
    onUpdateTabs: (WindowState) -> Unit
) {
    val activeUrl = windowState.webTabs[windowState.activeTabIndex]
    var urlInput by remember(activeUrl) { mutableStateOf(activeUrl) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab management bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE2E8F0))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            windowState.webTabs.forEachIndexed { idx, url ->
                val isActive = idx == windowState.activeTabIndex
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(if (isActive) Color.White else Color(0xFFCBD5E1))
                        .clickable { onUpdateTabs(windowState.copy(activeTabIndex = idx)) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = url.substringAfter("://").take(15),
                        fontSize = 11.sp,
                        color = Color.Black,
                        maxLines = 1
                    )
                    IconButton(
                        onClick = {
                            if (windowState.webTabs.size > 1) {
                                val updated = windowState.webTabs.toMutableList().apply { removeAt(idx) }
                                val newActive = if (windowState.activeTabIndex >= updated.size) updated.size - 1 else windowState.activeTabIndex
                                onUpdateTabs(windowState.copy(webTabs = updated, activeTabIndex = newActive))
                            }
                        },
                        modifier = Modifier.size(14.dp)
                    ) {
                        Icon(Icons.Default.Close, "Close Tab", tint = Color.Gray, modifier = Modifier.size(10.dp))
                    }
                }
            }

            IconButton(
                onClick = {
                    val updated = windowState.webTabs + "https://google.com"
                    onUpdateTabs(windowState.copy(webTabs = updated, activeTabIndex = updated.size - 1))
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.Add, "Add Tab", tint = Color.Black)
            }
        }

        // Navigation address bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { webViewRef?.goBack() },
                enabled = webViewRef?.canGoBack() == true,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            IconButton(
                onClick = { webViewRef?.goForward() },
                enabled = webViewRef?.canGoForward() == true,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.ArrowForward, "Forward")
            }
            IconButton(
                onClick = { webViewRef?.reload() },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Refresh, "Reload")
            }

            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF1F5F9),
                    unfocusedContainerColor = Color(0xFFF1F5F9),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(16.dp)
            )

            Button(
                onClick = {
                    val formatted = if (!urlInput.startsWith("http://") && !urlInput.startsWith("https://")) {
                        "https://$urlInput"
                    } else urlInput
                    val updated = windowState.webTabs.toMutableList().apply { set(windowState.activeTabIndex, formatted) }
                    onUpdateTabs(windowState.copy(webTabs = updated))
                    webViewRef?.loadUrl(formatted)
                },
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Go", fontSize = 12.sp)
            }
        }

        // Real WebView Render Area
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            url?.let {
                                urlInput = it
                                val updated = windowState.webTabs.toMutableList().apply { set(windowState.activeTabIndex, it) }
                                onUpdateTabs(windowState.copy(webTabs = updated))
                            }
                        }
                    }
                    webChromeClient = WebChromeClient()
                    loadUrl(activeUrl)
                    webViewRef = this
                }
            },
            update = { view ->
                if (view.url != activeUrl) {
                    view.loadUrl(activeUrl)
                }
            },
            modifier = Modifier.weight(1f)
        )
    }
}

// --- Settings App Composable ---
@Composable
fun SettingsAppContent(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    scaleFactor: Float,
    onScaleChange: (Float) -> Unit,
    selectedWallpaper: String,
    onWallpaperChange: (String) -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onEnterBios: () -> Unit,
    textColor: Color
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Virtual Environment Settings", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        // Theme Customization Toggle
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Color(0x22FFFFFF) else Color(0x11000000))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dark Display Theme", color = textColor, fontWeight = FontWeight.Bold)
                        Text("Toggles high-contrast night aesthetics", color = textColor.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                    Switch(checked = isDarkTheme, onCheckedChange = { onThemeToggle() })
                }
            }
        }

        // Wallpaper Selector Customization
        item {
            Column {
                Text("Simulated Wallpaper Selection", color = textColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Aurora Blue", "Cosmic Neon", "Minimalist Slate").forEach { paper ->
                        Button(
                            onClick = { onWallpaperChange(paper) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedWallpaper == paper) Color(0xFF03A9F4) else Color(0x33888888)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(paper, fontSize = 11.sp, color = if (selectedWallpaper == paper) Color.White else textColor)
                        }
                    }
                }
            }
        }

        // Resolution Scaling Scale Factor Customization
        item {
            Column {
                Text("Desktop Scaling Aspect", color = textColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = scaleFactor,
                    onValueChange = onScaleChange,
                    valueRange = 0.8f..1.2f,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Current Scale Multiplier: ${String.format("%.2f", scaleFactor)}x", color = textColor.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }

        // Simulated Volume Controls
        item {
            Column {
                Text("Audio Environment Volume", color = textColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..100f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Restart into BIOS mode
        item {
            Button(
                onClick = onEnterBios,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Shut Down & Enter BIOS Setup Utility", color = Color.White)
            }
        }
    }
}

// --- System Diagnostics Composable (Real RAM & Real Storage Usage) ---
@Composable
fun DiagnosticsAppContent(
    getRealRamInfo: () -> Pair<Long, Long>,
    getRealStorageInfo: () -> Pair<Long, Long>,
    textColor: Color
) {
    val ram = getRealRamInfo()
    val storage = getRealStorageInfo()

    // Real stats values format
    val totalRamGb = ram.first / (1024.0 * 1024.0 * 1024.0)
    val usedRamGb = ram.second / (1024.0 * 1024.0 * 1024.0)
    val ramUsagePct = (ram.second.toFloat() / ram.first.toFloat()) * 100f

    val totalStorageGb = storage.first / (1024.0 * 1024.0 * 1024.0)
    val usedStorageGb = storage.second / (1024.0 * 1024.0 * 1024.0)
    val storageUsagePct = (storage.second.toFloat() / storage.first.toFloat()) * 100f

    // Live CPU simulator thread
    var cpuActivityList by remember { mutableStateOf(List(20) { 15f }) }
    LaunchedEffect(Unit) {
        while (true) {
            val nextCpu = (5..85).random().toFloat()
            cpuActivityList = cpuActivityList.drop(1) + nextCpu
            delay(1000)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Real Hardware Diagnostics Panel", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Retrieving actual physical memory, storage, and device loads", color = textColor.copy(alpha = 0.6f), fontSize = 11.sp)
        }

        // RAM Widget
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Physical RAM Usage", color = textColor, fontWeight = FontWeight.Bold)
                    Text("${String.format("%.2f", usedRamGb)} GB / ${String.format("%.2f", totalRamGb)} GB (${ramUsagePct.toInt()}%)", color = textColor)
                }
                LinearProgressIndicator(
                    progress = usedRamGb.toFloat() / totalRamGb.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    color = Color(0xFF2196F3)
                )
            }
        }

        // Storage Widget
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Physical Storage Space", color = textColor, fontWeight = FontWeight.Bold)
                    Text("${String.format("%.1f", usedStorageGb)} GB / ${String.format("%.1f", totalStorageGb)} GB (${storageUsagePct.toInt()}%)", color = textColor)
                }
                LinearProgressIndicator(
                    progress = usedStorageGb.toFloat() / totalStorageGb.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    color = Color(0xFF4CAF50)
                )
            }
        }

        // Live CPU Load Waveform Graph
        item {
            Column {
                Text("Live CPU Usage Core Load Simulator", color = textColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Color(0xFF0F172A))
                ) {
                    val w = size.width
                    val h = size.height
                    val spacing = w / (cpuActivityList.size - 1)

                    // Draw gridlines
                    for (i in 1..3) {
                        val gridY = h * (i * 0.25f)
                        drawLine(Color(0x11FFFFFF), Offset(0f, gridY), Offset(w, gridY))
                    }

                    val path = Path().apply {
                        cpuActivityList.forEachIndexed { idx, value ->
                            val xPos = idx * spacing
                            val yPos = h - (value / 100f * h)
                            if (idx == 0) moveTo(xPos, yPos) else lineTo(xPos, yPos)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF00E676),
                        style = Stroke(width = 3f)
                    )
                }
            }
        }
    }
}

// --- Paint Canvas Drawing App Composable ---
@Composable
fun PaintAppContent() {
    var lines by remember { mutableStateOf(listOf<List<Offset>>()) }
    var currentLine by remember { mutableStateOf(listOf<Offset>()) }
    var brushColor by remember { mutableStateOf(Color.Black) }
    var brushWidth by remember { mutableFloatStateOf(6f) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Painting controls toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE2E8F0))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Color picker options
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(Color.Black, Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Magenta).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (brushColor == color) 2.dp else 1.dp,
                                color = if (brushColor == color) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { brushColor = color }
                    )
                }
            }

            // Erase clear canvas action
            Button(
                onClick = {
                    lines = emptyList()
                    currentLine = emptyList()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text("Clear Canvas", fontSize = 11.sp, color = Color.White)
            }
        }

        // Draw Canvas Board
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentLine = listOf(offset)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val nextOffset = currentLine.last() + dragAmount
                            currentLine = currentLine + nextOffset
                        },
                        onDragEnd = {
                            lines = lines + listOf(currentLine)
                            currentLine = emptyList()
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw all past path lines
                lines.forEach { line ->
                    if (line.size > 1) {
                        val path = Path().apply {
                            moveTo(line.first().x, line.first().y)
                            line.drop(1).forEach { offset ->
                                lineTo(offset.x, offset.y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = brushColor,
                            style = Stroke(width = brushWidth)
                        )
                    }
                }

                // Draw active drawing line
                if (currentLine.size > 1) {
                    val path = Path().apply {
                        moveTo(currentLine.first().x, currentLine.first().y)
                        currentLine.drop(1).forEach { offset ->
                            lineTo(offset.x, offset.y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = brushColor,
                        style = Stroke(width = brushWidth)
                    )
                }
            }
        }
    }
}

// --- BIOS Setup Utility Screen Composable ---
@Composable
fun BiosSetupScreen(onExit: () -> Unit, isDarkTheme: Boolean) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Main Setup", "Advanced Configuration", "System Security", "Boot Manager", "Licensing", "Save & Exit")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0000AA)) // Classic solid blue BIOS look!
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Text(
                text = "Phoenix SecureCore(tm) Setup Utility",
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Blue BIOS Tab selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFCCCCCC))
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                tabs.forEachIndexed { idx, tab ->
                    Text(
                        text = tab,
                        color = if (selectedTab == idx) Color(0xFF0000AA) else Color.Black,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(if (selectedTab == idx) Color.White else Color.Transparent)
                            .clickable { selectedTab = idx }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Content options based on selected tab
            Column(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Color.White)
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> { // Main Setup
                        BiosRow("System Time", "07:48:10")
                        BiosRow("System Date", "07/09/2026")
                        BiosRow("CPU Model", "Intel Core(TM) i9 3.4GHz")
                        BiosRow("System RAM", "16384 MB")
                        BiosRow("BIOS Version", "v1.5.0-all-jadx")
                    }
                    1 -> { // Advanced
                        BiosRow("Hardware Virtualization", "[ENABLED]")
                        BiosRow("Storage Controller Mode", "[AHCI]")
                        BiosRow("Graphic Memory Size", "[512 MB]")
                    }
                    2 -> { // Security
                        BiosRow("Supervisor Password", "[NOT INSTALLED]")
                        BiosRow("Secure Boot State", "[DISABLED]")
                    }
                    3 -> { // Boot
                        BiosRow("Boot Order 1", "[SATA SSD]")
                        BiosRow("Boot Order 2", "[USB Drive]")
                    }
                    4 -> { // Licensing
                        BiosRow("License Status", "[VERIFIED GENUINE]")
                        BiosRow("AI Studio Applet ID", "7735994a-c59f-4223")
                    }
                    5 -> { // Exit
                        Button(
                            onClick = onExit,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text("Reboot OS Environment Now", color = Color(0xFF0000AA), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Bottom controls strip help
            Divider(color = Color.White, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("F1: Help   Esc: Exit   Arrows: Select Item", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("F10: Save & Exit", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun BiosRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}
