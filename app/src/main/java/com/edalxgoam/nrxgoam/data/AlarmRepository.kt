package com.edalxgoam.nrxgoam.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class AlarmRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveAlarm(alarm: Alarm) {
        val alarms = getAllAlarms().toMutableList()
        alarms.add(alarm)
        saveAlarms(alarms)
    }

    fun getAllAlarms(): List<Alarm> {
        val json = prefs.getString(KEY_ALARMS, "[]")
        val type = object : TypeToken<List<Alarm>>() {}.type
        return gson.fromJson(json, type)
    }

    fun updateAlarm(alarm: Alarm) {
        val alarms = getAllAlarms().toMutableList()
        val index = alarms.indexOfFirst { it.id == alarm.id }
        if (index != -1) {
            alarms[index] = alarm
            saveAlarms(alarms)
        }
    }

    fun deleteAlarm(alarmId: Long) {
        val alarms = getAllAlarms().toMutableList()
        alarms.removeAll { it.id == alarmId }
        saveAlarms(alarms)
    }

    private fun saveAlarms(alarms: List<Alarm>) {
        val json = gson.toJson(alarms)
        prefs.edit().putString(KEY_ALARMS, json).apply()
    }

    companion object {
        private const val PREFS_NAME = "AlarmPrefs"
        private const val KEY_ALARMS = "alarms"
    }
} 