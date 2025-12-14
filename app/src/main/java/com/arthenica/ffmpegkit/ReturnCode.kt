package com.arthenica.ffmpegkit

/**
 * Stub implementation of ReturnCode for compilation in environment where Maven Central artifact is missing.
 */
class ReturnCode(val value: Int) {
    companion object {
        fun isSuccess(returnCode: ReturnCode): Boolean {
            return returnCode.value == 0
        }
    }
}
