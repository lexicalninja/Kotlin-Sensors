package com.kinetic.fit.kotlinsensors

import android.bluetooth.BluetoothGattService
import com.lexicalninja.kotlinsensors.BleCharacteristic
import com.lexicalninja.kotlinsensors.BleSensor
import com.lexicalninja.kotlinsensors.ICharacteristicFactory
import java.lang.ref.WeakReference
import kotlin.reflect.KClass


interface IServiceFactory {
    val uuid: String
    val characteristicTypes: MutableMap<String, ICharacteristicFactory>
    fun create(gattService: BluetoothGattService, sensor: WeakReference<out BleSensor>): BleService
}

open class BleService(var gattService: BluetoothGattService, var sensor: WeakReference<out BleSensor>) {

    var characteristics = mutableMapOf<String, BleCharacteristic>()
        internal set

    private val serviceType: KClass<out BleService>
        get() = this::class

    inline fun <reified T : BleCharacteristic?> characteristic(uuid: String? = null): T? {
        return if (uuid != null && characteristics[uuid] is T) {
            characteristics[uuid] as T
        } else {
            var c: T? = null
            characteristics.forEach {
                if (it.value is T) {
                    c = it.value as T
                    return@forEach
                }
            }
            c
        }
    }

    override fun equals(other: Any?): Boolean {
        return this.gattService.uuid == (other as? BleService)?.gattService?.uuid
    }

    override fun hashCode(): Int {
        var result = gattService.uuid.hashCode()
        result = 31 * result + sensor.hashCode()
        result = 31 * result + characteristics.hashCode()
        return result
    }
}