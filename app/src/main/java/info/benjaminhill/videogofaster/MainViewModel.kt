package info.benjaminhill.videogofaster

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class UiState(
    val isLoading: Boolean = false,
    val currentStep: String = "Idle",
    val outputLogs: List<String> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun startProcessing(sourceUri: Uri) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, currentStep = "Preparing...", outputLogs = emptyList()) }

            val context = getApplication<Application>().applicationContext

            // 1. Copy to Cache
            val originalFile = Utils.copyUriToCache(context, sourceUri)
            if (originalFile == null) {
                _uiState.update { it.copy(isLoading = false, currentStep = "Error: Could not access video file.") }
                return@launch
            }

            val originalName = Utils.getFileName(context, sourceUri)
            val speeds = listOf(2, 4, 8)

            try {
                for (speed in speeds) {
                    _uiState.update { it.copy(currentStep = "Blending ${speed}x (High Quality)...") }

                    val outputFile = File(context.cacheDir, "temp_${speed}x.mp4")

                    // Process
                    val session = VideoProcessor.processVideo(
                        inputPath = originalFile.absolutePath,
                        outputPath = outputFile.absolutePath,
                        blendFactor = speed
                    )

                    if (ReturnCode.isSuccess(session.returnCode)) {
                        // Save to MediaStore
                        val savedName = Utils.saveVideoToMediaStore(context, outputFile, originalName, speed)
                        if (savedName != null) {
                            val msg = "Saved: $savedName"
                            _uiState.update { it.copy(outputLogs = it.outputLogs + msg) }
                        } else {
                            _uiState.update { it.copy(outputLogs = it.outputLogs + "Error saving ${speed}x video.") }
                        }
                    } else {
                        val failStackTrace = session.failStackTrace
                        _uiState.update { it.copy(outputLogs = it.outputLogs + "Error processing ${speed}x: $failStackTrace") }
                    }

                    // Cleanup temp output
                    if (outputFile.exists()) {
                        outputFile.delete()
                    }
                }
                _uiState.update { it.copy(currentStep = "All Completed.", isLoading = false) }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, currentStep = "Error: ${e.message}") }
                e.printStackTrace()
            } finally {
                // Cleanup input copy
                if (originalFile.exists()) {
                    originalFile.delete()
                }
            }
        }
    }
}
