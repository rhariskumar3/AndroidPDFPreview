package com.harissk.pdfium.util

import android.os.ParcelFileDescriptor
import java.io.FileDescriptor
import java.lang.reflect.Field

/**
 * Created by Harishkumar on 26/11/23.
 */
object FileUtils {
    private val FD_CLASS: Class<*> = FileDescriptor::class.java

    private var mFdField: Field? = null

    fun getNumFd(fdObj: ParcelFileDescriptor?): Int = try {
        if (mFdField == null) {
            mFdField = FD_CLASS.getDeclaredField("descriptor")
            mFdField?.isAccessible = true
        }
        mFdField?.getInt(fdObj?.fileDescriptor) ?: -1
    } catch (e: ReflectiveOperationException) {
        e.printStackTrace()
        -1
    }
}