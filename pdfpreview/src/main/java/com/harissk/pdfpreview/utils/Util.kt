package com.harissk.pdfpreview.utils

import android.content.Context
import android.util.TypedValue
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Created by Harishkumar on 25/11/23.
 */

object Util {
    private const val DEFAULT_BUFFER_SIZE = 1024 * 4
    fun getDP(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    @Throws(IOException::class)
    fun toByteArray(inputStream: InputStream): ByteArray {
        val os = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var n: Int
        while (-1 != inputStream.read(buffer).also { n = it }) {
            os.write(buffer, 0, n)
        }
        return os.toByteArray()
    }
}