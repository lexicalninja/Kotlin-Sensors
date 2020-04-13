package com.lexicalninja.kotlinsensors.services

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import com.kinetic.fit.kotlinsensors.*
import com.lexicalninja.kotlinsensors.serializers.HeartRateSerializer
import com.lexicalninja.kotlinsensors.BleCharacteristic
import com.lexicalninja.kotlinsensors.ICharacteristicFactory
import java.lang.ref.WeakReference

open class HeartRateService(gattService: BluetoothGattService, sensor: WeakReference<out BleSensor>)
    : BleService(gattService, sensor) {
    open class Factory : IServiceFactory {
        override val uuid = "0000180D-0000-1000-8000-00805F9B34FB"
        override fun create(gattService: BluetoothGattService, sensor: WeakReference<out BleSensor>): BleService {
            return HeartRateService(gattService, sensor)
        }

        override val characteristicTypes: MutableMap<String, ICharacteristicFactory> = mutableMapOf(
                Measurement.factory().uuid to Measurement.factory(),
                BodySensorLocation.factory().uuid to BodySensorLocation.factory(),
                ControlPoint.factory().uuid to ControlPoint.factory()
        )
    }

    companion object {
        fun factory() = Factory()
    }

    val measurement: Measurement?
        get() = characteristic()
    val bodySensorLocation: BodySensorLocation?
        get() = characteristic()
    val controlPoint: ControlPoint?
        get() = characteristic()

    open class Measurement(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {
        class Factory : ICharacteristicFactory {
            override val uuid = "00002A37-0000-1000-8000-00805F9B34FB"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return Measurement(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }

        init {
            notify(true)
        }

        var currentMeasurement: HeartRateSerializer.MeasurementData? = null

        @ExperimentalUnsignedTypes
        override fun valueUpdated() {
            gattCharacteristic.value?.apply {
                currentMeasurement = HeartRateSerializer.readMeasurement(this.toUByteArray())
            }
            super.valueUpdated()
        }
    }


    open class BodySensorLocation(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {
        class Factory : ICharacteristicFactory {
            override val uuid = "00002A38-0000-1000-8000-00805F9B34FB"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return BodySensorLocation(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }

        init {
            readValue()
        }

        var location: HeartRateSerializer.BodySensorLocation? = null
            private set

        override fun valueUpdated() {
            gattCharacteristic.value?.apply {
                location = HeartRateSerializer.readSensorLocation(this)
            }
            super.valueUpdated()
        }
    }

    open class ControlPoint(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {
        class Factory : ICharacteristicFactory {
            override val uuid = "00002A39-0000-1000-8000-00805F9B34FB"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return ControlPoint(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }

        init {
            notify(true)
        }

        override fun valueUpdated() {
//            TODO: Unsure what value is read from the CP after we reset the energy expended (not documented?)
            super.valueUpdated()
        }

        open fun resetEnergyExpended() {
            gattCharacteristic.value = HeartRateSerializer.writeResetEnergyExpended()
            writeCharacteristic(this@ControlPoint)        }

    }
}