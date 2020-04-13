package com.lexicalninja.kotlinsensors.services

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import com.lexicalninja.kotlinsensors.BleSensor
import com.lexicalninja.kotlinsensors.serializers.KineticSerializer
import com.lexicalninja.kotlinsensors.BleCharacteristic
import com.lexicalninja.kotlinsensors.BleService
import com.lexicalninja.kotlinsensors.ICharacteristicFactory
import com.lexicalninja.kotlinsensors.IServiceFactory
import java.lang.ref.WeakReference

open class KineticService(gattService: BluetoothGattService, sensor: WeakReference<out BleSensor>)
    : BleService(gattService, sensor) {

    open class Factory : IServiceFactory {
        override val uuid = "E9410300-B434-446B-B5CC-36592FC4C724"
        override fun create(gattService: BluetoothGattService, sensor: WeakReference<out BleSensor>): BleService {
            return KineticService(gattService, WeakReference(sensor.get() as BleSensor))
        }

        override val characteristicTypes: MutableMap<String, ICharacteristicFactory> = mutableMapOf(
                Configuration.factory().uuid to Configuration.factory(),
                ControlPoint.factory().uuid to ControlPoint.factory(),
                Debug.factory().uuid to Debug.factory(),
                SystemWeight.factory().uuid to SystemWeight.factory()
        )
    }

    companion object {
        fun factory() = Factory()
    }

    val configuration: Configuration?
        get() = characteristic()
    val controlPoint: ControlPoint?
        get() = characteristic()
    val debug: Debug?
        get() = characteristic()
    val systemWeight: SystemWeight?
        get() = characteristic()

    fun writeSensorName(deviceName: String) {
        configuration?.apply {
            this.gattCharacteristic.value = KineticSerializer.setDeviceName(deviceName)
            writeCharacteristic(this)
        }
    }

    open class Configuration(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {
        class Factory : ICharacteristicFactory {
            override val uuid = "E9410301-B434-446B-B5CC-36592FC4C724"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return Configuration(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }

        init {
            notify(true)
        }

        var config: KineticSerializer.KineticConfig? = null

        override fun valueUpdated() {
            gattCharacteristic.value?.apply { config = KineticSerializer.readConfig(this) }
            super.valueUpdated()
        }
    }

    open class ControlPoint(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {
        class Factory : ICharacteristicFactory {
            override val uuid = "E9410302-B434-446B-B5CC-36592FC4C724"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return ControlPoint(service, gattCharacteristic)
            }
        }

        interface KineticControlPointObserver {
            fun controlPointUpdated()
        }

        val cpObservers = mutableSetOf<KineticControlPointObserver>()

        companion object {
            fun factory() = Factory()
        }

        init {
            notify(true)
        }

        var response: KineticSerializer.KineticControlPointResponse? = null

        override fun valueUpdated() {
            gattCharacteristic.value?.apply { response = KineticSerializer.readControlPointResponse(this) }
            cpObservers.forEach { it.controlPointUpdated() }
            super.valueUpdated()
        }
    }

    open class Debug(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {
        class Factory : ICharacteristicFactory {
            override val uuid = "E9410303-B434-446B-B5CC-36592FC4C724"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return Debug(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }

        init {
            notify(true)
        }

        var debugData: KineticSerializer.KineticDebugData? = null

        override fun valueUpdated() {
            gattCharacteristic.value?.apply { debugData = KineticSerializer.readDebugData(this) }
            super.valueUpdated()
        }
    }

    open class SystemWeight(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {
        class Factory : ICharacteristicFactory {
            override val uuid = "E9410304-B434-446B-B5CC-36592FC4C724"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return SystemWeight(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }

        init {
            readValue()
        }

        var weight: Int = 0
            private set

        override fun valueUpdated() {
            gattCharacteristic.value?.first()?.apply { weight = this.toInt() }
            super.valueUpdated()
        }
    }
}