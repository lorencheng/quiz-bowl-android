package com.quizbowl.app.data

import android.graphics.Bitmap
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf

class FeedbackManager {
    val isOpen = mutableStateOf(false)
    val screenshot = mutableStateOf<Bitmap?>(null)

    fun open(bmp: Bitmap? = null) {
        screenshot.value = bmp
        isOpen.value = true
    }

    fun dismiss() {
        isOpen.value = false
        screenshot.value?.recycle()
        screenshot.value = null
    }
}

val LocalFeedbackManager = compositionLocalOf<FeedbackManager> { error("No FeedbackManager provided") }
