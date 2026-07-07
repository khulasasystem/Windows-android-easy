package com.example.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import kotlin.OptIn
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.zIndex
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
import com.example.ui.DiskPartition
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
    var showProActivationDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1D))
    ) {
        if (uiState.currentOs == OsType.BOOTLOADER) {
            BootloaderScreen(viewModel = viewModel)
        } else if (uiState.isSetupInProcess) {
            OobeSetupScreen(
                viewModel = viewModel,
                uiState = uiState,
                modifier = Modifier.fillMaxSize()
            )
        } else if (uiState.isLocked) {
            LoginLockScreen(
                viewModel = viewModel,
                uiState = uiState,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            DesktopEnvironment(
                viewModel = viewModel,
                uiState = uiState,
                onProFeatureLocked = { showProActivationDialog = true }
            )
        }

        AnimatedVisibility(
            visible = uiState.isBooting,
            enter = fadeIn(animationSpec = tween(400)),
            exit = fadeOut(animationSpec = tween(400))
        ) {
            BootScreenOverlay(uiState = uiState)
        }

        if (showProActivationDialog) {
            ProActivationDialog(
                viewModel = viewModel,
                onClose = { showProActivationDialog = false }
            )
        }

        GlobalFloatingInputDock(viewModel = viewModel, uiState = uiState)
    }
}

@Composable
fun GlobalFloatingInputDock(
    viewModel: VirtualSystemViewModel,
    uiState: UiState
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(9999f),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .offset(x = if (isExpanded) 0.dp else 125.dp)
                .animateContentSize()
                .background(
                    color = Color(0xFF090F1E).copy(alpha = 0.95f),
                    shape = RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFF00FFCC).copy(alpha = 0.6f),
                    shape = RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
                )
                .padding(vertical = 6.dp, horizontal = 12.dp)
                .clickable { if (!isExpanded) isExpanded = true }
        ) {
            // Expand/Collapse Handle
            IconButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFF1E293B), CircleShape)
                    .border(1.dp, Color(0xFF00FFCC), CircleShape)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ArrowForward else Icons.Default.Menu,
                    contentDescription = "Expand Dock",
                    tint = Color(0xFF00FFCC),
                    modifier = Modifier.size(16.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(10.dp))
            
            if (isExpanded) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Keyboard toggle button
                    FloatingDockButton(
                        icon = Icons.Default.Edit,
                        label = "KEYBOARD",
                        isActive = uiState.isKeyboardOpen,
                        activeColor = Color(0xFF00FFCC),
                        onClick = { viewModel.toggleVirtualKeyboard() }
                    )
                    
                    // Mouse toggle button
                    FloatingDockButton(
                        icon = Icons.Default.PlayArrow,
                        label = "MOUSE",
                        isActive = uiState.isVirtualMouseActive,
                        activeColor = Color(0xFF00FFCC),
                        onClick = { viewModel.toggleVirtualMouse() }
                    )
                    
                    // Pro status/activate button
                    FloatingDockButton(
                        icon = if (uiState.isProActivated) Icons.Default.CheckCircle else Icons.Default.Warning,
                        label = if (uiState.isProActivated) "PRO ON" else "ACTIVATE",
                        isActive = uiState.isProActivated,
                        activeColor = Color(0xFF00FFCC),
                        onClick = {
                            if (!uiState.isProActivated) {
                                viewModel.activateLicenseKey("GATEUP-PRO-AUTO")
                            } else {
                                viewModel.deactivateLicense()
                            }
                        }
                    )
                    
                    // Reboot to Bootloader
                    FloatingDockButton(
                        icon = Icons.Default.Settings,
                        label = "REBOOT",
                        isActive = false,
                        activeColor = Color.Red,
                        onClick = { viewModel.rebootToBootloader() }
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingDockButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (isActive) activeColor.copy(alpha = 0.25f) else Color(0xFF1E293B),
                    CircleShape
                )
                .border(
                    1.dp,
                    if (isActive) activeColor else Color.Gray.copy(alpha = 0.5f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) activeColor else Color.LightGray,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (isActive) activeColor else Color.Gray,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}


@Composable
fun GrubLeftPanel(
    selectedOs: OsType,
    onSelectOs: (OsType) -> Unit,
    onBootNow: () -> Unit,
    onEnterBiosSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
            .background(Color(0xFF080D1A))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // GRUB Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GNU GRUB  version 2.06_multiarch",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "[ GRUB BOOT MENU ]",
                    color = Color(0xFF00FFCC),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Divider(color = Color(0xFF1E293B), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = "Use click/tap to highlight the Guest operating system. Selected options adjust the virtual hypervisor thread configurations dynamically.",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            // GRUB-style boxed list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
                    .background(Color(0xFF050811))
                    .padding(12.dp)
            ) {
                Text(
                    text = "BOOTLOADER MULTI-BOOT MENU LIST:",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val osList = listOf(
                    OsType.WIN11 to "Windows 11 Pro [Virtual Environment Layer]",
                    OsType.WIN10 to "Windows 10 Pro [Optimized & Stripped]",
                    OsType.WIN7 to "Windows 7 / XP Legacy [Compatibility Mod]",
                    OsType.KALI to "Kali Linux Rolling [Penetration Auditing Bash]"
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    osList.forEach { (os, label) ->
                        val isSelected = selectedOs == os
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) Color(0xFF1E293B) else Color.Transparent)
                                .clickable {
                                    onSelectOs(os)
                                }
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isSelected) "* " else "  ",
                                    color = if (isSelected) Color(0xFF00FFCC) else Color.LightGray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = label,
                                    color = if (isSelected) Color(0xFF00FFCC) else Color.LightGray,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            if (isSelected) {
                                Text(
                                    text = "<HIGHLIGHTED>",
                                    color = Color(0xFF00FFCC).copy(alpha = 0.8f),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onEnterBiosSetup,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFFFFFF55)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth().height(36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "BIOS Settings",
                        tint = Color(0xFFFFFF55),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ENTER BIOS SETUP UTILITY [F2]",
                        color = Color(0xFFFFFF55),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Keyboard controls guidance at bottom
        Column {
            Divider(color = Color(0xFF1E293B), thickness = 1.dp, modifier = Modifier.padding(bottom = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Press enter or tap BOOT VM NOW on the right.",
                    color = Color(0xFF64748B),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Hypervisor: HYBRID-KVM v4.2",
                    color = Color(0xFF64748B),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun HardwareConfigRightPanel(
    cores: Int,
    onCoresChanged: (Int) -> Unit,
    ram: Int,
    onRamChanged: (Int) -> Unit,
    driver: String,
    onDriverChanged: (String) -> Unit,
    dxvk: Boolean,
    onDxvkChanged: (Boolean) -> Unit,
    esync: Boolean,
    onEsyncChanged: (Boolean) -> Unit,
    perfGrade: String,
    perfColor: Color,
    onQuickBootClick: () -> Unit,
    onBootClick: () -> Unit,
    partitions: List<DiskPartition>,
    onResizePartition: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val driversList = listOf("Turnip+Zink (Recommended)", "VirGL Simulator", "LLVMpipe Software Renderer")

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF080D1A)),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp)
        ) {
            Column {
                // Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Specs",
                        tint = Color(0xFFFFA500),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SYSTEM CONFIGURATION PANEL",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                
                Divider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 10.dp))

                // CPU Core Selection
                Text(
                    text = "CPU Core Allocation: $cores Cores",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(2, 4, 6, 8).forEach { c ->
                        Button(
                            onClick = { onCoresChanged(c) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (cores == c) Color(0xFFFFA500) else Color(0xFF1E293B)
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("$c", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = if (cores == c) Color.Black else Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // RAM Selection
                Text(
                    text = "Virtual System RAM: $ram GB",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(4, 8, 12, 16).forEach { r ->
                        Button(
                            onClick = { onRamChanged(r) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (ram == r) Color(0xFFFFA500) else Color(0xFF1E293B)
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("${r}G", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = if (ram == r) Color.Black else Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Vulkan Graphics Driver Swap
                Text(
                    text = "GPU Driver: $driver",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Column {
                    driversList.forEach { drv ->
                        val active = driver == drv
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDriverChanged(drv) }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = active,
                                onClick = { onDriverChanged(drv) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFFA500))
                            )
                            Text(
                                text = drv,
                                color = if (active) Color.White else Color(0xFF64748B),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Translation layers checkboxes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = dxvk,
                            onCheckedChange = { onDxvkChanged(it) },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFFA500))
                        )
                        Text("DXVK Direct3D", color = Color(0xFF94A3B8), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = esync,
                            onCheckedChange = { onEsyncChanged(it) },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFFA500))
                        )
                        Text("Wine-Esync", color = Color(0xFF94A3B8), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // VIRTUAL DISK ALLOCATION (Visible Before Booting)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0C1324))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Disk Info",
                            tint = Color(0xFF00FFCC),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PRE-BOOT DISK ALLOCATION",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val winPartition = partitions.find { it.letter == "C:" }
                    val kaliPartition = partitions.find { it.letter == "ROOTFS" }
                    val allocatedCapacity = partitions.sumOf { it.sizeGb }
                    val unallocatedCapacity = (512 - allocatedCapacity).coerceAtLeast(0)

                    if (winPartition != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF131D35))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
                                .padding(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Windows 11 (C:)", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Text("${winPartition.sizeGb} GB", color = Color(0xFF00FFCC), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (winPartition.sizeGb > 40) {
                                            onResizePartition("C:", winPartition.sizeGb - 10)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                    modifier = Modifier.height(24.dp).weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("-10GB", fontSize = 8.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                }
                                Button(
                                    onClick = {
                                        if (unallocatedCapacity >= 10) {
                                            onResizePartition("C:", winPartition.sizeGb + 10)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                    modifier = Modifier.height(24.dp).weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("+10GB", fontSize = 8.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (kaliPartition != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF131D35))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
                                .padding(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Kali Linux (ROOTFS)", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Text("${kaliPartition.sizeGb} GB", color = Color(0xFFE040FB), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (kaliPartition.sizeGb > 30) {
                                            onResizePartition("ROOTFS", kaliPartition.sizeGb - 10)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                    modifier = Modifier.height(24.dp).weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("-10GB", fontSize = 8.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                }
                                Button(
                                    onClick = {
                                        if (unallocatedCapacity >= 10) {
                                            onResizePartition("ROOTFS", kaliPartition.sizeGb + 10)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                    modifier = Modifier.height(24.dp).weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("+10GB", fontSize = 8.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Pool: 512 GB", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text("Unallocated: $unallocatedCapacity GB", color = if (unallocatedCapacity > 0) Color(0xFF00FFCC) else Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Estimated performance grade bar (dynamic resource allocation feedback)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF030712))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "ESTIMATED PERFORMANCE GRADE:",
                        color = Color.Gray,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = perfGrade,
                        color = perfColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cancel / Quick boot with defaults (to let user bypass the configuration)
                Button(
                    onClick = onQuickBootClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "QUICK BOOT",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }

                // Boot configuration
                Button(
                    onClick = onBootClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(40.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "BOOT VM NOW",
                        color = Color(0xFF030712),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun BootloaderScreen(viewModel: VirtualSystemViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isBiosSetupOpen) {
        BiosSetupUtility(
            viewModel = viewModel,
            onClose = { viewModel.setBiosSetupOpen(false) }
        )
        return
    }

    val selectedOs = uiState.biosSelectedOs
    
    var cores by remember { mutableStateOf(uiState.cpuCores) }
    var ram by remember { mutableStateOf(uiState.ramSizeGb) }
    var driver by remember { mutableStateOf(uiState.graphicsDriver) }
    var dxvk by remember { mutableStateOf(uiState.isDxvkEnabled) }
    var esync by remember { mutableStateOf(uiState.isEsyncEnabled) }

    // Dynamic hypervisor performance calculation for polish
    val perfScore = cores * 1250 + ram * 450 + (if (driver.contains("Turnip")) 2500 else 800) + (if (dxvk) 1200 else 0) + (if (esync) 800 else 0)
    val perfGrade = when {
        perfScore > 13000 -> "Ultra Core / Gaming Ready (GFLOPS: $perfScore)"
        perfScore > 9000 -> "Balanced Desktop Mode (GFLOPS: $perfScore)"
        else -> "Standard Sandbox / Low Overhead (GFLOPS: $perfScore)"
    }
    val perfColor = when {
        perfScore > 13000 -> Color(0xFF00FFCC)
        perfScore > 9000 -> Color(0xFFFFA500)
        else -> Color(0xFFE040FB)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF030712), Color(0xFF0B132B))
                )
            )
            .padding(12.dp)
    ) {
        val isPortrait = maxWidth < maxHeight || maxWidth < 640.dp

        if (isPortrait) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GrubLeftPanel(
                    selectedOs = selectedOs,
                    onSelectOs = { viewModel.selectBiosOs(it) },
                    onBootNow = {
                        viewModel.setSpecifications(cores, ram, driver, dxvk, esync)
                        viewModel.bootSelectedOs(selectedOs)
                    },
                    onEnterBiosSetup = { viewModel.setBiosSetupOpen(true) },
                    modifier = Modifier.fillMaxWidth()
                )
                HardwareConfigRightPanel(
                    cores = cores,
                    onCoresChanged = { cores = it },
                    ram = ram,
                    onRamChanged = { ram = it },
                    driver = driver,
                    onDriverChanged = { driver = it },
                    dxvk = dxvk,
                    onDxvkChanged = { dxvk = it },
                    esync = esync,
                    onEsyncChanged = { esync = it },
                    perfGrade = perfGrade,
                    perfColor = perfColor,
                    onQuickBootClick = {
                        viewModel.setSpecifications(4, 8, "Turnip+Zink (Recommended)", true, true)
                        viewModel.bootSelectedOs(selectedOs)
                    },
                    onBootClick = {
                        viewModel.setSpecifications(cores, ram, driver, dxvk, esync)
                        viewModel.bootSelectedOs(selectedOs)
                    },
                    partitions = uiState.partitions,
                    onResizePartition = { letter, size -> viewModel.resizePartition(letter, size) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GrubLeftPanel(
                    selectedOs = selectedOs,
                    onSelectOs = { viewModel.selectBiosOs(it) },
                    onBootNow = {
                        viewModel.setSpecifications(cores, ram, driver, dxvk, esync)
                        viewModel.bootSelectedOs(selectedOs)
                    },
                    onEnterBiosSetup = { viewModel.setBiosSetupOpen(true) },
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                )
                HardwareConfigRightPanel(
                    cores = cores,
                    onCoresChanged = { cores = it },
                    ram = ram,
                    onRamChanged = { ram = it },
                    driver = driver,
                    onDriverChanged = { driver = it },
                    dxvk = dxvk,
                    onDxvkChanged = { dxvk = it },
                    esync = esync,
                    onEsyncChanged = { esync = it },
                    perfGrade = perfGrade,
                    perfColor = perfColor,
                    onQuickBootClick = {
                        viewModel.setSpecifications(4, 8, "Turnip+Zink (Recommended)", true, true)
                        viewModel.bootSelectedOs(selectedOs)
                    },
                    onBootClick = {
                        viewModel.setSpecifications(cores, ram, driver, dxvk, esync)
                        viewModel.bootSelectedOs(selectedOs)
                    },
                    partitions = uiState.partitions,
                    onResizePartition = { letter, size -> viewModel.resizePartition(letter, size) },
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                )
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
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFF00FFCC), CircleShape)
            ) {
                Image(
                    painter = painterResource(id = com.example.R.drawable.img_gateup_logo_new_1783393251374),
                    contentDescription = "Virtual Machine Boot Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(rotation),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "GATEUP PRO OS SIMULATOR",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "SECURE SANDBOX ENVIRONMENT v4.2",
                color = Color(0xFF00FFCC).copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LinearProgressIndicator(
                progress = uiState.bootProgress,
                color = Color(0xFF00FFCC),
                trackColor = Color.White.copy(alpha = 0.1f),
                modifier = Modifier
                    .width(280.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "PROVISIONING GUEST SYSTEM: ${(uiState.bootProgress * 100).roundToInt()}% COMPLETE",
                color = Color.Gray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            
            Spacer(modifier = Modifier.height(30.dp))
            
            // Diagnostics scrolling panel
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF050B14)),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier
                    .width(360.dp)
                    .height(110.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "--- REAL-TIME HARDWARE DIAGNOSTICS ---",
                        color = Color(0xFF64748B),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    val visibleLogs = uiState.bootDiagnosticsLog.takeLast(4)
                    visibleLogs.forEach { log ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "✓", color = Color(0xFF00FFCC), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = log,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }
                    }
                    
                    if (visibleLogs.isEmpty()) {
                        Text(
                            text = "Awaiting guest state translation trap...",
                            color = Color.DarkGray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopEnvironment(
    viewModel: VirtualSystemViewModel,
    uiState: UiState,
    onProFeatureLocked: () -> Unit
) {
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

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(top = 40.dp, start = 16.dp)
        ) {
            var selectedShortcutId by remember { mutableStateOf<String?>(null) }
            val cellWidth = 90.dp
            val cellHeight = 95.dp
            
            uiState.desktopShortcuts.forEach { shortcut ->
                var dragOffset by remember(shortcut.id) { mutableStateOf(Offset.Zero) }
                var isDragging by remember(shortcut.id) { mutableStateOf(false) }
                
                val density = LocalDensity.current
                val cellWidthPx = with(density) { cellWidth.toPx() }
                val cellHeightPx = with(density) { cellHeight.toPx() }
                
                val title = when (shortcut.id) {
                    "cmd" -> if (uiState.currentOs == OsType.KALI) "Bash Terminal" else "Command Prompt"
                    "settings" -> if (uiState.currentOs == OsType.KALI) "Sysconfig" else "Control Panel"
                    "installer" -> if (uiState.currentOs == OsType.KALI) "APT Installer" else "Software Center"
                    else -> shortcut.title
                }
                
                val icon = when (shortcut.iconName) {
                    "chrome" -> Icons.Default.Share
                    "explorer" -> Icons.Default.Home
                    "cmd" -> Icons.Default.List
                    "settings" -> Icons.Default.Settings
                    "installer" -> Icons.Default.Add
                    "printer" -> Icons.Default.Print
                    "documents", "downloads" -> Icons.Default.Folder
                    else -> Icons.Default.Home
                }
                
                val baseOffsetX = cellWidth * shortcut.gridX
                val baseOffsetY = cellHeight * shortcut.gridY
                val dragXDp = with(density) { dragOffset.x.toDp() }
                val dragYDp = with(density) { dragOffset.y.toDp() }
                
                Box(
                    modifier = Modifier
                        .offset(
                            x = baseOffsetX + dragXDp,
                            y = baseOffsetY + dragYDp
                        )
                        .zIndex(if (isDragging) 100f else 1f)
                        .pointerInput(shortcut.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    isDragging = true
                                },
                                onDragEnd = {
                                    isDragging = false
                                    val totalOffsetX = shortcut.gridX * cellWidthPx + dragOffset.x
                                    val totalOffsetY = shortcut.gridY * cellHeightPx + dragOffset.y
                                    
                                    val rawGridX = (totalOffsetX / cellWidthPx).roundToInt()
                                    val rawGridY = (totalOffsetY / cellHeightPx).roundToInt()
                                    
                                    val maxGridX = (constraints.maxWidth / cellWidthPx).toInt() - 1
                                    val maxGridY = (constraints.maxHeight / cellHeightPx).toInt() - 1
                                    
                                    val finalGridX = rawGridX.coerceIn(0, maxGridX.coerceAtLeast(0))
                                    val finalGridY = rawGridY.coerceIn(0, maxGridY.coerceAtLeast(0))
                                    
                                    viewModel.updateShortcutPosition(shortcut.id, finalGridX, finalGridY)
                                    dragOffset = Offset.Zero
                                },
                                onDragCancel = {
                                    isDragging = false
                                    dragOffset = Offset.Zero
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount
                                }
                            )
                        }
                ) {
                    DesktopIcon(
                        title = title,
                        iconName = shortcut.iconName,
                        isSelected = selectedShortcutId == shortcut.id,
                        onClick = {
                            selectedShortcutId = shortcut.id
                            if (shortcut.type != null) {
                                viewModel.openWindow(shortcut.type)
                            } else {
                                viewModel.openFolderShortcut(shortcut)
                            }
                        }
                    )
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val maxW = maxWidth
            val maxH = maxHeight

            uiState.windows.forEach { window ->
                val isVisible = !window.isMinimized
                key(window.type) {
                    val isActive = uiState.activeWindow == window.type
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn() + scaleIn(initialScale = 0.85f),
                        exit = slideOutVertically(targetOffsetY = { 100 }) + fadeOut() + scaleOut(targetScale = 0.85f),
                        modifier = Modifier.zIndex(window.zIndex.toFloat())
                    ) {
                        WindowWrapper(
                            window = window,
                            isActive = isActive,
                            currentOs = uiState.currentOs,
                            maxW = maxW,
                            maxH = maxH,
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
            StartMenuDialog(viewModel = viewModel, uiState = uiState, onProFeatureLocked = onProFeatureLocked)
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
            WindowsBottomTaskbar(viewModel = viewModel, uiState = uiState, onProFeatureLocked = onProFeatureLocked)
        }
    }
}

@Composable
fun WindowsIcon(iconName: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (iconName) {
            "chrome" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    
                    drawArc(
                        color = Color(0xFFEA4335),
                        startAngle = -30f,
                        sweepAngle = 120f,
                        useCenter = true
                    )
                    drawArc(
                        color = Color(0xFFFBBC05),
                        startAngle = 90f,
                        sweepAngle = 120f,
                        useCenter = true
                    )
                    drawArc(
                        color = Color(0xFF34A853),
                        startAngle = 210f,
                        sweepAngle = 120f,
                        useCenter = true
                    )
                    drawCircle(
                        color = Color(0xFF0F172A),
                        radius = radius * 0.45f,
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFF4285F4),
                        radius = radius * 0.32f,
                        center = center
                    )
                }
            }
            "explorer" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    drawRoundRect(
                        color = Color(0xFFFFC107),
                        topLeft = Offset(w * 0.1f, h * 0.28f),
                        size = androidx.compose.ui.geometry.Size(w * 0.8f, h * 0.58f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color(0xFFFF8F00),
                        topLeft = Offset(w * 0.1f, h * 0.16f),
                        size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.2f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color(0xFF00E5FF),
                        topLeft = Offset(w * 0.25f, h * 0.38f),
                        size = androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.12f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
                    )
                }
            }
            "cmd" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E293B), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFF475569), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ">_",
                        color = Color(0xFF00FFCC),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            "settings" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.minDimension * 0.35f
                    
                    drawCircle(
                        color = Color(0xFF00B0FF),
                        radius = radius,
                        center = center
                    )
                    for (angle in 0..360 step 45) {
                        val rad = Math.toRadians(angle.toDouble())
                        val toothCenter = Offset(
                            (center.x + Math.cos(rad) * radius).toFloat(),
                            (center.y + Math.sin(rad) * radius).toFloat()
                        )
                        drawCircle(
                            color = Color(0xFF00B0FF),
                            radius = radius * 0.25f,
                            center = toothCenter
                        )
                    }
                    drawCircle(
                        color = Color(0xFF0F172A),
                        radius = radius * 0.32f,
                        center = center
                    )
                }
            }
            "installer" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    drawArc(
                        color = Color(0xFFE040FB),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(w * 0.3f, h * 0.1f),
                        size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.3f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color(0xFFBA68C8),
                        topLeft = Offset(w * 0.15f, h * 0.3f),
                        size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.6f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(w * 0.45f, h * 0.45f),
                        size = androidx.compose.ui.geometry.Size(w * 0.1f, h * 0.3f)
                    )
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(w * 0.35f, h * 0.55f),
                        size = androidx.compose.ui.geometry.Size(w * 0.3f, h * 0.1f)
                    )
                }
            }
            "printer" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    drawRoundRect(
                        color = Color(0xFF00E5FF),
                        topLeft = Offset(w * 0.15f, h * 0.35f),
                        size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.45f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                    )
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(w * 0.3f, h * 0.15f),
                        size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.3f)
                    )
                    drawRect(
                        color = Color.LightGray,
                        topLeft = Offset(w * 0.25f, h * 0.65f),
                        size = androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.25f)
                    )
                }
            }
            "documents", "downloads" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val folderColor = if (iconName == "downloads") Color(0xFF0288D1) else Color(0xFFFFCA28)
                    
                    drawRoundRect(
                        color = folderColor,
                        topLeft = Offset(w * 0.1f, h * 0.25f),
                        size = androidx.compose.ui.geometry.Size(w * 0.8f, h * 0.6f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )
                    drawRoundRect(
                        color = folderColor.copy(alpha = 0.8f),
                        topLeft = Offset(w * 0.1f, h * 0.15f),
                        size = androidx.compose.ui.geometry.Size(w * 0.35f, h * 0.2f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                    )
                }
            }
            "taskmgr" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    drawRoundRect(
                        color = Color(0xFF1E293B),
                        size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )
                    val points = listOf(
                        Offset(w * 0.1f, h * 0.5f),
                        Offset(w * 0.3f, h * 0.5f),
                        Offset(w * 0.4f, h * 0.2f),
                        Offset(w * 0.5f, h * 0.8f),
                        Offset(w * 0.6f, h * 0.4f),
                        Offset(w * 0.7f, h * 0.5f),
                        Offset(w * 0.9f, h * 0.5f)
                    )
                    for (idx in 0 until points.size - 1) {
                        drawLine(
                            color = Color(0xFF00FFCC),
                            start = points[idx],
                            end = points[idx + 1],
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DesktopIcon(
    title: String,
    iconName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick)
            .background(if (isSelected) Color(0xFF00FFCC).copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFF00FFCC).copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
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
            WindowsIcon(
                iconName = iconName,
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
    maxW: androidx.compose.ui.unit.Dp,
    maxH: androidx.compose.ui.unit.Dp,
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

    // Animation ratio for maximizing / windowing
    val isMax = window.isMaximized
    val animRatio by animateFloatAsState(
        targetValue = if (isMax) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "maximizationRatio"
    )

    val bottomPadding = if (currentOs == OsType.KALI) 0.dp else 48.dp
    val topPadding = if (currentOs == OsType.KALI) 32.dp else 0.dp

    val targetW = maxW
    val targetH = maxH - bottomPadding - topPadding

    // Interpolate width, height, and offsets smoothly
    val currentWidth = window.width.dp + (targetW - window.width.dp) * animRatio
    val currentHeight = window.height.dp + (targetH - window.height.dp) * animRatio
    val currentX = window.x.dp + (0.dp - window.x.dp) * animRatio
    val currentY = window.y.dp + (topPadding - window.y.dp) * animRatio

    val modifier = Modifier
        .offset(currentX, currentY)
        .size(currentWidth, currentHeight)

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

data class ChromeTab(
    val id: Int,
    val title: String,
    val url: String,
    val renderMode: String, // "google_home", "google_results", "github", "speedtest", "wikipedia", "youtube"
    val searchInput: String = ""
)

@Composable
fun GoogleChromeAppContent(viewModel: VirtualSystemViewModel, uiState: UiState) {
    val tabs = remember { 
        mutableStateListOf(
            ChromeTab(1, "Google", "https://www.google.com", "google_home")
        ) 
    }
    var activeTabId by remember { mutableStateOf(1) }
    var nextTabId by remember { mutableStateOf(2) }

    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.first()

    fun updateActiveTab(updater: (ChromeTab) -> ChromeTab) {
        val index = tabs.indexOfFirst { it.id == activeTabId }
        if (index != -1) {
            tabs[index] = updater(tabs[index])
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F121D))) {
        // Tab Strip (Top of Chrome)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(Color(0xFF131722))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEach { tab ->
                val isActive = tab.id == activeTabId
                Row(
                    modifier = Modifier
                        .width(130.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (isActive) Color(0xFF1E2436) else Color(0xFF1B1E2B))
                        .clickable { activeTabId = tab.id }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Colored Favicon
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    when (tab.renderMode) {
                                        "google_home", "google_results" -> Color(0xFF4285F4)
                                        "github" -> Color.White
                                        "speedtest" -> Color(0xFF00FFCC)
                                        "wikipedia" -> Color.LightGray
                                        "youtube" -> Color.Red
                                        else -> Color.Gray
                                    }
                                )
                        )
                        Text(
                            text = tab.title,
                            color = if (isActive) Color.White else Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (tabs.size > 1) {
                        IconButton(
                            onClick = {
                                val currentIdx = tabs.indexOfFirst { it.id == tab.id }
                                tabs.removeAt(currentIdx)
                                if (activeTabId == tab.id) {
                                    activeTabId = tabs.first().id
                                }
                            },
                            modifier = Modifier.size(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close tab", tint = Color.Gray, modifier = Modifier.size(10.dp))
                        }
                    }
                }
            }

            // New Tab button
            IconButton(
                onClick = {
                    tabs.add(ChromeTab(nextTabId, "New Tab", "https://www.google.com", "google_home"))
                    activeTabId = nextTabId
                    nextTabId++
                },
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .size(24.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New Tab", tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }

        // Navigation Bar
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
                IconButton(
                    onClick = {
                        updateActiveTab { it.copy(renderMode = "google_home", url = "https://www.google.com", title = "Google") }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(14.dp))
                }
                IconButton(
                    onClick = {
                        updateActiveTab { it.copy(renderMode = "google_home", url = "https://www.google.com", title = "Google") }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = "Home", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }

            // User-Agent status pill
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF00E676).copy(alpha = 0.15f)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "DESKTOP AGENT (Win64; x64)",
                    color = Color(0xFF00FFCC),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            BasicTextField(
                value = activeTab.url,
                onValueChange = { newUrl ->
                    updateActiveTab { it.copy(url = newUrl) }
                },
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
                    val input = activeTab.url.lowercase()
                    when {
                        input.contains("github") -> {
                            updateActiveTab { it.copy(renderMode = "github", url = "https://github.com", title = "GitHub") }
                        }
                        input.contains("speedtest") -> {
                            updateActiveTab { it.copy(renderMode = "speedtest", url = "https://speedtest.net", title = "SpeedTest") }
                        }
                        input.contains("youtube") -> {
                            updateActiveTab { it.copy(renderMode = "youtube", url = "https://youtube.com", title = "YouTube") }
                        }
                        input.contains("wikipedia") -> {
                            updateActiveTab { it.copy(renderMode = "wikipedia", url = "https://wikipedia.org", title = "Wikipedia") }
                        }
                        else -> {
                            val query = activeTab.url.substringAfter("q=").substringBefore("&")
                            updateActiveTab { it.copy(renderMode = "google_results", url = "https://www.google.com/search?q=${activeTab.url}", searchInput = activeTab.url, title = activeTab.url) }
                        }
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

        // Webview Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF10141E))
        ) {
            when (activeTab.renderMode) {
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
                            value = activeTab.searchInput,
                            onValueChange = { valText ->
                                updateActiveTab { it.copy(searchInput = valText) }
                            },
                            placeholder = { Text("Search Google or type a URL", color = Color.Gray, fontSize = 12.sp) },
                            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                            singleLine = true,
                            modifier = Modifier
                                .width(450.dp)
                                .height(54.dp),
                            shape = RoundedCornerShape(27.dp),
                            trailingIcon = {
                                IconButton(onClick = {
                                    val q = activeTab.searchInput
                                    updateActiveTab {
                                        it.copy(
                                            renderMode = "google_results",
                                            url = "https://www.google.com/search?q=$q",
                                            title = "$q - Google Search"
                                        )
                                    }
                                }) {
                                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color.Gray)
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    val q = activeTab.searchInput
                                    updateActiveTab {
                                        it.copy(
                                            renderMode = "google_results",
                                            url = "https://www.google.com/search?q=$q",
                                            title = "$q - Google Search"
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202124))
                            ) {
                                Text("Google Search", fontSize = 11.sp, color = Color.LightGray)
                            }
                            Button(
                                onClick = {
                                    updateActiveTab {
                                        it.copy(
                                            renderMode = "github",
                                            url = "https://github.com",
                                            title = "GitHub"
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202124))
                            ) {
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
                            Text("Google Search Results for '${activeTab.searchInput}':", color = Color(0xFF8AB4F8), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                        
                        val searchTerms = activeTab.searchInput.lowercase()
                        val filteredResults = if (searchTerms.contains("wikipedia") || searchTerms.contains("wiki")) {
                            listOf(
                                "Windows 11 - Wikipedia encyclopedia" to "Learn about Windows 11 hybrid design architecture, system specifications, and integration layers...",
                                "Linux Kernel development - Wikipedia" to "Discover the history, monolithic architecture, and container virtualization modules of Kali's kernel..."
                            )
                        } else if (searchTerms.contains("youtube") || searchTerms.contains("video")) {
                            listOf(
                                "YouTube Sim tech hub" to "Watch high framerate box64 wine demonstration videos and virtualization optimization tutorials..."
                            )
                        } else {
                            listOf(
                                "Windows 11 x86_64 Emulator on Android - GitHub" to "How to compile box64, wine-esync, and dxvk on Snapdragon device with vulkan hardware acceleration...",
                                "Turnip Zink graphic driver setups" to "Get maximum hardware acceleration inside containerized PROOT layers using Adreno GPU mapping guides...",
                                "Learn Kali Linux penetration suite" to "Deploy advanced nmap scanners, wireshark packet decryptors and brute-force tools safely locally...",
                                "How to bypass website mobile detection" to "Configure your User-Agent string hardcoded to mimic standard Windows NT 10.0 architecture."
                            )
                        }

                        items(filteredResults) { (title, snippet) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable {
                                        if (title.contains("GitHub")) {
                                            updateActiveTab { it.copy(renderMode = "github", url = "https://github.com", title = "GitHub") }
                                        } else if (title.contains("Wikipedia")) {
                                            updateActiveTab { it.copy(renderMode = "wikipedia", url = "https://wikipedia.org", title = "Wikipedia") }
                                        } else if (title.contains("YouTube")) {
                                            updateActiveTab { it.copy(renderMode = "youtube", url = "https://youtube.com", title = "YouTube") }
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
                "wikipedia" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("Wikipedia - Virtual Academy", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("The Free Simulated Encyclopedia", color = Color.Gray, fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2436)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Featured Article: Windows 11 On Snapdragon", color = Color(0xFF8AB4F8), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Windows 11 features robust compatibility modules for ARM64 architectures. Through container layers and translation engines such as Box64, Snapdragon host environments can run x86_64 binaries natively with Direct3D rendering redirected through Vulkan translation. This allows highly optimized frame execution of heavy desktop titles within Android.",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
                "youtube" -> {
                    var selectedVideo by remember { mutableStateOf<String?>(null) }
                    var isPlaying by remember { mutableStateOf(false) }
                    var videoProgress by remember { mutableStateOf(0f) }

                    LaunchedEffect(isPlaying) {
                        if (isPlaying) {
                            videoProgress = 0f
                            while (isPlaying) {
                                delay(500)
                                videoProgress = (videoProgress + 0.05f).coerceIn(0f, 1f)
                                if (videoProgress >= 1f) {
                                    videoProgress = 0f
                                }
                            }
                        }
                    }

                    if (selectedVideo == null) {
                        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            Text("YouTube Simulator - Tech Hub", color = Color.Red, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                val videos = listOf(
                                    "Wine-Esync + DXVK Android Gaming Demonstration" to "Ultimate framerate tweaks inside containerized virtual frameworks...",
                                    "Kali Linux Complete Wifi Penetration Course" to "Ethical cybersecurity network analysis and port scanning tools...",
                                    "Termux Box64 Box86 Full Installation Guide" to "Step by step instructions for compiling userspace translations..."
                                )
                                items(videos) { (vTitle, vDesc) ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                            .clickable {
                                                selectedVideo = vTitle
                                                isPlaying = true
                                            },
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2436))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                            Box(modifier = Modifier.fillMaxWidth().height(60.dp).background(Color.Red.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Red, modifier = Modifier.size(32.dp))
                                            }
                                            Text(vTitle, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(vDesc, color = Color.Gray, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(onClick = { selectedVideo = null; isPlaying = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                                    Text("Back to Grid", color = Color.White, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(selectedVideo ?: "", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Black),
                                modifier = Modifier.fillMaxWidth().height(180.dp),
                                border = BorderStroke(1.dp, Color.Red)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Playing Simulated Stream...", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                        CircularProgressIndicator(progress = videoProgress, color = Color.Red, modifier = Modifier.size(36.dp))
                                        Text("${(videoProgress * 100).toInt()}% completed", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
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
                
                Text("Disk Partitions", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                
                uiState.partitions.forEach { partition ->
                    val path = if (partition.letter == "ROOTFS") "/" else partition.letter
                    val isSelected = if (path == "/") {
                        uiState.explorerPath.startsWith("/") && !uiState.explorerPath.contains(":")
                    } else {
                        uiState.explorerPath.startsWith(path)
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateToFolder(path) }
                            .background(if (isSelected) Color(0xFF00FFCC).copy(alpha = 0.12f) else Color.Transparent, RoundedCornerShape(4.dp))
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Canvas(modifier = Modifier.size(14.dp)) {
                                drawRoundRect(
                                    color = if (isSelected) Color(0xFF00FFCC) else Color.LightGray,
                                    size = size,
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                                )
                                drawLine(
                                    color = Color.DarkGray,
                                    start = Offset(size.width * 0.1f, size.height * 0.7f),
                                    end = Offset(size.width * 0.9f, size.height * 0.7f),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawCircle(
                                    color = Color.Green,
                                    radius = 1.dp.toPx(),
                                    center = Offset(size.width * 0.8f, size.height * 0.3f)
                                )
                            }
                            Text(
                                text = if (partition.letter == "ROOTFS") "ROOTFS (Linux)" else "${partition.name} (${partition.letter})",
                                color = if (isSelected) Color(0xFF00FFCC) else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        val progress = partition.usedGb.toFloat() / partition.sizeGb.toFloat()
                        LinearProgressIndicator(
                            progress = progress,
                            color = if (progress > 0.85f) Color.Red else Color(0xFF00FFCC),
                            trackColor = Color.Gray.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(1.5f.dp))
                        )
                        Text(
                            text = "${partition.usedGb}GB / ${partition.sizeGb}GB (${partition.fileSystem})",
                            color = Color.Gray,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
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
                                                viewModel.executeVirtualBinary(file)
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
    var activeTab by remember { mutableStateOf(0) } // 0: Specs, 1: Disk Partitions, 2: Environment Paths
    var newKey by remember { mutableStateOf("") }
    var newVal by remember { mutableStateOf("") }

    // Create Partition form states
    var createLetter by remember { mutableStateOf("") }
    var createName by remember { mutableStateOf("") }
    var createFileSystem by remember { mutableStateOf("NTFS") }
    var createSizeGb by remember { mutableStateOf("50") }
    var showCreateForm by remember { mutableStateOf(false) }

    // Resize state
    var selectedPartitionForResize by remember { mutableStateOf<DiskPartition?>(null) }
    var resizeSizeGb by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color(0xFF151926))
        ) {
            listOf("Specs Allocation", "Disk Partitions", "Environment Paths").forEachIndexed { index, title ->
                Button(
                    onClick = { activeTab = index },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTab == index) Color(0xFF1E253A) else Color.Transparent
                    ),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text(title, fontSize = 11.sp, color = Color.White, maxLines = 1)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF0D101A))
                .padding(12.dp)
        ) {
            when (activeTab) {
                0 -> {
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
                }
                1 -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "VIRTUAL STORAGE MANAGER & DISK PARTITIONS",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        val totalCapacity = uiState.storageSizeGb
                        val allocatedCapacity = uiState.partitions.sumOf { it.sizeGb }
                        val unallocatedCapacity = (totalCapacity - allocatedCapacity).coerceAtLeast(0)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF171D2F))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Disk 0 Basic (SSD) - Total Capacity: $totalCapacity GB", color = Color.LightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Text("Allocated: $allocatedCapacity GB | Free: $unallocatedCapacity GB", color = Color(0xFF00FFCC), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1E293B))
                            ) {
                                uiState.partitions.forEach { part ->
                                    val partWeight = (part.sizeGb.toFloat() / totalCapacity.toFloat()).coerceAtLeast(0.05f)
                                    val partColor = when (part.letter) {
                                        "C:" -> Color(0xFF00FFCC)
                                        "D:" -> Color(0xFFFFA500)
                                        "E:" -> Color(0xFF00E5FF)
                                        "ROOTFS" -> Color(0xFFE040FB)
                                        else -> Color(0xFFFFC107)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(partWeight)
                                            .fillMaxHeight()
                                            .background(partColor)
                                            .border(1.dp, Color.Black.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${part.letter} (${part.sizeGb}G)",
                                            color = Color.Black,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (unallocatedCapacity > 0) {
                                    val freeWeight = unallocatedCapacity.toFloat() / totalCapacity.toFloat()
                                    Box(
                                        modifier = Modifier
                                            .weight(freeWeight)
                                            .fillMaxHeight()
                                            .background(Color(0xFF424242))
                                            .border(1.dp, Color.Black.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Unallocated (${unallocatedCapacity}G)",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color(0xFF080C14))
                                .border(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(uiState.partitions) { part ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131826)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    val partColor = when (part.letter) {
                                                        "C:" -> Color(0xFF00FFCC)
                                                        "D:" -> Color(0xFFFFA500)
                                                        "E:" -> Color(0xFF00E5FF)
                                                        "ROOTFS" -> Color(0xFFE040FB)
                                                        else -> Color(0xFFFFC107)
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(RoundedCornerShape(50.dp))
                                                            .background(partColor)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "${part.name} (${part.letter})",
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                                Text(
                                                    text = "File System: ${part.fileSystem}",
                                                    color = Color.Gray,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Capacity: ${part.sizeGb} GB | Health Status: Healthy (Primary Partition)",
                                                color = Color.LightGray,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            
                                            Spacer(modifier = Modifier.height(6.dp))

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Button(
                                                    onClick = { viewModel.formatPartition(part.letter, part.fileSystem) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA500).copy(alpha = 0.2f)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(26.dp)
                                                ) {
                                                    Text("Format", color = Color(0xFFFFA500), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }

                                                Button(
                                                    onClick = { 
                                                        selectedPartitionForResize = part
                                                        resizeSizeGb = part.sizeGb.toString()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC).copy(alpha = 0.2f)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(26.dp)
                                                ) {
                                                    Text("Extend/Shrink", color = Color(0xFF00FFCC), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }

                                                if (!part.isSystem) {
                                                    Button(
                                                        onClick = { viewModel.deletePartition(part.letter) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(26.dp)
                                                    ) {
                                                        Text("Delete", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        selectedPartitionForResize?.let { part ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E253A)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Resize ${part.letter} (${part.name}):", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        BasicTextField(
                                            value = resizeSizeGb,
                                            onValueChange = { resizeSizeGb = it },
                                            textStyle = TextStyle(color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                            modifier = Modifier
                                                .width(80.dp)
                                                .background(Color.Black)
                                                .border(1.dp, Color.White.copy(alpha = 0.2f))
                                                .padding(4.dp)
                                        )
                                        Text("GB", color = Color.White, fontSize = 11.sp)
                                        Button(
                                            onClick = {
                                                val size = resizeSizeGb.toIntOrNull()
                                                if (size != null && size > 0) {
                                                    viewModel.resizePartition(part.letter, size)
                                                    selectedPartitionForResize = null
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                                            contentPadding = PaddingValues(horizontal = 12.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text("Apply", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { selectedPartitionForResize = null },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                            contentPadding = PaddingValues(horizontal = 12.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text("Cancel", color = Color.White, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }

                        if (showCreateForm) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E253A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("PROVISION NEW PARTITION:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Letter", color = Color.Gray, fontSize = 9.sp)
                                            BasicTextField(
                                                value = createLetter,
                                                onValueChange = { createLetter = it.take(2) },
                                                textStyle = TextStyle(color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.Black)
                                                    .border(1.dp, Color.White.copy(alpha = 0.2f))
                                                    .padding(6.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(2.5f)) {
                                            Text("Volume Label", color = Color.Gray, fontSize = 9.sp)
                                            BasicTextField(
                                                value = createName,
                                                onValueChange = { createName = it },
                                                textStyle = TextStyle(color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.Black)
                                                    .border(1.dp, Color.White.copy(alpha = 0.2f))
                                                    .padding(6.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.weight(1.5f)) {
                                            Text("File System", color = Color.Gray, fontSize = 9.sp)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.Black)
                                                    .border(1.dp, Color.White.copy(alpha = 0.2f))
                                                    .clickable {
                                                        createFileSystem = if (createFileSystem == "NTFS") "FAT32" else if (createFileSystem == "FAT32") "exFAT" else "NTFS"
                                                    }
                                                    .padding(6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(createFileSystem, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                                Text("▼", color = Color.Gray, fontSize = 9.sp)
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1.5f)) {
                                            Text("Size (GB)", color = Color.Gray, fontSize = 9.sp)
                                            BasicTextField(
                                                value = createSizeGb,
                                                onValueChange = { createSizeGb = it },
                                                textStyle = TextStyle(color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.Black)
                                                    .border(1.dp, Color.White.copy(alpha = 0.2f))
                                                    .padding(6.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = {
                                                val size = createSizeGb.toIntOrNull() ?: 50
                                                if (createLetter.isNotEmpty()) {
                                                    val cleanLetter = createLetter.trim().uppercase().removeSuffix(":") + ":"
                                                    if (unallocatedCapacity >= size) {
                                                        val added = viewModel.createPartition(cleanLetter, createName, createFileSystem, size)
                                                        if (added) {
                                                            showCreateForm = false
                                                            createLetter = ""
                                                            createName = ""
                                                        }
                                                    } else {
                                                        viewModel.triggerNotification("Insufficient Storage", "Cannot provision partition of $size GB. Unallocated space is only $unallocatedCapacity GB.")
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                                            modifier = Modifier.weight(1f).height(32.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("PROVISION", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { showCreateForm = false },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                            modifier = Modifier.weight(1f).height(32.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("CANCEL", color = Color.White, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            if (unallocatedCapacity > 0) {
                                Button(
                                    onClick = { showCreateForm = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("+ CREATE NEW VOLUME FROM UNALLOCATED", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            } else {
                                Text("Full virtual disk storage allocated. To create more partitions, shrink or delete an existing partition first.", color = Color.Gray, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
                2 -> {
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
}

@Composable
fun TaskManagerAppContent(viewModel: VirtualSystemViewModel, uiState: UiState) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Processes, 1 = Performance
    var selectedProcess by remember { mutableStateOf<String?>(null) }
    
    val cpuHistory = remember { mutableStateListOf<Float>().apply { repeat(20) { add(0.12f) } } }
    val ramHistory = remember { mutableStateListOf<Float>().apply { repeat(20) { add(0.32f) } } }

    LaunchedEffect(uiState.windows) {
        while (true) {
            delay(1000)
            val baseCpu = 2f + (Math.random() * 3).toFloat()
            val chromeCpu = if (uiState.windows.any { it.type == WindowType.CHROME }) 7f + (Math.random() * 5).toFloat() else 0f
            val cmdCpu = if (uiState.windows.any { it.type == WindowType.CMD }) 3f + (Math.random() * 4).toFloat() else 0f
            val totalCpu = (baseCpu + chromeCpu + cmdCpu).coerceIn(1f, 99f) / 100f

            val baseRam = 1.6f
            val extraRam = uiState.windows.sumOf {
                when (it.type) {
                    WindowType.CHROME -> 0.45
                    WindowType.EXPLORER -> 0.15
                    WindowType.CMD -> 0.04
                    WindowType.NOTEPAD -> 0.02
                    WindowType.SETTINGS -> 0.08
                    WindowType.TASK_MANAGER -> 0.06
                    WindowType.PRINTER_SCANNER -> 0.10
                    WindowType.PACK_INSTALLER -> 0.12
                }
            }.toFloat()
            val totalRam = ((baseRam + extraRam) / uiState.ramSizeGb).coerceIn(0.1f, 0.95f)

            if (cpuHistory.size >= 20) cpuHistory.removeAt(0)
            cpuHistory.add(totalCpu)

            if (ramHistory.size >= 20) ramHistory.removeAt(0)
            ramHistory.add(totalRam)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F131E))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { activeTab = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTab == 0) Color(0xFF00FFCC).copy(alpha = 0.2f) else Color.Transparent
                    ),
                    border = BorderStroke(1.dp, if (activeTab == 0) Color(0xFF00FFCC) else Color.Gray.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.List, contentDescription = "Processes", tint = Color.White, modifier = Modifier.size(14.dp))
                        Text("Processes", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Button(
                    onClick = { activeTab = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTab == 1) Color(0xFF00FFCC).copy(alpha = 0.2f) else Color.Transparent
                    ),
                    border = BorderStroke(1.dp, if (activeTab == 1) Color(0xFF00FFCC) else Color.Gray.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Performance", tint = Color.White, modifier = Modifier.size(14.dp))
                        Text("Performance", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            if (activeTab == 0) {
                val canEndTask = selectedProcess != null && 
                                 selectedProcess != "System" && 
                                 selectedProcess != "Registry" && 
                                 selectedProcess != "csrss.exe" && 
                                 selectedProcess != "taskmgr.exe"
                Button(
                    onClick = {
                        val process = selectedProcess
                        if (process != null) {
                            val windowType = when (process) {
                                "chrome.exe" -> WindowType.CHROME
                                "explorer.exe" -> WindowType.EXPLORER
                                "cmd.exe" -> WindowType.CMD
                                "notepad.exe" -> WindowType.NOTEPAD
                                "settings.exe" -> WindowType.SETTINGS
                                "spoolsv.exe" -> WindowType.PRINTER_SCANNER
                                "installer.exe" -> WindowType.PACK_INSTALLER
                                else -> null
                            }
                            if (windowType != null) {
                                viewModel.closeWindow(windowType)
                                selectedProcess = null
                            }
                        }
                    },
                    enabled = canEndTask,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canEndTask) Color.Red.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("End Task", color = if (canEndTask) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (activeTab == 0) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131826)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E2436))
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Name", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(3f))
                        Text("Status", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                        Text("CPU", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                        Text("Memory", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                    }
                    
                    val items = remember(uiState.windows) {
                        val list = mutableListOf(
                            Triple("System", "Running (Kernel)", "0.8%"),
                            Triple("Registry", "Suspended", "0.0%"),
                            Triple("csrss.exe", "Running", "0.1%"),
                            Triple("taskmgr.exe", "Running", "2.1%")
                        )
                        uiState.windows.forEach { win ->
                            val procName = when (win.type) {
                                WindowType.CHROME -> "chrome.exe"
                                WindowType.EXPLORER -> "explorer.exe"
                                WindowType.CMD -> "cmd.exe"
                                WindowType.NOTEPAD -> "notepad.exe"
                                WindowType.SETTINGS -> "settings.exe"
                                WindowType.PRINTER_SCANNER -> "spoolsv.exe"
                                WindowType.PACK_INSTALLER -> "installer.exe"
                                WindowType.TASK_MANAGER -> "taskmgr.exe"
                            }
                            // Don't duplicate taskmgr
                            if (procName != "taskmgr.exe") {
                                val status = if (win.isMinimized) "Minimized" else "Running"
                                val cpu = when (win.type) {
                                    WindowType.CHROME -> "8.4%"
                                    WindowType.CMD -> "4.2%"
                                    WindowType.PACK_INSTALLER -> "3.5%"
                                    else -> "1.2%"
                                }
                                list.add(Triple(procName, status, cpu))
                            }
                        }
                        list
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(items) { (name, status, cpu) ->
                            val isSelected = selectedProcess == name
                            val ramVal = when (name) {
                                "System" -> "120 MB"
                                "Registry" -> "32 MB"
                                "csrss.exe" -> "18 MB"
                                "chrome.exe" -> "450 MB"
                                "explorer.exe" -> "150 MB"
                                "cmd.exe" -> "40 MB"
                                "notepad.exe" -> "20 MB"
                                "settings.exe" -> "80 MB"
                                "spoolsv.exe" -> "100 MB"
                                "installer.exe" -> "120 MB"
                                else -> "60 MB"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedProcess = name }
                                    .background(if (isSelected) Color(0xFF00FFCC).copy(alpha = 0.15f) else Color.Transparent)
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(3f))
                                Text(status, color = if (status.contains("Running")) Color(0xFF00FFCC) else Color.Gray, fontSize = 11.sp, modifier = Modifier.weight(2f))
                                Text(cpu, color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                                Text(ramVal, color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                            }
                        }
                    }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF101726)), modifier = Modifier.weight(1f).fillMaxHeight(), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("CPU - Live Virtualized Core Usage", color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            val points = cpuHistory.toList()
                            val stepX = size.width / 19f
                            
                            for (i in 1..4) {
                                val y = size.height * (i * 0.25f)
                                drawLine(Color.White.copy(alpha = 0.05f), start = Offset(0f, y), end = Offset(size.width, y))
                            }

                            for (i in 0 until points.size - 1) {
                                val startX = i * stepX
                                val startY = size.height * (1f - points[i])
                                val endX = (i + 1) * stepX
                                val endY = size.height * (1f - points[i + 1])
                                drawLine(Color(0xFF00FFCC), start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = 3f)
                            }
                        }
                        val currentCpu = if (cpuHistory.isNotEmpty()) (cpuHistory.last() * 100).roundToInt() else 12
                        Text("Total Load: $currentCpu% (${uiState.cpuCores} Cores Active)", color = Color(0xFF00FFCC), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF101726)), modifier = Modifier.weight(1f).fillMaxHeight(), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("System Memory (Simulated RAM pool)", color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            val points = ramHistory.toList()
                            val stepX = size.width / 19f
                            
                            for (i in 1..4) {
                                val y = size.height * (i * 0.25f)
                                drawLine(Color.White.copy(alpha = 0.05f), start = Offset(0f, y), end = Offset(size.width, y))
                            }

                            for (i in 0 until points.size - 1) {
                                val startX = i * stepX
                                val startY = size.height * (1f - points[i])
                                val endX = (i + 1) * stepX
                                val endY = size.height * (1f - points[i + 1])
                                drawLine(Color(0xFFFFA500), start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = 3f)
                            }
                        }
                        val currentRamPercent = if (ramHistory.isNotEmpty()) ramHistory.last() else 0.35f
                        val memoryUsed = (uiState.ramSizeGb * currentRamPercent)
                        Text("RAM Used: ${String.format("%.2f", memoryUsed)} GB / ${uiState.ramSizeGb} GB (${(currentRamPercent * 100).roundToInt()}%)", color = Color(0xFFFFA500), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StartMenuDialog(
    viewModel: VirtualSystemViewModel,
    uiState: UiState,
    onProFeatureLocked: () -> Unit
) {
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

                var selectedAppInStart by remember { mutableStateOf<WindowType?>(null) }
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
                        val isSelected = selectedAppInStart == type
                        Card(
                            modifier = Modifier
                                .clickable(
                                    onClick = { 
                                        viewModel.openWindow(type)
                                        viewModel.toggleStartMenu()
                                    }
                                )
                                .height(56.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF00FFCC).copy(alpha = 0.12f) else Color(0xFF1F293D)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF00FFCC).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f)
                            )
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
                        Text(
                            text = uiState.oobeUsername.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.oobeUsername,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { viewModel.toggleVirtualKeyboard() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("KEYBOARD", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.toggleVirtualMouse() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("MOUSE", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.logoutSystem() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("LOCK", fontSize = 8.sp, color = Color.White)
                    }
                    Button(
                        onClick = { viewModel.rebootToBootloader() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("SHUTDOWN", fontSize = 8.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WindowsBottomTaskbar(
    viewModel: VirtualSystemViewModel,
    uiState: UiState,
    onProFeatureLocked: () -> Unit
) {
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
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable {
                            viewModel.toggleStartMenu()
                        }
                        .background(if (uiState.isStartMenuOpen) Color(0xFF00FFCC).copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(4.dp))
                        .border(1.dp, if (uiState.isStartMenuOpen) Color(0xFF00FFCC).copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Start Button Menu",
                        tint = if (uiState.isStartMenuOpen) Color(0xFF00FFCC) else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                listOf(
                    WindowType.CHROME to "chrome",
                    WindowType.EXPLORER to "explorer",
                    WindowType.CMD to "cmd",
                    WindowType.SETTINGS to "settings",
                    WindowType.TASK_MANAGER to "taskmgr",
                    WindowType.PRINTER_SCANNER to "printer",
                    WindowType.NOTEPAD to "documents",
                    WindowType.PACK_INSTALLER to "installer"
                ).forEach { (type, iconName) ->
                    val isOpen = uiState.windows.any { it.type == type }
                    val isActive = uiState.activeWindow == type
                    
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable {
                                viewModel.openWindow(type)
                            }
                            .background(
                                if (isActive) Color.White.copy(alpha = 0.08f) 
                                else Color.Transparent, 
                                RoundedCornerShape(4.dp)
                            )
                            .border(
                                1.dp, 
                                if (isActive) Color.White.copy(alpha = 0.2f) else Color.Transparent, 
                                RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        WindowsIcon(
                            iconName = iconName,
                            modifier = Modifier.size(22.dp)
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
                IconButton(onClick = { if (uiState.isProActivated) viewModel.toggleVirtualKeyboard() else onProFeatureLocked() }, modifier = Modifier.size(28.dp)) {
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
    var selectedKeyboardTab by remember { mutableStateOf(0) } // 0 = Modifiers Only, 1 = QWERTY Full
    
    val modifierRows = listOf(
        listOf("ESC", "F1", "F2", "F5", "F8", "F10", "F12"),
        listOf("CTRL", "ALT", "SHIFT", "TAB", "ENTER", "BKSP"),
        listOf("◄", "▲", "▼", "►")
    )
    
    val qwertyRows = listOf(
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L", "BKSP"),
        listOf("Z", "X", "C", "V", "B", "N", "M", "SPACE", "ENTER")
    )
    
    val activeRows = if (selectedKeyboardTab == 0) modifierRows else qwertyRows

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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "VIRTUAL KEYBOARD PANEL",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    // Keyboard Mode Toggles
                    Box(
                        modifier = Modifier
                            .clickable { selectedKeyboardTab = 0 }
                            .background(if (selectedKeyboardTab == 0) Color(0xFF00FFCC) else Color(0xFF1E263D), RoundedCornerShape(4.dp))
                            .padding(vertical = 2.dp, horizontal = 6.dp)
                    ) {
                        Text("SHORTCUTS (SHIFT/CTRL/ESC)", color = if (selectedKeyboardTab == 0) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Box(
                        modifier = Modifier
                            .clickable { selectedKeyboardTab = 1 }
                            .background(if (selectedKeyboardTab == 1) Color(0xFF00FFCC) else Color(0xFF1E263D), RoundedCornerShape(4.dp))
                            .padding(vertical = 2.dp, horizontal = 6.dp)
                    ) {
                        Text("FULL LETTERS", color = if (selectedKeyboardTab == 1) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
                
                IconButton(onClick = { viewModel.toggleVirtualKeyboard() }, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close Keyboard", tint = Color.LightGray)
                }
            }

            activeRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { key ->
                        val isSpecial = key == "BKSP" || key == "ENTER" || key == "SPACE" || key == "CTRL" || key == "SHIFT" || key == "ALT" || key == "ESC"
                        val keyWidth = if (key == "SPACE") 180.dp else if (isSpecial) 60.dp else 45.dp
                        
                        Card(
                            modifier = Modifier
                                .width(keyWidth)
                                .height(38.dp)
                                .padding(horizontal = 2.dp)
                                .clickable {
                                    viewModel.handleVirtualKeyInput(key)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSpecial) Color(0xFF1E263D) else Color(0xFF28324C)
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = key,
                                    color = if (isSpecial) Color(0xFF00FFCC) else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
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
fun ProActivationDialog(
    viewModel: VirtualSystemViewModel,
    onClose: () -> Unit
) {
    var enteredKey by remember { mutableStateOf("") }
    var keyError by remember { mutableStateOf<String?>(null) }
    var showTrialCodeGenerator by remember { mutableStateOf(false) }
    var generatedTrialKey by remember { mutableStateOf("") }

    if (showTrialCodeGenerator) {
        AlertDialog(
            onDismissRequest = { showTrialCodeGenerator = false },
            containerColor = Color(0xFF080D1A),
            shape = RoundedCornerShape(8.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = "Buy", tint = Color(0xFF00FFCC))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SPONSOR KEY GENERATOR", color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Thank you for supporting hypervisor research! Copy this active key to unlock premium features inside the guest platform:",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF030712))
                            .border(1.dp, Color(0xFF00FFCC), RoundedCornerShape(4.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("YOUR PRODUCT SERIAL KEY:", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = generatedTrialKey,
                                color = Color(0xFF00FFCC),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.activateLicenseKey(generatedTrialKey)
                        showTrialCodeGenerator = false
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC))
                ) {
                    Text("AUTO-ACTIVATE NOW", color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = Color(0xFF0F1322),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp)),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock", tint = Color(0xFF00FFCC), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "GATEUP HYPERVISOR PRO LOCKED",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "The integrated Virtual Touchpad and shortcut-modifier overlays require a registered GateUp Pro license key. Upgrade your free core translation stack to enjoy premium peripherals.",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontFamily = FontFamily.Monospace
                )
                
                OutlinedTextField(
                    value = enteredKey,
                    onValueChange = {
                        enteredKey = it
                        keyError = null
                    },
                    label = { Text("Enter 20-digit License Key", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00FFCC),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = Color(0xFF00FFCC),
                        unfocusedLabelColor = Color.Gray
                    ),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                )

                if (keyError != null) {
                    Text(keyError!!, color = Color.Red, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val codes = listOf(
                                "GTUP-8842-X86F-901D-KERN",
                                "GATEUP-PRO-2026",
                                "GTUP-PLAY-GROUND-2026"
                            )
                            generatedTrialKey = codes.random()
                            showTrialCodeGenerator = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E263D)),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("SIMULATE PURCHASE", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    
                    Button(
                        onClick = {
                            val active = viewModel.activateLicenseKey(enteredKey)
                            if (active) {
                                onClose()
                            } else {
                                keyError = "Invalid License Key! Click Simulate Purchase to get a mock code."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ACTIVATE", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        },
        confirmButton = {}
    )
}

// Helper Shortcut representation
data class ShortcutItem(
    val type: WindowType,
    val title: String,
    val icon: ImageVector,
    val iconColor: Color
)
