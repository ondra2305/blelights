package cz.svobodao.emosblelights

import android.Manifest
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity() {
    private var bluetoothGatt: BluetoothGatt? = null
    private lateinit var macAddressField: EditText
    private lateinit var commandButton1: Button
    private lateinit var commandButton2: Button
    private lateinit var commandButton3: Button
    private val serviceUUID = UUID.fromString("00001000-0000-1000-8000-00805f9b34fb") // Replace with your service UUID
    private val characteristicUUID = UUID.fromString("00001001-0000-1000-8000-00805f9b34fb") // Replace with your characteristic UUID

    private var commandSendJob: Job? = null
    private val COMMAND_TIMEOUT_MS = 3000L

    private var pendingCommand: ByteArray? = null

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Connected", Toast.LENGTH_SHORT).show()
                }
                saveMacAddress(gatt.device.address)
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION),
                        1)
                    return
                }
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Disconnected or failed to connect", Toast.LENGTH_SHORT).show()
                }
                bluetoothGatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(serviceUUID)
                val characteristic = service?.getCharacteristic(characteristicUUID)
                characteristic?.let { char ->
                    pendingCommand?.let { command ->
                        char.value = command
                        if (ActivityCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            ActivityCompat.requestPermissions(
                                this@MainActivity,
                                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION),
                                1)
                        }
                        val writeSuccessful = gatt.writeCharacteristic(char)
                        if (!writeSuccessful) {
                            showToast("Failed to send command")
                        } else {
                            startCommandTimeout()
                        }
                        pendingCommand = null // Clear the command once written
                    }
                }
            } else {
                showToast("Failed to discover services: Error $status")
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            commandSendJob?.cancel()
            if (status == BluetoothGatt.GATT_SUCCESS) {
                showToast("Command sent successfully")
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION),
                        1)
                    return
                }
                bluetoothGatt?.close()
                bluetoothGatt?.disconnect()

            } else {
                showToast("Failed to send command: Error $status")
                bluetoothGatt?.close()
                bluetoothGatt?.disconnect()
            }
        }
    }

    private fun startCommandTimeout() {
        commandSendJob?.cancel() // Cancel any previous job
        commandSendJob = CoroutineScope(Dispatchers.Main).launch {
            delay(COMMAND_TIMEOUT_MS)
            showToast("Timeout")
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun isValidMacAddress(macAddress: String): Boolean {
        val macRegex = Regex("[0-9A-Fa-f]{2}(:[0-9A-Fa-f]{2}){5}")
        return macRegex.matches(macAddress)
    }

    private fun hexStringToByteArray(hexString: String): ByteArray {
        val length = hexString.length
        val data = ByteArray(length / 2)
        for (i in 0 until length step 2) {
            data[i / 2] = ((Character.digit(hexString[i], 16) shl 4) + Character.digit(hexString[i + 1], 16)).toByte()
        }
        return data
    }

    private fun saveMacAddress(macAddress: String) {
        val sharedPref = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("LastUsedMacAddress", macAddress)
            apply()
        }
    }

    private fun getSavedMacAddress(): String? {
        val sharedPref = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        return sharedPref.getString("LastUsedMacAddress", null)
    }

    private fun connectAndSendCommand(macAddress: String, command: String) {
        if (!isValidMacAddress(macAddress)) {
            showToast("Invalid MAC address, expected XX:XX:XX:XX:XX:XX")
            return
        }

        pendingCommand = hexStringToByteArray(command)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        val device = bluetoothAdapter?.getRemoteDevice(macAddress)
        if (device == null) {
            showToast("Failed to connect: Device not found")
            return
        }
        device.let {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION),
                    1)
                return
            }
            startCommandTimeout()
            bluetoothGatt = it.connectGatt(this, false, gattCallback)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        macAddressField = findViewById(R.id.macAddressField)
        commandButton1 = findViewById(R.id.commandButton1)
        commandButton2 = findViewById(R.id.commandButton2)
        commandButton3 = findViewById(R.id.commandButton3)

        getSavedMacAddress()?.let {
            macAddressField.setText(it)
        }

        commandButton1.setOnClickListener {
            val macAddress = macAddressField.text.toString()
            connectAndSendCommand(macAddress, "54511FE64A4348")
        }

        commandButton2.setOnClickListener {
            val macAddress = macAddressField.text.toString()
            connectAndSendCommand(macAddress, "54500EF7595A")
        }

        //TODO
        commandButton3.setOnClickListener {
            val macAddress = macAddressField.text.toString()
            connectAndSendCommand(macAddress, "54500EF7595A")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION),
                1)
            return
        }
        bluetoothGatt?.close()
        bluetoothGatt?.disconnect()
    }
}

