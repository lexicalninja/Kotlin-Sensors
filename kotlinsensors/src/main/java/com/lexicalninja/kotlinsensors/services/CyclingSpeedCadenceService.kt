package com.lexicalninja.kotlinsensors.services

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import com.lexicalninja.kotlinsensors.BleSensor
import com.lexicalninja.kotlinsensors.BleService
import com.lexicalninja.kotlinsensors.IServiceFactory
import com.lexicalninja.kotlinsensors.BleCharacteristic
import com.lexicalninja.kotlinsensors.ICharacteristicFactory
import com.lexicalninja.kotlinsensors.serializers.CyclingSerializer
import com.lexicalninja.kotlinsensors.serializers.CyclingSpeedCadenceSerializer
import java.lang.ref.WeakReference
import java.util.*
import kotlin.properties.Delegates

open class CyclingSpeedCadenceService(gattService: BluetoothGattService, sensor: WeakReference<out BleSensor>)
    : BleService(gattService, sensor) {
    class Factory : IServiceFactory {
        override val uuid = "00001816-0000-1000-8000-00805F9B34FB"
        override fun create(gattService: BluetoothGattService, sensor: WeakReference<out BleSensor>): BleService {
            return CyclingSpeedCadenceService(gattService, sensor)
        }

        override val characteristicTypes: MutableMap<String, ICharacteristicFactory> = mutableMapOf(
                Measurement.factory().uuid to Measurement.factory(),
                Feature.factory().uuid to Feature.factory(),
                SensorLocation.factory().uuid to SensorLocation.factory()
        )
    }

    val measurement: Measurement?
        get() = characteristic()
    val feature: Feature?
        get() = characteristic()
    val sensorLocation: SensorLocation?
        get() = characteristic()

    open class Measurement(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {
        class Factory : ICharacteristicFactory {
            override val uuid = "00002A5B-0000-1000-8000-00805F9B34FB"
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

        var speedKPH: Double? = null
            private set

        var crankRPM: Double? = null
            private set

        var wheelCircumferenceCM: Double = 213.3

        var measurementData by Delegates.observable<CyclingSpeedCadenceSerializer.MeasurementData?>(null) { _, old, new ->
            if (old == null || new == null) return@observable
            CyclingSerializer.calculateWheelKPH(new, old, wheelCircumferenceCM, 1024)
                    ?.apply { speedKPH = this }
            CyclingSerializer.calculateCrankRPM(new, old)?.apply { crankRPM = this }
        }

        override fun valueUpdated() {
            gattCharacteristic.value?.takeIf { it.isNotEmpty() }?.apply {
                // Certain sensors (*cough* Mio Velo *cough*) will send updates in bursts
                // so we're going to do a little filtering here to get a more stable reading
                val now = Date().time
                // calculate the expected interval of wheel events based on currentFirebaseAcct speed
                // This results in a small "bump" of speed typically at the end. need to fix that...
                var reqInterval = 0.8
                speedKPH?.apply {
                    val speedCMS = this * 27.77777777777778
                    // A slower speed of around 5 kph would expect a wheel event every 1.5 seconds.
                    // These values could probably use some tweaking ...
                    reqInterval = kotlin.math.max(0.5, kotlin.math.min(wheelCircumferenceCM / speedCMS * 0.9, 1.5))
                }
                if(measurementData == null || now - measurementData!!.timestamp > reqInterval){
                    measurementData =  CyclingSpeedCadenceSerializer.readMeasurement(this)
                }
            }
            super.valueUpdated()
        }
    }

    open class Feature(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {
        class Factory : ICharacteristicFactory {
            override val uuid = "00002A5C-0000-1000-8000-00805F9B34FB"
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

        var features: CyclingSpeedCadenceSerializer.Features? = null

        override fun valueUpdated() {
            gattCharacteristic.value?.takeIf { it.isNotEmpty() }?.apply {
                features = CyclingSpeedCadenceSerializer.readFeatures(this)
            }
            super.valueUpdated()
            service.get()?.apply {
                this.sensor.get()?.notifyServiceFeaturesIdentified(this.sensor.get()!!, this)
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

        override fun valueUpdated() {
            gattCharacteristic.value?.takeIf { it.isNotEmpty() }?.apply {
                location = CyclingSerializer.readSensorLocation(this)
            }
            super.valueUpdated()
        }
    }
    
}
