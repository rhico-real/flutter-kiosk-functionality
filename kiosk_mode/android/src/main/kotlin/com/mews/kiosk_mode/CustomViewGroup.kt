package com.yahshua.yahshua_timekeeper_rfid.Utils

import android.content.Context
import android.view.MotionEvent
import android.view.ViewGroup

class CustomViewGroup(context: Context) : ViewGroup(context) {

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // No layout logic needed as this view is used to intercept touch events
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Intercept all touch events
        return true
    }
}
