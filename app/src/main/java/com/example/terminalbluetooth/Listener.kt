package com.example.terminalbluetooth

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.widget.Toast
import java.io.IOException

class Listener: NotificationListenerService() {

    /**
     * Función que se ejecuta al dispararse el listener registrado en la actividad principal.
     *
     * Cuando el sistema reciba una notificación de una aplicación externa, esta función
     * recoge esa notificación para poder obtener información sobre qué aplicación se
     * está notificando.
     *
     * @param notification notificación recogida.
     */
    override fun onNotificationPosted(notification: StatusBarNotification) {
        Toast.makeText(this, notification.packageName, Toast.LENGTH_LONG).show()
        if (notification.packageName == "com.whatsapp") {
            if(MainActivity.flagWhatsapp)
                sendNotification("Whatsapp")
        }else if(notification.packageName == "com.google.android.gm") {
            if(MainActivity.flagGmail)
                sendNotification("Gmail")
        }
    }
}

/**
 * Función que envía una String por Bluetooth mediante el socket
 * del módulo Bluetooth del dispositivo.
 *
 * @param input String a enviar al equipo externo.
 */
fun sendNotification(input: String) {
    if (MainActivity.m_bluetoothSocket != null) {
        try{
            MainActivity.m_bluetoothSocket!!.outputStream.write(input.toByteArray())
        } catch(e: IOException) {
            e.printStackTrace()
        }
    }
}