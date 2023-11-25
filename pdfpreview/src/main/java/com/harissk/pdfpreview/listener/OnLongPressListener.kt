package com.harissk.pdfpreview.listener

import android.view.MotionEvent

/**
 * Created by Harishkumar on 25/11/23.
 */
/**
 * Implement this interface to receive events from PDFView
 * when view has been long pressed
 */
fun interface OnLongPressListener {
    /**
     * Called when the user has a long tap gesture, before processing scroll handle toggling
     *
     * @param motionEvent MotionEvent that registered as a confirmed long press
     */
    fun onLongPress(motionEvent: MotionEvent?)
}