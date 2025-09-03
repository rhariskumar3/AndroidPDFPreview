package com.harissk.pdfpreview.listener

import android.view.MotionEvent
import androidx.annotation.MainThread

/**
 * Copyright [2025] [Haris Kumar R](https://github.com/rhariskumar3)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * */

/**
 * Interface for listening to gesture events.
 */
interface GestureEventListener {
    /**
     * Called when the user has a tap gesture, before processing scroll handle toggling.
     *
     * @param motionEvent The {@link MotionEvent} that registered as a confirmed single tap.
     * @return true if the single tap was handled, false to toggle scroll handle.
     */
    @MainThread
    fun onTap(motionEvent: MotionEvent): Boolean

    /**
     * Called when the user has a long tap gesture, before processing scroll handle toggling.
     *
     * @param motionEvent The {@link MotionEvent} that registered as a confirmed long press.
     */
    @MainThread
    fun onLongPress(motionEvent: MotionEvent)
}