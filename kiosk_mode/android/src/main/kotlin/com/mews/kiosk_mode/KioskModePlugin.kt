package com.mews.kiosk_mode

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
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
    private var interceptView: CustomViewGroup? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "com.mews.kiosk_mode/kiosk_mode")
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !Settings.canDrawOverlays(a)) {
                requestOverlayPermission(a)
                result.success(false)
                return
            }

            try {
                setupWindowManager(a)
                interceptView?.let { view ->
                    windowManager.addView(view, createLayoutParams(a))
                    result.success(true)
                } ?: result.success(false)
            } catch (e: RuntimeException) {
                e.printStackTrace()
                result.success(false)
            }
        } ?: result.success(false)
    }

    private fun stopKioskMode(result: MethodChannel.Result) {
        activity?.let {
            try {
                interceptView?.let { view ->
                    windowManager.removeView(view)
                    interceptView = null
                }
                result.success(true)
            } catch (e: RuntimeException) {
                e.printStackTrace()
                result.success(false)
            }
        } ?: result.success(false)
    }

    private fun requestOverlayPermission(activity: Activity) {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + activity.packageName))
        activity.startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
    }

    private fun setupWindowManager(activity: Activity) {
        windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        interceptView = CustomViewGroup(activity)
    }

    private fun createLayoutParams(activity: Activity): WindowManager.LayoutParams {
        val params = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            gravity = Gravity.TOP
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            width = WindowManager.LayoutParams.MATCH_PARENT

            val resId = activity.resources.getIdentifier("status_bar_height", "dimen", "android")
            height = if (resId > 0) activity.resources.getDimensionPixelSize(resId) else 0
            format = PixelFormat.TRANSPARENT
        }
        return params
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
