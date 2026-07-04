package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.EnvVar
import com.example.data.VirtualFile
import com.example.data.VirtualSystemRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class OsType {
    BOOTLOADER, WIN11, WIN10, WIN7, KALI
}

enum class WindowType {
    CMD, EXPLORER, SETTINGS, CHROME, NOTEPAD, TASK_MANAGER, PRINTER_SCANNER, PACK_INSTALLER
}

data class WindowState(
    val type: WindowType,
    val title: String,
    val isMinimized: Boolean = false,
    val isMaximized: Boolean = false,
    val width: Int = 800,
    val height: Int = 500,
    val x: Int = 100,
    val y: Int = 80,
    val zIndex: Int = 1
)

data class VirtualNotification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: String,
    val isRead: Boolean = false
)

data class VirtualApp(
    val id: String,
    val name: String,
    val iconName: String,
    val isInstalled: Boolean,
    val size: String,
    val description: String,
    val category: String,
    val fileType: String // ".exe" or "linux"
)

data class UiState(
    val currentOs: OsType = OsType.BOOTLOADER,
    val isBooting: Boolean = false,
    val bootProgress: Float = 0f,
    val isStartMenuOpen: Boolean = false,
    val windows: List<WindowState> = emptyList(),
    val activeWindow: WindowType? = null,
    
    // Virtual Specs
    val cpuCores: Int = 8,
    val ramSizeGb: Int = 8,
    val storageSizeGb: Int = 512,
    val graphicsDriver: String = "Turnip+Zink (Recommended)",
    val isDxvkEnabled: Boolean = true,
    val isEsyncEnabled: Boolean = true,
    val isWifiConnected: Boolean = true,
    
    // Command Terminal State
    val terminalHistory: List<String> = listOf("Emulator console initialized. Ready."),
    val terminalCurrentDir: String = "C:\\Users\\Administrator",
    
    // File Explorer State
    val explorerPath: String = "C:\\Users\\Administrator",
    val explorerFiles: List<VirtualFile> = emptyList(),
    val selectedFile: VirtualFile? = null,
    val fileContentToEdit: String? = null,
    val editedFileName: String = "",
    val isCreatingFolder: Boolean = false,
    val isCreatingFile: Boolean = false,
    
    // Environment Variables State
    val envVars: List<EnvVar> = emptyList(),
    
    // Notifications State
    val notifications: List<VirtualNotification> = emptyList(),
    
    // Virtual Keyboard & Mouse States
    val isKeyboardOpen: Boolean = false,
    val isVirtualMouseActive: Boolean = false,
    val mouseX: Float = 640f,
    val mouseY: Float = 360f,
    
    // Downloadable Apps Store / Package installer State
    val availableApps: List<VirtualApp> = emptyList(),
    
    // Printer / Scanner Simulated State
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val isPrinting: Boolean = false,
    val printProgress: Float = 0f,
    val scannedDocResult: String? = null
)

class VirtualSystemViewModel(
    private val repository: VirtualSystemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Fetch environment variables and files
            repository.initializeIfEmpty()
            
            repository.allEnvVarsFlow.collect { envs ->
                _uiState.update { it.copy(envVars = envs) }
            }
        }
        
        viewModelScope.launch {
            repository.allFilesFlow.collect {
                // Refresh directory contents in current explorer path
                refreshExplorerFiles(_uiState.value.explorerPath)
            }
        }
        
        // Initialize simulated apps
        initializeDownloadableApps()
    }

    private fun initializeDownloadableApps() {
        _uiState.update {
            it.copy(
                availableApps = listOf(
                    VirtualApp("python", "Python 3.11.2 Dev Environment", "python", false, "124 MB", "Advanced computer programming runtime interpreter & compiler.", "Development", ".exe"),
                    VirtualApp("nodejs", "Node.js 18.15 x64 Runtime", "nodejs", false, "95 MB", "Highly scalable asynchronous JavaScript server-side compiler environment.", "Development", ".exe"),
                    VirtualApp("wireshark", "Wireshark Network Decryptor", "wireshark", false, "110 MB", "Advanced packet capturing and security network diagnostics analyzer.", "Security", ".exe"),
                    VirtualApp("nmap", "Nmap Network Security Mapper", "nmap", false, "40 MB", "Port scanning, discovery, and network topology analysis toolkit.", "Security", "linux"),
                    VirtualApp("metasploit", "Metasploit Penetration Suite", "metasploit", false, "340 MB", "Advanced security vulnerability auditing, exploitation, and post-exploitation shell simulation.", "Security", "linux"),
                    VirtualApp("notepadpp", "Notepad++ Source Editor", "notepadpp", false, "15 MB", "Lightweight, ultra-fast tabbed text compiler & code editor.", "System", ".exe"),
                    VirtualApp("vlc", "VLC Media Player Classic", "vlc", false, "80 MB", "Modern x86_64 desktop codec player for native high-definition video feeds.", "System", ".exe")
                )
            )
        }
    }

    // --- BOOT PROCESS & OS SELECT ---
    fun setSpecifications(cores: Int, ram: Int, driver: String, dxvk: Boolean, esync: Boolean) {
        _uiState.update {
            it.copy(
                cpuCores = cores,
                ramSizeGb = ram,
                graphicsDriver = driver,
                isDxvkEnabled = dxvk,
                isEsyncEnabled = esync
            )
        }
    }

    fun bootSelectedOs(os: OsType) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBooting = true, bootProgress = 0f) }
            
            // Fast loading BIOS simulator
            for (i in 1..10) {
                delay(180)
                _uiState.update { it.copy(bootProgress = i * 0.1f) }
            }
            
            // Setup defaults for corresponding path based on OS type
            val initialPath = if (os == OsType.KALI) "/root" else "C:\\Users\\Administrator"
            _uiState.update {
                it.copy(
                    isBooting = false,
                    currentOs = os,
                    explorerPath = initialPath,
                    terminalCurrentDir = initialPath
                )
            }
            
            refreshExplorerFiles(initialPath)
            
            // Add boot message to terminal
            val initMessage = when (os) {
                OsType.KALI -> listOf(
                    "Kali Linux GNU/Linux 2026.1 (kali-rolling)",
                    "Kernel: x86_64 Linux 6.6.1-kali-amd64",
                    "CPU: Virtual 8 Cores (Snapdragon / Mediatek Host)",
                    "RAM: " + _uiState.value.ramSizeGb + " GB Core Allocation",
                    "Zink GPU Hardware-acceleration: Active",
                    "Security Framework: Loaded.",
                    "Type 'help' for instructions."
                )
                else -> listOf(
                    "Microsoft Windows [Version 10.0.22631.3007]",
                    "(c) Microsoft Corporation. All rights reserved.",
                    "Virtualization architecture: AMD64/x86_64 via Box64 Wine translation.",
                    "Turnip + Zink driver initialized. Vulkan Layer: DXVK Active.",
                    "Available RAM: " + _uiState.value.ramSizeGb + " GB / System Cores: " + _uiState.value.cpuCores,
                    "Bloatware stripped successfully. RAM load: 12% optimal.",
                    "Type 'help' for a list of Windows Commands."
                )
            }
            
            _uiState.update { it.copy(terminalHistory = initMessage) }
            
            triggerNotification(
                "System Initialized",
                "Booted successfully into " + getOsFriendlyName(os) + "! Default parameters applied."
            )
        }
    }

    fun rebootToBootloader() {
        _uiState.update {
            it.copy(
                currentOs = OsType.BOOTLOADER,
                windows = emptyList(),
                activeWindow = null,
                isStartMenuOpen = false
            )
        }
    }

    private fun getOsFriendlyName(os: OsType): String = when (os) {
        OsType.BOOTLOADER -> "Bootloader Menu"
        OsType.WIN11 -> "Windows 11 Pro"
        OsType.WIN10 -> "Windows 10 Pro"
        OsType.WIN7 -> "Windows 7 Ultimate"
        OsType.KALI -> "Kali Linux Rolling"
    }

    // --- WINDOWS MANAGEMENT ---
    fun openWindow(type: WindowType) {
        _uiState.update { state ->
            val alreadyOpen = state.windows.any { it.type == type }
            val updatedWindows = if (alreadyOpen) {
                // Restore if minimized and bring to front
                state.windows.map {
                    if (it.type == type) it.copy(isMinimized = false, zIndex = getNextMaxZIndex(state.windows))
                    else it
                }
            } else {
                // Setup default sizing based on app type
                val title = when (type) {
                    WindowType.CMD -> if (state.currentOs == OsType.KALI) "root@kali: ~" else "Command Prompt"
                    WindowType.EXPLORER -> "File Explorer"
                    WindowType.SETTINGS -> if (state.currentOs == OsType.KALI) "System Configuration" else "Settings & Control Panel"
                    WindowType.CHROME -> "Google Chrome"
                    WindowType.NOTEPAD -> "Notepad editor"
                    WindowType.TASK_MANAGER -> "Task Manager"
                    WindowType.PRINTER_SCANNER -> "Peripheral Devices Management"
                    WindowType.PACK_INSTALLER -> if (state.currentOs == OsType.KALI) "APT Package Installer" else "Windows Software Center"
                }
                
                val newWindow = WindowState(
                    type = type,
                    title = title,
                    zIndex = getNextMaxZIndex(state.windows),
                    width = if (type == WindowType.CHROME) 950 else 780,
                    height = if (type == WindowType.CHROME) 560 else 460,
                    x = 40 + (state.windows.size * 25) % 150,
                    y = 30 + (state.windows.size * 25) % 100
                )
                state.windows + newWindow
            }
            
            state.copy(
                windows = updatedWindows,
                activeWindow = type,
                isStartMenuOpen = false
            )
        }
    }

    fun closeWindow(type: WindowType) {
        _uiState.update { state ->
            val updated = state.windows.filter { it.type != type }
            val newActive = updated.maxByOrNull { it.zIndex }?.type
            state.copy(
                windows = updated,
                activeWindow = newActive
            )
        }
    }

    fun minimizeWindow(type: WindowType) {
        _uiState.update { state ->
            val updated = state.windows.map {
                if (it.type == type) it.copy(isMinimized = true) else it
            }
            val newActive = updated.filter { !it.isMinimized }.maxByOrNull { it.zIndex }?.type
            state.copy(
                windows = updated,
                activeWindow = newActive
            )
        }
    }

    fun focusWindow(type: WindowType) {
        _uiState.update { state ->
            val updated = state.windows.map {
                if (it.type == type) it.copy(isMinimized = false, zIndex = getNextMaxZIndex(state.windows)) else it
            }
            state.copy(
                windows = updated,
                activeWindow = type
            )
        }
    }

    fun toggleMaximizeWindow(type: WindowType) {
        _uiState.update { state ->
            val updated = state.windows.map {
                if (it.type == type) it.copy(isMaximized = !it.isMaximized) else it
            }
            state.copy(windows = updated)
        }
    }

    fun updateWindowPosition(type: WindowType, newX: Int, newY: Int) {
        _uiState.update { state ->
            val updated = state.windows.map {
                if (it.type == type && !it.isMaximized) it.copy(x = newX, y = newY) else it
            }
            state.copy(windows = updated)
        }
    }

    private fun getNextMaxZIndex(windows: List<WindowState>): Int {
        if (windows.isEmpty()) return 1
        return (windows.maxOfOrNull { it.zIndex } ?: 0) + 1
    }

    fun toggleStartMenu() {
        _uiState.update { it.copy(isStartMenuOpen = !it.isStartMenuOpen) }
    }

    // --- NOTIFICATION ENGINE ---
    fun triggerNotification(title: String, message: String) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val timestamp = sdf.format(Date())
        val id = java.util.UUID.randomUUID().toString()
        val newNotification = VirtualNotification(id, title, message, timestamp)
        
        _uiState.update {
            it.copy(notifications = (listOf(newNotification) + it.notifications).take(15))
        }
    }

    fun markNotificationRead(id: String) {
        _uiState.update { state ->
            state.copy(
                notifications = state.notifications.map {
                    if (it.id == id) it.copy(isRead = true) else it
                }
            )
        }
    }

    fun clearNotifications() {
        _uiState.update { it.copy(notifications = emptyList()) }
    }

    // --- FILE EXPLORER ENGINE ---
    fun refreshExplorerFiles(path: String) {
        viewModelScope.launch {
            val files = repository.getFilesByParent(path)
            _uiState.update { it.copy(explorerFiles = files) }
        }
    }

    fun navigateToFolder(path: String) {
        _uiState.update { it.copy(explorerPath = path, selectedFile = null) }
        refreshExplorerFiles(path)
    }

    fun navigateUpExplorer() {
        val current = _uiState.value.explorerPath
        val isKali = _uiState.value.currentOs == OsType.KALI
        
        if (isKali) {
            if (current == "/") return
            val lastSlash = current.lastIndexOf('/')
            val parent = if (lastSlash == 0) "/" else current.substring(0, lastSlash)
            navigateToFolder(parent)
        } else {
            if (current == "C:" || current == "D:" || current == "E:") return
            val lastSlash = current.lastIndexOf('\\')
            if (lastSlash != -1) {
                val parent = current.substring(0, lastSlash)
                navigateToFolder(parent)
            }
        }
    }

    fun selectFile(file: VirtualFile?) {
        _uiState.update { it.copy(selectedFile = file) }
    }

    fun startCreateFolder() {
        _uiState.update { it.copy(isCreatingFolder = true, isCreatingFile = false, editedFileName = "") }
    }

    fun startCreateFile() {
        _uiState.update { it.copy(isCreatingFile = true, isCreatingFolder = false, editedFileName = "") }
    }

    fun updateEditedFileName(name: String) {
        _uiState.update { it.copy(editedFileName = name) }
    }

    fun cancelCreation() {
        _uiState.update { it.copy(isCreatingFolder = false, isCreatingFile = false, editedFileName = "") }
    }

    fun executeCreation() {
        val name = _uiState.value.editedFileName.trim()
        if (name.isEmpty()) {
            cancelCreation()
            return
        }
        
        viewModelScope.launch {
            val path = _uiState.value.explorerPath
            if (_uiState.value.isCreatingFolder) {
                repository.createDirectory(path, name)
                triggerNotification("Directory Created", "Folder '$name' successfully added to $path")
            } else {
                repository.createFile(path, name, "")
                triggerNotification("File Created", "File '$name' successfully created.")
            }
            _uiState.update { it.copy(isCreatingFolder = false, isCreatingFile = false, editedFileName = "") }
            refreshExplorerFiles(path)
        }
    }

    fun deleteSelectedFile() {
        val file = _uiState.value.selectedFile ?: return
        viewModelScope.launch {
            repository.deleteFile(file.path)
            triggerNotification("Item Deleted", "Deleted '${file.name}' permanently.")
            _uiState.update { it.copy(selectedFile = null) }
            refreshExplorerFiles(_uiState.value.explorerPath)
        }
    }

    fun saveFileContent(path: String, content: String) {
        viewModelScope.launch {
            val file = repository.getFileByPath(path)
            if (file != null) {
                repository.deleteFile(path)
                repository.createFile(file.parentPath, file.name, content)
                triggerNotification("File Saved", "Changes saved successfully.")
                refreshExplorerFiles(_uiState.value.explorerPath)
            }
        }
    }

    // --- TERMINAL/COMMAND EXECUTOR ---
    fun executeCommand(commandInput: String) {
        val input = commandInput.trim()
        if (input.isEmpty()) return

        val isKali = _uiState.value.currentOs == OsType.KALI
        val promptSign = if (isKali) "root@kali:~# " else (_uiState.value.terminalCurrentDir + ">")
        
        val newHistory = _uiState.value.terminalHistory.toMutableList()
        newHistory.add(promptSign + input)

        val parts = input.split("\\s+".toRegex())
        val cmd = parts[0].lowercase()
        val args = parts.drop(1)

        viewModelScope.launch {
            if (isKali) {
                // BASH commands
                when (cmd) {
                    "help" -> {
                        newHistory.addAll(listOf(
                            "Available Linux Commands:",
                            "  ls                   - List files in current directory",
                            "  cd [path]            - Change directory",
                            "  cat [file]           - Output file contents",
                            "  mkdir [name]         - Create folder",
                            "  touch [name]         - Create blank file",
                            "  rm [path]            - Delete file or folder",
                            "  ifconfig             - Output network configs",
                            "  uname -a             - Detailed kernel details",
                            "  ping [target]        - Test connection latency",
                            "  whoami               - Print current user",
                            "  sudo apt install [app] - Simulates software deployment center",
                            "  nmap [target]        - Network port scanning vulnerability audit",
                            "  hydra [target]       - Brute-force credentials simulation",
                            "  msfconsole           - Launch Metasploit payload console framework",
                            "  clear                - Clear display"
                        ))
                    }
                    "clear" -> {
                        newHistory.clear()
                    }
                    "ls" -> {
                        val files = repository.getFilesByParent(_uiState.value.terminalCurrentDir)
                        if (files.isEmpty()) {
                            newHistory.add("Total 0 files")
                        } else {
                            files.forEach {
                                val d = if (it.isDirectory) "d---" else "f---"
                                newHistory.add("$d  ${it.name}")
                            }
                        }
                    }
                    "cd" -> {
                        if (args.isEmpty()) {
                            _uiState.update { it.copy(terminalCurrentDir = "/root") }
                        } else {
                            val target = args[0]
                            val newPath = when {
                                target == ".." -> {
                                    val current = _uiState.value.terminalCurrentDir
                                    if (current == "/") "/"
                                    else {
                                        val idx = current.lastIndexOf('/')
                                        if (idx == 0) "/" else current.substring(0, idx)
                                    }
                                }
                                target.startsWith("/") -> target
                                else -> {
                                    val current = _uiState.value.terminalCurrentDir
                                    if (current == "/") "/$target" else "$current/$target"
                                }
                            }
                            val dirFile = repository.getFileByPath(newPath)
                            if (dirFile != null && dirFile.isDirectory) {
                                _uiState.update { it.copy(terminalCurrentDir = newPath) }
                            } else {
                                newHistory.add("cd: no such file or directory: $target")
                            }
                        }
                    }
                    "cat" -> {
                        if (args.isEmpty()) {
                            newHistory.add("Usage: cat [filename]")
                        } else {
                            val target = args[0]
                            val path = if (target.startsWith("/")) target else {
                                val cur = _uiState.value.terminalCurrentDir
                                if (cur == "/") "/$target" else "$cur/$target"
                            }
                            val file = repository.getFileByPath(path)
                            if (file != null && !file.isDirectory) {
                                newHistory.addAll(file.content.split("\n"))
                            } else {
                                newHistory.add("cat: $target: No such file or is directory")
                            }
                        }
                    }
                    "mkdir" -> {
                        if (args.isEmpty()) {
                            newHistory.add("mkdir: missing operand")
                        } else {
                            val name = args[0]
                            val success = repository.createDirectory(_uiState.value.terminalCurrentDir, name)
                            if (success) {
                                newHistory.add("Folder '$name' successfully added.")
                                refreshExplorerFiles(_uiState.value.explorerPath)
                            } else {
                                newHistory.add("mkdir: cannot create directory '$name': File exists")
                            }
                        }
                    }
                    "touch" -> {
                        if (args.isEmpty()) {
                            newHistory.add("touch: missing file operand")
                        } else {
                            val name = args[0]
                            val success = repository.createFile(_uiState.value.terminalCurrentDir, name, "")
                            if (success) {
                                newHistory.add("File '$name' successfully created.")
                                refreshExplorerFiles(_uiState.value.explorerPath)
                            } else {
                                newHistory.add("touch: File exists")
                            }
                        }
                    }
                    "rm" -> {
                        if (args.isEmpty()) {
                            newHistory.add("rm: missing operand")
                        } else {
                            val target = args[0]
                            val path = if (target.startsWith("/")) target else {
                                val cur = _uiState.value.terminalCurrentDir
                                if (cur == "/") "/$target" else "$cur/$target"
                            }
                            val file = repository.getFileByPath(path)
                            if (file != null) {
                                repository.deleteFile(path)
                                newHistory.add("Removed '$target' permanently.")
                                refreshExplorerFiles(_uiState.value.explorerPath)
                            } else {
                                newHistory.add("rm: cannot remove '$target': No such file or directory")
                            }
                        }
                    }
                    "whoami" -> {
                        newHistory.add("root")
                    }
                    "uname" -> {
                        newHistory.add("Linux kali-vm 6.6.1-kali-amd64 #1 SMP PREEMPT_DYNAMIC Debian 6.6.1-1kali1 (2026-03-12) x86_64 GNU/Linux")
                    }
                    "ifconfig" -> {
                        newHistory.addAll(listOf(
                            "eth0: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1500",
                            "        inet 10.0.2.15  netmask 255.255.255.0  broadcast 10.0.2.255",
                            "        inet6 fe80::a00:27ff:fe8e:e915  prefixlen 64  scopeid 0x20<link>",
                            "        ether 08:00:27:8e:e9:15  txqueuelen 1000  (Ethernet)",
                            "        RX packets 45312  bytes 31405912 (29.9 MiB)",
                            "        TX packets 21490  bytes 1824905  (1.7 MiB)",
                            "lo: flags=73<UP,LOOPBACK,RUNNING>  mtu 65536",
                            "        inet 127.0.0.1  netmask 255.0.0.0",
                            "        inet6 ::1  prefixlen 128  scopeid 0x10<host>"
                        ))
                    }
                    "ping" -> {
                        if (args.isEmpty()) {
                            newHistory.add("Usage: ping [ip / domain]")
                        } else {
                            val target = args[0]
                            newHistory.addAll(listOf(
                                "PING $target (56 data bytes)...",
                                "64 bytes from $target: icmp_seq=1 ttl=64 time=14.3 ms",
                                "64 bytes from $target: icmp_seq=2 ttl=64 time=11.1 ms",
                                "64 bytes from $target: icmp_seq=3 ttl=64 time=15.2 ms",
                                "--- $target ping statistics ---",
                                "3 packets transmitted, 3 received, 0% packet loss, time 2003ms",
                                "rtt min/avg/max/mdev = 11.1/13.5/15.2/1.76 ms"
                            ))
                        }
                    }
                    "sudo" -> {
                        if (args.isEmpty()) {
                            newHistory.add("sudo: target required")
                        } else if (args[0] == "apt" && args.size >= 3 && args[1] == "install") {
                            val pkg = args[2]
                            newHistory.add("Reading package lists... Done")
                            newHistory.add("Building dependency tree... Done")
                            newHistory.add("Simulating download of $pkg x86_64 binaries...")
                            delay(500)
                            installSimulatedApp(pkg)
                            newHistory.add("Unpacking $pkg-amd64.deb...")
                            newHistory.add("Setting up $pkg ... Done.")
                        } else {
                            // forward remaining command
                            val cleanCmd = args.joinToString(" ")
                            newHistory.add("Executing as root: $cleanCmd")
                        }
                    }
                    "nmap" -> {
                        val target = if (args.isEmpty()) "127.0.0.1" else args[0]
                        newHistory.addAll(listOf(
                            "Starting Nmap 7.94 ( https://nmap.org ) at 2026-07-04 01:54 UTC",
                            "Nmap scan report for $target",
                            "Host is up (0.0020s latency).",
                            "Not shown: 997 closed tcp ports (reset)",
                            "PORT     STATE SERVICE",
                            "22/tcp   open  ssh",
                            "80/tcp   open  http",
                            "443/tcp  open  https",
                            "MAC Address: 08:00:27:8E:E9:15 (Oracle VirtualBox NIC)",
                            "Nmap done: 1 IP address (1 host up) scanned in 1.45 seconds"
                        ))
                    }
                    "hydra" -> {
                        val target = if (args.isEmpty()) "192.168.1.1" else args[0]
                        newHistory.addAll(listOf(
                            "Hydra v9.5 (c) 2026 by van Hauser/THC",
                            "Hydra active on target $target - service ssh (port 22) - 16 threads",
                            "[DATA] attacking login root with wordlist (50 passwords)",
                            "[22][ssh] host: $target   login: root   password: password123",
                            "1 of 1 target successfully completed, 1 valid password found."
                        ))
                    }
                    "msfconsole" -> {
                        newHistory.addAll(listOf(
                            "      .:okOOOo:.",
                            "    .:oOOo.       ...      ..",
                            "   .Ooo.         .oOOo.  .oOOo.",
                            "  .Ooo.          Oooo   Oooo",
                            "  .Ooo.          Oooo   Oooo",
                            "   .oOOo:        .oOOo:  .oOOo:",
                            "     .:okOOOo:.    ..      ..",
                            "Metasploit Framework v6.3.5-dev-",
                            "msf6 > use exploit/multi/handler",
                            "msf6 exploit(multi/handler) > set PAYLOAD windows/meterpreter/reverse_tcp",
                            "PAYLOAD => windows/meterpreter/reverse_tcp",
                            "msf6 exploit(multi/handler) > exploit",
                            "[*] Started reverse TCP handler on 10.0.2.15:4444",
                            "[*] Command shell session 1 opened (10.0.2.15:4444 -> 10.0.2.2:58301)",
                            "meterpreter > whoami",
                            "NT AUTHORITY\\SYSTEM",
                            "meterpreter > exit",
                            "msf6 exploit(multi/handler) > exit"
                        ))
                    }
                    else -> {
                        newHistory.add("bash: $cmd: command not found")
                    }
                }
            } else {
                // WINDOWS cmd commands
                when (cmd) {
                    "help" -> {
                        newHistory.addAll(listOf(
                            "Microsoft Windows cmd.exe help menu:",
                            "  DIR               - Display folder files",
                            "  CD [path]         - Change workspace directory",
                            "  MKDIR [name]      - Create directory",
                            "  TYPE [file]       - Read file text contents",
                            "  DEL [path]        - Delete file",
                            "  IPCONFIG          - Display network interface parameters",
                            "  SYSTEMINFO        - Display virtual hardware specs",
                            "  SETX [key] [val]  - Write persistent environment variable",
                            "  SET               - Display environment variables list",
                            "  TASKLIST          - List running virtual threads",
                            "  NETSTAT           - Display network socket listings",
                            "  PING [target]     - Execute latency evaluation packets",
                            "  SFC /SCANNOW      - Scan and fix virtual system directory",
                            "  HOSTNAME          - Print host name",
                            "  WINGET INSTALL [app] - Installs program from cloud binaries catalog",
                            "  CLEAR / CLS       - Clears terminal screen"
                        ))
                    }
                    "cls", "clear" -> {
                        newHistory.clear()
                    }
                    "dir" -> {
                        val files = repository.getFilesByParent(_uiState.value.terminalCurrentDir)
                        if (files.isEmpty()) {
                            newHistory.add(" Directory of " + _uiState.value.terminalCurrentDir)
                            newHistory.add("No files found.")
                        } else {
                            newHistory.add(" Directory of " + _uiState.value.terminalCurrentDir)
                            newHistory.add("")
                            files.forEach {
                                val d = if (it.isDirectory) "<DIR>" else "     "
                                newHistory.add("07/04/2026  12:00 PM    $d         ${it.name}")
                            }
                            newHistory.add("               ${files.size} File(s)")
                        }
                    }
                    "cd" -> {
                        if (args.isEmpty()) {
                            newHistory.add(_uiState.value.terminalCurrentDir)
                        } else {
                            val target = args[0]
                            val cur = _uiState.value.terminalCurrentDir
                            val newPath = when {
                                target == ".." -> {
                                    if (cur == "C:" || cur == "D:" || cur == "E:") cur
                                    else {
                                        val idx = cur.lastIndexOf('\\')
                                        if (idx != -1) cur.substring(0, idx) else cur
                                    }
                                }
                                target.contains(":") -> target
                                else -> {
                                    if (cur == "C:" || cur == "D:" || cur == "E:") "$cur\\$target" else "$cur\\$target"
                                }
                            }
                            val dirFile = repository.getFileByPath(newPath)
                            if (dirFile != null && dirFile.isDirectory) {
                                _uiState.update { it.copy(terminalCurrentDir = newPath) }
                            } else {
                                newHistory.add("The system cannot find the path specified: $target")
                            }
                        }
                    }
                    "type" -> {
                        if (args.isEmpty()) {
                            newHistory.add("The syntax of the command is incorrect.")
                        } else {
                            val target = args[0]
                            val path = if (target.contains(":")) target else "${_uiState.value.terminalCurrentDir}\\$target"
                            val file = repository.getFileByPath(path)
                            if (file != null && !file.isDirectory) {
                                newHistory.addAll(file.content.split("\n"))
                            } else {
                                newHistory.add("The system cannot find the file specified.")
                            }
                        }
                    }
                    "mkdir" -> {
                        if (args.isEmpty()) {
                            newHistory.add("The syntax of the command is incorrect.")
                        } else {
                            val name = args[0]
                            val success = repository.createDirectory(_uiState.value.terminalCurrentDir, name)
                            if (success) {
                                newHistory.add("Directory created successfully.")
                                refreshExplorerFiles(_uiState.value.explorerPath)
                            } else {
                                newHistory.add("A subdirectory or file '$name' already exists.")
                            }
                        }
                    }
                    "del" -> {
                        if (args.isEmpty()) {
                            newHistory.add("The syntax of the command is incorrect.")
                        } else {
                            val target = args[0]
                            val path = if (target.contains(":")) target else "${_uiState.value.terminalCurrentDir}\\$target"
                            val file = repository.getFileByPath(path)
                            if (file != null) {
                                repository.deleteFile(path)
                                newHistory.add("File deleted.")
                                refreshExplorerFiles(_uiState.value.explorerPath)
                            } else {
                                newHistory.add("Could not find $target")
                            }
                        }
                    }
                    "ipconfig" -> {
                        newHistory.addAll(listOf(
                            "Windows IP Configuration",
                            "",
                            "Ethernet adapter Ethernet0:",
                            "   Connection-specific DNS Suffix  . : localdomain",
                            "   IPv4 Address. . . . . . . . . . . : 192.168.1.100",
                            "   Subnet Mask . . . . . . . . . . . : 255.255.255.0",
                            "   Default Gateway . . . . . . . . . : 192.168.1.1",
                            "",
                            "Wireless LAN adapter Wi-Fi:",
                            "   Media State . . . . . . . . . . . : Connected",
                            "   IPv4 Address. . . . . . . . . . . : 10.0.2.15",
                            "   Default Gateway . . . . . . . . . : 10.0.2.2"
                        ))
                    }
                    "systeminfo" -> {
                        newHistory.addAll(listOf(
                            "Host Name:                 WIN-CONTAINER-X",
                            "OS Name:                   Microsoft Windows 11 Pro",
                            "OS Version:                10.0.22631 N/A Build 22631",
                            "OS Manufacturer:           Microsoft Corporation",
                            "OS Configuration:          Standalone Workstation",
                            "Product ID:                00330-80000-00000-AA144",
                            "System Manufacturer:       Android Container Emulation Layer",
                            "System Model:              Box64 Wine ARM64-to-x86_64 Machine",
                            "Processor(s):              1 Processor(s) Installed. Core Count: " + _uiState.value.cpuCores + " (Vulkan Turnip + Zink Active)",
                            "Total Physical Memory:     " + (_uiState.value.ramSizeGb * 1024) + " MB",
                            "Graphics Translation:      DXVK v2.3 API Active"
                        ))
                    }
                    "set" -> {
                        _uiState.value.envVars.forEach {
                            newHistory.add("${it.key}=${it.value}")
                        }
                    }
                    "setx" -> {
                        if (args.size < 2) {
                            newHistory.add("Usage: SETX [variable] [value]")
                        } else {
                            val key = args[0]
                            val value = args.drop(1).joinToString(" ")
                            repository.saveEnvVar(key, value)
                            newHistory.add("SUCCESS: Specified value was saved.")
                            triggerNotification("Environment Variable Modified", "Set '$key' system path variable successfully.")
                        }
                    }
                    "tasklist" -> {
                        newHistory.addAll(listOf(
                            "Image Name                     PID Session Name        Session#    Mem Usage",
                            "========================= ======== ================ =========== ============",
                            "System Idle Process              0 Services                   0          8 K",
                            "System                           4 Services                   0        124 K",
                            "smss.exe                       320 Services                   0        412 K",
                            "csrss.exe                      416 Services                   0      1,212 K",
                            "wininit.exe                    488 Services                   0      2,114 K",
                            "services.exe                   544 Services                   0      7,480 K",
                            "lsass.exe                      560 Services                   0     11,210 K",
                            "explorer.exe                  1240 Console                    1     64,812 K",
                            "chrome.exe                    1840 Console                    1    142,391 K",
                            "cmd.exe                       2190 Console                    1      4,180 K"
                        ))
                    }
                    "netstat" -> {
                        newHistory.addAll(listOf(
                            "Active Connections",
                            "",
                            "  Proto  Local Address          Foreign Address        State",
                            "  TCP    127.0.0.1:49673        127.0.0.1:49674        ESTABLISHED",
                            "  TCP    192.168.1.100:139      0.0.0.0:0              LISTENING",
                            "  TCP    10.0.2.15:58914        142.250.72.110:443     ESTABLISHED"
                        ))
                    }
                    "ping" -> {
                        if (args.isEmpty()) {
                            newHistory.add("Usage: ping [target_host]")
                        } else {
                            val target = args[0]
                            newHistory.addAll(listOf(
                                "Pinging $target [142.250.72.110] with 32 bytes of data:",
                                "Reply from 142.250.72.110: bytes=32 time=12ms TTL=118",
                                "Reply from 142.250.72.110: bytes=32 time=15ms TTL=118",
                                "Reply from 142.250.72.110: bytes=32 time=11ms TTL=118",
                                "",
                                "Ping statistics for $target:",
                                "    Packets: Sent = 3, Received = 3, Lost = 0 (0% loss),",
                                "Approximate round trip times in milli-seconds:",
                                "    Minimum = 11ms, Maximum = 15ms, Average = 12ms"
                            ))
                        }
                    }
                    "sfc" -> {
                        if (args.size >= 2 && args[0].lowercase() == "/scannow") {
                            newHistory.add("Beginning system scan. This process will take some time.")
                            newHistory.add("Beginning verification phase of system scan.")
                            newHistory.add("Verification 100% complete.")
                            newHistory.add("Windows Resource Protection did not find any integrity violations.")
                            triggerNotification("SFC Scannow Complete", "Virtual system file verification verified 100% healthy.")
                        } else {
                            newHistory.add("Usage: SFC /SCANNOW")
                        }
                    }
                    "hostname" -> {
                        newHistory.add("WIN-CONTAINER-X")
                    }
                    "winget" -> {
                        if (args.size >= 3 && args[0].lowercase() == "install") {
                            val pkg = args[2]
                            newHistory.add("Found package: $pkg")
                            newHistory.add("Simulating secure binary download of $pkg x86_64 installer...")
                            delay(400)
                            installSimulatedApp(pkg)
                            newHistory.add("Installing package... Done.")
                        } else {
                            newHistory.add("Usage: WINGET INSTALL [app_name]")
                        }
                    }
                    else -> {
                        newHistory.add("'$cmd' is not recognized as an internal or external command,")
                        newHistory.add("operable program or batch file.")
                    }
                }
            }
            
            _uiState.update { it.copy(terminalHistory = newHistory) }
        }
    }

    // --- APPLICATION DOWNLOAD & EXECUTION SECURITY ---
    fun installSimulatedApp(appId: String) {
        val app = _uiState.value.availableApps.find { it.id == appId } ?: return
        
        // Quality check: Block standard android APKs
        if (appId.lowercase().endsWith(".apk")) {
            triggerNotification(
                "APK Blocked", 
                "Standard Android APK installation is blocked! This system emulates high performance AMD64 desktop environments (.exe/.msi/binaries)."
            )
            return
        }

        val updated = _uiState.value.availableApps.map {
            if (it.id == appId) it.copy(isInstalled = true) else it
        }
        
        _uiState.update { it.copy(availableApps = updated) }
        triggerNotification(
            "Software Installed",
            "${app.name} is successfully configured. You can now access it via Start Menu or Command Console."
        )
    }

    fun handleApkInstallError() {
        triggerNotification(
            "APK Refused",
            "Blocked! Standard Android APK files are not runnable here. Download and run native PC files (.exe / .msi) instead."
        )
    }

    // --- APP REGISTRY: EXE / MSI BINARY VALIDATOR & APK REJECTOR ---
    fun executeVirtualBinary(file: VirtualFile) {
        if (file.isDirectory) return
        val nameLower = file.name.lowercase()
        if (nameLower.endsWith(".apk")) {
            triggerNotification(
                "App Registry Error (Code: 0xC000007B)",
                "EXECUTION BLOCKED: '${file.name}' is an Android Package (.apk). Standard Dalvik bytecode cannot run on x86_64 virtualization core. Use portable executable (.exe) or installer (.msi) targets."
            )
        } else if (nameLower.endsWith(".exe") || nameLower.endsWith(".msi")) {
            triggerNotification(
                "App Registry Verification",
                "Verifying file headers & AMD64 compatibility signature for '${file.name}'..."
            )
            viewModelScope.launch {
                delay(800)
                val baseName = file.name.substringBefore(".").lowercase()
                val targetAppId = when {
                    baseName.contains("chrome") -> "chrome"
                    baseName.contains("steam") -> "steam" // If we want to support steam
                    else -> null
                }
                
                val appToInstall = _uiState.value.availableApps.find { it.id == targetAppId }
                if (appToInstall != null) {
                    if (!appToInstall.isInstalled) {
                        installSimulatedApp(appToInstall.id)
                    } else {
                        triggerNotification(
                            "App Registry Engine",
                            "'${file.name}' successfully loaded through Wine/Box64 translation layers."
                        )
                        openWindow(when (appToInstall.id) {
                            "chrome" -> WindowType.CHROME
                            else -> WindowType.NOTEPAD
                        })
                    }
                } else {
                    triggerNotification(
                        "App Registry Sandbox",
                        "Executing '${file.name}' inside Wine prefix. Process started successfully (PID: ${(1000..9999).random()})."
                    )
                }
            }
        } else {
            selectFile(file)
            openWindow(WindowType.NOTEPAD)
        }
    }

    // --- PERIPHERALS SCANNER & PRINTER SIMULATOR ---
    fun runVirtualScanner() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanProgress = 0f, scannedDocResult = null) }
            for (i in 1..10) {
                delay(200)
                _uiState.update { it.copy(scanProgress = i * 0.1f) }
            }
            _uiState.update { 
                it.copy(
                    isScanning = false, 
                    scannedDocResult = "SCAN COMPLETED [VIRTUAL DOCUMENT ID: SC-92811]\n\nType: USB Scanner High Definition Feed\nDimensions: Letter 8.5 x 11 in\nText Found:\n'VIRTUAL INTEGRATED HARDWARE MAP: SYSTEM CONNECTED TO ANDROID KERNEL.'"
                ) 
            }
            triggerNotification("Scanner Complete", "Document scanned successfully! Displaying preview output.")
        }
    }

    fun runVirtualPrinter(documentName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPrinting = true, printProgress = 0f) }
            for (i in 1..10) {
                delay(250)
                _uiState.update { it.copy(printProgress = i * 0.1f) }
            }
            _uiState.update { it.copy(isPrinting = false) }
            triggerNotification("Printer Task Finished", "Successfully printed document '$documentName' to virtual system PDF printer.")
        }
    }

    // --- KEYBOARD & MOUSE INTEGRATIONS ---
    fun toggleVirtualKeyboard() {
        _uiState.update { it.copy(isKeyboardOpen = !it.isKeyboardOpen) }
    }

    fun toggleVirtualMouse() {
        _uiState.update { it.copy(isVirtualMouseActive = !it.isVirtualMouseActive) }
    }

    fun moveVirtualMouse(dx: Float, dy: Float) {
        _uiState.update { state ->
            val newX = (state.mouseX + dx).coerceIn(0f, 1280f)
            val newY = (state.mouseY + dy).coerceIn(0f, 720f)
            state.copy(mouseX = newX, mouseY = newY)
        }
    }
}
