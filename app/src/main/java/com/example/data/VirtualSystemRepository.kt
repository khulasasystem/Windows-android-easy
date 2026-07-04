package com.example.data

import kotlinx.coroutines.flow.Flow

class VirtualSystemRepository(
    private val virtualFileDao: VirtualFileDao,
    private val envVarDao: EnvVarDao
) {
    val allFilesFlow: Flow<List<VirtualFile>> = virtualFileDao.getAllFilesFlow()
    val allEnvVarsFlow: Flow<List<EnvVar>> = envVarDao.getAllEnvVarsFlow()

    suspend fun initializeIfEmpty() {
        if (virtualFileDao.getFileCount() == 0) {
            val defaultFiles = mutableListOf<VirtualFile>()

            // --- WINDOWS C: DRIVE ---
            defaultFiles.addAll(listOf(
                VirtualFile("C:", "C:", true, parentPath = ""),
                VirtualFile("C:\\Windows", "Windows", true, parentPath = "C:"),
                VirtualFile("C:\\Windows\\System32", "System32", true, parentPath = "C:\\Windows"),
                VirtualFile("C:\\Windows\\System32\\cmd.exe", "cmd.exe", false, "", "C:\\Windows\\System32"),
                VirtualFile("C:\\Windows\\System32\\explorer.exe", "explorer.exe", false, "", "C:\\Windows\\System32"),
                VirtualFile("C:\\Program Files", "Program Files", true, parentPath = "C:"),
                VirtualFile("C:\\Program Files\\Google", "Google", true, parentPath = "C:\\Program Files"),
                VirtualFile("C:\\Program Files\\Google\\Chrome", "Chrome", true, parentPath = "C:\\Program Files\\Google"),
                VirtualFile("C:\\Program Files\\Google\\Chrome\\chrome.exe", "chrome.exe", false, "", "C:\\Program Files\\Google\\Chrome"),
                VirtualFile("C:\\Users", "Users", true, parentPath = "C:"),
                VirtualFile("C:\\Users\\Administrator", "Administrator", true, parentPath = "C:\\Users"),
                VirtualFile("C:\\Users\\Administrator\\Desktop", "Desktop", true, parentPath = "C:\\Users\\Administrator"),
                VirtualFile("C:\\Users\\Administrator\\Documents", "Documents", true, parentPath = "C:\\Users\\Administrator"),
                VirtualFile("C:\\Users\\Administrator\\Downloads", "Downloads", true, parentPath = "C:\\Users\\Administrator"),
                VirtualFile(
                    "C:\\Users\\Administrator\\Desktop\\README.txt",
                    "README.txt",
                    false,
                    """==================================================
WINDOWS 11 PRO (AMD64 / x86_64) VIRTUAL ENVIRONMENT
==================================================
Host System: Containerized Android Layer (Termux/Winlator)
Architecture: x86_64 Translation Layer (Box64 + Wine)
Status: OPTIMIZED (UWP Bloatware Stripped)

Features Enabled:
- DXVK (DirectX to Vulkan Translator)
- Wine-Esync / Fsync (High Performance Mode)
- Turnip + Zink Vulkan Graphics Drivers
- Custom desktop user-agent for Google Chrome

Permanent System Tools:
1. Command Prompt (cmd.exe) / PowerShell
2. File Explorer (explorer.exe)
3. Settings & Control Panel

Feel free to create files, navigate folders, run cmd
commands, or browse the web!
==================================================""".trimIndent(),
                    "C:\\Users\\Administrator\\Desktop"
                ),
                VirtualFile(
                    "C:\\Users\\Administrator\\Documents\\notes.txt",
                    "notes.txt",
                    false,
                    "Virtual system note:\nRun 'help' inside cmd.exe to see all supported commands!\nEnjoy this high performance environment.",
                    "C:\\Users\\Administrator\\Documents"
                )
            ))

            // --- EXTERNAL D: AND E: DRIVES (USB & SD Card simulation) ---
            defaultFiles.addAll(listOf(
                VirtualFile("D:", "D: (USB Storage)", true, parentPath = ""),
                VirtualFile("D:\\Android_Shared", "Android_Shared", true, parentPath = "D:"),
                VirtualFile("D:\\Android_Shared\\MyPhotos", "MyPhotos", true, parentPath = "D:\\Android_Shared"),
                VirtualFile("D:\\Android_Shared\\MyPhotos\\vacation.txt", "vacation.txt", false, "Trip to Hawaii 2026. Captured beautiful views of volcanos and beaches.", "D:\\Android_Shared\\MyPhotos"),
                VirtualFile("E:", "E: (SD Card Backup)", true, parentPath = ""),
                VirtualFile("E:\\Backups", "Backups", true, parentPath = "E:"),
                VirtualFile("E:\\Backups\\system_config.cfg", "system_config.cfg", false, "cores=8\nram=16384\ndxvk=true\nesync=true\ndriver=turnip_zink", "E:\\Backups")
            ))

            // --- KALI LINUX FILE SYSTEM ---
            defaultFiles.addAll(listOf(
                VirtualFile("/", "rootfs", true, parentPath = ""),
                VirtualFile("/root", "root", true, parentPath = "/"),
                VirtualFile("/root/Desktop", "Desktop", true, parentPath = "/root"),
                VirtualFile("/root/Documents", "Documents", true, parentPath = "/root"),
                VirtualFile("/root/Downloads", "Downloads", true, parentPath = "/root"),
                VirtualFile("/etc", "etc", true, parentPath = "/"),
                VirtualFile("/etc/shadow", "shadow", false, "root:\$6\$pX9YdJ2\$kHlqS782sAdG:19245:0:99999:7:::\nadmin:\$6\$mQ42X91\$hJlQ9102sLdP:19245:0:99999:7:::", "/etc"),
                VirtualFile("/etc/hosts", "hosts", false, "127.0.0.1 localhost\n192.168.1.1 gateway.local\n10.0.2.15 kali-vm", "/etc"),
                VirtualFile("/root/Desktop/README_Kali.txt", "README_Kali.txt", false,
                    """==================================================
KALI LINUX PENETRATION TESTING SUITE (x86_64)
==================================================
Welcome to the ultimate security analysis container.
All core network scanners and decryption tools are active.

Tools Available in Bash:
- nmap [host]               - Network mapping utility
- hydra [host]              - Brute-force credentials simulation
- msfconsole                - Start Metasploit penetration framework
- uname -a                  - Output kernel structure
- ifconfig                  - Check simulated network interfaces
- apt install [app]         - Simulate package installation

Enjoy the fully working Bash console!
==================================================""".trimIndent(),
                    "/root/Desktop"
                ),
                VirtualFile("/root/Documents/targets.lst", "targets.lst", false, "192.168.1.1\n192.168.1.50 (Corporate Printer)\n192.168.1.144 (Local Server)\n10.0.2.2 (Default Gateway)", "/root/Documents")
            ))

            virtualFileDao.insertFiles(defaultFiles)
        }

        if (envVarDao.getCount() == 0) {
            val defaultVars = listOf(
                EnvVar("OS", "Windows_NT", true),
                EnvVar("PROCESSOR_ARCHITECTURE", "AMD64", true),
                EnvVar("NUMBER_OF_PROCESSORS", "8", true),
                EnvVar("PATH", "C:\\Windows\\System32;C:\\Program Files\\Google\\Chrome", true),
                EnvVar("USERPROFILE", "C:\\Users\\Administrator", true),
                EnvVar("USERNAME", "Administrator", true),
                EnvVar("COMPUTERNAME", "WIN-CONTAINER-X", true),
                EnvVar("SYSTEMROOT", "C:\\Windows", true),
                EnvVar("COMSPEC", "C:\\Windows\\System32\\cmd.exe", true)
            )
            envVarDao.insertEnvVars(defaultVars)
        }
    }

    suspend fun getFilesByParent(parentPath: String): List<VirtualFile> =
        virtualFileDao.getFilesByParent(parentPath)

    suspend fun getFileByPath(path: String): VirtualFile? =
        virtualFileDao.getFileByPath(path)

    suspend fun createDirectory(parentPath: String, name: String): Boolean {
        val path = when {
            parentPath == "C:" -> "C:\\$name"
            parentPath == "D:" -> "D:\\$name"
            parentPath == "E:" -> "E:\\$name"
            parentPath == "/" -> "/$name"
            parentPath.startsWith("/") -> if (parentPath == "/") "/$name" else "$parentPath/$name"
            else -> "$parentPath\\$name"
        }
        if (virtualFileDao.getFileByPath(path) != null) return false
        virtualFileDao.insertFile(VirtualFile(path, name, true, "", parentPath))
        return true
    }

    suspend fun createFile(parentPath: String, name: String, content: String = ""): Boolean {
        val path = when {
            parentPath == "C:" -> "C:\\$name"
            parentPath == "D:" -> "D:\\$name"
            parentPath == "E:" -> "E:\\$name"
            parentPath == "/" -> "/$name"
            parentPath.startsWith("/") -> if (parentPath == "/") "/$name" else "$parentPath/$name"
            else -> "$parentPath\\$name"
        }
        if (virtualFileDao.getFileByPath(path) != null) return false
        virtualFileDao.insertFile(VirtualFile(path, name, false, content, parentPath))
        return true
    }

    suspend fun deleteFile(path: String) {
        val file = virtualFileDao.getFileByPath(path) ?: return
        if (file.isDirectory) {
            virtualFileDao.deleteFilesByPrefix(path)
            virtualFileDao.deleteFileByPath(path)
        } else {
            virtualFileDao.deleteFileByPath(path)
        }
    }

    suspend fun saveEnvVar(key: String, value: String) {
        envVarDao.insertEnvVar(EnvVar(key, value, false))
    }

    suspend fun deleteEnvVar(key: String) {
        envVarDao.deleteEnvVar(key)
    }

    suspend fun getAllEnvVars(): List<EnvVar> = envVarDao.getAllEnvVars()
}
