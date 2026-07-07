package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
import com.example.R
import com.example.ui.OsType
import com.example.ui.UiState
import com.example.ui.VirtualSystemViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LoginLockScreen(
    viewModel: VirtualSystemViewModel,
    uiState: UiState,
    modifier: Modifier = Modifier
) {
    var isSwipedUp by remember { mutableStateOf(false) }
    var enteredPassword by remember { mutableStateOf("") }
    var showErrorMsg by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    
    // Live Clock & Date for Lock Screen
    var timeString by remember { mutableStateOf("") }
    var dateString by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        while (true) {
            val now = Date()
            timeString = timeFormat.format(now).uppercase()
            dateString = dateFormat.format(now)
            kotlinx.coroutines.delay(1000)
        }
    }

    val currentOsWallpaper = R.drawable.img_win11_wallpaper

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("login_lock_screen")
    ) {
        // Immersive background wallpaper (Blurred when login prompt is active)
        Image(
            painter = painterResource(id = currentOsWallpaper),
            contentDescription = "Lock Screen Wallpaper",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(if (isSwipedUp) 24.dp else 0.dp)
        )
        
        // Optional semi-transparent dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (isSwipedUp) 0.55f else 0.25f))
        )

        // Lock Screen Bottom Utility Icons (Shutdown, Reboot, Wifi Status)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(
                    onClick = { viewModel.rebootToBootloader() },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reboot to BIOS/Grub", tint = Color.White)
                }
                
                IconButton(
                    onClick = { /* Simulated Assistive Options */ },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "Accessibility", tint = Color.White)
                }
            }
        }

        // Toggle state container
        AnimatedContent(
            targetState = isSwipedUp,
            transitionSpec = {
                if (targetState) {
                    // Slide up transition when swiped up
                    slideInVertically { height -> -height } + fadeIn() togetherWith
                            slideOutVertically { height -> height } + fadeOut()
                } else {
                    slideInVertically { height -> height } + fadeIn() togetherWith
                            slideOutVertically { height -> -height } + fadeOut()
                }
            },
            label = "lock_screen_toggle",
            modifier = Modifier.fillMaxSize()
        ) { swiped ->
            if (!swiped) {
                // LOCK SCREEN VISUAL STATE (Huge clock and Date)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { isSwipedUp = true },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 80.dp)
                    ) {
                        Text(
                            text = timeString,
                            color = Color.White,
                            fontSize = 68.sp,
                            fontWeight = FontWeight.Light,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dateString,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif
                        )
                        
                        Spacer(modifier = Modifier.height(220.dp))
                        
                        // Breathing "Click to Unlock" cue
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val opacity by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = EaseInOut),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "cue_opacity"
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.blur(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock",
                                tint = Color.White.copy(alpha = opacity),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "CLICK OR SWIPE UP TO UNLOCK",
                                color = Color.White.copy(alpha = opacity),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            } else {
                // LOGIN SCREEN VISUAL STATE (Enter Username and Password / PIN)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .width(360.dp)
                            .padding(20.dp)
                    ) {
                        // User Avatar
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBox,
                                contentDescription = "User Avatar",
                                tint = Color.White,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Active Guest Username
                        Text(
                            text = uiState.oobeUsername,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        
                        Text(
                            text = if (uiState.currentOs == OsType.KALI) "Secure Root Shell Session" else "Local Guest Account",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(28.dp))
                        
                        // Password Textbox (only show if password is set)
                        if (uiState.oobePasswordText.isNotEmpty()) {
                            OutlinedTextField(
                                value = enteredPassword,
                                onValueChange = { 
                                    enteredPassword = it
                                    showErrorMsg = false
                                },
                                placeholder = { Text("Enter PIN or Password", color = Color.Gray, fontSize = 12.sp) },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    val success = viewModel.unlockSystem(enteredPassword)
                                    if (!success) showErrorMsg = true
                                }),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Close else Icons.Default.Lock,
                                            contentDescription = "Toggle password visibility",
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color.Black.copy(alpha = 0.4f),
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.4f),
                                    focusedBorderColor = Color(0xFF00FFCC),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                ),
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, textAlign = TextAlign.Center),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            )
                            
                            if (showErrorMsg) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Incorrect password! Please try again.",
                                    color = Color.Red,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                        } else {
                            // Pre-boot warning/notification
                            Text(
                                text = "Auto-Login Enabled. No password required.",
                                color = Color(0xFF00FFCC),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                        
                        // Sign In Trigger Button
                        Button(
                            onClick = {
                                val success = viewModel.unlockSystem(enteredPassword)
                                if (!success && uiState.oobePasswordText.isNotEmpty()) {
                                    showErrorMsg = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Text("SIGN IN", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Sign In", tint = Color.Black, modifier = Modifier.size(14.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "← GO BACK",
                                color = Color.LightGray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .clickable { isSwipedUp = false }
                                    .padding(8.dp)
                            )
                            
                            // Bypass option for ease of debug
                            Text(
                                text = "BYPASS LOCK [DEBUG]",
                                color = Color.Yellow,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .clickable { viewModel.unlockSystem(uiState.oobePasswordText) }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
