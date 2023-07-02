package com.example.terminalbluetooth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.io.IOException

class PowerOn : BroadcastReceiver() {

    /**
     * Función que se ejecuta al dispararse el recibidor registrado en la actividad principal.
     *
     * Cuando reciba la señal registrada de conectar el cargador en la actividad principal
     * enviará la orden de power al equipo externo por Bluetooth.
     *
     * @param context contexto de la actividad principal.
     * @param intent acción que se registra en el recibidor.
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        if(MainActivity.flagPower)
            sendPowerOn("Power")
    }
}

/**
 * Función que envía una String por Bluetooth mediante el socket
 * del módulo Bluetooth del dispositivo.
 *
 * @param input String a enviar al equipo externo.
 */
fun sendPowerOn(input: String) {
    if (MainActivity.m_bluetoothSocket != null) {
        try{
            MainActivity.m_bluetoothSocket!!.outputStream.write(input.toByteArray())
        } catch(e: IOException) {
            e.printStackTrace()
        }
    }
}