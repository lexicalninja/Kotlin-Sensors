package com.lexicalninja.kotlinsensors.services

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import com.lexicalninja.kotlinsensors.BleSensor
import com.lexicalninja.kotlinsensors.BleService
import com.lexicalninja.kotlinsensors.FTMS_UUID
import com.lexicalninja.kotlinsensors.IServiceFactory
import com.lexicalninja.kotlinsensors.BleCharacteristic
import com.lexicalninja.kotlinsensors.ICharacteristicFactory
import com.lexicalninja.kotlinsensors.serializers.FitnessMachineSerializer
import com.lexicalninja.kotlinsensors.serializers.FitnessMachineSerializer.MachineStatusOpCode.*
import java.lang.Double.MIN_VALUE
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.properties.Delegates

open class FitnessMachineService(gattService: BluetoothGattService, sensor: WeakReference<out BleSensor>)
    : BleService(gattService, sensor) {

    class Factory : IServiceFactory {
        override val uuid = FTMS_UUID
        override fun create(gattService: BluetoothGattService, sensor: WeakReference<out BleSensor>): BleService {
            return FitnessMachineService(gattService, sensor)
        }

        override val characteristicTypes: MutableMap<String, ICharacteristicFactory> = mutableMapOf(
                Feature.factory().uuid to Feature.factory(),
                ControlPoint.factory().uuid to ControlPoint.factory(),
                MachineStatus.factory().uuid to MachineStatus.factory(),
                TreadmillData.factory().uuid to TreadmillData.factory(),
                CrossTrainerData.factory().uuid to CrossTrainerData.factory(),
                StepClimberData.factory().uuid to StepClimberData.factory(),
                StairClimberData.factory().uuid to StairClimberData.factory(),
                RowerData.factory().uuid to RowerData.factory(),
                IndoorBikeData.factory().uuid to IndoorBikeData.factory(),
                TrainingStatus.factory().uuid to TrainingStatus.factory(),
                SupportedSpeedRange.factory().uuid to SupportedSpeedRange.factory(),
                SupportedInclinationRange.factory().uuid to SupportedInclinationRange.factory(),
                SupportedResistanceLevelRange.factory().uuid to SupportedResistanceLevelRange.factory(),
                SupportedPowerRange.factory().uuid to SupportedPowerRange.factory(),
                SupportedHeartRateRange.factory().uuid to SupportedHeartRateRange.factory()
        )
    }

    companion object {
        fun factory(): Factory = Factory()
    }


    open val feature: Feature?
        get() = characteristic()
    open val controlPoint: ControlPoint?
        get() = characteristic()
    open val machineStatus: MachineStatus?
        get() = characteristic()
    open val treadmillData: TreadmillData?
        get() = characteristic()
    open val crossTrainerData: CrossTrainerData?
        get() = characteristic()
    open val stepClimberData: StepClimberData?
        get() = characteristic()
    open val stairClimberData: StairClimberData?
        get() = characteristic()
    open val rowerData: RowerData?
        get() = characteristic()
    open val indoorBikeData: IndoorBikeData?
        get() = characteristic()
    open val trainingStatus: TrainingStatus?
        get() = characteristic()
    open val supportedSpeedRange: SupportedSpeedRange?
        get() = characteristic()
    open val supportedInclinationRange: SupportedInclinationRange?
        get() = characteristic()
    open val supportedResistanceLevelRange: SupportedResistanceLevelRange?
        get() = characteristic()
    open val supportedPowerRange: SupportedPowerRange?
        get() = characteristic()
    open val supportedHeartRateRange: SupportedHeartRateRange?
        get() = characteristic()


    open class Feature(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {

        class Factory : ICharacteristicFactory {
            override val uuid: String = "00002ACC-0000-1000-8000-00805F9B34FB"
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

        var machine: FitnessMachineSerializer.Features? = null
        var targetSettings: FitnessMachineSerializer.TargetSettingFeatures? = null

        override fun valueUpdated() {
            val value = gattCharacteristic.value
            value?.apply {
                val result = FitnessMachineSerializer.readFeatures(value)
                machine = FitnessMachineSerializer.Features(result.machine)
                targetSettings = FitnessMachineSerializer.TargetSettingFeatures(result.targetSettings)
            }
            super.valueUpdated()
            val serv = service.get()
            serv?.takeIf { it.sensor.get() != null }?.apply {
                this.sensor.get()!!.notifyServiceFeaturesIdentified(this.sensor.get()!!, this)
            }
        }
    }

    open class ControlPoint(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {
        init {
            indicate(true)
        }

        internal var pendingTargetPower: Long? = null
        internal var pendingTargetResistanceLevel: Double? = null
        internal var pendingTargetSimParameters: FitnessMachineSerializer.IndoorBikeSimulationParameters? = null


        class Factory : ICharacteristicFactory {
            override val uuid: String = "00002AD9-0000-1000-8000-00805F9B34FB"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return ControlPoint(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }

        var response: FitnessMachineSerializer.ControlPointResponse? = null
        var hasControl: Boolean = false

        override fun valueUpdated() {
            response = FitnessMachineSerializer.readControlPointResponse(gattCharacteristic.value)

            // check to see if we have control
            if (response?.requestOpCode == FitnessMachineSerializer.ControlOpCode.requestControl ) {
                hasControl = response?.resultCode == FitnessMachineSerializer.ResultCode.success
            } else if (response?.resultCode == FitnessMachineSerializer.ResultCode.controlNotPermitted){
                hasControl = false
            }

            super.valueUpdated()
        }

        open fun requestControl(): ByteArray {
            val bytes = FitnessMachineSerializer.requestControl()
            gattCharacteristic.value = bytes
            writeCharacteristic(this@ControlPoint)
            return bytes
        }

        open fun startOrResume(): ByteArray {
            val bytes = FitnessMachineSerializer.startOrResume()
            gattCharacteristic.value = bytes
            writeCharacteristic(this@ControlPoint)
            return bytes
        }

        open fun setTargetPower(watts: Long): ByteArray {
            val bytes = FitnessMachineSerializer.setTargetPower(watts.toInt())
            if (pendingTargetPower == watts) return bytes
            if ((service.get() as? FitnessMachineService)?.machineStatus?.message?.targetPower?.toLong() == watts) return bytes
            pendingTargetPower = watts
            gattCharacteristic.value = bytes
            writeCharacteristic(this@ControlPoint)
            return bytes
        }

        open fun setTargetResistanceLevel(level: Double): ByteArray {
            val bytes = FitnessMachineSerializer.setTargetResistanceLevel(level)

            pendingTargetResistanceLevel?.takeIf { abs(level - pendingTargetResistanceLevel!!) < MIN_VALUE }
                    ?.apply { return bytes }// skipping write, still waiting on MachineStatus Message before clearing

            (service.get() as? FitnessMachineService)?.machineStatus?.message?.targetResistanceLevel
                    ?.takeIf { abs(level - it) < MIN_VALUE }?.apply { return bytes }// skipping write, targetpower is already set

            pendingTargetResistanceLevel = level
            gattCharacteristic.value = bytes
            writeCharacteristic(this@ControlPoint)
            return bytes
        }

        open fun setIndoorBikeSimulationParameters(windSpeed: Double, grade: Double, crr: Double, crw: Double): ByteArray {
            val params = FitnessMachineSerializer.IndoorBikeSimulationParameters(windSpeed, grade, crr, crw)
            val bytes = FitnessMachineSerializer.setIndoorBikeSimulationParameters(params)
            // Prevent flooding the characteristic with unnecessary writes
            if (pendingTargetSimParameters == params) return bytes // skipping write, still waiting on MachineStatus Message before clearing
            if ((service.get() as? FitnessMachineService)?.machineStatus?.message?.targetSimParameters == params) {
                // skipping write, targetpower is already set
                return bytes
            }
            pendingTargetSimParameters = params
            gattCharacteristic.value = bytes
            writeCharacteristic(this@ControlPoint)
            return bytes
        }

        open fun startSpinDownProcess(): ByteArray {
            val bytes = FitnessMachineSerializer.startSpinDownControl()
            gattCharacteristic.value = bytes
            writeCharacteristic(this@ControlPoint)
            return bytes
        }

        open fun ignoreSpindownRequest(): ByteArray {
            val bytes = FitnessMachineSerializer.ignoreSpinDownControlRequest()
            gattCharacteristic.value = bytes
            writeCharacteristic(this@ControlPoint)
            return bytes
        }
    }

    open class MachineStatus(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {

        init {
            notify(true)
        }

        class Factory : ICharacteristicFactory {
            override val uuid: String = "00002ADA-0000-1000-8000-00805F9B34FB"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return MachineStatus(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }

        var message by Delegates.observable<FitnessMachineSerializer.MachineStatusMessage?>(null) { _, _, new ->
            new?.run {
                when (this.opCode) {
                    targetPowerChanged -> (service.get() as? FitnessMachineService)?.controlPoint?.pendingTargetPower = null
                    targetResistancLevelChanged ->
                        (service.get() as? FitnessMachineService)?.controlPoint?.pendingTargetResistanceLevel = null
                    indoorBikeSimulationParametersChanged ->
                        (service.get() as? FitnessMachineService)?.controlPoint?.pendingTargetSimParameters = null
                    else -> return@run
                }
            }
        }

        override fun valueUpdated() {
            message = FitnessMachineSerializer.readMachineStatus(gattCharacteristic.value)
            super.valueUpdated()
        }
    }

    open class TrainingStatus(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {

        init {
            notify(true)
            readValue()
        }

        class Factory : ICharacteristicFactory {
            override val uuid: String = "00002AD3-0000-1000-8000-00805F9B34FB"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return TrainingStatus(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }

        var data: FitnessMachineSerializer.TrainingStatus? = null
        override fun valueUpdated() {
            data = FitnessMachineSerializer.readTrainingStatus(gattCharacteristic.value)
            super.valueUpdated()
        }
    }

    open class TreadmillData(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {

        init {
            notify(true)
        }

        class Factory : ICharacteristicFactory {
            override val uuid: String = "00002ACD-0000-1000-8000-00805F9B34FB"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return TreadmillData(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }

        override fun valueUpdated() {
//        todo: deserialize value
            super.valueUpdated()
        }
    }

    open class CrossTrainerData(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {

        init {
            notify(true)
        }

        class Factory : ICharacteristicFactory {
            override val uuid: String = "00002ACE-0000-1000-8000-00805F9B34FB"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return CrossTrainerData(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }

        override fun valueUpdated() {
//        todo: deserialize value
            super.valueUpdated()
        }
    }

    open class StepClimberData(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {

        init {
            notify(true)
        }

        class Factory : ICharacteristicFactory {
            override val uuid: String = "00002ACF-0000-1000-8000-00805F9B34FB"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return StepClimberData(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }

        override fun valueUpdated() {
//        todo: deserialize value
            super.valueUpdated()
        }
    }

    open class StairClimberData(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {

        init {
            notify(true)
        }

        class Factory : ICharacteristicFactory {
            override val uuid: String = "00002AD0-0000-1000-8000-00805F9B34FB"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return StairClimberData(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }

        override fun valueUpdated() {
//        todo: deserialize value
            super.valueUpdated()
        }
    }

    open class RowerData(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {

        init {
            notify(true)
        }

        class Factory : ICharacteristicFactory {
            override val uuid: String = "00002AD1-0000-1000-8000-00805F9B34FB"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return RowerData(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }

        override fun valueUpdated() {
//        todo: deserialize value
            super.valueUpdated()
        }
    }

    open class IndoorBikeData(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {

        init {
            notify(true)
        }

        class Factory : ICharacteristicFactory {
            override val uuid: String = "00002AD2-0000-1000-8000-00805F9B34FB"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return IndoorBikeData(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }

        var data by Delegates.observable<FitnessMachineSerializer.IndoorBikeData?>(null) { _, old, _ ->
            if (old == null) {
                service.get()?.sensor?.get()?.apply{
                    this.notifyServiceFeaturesIdentified(this, service.get()!!)
                }
            }
        }

        override fun valueUpdated() {
            data = FitnessMachineSerializer.readIndoorBikeData(gattCharacteristic.value)
            super.valueUpdated()
        }
    }

    open class SupportedSpeedRange(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {

        init {
            notify(true)
        }

        class Factory : ICharacteristicFactory {
            override val uuid: String = "00002AD4-0000-1000-8000-00805F9B34FB"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return SupportedSpeedRange(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }

        override fun valueUpdated() {
//       todo: deserialize this value
            super.valueUpdated()
        }
    }

    open class SupportedInclinationRange(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {

        init {
            notify(true)
        }

        class Factory : ICharacteristicFactory {
            override val uuid: String = "00002AD5-0000-1000-8000-00805F9B34FB"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return SupportedInclinationRange(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }

        override fun valueUpdated() {
//       todo: deserialize this value
            super.valueUpdated()
        }
    }

    open class SupportedResistanceLevelRange(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {

        init {
            notify(true)
            readValue()
        }

        class Factory : ICharacteristicFactory {
            override val uuid: String = "00002AD6-0000-1000-8000-00805F9B34FB"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return SupportedResistanceLevelRange(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }

        var data: FitnessMachineSerializer.SupportedResistanceLevelRange? = null
        override fun valueUpdated() {
            data = FitnessMachineSerializer.readSupportedResistanceLevelRange(gattCharacteristic.value)
            super.valueUpdated()
        }

        fun convert(percent: Double): Double {
            return data?.run {
                return if (maximumResistanceLevel >= 0) {
                    minimumResistanceLevel + (percent * (maximumResistanceLevel - minimumResistanceLevel))
                } else {
                    val absMax = max(abs(minimumResistanceLevel), maximumResistanceLevel)
                    max(minimumResistanceLevel, min(percent * absMax, maximumResistanceLevel))
                }
            } ?: 0.0
        }
    }

    open class SupportedHeartRateRange(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {

        init {
            readValue()
        }

        class Factory : ICharacteristicFactory {
            override val uuid: String = "00002AD7-0000-1000-8000-00805F9B34FB"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return SupportedHeartRateRange(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }

        override fun valueUpdated() {
//       todo: deserialize this value
            super.valueUpdated()
        }
    }

    open class SupportedPowerRange(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : BleCharacteristic(service, gattCharacteristic) {

        init {
            notify(true)
        }

        class Factory : ICharacteristicFactory {
            override val uuid: String = "00002AD8-0000-1000-8000-00805F9B34FB"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return SupportedPowerRange(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }

        var data: FitnessMachineSerializer.SupportedPowerRange? = null
        override fun valueUpdated() {
            data = FitnessMachineSerializer.readSupportedPowerRange(gattCharacteristic.value)
            super.valueUpdated()
        }
    }
}