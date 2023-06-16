package com.example.terminalbluetooth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import java.io.IOException
import java.util.Date

class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if(MainActivity.flagLlamada)
            sendCall("Llamada")
    }
}

fun sendCall(input: String) {
    if (MainActivity.m_bluetoothSocket != null) {
        try{
            MainActivity.m_bluetoothSocket!!.outputStream.write(input.toByteArray())
        } catch(e: IOException) {
            e.printStackTrace()
        }
    }
}
