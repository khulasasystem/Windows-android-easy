package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.OsType
import com.example.ui.UiState
import com.example.ui.VirtualSystemViewModel

@Composable
fun OobeSetupScreen(
    viewModel: VirtualSystemViewModel,
    uiState: UiState,
    modifier: Modifier = Modifier
) {
    val step = uiState.oobeCurrentStep
    val currentOsName = when (uiState.currentOs) {
        OsType.WIN11 -> "Windows 11 Pro"
        OsType.WIN10 -> "Windows 10 Pro"
        OsType.WIN7 -> "Windows 7 Ultimate"
        OsType.KALI -> "Kali Linux Rolling"
        else -> "Guest OS"
    }

    // OOBE Wizard State
    var username by remember { mutableStateOf(uiState.oobeUsername) }
    var computerName by remember { mutableStateOf(uiState.oobeComputerName) }
    var password by remember { mutableStateOf(uiState.oobePasswordText) }
    var isPasswordEnabled by remember { mutableStateOf(uiState.oobePasswordText.isNotEmpty()) }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // VM Config options
    var firewallEnabled by remember { mutableStateOf(true) }
    var clipboardEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var telemetryDisabled by remember { mutableStateOf(true) }
    
    var showPasswordError by remember { mutableStateOf(false) }

    // Blue/Teal gradient background reminiscent of modern OS Out of Box Experience
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Slate 900
                        Color(0xFF1E3A8A), // Blue 900
                        Color(0xFF0F172A)
                    )
                )
            )
            .testTag("oobe_setup_screen"),
        contentAlignment = Alignment.Center
    ) {
        val isCompact = maxWidth < 640.dp

        Card(
            modifier = if (isCompact) {
                Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            } else {
                Modifier
                    .width(820.dp)
                    .height(520.dp)
                    .padding(16.dp)
            }.border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0F1D).copy(alpha = 0.95f))
        ) {
            if (isCompact) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Left Panel as Compact Header Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF1E293B),
                                        Color(0xFF0F172A)
                                    )
                                )
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "OOBE Setup",
                                    tint = Color(0xFF00FFCC),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "GATEUP SETUP",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Text(
                                text = "STEP ${step + 1} OF 4",
                                color = Color(0xFF00FFCC),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Right Panel inside a scrollable column for mobile
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            // Header
                            Text(
                                text = when (step) {
                                    0 -> "Welcome & Regional Setup"
                                    1 -> "Create Guest User Account"
                                    2 -> "Guest Performance Tuning"
                                    else -> "Ready to Boot System"
                                },
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (step) {
                                    0 -> "Choose preferred language and computer identifier."
                                    1 -> "Set password permissions to safeguard memory decryption."
                                    2 -> "Optimize hypervisor services for your host device."
                                    else -> "Review specifications and launch translation kernel."
                                },
                                color = Color(0xFF64748B),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 12.dp))
                            
                            // Content
                            Box(modifier = Modifier.fillMaxWidth()) {
                                when (step) {
                                    0 -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                            Text("CHOOSE REGION", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                    .padding(12.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = "Region", tint = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(text = "Global / United States (UTC-7)", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                                }
                                            }

                                            Text("COMPUTER NAME", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            OutlinedTextField(
                                                value = computerName,
                                                onValueChange = { computerName = it },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White,
                                                    focusedBorderColor = Color(0xFF00FFCC),
                                                    unfocusedBorderColor = Color(0xFF334155)
                                                ),
                                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                    1 -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text("CREATE USER ACCOUNT", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            OutlinedTextField(
                                                value = username,
                                                onValueChange = { username = it },
                                                label = { Text("Username", color = Color.Gray, fontSize = 10.sp) },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White,
                                                    focusedBorderColor = Color(0xFF00FFCC),
                                                    unfocusedBorderColor = Color(0xFF334155)
                                                ),
                                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                                Checkbox(
                                                    checked = isPasswordEnabled,
                                                    onCheckedChange = { isPasswordEnabled = it },
                                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00FFCC))
                                                )
                                                Text("Require Password to Decrypt Guest VM", color = Color.White, fontSize = 11.sp)
                                            }

                                            if (isPasswordEnabled) {
                                                OutlinedTextField(
                                                    value = password,
                                                    onValueChange = { 
                                                        password = it
                                                        showPasswordError = false
                                                    },
                                                    label = { Text("Decryption Password", color = Color.Gray, fontSize = 10.sp) },
                                                    singleLine = true,
                                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = Color.White,
                                                        unfocusedTextColor = Color.White,
                                                        focusedBorderColor = Color(0xFF00FFCC),
                                                        unfocusedBorderColor = Color(0xFF334155)
                                                    ),
                                                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                OutlinedTextField(
                                                    value = confirmPassword,
                                                    onValueChange = { 
                                                        confirmPassword = it
                                                        showPasswordError = false
                                                    },
                                                    label = { Text("Confirm Password", color = Color.Gray, fontSize = 10.sp) },
                                                    singleLine = true,
                                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = Color.White,
                                                        unfocusedTextColor = Color.White,
                                                        focusedBorderColor = Color(0xFF00FFCC),
                                                        unfocusedBorderColor = Color(0xFF334155)
                                                    ),
                                                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                if (showPasswordError) {
                                                    Text(
                                                        text = "Passwords do not match! Please check.",
                                                        color = Color.Red,
                                                        fontSize = 11.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    2 -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text("HYPERVISOR INTEGRATIONS", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = Icons.Default.Home, contentDescription = "Firewall", tint = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Enable Guest Firewall Protection", color = Color.White, fontSize = 11.sp)
                                                }
                                                Switch(
                                                    checked = firewallEnabled,
                                                    onCheckedChange = { firewallEnabled = it },
                                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color(0xFF00FFCC))
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = Icons.Default.Share, contentDescription = "Clipboard", tint = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Bidirectional Clipboard Sharing", color = Color.White, fontSize = 11.sp)
                                                }
                                                Switch(
                                                    checked = clipboardEnabled,
                                                    onCheckedChange = { clipboardEnabled = it },
                                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color(0xFF00FFCC))
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Audio", tint = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Emulated 16-bit Virtual Audio Driver", color = Color.White, fontSize = 11.sp)
                                                }
                                                Switch(
                                                    checked = soundEnabled,
                                                    onCheckedChange = { soundEnabled = it },
                                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color(0xFF00FFCC))
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Telemetry", tint = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Optimize Out Guest Advertising & Telemetry", color = Color.White, fontSize = 11.sp)
                                                }
                                                Switch(
                                                    checked = telemetryDisabled,
                                                    onCheckedChange = { telemetryDisabled = it },
                                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color(0xFF00FFCC))
                                                )
                                            }
                                        }
                                    }
                                    else -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text("COMPILATION TARGET VERIFICATION", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF030712))
                                                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                                    .padding(12.dp)
                                            ) {
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                        Text("OPERATING SYSTEM:", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                                        Text(currentOsName, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                    }
                                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                        Text("COMPUTER IDENTIFIER:", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                                        Text(computerName, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                    }
                                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                        Text("USER ACCOUNT CREATED:", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                                        Text(username, color = Color(0xFF00FFCC), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                    }
                                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                        Text("SECURITY LAYER:", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                                        Text(if (isPasswordEnabled) "AES-256 USER LOCK OK" else "UNENCRYPTED AUTO-LOGIN", color = if (isPasswordEnabled) Color(0xFF00FFCC) else Color.Yellow, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                                    }
                                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                        Text("HARDWARE ALLOCATION:", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                                        Text("${uiState.cpuCores} Cores / ${uiState.ramSizeGb} GB / ${uiState.graphicsDriver}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))
                                            
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF1E293B).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                    .padding(10.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Status", tint = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Host integrity checks passed. Virtualization containers compiled successfully.",
                                                    color = Color.LightGray,
                                                    fontSize = 10.sp,
                                                    lineHeight = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Footer Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (step > 0) {
                                OutlinedButton(
                                    onClick = { viewModel.setOobeStep(step - 1) },
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("BACK", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        // Skip configuration, auto-generate standard credentials, and login immediately
                                        viewModel.updateOobeUsername("Admin_User")
                                        viewModel.updateOobePassword("")
                                        viewModel.updateOobeComputerName("GATEUP-PC")
                                        viewModel.completeOobeSetup()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("SKIP SETUP", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        if (step == 1 && isPasswordEnabled && password != confirmPassword) {
                                            showPasswordError = true
                                        } else if (step < 3) {
                                            viewModel.updateOobeUsername(username)
                                            viewModel.updateOobePassword(if (isPasswordEnabled) password else "")
                                            viewModel.updateOobeComputerName(computerName)
                                            viewModel.setOobeStep(step + 1)
                                        } else {
                                            viewModel.updateOobeUsername(username)
                                            viewModel.updateOobePassword(if (isPasswordEnabled) password else "")
                                            viewModel.updateOobeComputerName(computerName)
                                            viewModel.completeOobeSetup()
                                        }
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (step == 3) Color(0xFF00FFCC) else Color(0xFF1E40AF),
                                        contentColor = if (step == 3) Color.Black else Color.White
                                    )
                                ) {
                                    Text(
                                        text = if (step == 3) "START SYSTEM" else "NEXT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (step == 3) Icons.Default.PlayArrow else Icons.Default.ArrowForward,
                                        contentDescription = "Next",
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Wide Screen Side-By-Side Row Layout
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Panel: Dynamic welcome & branding
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF1E293B),
                                        Color(0xFF0F172A)
                                    )
                                )
                            )
                            .padding(28.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "OOBE Setup",
                                        tint = Color(0xFF00FFCC),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "GATEUP SETUP",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(48.dp))
                                
                                Text(
                                    text = "Setup Wizard for $currentOsName",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    lineHeight = 32.sp
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = "Let's configure your secure guest operating system. Standard sandboxing permissions will be established immediately.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                            
                            // Setup progress list indicator
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OobeStepIndicator(label = "1. Region & Identity", isActive = step == 0, isCompleted = step > 0)
                                OobeStepIndicator(label = "2. User Security", isActive = step == 1, isCompleted = step > 1)
                                OobeStepIndicator(label = "3. Sandbox Integrity", isActive = step == 2, isCompleted = step > 2)
                                OobeStepIndicator(label = "4. Final Provisioning", isActive = step == 3, isCompleted = step > 3)
                            }
                            
                            Text(
                                text = "Hypervisor Version v4.2 KVM-Hybrid",
                                color = Color(0xFF475569),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    
                    // Right Panel: Form steps content
                    Column(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                            .padding(28.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            // Header
                            Text(
                                text = when (step) {
                                    0 -> "Welcome & Regional Setup"
                                    1 -> "Create Guest User Account"
                                    2 -> "Guest Performance Tuning"
                                    else -> "Ready to Boot System"
                                },
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (step) {
                                    0 -> "Choose preferred language and computer identifier."
                                    1 -> "Set password permissions to safeguard memory decryption."
                                    2 -> "Optimize hypervisor services for your host device."
                                    else -> "Review specifications and launch translation kernel."
                                },
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 20.dp)
                            )
                            
                            Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 16.dp))
                            
                            // Content of current step
                            Box(modifier = Modifier.fillMaxWidth()) {
                                when (step) {
                                    0 -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                            Text("CHOOSE REGION", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                    .padding(12.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = "Region", tint = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(text = "Global / United States (UTC-7)", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                                }
                                            }

                                            Text("COMPUTER NAME", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            OutlinedTextField(
                                                value = computerName,
                                                onValueChange = { computerName = it },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White,
                                                    focusedBorderColor = Color(0xFF00FFCC),
                                                    unfocusedBorderColor = Color(0xFF334155)
                                                ),
                                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                    1 -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text("CREATE USER ACCOUNT", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            OutlinedTextField(
                                                value = username,
                                                onValueChange = { username = it },
                                                label = { Text("Username", color = Color.Gray, fontSize = 10.sp) },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White,
                                                    focusedBorderColor = Color(0xFF00FFCC),
                                                    unfocusedBorderColor = Color(0xFF334155)
                                                ),
                                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                                Checkbox(
                                                    checked = isPasswordEnabled,
                                                    onCheckedChange = { isPasswordEnabled = it },
                                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00FFCC))
                                                )
                                                Text("Require Password to Decrypt Guest VM", color = Color.White, fontSize = 11.sp)
                                            }

                                            if (isPasswordEnabled) {
                                                OutlinedTextField(
                                                    value = password,
                                                    onValueChange = { 
                                                        password = it
                                                        showPasswordError = false
                                                    },
                                                    label = { Text("Decryption Password", color = Color.Gray, fontSize = 10.sp) },
                                                    singleLine = true,
                                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = Color.White,
                                                        unfocusedTextColor = Color.White,
                                                        focusedBorderColor = Color(0xFF00FFCC),
                                                        unfocusedBorderColor = Color(0xFF334155)
                                                    ),
                                                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                OutlinedTextField(
                                                    value = confirmPassword,
                                                    onValueChange = { 
                                                        confirmPassword = it
                                                        showPasswordError = false
                                                    },
                                                    label = { Text("Confirm Password", color = Color.Gray, fontSize = 10.sp) },
                                                    singleLine = true,
                                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = Color.White,
                                                        unfocusedTextColor = Color.White,
                                                        focusedBorderColor = Color(0xFF00FFCC),
                                                        unfocusedBorderColor = Color(0xFF334155)
                                                    ),
                                                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                if (showPasswordError) {
                                                    Text(
                                                        text = "Passwords do not match! Please check.",
                                                        color = Color.Red,
                                                        fontSize = 11.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    2 -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text("HYPERVISOR INTEGRATIONS", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = Icons.Default.Home, contentDescription = "Firewall", tint = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Enable Guest Firewall Protection", color = Color.White, fontSize = 11.sp)
                                                }
                                                Switch(
                                                    checked = firewallEnabled,
                                                    onCheckedChange = { firewallEnabled = it },
                                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color(0xFF00FFCC))
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = Icons.Default.Share, contentDescription = "Clipboard", tint = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Bidirectional Clipboard Sharing", color = Color.White, fontSize = 11.sp)
                                                }
                                                Switch(
                                                    checked = clipboardEnabled,
                                                    onCheckedChange = { clipboardEnabled = it },
                                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color(0xFF00FFCC))
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Audio", tint = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Emulated 16-bit Virtual Audio Driver", color = Color.White, fontSize = 11.sp)
                                                }
                                                Switch(
                                                    checked = soundEnabled,
                                                    onCheckedChange = { soundEnabled = it },
                                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color(0xFF00FFCC))
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Telemetry", tint = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Optimize Out Guest Advertising & Telemetry", color = Color.White, fontSize = 11.sp)
                                                }
                                                Switch(
                                                    checked = telemetryDisabled,
                                                    onCheckedChange = { telemetryDisabled = it },
                                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color(0xFF00FFCC))
                                                )
                                            }
                                        }
                                    }
                                    else -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text("COMPILATION TARGET VERIFICATION", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF030712))
                                                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                                    .padding(12.dp)
                                            ) {
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                        Text("OPERATING SYSTEM:", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                                        Text(currentOsName, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                    }
                                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                        Text("COMPUTER IDENTIFIER:", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                                        Text(computerName, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                    }
                                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                        Text("USER ACCOUNT CREATED:", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                                        Text(username, color = Color(0xFF00FFCC), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                    }
                                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                        Text("SECURITY LAYER:", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                                        Text(if (isPasswordEnabled) "AES-256 USER LOCK OK" else "UNENCRYPTED AUTO-LOGIN", color = if (isPasswordEnabled) Color(0xFF00FFCC) else Color.Yellow, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                                    }
                                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                        Text("HARDWARE ALLOCATION:", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                                        Text("${uiState.cpuCores} Cores / ${uiState.ramSizeGb} GB / ${uiState.graphicsDriver}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))
                                            
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF1E293B).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                    .padding(10.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Status", tint = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Host integrity checks passed. Virtualization containers compiled successfully.",
                                                    color = Color.LightGray,
                                                    fontSize = 10.sp,
                                                    lineHeight = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Footer Action Buttons ("Back" and "Next/Start")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Back Button
                            if (step > 0) {
                                OutlinedButton(
                                    onClick = { viewModel.setOobeStep(step - 1) },
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("BACK", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }
                            
                            // Next / Start Button
                            Button(
                                onClick = {
                                    if (step == 1 && isPasswordEnabled && password != confirmPassword) {
                                        showPasswordError = true
                                    } else if (step < 3) {
                                        // Save state inputs so far
                                        viewModel.updateOobeUsername(username)
                                        viewModel.updateOobePassword(if (isPasswordEnabled) password else "")
                                        viewModel.updateOobeComputerName(computerName)
                                        viewModel.setOobeStep(step + 1)
                                    } else {
                                        // Complete OOBE and trigger lock screen!
                                        viewModel.updateOobeUsername(username)
                                        viewModel.updateOobePassword(if (isPasswordEnabled) password else "")
                                        viewModel.updateOobeComputerName(computerName)
                                        viewModel.completeOobeSetup()
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (step == 3) Color(0xFF00FFCC) else Color(0xFF1E40AF),
                                    contentColor = if (step == 3) Color.Black else Color.White
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                    Text(
                                        text = if (step == 3) "START SYSTEM" else "NEXT",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = if (step == 3) Icons.Default.PlayArrow else Icons.Default.ArrowForward,
                                        contentDescription = "Next",
                                        modifier = Modifier.size(14.dp)
                                    )
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
fun OobeStepIndicator(
    label: String,
    isActive: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(
                    if (isCompleted) Color(0xFF00FFCC)
                    else if (isActive) Color(0xFF1E40AF)
                    else Color(0xFF334155)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Done", tint = Color.Black, modifier = Modifier.size(12.dp))
            } else {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White))
            }
        }
        
        Text(
            text = label,
            color = if (isActive) Color.White else if (isCompleted) Color.LightGray else Color.Gray,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace
        )
    }
}
