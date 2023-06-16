package com.example.terminalbluetooth

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.widget.Toast
import java.io.IOException

class Listener: NotificationListenerService() {
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

fun sendNotification(input: String) {
    if (MainActivity.m_bluetoothSocket != null) {
        try{
            MainActivity.m_bluetoothSocket!!.outputStream.write(input.toByteArray())
        } catch(e: IOException) {
            e.printStackTrace()
        }
    }
}