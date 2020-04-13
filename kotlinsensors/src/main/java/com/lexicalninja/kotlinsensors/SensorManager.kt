package com.lexicalninja.kotlinsensors

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.STATE_OFF
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import com.kinetic.fit.kotlinsensors.IServiceFactory
import java.util.*
import kotlin.properties.Delegates
import kotlin.reflect.KClass
import kotlin.reflect.KFunction


private const val TAG = "SensorManager"

class SensorManager : Service() {
    interface Observer {
        fun onBluetoothStateChanged(state: ManagerState)
        fun onSensorDiscovered(sensor: BleSensor)
        fun onSensorConnected(sensor: BleSensor)
        fun onSensorConnectionFailed(sensor: BleSensor)
        fun onSensorDisconnected(sensor: BleSensor)
        fun onSensorRemoved(sensor: BleSensor)
    }

    //    Passive Scan will be correlated with ScanMode.SCAN_MODE_LOW_POWER
//    Aggressive Scan will be corrrelated with ScanMode.SCAN_MODE_OPPORTUNISTIC
    enum class ManagerState { Off, Idle, PassiveScan, AggressiveScan }

    var state by Delegates.observable(ManagerState.Off) { _, old, new ->
        if (new != old) stateUpdated()
    }

    var SensorType: KClass<out BleSensor> = BleSensor::class

    private val binder = SensorManagerBinder()

    private val bluetoothManager: BluetoothManager by lazy {
        applicationContext.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy { bluetoothManager.adapter }

    private val sensors: MutableSet<BleSensor>
        get() = mutableSetOf(*sensorsById.values.toTypedArray())

    private var sensorsById = mutableMapOf<String, BleSensor>()

    private fun MutableSet<BleSensor>.hasIn(sensor: BleSensor?): Boolean {
        return this.map { it.device?.address }.contains(sensor?.device?.address)
    }

    private fun MutableSet<BleSensor>.getSensor(sensor: BleSensor?): BleSensor? {
        var s: BleSensor? = null
        this.forEach {
            if (it.device?.address == sensor?.device?.address) {
                s = it
            }
        }
        return s
    }

    private val serviceFactory = ServiceFactory()

    private var activityUpdateTimer: Timer = Timer()

    private var observers = mutableSetOf<Observer>()

    private fun notifySensorRemoved(sensor: BleSensor) {
//        Log.d(TAG, "Sensor ${sensor.name} removed")
//        Log.d(TAG, "Sensors after removal: ${sensors.map { it.device?.address }}")
        observers.forEach { it.onSensorRemoved(sensor) }
    }

    private fun notifySensorDiscovered(sensor: BleSensor) {
        observers.forEach { it.onSensorDiscovered(sensor) }
    }

    companion object {
        internal const val RssiPingInterval = 2000
        private const val ActivityInterval = 5000
        internal const val InactiveTimeInterval = 4000
        internal fun getScanFilterForUuid(uuidString: String): ScanFilter {
            return ScanFilter.Builder()
                .setServiceUuid(ParcelUuid.fromString(uuidString), BLE_SERVICE_MASK_UUID)
                .build()
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(applicationInBGReceiver, IntentFilter(APPLICATION_IN_BACKGROUND))
        registerReceiver(applicationInFGReceiver, IntentFilter(APPLICATION_IN_FOREGROUND))
        registerReceiver(sessionControllerScanReceiver, IntentFilter(START_SENSOR_SCAN))
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.")
        }
    }

//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        val notificationIntent = Intent(this, RootActivity::class.java)
//        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
//        val builder = NotificationCompat.Builder(this, "Sensor Service")
//                .setSmallIcon(R.drawable.kinetic_logo)
//                .setContentTitle("Kinetic Sensor Service")
//                .setContentText("Kinetic is running in the background to stay connected with your sensors. Slide to change settings for this notification")
//                .setTicker("A Kinetic Fit service is running")
//                .setContentIntent(pendingIntent)
//        val notification = builder.build()
//        if (SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel("Sensor Service", "Sensor Connection Service", NotificationManager.IMPORTANCE_DEFAULT)
//            channel.description = "Kinetic Sensor Scanning and Connection Channel"
//            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            notificationManager.createNotificationChannel(channel)
//        }
//        startForeground(33, notification)
//        return super.onStartCommand(intent, flags, startId)
//    }

    override fun onBind(p0: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(applicationInBGReceiver)
        unregisterReceiver(applicationInFGReceiver)
        unregisterReceiver(sessionControllerScanReceiver)
        stopScan()
    }

    private fun stateUpdated() {
        if (bluetoothAdapter?.state == STATE_OFF) return
        when (state) {
            ManagerState.Off -> {
                stopScan()
                sensors.forEach { it.disconnect() }
            }
            ManagerState.Idle -> {
                stopScan()
//                stateUpdated()
            }
            ManagerState.PassiveScan -> {
//                startScan(ManagerState.PassiveScan)
                startActivityTimer()
            }
            ManagerState.AggressiveScan -> {
//                startScan(ManagerState.AggressiveScan)
                startActivityTimer()
            }
        }
    }

    private fun startScan(state: ManagerState) {
//        Log.d(TAG, "Starting LEScan")
        this.state = state
        bluetoothAdapter?.apply {
            val builder = Builder()
                .setReportDelay(0)
                .setScanMode(if (state == ManagerState.PassiveScan) SCAN_MODE_LOW_POWER else SCAN_MODE_LOW_LATENCY)
            if (SDK_INT >= M) {
                builder.setCallbackType(CALLBACK_TYPE_ALL_MATCHES)
                    .setMatchMode(MATCH_MODE_STICKY)
                    .setNumOfMatches(MATCH_NUM_MAX_ADVERTISEMENT)
            }
            bluetoothLeScanner?.startScan(serviceFactory.scanFilters, builder.build(), scanCallback)
        }
    }

    private fun stopScan() = bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)

    private fun startActivityTimer() {
        activityUpdateTimer.cancel()
        activityUpdateTimer.purge()
        activityUpdateTimer = Timer()
        activityUpdateTimer.scheduleAtFixedRate(
            ActivityTimerTask(),
            Date(),
            ActivityInterval.toLong()
        )
    }

    private fun removeInactive(inactiveTime: Int) {
        val now = Date().time
        sensors.forEach {
            if (now - it.lastSensorActivity > inactiveTime) {
                if (sensorsById.remove(it.device!!.address) != null) {
                    notifySensorRemoved(it)
                }
            }
        }
    }

    private var scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (callbackType == CALLBACK_TYPE_ALL_MATCHES) {
                sensorForDevice(result.device, true)
            }
//            Log.d(TAG, "${sensors.map { it.device?.address }}")
        }
    }

    private fun sensorForDevice(
        device: BluetoothDevice,
        create: Boolean,
        advertisements: Array<UUID> = arrayOf()
    ): BleSensor? {
        if (sensorsById[device.address] != null) return sensorsById[device.address]
        if (!create) return null
        val sensor = createEntity(
            SensorType.constructors.first(),
            device,
            applicationContext,
            arrayOf<UUID>()
        )
        sensor.setServiceFactory(this.serviceFactory)
        sensorsById[device.address] = sensor
        notifySensorDiscovered(sensor)
        return sensor
    }


    private val applicationInBGReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            stopScan()
        }
    }

    private val applicationInFGReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            startScan(ManagerState.PassiveScan)
        }
    }

    private val sessionControllerScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            startScan(ManagerState.PassiveScan)
        }
    }

    inner class SensorManagerBinder : Binder() {

        val sensorList: MutableList<BleSensor>
            get() = sensors.toMutableList()

        fun scan(state: ManagerState) = startScan(state)

        fun setServicesToScanFor(vararg serviceTypes: IServiceFactory) {
            addServiceTypes(*serviceTypes)
            addScanFilters(*serviceTypes.map { it.uuid }.toTypedArray())
        }

        fun getSensor(sensorId: String) = sensorsById[sensorId]

        fun addServiceTypes(vararg serviceTypes: IServiceFactory) {
            serviceTypes.forEach {
                serviceFactory.serviceTypes[it.uuid] = it
                serviceFactory.serviceUUIDs?.add(UUID.fromString(it.uuid))
            }
        }

        fun addScanFilter(uuidString: String) {
            serviceFactory.scanFilters.add(getScanFilterForUuid(uuidString))
        }

        fun addScanFilters(vararg uuids: String) {
            uuids.forEach { addScanFilter(it) }
        }

        fun connectToSensor(sensor: BleSensor) = sensor.connect()

        fun disconnectFromSensor(sensor: BleSensor) = sensor.disconnect()

        fun removeInactiveSensors(inactiveTime: Int) {
            removeInactive(inactiveTime)
        }

        fun addObserver(observer: Observer) = observers.add(observer)

        fun removeObserver(observer: Observer) = observers.remove(observer)

        fun setSensorType(type: KClass<out BleSensor>) {
            SensorType = type
        }
    }

    inner class ServiceFactory {
        var serviceTypes = mutableMapOf<String, IServiceFactory>()
        var serviceUUIDs: MutableList<UUID>? = mutableListOf()
            get() {
                return if (serviceTypes.isEmpty()) null else serviceTypes.map { UUID.fromString(it.key) }
                    .toMutableList()
            }
        internal val scanFilters = arrayListOf<ScanFilter>()
    }

    private inner class ActivityTimerTask : TimerTask() {
        override fun run() {
            sensors.forEach {
                val now = Date().time
//                Log.d(TAG, "Now: $now; last: ${it.lastSensorActivity}; now minus last: ${now - it.lastSensorActivity}; Sensor: ${it.sensorId}")
                if (now - it.lastSensorActivity > InactiveTimeInterval) {
                    it.rssi = Int.MIN_VALUE
                }
            }
        }
    }

    private fun <T> createEntity(constructor: KFunction<T>, vararg args: Any): T {
        return constructor.call(*args)
    }
}