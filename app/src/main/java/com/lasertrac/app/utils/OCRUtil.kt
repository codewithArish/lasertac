package com.lasertrac.app.utils

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * OCR helper using ML Kit Text Recognition.
 *
 * Usage:
 *  - Add ML Kit dependency to your module Gradle (see the README section below).
 *  - Call recognizeTextFromImageUri from a coroutine (e.g., viewModelScope.launch) and handle the returned Result.
 */
object OCRUtil {
    /**
     * Recognizes text from an image Uri using ML Kit Text Recognition.
     *
     * This function is a suspend function and returns a Result<String> containing the recognized text
     * on success or the caught Exception on failure.
     *
     * Note: This implementation uses InputImage.fromFilePath which works for content/file Uris accessible
     * via the provided Context. If your Uri comes from a camera intent or a content provider that
     * requires special handling, adapt accordingly.
     */
    suspend fun recognizeTextFromImageUri(context: Context, uri: Uri): Result<String> {
        return try {
            // Build ML Kit InputImage from the Uri
            val inputImage = InputImage.fromFilePath(context, uri)

            // Create text recognizer using Latin options (on-device)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            // Process the image and await the result using kotlinx-coroutines-play-services
            val visionText = recognizer.process(inputImage).await()

            // Return recognized full text
            val recognizedText: String = visionText.text
            Result.success(recognizedText)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
