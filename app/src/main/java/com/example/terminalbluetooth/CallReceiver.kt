package com.example.terminalbluetooth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import java.io.IOException
import java.util.Date

/**
 * Clase del recibidor de llamada entrante
 *
 * Esta clase deriva de la clase BroadcastReceiver() y
 * tiene la función de enviar la señal de llamada al equipo externo
 *
 */
class CallReceiver : BroadcastReceiver() {

    /**
     * Función que se ejecuta al dispararse el recibidor registrado en la actividad principal.
     *
     * Cuando reciba la señal registrada de las llamadas en la actividad principal
     * enviará la orden de llamada al equipo externo por Bluetooth.
     *
     * @param context contexto de la actividad principal.
     * @param intent acción que se registra en el recibidor.
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        if(MainActivity.flagLlamada)
            sendCall("Llamada")
    }
}

/**
 * Función que envía una String por Bluetooth mediante el socket
 * del módulo Bluetooth del dispositivo.
 *
 * @param input String a enviar al equipo externo.
 */
fun sendCall(input: String) {
    if (MainActivity.m_bluetoothSocket != null) {
        try{
            MainActivity.m_bluetoothSocket!!.outputStream.write(input.toByteArray())
        } catch(e: IOException) {
            e.printStackTrace()
        }
    }
}
