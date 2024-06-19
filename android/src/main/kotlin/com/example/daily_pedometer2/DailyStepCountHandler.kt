package com.example.daily_pedometer2

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Looper
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import android.os.Handler
import java.util.Calendar
import android.util.Log

class DailyStepCountHandler() : EventChannel.StreamHandler {

    private lateinit var context: Context
    private lateinit var flutterPluginBinding: FlutterPlugin.FlutterPluginBinding
    private lateinit var sharedPrefs: SharedPreferences
    
    private var sensorEventListener: SensorEventListener? = null
    private var sensorManager: SensorManager? = null
    private var stepCounterSensor: Sensor? = null
    private var dailyStepCount: Int = 0
    private var initialStepCount: Int = -1
    
    constructor(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) : this() {
        this.context = flutterPluginBinding.applicationContext
        this.sharedPrefs = context.getSharedPreferences("pedometerPrefs", Context.MODE_PRIVATE)
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        this.flutterPluginBinding = flutterPluginBinding

        // Load saved step count and initial step count at initialization
        dailyStepCount = sharedPrefs.getInt("dailyStepCount", 0);
        initialStepCount = sharedPrefs.getInt("initialStepCount", -1)
        val savedDate = sharedPrefs.getLong("lastSavedDate", 0L);

        Log.d("DailyStepCountHandler", "Initial load - dailyStepCount: $dailyStepCount, initialStepCount: $initialStepCount")

        if (isDifferentDay(savedDate)) {
            // Reset step count at the start of a new day
            resetStepCount()
        }
    }
    private fun getDailyEventListener(events: EventChannel.EventSink): SensorEventListener? {
        return object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
                    val currentStepCount = event.values[0].toInt();
              
                    if (initialStepCount == -1) {
                        initialStepCount = currentStepCount
                        // Save the initial step count
                        sharedPrefs.edit().putInt("initialStepCount", initialStepCount).apply()
                        Log.d("DailyStepCountHandler", "Initial step count set to: $initialStepCount")
                    }
                    
                    if(currentStepCount >= initialStepCount){
                      dailyStepCount = currentStepCount - initialStepCount
                    }else{
                        dailyStepCount = initialStepCount
                    }
                  
                    // Save the updated step count
                    sharedPrefs.edit().putInt("dailyStepCount", dailyStepCount).apply();
                    val savedDate = sharedPrefs.getLong("lastSavedDate", 0L)
                    val result = mapOf(
                        "daily_step_count" to dailyStepCount,
                        "save_date" to savedDate
                    )
                    Log.d("DailyStepCountHandler", "Saved step count: $result")
                    Log.d("DailyStepCountHandler", "Current step count: $currentStepCount, Daily step count: $dailyStepCount")
                    events!!.success(result)
                }
            }
        }
    }

    private fun isDifferentDay(savedDate: Long): Boolean {
        val savedCalendar = Calendar.getInstance().apply { timeInMillis = savedDate }
        val currentCalendar = Calendar.getInstance()

        return savedCalendar.get(Calendar.DAY_OF_YEAR) != currentCalendar.get(Calendar.DAY_OF_YEAR) ||
                savedCalendar.get(Calendar.YEAR) != currentCalendar.get(Calendar.YEAR) || (currentCalendar.get(Calendar.HOUR_OF_DAY) == 16 && currentCalendar.get(Calendar.MINUTE) == 25)
    //    // Get the current hour and minute in 12-hour format with AM/PM
    //     int hour = currentCalendar.get(Calendar.HOUR);
    //     int minute = currentCalendar.get(Calendar.MINUTE);
    //     int second = currentCalendar.get(Calendar.SECOND);
    //     String amPm = currentCalendar.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";

    //    // Check if the time is exactly 12:00:00 AM
    //     boolean isMidnight = (hour == 0 && minute == 0 && second == 0 && amPm.equals("AM"));   
    //     return isDifferentDay || isMidnight;
        }
    private fun resetStepCount() {
 // Reset step count at the start of a new day
 Log.d("DailyStepCountHandler", "Restarting daily step count")

         dailyStepCount = 0
         initialStepCount = -1
        sharedPrefs.edit().putLong("lastSavedDate", System.currentTimeMillis()).apply();
        Log.d("DailyStepCountHandler", "New day - dailyStepCount reset to: $dailyStepCount, initialStepCount reset to: $initialStepCount")
    }


    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        if (stepCounterSensor == null) {
            events!!.error("1", "DailyStepCount not available", "DailyStepCount is not available on this device");
        } else {
            sensorEventListener = getDailyEventListener(events!!);
            sensorManager!!.registerListener(sensorEventListener, stepCounterSensor, SensorManager.SENSOR_DELAY_FASTEST);
            Log.d("DailyStepCountHandler", "Sensor listener registered")
            val savedDate = sharedPrefs.getLong("lastSavedDate", 0L);
            Log.d("DailyStepCountHandler", "savedDate: $savedDate")
            if (isDifferentDay(savedDate)) {
                Log.d("isDifferentDay", "true")
                resetStepCount()
            }
        }
    }

    override fun onCancel(arguments: Any?) {
        sensorManager!!.unregisterListener(sensorEventListener);
        Log.d("DailyStepCountHandler", "Sensor listener unregistered")
    }

}