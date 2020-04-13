package com.lexicalninja.kotlinsensors.services

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.util.Log
import com.kinetic.fit.kotlinsensors.BleSensor
import com.kinetic.sdk.smartcontrol.ConfigData
import com.kinetic.sdk.smartcontrol.PowerData
import com.kinetic.sdk.smartcontrol.SmartControl
import com.lexicalninja.kotlinsensors.BleCharacteristic
import com.kinetic.fit.kotlinsensors.BleService
import com.lexicalninja.kotlinsensors.ICharacteristicFactory
import com.kinetic.fit.kotlinsensors.IServiceFactory
import java.lang.ref.WeakReference
import kotlin.properties.Delegates

const val SC_CONTROLPOINT_UPDATED = "SC_CONTROLPOINT_UPDATED"
open class SmartControlService(gattService: BluetoothGattService, sensor: WeakReference<out BleSensor>)
    : BleService(gattService, sensor) {

    interface SCSystemIdObserver {
        fun onScSystemId(bytes: ByteArray)
    }

    interface SCCalibrationObserver {
        fun scCalibrationStarted()
        fun scCalibrationStopped()
    }

    open class Factory : IServiceFactory {
        override val uuid = "E9410200-B434-446B-B5CC-36592FC4C724"
        override fun create(gattService: BluetoothGattService, sensor: WeakReference<out BleSensor>): BleService {
            return SmartControlService(gattService, sensor)
        }

        override val characteristicTypes: MutableMap<String, ICharacteristicFactory> = mutableMapOf(
                Measurement.factory().uuid to Measurement.factory(),
                ControlPoint.factory().uuid to ControlPoint.factory(),
                Configuration.factory().uuid to Configuration.factory(),
                DebugData.factory().uuid to DebugData.factory()
        )
    }

    companion object {
        fun factory() = Factory()
    }

    val scSysIdObservers = mutableSetOf<SCSystemIdObserver>()
    val scCalObservers = mutableSetOf<SCCalibrationObserver>()

    private var _systemIdCache: ByteArray? = null
    val systemId: ByteArray?
        get() {
            if (_systemIdCache == null) {
                sensor.get()?.service<DeviceInformationService>(DeviceInformationService.factory().uuid)
                        ?.characteristic<DeviceInformationService.SystemID>(DeviceInformationService.SystemID.factory().uuid)
                        ?.apply {
                            _systemIdCache = this.value
                            if (_systemIdCache == null) {
                                this.readValue()
                            } else {
                                this@SmartControlService.scSysIdObservers.forEach { it.onScSystemId(_systemIdCache!!) }
                            }
                        }
            }
            return _systemIdCache
        }

    private var targetWatts by Delegates.observable<Long?>(null) { _, old, new ->
        new?.takeIf { controlPoint != null }?.apply {

            try {
                val command = SmartControl.SetERGMode(this.toInt())
                controlPoint!!.apply {
                    gattCharacteristic.value = command
                    writeCharacteristic(this)
                }
            } catch (e: Exception) {
                Log.d("SC Service", e.localizedMessage)
            }
        }
    }

    val measurement: Measurement?
        get() = characteristic()

    val controlPoint: ControlPoint?
        get() = characteristic()

    val configuration: Configuration?
        get() = characteristic()

    val debug: DebugData?
        get() = characteristic()

    open fun setResistanceFluid(level: Int) {
        targetWatts = null
        controlPoint?.apply {
            try {
                val command = SmartControl.SetFluidMode(level)
                gattCharacteristic.value = command
                writeCharacteristic(this)
            } catch (e: Exception) {
                Log.d("SC Service", e.localizedMessage)
            }
        }
    }

    open fun setResistanceErg(targetWatts: Long) {
        if (this@SmartControlService.targetWatts == targetWatts) return
        else this@SmartControlService.targetWatts = targetWatts
    }

//    open fun setResistanceBrake(percent: Float) {
//        targetWatts = null
//        controlPoint?.apply {
//            try {
//                val command = SmartControl.
//            }
//        }
//    }

    open fun setSimMode(weight: Float, rollingResistance: Float, windResistance: Float, grade: Float, windSpeed: Float) {
        targetWatts = null
        controlPoint?.apply {
            try {
                val command = SmartControl.SetSimulationMode(weight, rollingResistance, windResistance, grade, windSpeed)
                gattCharacteristic.value = command
                writeCharacteristic(this)
            } catch (e: Exception) {
                Log.d("SC Service", e.localizedMessage)
            }
        }
    }

    open fun stopCalibration(): Boolean {
        scCalObservers.forEach { it.scCalibrationStopped() }
        controlPoint?.apply {
            return try {
                val command = SmartControl.StopCalibrationCommandData()
                gattCharacteristic.value = command
                writeCharacteristic(this)
                true
            } catch (e: Exception) {
                Log.d("SC Service", e.localizedMessage)
                false
            }
        }
        return false
    }

    open fun startCalibration(): Boolean {
        controlPoint?.apply {
            return try {
                val command = SmartControl.StartCalibrationCommandData()
                gattCharacteristic.value = command
                writeCharacteristic(this)
                scCalObservers.forEach { it.scCalibrationStarted() }
                true
            } catch (e: Exception) {
                Log.d("SC Service", e.localizedMessage)
                false
            }
        }
        return false
    }

    open fun writeSensorName(name: String) {
//        controlPoint?.apply {
//            try {
//                SmartControl.
//            }
//        }
    }

    open class Measurement(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {
        class Factory : ICharacteristicFactory {
            override val uuid = "E9410201-B434-446B-B5CC-36592FC4C724"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return Measurement(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }

        init {
            notify(true)
            service.get()?.sensor?.get()?.observers?.forEach {
                it.sensorServiceFeaturesIdentified(service.get()!!.sensor.get()!!, service.get()!!)
            }
        }

        open var powerData by Delegates.observable<PowerData?>(null) { _, old, new ->
            new?.takeIf { (service.get() as? SmartControlService)?.targetWatts != null }?.apply {
                if (mode == PowerData.ControlMode.ERG && targetResistance != (service.get() as? SmartControlService)?.targetWatts?.toInt()) {
                    return@apply
                } else {
                    (service.get() as? SmartControlService)?.targetWatts = null
                }
            }
        }


        override fun valueUpdated() {
            gattCharacteristic.value?.takeIf { (service.get() as? SmartControlService)?.systemId != null }?.apply {
                try {
                    powerData = SmartControl.ProcessPowerData(this, (service.get() as? SmartControlService)?.systemId)
                } catch (e: Exception) {
                    Log.d("SC Service", e.localizedMessage)
                }
            }
            super.valueUpdated()
        }
    }

    open class Configuration(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {
        class Factory : ICharacteristicFactory {
            override val uuid = "E9410202-B434-446B-B5CC-36592FC4C724"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return Configuration(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }

        init {
            notify(true)
            readValue()
        }

        open var configData: ConfigData? = null

        override fun valueUpdated() {
            gattCharacteristic.value?.apply {
                try {
                    configData = SmartControl.ProcessConfigurationData(this)
                } catch (e: Exception) {
                    Log.d("SC Service", e.localizedMessage)
                }
            }
            super.valueUpdated()
        }
    }

    open class ControlPoint(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {
        class Factory : ICharacteristicFactory {
            override val uuid = "E9410203-B434-446B-B5CC-36592FC4C724"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return ControlPoint(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
            val writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        }

        init {
            notify(true)
        }

        override fun valueWritten() {
//            service.get()?.sensor?.get()?.context?.sendBroadcast(Intent(SC_CONTROLPOINT_UPDATED))
            super.valueWritten()
        }

        override fun valueUpdated() {
//            service.get()?.sensor?.get()?.context?.sendBroadcast(Intent(SC_CONTROLPOINT_UPDATED))
            super.valueUpdated()
        }
    }

    open class DebugData(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {
        class Factory : ICharacteristicFactory {
            override val uuid = "E9410204-B434-446B-B5CC-36592FC4C724"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return DebugData(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }

        init {
            notify(true)
            val writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        }
    }

}