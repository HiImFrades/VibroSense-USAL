package com.example.terminalbluetooth

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import java.io.IOException


/**
 * Clase del recibidor de las alarmas
 *
 * Esta clase deriva de la clase BroadcastReceiver() y
 * tiene dos funciones: enviar la señal de alarma al equipo
 * y reigstrar la siguiente alarma del dispositivo móvil.
 *
 */
class AlarmReceiver : BroadcastReceiver() {

    /**
     * Función que se ejecuta al dispararse el recibidor registrado en la actividad principal.
     *
     * Cuando reciba la señal registrada de la alarma en la actividad principal
     * enviará la orden de alarma al equipo externo por Bluetooth
     * y buscará la siguiente alarma del dispositivo para registrarla.
     *
     * @param context contexto de la actividad principal.
     * @param intent acción que se registra en el recibidor.
     */
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

/**
 * Función que envía una String por Bluetooth mediante el socket
 * del módulo Bluetooth del dispositivo.
 *
 * @param input String a enviar al equipo externo.
 */
fun sendAlarm(input: String) {
    if (MainActivity.m_bluetoothSocket != null) {
        try{
            MainActivity.m_bluetoothSocket!!.outputStream.write(input.toByteArray())
        } catch(e: IOException) {
            e.printStackTrace()
        }
    }
}
