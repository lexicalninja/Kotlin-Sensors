package com.lexicalninja.kotlinsensors.services

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import com.lexicalninja.kotlinsensors.BleSensor
import com.kinetic.fit.kotlinsensors.BleService
import com.kinetic.fit.kotlinsensors.IServiceFactory
import com.lexicalninja.kotlinsensors.BleCharacteristic
import com.lexicalninja.kotlinsensors.ICharacteristicFactory
import com.lexicalninja.kotlinsensors.serializers.CyclingPowerSerializer
import com.lexicalninja.kotlinsensors.serializers.CyclingSerializer
import java.lang.ref.WeakReference
import kotlin.properties.Delegates

open class CyclingPowerService(gattService: BluetoothGattService, sensor: WeakReference<out BleSensor>)
    : BleService(gattService, sensor) {
    open class Factory : IServiceFactory {
        override val uuid = "00001818-0000-1000-8000-00805F9B34FB"
        override fun create(gattService: BluetoothGattService, sensor: WeakReference<out BleSensor>): BleService {
            return CyclingPowerService(gattService, sensor)
        }

        override val characteristicTypes: MutableMap<String, ICharacteristicFactory> = mutableMapOf(
                Measurement.factory().uuid to Measurement.factory(),
                Feature.factory().uuid to Feature.factory(),
                SensorLocation.factory().uuid to SensorLocation.factory(),
                ControlPoint.factory().uuid to ControlPoint.factory()
        )
    }

    val measurement: Measurement?
        get() = characteristic()
    val feature: Feature?
        get() = characteristic()
    val sensorLocation: SensorLocation?
        get() = characteristic()
    val controlPoint: ControlPoint?
        get() = characteristic()

    open class Measurement(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {
        class Factory : ICharacteristicFactory {
            override val uuid = "00002A63-0000-1000-8000-00805F9B34FB"
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

        var instantaneousPower: Int? = null
            private set

        var speedKPH: Double? = null
            private set

        var crankRPM: Double? = null
            private set

        var wheelCircumferenceCM: Double = 213.3

        var measurementData by Delegates.observable<CyclingPowerSerializer.MeasurementData?>(null) { _, old, new ->
            new?.apply {
                this@Measurement.instantaneousPower = this.instantaneousPower.toInt()
            } ?: return@observable

            old?.apply {
                speedKPH = CyclingSerializer.calculateWheelKPH(new, old, wheelCircumferenceCM, 2048)
                crankRPM = CyclingSerializer.calculateCrankRPM(new, old)
            }
        }

        override fun valueUpdated() {
            gattCharacteristic.value?.takeIf { !it.isEmpty() }?.apply {
                measurementData = CyclingPowerSerializer.readMeasurement(this)
            }
            super.valueUpdated()
        }
    }

    open class Feature(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {
        class Factory : ICharacteristicFactory {
            override val uuid = "00002A65-0000-1000-8000-00805F9B34FB"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return Feature(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }

        init {
            readValue()
        }

        var features: CyclingPowerSerializer.Features? = null
            private set

        override fun valueUpdated() {
            gattCharacteristic.value?.takeIf { !it.isEmpty() }?.apply {
                features = CyclingPowerSerializer.readFeatures(value)
            }
            super.valueUpdated()
            service.get()?.apply {
                sensor.get()?.notifyServiceFeaturesIdentified(sensor.get()!!, this)
            }
        }
    }

    open class SensorLocation(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {
        class Factory : ICharacteristicFactory {
            override val uuid = "00002A5D-0000-1000-8000-00805F9B34FB"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return SensorLocation(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }

        init {
            readValue()
        }

        var location: CyclingSerializer.SensorLocation? = null
            private set

        override fun valueUpdated() {
            gattCharacteristic.value?.takeIf { !it.isEmpty() }?.apply {
                location = CyclingSerializer.readSensorLocation(this)
            }
            super.valueUpdated()
        }
    }

    open class ControlPoint(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {
        class Factory : ICharacteristicFactory {
            override val uuid = "00002A66-0000-1000-8000-00805F9B34FB"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return ControlPoint(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
            val writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        }

        init {
            indicate(true)
        }

        override fun valueUpdated() {
//           todo: process this response
            super.valueUpdated()
        }
    }
}
