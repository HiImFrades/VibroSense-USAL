package com.example.terminalbluetooth

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import java.io.IOException



class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if(MainActivity.flagAlarma)
            sendAlarm("Alarma")
        val alarmMgr = context?.getSystemService(ComponentActivity.ALARM_SERVICE) as AlarmManager
        val intentAlarm = Intent(context, AlarmReceiver::class.java)
        val alarmIntent = PendingIntent.getBroadcast(context, 0, intentAlarm, PendingIntent.FLAG_IMMUTABLE)

        if (alarmMgr.nextAlarmClock != null)
            alarmMgr.setAlarmClock(alarmMgr.nextAlarmClock, alarmIntent)
    }
}

fun sendAlarm(input: String) {
    if (MainActivity.m_bluetoothSocket != null) {
        try{
            MainActivity.m_bluetoothSocket!!.outputStream.write(input.toByteArray())
        } catch(e: IOException) {
            e.printStackTrace()
        }
    }
}
