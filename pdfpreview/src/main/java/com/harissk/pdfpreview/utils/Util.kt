package com.harissk.pdfpreview.utils

import android.content.Context
import android.util.TypedValue

/**
 * Created by Harishkumar on 25/11/23.
 */

object Util {
    fun getDP(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

}