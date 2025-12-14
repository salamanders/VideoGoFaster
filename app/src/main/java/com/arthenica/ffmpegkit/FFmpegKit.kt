package com.arthenica.ffmpegkit

/**
 * Stub implementation of FFmpegKit.
 * Replace with real dependency when available.
 */
object FFmpegKit {
    fun execute(command: String): FFmpegSession {
        // Stub: Just print command and pretend success
        // In a real stub, we might want to simulate delay or write a dummy file if needed,
        // but for now, we just pass.
        // We might want to parse the command to create the output file so the app flow continues?
        // Command: ... -y -i "input" ... "output"
        // We should create "output" as a dummy file.

        val parts = command.split(" ")
        val lastPart = parts.last().replace("\"", "")
        if (lastPart.endsWith(".mp4")) {
            val file = java.io.File(lastPart)
            try {
                if (!file.exists()) {
                    file.createNewFile()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        println("STUB: Executing FFmpeg command: $command")
        return FFmpegSession(ReturnCode(0))
    }
}
