package com.zuhlke.bellrock

import android.os.AsyncTask
import com.google.android.things.pio.PeripheralManager
import com.google.android.things.pio.UartDevice
import com.google.android.things.pio.UartDeviceCallback
import java.io.IOException
import java.lang.StringBuilder


class SmsAsyncTask : AsyncTask<Unit, Unit, Unit>() {
    companion object {
        const val TAG = "SimService"
        const val UART_DEVICE_NAME = "MINIUART"
        const val BUFFER = 128
    }

    private var device: UartDevice? = null

    override fun doInBackground(vararg p0: Unit?) {
        val manager = PeripheralManager.getInstance()
        val deviceList: List<String> = manager.uartDeviceList
        if (deviceList.isEmpty()) {
            android.util.Log.i(TAG, "No UART port available on this device.")
        } else {
            android.util.Log.i(TAG, "List of available devices: $deviceList")
            connect()
        }
    }

    private fun connect() {
        device = try {
            PeripheralManager.getInstance().openUartDevice(UART_DEVICE_NAME)
        } catch (e: IOException) {
            android.util.Log.w(TAG, "Unable to access UART device", e)
            null
        }

        device?.apply {
            registerUartDeviceCallback(uartCallback)
            android.util.Log.d(TAG, "TEXT MODE")
            writeUartData(AT_COMMANDS.TEXT_MODE)
        }
    }

    private fun readSMS() {
        android.util.Log.d(TAG, "READ SMS")
        device!!.writeUartData(AT_COMMANDS.READ_SMS)
    }

    private fun clearSMS() {
        android.util.Log.d(TAG, "CLEAR SMS")
        //device?.writeUartData(AT_COMMANDS.CLEAR_SMS)
    }

    private fun processSMS(data: String) {
        android.util.Log.d(TAG, "SMS PROCESSED!")
        clearSMS()
    }

    private fun processResult(data: String) {
        android.util.Log.d(TAG, "From device:\r\n$data\r\n")

        if (data.contains(AT_RESPONSES.TEXT_MODE)) {
            //loopRead()
            readSMS()
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
            // Read available data from the UART device
            try {
                readUartBuffer(uart)
            } catch (e: IOException) {
                android.util.Log.w(TAG, "Unable to access UART device", e)
            }

            // Continue listening for more interrupts
            return true
        }

        override fun onUartDeviceError(uart: UartDevice?, error: Int) {
            android.util.Log.w(TAG, "$uart: Error event $error")
        }
    }


    override fun onCancelled() {
        super.onCancelled()

        try {
            device?.unregisterUartDeviceCallback(uartCallback)
            device?.close()
            device = null
        } catch (e: IOException) {
            android.util.Log.w(TAG, "Unable to close UART device", e)
        }
    }
}