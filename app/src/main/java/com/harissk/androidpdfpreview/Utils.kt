package com.harissk.androidpdfpreview

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

internal fun Uri.getFileName(context: Context): String? {
    Log.d("PDFPreview", "getFileName: $this")
    val cursor = context.contentResolver.query(this, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            Log.d("PDFPreview", "getFileName: ${it.getString(displayNameIndex)}")
            return it.getString(displayNameIndex)
        }
    }
    return null
}

internal fun Uri.uriToFile(context: Context) = try {
    Log.d("PDFPreview", "uriToFile: $this")
    val tempFile = File.createTempFile("temp_pdf", ".pdf", context.cacheDir)
    context.contentResolver.openInputStream(this)?.use { inputStream ->
        FileOutputStream(tempFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }
    Log.d("PDFPreview", "uriToFile: ${tempFile.absolutePath}")
    tempFile
} catch (e: IOException) {
    e.printStackTrace()
    null
}