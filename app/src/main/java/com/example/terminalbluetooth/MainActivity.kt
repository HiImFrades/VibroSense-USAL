package com.example.terminalbluetooth

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
import android.content.res.ColorStateList
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
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.*

const val fileName = "address.txt"

/**
 * Clase principal de la aplicación.
 */
class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    lateinit var mBtAdapter: BluetoothAdapter
    var mAddressDevices: ArrayAdapter<String>? = null
    var mNameDevices: ArrayAdapter<String>? = null
    var tts: TextToSpeech? = null

    val requestEnableBluetooth =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                ttsSpeak("Bluetooth activated")

                val idSpinDisp = findViewById<Spinner>(R.id.idSpinDisp)
                val idBtnConect = findViewById<Button>(R.id.idBtnConect)
                val idBtnOn = findViewById<Button>(R.id.idBtnOn)

                val address = readFile()

                if(bluetoothConnect(address)==true){
                    idBtnOn.isEnabled = true
                    idSpinDisp.isEnabled = false
                    idBtnConect.isEnabled = false
                }else{
                    idSpinDisp.isEnabled = true
                    idBtnConect.isEnabled = true
                    loadDevices()
                }

            } else {
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

    /**
     * Función que se ejecuta al crearse la clase MainActivity.
     *
     * Esta función compone el main del programa.
     *
     * @param savedInstanceState parámetro de configuración.
     */
    @SuppressLint("SuspiciousIndentation", "MissingPermission", "UseSwitchCompatOrMaterialCode")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        tts = TextToSpeech(this, this)

        mAddressDevices = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        mNameDevices = ArrayAdapter(this, android.R.layout.simple_list_item_1)

        val idBtnOnBT = findViewById<Button>(R.id.idBtnOnBT)
        val idSpinDisp = findViewById<Spinner>(R.id.idSpinDisp)
        val idBtnConect = findViewById<Button>(R.id.idBtnConect)
        val idBtnOn = findViewById<Button>(R.id.idBtnOn)
        val idBtnOff = findViewById<Button>(R.id.idBtnOff)
        val idBtnPopUp = findViewById<ImageButton>(R.id.idBtnPopUp)

        idSpinDisp.isEnabled = false
        idBtnConect.isEnabled = false
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.RECEIVE_SMS,
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

        if (mBtAdapter.isEnabled) {
            val address = readFile()
            if (address != ""){
                if(bluetoothConnect(address) == true){
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
                Toast.makeText(this, "Bluetooth ya se encuentra activado", Toast.LENGTH_LONG).show()
            } else {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                requestEnableBluetooth.launch(enableBtIntent)
            }
        }

        //Botón Conectar
        idBtnConect.setOnClickListener {
            if(idSpinDisp.isEnabled == true){
                val IntValSpin = idSpinDisp.selectedItemPosition
                m_address = mAddressDevices!!.getItem(IntValSpin).toString()
                if(bluetoothConnect(m_address)==true){
                    idBtnOn.isEnabled = true
                }
            }
        }

        //Botón encendido
        idBtnOn.setOnClickListener {
            sendCommand("Encender")
            idBtnOff.isEnabled = true
            idBtnOn.isEnabled = false
            ttsSpeak("Distance sensor turned on")
        }

        //Botón apagado
        idBtnOff.setOnClickListener {
            sendCommand("Apagar")
            idBtnOff.isEnabled = false
            idBtnOn.isEnabled = true
            ttsSpeak("Distance sensor turned off")
        }

        //Botón de ajustes
        idBtnPopUp.setOnClickListener {
            val popUpView =  LayoutInflater.from(applicationContext).inflate(R.layout.popup_layout,null,false);
            val popupWindow = PopupWindow(popUpView, 1000, 1600, false)
            popupWindow.showAtLocation(idBtnPopUp, Gravity.TOP, 0, 0)

            popUpView.setOnClickListener { popupWindow.dismiss() }

            val idSwitchAlarma = popUpView.findViewById<Switch>(R.id.idSwitchAlarma)
            val idSwitchLlamada = popUpView.findViewById<Switch>(R.id.idSwitchLlamada)
            val idSwitchBateria = popUpView.findViewById<Switch>(R.id.idSwitchBateria)
            val idSwitchPower = popUpView.findViewById<Switch>(R.id.idSwitchPower)
            val idSwitchWhatsapp = popUpView.findViewById<Switch>(R.id.idSwitchWhatsapp)
            val idSwitchGmail = popUpView.findViewById<Switch>(R.id.idSwitchGmail)
            val idSwitchSMS = popUpView.findViewById<Switch>(R.id.idSwitchSMS)

            val thumbActiveColor = ContextCompat.getColor(this, R.color.activeThumb)
            val thumbInactiveColor = ContextCompat.getColor(this, R.color.inactiveThumb)
            val trackActiveColor = ContextCompat.getColor(this, R.color.activeTrack)
            val trackInactiveColor = ContextCompat.getColor(this, R.color.inactiveTrack)

            idSwitchAlarma.isChecked = flagAlarma
            if (idSwitchAlarma.isChecked) switchColor(thumbActiveColor, trackActiveColor, idSwitchAlarma)
            idSwitchLlamada.isChecked = flagLlamada
            if (idSwitchLlamada.isChecked) switchColor(thumbActiveColor, trackActiveColor, idSwitchLlamada)
            idSwitchBateria.isChecked = flagBateria
            if (idSwitchBateria.isChecked) switchColor(thumbActiveColor, trackActiveColor, idSwitchBateria)
            idSwitchPower.isChecked = flagPower
            if (idSwitchPower.isChecked) switchColor(thumbActiveColor, trackActiveColor, idSwitchPower)
            idSwitchWhatsapp.isChecked = flagWhatsapp
            if (idSwitchWhatsapp.isChecked) switchColor(thumbActiveColor, trackActiveColor, idSwitchWhatsapp)
            idSwitchGmail.isChecked = flagGmail
            if (idSwitchGmail.isChecked) switchColor(thumbActiveColor, trackActiveColor, idSwitchGmail)
            idSwitchSMS.isChecked = flagSMS
            if (idSwitchSMS.isChecked) switchColor(thumbActiveColor, trackActiveColor, idSwitchSMS)

            idSwitchAlarma.setOnCheckedChangeListener { _, isChecked ->
                flagAlarma = isChecked
                if (isChecked) switchColor(thumbActiveColor, trackActiveColor, idSwitchAlarma)
                else switchColor(thumbInactiveColor, trackInactiveColor, idSwitchAlarma)
            }

            idSwitchLlamada.setOnCheckedChangeListener { _, isChecked ->
                flagLlamada = isChecked
                if (isChecked) switchColor(thumbActiveColor, trackActiveColor, idSwitchLlamada)
                else switchColor(thumbInactiveColor, trackInactiveColor, idSwitchLlamada)
            }

            idSwitchBateria.setOnCheckedChangeListener { _, isChecked ->
                flagBateria = isChecked
                if (isChecked) switchColor(thumbActiveColor, trackActiveColor, idSwitchBateria)
                else switchColor(thumbInactiveColor, trackInactiveColor, idSwitchBateria)
            }

            idSwitchPower.setOnCheckedChangeListener { _, isChecked ->
                flagPower = isChecked
                if (isChecked) switchColor(thumbActiveColor, trackActiveColor, idSwitchPower)
                else switchColor(thumbInactiveColor, trackInactiveColor, idSwitchPower)
            }

            idSwitchWhatsapp.setOnCheckedChangeListener { _, isChecked ->
                flagWhatsapp = isChecked
                if (isChecked) switchColor(thumbActiveColor, trackActiveColor, idSwitchWhatsapp)
                else switchColor(thumbInactiveColor, trackInactiveColor, idSwitchWhatsapp)
            }

            idSwitchGmail.setOnCheckedChangeListener { _, isChecked ->
                flagGmail = isChecked
                if (isChecked) switchColor(thumbActiveColor, trackActiveColor, idSwitchGmail)
                else switchColor(thumbInactiveColor, trackInactiveColor, idSwitchGmail)
            }

            idSwitchSMS.setOnCheckedChangeListener { _, isChecked ->
                flagSMS = isChecked
                if (isChecked) switchColor(thumbActiveColor, trackActiveColor, idSwitchSMS)
                else switchColor(thumbInactiveColor, trackInactiveColor, idSwitchSMS)
            }
        }
    }

    /**
     * Función que se ejecuta al iniciar la aplicación.
     *
     * La función inicializará el objeto TextToSpeech,
     * el cuál servirá para introducir texto y se reproduzca una voz leyendo este texto.
     *
     * @param p0 es una variable de control
     */
    override fun onInit(p0: Int) {
        if (p0 == TextToSpeech.SUCCESS) {
            var output = tts!!.setLanguage(Locale.ENGLISH)
            if (output == TextToSpeech.LANG_MISSING_DATA || output == TextToSpeech.LANG_NOT_SUPPORTED)
                Log.e("TTS", "No hay soporte para este idioma.")
        } else {
            Log.e("TTS", "Fallo al iniciar el speaker.")
        }
    }

    /**
     * Función que se ejecuta al cerrar la aplicación.
     *
     * La función cerrará correctamente el objeto TextToSpeech.
     *
     */
    override fun onDestroy() {
        super.onDestroy()
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
    }

    /**
     * Función que se ejecuta al volver a la aplicación desde el segundo plano.
     *
     * La función buscará si se ha añadido alguna alarma nueva al dispositivo
     * para registrarla en la aplicación.
     *
     */
    override fun onRestart() {
        super.onRestart()
        // Registrar el receptor de la alarma
        val alarmMgr = getSystemService(ALARM_SERVICE) as AlarmManager
        val intentAlarm = Intent(this, AlarmReceiver::class.java)
        val alarmIntent = PendingIntent.getBroadcast(this, 0, intentAlarm, PendingIntent.FLAG_IMMUTABLE)

        // Buscar la próxima alarma y registrarla
        if (alarmMgr.nextAlarmClock != null)
            alarmMgr.setAlarmClock(alarmMgr.nextAlarmClock, alarmIntent)
    }

    /**
     * Función del TextToSpeech.
     *
     * A esta función se le pasa una String la cual, con un método
     * de la clase TextToSpeech, se convierte de texto a audio y se reproduce.
     *
     * @param cadena string a convertir de texto a audio.
     */
    private fun ttsSpeak(cadena: String) {
        tts!!.speak(cadena, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    /**
     * Función para enviar String al equipo externo.
     *
     * A esta función se le pasa una String que, a través del socket
     * del módulo Bluetooth del dispositivo, se le envía al equipo externo.
     *
     * @param input
     */
    fun sendCommand(input: String) {
        if (m_bluetoothSocket != null) {
            try {
                m_bluetoothSocket!!.outputStream.write(input.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Función para realizar la conexión Bluetooth.
     *
     * A esta función se le pasa una String, la cual es una dirección MAC, a la
     * que el socket del módulo Bluetooth del dispositivo se intentará conectar.
     * Devuelve un Boolean que representa si se ha tenido éxito o no en la conexión.
     *
     * @param address dirección MAC a la que conectarse.
     * @return true o false si ha habido éxito o no.
     */
    private fun bluetoothConnect(address: String): Boolean{
        if(address!=""){
            Toast.makeText(this, "Connecting to: $address ...", Toast.LENGTH_LONG).show()
            try {
                if (m_bluetoothSocket == null || !m_isConnected) {
                    m_address = address
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

    /**
     * Función para cargar los dispositivos en el desplegable de la lista principal.
     *
     * Esta función obtiene la lista de dispositivos vinculados por Bluetooth
     * del sistema y la carga en el desplegable de la vista principal, pudiendo seleccionar
     * una MAC concreta para futuras conexiones.
     *
     */
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
                mNameDevices!!.add(deviceName)
            }
            idSpinDisp.setAdapter(mNameDevices)
        } else {
            val noDevices = "Ningun dispositivo pudo ser emparejado"
            mAddressDevices!!.add(noDevices)
            mNameDevices!!.add(noDevices)
            Toast.makeText(this, "Primero vincule un dispositivo bluetooth", Toast.LENGTH_LONG)
                .show()
        }
    }

    /**
     * Función para escribir en el fichero interno "address.txt".
     *
     * A esta función se le pasa una String (normalmente una dirección),
     * la cual se almacenará en un fichero interno de la aplicación
     * llamado "address.txt".
     *
     * @param address dirección MAC a guardar en el fichero.
     */
    private fun writeFile(address: String){
        this.openFileOutput(fileName, MODE_PRIVATE).use { output ->
            output.write(address.toByteArray())
        }

    }

    /**
     * Función para leer el fichero interno "address.txt".
     *
     * Esta función lee una String (normalmente una dirección) del fichero
     * interno de la aplicación llamado "address.txt", la cual la devolverá
     * en el valor de retorno.
     *
     * @return devuelve la cadena en caso de éxito, una cadena vacía si hay fallo.
     */
    private fun readFile(): String{
        try{
            this.openFileInput(fileName).use { stream ->
                val texto = stream.bufferedReader().use {
                    it.readText()
                }
                if(texto!=""){
                    return texto
                }
            }
        }catch (e: IOException){
            ttsSpeak("File address.txt doesn't exists")
        }
        return ""
    }

    /**
     * Función para cambiar de color los interruptores.
     *
     * Esta función cambia el color de los interruptores al
     * activarlos o desactivarlos, pasandole los colores nuevos
     * y el identificador del interruptor que se va a cambiar el color.
     *
     * @param thumbColor Integer del color del thumb del switch.
     * @param trackColor Integer del color del track del switch.
     * @param switch Identificador del switch a cambiar.
     */
    private fun switchColor(thumbColor: Int, trackColor: Int, switch: Switch){
        switch.thumbTintList = ColorStateList.valueOf(thumbColor)
        switch.trackTintList = ColorStateList.valueOf(trackColor)
    }
}
