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

    private lateinit var stepDetectionHandler: SensorStreamHandler
    private lateinit var stepCountHandler: SensorStreamHandler
    private lateinit var dailyStepCountHandler: DailyStepCountHandler

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        initializeChannelsAndHandlers(flutterPluginBinding)
    }

    private fun initializeChannelsAndHandlers(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        /// Create channels
        stepDetectionChannel = EventChannel(flutterPluginBinding.binaryMessenger, "step_detection")
        stepCountChannel = EventChannel(flutterPluginBinding.binaryMessenger, "step_count")
        dailyStepCountChannel = EventChannel(flutterPluginBinding.binaryMessenger, "daily_step_count")
        isDifferentDayChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "reset_step_count")
        
        /// Create handlers
        stepDetectionHandler = SensorStreamHandler(flutterPluginBinding, Sensor.TYPE_STEP_DETECTOR)
        stepCountHandler = SensorStreamHandler(flutterPluginBinding, Sensor.TYPE_STEP_COUNTER)
        dailyStepCountHandler = DailyStepCountHandler(flutterPluginBinding)

        isDifferentDayChannel.setMethodCallHandler { call, result ->
            Log.d("Method channel", "isDifferentDay")
            if (call.method == "isDifferentDay") {
                dailyStepCountHandler.resetStepCount()
                reinitializeStreams()
            } 
        }

        /// Set handlers
        stepDetectionChannel.setStreamHandler(stepDetectionHandler)
        stepCountChannel.setStreamHandler(stepCountHandler)
        dailyStepCountChannel.setStreamHandler(dailyStepCountHandler)
    }

    private fun reinitializeStreams() {
        Log.d("Method channel", "Reset the handlers")

        /// Reset the handlers
        stepDetectionChannel.setStreamHandler(null)
        stepCountChannel.setStreamHandler(null)
        dailyStepCountChannel.setStreamHandler(null)
        
        /// Reattach the handlers to the channels
        Log.d("Method channel", " Reattach the handlers to the channels")

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
