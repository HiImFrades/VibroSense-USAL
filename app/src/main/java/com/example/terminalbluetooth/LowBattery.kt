package com.example.terminalbluetooth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.io.IOException

class LowBattery : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if(MainActivity.flagBateria)
            sendLowBattery("Bateria")
    }
}

fun sendLowBattery(input: String) {
    if (MainActivity.m_bluetoothSocket != null) {
        try{
            MainActivity.m_bluetoothSocket!!.outputStream.write(input.toByteArray())
        } catch(e: IOException) {
            e.printStackTrace()
        }
    }
}