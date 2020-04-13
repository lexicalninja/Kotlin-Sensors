package com.lexicalninja.kotlinsensors

import android.bluetooth.BluetoothGattCharacteristic
import com.kinetic.fit.kotlinsensors.BleService
import java.lang.ref.WeakReference
import java.nio.charset.Charset

interface ICharacteristicObserver {
    fun onValueUpdated(characteristic: BleCharacteristic)
    fun onValueWritten(characteristic: BleCharacteristic)
}

interface ICharacteristicFactory {
    val uuid: String
    fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic
}

open class BleCharacteristic(var service: WeakReference<BleService>, var gattCharacteristic: BluetoothGattCharacteristic) {
    var valueUpdatedTimestamp = Int.MAX_VALUE
        private set
    var valueWrittenTimestamp = Int.MAX_VALUE
        private set

    var observers = mutableSetOf<ICharacteristicObserver>()

    var value: ByteArray? = null
        get() = gattCharacteristic.value

    init {
        value = gattCharacteristic.value
    }
    open fun valueUpdated() {
        valueUpdatedTimestamp = Int.MAX_VALUE
        observers.forEach { it.onValueUpdated(this) }
    }

    open fun valueWritten() {
        valueWrittenTimestamp = Int.MAX_VALUE
        observers.forEach { it.onValueWritten(this) }
    }

    fun readValue() {
       service.get()?.sensor?.get()?.readCharacteristic(gattCharacteristic)
    }

    fun notify(notify: Boolean) {
        val n = service.get()?.sensor?.get()?.setNotifyForCharacteristic(gattCharacteristic, notify)
//        val descriptor = gattCharacteristic.getDescriptor(uuid.fromString(NOTIFICATION_DESCRIPTOR))
//        descriptor?.value = if(notify) ENABLE_NOTIFICATION_VALUE else DISABLE_NOTIFICATION_VALUE
//        service.get()?.sensor?.get()?.writeDescriptor(descriptor)
    }

    fun indicate(notify: Boolean) {
        service.get()?.sensor?.get()?.setIndicateForCharacteristic(gattCharacteristic, notify)
    }

    open fun writeCharacteristic(characteristic: BleCharacteristic) {
        service.get()?.sensor?.get()?.writeCharacteristic(gattCharacteristic)
    }



}

open class UTF8Characteristic(service: WeakReference<BleService>,
                              gattCharacteristic: BluetoothGattCharacteristic)
    : BleCharacteristic(service, gattCharacteristic) {
    var stringValue: String? = null
        get() = if (value != null && value!!.isNotEmpty()) String(value!!, Charset.forName("UTF-8")) else null

    init {
        readValue()
    }


}