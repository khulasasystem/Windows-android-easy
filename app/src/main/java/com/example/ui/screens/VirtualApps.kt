package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.random.Random

// --- Functional Terminal Shell App Composable ---
@Composable
fun TerminalAppContent(
    virtualRoot: VirtualFile,
    onLaunchWindow: (AppType, String, String, String) -> Unit,
    textColor: Color
) {
    var commandInput by remember { mutableStateOf("") }
    var currentPathSegments by remember { mutableStateOf(listOf("Desktop")) }
    var consoleHistory by remember {
        mutableStateOf(
            listOf(
                "Microsoft Windows OS Terminal Shell v11.5 [Version 10.0.22621]",
                "(c) Microsoft Corporation. All rights reserved.",
                "Simulated environment path active: C:\\Users\\Admin\\Desktop",
                "Type 'dir' or 'ls' to see local files. Type 'help' for support.",
                ""
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(12.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(consoleHistory) { line ->
                Text(
                    text = line,
                    color = Color(0xFF00FF66),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x3300FF66), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val pathStr = if (currentPathSegments.isEmpty()) "" else "\\" + currentPathSegments.joinToString("\\")
            Text("C:\\Users\\Admin$pathStr> ", color = Color(0xFF00FF66), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF00FF66),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true
            )
            IconButton(
                onClick = {
                    if (commandInput.isNotBlank()) {
                        val input = commandInput.trim()
                        val parts = input.split(" ")
                        val cmd = parts[0].lowercase()
                        val arg = if (parts.size > 1) parts.drop(1).joinToString(" ") else ""

                        val currentDir = virtualRoot.resolvePath(currentPathSegments)

                        val response = when (cmd) {
                            "help" -> listOf(
                                "Available terminal commands:",
                                "  help            - Show available command listings",
                                "  dir / ls        - List files in current folder",
                                "  cd <folder>     - Change active working directory",
                                "  cd ..           - Move up to parent directory",
                                "  cat / type <f>  - View file content in console",
                                "  ./<program.exe> - Run executable app script",
                                "  system / ver    - Fetch operating system specs",
                                "  clear / cls     - Flush the terminal console buffer"
                            )
                            "system", "ver" -> listOf(
                                "OS: Windows Virtual Environment Shell v11.5",
                                "Build: AI Studio Jetpack Compose Engine 2.1",
                                "Active Terminal Thread ID: " + UUID.randomUUID().toString().take(8).uppercase(),
                                "Storage FS Mode: Sandboxed Virtual Tree State Layer"
                            )
                            "clear", "cls" -> emptyList()
                            "dir", "ls" -> {
                                val results = mutableListOf<String>()
                                results.add(" Directory of C:\\Users\\Admin\\" + currentPathSegments.joinToString("\\"))
                                results.add("")
                                var dirCount = 0
                                var fileCount = 0
                                currentDir.children.forEach { child ->
                                    if (child.isDirectory) {
                                        results.add("  <DIR>    ${child.name}")
                                        dirCount++
                                    } else {
                                        results.add("           ${child.name}  (${child.content.length} bytes)")
                                        fileCount++
                                    }
                                }
                                results.add("")
                                results.add("     $fileCount File(s), $dirCount Dir(s)")
                                results
                            }
                            "cd" -> {
                                if (arg == "..") {
                                    if (currentPathSegments.isNotEmpty()) {
                                        currentPathSegments = currentPathSegments.dropLast(1)
                                        listOf("Navigated to parent directory.")
                                    } else {
                                        listOf("Already at virtual Root.")
                                    }
                                } else if (arg.isNotEmpty()) {
                                    val match = currentDir.children.find { it.name.equals(arg, ignoreCase = true) && it.isDirectory }
                                    if (match != null) {
                                        currentPathSegments = currentPathSegments + match.name
                                        listOf("Navigated to path Segment: ${match.name}")
                                    } else {
                                        listOf("Directory not found: $arg")
                                    }
                                } else {
                                    listOf("Usage: cd <directory_name>")
                                }
                            }
                            "cat", "type" -> {
                                if (arg.isNotEmpty()) {
                                    val match = currentDir.children.find { it.name.equals(arg, ignoreCase = true) && !it.isDirectory }
                                    if (match != null) {
                                        match.content.split("\n")
                                    } else {
                                        listOf("File not found: $arg")
                                    }
                                } else {
                                    listOf("Usage: cat <filename>")
                                }
                            }
                            else -> {
                                val cleanCmd = cmd.removePrefix("./").trim()
                                val match = currentDir.children.find { it.name.equals(cleanCmd, ignoreCase = true) && !it.isDirectory && it.name.endsWith(".exe", ignoreCase = true) }
                                if (match != null) {
                                    onLaunchWindow(
                                        AppType.EXE_RUNNER,
                                        "Running: ${match.name}",
                                        match.name,
                                        match.content
                                    )
                                    listOf("Launching program process ${match.name}...", "Simulated binary PID allocated.")
                                } else {
                                    listOf("Command or program not recognized: '$cmd'. Type 'help' for instructions.")
                                }
                            }
                        }

                        consoleHistory = if (cmd == "clear" || cmd == "cls") {
                            emptyList()
                        } else {
                            val pathStrWithPrefix = if (currentPathSegments.isEmpty()) "" else "\\" + currentPathSegments.joinToString("\\")
                            consoleHistory + "C:\\Users\\Admin$pathStrWithPrefix> $input" + response + ""
                        }
                        commandInput = ""
                    }
                }
            ) {
                Icon(Icons.Default.Send, "Execute", tint = Color(0xFF00FF66))
            }
        }
    }
}

// --- Beautiful File Explorer Composable ---
@Composable
fun ExplorerAppContent(
    virtualRoot: VirtualFile,
    onUpdateFileSystem: (VirtualFile) -> Unit,
    onLaunchWindow: (AppType, String, String, String) -> Unit,
    textColor: Color
) {
    var currentPathSegments by remember { mutableStateOf(listOf<String>()) }
    val currentFolder = remember(virtualRoot, currentPathSegments) {
        virtualRoot.resolvePath(currentPathSegments)
    }
    var selectedFile by remember { mutableStateOf<VirtualFile?>(null) }
    
    var isCreatingFile by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var isFolderCreation by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Toolbar Navigation Path
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F5F9))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = {
                    if (currentPathSegments.isNotEmpty()) {
                        currentPathSegments = currentPathSegments.dropLast(1)
                        selectedFile = null
                    }
                },
                enabled = currentPathSegments.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = if (currentPathSegments.isNotEmpty()) Color.Black else Color.LightGray,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = "Root / " + currentPathSegments.joinToString(" / "),
                fontSize = 11.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    isFolderCreation = false
                    newFileName = ""
                    isCreatingFile = true
                }
            ) {
                Icon(Icons.Default.Add, "Create File", tint = Color(0xFF03A9F4), modifier = Modifier.size(18.dp))
            }

            IconButton(
                onClick = {
                    isFolderCreation = true
                    newFileName = ""
                    isCreatingFile = true
                }
            ) {
                Icon(Icons.Default.CreateNewFolder, "Create Folder", tint = Color(0xFFFFC107), modifier = Modifier.size(18.dp))
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // Left sidebar: Folder contents list
            LazyColumn(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .background(Color(0xFFEFF6FF))
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(currentFolder.children) { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedFile?.name == file.name) Color(0x332196F3) else Color.Transparent)
                            .clickable {
                                if (file.isDirectory) {
                                    currentPathSegments = currentPathSegments + file.name
                                    selectedFile = null
                                } else {
                                    selectedFile = file
                                }
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                                contentDescription = file.name,
                                tint = if (file.isDirectory) Color(0xFFFFC107) else Color(0xFF2196F3),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = file.name,
                                color = Color.Black,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = {
                                val updated = virtualRoot.deleteFileOrFolder(currentPathSegments, file.name)
                                onUpdateFileSystem(updated)
                                if (selectedFile?.name == file.name) {
                                    selectedFile = null
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Delete, "Delete File", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Right side: file preview panel
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
                    .background(Color.White)
                    .padding(12.dp)
            ) {
                if (selectedFile != null) {
                    val file = selectedFile!!
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = file.name,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (file.name.endsWith(".exe", ignoreCase = true)) "Type: Executable Application" else "Type: Text Document",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))

                        if (file.name.endsWith(".exe", ignoreCase = true)) {
                            Button(
                                onClick = {
                                    onLaunchWindow(
                                        AppType.EXE_RUNNER,
                                        "Running: ${file.name}",
                                        file.name,
                                        file.content
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Execute Program", color = Color.White, fontSize = 11.sp)
                            }
                        } else if (file.name.endsWith(".txt", ignoreCase = true)) {
                            Button(
                                onClick = {
                                    onLaunchWindow(
                                        AppType.NOTEPAD,
                                        "Editing: ${file.name}",
                                        file.name,
                                        file.content
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4)),
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit in Notepad", color = Color.White, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Content Preview:", color = Color.Gray, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(4.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = file.content,
                                color = Color.Black,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Info, "Help", tint = Color.LightGray, modifier = Modifier.size(32.dp))
                            Text("Double click folders to enter. Click files to view and run custom scripts.", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }

    if (isCreatingFile) {
        AlertDialog(
            onDismissRequest = { isCreatingFile = false },
            title = { Text(if (isFolderCreation) "Create Directory" else "Create File", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isFolderCreation) "Enter name for the new folder:" else "Enter filename (use .exe for scripts, .txt for documents):",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        placeholder = { Text(if (isFolderCreation) "NewFolder" else "MyScript.exe", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newFileName.trim()
                        if (trimmed.isNotEmpty()) {
                            val newFile = if (isFolderCreation) {
                                VirtualFile(name = trimmed, isDirectory = true)
                            } else {
                                val templateContent = if (trimmed.endsWith(".exe", ignoreCase = true)) {
                                    "print \"Executing $trimmed...\"\ncolor green\nwait 1000\nprint \"Hello World\"\nbeep"
                                } else {
                                    "Created with OS Simulator 11 text editor."
                                }
                                VirtualFile(name = trimmed, isDirectory = false, content = templateContent)
                            }
                            val updated = virtualRoot.updateFileOrFolder(currentPathSegments, newFile)
                            onUpdateFileSystem(updated)
                            isCreatingFile = false
                            newFileName = ""
                        }
                    }
                ) {
                    Text("Create", fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { isCreatingFile = false }) {
                    Text("Cancel", fontSize = 13.sp)
                }
            }
        )
    }
}

// --- Notepad App Content Composable ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotepadAppContent(
    virtualRoot: VirtualFile,
    onUpdateFileSystem: (VirtualFile) -> Unit,
    onLaunchWindow: (AppType, String, String, String) -> Unit,
    textColor: Color
) {
    var fileName by remember { mutableStateOf("MyScript.exe") }
    var fileContent by remember {
        mutableStateOf(
            "print \"Launching Notepad Custom Script...\"\n" +
            "color cyan\n" +
            "wait 500\n" +
            "beep\n" +
            "print \"Running sequence check...\"\n" +
            "wait 800\n" +
            "sysinfo\n" +
            "wait 1000\n" +
            "msgbox \"Congratulations! You created and compiled this executable using Notepad!\"\n" +
            "color green\n" +
            "print \"Finished script execution!\""
        )
    }
    var selectedFolder by remember { mutableStateOf("Desktop") }
    val folders = listOf("Desktop", "Documents", "Downloads")
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F5F9))
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = fileName,
                onValueChange = { fileName = it },
                label = { Text("Filename", fontSize = 9.sp) },
                singleLine = true,
                modifier = Modifier.width(130.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )

            Box {
                Button(
                    onClick = { isDropdownExpanded = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCBD5E1)),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(selectedFolder, color = Color.Black, fontSize = 11.sp)
                    Icon(Icons.Default.ArrowDropDown, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                }
                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false }
                ) {
                    folders.forEach { folderName ->
                        DropdownMenuItem(
                            text = { Text(folderName, fontSize = 12.sp) },
                            onClick = {
                                selectedFolder = folderName
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val updated = virtualRoot.updateFileOrFolder(listOf(selectedFolder), VirtualFile(fileName, false, fileContent))
                    onUpdateFileSystem(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4)),
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Save, null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save", color = Color.White, fontSize = 11.sp)
            }

            if (fileName.endsWith(".exe", ignoreCase = true)) {
                Button(
                    onClick = {
                        val updated = virtualRoot.updateFileOrFolder(listOf(selectedFolder), VirtualFile(fileName, false, fileContent))
                        onUpdateFileSystem(updated)
                        onLaunchWindow(
                            AppType.EXE_RUNNER,
                            "Running: $fileName",
                            fileName,
                            fileContent
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Run .EXE", color = Color.White, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = fileContent,
            onValueChange = { fileContent = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            textStyle = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Color.Black
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFFAFAFA),
                unfocusedContainerColor = Color(0xFFFAFAFA),
                focusedBorderColor = Color(0xFFE2E8F0),
                unfocusedBorderColor = Color(0xFFE2E8F0)
            ),
            placeholder = { Text("Write your custom .exe script commands here...", color = Color.Gray, fontSize = 11.sp) }
        )
    }
}

// --- Dynamic Glowing Matrix Rain Animation ---
@Composable
fun CyberRainAnimation() {
    var columns by remember { mutableStateOf(listOf<Float>()) }
    var speeds by remember { mutableStateOf(listOf<Float>()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(50)
            columns = columns.mapIndexed { idx, y ->
                val speed = speeds.getOrElse(idx) { 8f }
                if (y > 600f) {
                    if ((0..5).random() == 0) -50f else y + speed
                } else {
                    y + speed
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
    ) {
        val width = maxWidth.value
        val colCount = (width / 16).toInt().coerceAtLeast(1)

        if (columns.size != colCount) {
            columns = List(colCount) { (-200..400).random().toFloat() }
            speeds = List(colCount) { (4..12).random().toFloat() }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            columns.forEachIndexed { i, y ->
                val x = i * 16.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x3300FF66),
                            Color(0xFF00FF66)
                        ),
                        startY = y - 120f,
                        endY = y
                    ),
                    topLeft = Offset(x, y - 120f),
                    size = androidx.compose.ui.geometry.Size(3.dp.toPx(), 120f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5f.dp.toPx(),
                    center = Offset(x + 1.5f.dp.toPx(), y)
                )
            }
        }
    }
}

// --- Classic Retro Snake Game ---
@Composable
fun RetroSnakeGame() {
    var snake by remember { mutableStateOf(listOf(Pair(10, 10), Pair(10, 11), Pair(10, 12))) }
    var food by remember { mutableStateOf(Pair(5, 5)) }
    var dir by remember { mutableStateOf(Pair(0, -1)) }
    var score by remember { mutableStateOf(0) }
    var highScore by remember { mutableStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }

    LaunchedEffect(dir, isGameOver) {
        if (isGameOver) return@LaunchedEffect
        while (true) {
            delay(150)
            val head = snake.first()
            val newHead = Pair(head.first + dir.first, head.second + dir.second)

            if (newHead.first !in 0..19 || newHead.second !in 0..19 || snake.contains(newHead)) {
                isGameOver = true
                if (score > highScore) highScore = score
                break
            }

            val updatedSnake = mutableListOf(newHead) + snake
            if (newHead == food) {
                score += 10
                var nextFood = Pair((0..19).random(), (0..19).random())
                while (snake.contains(nextFood)) {
                    nextFood = Pair((0..19).random(), (0..19).random())
                }
                food = nextFood
                snake = updatedSnake
            } else {
                snake = updatedSnake.dropLast(1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Score: $score", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text("High Score: $highScore", color = Color(0xFFFFC107), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }

        Box(
            modifier = Modifier
                .size(220.dp)
                .border(2.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                .background(Color(0xFF020617))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellW = size.width / 20f
                val cellH = size.height / 20f

                for (i in 1..19) {
                    drawLine(Color(0x11FFFFFF), Offset(i * cellW, 0f), Offset(i * cellW, size.height))
                    drawLine(Color(0x11FFFFFF), Offset(0f, i * cellH), Offset(size.width, i * cellH))
                }

                drawRect(
                    color = Color.Red,
                    topLeft = Offset(food.first * cellW + 1f, food.second * cellH + 1f),
                    size = androidx.compose.ui.geometry.Size(cellW - 2f, cellH - 2f)
                )

                snake.forEachIndexed { index, segment ->
                    val color = if (index == 0) Color(0xFF00FF66) else Color(0xFF00B34A)
                    drawRect(
                        color = color,
                        topLeft = Offset(segment.first * cellW + 1f, segment.second * cellH + 1f),
                        size = androidx.compose.ui.geometry.Size(cellW - 2f, cellH - 2f)
                    )
                }
            }

            if (isGameOver) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xCC020617)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("GAME OVER", color = Color.Red, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                snake = listOf(Pair(10, 10), Pair(10, 11), Pair(10, 12))
                                food = Pair(5, 5)
                                dir = Pair(0, -1)
                                score = 0
                                isGameOver = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4))
                        ) {
                            Text("Restart Game", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            IconButton(
                onClick = { if (dir.second == 0) dir = Pair(0, -1) },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
            ) {
                Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (dir.first == 0) dir = Pair(-1, 0) },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                ) {
                    Icon(Icons.Default.KeyboardArrowLeft, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.size(24.dp))

                IconButton(
                    onClick = { if (dir.first == 0) dir = Pair(1, 0) },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                ) {
                    Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            IconButton(
                onClick = { if (dir.second == 0) dir = Pair(0, 1) },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
            ) {
                Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// --- Retro 3D Screensaver Walkthrough Corridor ---
@Composable
fun Retro3DMaze() {
    var ticks by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30)
            ticks += 0.05f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF03001E))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            val rand = Random(42)
            for (s in 0..40) {
                val sx = rand.nextFloat() * w
                val sy = rand.nextFloat() * h
                val sizeStar = (1..3).random(rand).toFloat()
                drawCircle(Color(0xFF7F00FF).copy(alpha = 0.5f), sizeStar, Offset(sx, sy))
            }

            val numWalls = 6
            val cycle = ticks % 1.0f

            val turnPhase = (ticks / 4.0f).toInt() % 4
            val turnProgress = (ticks % 4.0f) / 4.0f
            val xOffset = if (turnPhase == 1) {
                kotlin.math.sin(turnProgress * Math.PI.toFloat()) * 80f
            } else if (turnPhase == 3) {
                -kotlin.math.sin(turnProgress * Math.PI.toFloat()) * 80f
            } else {
                0f
            }

            for (i in numWalls downTo 0) {
                val progress = (i + cycle) / numWalls.toFloat()
                val sizePct = progress * progress
                val rw = w * 0.9f * sizePct
                val rh = h * 0.9f * sizePct

                if (rw > 10f && rh > 10f) {
                    val color = if (i % 2 == 0) Color(0xFF00F2FE) else Color(0xFF4FACFE)
                    val alpha = (1.0f - progress).coerceIn(0f, 1f)
                    val strokeWidth = (1f + (5f * progress))

                    drawRect(
                        color = color.copy(alpha = alpha),
                        topLeft = Offset(cx - rw / 2f + xOffset * (1.0f - progress), cy - rh / 2f),
                        size = androidx.compose.ui.geometry.Size(rw, rh),
                        style = Stroke(width = strokeWidth)
                    )
                }
            }

            val floorY = cy + 20f
            for (lineIdx in -5..5) {
                val startX = cx + (lineIdx * 50f)
                val endX = cx + (lineIdx * 500f)
                drawLine(
                    color = Color(0xFFFF007F).copy(alpha = 0.4f),
                    start = Offset(startX + xOffset * 0.2f, floorY),
                    end = Offset(endX + xOffset, h),
                    strokeWidth = 2f
                )
            }
            for (g in 1..5) {
                val progress = g / 5f
                val gy = floorY + (h - floorY) * progress * progress
                drawLine(
                    color = Color(0xFFFF007F).copy(alpha = 0.3f),
                    start = Offset(0f, gy),
                    end = Offset(w, gy),
                    strokeWidth = 1.5f
                )
            }

            drawRect(
                color = Color(0x33000000),
                topLeft = Offset(10f, 10f),
                size = androidx.compose.ui.geometry.Size(200f, 60f)
            )
            drawRect(
                color = Color(0xFF00F2FE),
                topLeft = Offset(10f, 10f),
                size = androidx.compose.ui.geometry.Size(200f, 60f),
                style = Stroke(width = 1f)
            )
        }

        Column(
            modifier = Modifier
                .padding(16.dp)
                .background(Color(0x99000000))
                .padding(8.dp)
        ) {
            Text("Retro3D Engine v1.0", color = Color(0xFF00F2FE), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("Sector: DELTA-9", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text("Status: RUNNING", color = Color(0xFF00FF66), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

// --- Isolated Executable Sandbox Runner Composable ---
@Composable
fun ExeRunnerAppContent(
    fileName: String,
    fileContent: String,
    getRealRamInfo: () -> Pair<Long, Long>,
    getRealStorageInfo: () -> Pair<Long, Long>,
    onCloseWindow: () -> Unit
) {
    if (fileContent == "SYSTEM_APP:MATRIX") {
        CyberRainAnimation()
        return
    }
    if (fileContent == "SYSTEM_APP:SNAKE") {
        RetroSnakeGame()
        return
    }
    if (fileContent == "SYSTEM_APP:MAZE") {
        Retro3DMaze()
        return
    }

    val lines = remember(fileContent) { fileContent.split("\n") }
    var currentLineIdx by remember { mutableIntStateOf(0) }
    var outputLog by remember { mutableStateOf(listOf("Initializing EXE Sandbox Environment...", "Loaded Executable File: $fileName", "")) }
    var shellBgColor by remember { mutableStateOf(Color(0xFF0F172A)) }
    var shellTextColor by remember { mutableStateOf(Color(0xFF00FF66)) }
    var isRunning by remember { mutableStateOf(true) }
    var showDialogMsg by remember { mutableStateOf<String?>(null) }
    var isPausedForDialog by remember { mutableStateOf(false) }

    LaunchedEffect(fileContent) {
        delay(300)
        for (idx in lines.indices) {
            currentLineIdx = idx
            val line = lines[idx].trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue

            if (line.startsWith("print ", ignoreCase = true)) {
                val text = line.substring(6).removeSurrounding("\"")
                outputLog = outputLog + text
            } else if (line.startsWith("color ", ignoreCase = true)) {
                val colorStr = line.substring(6).trim().lowercase()
                shellTextColor = when (colorStr) {
                    "green" -> Color(0xFF00FF66)
                    "red" -> Color(0xFFEF4444)
                    "blue" -> Color(0xFF3B82F6)
                    "cyan" -> Color(0xFF06B6D4)
                    "yellow" -> Color(0xFFF59E0B)
                    "white" -> Color(0xFFFFFFFF)
                    else -> Color(0xFF00FF66)
                }
            } else if (line.startsWith("wait ", ignoreCase = true)) {
                val delayMs = line.substring(5).trim().toLongOrNull() ?: 500L
                delay(delayMs)
            } else if (line.equals("clear", ignoreCase = true)) {
                outputLog = emptyList()
            } else if (line.startsWith("msgbox ", ignoreCase = true)) {
                val msg = line.substring(7).removeSurrounding("\"")
                showDialogMsg = msg
                isPausedForDialog = true
                while (isPausedForDialog) {
                    delay(100)
                }
            } else if (line.equals("beep", ignoreCase = true)) {
                outputLog = outputLog + "*SYSTEM BEEP*"
            } else if (line.equals("sysinfo", ignoreCase = true)) {
                val ram = getRealRamInfo()
                val storage = getRealStorageInfo()
                outputLog = outputLog + listOf(
                    "--- SYSTEM SPECIFICATIONS ---",
                    "MEM TOTAL: ${String.format("%.2f", ram.first / (1024.0 * 1024.0 * 1024.0))} GB",
                    "MEM USED : ${String.format("%.2f", ram.second / (1024.0 * 1024.0 * 1024.0))} GB",
                    "HDD TOTAL: ${String.format("%.1f", storage.first / (1024.0 * 1024.0 * 1024.0))} GB",
                    "HDD USED : ${String.format("%.1f", storage.second / (1024.0 * 1024.0 * 1024.0))} GB",
                    "-----------------------------"
                )
            } else {
                outputLog = outputLog + "Executing: $line"
                delay(150)
            }
        }
        isRunning = false
        outputLog = outputLog + "" + "[Execution finished successfully with Exit Code 0]"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(shellBgColor)
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(shellTextColor.copy(alpha = 0.1f))
                    .border(1.dp, shellTextColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRunning) "Sandbox Interpreter: Line ${currentLineIdx + 1}/${lines.size}" else "Process Completed",
                    color = shellTextColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                LinearProgressIndicator(
                    progress = if (lines.isNotEmpty()) (currentLineIdx + 1).toFloat() / lines.size.toFloat() else 1.0f,
                    modifier = Modifier.width(100.dp),
                    color = shellTextColor,
                    trackColor = shellTextColor.copy(alpha = 0.2f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(outputLog) { logLine ->
                    Text(
                        text = logLine,
                        color = shellTextColor,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        if (showDialogMsg != null) {
            AlertDialog(
                onDismissRequest = {
                    isPausedForDialog = false
                    showDialogMsg = null
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Info, "Notification", tint = Color(0xFF03A9F4))
                        Text("Simulated App Dialog", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Text(showDialogMsg!!, fontSize = 13.sp)
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isPausedForDialog = false
                            showDialogMsg = null
                        }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

data class AIAgentMessage(
    val sender: String, // "User" or "Copilot"
    val text: String,
    val time: String
)

@Composable
fun SearchingWaveAnimation(
    searchTerm: String,
    onFinished: (List<VirtualFile>) -> Unit,
    virtualRoot: VirtualFile
) {
    var radiusFraction by remember { mutableFloatStateOf(0f) }
    var currentScannedFile by remember { mutableStateOf("") }
    
    // Collect all files recursively to simulate scanning them
    val allFiles = remember(virtualRoot) {
        val list = mutableListOf<VirtualFile>()
        fun traverse(node: VirtualFile) {
            if (!node.isDirectory) {
                list.add(node)
            } else {
                node.children.forEach { traverse(it) }
            }
        }
        traverse(virtualRoot)
        list
    }

    LaunchedEffect(Unit) {
        // Run a sweeping wave animation
        for (i in 1..50) {
            radiusFraction = (i / 50f)
            if (allFiles.isNotEmpty()) {
                currentScannedFile = allFiles[(i % allFiles.size)].name
            }
            delay(50)
        }
        // Filter matching files
        val matches = allFiles.filter { 
            it.name.contains(searchTerm, ignoreCase = true) || 
            it.content.contains(searchTerm, ignoreCase = true) 
        }
        onFinished(matches)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        // Glowing sonar wave lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = kotlin.math.min(size.width, size.height) / 1.5f
            
            // Draw radar sweeps
            for (ring in 1..3) {
                val currentRadius = ((radiusFraction + (ring * 0.33f)) % 1f) * maxRadius
                val alpha = (1f - (currentRadius / maxRadius)).coerceIn(0f, 1f)
                drawCircle(
                    color = Color(0xFF00FF66).copy(alpha = alpha * 0.4f),
                    radius = currentRadius,
                    center = center,
                    style = Stroke(width = 3f)
                )
            }
            
            // Draw crosshairs
            drawLine(
                color = Color(0xFF00FF66).copy(alpha = 0.2f),
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2)
            )
            drawLine(
                color = Color(0xFF00FF66).copy(alpha = 0.2f),
                start = Offset(size.width / 2, 0f),
                end = Offset(size.width / 2, size.height)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Waves,
                contentDescription = "Radar Search Wave",
                tint = Color(0xFF00FF66),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "SEARCHING WAVE ACTIVE",
                color = Color(0xFF00FF66),
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Searching for: \"$searchTerm\"",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x3300FF66))
                    .padding(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color(0xFF00FF66),
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Scanning: C:\\OS\\$currentScannedFile",
                    color = Color(0xFF00FF66),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AIAgentAppContent(
    virtualRoot: VirtualFile,
    onUpdateFileSystem: (VirtualFile) -> Unit,
    onLaunchWindow: (AppType, String, String, String) -> Unit,
    textColor: Color
) {
    var messages by remember {
        mutableStateOf(
            listOf(
                AIAgentMessage(
                    sender = "Copilot",
                    text = "Hello! I am your Offline AI OS Copilot. I can assist you with local environment diagnostics, file searches via Searching Wave, querying time & date, or writing and compiling custom desktop (.exe) applications. How can I help you today?",
                    time = "04:05 AM"
                )
            )
        )
    }

    var userInput by remember { mutableStateOf("") }
    var searchKeyword by remember { mutableStateOf("") }
    var isSearchingWaveActive by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<VirtualFile>>(emptyList()) }
    var showSearchResultsView by remember { mutableStateOf(false) }

    // Desktop App Creation state
    var selectedAppTemplateIndex by remember { mutableIntStateOf(-1) }
    val templates = remember {
        listOf(
            Triple(
                "System Health Monitor",
                "HealthMonitor.exe",
                "print \"--- INTRODUCING HEALTH MONITOR ---\"\ncolor cyan\nwait 500\nprint \"Scanning memory sectors...\"\nwait 600\nprint \"Scanning storage sectors...\"\nwait 400\nsysinfo\nwait 800\ncolor green\nprint \"All checks passed successfully!\"\nmsgbox \"System Diagnostics Completed. Performance optimal!\""
            ),
            Triple(
                "AI Oracle Fate Decoder",
                "FateDecoder.exe",
                "print \"--- CONNECTING TO DIGITAL ORACLE ---\"\ncolor yellow\nwait 500\nprint \"Decoding your simulated fate...\"\nwait 1000\ncolor cyan\nprint \"The AI Oracle says: Your code will compile on the first try today!\"\nwait 500\nbeep\nprint \"Initializing digital cascade...\"\nwait 800\nmsgbox \"Oracle Fate Decoded: Keep building and writing elegant Kotlin Compose views!\""
            ),
            Triple(
                "Retro Music Synth",
                "RetroSynth.exe",
                "print \"--- DIGITAL AUDIO SYNTHESIZER ---\"\ncolor blue\nwait 400\nprint \"Loading soundfont files...\"\nwait 500\nprint \"Playing Note: C4 (261.63 Hz)\"\nbeep\nwait 400\nprint \"Playing Note: E4 (329.63 Hz)\"\nbeep\nwait 400\nprint \"Playing Note: G4 (392.00 Hz)\"\nbeep\nwait 400\nprint \"Playing Note: C5 (523.25 Hz)\"\nbeep\nwait 600\ncolor cyan\nprint \"Synthesizer sequence complete.\"\nmsgbox \"Retro Synth composition finished playing successfully!\""
            ),
            Triple(
                "Binary Counter Loop",
                "BinaryCounter.exe",
                "print \"--- STARTING RETRO BINARY COUNTER ---\"\ncolor cyan\nwait 300\nprint \"00000001 (1)\"\nbeep\nwait 300\nprint \"00000010 (2)\"\nbeep\nwait 300\nprint \"00000011 (3)\"\nbeep\nwait 300\nprint \"00000100 (4)\"\nbeep\nwait 300\nprint \"00000101 (5)\"\nbeep\nwait 500\ncolor green\nprint \"Counter complete!\"\nmsgbox \"Binary counter finished counts 1 to 5!\""
            )
        )
    }

    var showCompileDialogSuccess by remember { mutableStateOf<String?>(null) }

    val handleSend = { input: String ->
        val trimmed = input.trim()
        if (trimmed.isNotEmpty()) {
            val cal = java.util.Calendar.getInstance()
            val timeString = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(cal.time)
            val userMsg = AIAgentMessage("User", trimmed, timeString)
            messages = messages + userMsg
            userInput = ""

            // Simple offline local rule processor
            val lower = trimmed.lowercase()
            val responseText = when {
                lower.contains("time") || lower.contains("date") || lower.contains("clock") -> {
                    val dateString = java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.getDefault()).format(cal.time)
                    val timeFull = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(cal.time)
                    "The current virtual local time is $timeFull, and the date is $dateString. Clocks are synchronized with system sensors!"
                }
                lower.contains("search") || lower.contains("wave") || lower.contains("find") || lower.contains("locate") -> {
                    // Extract search term
                    val term = trimmed.split(" ").lastOrNull() ?: "exe"
                    searchKeyword = term
                    isSearchingWaveActive = true
                    "Initiating local Searching Wave scan for query term: \"$term\"..."
                }
                lower.contains("app") || lower.contains("create") || lower.contains("compile") || lower.contains("build") -> {
                    "Sure! I have specialized developer profiles to help you create, build and compile retro desktop apps directly onto your Virtual Desktop. Select one of the app options below to start compiling!"
                }
                lower.contains("sysinfo") || lower.contains("ram") || lower.contains("storage") || lower.contains("diagnostics") || lower.contains("hardware") -> {
                    "Analyzing physical environment... All physical cores are performing optimally. Memory is balanced. Type 'sysinfo' or open 'Diagnostics App' from the desktop for full details."
                }
                lower.contains("hello") || lower.contains("hi") || lower.contains("hey") -> {
                    "Hello Administrator! Ready to build, compile, search or check local systems. Just type or choose an option!"
                }
                else -> {
                    "Interesting query! As an Offline Copilot, I can help you: \n1. Sync/Query 'Date and Time'\n2. Search with 'Searching Wave'\n3. 'Create Desktop Apps' (.exe files directly to your Desktop!)\n4. Check 'Hardware stats'\n\nTry tapping the quick-action buttons below for a live demo!"
                }
            }

            // Simulate delayed response
            messages = messages + AIAgentMessage("Copilot", responseText, timeString)
        }
    }

    if (isSearchingWaveActive) {
        SearchingWaveAnimation(
            searchTerm = searchKeyword,
            onFinished = { matches ->
                searchResults = matches
                isSearchingWaveActive = false
                showSearchResultsView = true
                val cal = java.util.Calendar.getInstance()
                val timeString = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(cal.time)
                messages = messages + AIAgentMessage(
                    sender = "Copilot",
                    text = "Searching Wave scan completed. Found ${matches.size} matching items inside the virtual directory. Tap 'View Search Results' below to view and execute them!",
                    time = timeString
                )
            },
            virtualRoot = virtualRoot
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // App header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF8E24AA))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Face, "AI Icon", tint = Color.White, modifier = Modifier.size(24.dp))
            Column {
                Text("AI Copilot Engine v2.0", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("State-Aware Offline AI Assistant", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
            }
        }

        // Action Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F5F9))
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    handleSend("Query virtual time and date")
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B7280)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(Icons.Default.Schedule, null, tint = Color.White, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Date/Time", fontSize = 10.sp, color = Color.White)
            }

            Button(
                onClick = {
                    searchKeyword = "exe"
                    isSearchingWaveActive = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(Icons.Default.Waves, null, tint = Color.Black, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Search Wave", fontSize = 10.sp, color = Color.Black)
            }

            Button(
                onClick = {
                    selectedAppTemplateIndex = 0
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(Icons.Default.Build, null, tint = Color.White, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Create App", fontSize = 10.sp, color = Color.White)
            }

            if (searchResults.isNotEmpty()) {
                Button(
                    onClick = { showSearchResultsView = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("View Results (${searchResults.size})", fontSize = 10.sp, color = Color.Black)
                }
            }
        }

        // Template compiler view overlay
        if (selectedAppTemplateIndex >= 0) {
            val template = templates[selectedAppTemplateIndex]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Copilot Compiler: ${template.first}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF8E24AA)
                        )
                        IconButton(
                            onClick = { selectedAppTemplateIndex = -1 },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, "Close", modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Writes an executable file \"${template.second}\" onto your simulated Desktop.",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(4.dp))
                            .padding(6.dp)
                    ) {
                        Text(
                            text = template.third,
                            color = Color(0xFF00FF66),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                selectedAppTemplateIndex = (selectedAppTemplateIndex + 1) % templates.size
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                            modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Next Template", fontSize = 11.sp, color = Color.Black)
                        }

                        Button(
                            onClick = {
                                // Install to Desktop
                                val updated = virtualRoot.updateFileOrFolder(
                                    listOf("Desktop"),
                                    VirtualFile(
                                        name = template.second,
                                        isDirectory = false,
                                        content = template.third
                                    )
                                )
                                onUpdateFileSystem(updated)
                                showCompileDialogSuccess = template.second
                                selectedAppTemplateIndex = -1

                                val cal = java.util.Calendar.getInstance()
                                val timeString = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(cal.time)
                                messages = messages + AIAgentMessage(
                                    sender = "Copilot",
                                    text = "Successfully created and compiled modern desktop app \"${template.second}\"! You can find it on your Desktop icon tray or run it from the File Explorer.",
                                    time = timeString
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E24AA)),
                            modifier = Modifier.weight(1.5f).height(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Compile to Desktop", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // Search Results View Overlay
        if (showSearchResultsView) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .border(1.dp, Color(0xFFFFC107), RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF5))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Searching Wave Scan Results for \"$searchKeyword\"",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.Black
                        )
                        IconButton(
                            onClick = { showSearchResultsView = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, "Close", modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    if (searchResults.isEmpty()) {
                        Text(
                            text = "No exact matches found in Virtual System memory. Showing cached internet indices: Web searching waves indicate a high correlation with the phrase: \"Simulated OS Applet sandbox running Jetpack Compose in Android physical device layer!\"",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(searchResults) { file ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFEFF6FF))
                                        .clickable {
                                            if (file.name.endsWith(".exe", ignoreCase = true)) {
                                                onLaunchWindow(
                                                    AppType.EXE_RUNNER,
                                                    "Running: ${file.name}",
                                                    file.name,
                                                    file.content
                                                )
                                            } else {
                                                onLaunchWindow(
                                                    AppType.NOTEPAD,
                                                    "Editing: ${file.name}",
                                                    file.name,
                                                    file.content
                                                )
                                            }
                                            showSearchResultsView = false
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = Color(0xFF2196F3),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(file.name, fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Medium)
                                    }
                                    Text("Run/View ➜", fontSize = 10.sp, color = Color(0xFF2196F3))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Chat Conversation Window
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                val isUser = msg.sender == "User"
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (isUser) 12.dp else 0.dp,
                                    bottomEnd = if (isUser) 0.dp else 12.dp
                                )
                            )
                            .background(if (isUser) Color(0xFF03A9F4) else Color(0xFFECEFF1))
                            .padding(10.dp)
                            .widthIn(max = 280.dp)
                    ) {
                        Text(
                            text = msg.text,
                            color = if (isUser) Color.White else Color.Black,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${msg.sender} • ${msg.time}",
                        fontSize = 9.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }

        // Input field bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(1.dp, Color(0xFFE2E8F0))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = userInput,
                onValueChange = { userInput = it },
                placeholder = { Text("Ask Copilot... (e.g., time, search, create app)", fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF8E24AA),
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
            )

            IconButton(
                onClick = { handleSend(userInput) },
                enabled = userInput.trim().isNotEmpty(),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (userInput.trim().isNotEmpty()) Color(0xFF8E24AA) else Color(0xFFECEFF1))
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (userInput.trim().isNotEmpty()) Color.White else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showCompileDialogSuccess != null) {
        AlertDialog(
            onDismissRequest = { showCompileDialogSuccess = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, "Success", tint = Color(0xFF4CAF50))
                    Text("App Compilation Succeeded", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "The application \"${showCompileDialogSuccess}\" has been successfully assembled and written to C:\\Desktop\\.\n\nYou can run it by double-clicking its icon directly from your desktop interface!",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showCompileDialogSuccess = null }
                ) {
                    Text("Awesome")
                }
            }
        )
    }
}

