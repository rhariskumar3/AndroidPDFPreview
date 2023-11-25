package com.harissk.pdfpreview.listener

import android.view.MotionEvent

/**
 * Created by Harishkumar on 25/11/23.
 */
/**
 * Implement this interface to receive events from PDFView
 * when view has been touched
 */
fun interface OnTapListener {
    /**
     * Called when the user has a tap gesture, before processing scroll handle toggling
     *
     * @param motionEvent MotionEvent that registered as a confirmed single tap
     * @return true if the single tap was handled, false to toggle scroll handle
     */
    fun onTap(motionEvent: MotionEvent?): Boolean
}