package com.example.roy.mybtdual_2

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.content_bt_app.*
import java.io.IOException

class btAppActivity : AppCompatActivity() {

    var maxBtDevice = 0
    var isBtBind = false
    lateinit var btService: Messenger
    private var clientMsgHandler = Messenger(ClientMsgHandler())
    inner class ClientMsgHandler : Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            var clientMsg: Message
            var clientBundle: Bundle

            if(msg != null)
            {
                Log.d(KotlinLog, "client App message: ${msg.what}, arg1: ${msg.arg1}, arg2: ${msg.arg2}")
                clientBundle = msg.data
                when(msg.what)
                {
                    0 -> {
                    }
                    1 -> {
                        setResult(101)
                        finish()
                    }
                    2 -> {
                        clientBundle = msg.data
                        val rfcCmd = clientBundle.getByteArray("rfcCmd")
                        rfcCmdParse(rfcCmd, msg.arg1)
                    }
                    else -> Log.d(KotlinLog, "client App message other")
                }
            }
        }
    }

    val serviceConn = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val getFwCmd = byteArrayOf(0x40, 0 ,0x00)
            val getNameCmd = byteArrayOf(0x48, 0 ,0x00)
            val msg = Message.obtain(null, 100, 0, 0)

            Log.d(KotlinLog, "onServiceConnected")
            btService = Messenger(service)
            isBtBind = true

            msg.replyTo = clientMsgHandler
            sendMsg(btService, msg)
            btCmdSend(getFwCmd, 0)
            btCmdSend(getNameCmd, 0)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBtBind = false
            Log.d(KotlinLog, "onServiceDisconnected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bt_app)

        var bundle = intent.extras
        maxBtDevice = bundle.getInt("maxBt", 1)
        Log.d(KotlinLog, "maxBtDevice $maxBtDevice")
        if(!isBtBind)
            bindService(Intent(this, BtService::class.java), serviceConn, Context.BIND_AUTO_CREATE)
    }

    fun btCmdSend(cmd: ByteArray, btDevice: Int)
    {
        val msg = Message.obtain(null, 2, btDevice, 0)
        val bundle = Bundle()

        Log.d(KotlinLog, "btCmdSend")
        bundle.putByteArray("rfcCmd", cmd)
        msg.data = bundle
        sendMsg(btService, msg)
    }

    fun sendMsg(messenger: Messenger, msg: Message) {
        if(!isBtBind) return
        // msg.replyTo = clientMsgHandler
        try {
            messenger.send(msg)
        } catch (e: IOException) {
            Log.d(KotlinLog, "service send message exception")
            e.printStackTrace()
        }
    }

    fun rfcCmdParse(cmdBuf: ByteArray, btDevice: Int) {
        // val len = cmdBuf[1]
        // for(i: Int in 0..len + 1)
        // Log.d(ktLog, "${rfcRecData[i]}")
        // Log.d(ktLog, "command length: ${len} ${rfcRecDataLen}")
        when(cmdBuf[0]) {
            0x29.toByte() -> {
                Log.d(KotlinLog, "Pair A2 command send")
            }
            0x41.toByte() -> {
                Log.d(KotlinLog," firmware version: ${String(cmdBuf, 2, cmdBuf[1].toInt())}")
                when(btDevice) {
                    0 -> txvAppFwVer0.text = String(cmdBuf, 2, cmdBuf[1].toInt())
                    // 1 -> txvAppFwVer1.text = String(cmdBuf, 2, cmdBuf[1].toInt())
                }
            }
            0x43.toByte() -> {
                // Log.d(ktLog, "feature response command ${rfcRecData[1]}")
                val feature = cmdBuf[2].toInt().and(0xff).shl(8) + cmdBuf[3].toInt().and(0xff)
                val deviceMax = cmdBuf[4].toInt().and(0xff)
                val talkMax = cmdBuf[5].toInt().and(0xff)
                val bdaLapFilter = cmdBuf[6].toInt().and(0xff).shl(24) + cmdBuf[7].toInt().and(0xff).shl(16) + cmdBuf[8].toInt().and(0xff).shl(8) + cmdBuf[9].toInt().and(0xff)
                val bdaUapFilter = cmdBuf[10].toInt().and(0xff)
                val bdaNapFilter = cmdBuf[11].toInt().and(0xff).shl(8) + cmdBuf[12].toInt().and(0xff)
                val ledBlue = cmdBuf[13].toInt().and(0xff).shl(8) + cmdBuf[14].toInt().and(0xff)
                val ledRed = cmdBuf[15].toInt().and(0xff).shl(8) + cmdBuf[16].toInt().and(0xff)
                val ledBluePwr = cmdBuf[17].toInt().and(0xff).shl(8) + cmdBuf[18].toInt().and(0xff)
                val ledRedPwr = cmdBuf[19].toInt().and(0xff).shl(8) + cmdBuf[20].toInt().and(0xff)
                Log.d(KotlinLog, "feature: ${feature.toString(16)} deviceMax: ${deviceMax.toString(16)} talkMax: ${talkMax.toString(16)} bda filter: ${bdaNapFilter.toString(16)}:${bdaUapFilter.toString(16)}:${bdaLapFilter.toString(16)} ledBlue: ${ledBlue.toString(16)} ledRed: ${ledRed.toString(16)} ledBluePwr: ${ledBluePwr.toString(16)} ledRedPwr: ${ledRedPwr.toString(16)}")
            }
            0x45.toByte() -> {
                //Log.d(ktLog, "volume response command ${rfcRecData[1]}")
                val wiredMic = cmdBuf[2].toInt().and(0xff)
                val wiredSpkr = cmdBuf[3].toInt().and(0xff)
                val usbMic = cmdBuf[4].toInt().and(0xff)
                val usbSpkr = cmdBuf[5].toInt().and(0xff)
                val btMic = cmdBuf[6].toInt().and(0xff)
                val btSpkr = cmdBuf[7].toInt().and(0xff)
                val vcsMic = cmdBuf[8].toInt().and(0xff)
                val vcsSpkr = cmdBuf[9].toInt().and(0xff)
                val wiredAv = cmdBuf[10].toInt().and(0xff)
                val usbAv = cmdBuf[11].toInt().and(0xff)
                val btAv = cmdBuf[12].toInt().and(0xff)
                val vcsAv = cmdBuf[13].toInt().and(0xff)
                val spkrDecad = cmdBuf[14].toInt().and(0xff)
                Log.d(KotlinLog, "wired mic/spkr/av: ${wiredMic.toString(16)} :${wiredSpkr.toString(16)} :${wiredAv.toString(16)} usb mic/spkr/av: ${usbMic.toString(16)} :${usbSpkr.toString(16)} :${usbAv.toString(16)} bt mic/spkr/av: ${btMic.toString(16)}: ${btSpkr.toString(16)}: ${btAv.toString(16)} vcs mic/spkr/av: ${vcsMic.toString(16)}: ${vcsSpkr.toString(16)}: ${vcsAv.toString(16)} speaker decad: ${spkrDecad.toString(16)}")
            }
            0x47.toByte() -> {
                // Log.d(ktLog, "max volume response command ${rfcRecData[1]}")
                val wiredMic = cmdBuf[2].toInt().and(0xff)
                val wiredSpkr = cmdBuf[3].toInt().and(0xff)
                val usbMic = cmdBuf[4].toInt().and(0xff)
                val usbSpkr = cmdBuf[5].toInt().and(0xff)
                val btMic = cmdBuf[6].toInt().and(0xff)
                val btSpkr = cmdBuf[7].toInt().and(0xff)
                val vcsMic = cmdBuf[8].toInt().and(0xff)
                val vcsSpkr = cmdBuf[9].toInt().and(0xff)
                val wiredAv = cmdBuf[10].toInt().and(0xff)
                val usbAv = cmdBuf[11].toInt().and(0xff)
                val btAv = cmdBuf[12].toInt().and(0xff)
                val vcsAv = cmdBuf[13].toInt().and(0xff)
                val spkrDecad = cmdBuf[14].toInt().and(0xff)
                Log.d(KotlinLog, "wired mic/spkr/av: ${wiredMic.toString(16)} :${wiredSpkr.toString(16)} :${wiredAv.toString(16)} usb mic/spkr/av: ${usbMic.toString(16)} :${usbSpkr.toString(16)} :${usbAv.toString(16)} bt mic/spkr/av: ${btMic.toString(16)}: ${btSpkr.toString(16)}: ${btAv.toString(16)} vcs mic/spkr/av: ${vcsMic.toString(16)}: ${vcsSpkr.toString(16)}: ${vcsAv.toString(16)} speaker decad: ${spkrDecad.toString(16)}")
            }
            0x49.toByte() -> {
                // Log.d(ktLog, "local name response command ${rfcRecData[1]}")
                Log.d(KotlinLog," local name: ${String(cmdBuf, 2, cmdBuf[1].toInt())}")
                when(btDevice) {
                    0 -> txvAppGetName0.text = String(cmdBuf, 2, cmdBuf[1].toInt())
                    // 1 -> txvAppGetName1.text = String(cmdBuf, 2, cmdBuf[1].toInt())

                }
            }
            0x9B.toByte() -> {
                // Log.d(ktLog, "state response command ${rfcRecData[1]}")
                val conState = cmdBuf[2].toInt().and(0xff).shl(24) + cmdBuf[3].toInt().and(0xff).shl(16) + cmdBuf[4].toInt().and(0xff).shl(8) + cmdBuf[5].toInt().and(0xff)
                val setState = cmdBuf[6].toInt().and(0xff).shl(24) + cmdBuf[7].toInt().and(0xff).shl(16) + cmdBuf[8].toInt().and(0xff).shl(8) + cmdBuf[9].toInt().and(0xff)
                Log.d(KotlinLog, "connect state: ${conState.toString(16)} connect set: ${setState.toString(16)}")
            }
            0xa1.toByte() -> {
                Log.d(KotlinLog, "pstn parameter send")
            }
            0xa3.toByte() -> {
                val flashTime = cmdBuf[2].toInt().and(0xff)
                val pstnSpkr = cmdBuf[3].toInt().and(0xff)
                Log.d(KotlinLog, "pstn parameter flash time: ${flashTime} speaker vol: ${pstnSpkr}")
            }
            0xa5.toByte() -> {
                Log.d(KotlinLog, "pstn flash command response")
            }
            0x01.toByte() -> {
                Log.d(KotlinLog, "hfp pair cmd ${cmdBuf[2].toInt().and(0xff).toString(16)} ${cmdBuf[3].toInt().and(0xff).toString(16)} ${cmdBuf[4].toInt().and(0xff).toString(16)} ${cmdBuf[5].toInt().and(0xff).toString(16)} ${cmdBuf[6].toInt().and(0xff).toString(16)} ${cmdBuf[7].toInt().and(0xff).toString(16)} ${cmdBuf[8].toInt().and(0xff).toString(16)} ${cmdBuf[9].toInt().and(0xff).toString(16)}")
            }
            0x03.toByte() -> {
                Log.d(KotlinLog, "hfp con cmd ${cmdBuf[2].toInt().and(0xff).toString(16)} ${cmdBuf[3].toInt().and(0xff).toString(16)} ${cmdBuf[4].toInt().and(0xff).toString(16)} ${cmdBuf[5].toInt().and(0xff).toString(16)} ${cmdBuf[6].toInt().and(0xff).toString(16)} ${cmdBuf[7].toInt().and(0xff).toString(16)} ${cmdBuf[8].toInt().and(0xff).toString(16)} ${cmdBuf[9].toInt().and(0xff).toString(16)}")
            }
            0x05.toByte() -> {
                Log.d(KotlinLog, "voice cmd ${cmdBuf[2].toInt().and(0xff).toString(16)} ${cmdBuf[3].toInt().and(0xff).toString(16)} ${cmdBuf[4].toInt().and(0xff).toString(16)} ${cmdBuf[5].toInt().and(0xff).toString(16)} ${cmdBuf[6].toInt().and(0xff).toString(16)} ${cmdBuf[7].toInt().and(0xff).toString(16)} ${cmdBuf[8].toInt().and(0xff).toString(16)} ${cmdBuf[9].toInt().and(0xff).toString(16)}")
            }
            0x07.toByte() -> {
                Log.d(KotlinLog, "volume cmd ${cmdBuf[2].toInt().and(0xff).toString(16)} ${cmdBuf[3].toInt().and(0xff).toString(16)} ${cmdBuf[4].toInt().and(0xff).toString(16)} ${cmdBuf[5].toInt().and(0xff).toString(16)} ${cmdBuf[6].toInt().and(0xff).toString(16)} ${cmdBuf[7].toInt().and(0xff).toString(16)} ${cmdBuf[8].toInt().and(0xff).toString(16)} ${cmdBuf[9].toInt().and(0xff).toString(16)} ${cmdBuf[10].toInt().and(0xff).toString(16)} ${cmdBuf[11].toInt().and(0xff).toString(16)}")
            }
            0x09.toByte() -> {
                Log.d(KotlinLog, "charge cmd ${cmdBuf[2].toInt().and(0xff).toString(16)} ${cmdBuf[3].toInt().and(0xff).toString(16)} ${cmdBuf[4].toInt().and(0xff).toString(16)} ${cmdBuf[5].toInt().and(0xff).toString(16)} ${cmdBuf[6].toInt().and(0xff).toString(16)} ${cmdBuf[7].toInt().and(0xff).toString(16)} ${cmdBuf[8].toInt().and(0xff).toString(16)} ${cmdBuf[9].toInt().and(0xff).toString(16)}")
            }
            0x0b.toByte() -> {
                Log.d(KotlinLog, "battery cmd ${cmdBuf[2].toInt().and(0xff).toString(16)} ${cmdBuf[3].toInt().and(0xff).toString(16)} ${cmdBuf[4].toInt().and(0xff).toString(16)} ${cmdBuf[5].toInt().and(0xff).toString(16)} ${cmdBuf[6].toInt().and(0xff).toString(16)} ${cmdBuf[7].toInt().and(0xff).toString(16)} ${cmdBuf[8].toInt().and(0xff).toString(16)} ${cmdBuf[9].toInt().and(0xff).toString(16)}")
            }
            0x0d.toByte() -> {
                Log.d(KotlinLog, "vote cmd ${cmdBuf[2].toInt().and(0xff).toString(16)} ${cmdBuf[3].toInt().and(0xff).toString(16)} ${cmdBuf[4].toInt().and(0xff).toString(16)} ${cmdBuf[5].toInt().and(0xff).toString(16)} ${cmdBuf[6].toInt().and(0xff).toString(16)} ${cmdBuf[7].toInt().and(0xff).toString(16)} ${cmdBuf[8].toInt().and(0xff).toString(16)} ${cmdBuf[9].toInt().and(0xff).toString(16)}")
            }
            0x0f.toByte() -> {
                Log.d(KotlinLog, "rssi cmd ${cmdBuf[2].toInt().and(0xff).toString(16)} ${cmdBuf[3].toInt().and(0xff).toString(16)} ${cmdBuf[4].toInt().and(0xff).toString(16)} ${cmdBuf[5].toInt().and(0xff).toString(16)} ${cmdBuf[6].toInt().and(0xff).toString(16)} ${cmdBuf[7].toInt().and(0xff).toString(16)} ${cmdBuf[8].toInt().and(0xff).toString(16)} ${cmdBuf[9].toInt().and(0xff).toString(16)} ${cmdBuf[10].toInt().and(0xff).toString(16)} ${cmdBuf[11].toInt().and(0xff).toString(16)}")
            }
            else -> Log.d(KotlinLog, "other command data: ${cmdBuf[2].toString(16)} ${cmdBuf[3].toString(16)} ${cmdBuf[4].toString(16)} ${cmdBuf[5].toString(16)} ${cmdBuf[6].toString(16)} ${cmdBuf[7].toString(16)} ${cmdBuf[8].toString(16)} ${cmdBuf[9].toString(16)}")
        }
    }
}
