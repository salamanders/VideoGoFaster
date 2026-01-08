package info.benjaminhill.videogofaster

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoProcessorTest {

    @Test
    fun `generateCommand produces correct ffmpeg arguments for 2x speed`() {
        val input = "/path/to/input.mp4"
        val output = "/path/to/output.mp4"
        val factor = 2
        val expectedPtsModifier = 0.5 // 1.0 / 2

        val expectedFilter =
            "format=pix_fmts=yuv420p10le,tmix=frames=2,select='not(mod(n\\,2))',setpts=$expectedPtsModifier*PTS"
        val expectedCmd =
            "-y -i \"$input\" -vf \"$expectedFilter\" -c:v libx265 -crf 18 -preset slow -pix_fmt yuv420p10le -an \"$output\""

        val actualCmd = VideoProcessor.generateCommand(input, output, factor)

        assertEquals(expectedCmd, actualCmd)
    }

    @Test
    fun `generateCommand produces correct ffmpeg arguments for 4x speed`() {
        val input = "in.mp4"
        val output = "out.mp4"
        val factor = 4
        val expectedPtsModifier = 0.25 // 1.0 / 4

        val expectedFilter =
            "format=pix_fmts=yuv420p10le,tmix=frames=4,select='not(mod(n\\,4))',setpts=$expectedPtsModifier*PTS"
        val expectedCmd =
            "-y -i \"$input\" -vf \"$expectedFilter\" -c:v libx265 -crf 18 -preset slow -pix_fmt yuv420p10le -an \"$output\""

        val actualCmd = VideoProcessor.generateCommand(input, output, factor)

        assertEquals(expectedCmd, actualCmd)
    }

    @Test
    fun `generateCommand produces correct ffmpeg arguments for 8x speed`() {
        val input = "video.MOV"
        val output = "result.mp4"
        val factor = 8
        val expectedPtsModifier = 0.125 // 1.0 / 8

        val expectedFilter =
            "format=pix_fmts=yuv420p10le,tmix=frames=8,select='not(mod(n\\,8))',setpts=$expectedPtsModifier*PTS"
        val expectedCmd =
            "-y -i \"$input\" -vf \"$expectedFilter\" -c:v libx265 -crf 18 -preset slow -pix_fmt yuv420p10le -an \"$output\""

        val actualCmd = VideoProcessor.generateCommand(input, output, factor)

        assertEquals(expectedCmd, actualCmd)
    }
}
