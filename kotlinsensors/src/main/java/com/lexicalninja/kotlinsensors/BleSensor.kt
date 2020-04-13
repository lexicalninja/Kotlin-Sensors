package com.kinetic.fit.kotlinsensors

import android.bluetooth.*
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import androidx.annotation.UiThread
import com.kinetic.fit.kotlinsensors.SensorManager.Companion.RssiPingInterval
import com.lexicalninja.kotlinsensors.BleCharacteristic
import com.lexicalninja.kotlinsensors.ICharacteristicObserver
import java.lang.ref.WeakReference
import java.util.*
import kotlin.properties.Delegates

private const val TAG = "BleSensor"

open class BleSensor(val device: BluetoothDevice?, internal val context: Context, val advertisements: Array<UUID> = arrayOf())
    : ICharacteristicObserver {
    interface Observer {
        fun sensorStateChanged(sensor: BleSensor, state: State)
        fun rssiValueChanged(sensor: BleSensor, rssi: Int)
        fun sensorServiceDiscovered(service: BleService)
        fun sensorServiceFeaturesIdentified(sensor: BleSensor, service: BleService)
        fun sensorCharacteristicValueChanged(sensor: BleSensor, characteristic: BleCharacteristic)
        fun sensorCharacteristicDiscovered(characteristic: BleCharacteristic)
    }

    open var observers: MutableSet<Observer> = Collections.newSetFromMap<Observer>(WeakHashMap<Observer, Boolean>())
    open fun addObserver(observer: Observer) = observers.add(observer)
    open fun removeObserver(observer: Observer) = observers.remove(observer)

    private val bluetoothManager: BluetoothManager by lazy { context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager }
    internal var bluetoothGatt: BluetoothGatt? = null
    private var operationQueue: Queue<BluetoothOperation> = LinkedList<BluetoothOperation>()

    open val sensorId: String = device?.address ?: "No Device"
    open val name: String = device?.name ?: "No Device"
    private var rssiPingTimer: Timer? = null

    private var rssiPingEnable: Boolean by Delegates.observable(false) { prop, old, new ->
        if (new) {
            if (rssiPingTimer == null) {
                rssiPingTimer = Timer()
                rssiPingTimer!!.scheduleAtFixedRate(PingTimerTask(), Date(), RssiPingInterval.toLong())
            }
        } else {
            rssi = Int.MIN_VALUE
            rssiPingTimer?.cancel()
            rssiPingTimer?.purge()
            rssiPingTimer = null
        }
    }

    internal fun markSensorActivity() {
        lastSensorActivity = Date().time
    }

    private inner class PingTimerTask : TimerTask() {
        override fun run() {
            if (getState() == State.Connected) {
                readRemoteRssi()
            }
        }
    }

    internal var serviceFactory: WeakReference<SensorManager.ServiceFactory>? = null
        private set

    fun setServiceFactory(factory: SensorManager.ServiceFactory) {
        this.serviceFactory = WeakReference(factory)
    }

    enum class State { Connecting, Connected, Disconnecting, Disconnected }

    var services: MutableMap<String, BleService> = mutableMapOf()
        private set

    var rssi: Int by Delegates.observable(Int.MIN_VALUE) { prop, old, new ->
        notifyObserversRssiChanged(new)
    }
        internal set

    //    Last time of Sensor Communication with the Sensor Manager

    var lastSensorActivity = Date().time

    override fun equals(other: Any?): Boolean {
        if (other !is BleSensor) return false
        return this.device?.address == other.device?.address
    }

    override fun hashCode(): Int {
        var result = device?.hashCode() ?: 0
        result = 31 * result + context.hashCode()
        result = 31 * result + sensorId.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun onValueUpdated(characteristic: BleCharacteristic) {
        notifyObserversCharacteristicValueChanged(characteristic)
    }

    override fun onValueWritten(characteristic: BleCharacteristic) {}

    inline fun <reified T : BleService?> service(uuid: String? = null): T? {
        return if (uuid != null && services[uuid] is T) {
            services[uuid] as T
        } else {
            var s: T? = null
            services.values.forEach {
                if (it is T)
                    s = it
                return@forEach
            }
            s
        }
    }

    open fun advertisedService(uuid: String): Boolean {
        val service = UUID.fromString(uuid)
        advertisements.forEach {
            if (it == service) return true
        }
        return false
    }

    protected open fun notifyObserversRssiChanged(rssi: Int) {
        observers.forEach { it.rssiValueChanged(this@BleSensor, rssi) }
    }

    protected open fun notifyObserversStateChanged() {
        observers.forEach { it.sensorStateChanged(this@BleSensor, getState()) }
    }

    protected open fun notifyObserversCharacteristicValueChanged(characteristic: BleCharacteristic) {
        observers.forEach { it.sensorCharacteristicValueChanged(this@BleSensor, characteristic) }
    }

    @UiThread
    protected open fun notifySensorCharacteristicDiscovered(characteristic: BleCharacteristic) {
        observers.forEach { it.sensorCharacteristicDiscovered(characteristic) }
    }

    @UiThread
    protected open fun notifyServiceDiscovered(service: BleService) {
        observers.forEach { it.sensorServiceDiscovered(service) }
    }

    open fun notifyServiceFeaturesIdentified(sensor: BleSensor, service: BleService) {
        observers.forEach { it.sensorServiceFeaturesIdentified(sensor, service) }
    }

    open fun getState(): State {
        return bluetoothGatt?.takeIf { device != null }?.run {
            when (bluetoothManager.getConnectionState(device, BluetoothProfile.GATT)) {
                BluetoothProfile.STATE_CONNECTING -> State.Connecting
                BluetoothProfile.STATE_CONNECTED -> State.Connected
                BluetoothProfile.STATE_DISCONNECTING -> State.Disconnecting
                else -> return State.Disconnected
            }
        } ?: State.Disconnected
    }

    open fun connect() {
//        Log.d(TAG, "Connecting to sensor...")
        if (bluetoothGatt == null) {
            bluetoothGatt = device?.connectGatt(context, true, gattCallbacks)
//            notifyObserversStateChanged()
            operationQueue.clear()
        } else {
            val state = bluetoothManager.getConnectionState(device, BluetoothProfile.GATT)
            if (state != BluetoothProfile.STATE_CONNECTED && state != BluetoothProfile.STATE_CONNECTING) {
                bluetoothGatt?.connect()
//                notifyObserversStateChanged()
                operationQueue.clear()
            }
        }
    }

    open fun disconnect() {
        bluetoothGatt?.apply {
//            Log.d(TAG, "Disconnecting from sensor...")
            disconnect()
            operationQueue.clear()
        }
    }


    /**
     *
     *       Bluetooth QUEUE operations
     *
     **/

    private interface BluetoothOperation {
        fun execute(gatt: BluetoothGatt)
    }

    private inner class BluetoothOperationWriteDescriptor(internal var mDescriptor: BluetoothGattDescriptor) : BluetoothOperation {
        override fun execute(gatt: BluetoothGatt) {
            val w = gatt.writeDescriptor(mDescriptor)
        }
    }

    private inner class BluetoothOperationReadDescriptor(internal var mDescriptor: BluetoothGattDescriptor) : BluetoothOperation {
        override fun execute(gatt: BluetoothGatt) {
            gatt.readDescriptor(mDescriptor)
        }
    }

    private inner class BluetoothOperationReadCharacteristic(internal var mCharacteristic: BluetoothGattCharacteristic) : BluetoothOperation {
        override fun execute(gatt: BluetoothGatt) {
            gatt.readCharacteristic(mCharacteristic)
        }
    }

    private inner class BluetoothOperationWriteCharacteristic(internal var mCharacteristic: BluetoothGattCharacteristic) : BluetoothOperation {
        override fun execute(gatt: BluetoothGatt) {
            val w = gatt.writeCharacteristic(mCharacteristic)
        }
    }

    private inner class BluetoothOperationReadRemoteRssi : BluetoothOperation {
        override fun execute(gatt: BluetoothGatt) {
            val w = gatt.readRemoteRssi()
        }
    }

    private fun processQueue() {
        if (!operationQueue.isEmpty() && bluetoothGatt != null) {
            val qItem = operationQueue.peek()
            bluetoothGatt?.run { qItem.execute(this) }
        }
    }

    fun setNotifyForCharacteristic(characteristic: BluetoothGattCharacteristic, notify: Boolean) {
        bluetoothGatt?.setCharacteristicNotification(characteristic, true)
        for (descriptor in characteristic.descriptors) {
            if (descriptor.uuid == UUID.fromString(NOTIFICATION_DESCRIPTOR)) {
                if (notify) {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                } else {
                    descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                }
                writeDescriptor(descriptor)
            }
        }
    }

    fun setIndicateForCharacteristic(characteristic: BluetoothGattCharacteristic, notify: Boolean) {
        bluetoothGatt?.setCharacteristicNotification(characteristic, true)
        for (descriptor in characteristic.descriptors) {
            if (descriptor.uuid == UUID.fromString(NOTIFICATION_DESCRIPTOR)) {
                if (notify) {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                } else {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                }
                writeDescriptor(descriptor)
            }
        }
    }

    fun writeDescriptor(descriptor: BluetoothGattDescriptor) {
        val operation = BluetoothOperationWriteDescriptor(descriptor)
        operationQueue.add(operation)

        if (operationQueue.size == 1) {
            processQueue()
        }
    }

    fun readDescriptor(descriptor: BluetoothGattDescriptor) {
        val operation = BluetoothOperationReadDescriptor(descriptor)
        operationQueue.add(operation)

        if (operationQueue.size == 1) {
            processQueue()
        }
    }

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        val operation = BluetoothOperationReadCharacteristic(characteristic)
        operationQueue.add(operation)

        if (operationQueue.size == 1) {
            processQueue()
        }
    }

    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic) {
        val operation = BluetoothOperationWriteCharacteristic(characteristic)
        operationQueue.add(operation)

        if (operationQueue.size == 1) {
            processQueue()
        }
    }

    protected open fun readRemoteRssi() {
        val operation = BluetoothOperationReadRemoteRssi()
        operationQueue.add(operation)
        if (operationQueue.size == 1) {
            processQueue()
        }
    }

    protected open fun processServices(gatt: BluetoothGatt) {
        gatt.services?.forEach {
            if (services.keys.contains(it.uuid.toString().toUpperCase())) return

            val iServiceFactory = serviceFactory?.get()?.serviceTypes?.get(it.uuid.toString().toUpperCase())

            iServiceFactory?.apply {
                val service = create(it, WeakReference(this@BleSensor))
                services[it.uuid.toString().toUpperCase()] = service
                notifyServiceDiscovered(service)
//                Log.d(TAG, "Service discovered: ${service.javaClass.canonicalName}")
                discoverCharacteristics(service)
            }
        }
    }

    private fun discoverCharacteristics(service: BleService) {
        service.gattService.characteristics.forEach {
            if (service.characteristics.keys.contains(it.uuid.toString().toUpperCase())) return
            val iServiceFactory = serviceFactory?.get()?.serviceTypes?.get(service.gattService.uuid.toString().toUpperCase())
            iServiceFactory?.apply {
                val characteristicType = this.characteristicTypes[it.uuid.toString().toUpperCase()]
                characteristicType?.apply {
                    val characteristic = this.create(WeakReference(service), it)
                    service.characteristics[it.uuid.toString().toUpperCase()] = characteristic
                    characteristic.observers.add(this@BleSensor)
                    notifySensorCharacteristicDiscovered(characteristic)
                    notifyServiceFeaturesIdentified(this@BleSensor, service)
                }
            }
        }

    }

    protected open fun processCharacteristicValue(characteristic: BluetoothGattCharacteristic) {
        val service = services[characteristic.service.uuid.toString().toUpperCase()]
        val char = service?.characteristics?.get(characteristic.uuid.toString().toUpperCase())
        char?.valueUpdated()
        if(characteristic.uuid.toString().substring(4, 8) != "2ad2") {
//            Log.d(TAG, "char ${characteristic.uuid.toString().substring(4, 8)} val: ${characteristic.value?.contentToString()}")
        }
        markSensorActivity()
    }

    protected open fun characteristicValueWritten(characteristic: BluetoothGattCharacteristic) {
        val service = services[characteristic.service.uuid.toString().toUpperCase()]
        val char = service?.characteristics?.get(characteristic.uuid.toString().toUpperCase())
        char?.valueWritten()
    }

    /**
     *
     *     GATT Callbacks
     *
     **/

    private val gattCallbacks = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
//            Log.d(TAG, "onConnectionStateChange (${device?.name}) $newState status: $status")

            try {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        bluetoothGatt?.discoverServices()
                        rssiPingEnable = true
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        bluetoothGatt?.close()
                        bluetoothGatt = null
                        rssiPingEnable = false
                        services.clear()
                    }
                    BluetoothProfile.STATE_CONNECTING -> {
//                        Log.d(TAG, "Connecting to device")
                    }
                    BluetoothProfile.STATE_DISCONNECTING -> {
//                        Log.d(TAG, "Disconnecting from device")
                    }
                }
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
            notifyObserversStateChanged()
            markSensorActivity()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            processServices(gatt)
            markSensorActivity()
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            processCharacteristicValue(characteristic)
            markSensorActivity()
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            processCharacteristicValue(characteristic)
            operationQueue.remove()
            processQueue()
            markSensorActivity()
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            characteristicValueWritten(characteristic)
            operationQueue.remove()
            processQueue()
            markSensorActivity()
        }

        override fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            operationQueue.remove()
            processQueue()
            markSensorActivity()
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            operationQueue.remove()
            processQueue()
            markSensorActivity()
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            this@BleSensor.rssi = rssi
            operationQueue.remove()
            processQueue()
            markSensorActivity()
        }
    }
}