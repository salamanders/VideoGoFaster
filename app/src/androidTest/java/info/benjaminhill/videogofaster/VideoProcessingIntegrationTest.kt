package info.benjaminhill.videogofaster

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.antonkarpenko.ffmpegkit.ReturnCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class VideoProcessingIntegrationTest {

    private lateinit var context: Context
    private lateinit var inputVideoFile: File
    private lateinit var outputDir: File

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        outputDir = context.cacheDir
        // Ensure we start with a clean slate for the input file
        inputVideoFile = File(outputDir, "input_test_video.mp4")
        copyResourceToTempFile(info.benjaminhill.videogofaster.R.raw.test_video, inputVideoFile)
    }

    @Test
    fun testSpeed2x() = runBlocking {
        val outputFile = File(outputDir, "output_2x.mp4")
        if (outputFile.exists()) outputFile.delete()

        val session = VideoProcessor.processVideo(
            inputPath = inputVideoFile.absolutePath,
            outputPath = outputFile.absolutePath,
            blendFactor = 2
        )

        assertTrue(
            "FFmpeg process failed with logs: ${session.allLogsAsString}",
            ReturnCode.isSuccess(session.returnCode)
        )
        assertTrue("Output file was not created", outputFile.exists())

        verifyDuration(inputVideoFile, outputFile, 2.0)
    }

    @Test
    fun testSpeed4x() = runBlocking {
        val outputFile = File(outputDir, "output_4x.mp4")
        if (outputFile.exists()) outputFile.delete()

        val session = VideoProcessor.processVideo(
            inputPath = inputVideoFile.absolutePath,
            outputPath = outputFile.absolutePath,
            blendFactor = 4
        )

        assertTrue(
            "FFmpeg process failed with logs: ${session.allLogsAsString}",
            ReturnCode.isSuccess(session.returnCode)
        )
        assertTrue("Output file was not created", outputFile.exists())

        verifyDuration(inputVideoFile, outputFile, 4.0)
    }

    @Test
    fun testSpeed8x() = runBlocking {
        val outputFile = File(outputDir, "output_8x.mp4")
        if (outputFile.exists()) outputFile.delete()

        val session = VideoProcessor.processVideo(
            inputPath = inputVideoFile.absolutePath,
            outputPath = outputFile.absolutePath,
            blendFactor = 8
        )

        assertTrue(
            "FFmpeg process failed with logs: ${session.allLogsAsString}",
            ReturnCode.isSuccess(session.returnCode)
        )
        assertTrue("Output file was not created", outputFile.exists())

        verifyDuration(inputVideoFile, outputFile, 8.0)
    }

    private fun verifyDuration(input: File, output: File, speedFactor: Double) {
        val inputDuration = getDuration(input)
        val outputDuration = getDuration(output)
        val expectedDuration = (inputDuration / speedFactor).toLong()

        // Allow for some variance due to frame rounding and container overhead.
        // 500ms is generous but safe for integration tests to ensure logic is directionally correct.
        val delta = 500L

        assertTrue(
            "Duration mismatch. Expected approx $expectedDuration ms (Input: $inputDuration / $speedFactor), got $outputDuration ms",
            abs(expectedDuration - outputDuration) < delta
        )
    }

    private fun getDuration(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            time?.toLong() ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        } finally {
            retriever.release()
        }
    }

    private fun copyResourceToTempFile(resourceId: Int, file: File) {
        val inputStream: InputStream = context.resources.openRawResource(resourceId)
        val outputStream = FileOutputStream(file)
        try {
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
        } finally {
            inputStream.close()
            outputStream.close()
        }
    }
}
