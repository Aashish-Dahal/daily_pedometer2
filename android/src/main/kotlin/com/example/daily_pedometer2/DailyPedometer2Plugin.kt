package com.example.daily_pedometer2

import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import android.util.Log



/** DailyPedometer2Plugin */
class DailyPedometer2Plugin : FlutterPlugin {
    private lateinit var stepDetectionChannel: EventChannel
    private lateinit var stepCountChannel: EventChannel
    private lateinit var dailyStepCountChannel: EventChannel
    private lateinit var isDifferentDayChannel: MethodChannel

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        /// Create channels

        stepDetectionChannel = EventChannel(flutterPluginBinding.binaryMessenger, "step_detection")
        stepCountChannel = EventChannel(flutterPluginBinding.binaryMessenger, "step_count")
        dailyStepCountChannel = EventChannel(flutterPluginBinding.binaryMessenger, "daily_step_count")
        isDifferentDayChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "reset_step_count")
      
        
        /// Create handlers
        val stepDetectionHandler = SensorStreamHandler(flutterPluginBinding, Sensor.TYPE_STEP_DETECTOR)
        val stepCountHandler = SensorStreamHandler(flutterPluginBinding, Sensor.TYPE_STEP_COUNTER)
        val dailyStepCountHandler = DailyStepCountHandler(flutterPluginBinding)

        isDifferentDayChannel.setMethodCallHandler { call, result ->
            Log.d("Method channel", "isDifferentDay")
            if (call.method == "isDifferentDay") {
                dailyStepCountHandler.resetStepCount()
            } 
        }

        /// Set handlers
        stepDetectionChannel.setStreamHandler(stepDetectionHandler)
        stepCountChannel.setStreamHandler(stepCountHandler)
        dailyStepCountChannel.setStreamHandler(dailyStepCountHandler)
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        stepDetectionChannel.setStreamHandler(null)
        stepCountChannel.setStreamHandler(null)
        dailyStepCountChannel.setStreamHandler(null)
    }

}
