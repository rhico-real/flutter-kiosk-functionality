package com.mews.kiosk_mode

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class KioskModePlugin : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {

    private lateinit var channel: MethodChannel
    private var activity: Activity? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "com.mews.kiosk_mode/kiosk_mode")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "startKioskMode" -> startKioskMode(result)
            "stopKioskMode" -> stopKioskMode(result)
            "isDefaultHomeLauncher" -> isDefaultHomeLauncher(result)
            else -> result.notImplemented()
        }
    }

    private fun startKioskMode(result: MethodChannel.Result) {
        activity?.let { a ->
            redirectToHomeLauncherSettings(a)
        } ?: result.success(false)
    }

    private fun stopKioskMode(result: MethodChannel.Result) {
        activity?.let { a ->
            try {
                redirectToHomeLauncherSettings(a)
                result.success(true)
            } catch (e: RuntimeException) {
                e.printStackTrace()
                result.success(false)
            }
        } ?: result.success(false)
    }

    private fun isDefaultHomeLauncher(result: MethodChannel.Result) {
        activity?.let { a ->
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            val resolveInfo = a.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            val defaultHome = resolveInfo?.activityInfo?.packageName
            val isDefault = defaultHome == a.packageName
            result.success(isDefault)
        } ?: result.success(false)
    }

    private fun redirectToHomeLauncherSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
        activity.startActivity(intent)
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
