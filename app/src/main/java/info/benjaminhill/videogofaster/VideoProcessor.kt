package info.benjaminhill.videogofaster

import com.antonkarpenko.ffmpegkit.FFmpegKit
import com.antonkarpenko.ffmpegkit.FFmpegSession

/**
 * Responsibility: Handles video transcoding logic and command construction.
 * Separation of Concerns: This object encapsulates all FFmpeg-specific knowledge,
 * preventing encoding details (flags, codecs, filters) from leaking into the ViewModel.
 */
object VideoProcessor {

    /**
     * Executes the video processing command using FFmpeg.
     *
     * @param blendFactor The speed multiplier (2, 4, or 8).
     * @return The FFmpeg session result.
     */
    suspend fun processVideo(
        inputPath: String,
        outputPath: String,
        blendFactor: Int
    ): FFmpegSession {
        val cmd = generateCommand(inputPath, outputPath, blendFactor)
        // FFmpegKit.execute() is blocking, but intended here for simplicity in this architecture.
        // The ViewModel runs this on Dispatchers.IO.
        return FFmpegKit.execute(cmd)
    }

    /**
     * Generates the FFmpeg command string.
     * Pure function, easily testable.
     *
     * @param blendFactor: The speed multiplier (2, 4, or 8).
     * This determines how many frames are blended and the decimation rate.
     */
    fun generateCommand(
        inputPath: String,
        outputPath: String,
        blendFactor: Int
    ): String {
        // 1. Calculate PTS modifier (e.g., 0.5 for 2x speed)
        val ptsModifier = 1.0 / blendFactor

        // 2. Build the Filter String
        // - format=yuv420p10le: Promotes 8-bit inputs to 10-bit immediately.
        // - tmix=frames=N: Blends N frames.
        // - select='not(mod(n,N))': Keeps only the blended frames (effectively dropping intermediates).
        // - setpts=M*PTS: Speeds up playback to match frame reduction.
        // Escaping comma in select filter as it is inside a filter chain
        val filterChain =
            "format=pix_fmts=yuv420p10le,tmix=frames=$blendFactor,select='not(mod(n\\,$blendFactor))',setpts=$ptsModifier*PTS"

        // 3. Construct Command
        // -c:v libx265: Use software HEVC encoder for consistent 10-bit support.
        // -crf 18: Near-lossless quality (improved from 20).
        // -preset slow: Better compression/quality tradeoff (improved from medium).
        // -an: Remove audio.
        return "-y -i \"$inputPath\" -vf \"$filterChain\" -c:v libx265 -crf 18 -preset slow -pix_fmt yuv420p10le -an \"$outputPath\""
    }
}
