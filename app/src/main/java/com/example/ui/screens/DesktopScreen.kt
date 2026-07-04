package com.example.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.EnvVar
import com.example.data.VirtualFile
import com.example.ui.OsType
import com.example.ui.UiState
import com.example.ui.VirtualApp
import com.example.ui.VirtualNotification
import com.example.ui.VirtualSystemViewModel
import com.example.ui.WindowState
import com.example.ui.WindowType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DesktopScreen(
    viewModel: VirtualSystemViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1D))
    ) {
        if (uiState.currentOs == OsType.BOOTLOADER) {
            BootloaderScreen(viewModel = viewModel)
        } else {
            DesktopEnvironment(viewModel = viewModel, uiState = uiState)
        }

        AnimatedVisibility(
            visible = uiState.isBooting,
            enter = fadeIn(animationSpec = tween(400)),
            exit = fadeOut(animationSpec = tween(400))
        ) {
            BootScreenOverlay(uiState = uiState)
        }
    }
}

@Composable
fun BootloaderScreen(viewModel: VirtualSystemViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedOs by remember { mutableStateOf(OsType.WIN11) }
    
    var cores by remember { mutableStateOf(uiState.cpuCores) }
    var ram by remember { mutableStateOf(uiState.ramSizeGb) }
    var driver by remember { mutableStateOf(uiState.graphicsDriver) }
    var dxvk by remember { mutableStateOf(uiState.isDxvkEnabled) }
    var esync by remember { mutableStateOf(uiState.isEsyncEnabled) }

    val driversList = listOf("Turnip+Zink (Recommended)", "VirGL Simulator", "LLVMpipe Software Renderer")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF070B19), Color(0xFF141E30))
                )
            )
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "BIOS",
                            tint = Color(0xFF00FFCC),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "HYBRID-BIOS x86_64 BOOTLOADER v4.2",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Divider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))
                    
                    Text(
                        text = "Please select the Guest Operating System to boot into:",
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    listOf(
                        OsType.WIN11 to "Windows 11 Pro [Virtual Environment Layer]",
                        OsType.WIN10 to "Windows 10 Pro [Optimized & Stripped]",
                        OsType.WIN7 to "Windows 7 Ultimate [Legacy Compatibility Mod]",
                        OsType.KALI to "Kali Linux Rolling [Penetration Auditing Bash Suite]"
                    ).forEach { (os, label) ->
                        val isSelected = selectedOs == os
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { selectedOs = os }
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.08f),
                                    RoundedCornerShape(8.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF1F2D44) else Color(0xFF101625)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when(os) {
                                            OsType.KALI -> Icons.Default.Build
                                            else -> Icons.Default.Home
                                        },
                                        contentDescription = "OS",
                                        tint = if (isSelected) Color(0xFF00FFCC) else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else Color.LightGray,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                if (isSelected) {
                                    Text(
                                        text = "[ACTIVE]",
                                        color = Color(0xFF00FFCC),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Press enter or double-click to boot instantly with selected system configuration.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1424)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Specs",
                                tint = Color(0xFFFFA500),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "VIRTUAL HARDWARE SPECS",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 10.dp))

                        Text(
                            text = "CPU Core Allocation: $cores Cores",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(2, 4, 6, 8).forEach { c ->
                                Button(
                                    onClick = { cores = c },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (cores == c) Color(0xFFFFA500) else Color(0xFF1E283D)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("$c", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Virtual System RAM: $ram GB",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(4, 8, 12, 16).forEach { r ->
                                Button(
                                    onClick = { ram = r },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (ram == r) Color(0xFFFFA500) else Color(0xFF1E283D)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("${r}G", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Vulkan Graphics Driver Swap:",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Column {
                            driversList.forEach { drv ->
                                val active = driver == drv
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { driver = drv }
                                        .padding(vertical = 4.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = active,
                                        onClick = { driver = drv },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFFA500))
                                    )
                                    Text(
                                        text = drv,
                                        color = if (active) Color.White else Color.Gray,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = dxvk,
                                    onCheckedChange = { dxvk = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFFA500))
                                )
                                Text("DXVK Layer", color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = esync,
                                    onCheckedChange = { esync = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFFA500))
                                )
                                Text("Wine-Esync", color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.setSpecifications(cores, ram, driver, dxvk, esync)
                            viewModel.bootSelectedOs(selectedOs)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "BOOT SYSTEM NOW",
                            color = Color(0xFF070B19),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BootScreenOverlay(uiState: UiState) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .testTag("boot_loader"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Virtual Machine Boot",
                tint = Color(0xFF00FFCC),
                modifier = Modifier
                    .size(80.dp)
                    .rotate(rotation)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "HYBRID HYPERVISOR VIRTUAL MACHINE",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Initializing AMD64 Container Translation Layer...",
                color = Color.Gray,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            
            Spacer(modifier = Modifier.height(30.dp))
            
            LinearProgressIndicator(
                progress = uiState.bootProgress,
                color = Color(0xFF00FFCC),
                trackColor = Color.White.copy(alpha = 0.1f),
                modifier = Modifier
                    .width(280.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "${(uiState.bootProgress * 100).roundToInt()}% Ready",
                color = Color(0xFF00FFCC),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun DesktopEnvironment(viewModel: VirtualSystemViewModel, uiState: UiState) {
    val scope = rememberCoroutineScope()
    val wallpaperRes = R.drawable.img_win11_wallpaper
    
    val desktopBackground = if (uiState.currentOs == OsType.KALI) {
        Brush.radialGradient(
            colors = listOf(Color(0xFF1F0D2B), Color(0xFF08030C))
        )
    } else {
        Brush.linearGradient(
            colors = when (uiState.currentOs) {
                OsType.WIN10 -> listOf(Color(0xFF0B214F), Color(0xFF020715))
                OsType.WIN7 -> listOf(Color(0xFF1B5AA7), Color(0xFF00112A))
                else -> listOf(Color(0xFF1F4068), Color(0xFF162447))
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(desktopBackground)
    ) {
        if (uiState.currentOs != OsType.KALI) {
            Image(
                painter = painterResource(id = wallpaperRes),
                contentDescription = "Desktop Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.85f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(200.dp)
                .padding(top = 20.dp, start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            listOf(
                ShortcutItem(WindowType.CHROME, "Google Chrome", Icons.Default.Share, Color(0xFF00E676)),
                ShortcutItem(WindowType.EXPLORER, "File Explorer", Icons.Default.Home, Color(0xFFFFC107)),
                ShortcutItem(WindowType.CMD, if (uiState.currentOs == OsType.KALI) "Bash Terminal" else "Command Prompt", Icons.Default.List, Color(0xFF90A4AE)),
                ShortcutItem(WindowType.SETTINGS, if (uiState.currentOs == OsType.KALI) "Sysconfig" else "Control Panel", Icons.Default.Settings, Color(0xFF00B0FF)),
                ShortcutItem(WindowType.PACK_INSTALLER, if (uiState.currentOs == OsType.KALI) "APT Installer" else "Software Center", Icons.Default.Add, Color(0xFFE040FB)),
                ShortcutItem(WindowType.PRINTER_SCANNER, "Peripherals manager", Icons.Default.Print, Color(0xFF00E5FF))
            ).forEach { item ->
                DesktopIcon(
                    title = item.title,
                    icon = item.icon,
                    iconColor = item.iconColor,
                    onClick = { viewModel.openWindow(item.type) }
                )
            }
        }

        uiState.windows.filter { !it.isMinimized }.forEach { window ->
            key(window.type) {
                val isActive = uiState.activeWindow == window.type
                WindowWrapper(
                    window = window,
                    isActive = isActive,
                    currentOs = uiState.currentOs,
                    onFocus = { viewModel.focusWindow(window.type) },
                    onMinimize = { viewModel.minimizeWindow(window.type) },
                    onMaximizeToggle = { viewModel.toggleMaximizeWindow(window.type) },
                    onClose = { viewModel.closeWindow(window.type) },
                    onMove = { dx, dy ->
                        viewModel.updateWindowPosition(
                            window.type,
                            (window.x + dx).roundToInt(),
                            (window.y + dy).roundToInt()
                        )
                    }
                ) {
                    when (window.type) {
                        WindowType.CMD -> TerminalAppContent(viewModel = viewModel, uiState = uiState)
                        WindowType.EXPLORER -> FileExplorerAppContent(viewModel = viewModel, uiState = uiState)
                        WindowType.SETTINGS -> SettingsAppContent(viewModel = viewModel, uiState = uiState)
                        WindowType.CHROME -> GoogleChromeAppContent(viewModel = viewModel, uiState = uiState)
                        WindowType.NOTEPAD -> NotepadAppContent(viewModel = viewModel, uiState = uiState)
                        WindowType.TASK_MANAGER -> TaskManagerAppContent(viewModel = viewModel, uiState = uiState)
                        WindowType.PRINTER_SCANNER -> PrinterScannerAppContent(viewModel = viewModel, uiState = uiState)
                        WindowType.PACK_INSTALLER -> PackageInstallerAppContent(viewModel = viewModel, uiState = uiState)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.isStartMenuOpen,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
            modifier = Modifier
                .align(
                    if (uiState.currentOs == OsType.KALI) Alignment.TopStart 
                    else if (uiState.currentOs == OsType.WIN11) Alignment.BottomCenter 
                    else Alignment.BottomStart
                )
                .padding(
                    top = if (uiState.currentOs == OsType.KALI) 48.dp else 0.dp,
                    bottom = if (uiState.currentOs != OsType.KALI) 54.dp else 0.dp,
                    start = if (uiState.currentOs == OsType.WIN11 || uiState.currentOs == OsType.KALI) 16.dp else 0.dp
                )
        ) {
            StartMenuDialog(viewModel = viewModel, uiState = uiState)
        }

        AnimatedVisibility(
            visible = uiState.isKeyboardOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(200.dp)
        ) {
            VirtualKeyboardPanel(viewModel = viewModel, uiState = uiState)
        }

        if (uiState.isVirtualMouseActive) {
            VirtualMousePointerAndGuide(viewModel = viewModel, uiState = uiState)
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 58.dp, end = 16.dp)
                .width(320.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.notifications.filter { !it.isRead }.take(3).forEach { notification ->
                    NotificationToast(notification = notification, onDismiss = {
                        viewModel.markNotificationRead(notification.id)
                    })
                }
            }
        }

        if (uiState.currentOs == OsType.KALI) {
            KaliTopPanel(viewModel = viewModel, uiState = uiState)
        } else {
            WindowsBottomTaskbar(viewModel = viewModel, uiState = uiState)
        }
    }
}

@Composable
fun DesktopIcon(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black,
                    offset = Offset(1f, 1f),
                    blurRadius = 4f
                )
            )
        )
    }
}

@Composable
fun WindowWrapper(
    window: WindowState,
    isActive: Boolean,
    currentOs: OsType,
    onFocus: () -> Unit,
    onMinimize: () -> Unit,
    onMaximizeToggle: () -> Unit,
    onClose: () -> Unit,
    onMove: (Float, Float) -> Unit,
    content: @Composable () -> Unit
) {
    val cornerRadius = when(currentOs) {
        OsType.WIN11 -> 12.dp
        OsType.KALI -> 6.dp
        else -> 0.dp
    }

    val headerBg = when(currentOs) {
        OsType.KALI -> Color(0xFF1E1E24)
        OsType.WIN7 -> Color(0xFF4C87C9).copy(alpha = 0.85f)
        else -> if (isActive) Color(0xFF1C2230) else Color(0xFF141924)
    }

    val borderStroke = if (isActive) {
        BorderStroke(1.dp, Color(0xFF00FFCC).copy(alpha = 0.6f))
    } else {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    }

    val modifier = if (window.isMaximized) {
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(bottom = if (currentOs == OsType.KALI) 0.dp else 48.dp, top = if (currentOs == OsType.KALI) 32.dp else 0.dp)
    } else {
        Modifier
            .offset { IntOffset(window.x, window.y) }
            .size(window.width.dp, window.height.dp)
    }

    Card(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onFocus()
                }
            }
            .clip(RoundedCornerShape(cornerRadius))
            .border(borderStroke, RoundedCornerShape(cornerRadius)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101420)),
        shape = RoundedCornerShape(cornerRadius)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(headerBg)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onMove(dragAmount.x, dragAmount.y)
                        }
                    }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when(window.type) {
                            WindowType.CHROME -> Icons.Default.Share
                            WindowType.EXPLORER -> Icons.Default.Home
                            WindowType.CMD -> Icons.Default.List
                            WindowType.NOTEPAD -> Icons.Default.Edit
                            WindowType.TASK_MANAGER -> Icons.Default.Refresh
                            else -> Icons.Default.Settings
                        },
                        contentDescription = "App Icon",
                        tint = if (isActive) Color(0xFF00FFCC) else Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = window.title,
                        color = if (isActive) Color.White else Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onMinimize, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Minimize", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                    }
                    IconButton(onClick = onMaximizeToggle, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Maximize", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF0C0F17))
            ) {
                content()
            }
        }
    }
}

@Composable
fun GoogleChromeAppContent(viewModel: VirtualSystemViewModel, uiState: UiState) {
    var webUrl by remember { mutableStateOf("https://www.google.com") }
    var searchInput by remember { mutableStateOf("") }
    var currentRenderMode by remember { mutableStateOf("google_home") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(Color(0xFF1E2436))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { currentRenderMode = "google_home" }, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = { currentRenderMode = "google_home"; webUrl = "https://www.google.com" }, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = "Home", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF00E676).copy(alpha = 0.15f)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "DESKTOP USER-AGENT DETECTED (Win64; x64)",
                    color = Color(0xFF00FFCC),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            BasicTextField(
                value = webUrl,
                onValueChange = { webUrl = it },
                textStyle = TextStyle(color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0D1117))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
            
            Button(
                onClick = {
                    if (webUrl.contains("github.com")) {
                        currentRenderMode = "github"
                    } else if (webUrl.contains("speedtest")) {
                        currentRenderMode = "speedtest"
                    } else {
                        currentRenderMode = "google_results"
                        searchInput = webUrl.substringAfter("q=").substringBefore("&")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("GO", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF10141E))
        ) {
            when (currentRenderMode) {
                "google_home" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Google",
                            color = Color.White,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        
                        OutlinedTextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            placeholder = { Text("Search Google or type a URL", color = Color.Gray, fontSize = 12.sp) },
                            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                            singleLine = true,
                            modifier = Modifier
                                .width(450.dp)
                                .height(54.dp),
                            shape = RoundedCornerShape(27.dp),
                            trailingIcon = {
                                IconButton(onClick = {
                                    webUrl = "https://www.google.com/search?q=$searchInput"
                                    currentRenderMode = "google_results"
                                }) {
                                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color.Gray)
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = {
                                webUrl = "https://www.google.com/search?q=$searchInput"
                                currentRenderMode = "google_results"
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202124))) {
                                Text("Google Search", fontSize = 11.sp, color = Color.LightGray)
                            }
                            Button(onClick = {
                                webUrl = "https://github.com"
                                currentRenderMode = "github"
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202124))) {
                                Text("I'm Feeling Lucky", fontSize = 11.sp, color = Color.LightGray)
                            }
                        }
                    }
                }
                "google_results" -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        item {
                            Text("Google Search Results for '$searchInput':", color = Color(0xFF8AB4F8), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                        items(listOf(
                            "Windows 11 x86_64 Emulator on Android - GitHub" to "How to compile box64, wine-esync, and dxvk on Snapdragon device with vulkan hardware acceleration...",
                            "Turnip Zink graphic driver setups" to "Get maximum hardware acceleration inside containerized PROOT layers using Adreno GPU mapping guides...",
                            "Learn Kali Linux penetration suite" to "Deploy advanced nmap scanners, wireshark packet decryptors and brute-force tools safely locally...",
                            "How to bypass website mobile detection" to "Configure your User-Agent string hardcoded to mimic standard Windows NT 10.0 architecture."
                        )) { (title, snippet) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable {
                                        if (title.contains("GitHub")) {
                                            webUrl = "https://github.com"
                                            currentRenderMode = "github"
                                        }
                                    },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E212E))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = title, color = Color(0xFF8AB4F8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = snippet, color = Color.LightGray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                "github" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Github Logo", tint = Color.White, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("GitHub Open-Source Sim", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))
                        
                        Text("Search repositories: 'winlator-box64-translation'", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        
                        listOf(
                            "ptitSeb/box64" to "Linux Userspace x86_64 Emulator with hardware acceleration for ARM64 devices.",
                            "doitsujin/dxvk" to "Vulkan-based translation layer for Direct3D 9/10/11 running on Linux / Wine.",
                            "Mesa3D/turnip-driver" to "Adreno Vulkan driver mapped inside rootfs container.",
                            "wine-esync-fsync" to "Wine with Eventfd synchronization patches for ultimate frame rates."
                        ).forEach { (repo, desc) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(repo, color = Color(0xFF58A6FF), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(desc, color = Color.LightGray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                "speedtest" -> {
                    var testRunning by remember { mutableStateOf(false) }
                    var speedResult by remember { mutableStateOf("") }
                    var speedPercent by remember { mutableStateOf(0f) }

                    LaunchedEffect(testRunning) {
                        if (testRunning) {
                            speedPercent = 0f
                            for (i in 1..10) {
                                delay(200)
                                speedPercent = i * 0.1f
                            }
                            speedResult = "TEST COMPLETED SUCCESSFULLY\n\nDownload: 874.5 Mbps\nUpload: 432.1 Mbps\nLatency Ping: 8 ms\nHost Provider: Snapdragon Virtual NIC Gateway\nHardware Acceleration: ACTIVE"
                            testRunning = false
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Network Bandwidth SpeedTest (Real Android Connection)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        if (speedResult.isEmpty()) {
                            Button(
                                onClick = { testRunning = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                                enabled = !testRunning
                            ) {
                                Text(if (testRunning) "Evaluating Network Speed..." else "RUN SPEED TEST", color = Color.Black)
                            }
                            if (testRunning) {
                                Spacer(modifier = Modifier.height(14.dp))
                                LinearProgressIndicator(progress = speedPercent, color = Color(0xFF00FFCC), modifier = Modifier.width(200.dp))
                            }
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
                                modifier = Modifier.width(360.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(speedResult, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(onClick = { speedResult = "" }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                                        Text("RETEST", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalAppContent(viewModel: VirtualSystemViewModel, uiState: UiState) {
    var cmdInput by remember { mutableStateOf("") }
    val isKali = uiState.currentOs == OsType.KALI
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.terminalHistory.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    val terminalBg = if (isKali) Color(0xFF0D0314) else Color(0xFF000000)
    val textStyle = TextStyle(
        color = if (isKali) Color(0xFF00FF33) else Color.White,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(terminalBg)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            Column {
                uiState.terminalHistory.forEach { line ->
                    Text(text = line, style = textStyle, modifier = Modifier.padding(vertical = 1.dp))
                }
            }
        }

        Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isKali) "root@kali:~# " else "${uiState.terminalCurrentDir}>",
                style = textStyle,
                fontWeight = FontWeight.Bold
            )
            BasicTextField(
                value = cmdInput,
                onValueChange = { cmdInput = it },
                textStyle = textStyle,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val prompt = cmdInput
                    cmdInput = ""
                    viewModel.executeCommand(prompt)
                }),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
                    .testTag("terminal_input")
            )
        }
    }
}

@Composable
fun FileExplorerAppContent(viewModel: VirtualSystemViewModel, uiState: UiState) {
    var editModeContent by remember { mutableStateOf<String?>(null) }
    var viewingFilePath by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(Color(0xFF1E2538))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { viewModel.navigateUpExplorer() }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Up Folder", tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = uiState.explorerPath,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { viewModel.startCreateFolder() }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "New Folder", tint = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { viewModel.startCreateFile() }) {
                    Icon(imageVector = Icons.Default.Create, contentDescription = "New File", tint = Color(0xFFFFA500), modifier = Modifier.size(16.dp))
                }
                if (uiState.selectedFile != null) {
                    IconButton(onClick = { viewModel.deleteSelectedFile() }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete File", tint = Color.Red, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        if (uiState.isCreatingFolder || uiState.isCreatingFile) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161A26))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (uiState.isCreatingFolder) "Folder Name:" else "File Name:",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                BasicTextField(
                    value = uiState.editedFileName,
                    onValueChange = { viewModel.updateEditedFileName(it) },
                    textStyle = TextStyle(color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.Black)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(6.dp)
                )
                Button(
                    onClick = { viewModel.executeCreation() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text("OK", color = Color.Black, fontSize = 11.sp)
                }
                Button(
                    onClick = { viewModel.cancelCreation() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text("Cancel", color = Color.White, fontSize = 11.sp)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(
                modifier = Modifier
                    .width(150.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF131826))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Drives & Shares", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                
                if (uiState.currentOs != OsType.KALI) {
                    listOf(
                        "C:" to "Local Disk (C:)",
                        "D:" to "USB Drive (D:)",
                        "E:" to "Backup SD (E:)"
                    ).forEach { (path, label) ->
                        Text(
                            text = label,
                            color = if (uiState.explorerPath.startsWith(path)) Color(0xFF00FFCC) else Color.LightGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.navigateToFolder(path) }
                                .padding(6.dp)
                        )
                    }
                } else {
                    listOf(
                        "/" to "Root (/) ",
                        "/root" to "root directory",
                        "/etc" to "etc configs"
                    ).forEach { (path, label) ->
                        Text(
                            text = label,
                            color = if (uiState.explorerPath == path) Color(0xFF00FFCC) else Color.LightGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.navigateToFolder(path) }
                                .padding(6.dp)
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF0C101A))) {
                if (uiState.explorerFiles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("This folder is empty.", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 90.dp),
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.explorerFiles) { file ->
                            val isSelected = uiState.selectedFile?.path == file.path
                            Column(
                                modifier = Modifier
                                    .width(80.dp)
                                    .clickable { viewModel.selectFile(file) }
                                    .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                    .padding(6.dp)
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, _ ->
                                            change.consume()
                                        }
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (file.isDirectory) Icons.Default.Home else Icons.Default.Edit,
                                    contentDescription = file.name,
                                    tint = if (file.isDirectory) Color(0xFFFFC107) else Color(0xFF81D4FA),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable {
                                            if (file.isDirectory) {
                                                viewModel.navigateToFolder(file.path)
                                            } else {
                                                if (file.name.endsWith(".apk")) {
                                                    viewModel.handleApkInstallError()
                                                } else {
                                                    viewModel.openWindow(WindowType.NOTEPAD)
                                                }
                                            }
                                        }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = file.name,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotepadAppContent(viewModel: VirtualSystemViewModel, uiState: UiState) {
    val file = uiState.selectedFile
    var text by remember { mutableStateOf("") }
    
    LaunchedEffect(file) {
        if (file != null && !file.isDirectory) {
            text = file.content
        } else {
            text = "Welcome to Notepad.\nSelect a text file in File Explorer to begin editing."
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(Color(0xFF1C2230))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = file?.name ?: "untitled.txt",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            if (file != null) {
                Button(
                    onClick = { viewModel.saveFileContent(file.path, text) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text("SAVE FILE", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        BasicTextField(
            value = text,
            onValueChange = { text = it },
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F121E))
                .padding(12.dp)
        )
    }
}

@Composable
fun SettingsAppContent(viewModel: VirtualSystemViewModel, uiState: UiState) {
    var specTabActive by remember { mutableStateOf(true) }
    var newKey by remember { mutableStateOf("") }
    var newVal by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color(0xFF151926))
        ) {
            Button(
                onClick = { specTabActive = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (specTabActive) Color(0xFF1E253A) else Color.Transparent
                ),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(0.dp)
            ) {
                Text("Specs Allocation", fontSize = 12.sp, color = Color.White)
            }
            Button(
                onClick = { specTabActive = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!specTabActive) Color(0xFF1E253A) else Color.Transparent
                ),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(0.dp)
            ) {
                Text("Environment Paths", fontSize = 12.sp, color = Color.White)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF0D101A))
                .padding(16.dp)
        ) {
            if (specTabActive) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("ACTIVE VIRTUAL ARCHITECTURE:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF171D2F)), modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("CPU Cores Limit", color = Color.Gray, fontSize = 11.sp)
                                Text("${uiState.cpuCores} Cores Allocated", color = Color(0xFFFFA500), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF171D2F)), modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Allocated RAM", color = Color.Gray, fontSize = 11.sp)
                                Text("${uiState.ramSizeGb} GB RAM", color = Color(0xFF00FFCC), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF171D2F)), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Active Display Acceleration Drivers", color = Color.Gray, fontSize = 11.sp)
                            Text(uiState.graphicsDriver, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Translators: DXVK ${if (uiState.isDxvkEnabled) "Active" else "Off"} | Wine-Esync ${if (uiState.isEsyncEnabled) "Active" else "Off"}", color = Color.LightGray, fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = { viewModel.rebootToBootloader() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SHUT DOWN & OPEN BIOS", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Column {
                    Text("Advanced Environment Variables Manager:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = newKey,
                            onValueChange = { newKey = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.Black)
                                .border(1.dp, Color.White.copy(alpha = 0.2f))
                                .padding(6.dp)
                        )
                        Text("=", color = Color.White)
                        BasicTextField(
                            value = newVal,
                            onValueChange = { newVal = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                            modifier = Modifier
                                .weight(1.5f)
                                .background(Color.Black)
                                .border(1.dp, Color.White.copy(alpha = 0.2f))
                                .padding(6.dp)
                        )
                        Button(
                            onClick = {
                                if (newKey.trim().isNotEmpty()) {
                                    viewModel.executeCommand("SETX ${newKey.trim()} ${newVal.trim()}")
                                    newKey = ""
                                    newVal = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("SET", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(uiState.envVars) { variable ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${variable.key} = ${variable.value}", color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    if (!variable.isSystem) {
                                        IconButton(onClick = { viewModel.executeCommand("SETX ${variable.key} ") }, modifier = Modifier.size(16.dp)) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskManagerAppContent(viewModel: VirtualSystemViewModel, uiState: UiState) {
    val infiniteTransition = rememberInfiniteTransition(label = "performance_pulse")
    val cpuPulse by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cpu"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("PERFORMANCE MONITOR (Live virtualization load)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF101726)), modifier = Modifier.weight(1f).fillMaxHeight()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("CPU Core Usage", color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        val points = listOf(0.1f, 0.25f, 0.18f, 0.35f, 0.22f, cpuPulse, cpuPulse * 1.2f)
                        val stepX = size.width / (points.size - 1)
                        
                        for (i in 1..4) {
                            val y = size.height * (i * 0.2f)
                            drawLine(Color.White.copy(alpha = 0.05f), start = Offset(0f, y), end = Offset(size.width, y))
                        }

                        for (i in 0 until points.size - 1) {
                            val startX = i * stepX
                            val startY = size.height * (1f - points[i])
                            val endX = (i + 1) * stepX
                            val endY = size.height * (1f - points[i + 1])
                            drawLine(Color(0xFF00FFCC), start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = 4f)
                        }
                    }
                    Text("Total Load: ${(cpuPulse * 100).roundToInt()}% (${uiState.cpuCores} Cores Active)", color = Color(0xFF00FFCC), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF101726)), modifier = Modifier.weight(1f).fillMaxHeight()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("System Virtual Memory", color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        val points = listOf(0.35f, 0.35f, 0.36f, 0.35f, 0.37f, 0.37f, 0.38f)
                        val stepX = size.width / (points.size - 1)
                        
                        for (i in 1..4) {
                            val y = size.height * (i * 0.2f)
                            drawLine(Color.White.copy(alpha = 0.05f), start = Offset(0f, y), end = Offset(size.width, y))
                        }

                        for (i in 0 until points.size - 1) {
                            val startX = i * stepX
                            val startY = size.height * (1f - points[i])
                            val endX = (i + 1) * stepX
                            val endY = size.height * (1f - points[i + 1])
                            drawLine(Color(0xFFFFA500), start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = 4f)
                        }
                    }
                    val memoryUsed = (uiState.ramSizeGb * 0.38f)
                    Text("RAM Used: ${String.format("%.1f", memoryUsed)} GB / ${uiState.ramSizeGb} GB", color = Color(0xFFFFA500), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun PrinterScannerAppContent(viewModel: VirtualSystemViewModel, uiState: UiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Print, contentDescription = "peripherals", tint = Color(0xFF00FFCC), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("VIRTUAL USB PRINTER & SCANNER SUITE", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF131826)), modifier = Modifier.weight(1.5f).fillMaxHeight()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Integrated Scanner Driver (USB-Port-1)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (uiState.isScanning) {
                            Text("Hardware Feed scanning document...", color = Color(0xFF00FFCC), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(progress = uiState.scanProgress, color = Color(0xFF00FFCC), modifier = Modifier.fillMaxWidth())
                        } else if (uiState.scannedDocResult != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .background(Color.Black)
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(uiState.scannedDocResult, color = Color.LightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        } else {
                            Text("Ready to scan virtual physical documents or hardware templates.", color = Color.Gray, fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = { viewModel.runVirtualScanner() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                        enabled = !uiState.isScanning,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("START SCANNING TASK", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF131826)), modifier = Modifier.weight(1f).fillMaxHeight()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Active PDF Printer (LPT-1)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (uiState.isPrinting) {
                            Text("Rendering virtual spool task...", color = Color(0xFFFFA500), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(progress = uiState.printProgress, color = Color(0xFFFFA500), modifier = Modifier.fillMaxWidth())
                        } else {
                            Text("Printer state: IDLE\nResolution: 600 DPI\nPaper limit: Unlimited", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Button(
                        onClick = { viewModel.runVirtualPrinter("virtual_notes.pdf") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA500)),
                        enabled = !uiState.isPrinting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("TEST PRINT FILE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun PackageInstallerAppContent(viewModel: VirtualSystemViewModel, uiState: UiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = if (uiState.currentOs == OsType.KALI) "APT CLOUD PACKAGE INSTALLATION CENTER" else "WINDOWS NATIVE REPOSITORY STORE (WINGET)",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.availableApps) { app ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151C2C)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(app.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.width(6.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                                ) {
                                    Text(app.size, color = Color.LightGray, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(app.description, color = Color.LightGray, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.installSimulatedApp(app.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (app.isInstalled) Color.Gray else Color(0xFF00FFCC)
                            ),
                            enabled = !app.isInstalled,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(if (app.isInstalled) "INSTALLED" else "INSTALL", color = if (app.isInstalled) Color.White else Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StartMenuDialog(viewModel: VirtualSystemViewModel, uiState: UiState) {
    Card(
        modifier = Modifier
            .width(420.dp)
            .height(340.dp)
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141926).copy(alpha = 0.95f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search apps", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Type here to search apps, files or configs...", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))

                Text("Pinned Desktop Tools", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(listOf(
                        "File Explorer" to WindowType.EXPLORER,
                        "Command Prompt" to WindowType.CMD,
                        "Google Chrome" to WindowType.CHROME,
                        "Task Manager" to WindowType.TASK_MANAGER,
                        "Peripherals" to WindowType.PRINTER_SCANNER,
                        "Notepad Text" to WindowType.NOTEPAD
                    )) { (label, type) ->
                        Card(
                            modifier = Modifier
                                .clickable { viewModel.openWindow(type) }
                                .height(56.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F293D)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(label, color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFA500)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("A", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Admin_User", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.toggleVirtualKeyboard() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F293D)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("KEYBOARD", fontSize = 9.sp, color = Color.LightGray)
                    }
                    Button(
                        onClick = { viewModel.toggleVirtualMouse() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F293D)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("MOUSE GUIDE", fontSize = 9.sp, color = Color.LightGray)
                    }
                    Button(
                        onClick = { viewModel.rebootToBootloader() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("SHUTDOWN", fontSize = 9.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun WindowsBottomTaskbar(viewModel: VirtualSystemViewModel, uiState: UiState) {
    val currentTime = remember { mutableStateOf("") }
    val currentDate = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val sdfTime = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val sdfDate = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            currentTime.value = sdfTime.format(Date())
            currentDate.value = sdfDate.format(Date())
            delay(15000)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color(0xFF0F121E).copy(alpha = 0.95f))
            .border(1.dp, Color.White.copy(alpha = 0.08f)),
        color = Color(0xFF0F121E).copy(alpha = 0.95f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val alignValue = if (uiState.currentOs == OsType.WIN11) Alignment.Center else Alignment.CenterStart
            
            Row(
                modifier = Modifier
                    .align(alignValue)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { viewModel.toggleStartMenu() }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Start Button Menu",
                        tint = if (uiState.isStartMenuOpen) Color(0xFF00FFCC) else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                listOf(
                    WindowType.CHROME to Icons.Default.Share,
                    WindowType.EXPLORER to Icons.Default.Home,
                    WindowType.CMD to Icons.Default.List,
                    WindowType.SETTINGS to Icons.Default.Settings,
                    WindowType.TASK_MANAGER to Icons.Default.Refresh,
                    WindowType.PRINTER_SCANNER to Icons.Default.Print
                ).forEach { (type, icon) ->
                    val isOpen = uiState.windows.any { it.type == type }
                    val isActive = uiState.activeWindow == type
                    
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { viewModel.openWindow(type) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "App Indicator",
                            tint = if (isActive) Color(0xFF00FFCC) else if (isOpen) Color.White else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        if (isOpen) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 2.dp)
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(if (isActive) Color(0xFF00FFCC) else Color.Gray)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = { viewModel.toggleVirtualKeyboard() }, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Onscreen Keyboard", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                }

                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "WiFi Network Status",
                    tint = if (uiState.isWifiConnected) Color(0xFF00FFCC) else Color.Red,
                    modifier = Modifier.size(16.dp)
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = currentTime.value, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(text = currentDate.value, color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun KaliTopPanel(viewModel: VirtualSystemViewModel, uiState: UiState) {
    val currentTime = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            currentTime.value = sdf.format(Date())
            delay(1000)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(Color(0xFF1E1E24))
            .border(1.dp, Color(0xFF32323A)),
        color = Color(0xFF1E1E24)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { viewModel.toggleStartMenu() }, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Menu, contentDescription = "Kali applications", tint = Color(0xFF00FF33), modifier = Modifier.size(18.dp))
                }
                Text("Applications", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("CPU: [x86_64]", color = Color(0xFF00FF33), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text(currentTime.value, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun NotificationToast(notification: VirtualNotification, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDismiss() }
            .border(1.dp, Color(0xFF00FFCC).copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121824).copy(alpha = 0.95f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = notification.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = notification.message, color = Color.LightGray, fontSize = 11.sp)
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = Color.Gray, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
fun VirtualMousePointerAndGuide(viewModel: VirtualSystemViewModel, uiState: UiState) {
    var pointerX by remember { mutableStateOf(uiState.mouseX) }
    var pointerY by remember { mutableStateOf(uiState.mouseY) }

    LaunchedEffect(uiState.mouseX, uiState.mouseY) {
        pointerX = uiState.mouseX
        pointerY = uiState.mouseY
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(pointerX.roundToInt(), pointerY.roundToInt()) }
            .size(16.dp)
            .clip(CircleShape)
            .background(Color.Red)
            .border(2.dp, Color.White, CircleShape)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 60.dp, start = 16.dp)
                .size(120.dp)
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        viewModel.moveVirtualMouse(dragAmount.x * 1.5f, dragAmount.y * 1.5f)
                    }
                },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131722).copy(alpha = 0.85f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("VIRTUAL", color = Color.LightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("TOUCHPAD", color = Color(0xFF00FFCC), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Drag to guide mouse", color = Color.Gray, fontSize = 8.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun VirtualKeyboardPanel(viewModel: VirtualSystemViewModel, uiState: UiState) {
    val keyboardRows = listOf(
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L", "BKSP"),
        listOf("Z", "X", "C", "V", "B", "N", "M", "SPACE", "ENTER")
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color(0xFF0F131E).copy(alpha = 0.95f))
            .border(1.dp, Color.White.copy(alpha = 0.1f)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F131E).copy(alpha = 0.95f)),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("INTEGRATED DESKTOP KEYBOARD GUIDE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                IconButton(onClick = { viewModel.toggleVirtualKeyboard() }, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close Keyboard", tint = Color.LightGray)
                }
            }

            keyboardRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { key ->
                        val isSpecial = key == "BKSP" || key == "ENTER" || key == "SPACE"
                        val keyWidth = if (key == "SPACE") 180.dp else if (isSpecial) 60.dp else 40.dp
                        
                        Card(
                            modifier = Modifier
                                .width(keyWidth)
                                .height(38.dp)
                                .padding(horizontal = 2.dp)
                                .clickable {
                                    // Custom visual feedback
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSpecial) Color(0xFF1E263D) else Color(0xFF28324C)
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = key, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper Shortcut representation
data class ShortcutItem(
    val type: WindowType,
    val title: String,
    val icon: ImageVector,
    val iconColor: Color
)
