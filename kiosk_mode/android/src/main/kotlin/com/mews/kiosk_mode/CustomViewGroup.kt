import android.content.Context
import android.view.View
import android.view.ViewGroup

class CustomViewGroup(context: Context) : ViewGroup(context) {
    override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {}

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        // Intercept touch events to prevent status bar from being accessed
        return true
    }
}
