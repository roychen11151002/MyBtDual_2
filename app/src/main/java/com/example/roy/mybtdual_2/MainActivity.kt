package com.example.roy.mybtdual_2

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException


const val KotlinLog = "kotlinTest"

class MainActivity : AppCompatActivity() {
    lateinit var preferData : SharedPreferences
    val BtServiceRequestCode: Int = 3
    // var btBda =  arrayOf("00:00:00:00:00:00", "00:00:00:00:00:00", "00:00:00:00:00:00", "00:00:00:00:00:00", "00:00:00:00:00:00", "00:00:00:00:00:00", "00:00:00:00:00:00", "00:00:00:00:00:00", "00:00:00:00:00:00", "00:00:00:00:00:00")
    val maxBtDevice = 2
    var btBda = arrayOfNulls<String>(maxBtDevice)
    lateinit var adapter: BtListAdapter
    var btList = ArrayList<String>()

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
                Log.d(KotlinLog, "client message: ${msg.what}, arg1: ${msg.arg1}, arg2: ${msg.arg2}")
                clientBundle = msg.data
                when(msg.what)
                {
                    0 -> {
                        clientMsg = Message.obtain(null, 1, 0, 0)
                        clientBundle.putStringArray("remoteBda", btBda)
                        clientMsg.data = clientBundle
                        sendMsg(btService, clientMsg)
                        clientMsg = Message.obtain(null, 1, 1, 0)
                        clientMsg.data = clientBundle
                        sendMsg(btService, clientMsg)
                        txvBtState0.text = "Bluetooth connecting"
                        txvBtState1.text = "Bluetooth connecting"
                    }
                    1 -> {
                        if(msg.arg2 == 0) {
                            var it = Intent()
                            var bundle = Bundle()
                            bundle.putInt("maxBt", 2)
                            it.setClass(this@MainActivity, btAppActivity::class.java)
                            it.putExtras(bundle)
                            startActivityForResult(it, 100)
                        }
                        when(msg.arg2) {
                            0 -> {
                                when(msg.arg1) {
                                    0 -> txvBtState0.text = "Bluetooth connected"
                                    1 -> txvBtState1.text = "Bluetooth connected"
                                }
                            }
                            1 -> {
                                when(msg.arg1) {
                                    0 -> txvBtState0.text = "Bluetooth disconnect"
                                    1 -> txvBtState1.text = "Bluetooth disconnect"
                                }
                            }
                            2 -> {
                                // val pairedStr = StringBuilder()

                                for(device in clientBundle.getStringArrayList("pairedList"))
                                    btList.add(device)
                                recycleViewDiscovery.layoutManager.scrollToPosition(0)
                            }
                            3 -> {
                                val str = clientBundle.getString("discoveryStr")
                                Log.d(KotlinLog, "$str")
                                btList.add(str)
                                recycleViewDiscovery.layoutManager.scrollToPosition(0)
                            }
                            4 -> {
                                btnPairedList.isEnabled = true
                                txvDiscoveryStr.text = "find device end"
                            }
                            else -> {}
                        }
                    }
                    2 -> {
                        clientBundle = msg.data
                        val rfcCmd = clientBundle.getByteArray("rfcCmd")
                        rfcCmdParse(rfcCmd, msg.arg1)
                    }
                    else -> Log.d(KotlinLog, "client message other")
                }
            }
        }
    }

    val serviceConn = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val msg = Message.obtain(null, 0, 0, maxBtDevice)

            Log.d(KotlinLog, "onServiceConnected")
            btService = Messenger(service)
            isBtBind = true

            msg.replyTo = clientMsgHandler
            sendMsg(btService, msg)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBtBind = false
            Log.d(KotlinLog, "onServiceDisconnected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(KotlinLog, "onCreate")
        btInit()
        preferData = getSharedPreferences("iMageBda", Context.MODE_PRIVATE)
        for(i: Int in 0 until  maxBtDevice) {
            btBda[i] = preferData.getString("btBda${i.toString()}", "00:00:00:00:00:00")
            Log.d(KotlinLog, "bluetooth bda$i: ${btBda[i]}")
        }


        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        btnGetFwVer0.setOnClickListener {
            val getFwCmd = byteArrayOf(0x40, 0 ,0x00)

            btCmdSend(getFwCmd, 0)
            txvFwVer0.text = "none"
        }

        btnGetFwVer1.setOnClickListener {
            val getFwCmd = byteArrayOf(0x40, 0 ,0x00)

            btCmdSend(getFwCmd, 1)
            txvFwVer1.text = "none"
        }

        btnGetName0.setOnClickListener {
            val getNameCmd = byteArrayOf(0x48, 0 ,0x00)

            btCmdSend(getNameCmd, 0)
            txvGetVer0.text = "none"
        }

        btnGetName1.setOnClickListener {
            val getNameCmd = byteArrayOf(0x48, 0 ,0x00)

            btCmdSend(getNameCmd, 1)
            txvGetName1.text = "none"
        }

        btnPairedList.setOnClickListener {
            val msg = Message.obtain(null, 1, 0, 2)

            sendMsg(btService, msg)
            btList.removeAll(btList)
            recycleViewDiscovery.adapter.notifyDataSetChanged()
            recycleViewDiscovery.layoutManager
            txvDiscoveryStr.text = "paired list"
        }

        btnBtDiscovery.setOnClickListener {
            if(btnPairedList.isEnabled) {
                val msg = Message.obtain(null, 1, 0, 3)
                btnPairedList.isEnabled = false
                sendMsg(btService, msg)
                btList.removeAll(btList)
                recycleViewDiscovery.adapter.notifyDataSetChanged()
                recycleViewDiscovery.layoutManager
                txvDiscoveryStr.text = "find device ..."
            }
            else {
                val msg = Message.obtain(null, 1, 0, 4)
                sendMsg(btService, msg)
            }
        }

        btncon0.setOnClickListener {
            val msg = Message.obtain(null, 1, 0, 0)
            val bundle = Bundle()

            bundle.putStringArray("remoteBda", btBda)
            msg.data = bundle
            sendMsg(btService, msg)
            txvBtState0.text = "Bluetooth connecting"
        }

        btncon1.setOnClickListener {
            val msg = Message.obtain(null, 1, 0, 1)

            sendMsg(btService, msg)
            txvBtState1.text = "Bluetooth connecting"
        }

        adapter = BtListAdapter(btList)
        recycleViewDiscovery.layoutManager = LinearLayoutManager(applicationContext)
        recycleViewDiscovery.adapter = adapter
        adapter.setOnItemClickListener(object : BtListAdapter.OnItemClickListener {
            override fun onItemClick(view: View, position: Int, bda: String) {
                val msg = Message.obtain(null, 1, 0, 0)
                val bundle = Bundle()
                val preferDataEdit = preferData.edit()

                Log.d(KotlinLog, "\tonItemClick")
                preferDataEdit.putString("btBda0", bda)
                preferDataEdit.apply()

                btBda[0] = bda
                bundle.putStringArray("remoteBda", btBda)
                msg.data = bundle
                sendMsg(btService, msg)
                txvBtState0.text = "Bluetooth connecting"
                Log.d(KotlinLog, "discovery address:$btBda[0]")
            }
        })

        adapter.setOnItemLongClickListener(object : BtListAdapter.OnItemLongClickListener {
            override fun onItemLongClick(view: View, position: Int, bda: String): Boolean {
                val msg = Message.obtain(null, 1, 1, 0)
                val bundle = Bundle()
                val preferDataEdit = preferData.edit()

                Log.d(KotlinLog, "\tonItemLongClick")
                preferDataEdit.putString("btBda1", bda)
                preferDataEdit.apply()

                btBda[1] = bda
                bundle.putStringArray("remoteBda", btBda)
                msg.data = bundle
                sendMsg(btService, msg)
                txvBtState1.text = "Bluetooth connecting"
                Log.d(KotlinLog, "discovery address:$btBda[1]")
                return true
            }
        })
    }

    override fun onStart() {
        super.onStart()
        Log.d(KotlinLog, "onStart")
        // startService(Intent(this, BtService::class.java))
        // sendMessage()
    }

    override fun onResume() {
        super.onResume()
        Log.d(KotlinLog, "onResume")
        // requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
/*
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE)
*/
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(KotlinLog, "onRestart")
    }

    override fun onPause() {
        super.onPause()
        Log.d(KotlinLog, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(KotlinLog, "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(KotlinLog, "onDestroy")
        // isBtBind = false
        // unbindService(serviceConn)
        // stopService(Intent(this, BtService::class.java))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 1) {
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                btInit()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d(KotlinLog, "requestCode $requestCode resultCode $resultCode")
        when(resultCode) {
            101 -> {
                val msg = Message.obtain(null, 100, 0, 0)

                msg.replyTo = clientMsgHandler
                sendMsg(btService, msg)
            }

        }

        when (requestCode) {
            BtServiceRequestCode -> {
                if (resultCode == Activity.RESULT_OK) {
                    Log.d(KotlinLog, "Bluetooth result OK")
                    txvBtState0.text = "Bluetooth enable"
                    btInit()
                    // if(!isBtBind)
                    //    bindService(Intent(this, BtService::class.java), serviceConn, Context.BIND_AUTO_CREATE)
                }
                else {
                    Log.d(KotlinLog, "Bluetooth result cancel")
                    finish()
                }
            }
/*
            (BtServiceRequestCode + 1) -> {
                if(resultCode == Activity.RESULT_OK) {
                    Log.d(KotlinLog, "Bluetooth discoverable")
                }
            }
            (BtServiceRequestCode + 2) -> {
                    Log.d(KotlinLog, " bluetooth result code is $resultCode")
            }
*/
        }
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

    fun btInit() {
        Log.d(KotlinLog, "btInit")
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) &&
                (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            Log.d(KotlinLog, "Bluetooth request permission")
        } else if (BluetoothAdapter.getDefaultAdapter().isEnabled) {
            Log.d(KotlinLog, "Bluetooth enable")
            txvBtState0.text = "Bluetooth enable"
            txvBtState1.text = "Bluetooth enable"
            // startActivityForResult(Intent(this, BluetoothActivity::class.java), BtServiceRequestCode + 2)
            // onActivityResult(BtServiceRequestCode + 2, 0, null)
            if(!isBtBind)
                bindService(Intent(this, BtService::class.java), serviceConn, Context.BIND_AUTO_CREATE)
        } else {
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), BtServiceRequestCode)
            Log.d(KotlinLog, "Bluetooth disable")
            txvBtState0.text = "Bluetooth disable"
            txvBtState1.text = "Bluetooth disable"
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
                    0 -> txvFwVer0.text = String(cmdBuf, 2, cmdBuf[1].toInt())
                    1 -> txvFwVer1.text = String(cmdBuf, 2, cmdBuf[1].toInt())
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
                    0 -> txvGetVer0.text = String(cmdBuf, 2, cmdBuf[1].toInt())
                    1 -> txvGetName1.text = String(cmdBuf, 2, cmdBuf[1].toInt())

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
