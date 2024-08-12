package com.mews.kiosk_mode

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler

private const val methodChannelName = "com.mews.kiosk_mode/kiosk_mode"
private const val eventChannelName = "com.mews.kiosk_mode/kiosk_mode_stream"

class KioskModePlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    private lateinit var channel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private var activity: Activity? = null
    private lateinit var kioskModeHandler: KioskModeStreamHandler
    private var windowManager: WindowManager? = null
    private var interceptView: CustomViewGroup? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, methodChannelName)
        channel.setMethodCallHandler(this)
        kioskModeHandler = KioskModeStreamHandler(this::isInKioskMode)

        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, eventChannelName)
        eventChannel.setStreamHandler(kioskModeHandler)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "startKioskMode" -> startKioskMode(result)
            "stopKioskMode" -> stopKioskMode(result)
            "isInKioskMode" -> isInKioskMode(result)
            "isManagedKiosk" -> isManagedKiosk(result)
            else -> result.notImplemented()
        }
    }

    private fun startKioskMode(result: MethodChannel.Result) {
        activity?.let { a ->
            a.findViewById<ViewGroup>(android.R.id.content).getChildAt(0).post {
                try {
                    a.startLockTask()

                    // Lock the status bar by overlaying a custom view
                    lockStatusBar(a)

                    result.success(true)
                    kioskModeHandler.emit()
                } catch (e: IllegalArgumentException) {
                    result.success(false)
                }
            }
        } ?: result.success(false)
    }

    private fun lockStatusBar(context: Context) {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams()
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR
        params.gravity = Gravity.TOP
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        params.width = WindowManager.LayoutParams.MATCH_PARENT

        // Get the status bar height
        val resId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight = if (resId > 0) context.resources.getDimensionPixelSize(resId) else 0
        params.height = statusBarHeight
        params.format = PixelFormat.TRANSPARENT

        interceptView = CustomViewGroup(context)
        try {
            windowManager?.addView(interceptView, params)
        } catch (e: RuntimeException) {
            e.printStackTrace()
        }
    }

    private fun stopKioskMode(result: MethodChannel.Result) {
        activity?.stopLockTask()
        interceptView?.let {
            windowManager?.removeView(it)
            interceptView = null
        }
        result.success(null)
        kioskModeHandler.emit()
    }

    private fun isManagedKiosk(result: MethodChannel.Result) {
        val service = activity?.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        if (service == null) {
            result.success(null)
            return
        }

        result.success(service.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_LOCKED)
    }

    private fun isInKioskMode(result: MethodChannel.Result) {
        result.success(isInKioskMode())
    }

    private fun isInKioskMode(): Boolean? {
        val service = activity?.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return null

        return when (service.lockTaskModeState) {
            ActivityManager.LOCK_TASK_MODE_PINNED,
            ActivityManager.LOCK_TASK_MODE_LOCKED -> true
            else -> false
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        this.activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {}
    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {}

    override fun onDetachedFromActivity() {
        this.activity = null
    }
}
