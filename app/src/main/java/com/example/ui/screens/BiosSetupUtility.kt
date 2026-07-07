package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.VirtualSystemViewModel
import com.example.ui.OsType

enum class BiosTab {
    MAIN, ADVANCED, SECURITY, BOOT, LICENSING, EXIT
}

@Composable
fun BiosSetupUtility(
    viewModel: VirtualSystemViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var activeTab by remember { mutableStateOf(BiosTab.MAIN) }
    
    // Configurable state mirrors
    var selectedCores by remember { mutableStateOf(uiState.cpuCores) }
    var selectedRam by remember { mutableStateOf(uiState.ramSizeGb) }
    var selectedDriver by remember { mutableStateOf(uiState.graphicsDriver) }
    var isDxvkEnabled by remember { mutableStateOf(uiState.isDxvkEnabled) }
    var isEsyncEnabled by remember { mutableStateOf(uiState.isEsyncEnabled) }
    
    var selectedBootDevice by remember { mutableStateOf(uiState.biosBootDevice) }
    var isSecureBootEnabled by remember { mutableStateOf(uiState.biosSecureBoot) }
    var isVirtualTpmEnabled by remember { mutableStateOf(uiState.biosVirtualTPM) }
    var selectedOs by remember { mutableStateOf(uiState.biosSelectedOs) }
    
    var enteredLicenseKey by remember { mutableStateOf("") }
    var licenseError by remember { mutableStateOf<String?>(null) }
    var showKeyGeneratorDialog by remember { mutableStateOf(false) }
    var generatedKeyText by remember { mutableStateOf("") }
    
    // Retro Blue BIOS Palette
    val biosBlue = Color(0xFF0000AA)
    val biosGrey = Color(0xFFC0C0C0)
    val biosWhite = Color(0xFFFFFFFF)
    val biosYellow = Color(0xFFFFFF55)
    val biosCyan = Color(0xFF55FFFF)
    val biosDarkBlue = Color(0xFF000080)

    if (showKeyGeneratorDialog) {
        AlertDialog(
            onDismissRequest = { showKeyGeneratorDialog = false },
            containerColor = Color(0xFF080D1A),
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 8.dp,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = "Sponsor", tint = Color(0xFF00FFCC))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SPONSOR / UNLOCK CODE GENERATOR", color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "GateUp is an open-source high-performance hypervisor sandbox. You can sponsor developers or purchase a commercial license to obtain a premium key.",
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
                            Text("YOUR SIMULATED PRO LICENSE ACTIVATION KEY:", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = generatedKeyText,
                                color = Color(0xFF00FFCC),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    
                    Text(
                        text = "Tip: Copy this code and paste it into the Licensing Tab in BIOS Setup to unlock all premium peripherals!",
                        color = Color(0xFFFFA500),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.activateLicenseKey(generatedKeyText)
                        showKeyGeneratorDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC))
                ) {
                    Text("AUTO-ACTIVATE NOW", color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(biosBlue)
            .border(4.dp, biosGrey)
            .padding(4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. TOP HEADER STATUS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(biosGrey)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "GateUp Setup Utility",
                        color = biosDarkBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            viewModel.setSpecifications(selectedCores, selectedRam, selectedDriver, isDxvkEnabled, isEsyncEnabled)
                            viewModel.updateBiosSettings(selectedBootDevice, isSecureBootEnabled, "KVM-Hybrid v4.2", isVirtualTpmEnabled, selectedOs)
                            viewModel.setBiosSetupOpen(false)
                            onClose()
                            viewModel.bootSelectedOs(selectedOs)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Boot",
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SAVE & BOOT SYSTEM",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                Text(
                    text = "Version 4.20-A10 (C) 2026 GateUp Inc.",
                    color = biosDarkBlue,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // 2. TABS BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(biosDarkBlue),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BiosTab.values().forEach { tab ->
                    val isSelected = activeTab == tab
                    val tabName = when (tab) {
                        BiosTab.MAIN -> "   Main   "
                        BiosTab.ADVANCED -> " Advanced "
                        BiosTab.SECURITY -> " Security "
                        BiosTab.BOOT -> "   Boot   "
                        BiosTab.LICENSING -> " Licensing"
                        BiosTab.EXIT -> "   Exit   "
                    }
                    Box(
                        modifier = Modifier
                            .clickable { activeTab = tab }
                            .background(if (isSelected) biosGrey else Color.Transparent)
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                    ) {
                        Text(
                            text = tabName,
                            color = if (isSelected) biosBlue else biosGrey,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clickable {
                            viewModel.setSpecifications(selectedCores, selectedRam, selectedDriver, isDxvkEnabled, isEsyncEnabled)
                            viewModel.updateBiosSettings(selectedBootDevice, isSecureBootEnabled, "KVM-Hybrid v4.2", isVirtualTpmEnabled, selectedOs)
                            viewModel.setBiosSetupOpen(false)
                            onClose()
                            viewModel.bootSelectedOs(selectedOs)
                        }
                        .background(Color(0xFFE11D48))
                        .padding(vertical = 4.dp, horizontal = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Boot",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "BOOT NOW",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Separator bar
            Divider(color = biosWhite, thickness = 1.dp)

            // 3. MAIN WORKSPACE (LEFT ITEM LIST & RIGHT CONFIG CARD)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // LEFT CONFIG DETAILS (Takes 70% width)
                Column(
                    modifier = Modifier
                        .weight(2.2f)
                        .fillMaxHeight()
                        .border(1.dp, biosWhite)
                        .background(biosBlue)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (activeTab) {
                        BiosTab.MAIN -> {
                            Text("SYSTEM INFORMATION", color = biosYellow, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            BiosRow("System Time", "[04:15:23]", biosWhite, biosCyan)
                            BiosRow("System Date", "[07/04/2026]", biosWhite, biosCyan)
                            BiosRow("BIOS Version", "GUP-v4.20-A10", biosWhite, biosCyan)
                            BiosRow("Processor Type", "Virtual AMD64 Container Engine", biosWhite, biosCyan)
                            BiosRow("Hardware Core Specs", "${uiState.cpuCores} Allocated Cores", biosWhite, biosCyan)
                            BiosRow("Memory Capacity", "${uiState.ramSizeGb} GB RAM Map", biosWhite, biosCyan)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("LICENSE ACTIVATION STATUS:", color = biosYellow, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            
                            if (uiState.isProActivated) {
                                Text("PRO LICENSE STATUS: ACTIVE / PREMIUM KEY ENABLED", color = Color.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text("Key: ${uiState.activationKey}", color = biosCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            } else {
                                Text("PRO LICENSE STATUS: UNREGISTERED / LITE (RESTRICTED)", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text("Peripherals (virtual keyboard and mouse guides) are locked in standard sandbox.", color = biosGrey, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                        
                        BiosTab.ADVANCED -> {
                            Text("ADVANCED HARDWARE ALLOCATION", color = biosYellow, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // CPU Cores Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Hypervisor CPU Cores", color = biosWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(2, 4, 6, 8).forEach { c ->
                                        val isSel = selectedCores == c
                                        Text(
                                            text = "[$c]",
                                            color = if (isSel) biosYellow else biosGrey,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier
                                                .clickable { selectedCores = c }
                                                .padding(horizontal = 4.dp)
                                        )
                                    }
                                }
                            }

                            // RAM Allocation Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Virtual System Memory", color = biosWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(4, 8, 12, 16).forEach { r ->
                                        val isSel = selectedRam == r
                                        Text(
                                            text = "[${r}G]",
                                            color = if (isSel) biosYellow else biosGrey,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier
                                                .clickable { selectedRam = r }
                                                .padding(horizontal = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Graphics Driver Selection Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Vulkan Graphics GPU Driver", color = biosWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Column(horizontalAlignment = Alignment.End) {
                                    listOf("Turnip+Zink (Recommended)", "VirGL Simulator", "LLVMpipe Software").forEach { d ->
                                        val isSel = selectedDriver == d
                                        Text(
                                            text = if (isSel) "» $d" else "  $d",
                                            color = if (isSel) biosYellow else biosGrey,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.clickable { selectedDriver = d }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Translation Layer Toggles
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("DXVK Direct3D Renderer", color = biosWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = if (isDxvkEnabled) "[Enabled]" else "[Disabled]",
                                    color = if (isDxvkEnabled) biosCyan else biosGrey,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.clickable { isDxvkEnabled = !isDxvkEnabled }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Wine-Esync Hyper-Threading", color = biosWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = if (isEsyncEnabled) "[Enabled]" else "[Disabled]",
                                    color = if (isEsyncEnabled) biosCyan else biosGrey,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.clickable { isEsyncEnabled = !isEsyncEnabled }
                                )
                            }
                        }
                        
                        BiosTab.SECURITY -> {
                            Text("SECURITY & SYSTEM PLATFORM CONFIG", color = biosYellow, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Virtual TPM (Trusted Platform Module)", color = biosWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = if (isVirtualTpmEnabled) "[Enabled]" else "[Disabled]",
                                    color = if (isVirtualTpmEnabled) biosCyan else biosGrey,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.clickable { isVirtualTpmEnabled = !isVirtualTpmEnabled }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("UEFI Secure Boot Mode", color = biosWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = if (isSecureBootEnabled) "[Enabled]" else "[Disabled]",
                                    color = if (isSecureBootEnabled) biosCyan else biosGrey,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.clickable { isSecureBootEnabled = !isSecureBootEnabled }
                                )
                            }
                            
                            BiosRow("Supervisor Password Status", "Not Installed / Cleared", biosWhite, biosCyan)
                            BiosRow("Secure Shell Sandbox Kernel", "SHA-256 Verified", biosWhite, biosCyan)
                        }
                        
                        BiosTab.BOOT -> {
                            Text("BOOT PRIORITY & OS SELECTION", color = biosYellow, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Select primary virtual operating system to boot:", color = biosGrey, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            listOf(
                                OsType.WIN11 to "Windows 11 Pro",
                                OsType.WIN10 to "Windows 10 Pro",
                                OsType.WIN7 to "Windows 7 Ultimate",
                                OsType.KALI to "Kali Linux Rolling"
                            ).forEach { (os, label) ->
                                val isSel = selectedOs == os
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedOs = os
                                            selectedBootDevice = label
                                        }
                                        .background(if (isSel) biosDarkBlue else Color.Transparent)
                                        .padding(vertical = 6.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isSel) "» " else "  ",
                                        color = biosYellow,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = when (os) {
                                            OsType.WIN11 -> "Windows 11 Pro [Virtual Environment Layer]"
                                            OsType.WIN10 -> "Windows 10 Pro [Optimized & Stripped]"
                                            OsType.WIN7 -> "Windows 7 / XP Legacy [Compatibility Mod]"
                                            OsType.KALI -> "Kali Linux Rolling [Penetration Auditing Bash]"
                                            else -> label
                                        },
                                        color = if (isSel) biosYellow else biosWhite,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                        
                        BiosTab.LICENSING -> {
                            Text("GATEUP PREMIUM HYPERVISOR LICENSING", color = biosYellow, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            if (uiState.isProActivated) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF003300)),
                                    border = BorderStroke(1.dp, Color.Green)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("✔ PRO LICENSE ACTIVATED", color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Virtual Mouse, custom keyboard guides, high performance drivers, and 8+ Core operations are fully unlocked.", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { viewModel.deactivateLicense() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Text("DEACTIVATE LICENSE KEY", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "Unlock professional peripherals, custom mouse controls, transparent short-cut keyboards, and high efficiency virtualization frameworks.",
                                    color = biosGrey,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                OutlinedTextField(
                                    value = enteredLicenseKey,
                                    onValueChange = {
                                        enteredLicenseKey = it
                                        licenseError = null
                                    },
                                    label = { Text("Enter License Key (e.g. GATEUP-PRO-2026)", color = biosGrey, fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = biosWhite,
                                        unfocusedTextColor = biosWhite,
                                        focusedBorderColor = biosYellow,
                                        unfocusedBorderColor = biosGrey,
                                        focusedLabelColor = biosYellow,
                                        unfocusedLabelColor = biosGrey
                                    ),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                )
                                
                                if (licenseError != null) {
                                    Text(licenseError!!, color = Color.Red, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val valid = viewModel.activateLicenseKey(enteredLicenseKey)
                                            if (valid) {
                                                enteredLicenseKey = ""
                                                licenseError = null
                                            } else {
                                                licenseError = "Invalid Product Code. Try again!"
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = biosYellow)
                                    ) {
                                        Text("ACTIVATE KEY", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                    
                                    Button(
                                        onClick = {
                                            val codes = listOf(
                                                "GTUP-8842-X86F-901D-KERN",
                                                "GATEUP-PRO-2026",
                                                "GTUP-HACK-L33T-9271-BIOS",
                                                "GTUP-PLAY-GROUND-2026"
                                            )
                                            generatedKeyText = codes.random()
                                            showKeyGeneratorDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = biosGrey)
                                    ) {
                                        Text("GENERATE MOCK KEY", color = biosBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                        
                        BiosTab.EXIT -> {
                            Text("SAVE & EXIT OPTIONS", color = biosYellow, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "» Exit Saving Changes",
                                    color = biosWhite,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .clickable {
                                            viewModel.setSpecifications(selectedCores, selectedRam, selectedDriver, isDxvkEnabled, isEsyncEnabled)
                                            viewModel.updateBiosSettings(selectedBootDevice, isSecureBootEnabled, "KVM-Hybrid v4.2", isVirtualTpmEnabled, selectedOs)
                                            viewModel.setBiosSetupOpen(false)
                                            onClose()
                                        }
                                        .padding(vertical = 4.dp)
                                )
                                Text(
                                    text = "» Exit Discarding Changes",
                                    color = biosWhite,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .clickable {
                                            viewModel.setBiosSetupOpen(false)
                                            onClose()
                                        }
                                        .padding(vertical = 4.dp)
                                )
                                Text(
                                    text = "» Load Optimal Setup Defaults",
                                    color = biosWhite,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .clickable {
                                            selectedCores = 4
                                            selectedRam = 8
                                            selectedDriver = "Turnip+Zink (Recommended)"
                                            isDxvkEnabled = true
                                            isEsyncEnabled = true
                                            selectedBootDevice = "Virtual HDD (SATA 0)"
                                            isSecureBootEnabled = true
                                            isVirtualTpmEnabled = true
                                        }
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // RIGHT INSTRUCTION PANE (Takes 30% width)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(1.dp, biosWhite)
                        .background(biosBlue)
                        .padding(8.dp)
                ) {
                    Text("ITEM SPECIFIC HELP", color = biosYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    val helpText = when (activeTab) {
                        BiosTab.MAIN -> "Displays system parameters. Licences may be toggled directly by typing values inside the dedicated licensing tab of this Phoenix-styled GateUp EFI ROM."
                        BiosTab.ADVANCED -> "Select physical thread allocation, memory parameters, graphics acceleration layers, and driver libraries dynamically for optimization."
                        BiosTab.SECURITY -> "Toggle Secure Boot options or virtual TPM modules required to run Windows 11 secure operating environments inside hybrid guest layers."
                        BiosTab.BOOT -> "Select default hardware device sequence for the primary execution stream of virtual kernels. Booting network PXE requires active Wi-Fi."
                        BiosTab.LICENSING -> "GateUp Premium activation options. Input code, click Activate, or use Generate Mock Key to simulate commercial activation immediately!"
                        BiosTab.EXIT -> "Save settings or revert values to standard defaults. Selecting Exit Saving Changes will apply settings and reboot directly."
                    }
                    
                    Text(
                        text = helpText,
                        color = biosWhite,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 13.sp
                    )
                }
            }

            // 4. BOTTOM KEYBOARD SHORTCUTS LEGEND
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(biosGrey)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "F1: Help | Esc: Exit Setup | ◄►: Select Tab | ▲▼: Select Item",
                    color = biosDarkBlue,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "F10: Save & Boot | Enter: Select | Double-tap items to change",
                    color = biosDarkBlue,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun BiosRow(label: String, value: String, labelColor: Color, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = labelColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = valueColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}
