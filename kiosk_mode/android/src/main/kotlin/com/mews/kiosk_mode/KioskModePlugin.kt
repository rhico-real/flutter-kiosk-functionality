package com.mews.kiosk_mode

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.view.View
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
                    // Start Lock Task (Kiosk) Mode
                    a.startLockTask()
    
                    // Adjust the system UI to keep the status bar visible and swipable
                    a.window.decorView.systemUiVisibility = (
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_VISIBLE
                            )
    
                    // Keep the screen on
                    a.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    
                    result.success(true)
                    kioskModeHandler.emit()
                } catch (e: IllegalArgumentException) {
                    result.success(false)
                }
            }
        } ?: result.success(false)
    }

    private fun stopKioskMode(result: MethodChannel.Result) {
        activity?.let { a ->
            // Stop Lock Task (Kiosk) Mode
            a.stopLockTask()
          
            // Restore the system UI visibility
            a.window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    )
    
            // Remove the flag to allow the screen to turn off
            a.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
