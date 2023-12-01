package com.harissk.pdfpreview.utils

import android.content.Context
import android.util.TypedValue

internal fun Context.toPx(dp: Float) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()