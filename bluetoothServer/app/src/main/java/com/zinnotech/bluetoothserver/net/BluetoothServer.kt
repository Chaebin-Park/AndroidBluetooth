package com.zinnotech.bluetoothserver.net

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

class BluetoothServer(activity: Activity) {
    private var btManager = activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private var btAdapter = btManager.adapter
    private var acceptThread: AcceptThread? = null
    private var commThread: CommThread? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var socketListener: SocketListener? = null

    fun setOnSocketListener(listener: SocketListener?) {
        socketListener = listener
    }

    fun onConnect() {
        socketListener?.onConnect()
    }

    fun onDisconnect() {
        socketListener?.onDisconnect()
    }

    fun onLogPrint(message: String?) {
        socketListener?.onLogPrint(message)
    }

    fun onError(e: Exception) {
        socketListener?.onError(e)
    }

    fun onReceive(msg: String) {
        socketListener?.onReceive(msg)
    }

    fun onSend(msg: String) {
        socketListener?.onSend(msg)
    }

    fun accept() {
        stop()

        onLogPrint("Waiting for accept the client..")
        acceptThread = AcceptThread()
        acceptThread?.start()
    }

    fun stop() {
        if (acceptThread == null) return

        try {
            acceptThread?.let {
                onLogPrint("Stop accepting")

                it.stopThread()
                it.join(1000)
                it.interrupt()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    inner class AcceptThread : Thread() {
        private var acceptSocket: BluetoothServerSocket? = null
        private var socket: BluetoothSocket? = null

        override fun run() {
            while (true) {
                socket = try {
                    acceptSocket?.accept()
                } catch (e: Exception) {
                    e.printStackTrace()
                    break
                }

                if (socket != null) {
                    onConnect()

                    commThread = CommThread(socket)
                    commThread?.start()
                    break
                }
            }
        }

        fun stopThread() {
            try {
                acceptSocket?.close()
                socket?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        init {
            try {
                acceptSocket = btAdapter.listenUsingRfcommWithServiceRecord(
                    "bluetoothTest",
                    BTConstant.BLUETOOTH_UUID_SECURE
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    internal inner class CommThread(private val socket: BluetoothSocket?): Thread() {

        override fun run() {
            try {
                outputStream = socket?.outputStream
                inputStream = socket?.inputStream
            } catch (e: Exception) {
                e.printStackTrace()
            }

            var len: Int
            val buffer = ByteArray(1024)
            val byteArrayOutputStream = ByteArrayOutputStream()

            while (true) {
                try {
                    len = socket?.inputStream?.read(buffer)!!
                    val data = buffer.copyOf(len)
                    byteArrayOutputStream.write(data)

                    socket.inputStream?.available()?.let { available ->

                        if (available == 0) {
                            val dataByteArray = byteArrayOutputStream.toByteArray()
                            val dataString = String(dataByteArray)
                            onReceive(dataString)

                            byteArrayOutputStream.reset()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    stopThread()
                    accept()
                    break
                }
            }
        }

        fun stopThread() {
            try {
                inputStream?.close()
                outputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendData(msg: String) {
        if (outputStream == null) return

        try {
            outputStream?.let {
                onSend(msg)

                Log.e("LOG_TEST_SEND", msg)
                Log.e("LOG_TEST_SEND", "${msg.length}")

                it.write(msg.toByteArray())
                it.flush()
            }
        } catch (e: Exception) {
            onError(e)
            e.printStackTrace()
            stop()
        }
    }
}