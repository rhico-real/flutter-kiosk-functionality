import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.graphics.PixelFormat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

private const val REQUEST_CODE_OVERLAY_PERMISSION = 1001

class KioskModePlugin : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {

    private lateinit var channel: MethodChannel
    private var activity: Activity? = null
    private lateinit var windowManager: WindowManager
    private lateinit var interceptView: CustomViewGroup

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "com.mews.kiosk_mode/kiosk_mode")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "startKioskMode" -> startKioskMode(result)
            else -> result.notImplemented()
        }
    }

    private fun startKioskMode(result: MethodChannel.Result) {
        activity?.let { a ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !Settings.canDrawOverlays(a)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + a.packageName))
                a.startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
                result.success(false)
                return
            }

            windowManager = a.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val params = WindowManager.LayoutParams()
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            params.gravity = Gravity.TOP
            params.flags = (
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            )
            params.width = WindowManager.LayoutParams.MATCH_PARENT

            val resId = a.resources.getIdentifier("status_bar_height", "dimen", "android")
            params.height = if (resId > 0) a.resources.getDimensionPixelSize(resId) else 0
            params.format = PixelFormat.TRANSPARENT

            interceptView = CustomViewGroup(a)

            try {
                windowManager.addView(interceptView, params)
                result.success(true)
            } catch (e: RuntimeException) {
                e.printStackTrace()
                result.success(false)
            }
        } ?: result.success(false)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        this.activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {}

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {}

    override fun onDetachedFromActivity() {
        this.activity = null
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}
