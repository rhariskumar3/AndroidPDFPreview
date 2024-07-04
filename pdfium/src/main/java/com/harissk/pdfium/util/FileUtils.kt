package com.harissk.pdfium.util

import android.os.ParcelFileDescriptor
import java.io.FileDescriptor
import java.lang.reflect.Field

/**
 * Utility class for file-related operations.
 */
object FileUtils {
    private val mFdField: Field by lazy {
        FileDescriptor::class.java.getDeclaredField("descriptor").apply {
            isAccessible = true
        }
    }

    /**
     * Retrieves the file descriptor number from a {@link ParcelFileDescriptor}.
     *
     * @param fdObj The {@link ParcelFileDescriptor} to get the file descriptor from.
     * @return The file descriptor number, or -1 if an error occurred.
     */
    fun getNumFd(fdObj: ParcelFileDescriptor?): Int = try {
        mFdField.getInt(fdObj?.fileDescriptor ?: throw NullPointerException())
    } catch (e: Exception) {
        -1
    }
}