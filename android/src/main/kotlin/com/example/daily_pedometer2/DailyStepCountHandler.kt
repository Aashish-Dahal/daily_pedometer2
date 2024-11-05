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
import java.util.Date


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
            resetStepCount()
        }
    }
    private fun getDailyEventListener(events: EventChannel.EventSink): SensorEventListener? {
        var isNewDayFlag = false
        return object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
                    val currentStepCount = event.values[0].toInt();


                    val currentDate = System.currentTimeMillis()
                    val lastSavedDate = sharedPrefs.getLong("lastSavedDate", 0L)

                    val isNewDay = !isSameDay(lastSavedDate, currentDate)

                    if (isNewDay) {
                        // Reset initial step count for a new day
                        isNewDayFlag = true
                        initialStepCount = currentStepCount
                        sharedPrefs.edit().putInt("initialStepCount", initialStepCount).apply()
                        
                        // Update the last saved date
                        sharedPrefs.edit().putLong("lastSavedDate", currentDate).apply()

                        Log.d("DailyStepCountHandler", "5-minute interval reached. Initial step count reset to: $initialStepCount")

                        Log.d("DailyStepCountHandler", "New day detected. Initial step count reset to: $initialStepCount")
                    }
                    if (initialStepCount == -1) {
                        initialStepCount = currentStepCount
                        // Save the initial step count
                        sharedPrefs.edit().putInt("initialStepCount", initialStepCount).apply()
                        sharedPrefs.edit().putLong("lastSavedDate", currentDate).apply()
                        Log.d("DailyStepCountHandler", "Initial step count set to: $initialStepCount")
                    }
                    
                    if(currentStepCount >= initialStepCount){
                      dailyStepCount = currentStepCount - initialStepCount
                    }else{
                        dailyStepCount = initialStepCount
                    }
                  
                    // Save the updated step count
                    sharedPrefs.edit().putInt("dailyStepCount", dailyStepCount).apply();
                    dailyStepCount = maxOf(currentStepCount - initialStepCount, 0)
                    
                    val result = mapOf(
                        "daily_step_count" to dailyStepCount,
                        "save_date" to lastSavedDate,
                        "is_new_day" to isNewDayFlag
                    )
                    Log.d("DailyStepCountHandler", "Saved step count: $result")
                    Log.d("DailyStepCountHandler", "Current step count: $currentStepCount, Daily step count: $dailyStepCount")

                    events!!.success(result)

                    if (isNewDayFlag) {
                        isNewDayFlag = false
                    }
                }
            }
        }
    }

    private fun isDifferentDay(savedDate: Long): Boolean {
        val savedCalendar = Calendar.getInstance().apply { timeInMillis = savedDate }
        val currentCalendar = Calendar.getInstance()

        return savedCalendar.get(Calendar.DAY_OF_YEAR) != currentCalendar.get(Calendar.DAY_OF_YEAR) ||
                savedCalendar.get(Calendar.YEAR) != currentCalendar.get(Calendar.YEAR) 
    
        }

    private fun isSameDay(time1: Long, time2: Long): Boolean {
            val calendar1 = Calendar.getInstance().apply { timeInMillis = time1 }
            val calendar2 = Calendar.getInstance().apply { timeInMillis = time2 }
            return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
                   calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR)
        }

 fun resetStepCount(): Map<String, Any> {
        // Reset step count at the start of a new day
        Log.d("DailyStepCountHandler", "resetting")
        dailyStepCount = 0
        initialStepCount = -1
        sharedPrefs.edit().putLong("lastSavedDate", System.currentTimeMillis()).apply();
        Log.d("DailyStepCountHandler", "New day - dailyStepCount reset to: $dailyStepCount, initialStepCount reset to: $initialStepCount")
        val savedDate = sharedPrefs.getLong("lastSavedDate", 0L)
        val status= mapOf(
                        "daily_step_count" to dailyStepCount,
                        "save_date" to savedDate,
                        "status" to true
                    )
        return status;
    }


    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        if (stepCounterSensor == null) {
            events!!.error("1", "DailyStepCount not available", "DailyStepCount is not available on this device");
        } else {
            sensorEventListener = getDailyEventListener(events!!);
            sensorManager!!.registerListener(sensorEventListener, stepCounterSensor, SensorManager.SENSOR_DELAY_FASTEST);
            Log.d("DailyStepCountHandler", "Sensor listener registered")
        }
      
    }

    override fun onCancel(arguments: Any?) {
        sensorManager!!.unregisterListener(sensorEventListener);
        Log.d("DailyStepCountHandler", "Sensor listener unregistered")
    }

}