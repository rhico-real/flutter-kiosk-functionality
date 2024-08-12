package com.mews.kiosk_mode

import android.app.Activity
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler

private const val methodChannelName = "com.mews.kiosk_mode/kiosk_mode"

class KioskModePlugin : FlutterPlugin, MethodCallHandler, ActivityAware {

    private lateinit var channel: MethodChannel
    private var activity: Activity? = null

    private lateinit var windowManager: WindowManager
    private lateinit var interceptView: CustomViewGroup

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, methodChannelName)
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "startKioskMode" -> startKioskMode(result)
            "stopKioskMode" -> stopKioskMode(result)
            else -> result.notImplemented()
        }
    }

    private fun startKioskMode(result: MethodChannel.Result) {
        activity?.let { a ->
            windowManager = a.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val params = WindowManager.LayoutParams().apply {
                type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR
                gravity = Gravity.TOP
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                width = WindowManager.LayoutParams.MATCH_PARENT

                // Get the status bar height
                val resId = a.resources.getIdentifier("status_bar_height", "dimen", "android")
                height = if (resId > 0) a.resources.getDimensionPixelSize(resId) else 0
                format = PixelFormat.TRANSPARENT
            }

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

    private fun stopKioskMode(result: MethodChannel.Result) {
        activity?.let {
            try {
                windowManager.removeView(interceptView)
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
    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        this.activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        this.activity = null
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}