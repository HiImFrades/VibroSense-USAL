package com.example.terminalbluetooth

//a

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.speech.tts.TextToSpeech
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.isInvisible
import com.google.android.gms.maps.MapsInitializer
import java.io.File
import java.io.IOException
import java.util.*


//b
const val REQUEST_ENABLE_BT = 1
const val fileName = "address.txt"

//-----------------------------------------------------------------> OnMapReadyCallback
class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    //c
    lateinit var mBtAdapter: BluetoothAdapter
    var mAddressDevices: ArrayAdapter<String>? = null
    var mNameDevices: ArrayAdapter<String>? = null
    var tts: TextToSpeech? = null

    val requestEnableBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // Bluetooth habilitado, realizar acción correspondiente
                ttsSpeak("Bluetooth activated")

                val idSpinDisp = findViewById<Spinner>(R.id.idSpinDisp)
                val idBtnConect = findViewById<Button>(R.id.idBtnConect)
                val idTextOut = findViewById<EditText>(R.id.idTextOut)
                val idBtnEnviar = findViewById<Button>(R.id.idBtnEnviar)
                val idBtnOn = findViewById<Button>(R.id.idBtnOn)

                val address = readFile()

                if(bluetoothConnect(address)==true){
                    idTextOut.isEnabled = true
                    idBtnEnviar.isEnabled = true
                    idBtnOn.isEnabled = true
                    idSpinDisp.isEnabled = false
                    idBtnConect.isEnabled = false
                }else{
                    idSpinDisp.isEnabled = true
                    idBtnConect.isEnabled = true
                    loadDevices()
                }

            } else {
                // Bluetooth no habilitado, realizar acción correspondiente
                ttsSpeak("Permission denied")
            }
        }

    companion object {
        var m_myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var m_bluetoothSocket: BluetoothSocket? = null

        var m_isConnected: Boolean = false
        lateinit var m_address: String

        var flagAlarma: Boolean = true
        var flagBateria: Boolean = true
        var flagPower: Boolean = true
        var flagLlamada: Boolean = true
        var flagSMS: Boolean = true
        var flagWhatsapp: Boolean = true
        var flagGmail: Boolean = true

    }

    @SuppressLint("SuspiciousIndentation", "MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST) {
            //println(it.name)
        }
        setContentView(R.layout.activity_main)

        //d

        tts = TextToSpeech(this, this)

        mAddressDevices = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        mNameDevices = ArrayAdapter(this, android.R.layout.simple_list_item_1)


        val idBtnOnBT = findViewById<Button>(R.id.idBtnOnBT)
        val idSpinDisp = findViewById<Spinner>(R.id.idSpinDisp)
        val idBtnConect = findViewById<Button>(R.id.idBtnConect)
        val idTextOut = findViewById<EditText>(R.id.idTextOut)
        val idBtnEnviar = findViewById<Button>(R.id.idBtnEnviar)
        val idBtnOn = findViewById<Button>(R.id.idBtnOn)
        val idBtnOff = findViewById<Button>(R.id.idBtnOff)
        val idBtnPopUp = findViewById<ImageButton>(R.id.idBtnPopUp)

        idSpinDisp.isEnabled = false
        idBtnConect.isEnabled = false
        idTextOut.isEnabled = false
        idBtnEnviar.isEnabled = false
        idBtnOn.isEnabled = false
        idBtnOff.isEnabled = false

        var requestBluetooth = registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                //granted
            } else {
                //deny
            }
        }

        val requestMultiplePermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                permissions.entries.forEach {
                    Log.d("test006", "${it.key} = ${it.value}")
                }
            }

        val someActivityResultLauncher = registerForActivityResult(
            StartActivityForResult()
        ) { result ->
            if (result.resultCode == REQUEST_ENABLE_BT) {
                Log.i("MainActivity", "ACTIVIDAD REGISTRADA")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE
                )
            )
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(enableBtIntent)
        }

        if (!NotificationManagerCompat.getEnabledListenerPackages(this).contains(this.packageName)) {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
        }

        //====================================================================//
        //Registrar el listener de notificaciones
        val serviceComponent = ComponentName(this, Listener::class.java)
        startService(Intent().setComponent(serviceComponent))

        // Registrar el receptor de la alarma
        val alarmMgr = getSystemService(ALARM_SERVICE) as AlarmManager
        val intentAlarm = Intent(this, AlarmReceiver::class.java)
        val alarmIntent = PendingIntent.getBroadcast(this, 0, intentAlarm, PendingIntent.FLAG_IMMUTABLE)

        // Buscar la próxima alarma y registrarla
        if (alarmMgr.nextAlarmClock != null)
            alarmMgr.setAlarmClock(alarmMgr.nextAlarmClock, alarmIntent)

        // Registrar el recepctor de las llamadas
        val callReceiver: BroadcastReceiver = CallReceiver()
        val callFilter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)

        //Registrar el receptor de la bateria baja
        val lowBattery: BroadcastReceiver = LowBattery()
        val batFilter = IntentFilter(Intent.ACTION_BATTERY_LOW)

        //Registrar el receptor de la bateria
        val powerOn: BroadcastReceiver = PowerOn()
        val powFilter = IntentFilter(Intent.ACTION_POWER_CONNECTED)

        //Registrar el receptor de SMS
        val smsReceiver: BroadcastReceiver = SMSReceiver()
        val smsFilter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)

        // Registrar actividad de la llamada
        registerReceiver(callReceiver, callFilter)

        // Registrar actividad de la batería baja
        registerReceiver(lowBattery, batFilter)

        // Registrar actividad de la batería
        registerReceiver(powerOn, powFilter)

        // Registrar actividad de los sms
        registerReceiver(smsReceiver, smsFilter)

        //====================================================================//

        //Inicializacion del bluetooth adapter
        this.mBtAdapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter

        //Comprobar si esta disponible o no
        if (mBtAdapter == null) {
            Toast.makeText(
                this,
                "Bluetooth no está disponible en este dipositivo",Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Bluetooth está disponible en este dispositivo", Toast.LENGTH_LONG)
                .show()
        }

        if (mBtAdapter.isEnabled) {
            //Si ya está activado
            //leer fichero
            val address = readFile()
            if (address != ""){
                if(bluetoothConnect(address) == true){
                    idTextOut.isEnabled = true
                    idBtnEnviar.isEnabled = true
                    idBtnOn.isEnabled = true
                    idSpinDisp.isEnabled = false
                    idBtnConect.isEnabled = false
                }else{
                    idSpinDisp.isEnabled = true
                    idBtnConect.isEnabled = true
                    loadDevices()
                }
            }else{
                idSpinDisp.isEnabled = true
                idBtnConect.isEnabled = true
                loadDevices()
            }
        }

        //Boton Encender bluetooth
        idBtnOnBT.setOnClickListener {
            if (mBtAdapter.isEnabled) {
                //Si ya está activado
                Toast.makeText(this, "Bluetooth ya se encuentra activado", Toast.LENGTH_LONG).show()
            } else {
                //Encender Bluetooth
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                requestEnableBluetooth.launch(enableBtIntent)
            }
        }

        idBtnConect.setOnClickListener {
            if(idSpinDisp.isEnabled == true){
                val IntValSpin = idSpinDisp.selectedItemPosition
                m_address = mAddressDevices!!.getItem(IntValSpin).toString()
                if(bluetoothConnect(m_address)==true){
                    idTextOut.isEnabled = true
                    idBtnEnviar.isEnabled = true
                    idBtnOn.isEnabled = true
                }
            }
        }

        idBtnEnviar.setOnClickListener {
            if (idTextOut.text.toString().isEmpty()) {
                Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT)
            } else {
                var mensaje_out: String = idTextOut.text.toString()
                sendCommand(mensaje_out)
            }
        }

        idBtnOn.setOnClickListener {
            sendCommand("Encender")
            idBtnOff.isEnabled = true
            idBtnOn.isEnabled = false
            ttsSpeak("Distance sensor turned on")
        }

        idBtnOff.setOnClickListener {
            sendCommand("Apagar")
            idBtnOff.isEnabled = false
            idBtnOn.isEnabled = true
            ttsSpeak("Distance sensor turned off")
        }

        idBtnPopUp.setOnClickListener {
            val popUpView =  LayoutInflater.from(applicationContext).inflate(R.layout.popup_layout,null,false);
            val popupWindow = PopupWindow(popUpView, 1000, 1600, false)
            popupWindow.showAtLocation(idBtnPopUp, Gravity.CENTER, 0, 0)

            popUpView.setOnClickListener { popupWindow.dismiss() }

            val idSwitchAlarma = popUpView.findViewById<Switch>(R.id.idSwitchAlarma)
            val idSwitchLlamada = popUpView.findViewById<Switch>(R.id.idSwitchLlamada)
            val idSwitchBateria = popUpView.findViewById<Switch>(R.id.idSwitchBateria)
            val idSwitchPower = popUpView.findViewById<Switch>(R.id.idSwitchPower)
            val idSwitchWhatsapp = popUpView.findViewById<Switch>(R.id.idSwitchWhatsapp)
            val idSwitchGmail = popUpView.findViewById<Switch>(R.id.idSwitchGmail)
            val idSwitchSMS = popUpView.findViewById<Switch>(R.id.idSwitchSMS)

            idSwitchAlarma.isChecked = flagAlarma
            idSwitchLlamada.isChecked = flagLlamada
            idSwitchBateria.isChecked = flagBateria
            idSwitchPower.isChecked = flagPower
            idSwitchWhatsapp.isChecked = flagWhatsapp
            idSwitchGmail.isChecked = flagGmail
            idSwitchSMS.isChecked = flagSMS

            idSwitchAlarma.setOnCheckedChangeListener { _, isChecked ->
                flagAlarma = isChecked
            }

            idSwitchLlamada.setOnCheckedChangeListener { _, isChecked ->
                flagLlamada = isChecked
            }

            idSwitchBateria.setOnCheckedChangeListener { _, isChecked ->
                flagBateria = isChecked
            }

            idSwitchPower.setOnCheckedChangeListener { _, isChecked ->
                flagPower = isChecked
            }

            idSwitchWhatsapp.setOnCheckedChangeListener { _, isChecked ->
                flagWhatsapp = isChecked
            }

            idSwitchGmail.setOnCheckedChangeListener { _, isChecked ->
                flagGmail = isChecked
            }

            idSwitchSMS.setOnCheckedChangeListener { _, isChecked ->
                flagSMS = isChecked
            }
        }
    }

    override fun onInit(p0: Int) {
        if (p0 == TextToSpeech.SUCCESS) {
            var output = tts!!.setLanguage(Locale.ENGLISH)
            if (output == TextToSpeech.LANG_MISSING_DATA || output == TextToSpeech.LANG_NOT_SUPPORTED)
                Log.e("TTS", "No hay soporte para este idioma.")
        } else {
            Log.e("TTS", "Fallo al iniciar el speaker.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
    }

    //MÉTODO DE COMPROBACIONES CUANDO VOLVEMOS A ENTRAR EL LA APLICACIÓN DESDE EL MODO BACKGROUND
    override fun onRestart() {
        super.onRestart()
        Toast.makeText(this, "HOLA DE NUEVO", Toast.LENGTH_LONG).show()

        /*val idBtnDispBT = findViewById<Button>(R.id.idBtnDispBT)
        val idSpinDisp = findViewById<Spinner>(R.id.idSpinDisp)
        val idBtnConect = findViewById<Button>(R.id.idBtnConect)
        val idTextOut = findViewById<EditText>(R.id.idTextOut)
        val idBtnEnviar = findViewById<Button>(R.id.idBtnEnviar)
        val idBtnOn = findViewById<Button>(R.id.idBtnOn)
        val idBtnOff = findViewById<Button>(R.id.idBtnOff)*/

        var requestBluetooth = registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                //granted
            } else {
                //deny
            }
        }

        val requestMultiplePermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                permissions.entries.forEach {
                    Log.d("test006", "${it.key} = ${it.value}")
                }
            }

        val someActivityResultLauncher = registerForActivityResult(
            StartActivityForResult()
        ) { result ->
            if (result.resultCode == REQUEST_ENABLE_BT) {
                Log.i("MainActivity", "ACTIVIDAD REGISTRADA")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.READ_PHONE_STATE
                )
            )
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(enableBtIntent)
        }

        // Registrar el receptor de la alarma
        val alarmMgr = getSystemService(ALARM_SERVICE) as AlarmManager
        val intentAlarm = Intent(this, AlarmReceiver::class.java)
        val alarmIntent = PendingIntent.getBroadcast(this, 0, intentAlarm, PendingIntent.FLAG_IMMUTABLE)

        // Buscar la próxima alarma y registrarla
        if (alarmMgr.nextAlarmClock != null)
            alarmMgr.setAlarmClock(alarmMgr.nextAlarmClock, alarmIntent)
    }

    //e
    fun sendCommand(input: String) {
        if (m_bluetoothSocket != null) {
            try {
                m_bluetoothSocket!!.outputStream.write(input.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun ttsSpeak(cadena: String) {
        tts!!.speak(cadena, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun bluetoothConnect(address: String): Boolean{
        if(address!=""){
            try {
                if (m_bluetoothSocket == null || !m_isConnected) {
                    m_address = address
                    Toast.makeText(this, m_address, Toast.LENGTH_LONG).show()
                    // Cancel discovery because it otherwise slows down the connection.
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_SCAN
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return false
                    }
                    mBtAdapter?.cancelDiscovery()
                    val device: BluetoothDevice = mBtAdapter.getRemoteDevice(m_address)
                    m_bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(m_myUUID)
                    m_bluetoothSocket!!.connect()

                    Toast.makeText(this, "CONEXION EXITOSA", Toast.LENGTH_LONG).show()
                    Log.i("MainActivity", "CONEXION EXITOSA")

                    ttsSpeak("Connected to " + mBtAdapter.getRemoteDevice(m_address).name)
                    writeFile(m_address)

                    return true
                }

            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "ERROR DE CONEXION", Toast.LENGTH_LONG).show()
                Log.i("MainActivity", "ERROR DE CONEXION")
                ttsSpeak("Connection refused, please restart the application")
                writeFile("")

                return false
            }
        }
        return false
    }

    @SuppressLint("MissingPermission")
    private fun loadDevices(){
        val idSpinDisp = findViewById<Spinner>(R.id.idSpinDisp)
        if (mBtAdapter.isEnabled) {
            val pairedDevices: Set<BluetoothDevice>? = mBtAdapter?.bondedDevices
            mAddressDevices!!.clear()
            mNameDevices!!.clear()

            pairedDevices?.forEach { device ->
                val deviceName = device.name
                val deviceHardwareAddress = device.address // MAC address
                mAddressDevices!!.add(deviceHardwareAddress)
                //........... EN ESTE PUNTO GUARDO LOS NOMBRE A MOSTRARSE EN EL COMBO BOX
                mNameDevices!!.add(deviceName)
            }
            //ACTUALIZO LOS DISPOSITIVOS
            idSpinDisp.setAdapter(mNameDevices)
        } else {
            val noDevices = "Ningun dispositivo pudo ser emparejado"
            mAddressDevices!!.add(noDevices)
            mNameDevices!!.add(noDevices)
            Toast.makeText(this, "Primero vincule un dispositivo bluetooth", Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun writeFile(address: String){
        this.openFileOutput(fileName, MODE_PRIVATE).use { output ->
            output.write(address.toByteArray())
        }

    }

    private fun readFile(): String{
        try{
            this.openFileInput(fileName).use { stream ->
                val texto = stream.bufferedReader().use {
                    it.readText()
                }
                Toast.makeText(this, texto, Toast.LENGTH_LONG).show()
                if(texto!=""){
                    return texto
                }
            }
        }catch (e: IOException){
            ttsSpeak("File address.txt doesn't exists")
        }
        return ""
    }
}
