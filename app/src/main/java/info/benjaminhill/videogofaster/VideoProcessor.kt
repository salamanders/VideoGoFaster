package info.benjaminhill.videogofaster

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession

object VideoProcessor {

    /**
     * @param blendFactor: The speed multiplier (2, 4, or 8).
     * This determines how many frames are blended and the decimation rate.
     */
    suspend fun processVideo(
        inputPath: String,
        outputPath: String,
        blendFactor: Int
    ): FFmpegSession {
        // 1. Calculate PTS modifier (e.g., 0.5 for 2x speed)
        val ptsModifier = 1.0 / blendFactor

        // 2. Build the Filter String
        // - format=yuv420p10le: Promotes 8-bit inputs to 10-bit immediately.
        // - tmix=frames=N: Blends N frames.
        // - select='not(mod(n,N))': Keeps only the blended frames (effectively dropping intermediates).
        // - setpts=M*PTS: Speeds up playback to match frame reduction.
        // Escaping comma in select filter as it is inside a filter chain
        val filterChain =
            "format=pix_fmts=yuv420p10le,tmix=frames=$blendFactor:weights=1,select='not(mod(n\\,$blendFactor))',setpts=$ptsModifier*PTS"

        // 3. Construct Command
        // -c:v libx265: Use software HEVC encoder for consistent 10-bit support.
        // -crf 20: High quality variable bitrate.
        // -an: Remove audio.
        val cmd =
            "-y -i \"$inputPath\" -vf \"$filterChain\" -c:v libx265 -crf 20 -preset medium -pix_fmt yuv420p10le -an \"$outputPath\""

        // FFmpegKit.execute is blocking, but we are in a suspend function.
        // However, FFmpegKit has executeAsync. But the spec example uses execute(cmd) which blocks.
        // Since we will call this from Dispatchers.IO in ViewModel, blocking the thread is "okay" but
        // FFmpegKit.execute() is strictly blocking.
        // The spec implies: return FFmpegKit.execute(cmd)

        return FFmpegKit.execute(cmd)
    }
}
