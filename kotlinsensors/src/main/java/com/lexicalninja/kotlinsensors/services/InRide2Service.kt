package com.lexicalninja.kotlinsensors.services

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import com.kinetic.fit.kotlinsensors.*
import com.kinetic.sdk.inride.ConfigData
import com.kinetic.sdk.inride.InRide
import com.kinetic.sdk.inride.PowerData
import com.kinetic.sdk.smartcontrol.SmartControl
import com.lexicalninja.kotlinsensors.BleCharacteristic
import com.lexicalninja.kotlinsensors.ICharacteristicFactory
import java.lang.ref.WeakReference
import kotlin.properties.Delegates

interface IR2SystemIdObserver {
    fun onir2SystemId(bytes: ByteArray)
}

interface IR2CalibrationObserver {
    fun ir2CalibrationStarted()
    fun ir2CalibrationStopped()
    fun onClaibrationResult(inRide: InRide2Service, lastSpindownTime: Double, calibrationResult: PowerData.SensorCalibrationResult)
}

open class InRide2Service(gattService: BluetoothGattService, sensor: WeakReference<out BleSensor>)
    : BleService(gattService, sensor) {
    open class Factory : IServiceFactory {
        override val uuid = "E9410100-B434-446B-B5CC-36592FC4C724"
        override fun create(gattService: BluetoothGattService, sensor: WeakReference<out BleSensor>): BleService {
            return InRide2Service(gattService, sensor)
        }

        override val characteristicTypes: MutableMap<String, ICharacteristicFactory> = mutableMapOf(
                Measurement.factory().uuid to Measurement.factory(),
                Configuration.factory().uuid to Configuration.factory(),
                ControlPoint.factory().uuid to ControlPoint.factory()
        )
    }

    companion object {
        fun factory() = Factory()
    }

    init {

    }

    var lastSuccessfulSpindownDuration: Double = 0.0
    var lastSpindownDuration: Double = 0.0
    val lastSpindownResult: PowerData.SensorCalibrationResult
        get() = measurement?.powerData?.calibrationResult
                ?: PowerData.SensorCalibrationResult.Unknown
    val inRideState: PowerData.SensorState
        get() = measurement?.powerData?.state ?: PowerData.SensorState.Normal
    val measurement: Measurement?
        get() = characteristic()
    val controlPoint: ControlPoint?
        get() = characteristic()
    val configuration: Configuration?
        get() = characteristic()
    val ir2SysIdObservers = mutableSetOf<IR2SystemIdObserver>()
    val ir2CalObservers = mutableSetOf<IR2CalibrationObserver>()
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
                                this@InRide2Service.ir2SysIdObservers.forEach { it.onir2SystemId(_systemIdCache!!) }
                            }
                        }
            }
            return _systemIdCache
        }

    open fun stopCalibration(): Boolean {
        ir2CalObservers.forEach { it.ir2CalibrationStopped() }
        return if (systemId != null && controlPoint != null && inRideState != PowerData.SensorState.Normal) {
            val command = InRide.StopCalibrationCommandData(systemId)
            controlPoint!!.gattCharacteristic.value = command
            controlPoint!!.writeCharacteristic(controlPoint!!)
            true
        } else false
    }

    open fun startCalibration(): Boolean {
        return if (systemId != null && controlPoint != null && inRideState == PowerData.SensorState.Normal) {
            val command = InRide.StartCalibrationCommandData(systemId)
            controlPoint!!.gattCharacteristic.value = command
            controlPoint!!.writeCharacteristic(controlPoint!!)
            ir2CalObservers.forEach { it.ir2CalibrationStarted() }
            true
        } else false
    }

    open fun setUpdateRate(rate: ConfigData.SensorUpdateRate): Boolean {
        if (systemId == null || controlPoint == null) return false
        val command = InRide.ConfigureSensorCommandData(rate, systemId)
        controlPoint!!.gattCharacteristic.value = command
        controlPoint!!.writeCharacteristic(controlPoint!!)
        return true
    }

    open fun writeSensorName(name: String) {
        systemId?.run {
            val command = InRide.SetPeripheralNameCommandData(name, this)
            controlPoint?.gattCharacteristic?.value = command
            controlPoint?.writeCharacteristic(controlPoint!!)
        }
    }

    open class Measurement(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {
        class Factory : ICharacteristicFactory {
            override val uuid = "E9410101-B434-446B-B5CC-36592FC4C724"
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

        open var powerData by Delegates.observable<PowerData?>(null) { _, old, new ->
            (service.get() as? InRide2Service)?.takeIf { new != null }?.apply {
                if (new?.calibrationResult == PowerData.SensorCalibrationResult.Success) {
                    lastSuccessfulSpindownDuration = new.spindownTime
                }

                if (new?.state == PowerData.SensorState.Normal && lastSpindownDuration != new.lastSpindownResultTime) {
                    lastSpindownDuration = new.lastSpindownResultTime
                    ir2CalObservers.forEach { it.onClaibrationResult(this, new.lastSpindownResultTime, new.calibrationResult) }
                }
            }
        }

        override fun valueUpdated() {
            gattCharacteristic.value?.takeIf { (service.get() as? InRide2Service)?.systemId != null }?.apply {
                powerData = InRide.ProcessPowerData(this, (service.get() as InRide2Service).systemId)
            }
            super.valueUpdated()
        }
    }

    open class Configuration(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic) :
            BleCharacteristic(service, gattCharacteristic) {
        class Factory : ICharacteristicFactory {
            override val uuid = "E9410104-B434-446B-B5CC-36592FC4C724"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return Configuration(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }

        init {
            readValue()
        }

        open var configData: ConfigData? = null

        override fun valueUpdated() {
            gattCharacteristic.value?.apply {
                configData = InRide.ProcessConfigurationData(this)
            }
            super.valueUpdated()
        }
    }

    open class ControlPoint(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {
        class Factory : ICharacteristicFactory {
            override val uuid = "E9410102-B434-446B-B5CC-36592FC4C724"
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

//        todo: process valueUpdated for control point

    }

}