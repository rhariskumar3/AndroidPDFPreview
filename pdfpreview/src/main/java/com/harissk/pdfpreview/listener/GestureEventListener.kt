package com.harissk.pdfpreview.listener

import android.view.MotionEvent
import androidx.annotation.MainThread

/**
 * Created by Harishkumar on 05/12/23.
 */
/**
 * For PDF page gesture events
 */
interface GestureEventListener {
    /**
     * Called when the user has a tap gesture, before processing scroll handle toggling
     *
     * @param motionEvent MotionEvent that registered as a confirmed single tap
     * @return true if the single tap was handled, false to toggle scroll handle
     */
    @MainThread
    fun onTap(motionEvent: MotionEvent): Boolean

    /**
     * Called when the user has a long tap gesture, before processing scroll handle toggling
     *
     * @param motionEvent MotionEvent that registered as a confirmed long press
     */
    @MainThread
    fun onLongPress(motionEvent: MotionEvent)
}