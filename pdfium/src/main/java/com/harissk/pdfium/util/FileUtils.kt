package com.harissk.pdfium.util

import android.os.ParcelFileDescriptor
import java.io.FileDescriptor
import java.lang.reflect.Field

/**
 * Created by Harishkumar on 26/11/23.
 */
object FileUtils {
    private val mFdField: Field by lazy {
        FileDescriptor::class.java.getDeclaredField("descriptor").apply {
            isAccessible = true
        }
    }

    fun getNumFd(fdObj: ParcelFileDescriptor?): Int = try {
        mFdField.getInt(fdObj?.fileDescriptor ?: throw NullPointerException())
    } catch (e: Exception) {
        -1
    }
}