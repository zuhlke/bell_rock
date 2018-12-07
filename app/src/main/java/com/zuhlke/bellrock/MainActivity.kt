package com.zuhlke.bellrock

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import com.google.android.things.pio.UartDevice
import com.google.android.things.pio.UartDeviceCallback
import java.io.IOException
import java.lang.StringBuilder

class MainActivity : Activity() {
    companion object {

        const val TAG = "SMSStuff"
        const val UART_DEVICE_NAME = "MINIUART"
        const val BUFFER = 128

        const val GPIO_NAME = "BCM25"
    }

    private var uartDevice: UartDevice? = null
    private var gpio25: Gpio? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val manager = PeripheralManager.getInstance()
        val deviceList: List<String> = manager.uartDeviceList
        if (deviceList.isEmpty()) {
            android.util.Log.i(TAG, "No UART port available on this uartDevice.")
        } else {
            android.util.Log.i(TAG, "List of available devices: $deviceList")
            connectGSM()
        }

        val portList: List<String> = manager.gpioList
        if (portList.isEmpty()) {
            android.util.Log.i(TAG, "No GPIO port available on this device.")
        } else {
            android.util.Log.i(TAG, "List of available ports: $portList")
        }

        connectGPIO()
    }

    private fun connectGPIO() {
        gpio25 = try {
            PeripheralManager.getInstance().openGpio(GPIO_NAME)
        } catch (e: IOException) {
            android.util.Log.w(TAG, "Unable to access GPIO", e)
            null
        }

        gpio25!!.apply {
            // Initialize the pin as a high output
            setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH)
            // Low voltage is considered active
            setActiveType(Gpio.ACTIVE_LOW)
            // Toggle the value to be LOW
            value = true
        }
    }

    private fun connectGSM() {
        uartDevice = try {
            PeripheralManager.getInstance().openUartDevice(UART_DEVICE_NAME)
        } catch (e: IOException) {
            android.util.Log.w(TAG, "Unable to access UART uartDevice", e)
            null
        }

        uartDevice?.apply {
            registerUartDeviceCallback(uartCallback)
            android.util.Log.d(TAG, "TEXT MODE")
            writeUartData(AT_COMMANDS.TEXT_MODE)
        }
    }

    private fun readSMS() {
        android.util.Log.d(TAG, "READ SMS")
        uartDevice!!.writeUartData(AT_COMMANDS.READ_SMS)
    }

    private fun clearSMS() {
        android.util.Log.d(TAG, "CLEAR SMS")
        uartDevice!!.writeUartData(AT_COMMANDS.CLEAR_SMS)
    }

    private fun processSMS(data: String) {
        android.util.Log.d(TAG, "SMS PROCESSED!")
        if (data.contains("REC UNREAD")) {
            gpio25?.value = !gpio25!!.value
            clearSMS()
        }
    }

    private fun loopRead() {
        readSMS()
        Handler().postDelayed({loopRead()}, 1000)
    }

    private fun processResult(data: String) {
        android.util.Log.d(TAG, "From uartDevice:\r\n$data\r\n")

        if (data.contains(AT_RESPONSES.TEXT_MODE)) {
            loopRead()
            //readSMS()
        }

        if (data.contains(AT_RESPONSES.READ_SMS)) {
            if (data.contains(AT_RESPONSES.OK)) {
                processSMS(data)
            }
        }
    }


    @Throws(IOException::class)
    fun UartDevice.writeUartData(data: String) {
        run {
            data.toByteArray().let { buffer ->
                write(buffer, buffer.size)
            }
        }
    }

    @Throws(IOException::class)
    fun readUartBuffer(uart: UartDevice) {
        uart.apply {
            val output = StringBuilder()
            val buffer = ByteArray(BUFFER)
            var count: Int = read(buffer, buffer.size)
            output.append(String(buffer).substring(0, count))
            while (count > 0) {
                //android.util.Log.d(TAG, "Read $count bytes from peripheral")
                count = read(buffer, buffer.size)
                output.append(String(buffer).substring(0, count))
            }
            processResult(output.toString())
        }
    }

    private val uartCallback = object : UartDeviceCallback {
        override fun onUartDeviceDataAvailable(uart: UartDevice): Boolean {
            // Read available data from the UART uartDevice
            try {
                readUartBuffer(uart)
            } catch (e: IOException) {
                android.util.Log.w(TAG, "Unable to access UART uartDevice", e)
            }

            // Continue listening for more interrupts
            return true
        }

        override fun onUartDeviceError(uart: UartDevice?, error: Int) {
            android.util.Log.w(TAG, "$uart: Error event $error")
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        try {
            uartDevice?.unregisterUartDeviceCallback(uartCallback)
            uartDevice?.close()
            uartDevice = null
        } catch (e: IOException) {
            android.util.Log.w(TAG, "Unable to close UART uartDevice", e)
        }

        try {
            gpio25?.close()
            gpio25 = null
        } catch (e: IOException) {
            android.util.Log.w(TAG, "Unable to close GPIO", e)
        }
    }
}
