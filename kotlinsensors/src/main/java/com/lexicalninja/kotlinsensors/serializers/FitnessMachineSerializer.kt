package com.lexicalninja.kotlinsensors.serializers

import android.util.Log
import com.lexicalninja.kotlinsensors.FlagStruct
import com.lexicalninja.kotlinsensors.FtmsPair
import kotlin.math.abs

/**
 * Created by Saxton on 1/9/18.
 */

const val EPSILON = 0.000000119

class FitnessMachineSerializer {

    object MachineFeaturesFlags {
        const val AverageSpeedSupported = 0
        const val CadenceSupported = 1
        const val TotalDistanceSupported = 2
        const val InclinationSupported = 3
        const val ElevationGainSupported = 4
        const val PaceSupported = 5
        const val StepCountSupported = 6
        const val ResistanceLevelSupported = 7
        const val StrideCountSupported = 8
        const val ExpendedEnergySupported = 9
        const val HeartRateMeasurementSupported = 10
        const val MetabolicEquivalentSupported = 11
        const val ElapsedTimeSupported = 12
        const val RemainingTimeSupported = 13
        const val PowerMeasurementSupported = 14
        const val ForceOnBeltAndPowerOutputSupported = 15
        const val UserDataRetentionSupported = 16
    }

    object TargetSettingFeaturesFlags {
        const val SpeedTargetSettingSupported = 0
        const val InclinationTargetSettingSupported = 1
        const val ResistanceTargetSettingSupported = 2
        const val PowerTargetSettingSupported = 3
        const val HeartRateTargetSettingSupported = 4
        const val TargetedExpendedEnergyConfigurationSupported = 5
        const val TargetedStepNumberConfigurationSupported = 6
        const val TargetedStrideNumberConfigurationSupported = 7
        const val TargetedDistanceConfigurationSupported = 8
        const val TargetedTrainingTimeConfigurationSupported = 9
        const val TargetedTimeInTwoHeartRateZonesConfigurationSupported = 10
        const val TargetedTimeInThreeHeartRateZonesConfigurationSupported = 11
        const val TargetedTimeInFiveHeartRateZonesConfigurationSupported = 12
        const val IndoorBikeSimulationParametersSupported = 13
        const val WheelCircumferenceConfigurationSupported = 14
        const val SpinDownControlSupported = 15
        const val TargetedCadenceConfigurationSupported = 16
    }

    object TrainerStatusFlags {
        const val TrainingStatusStringPresent = 0
        const val ExtendedStringPresent = 2
    }

    object IndoorBikeDataFlags {
        const val MoreData = 0
        const val AverageSpeedPresent = 1
        const val InstantaneousCadencePresent = 2
        const val AverageCadencePresent = 3
        const val TotalDistancePresent = 4
        const val ResistanceLevelPresent = 5
        const val InstantaneousPowerPresent = 6
        const val AveragePowerPresent = 7
        const val ExpendedEnergyPresent = 8
        const val HeartRatePresent = 9
        const val MetabolicEquivalentPresent = 10
        const val ElapsedTimePresent = 11
        const val RemainingTimePresent = 12
    }

    class IndoorBikeData(rawFlags: Int) : FlagStruct(rawFlags) {
        var instantaneousSpeed: Double? = null
        var averageSpeed: Double? = null
        var instantaneousCadence: Double? = null
        var averageCadence: Double? = null
        var totalDistance: Int? = null
        var resistanceLevel: Int? = null
        var instantaneousPower: Int? = null
        var averagePower: Int? = null
        var totalEnergy: Int? = null
        var energyPerHour: Int? = null
        var energyPerMinute: Int? = null
        var heartRate: Int? = null
        var metabolicEquivalent: Double? = null
        var elapsedTime: Int? = null
        var remainingTime: Int? = null
    }

    enum class TrainingStatusField(var bits: Int) {
        other(0x00),
        idle(0x01),
        warmingUp(0x02),
        lowIntensityInterval(0x03),
        highIntensityInterval(0x04),
        recoveryInterval(0x05),
        isometric(0x06),
        heartRateControl(0x07),
        fitnessTest(0x08),
        speedOutsideControlRegionLow(0x09),
        speedOutsideControlRegionHigh(0x0A),
        coolDown(0x0B), wattControl(0x0C),
        manualMode(0x0D), preWorkout(0x0E),
        postWorkout(0x0F);

        companion object {
            fun getFromBits(bits: Int): TrainingStatusField {
                for (field in values()) {
                    if (field.bits == bits) {
                        return field
                    }
                }
                return other
            }
        }
    }

    enum class MachineStatusOpCode(var bits: Int) {
        reservedForFutureUse(0x00),
        reset(0x01),
        stoppedOrPausedByUser(0x02),
        stoppedBySafetyKey(0x03),
        startedOrResumedByUser(0x04),
        targetSpeedChanged(0x05),
        targetInclineChanged(0x06),
        targetResistancLevelChanged(0x07),
        targetPowerChanged(0x08),
        targetHeartRateChanged(0x09),
        targetedExpendedEnergyChanged(0x0A),
        targetedNumberOfStepsChanged(0x0B),
        targetedNumberOfStridesChanged(0x0C),
        targetedDistanceChanged(0x0D),
        targetedTrainingTimeChanged(0x0E),
        targetedTimeInTwoHeartRateZonesChanged(0x0F),
        targetedTimeInThreeHeartRateZonesChanged(0x10),
        targetedTimeInFiveHeartRateZonesChanged(0x11),
        indoorBikeSimulationParametersChanged(0x12),
        wheelCircumferenceChanged(0x13),
        spinDownStatus(0x14),
        targetedCadenceChanged(0x15),
        controlPermissionLost(0xFF);

        companion object {
            fun getFromBits(bits: Int): MachineStatusOpCode {
                for (field in values()) {
                    if (field.bits == bits) {
                        return field
                    }
                }
                return reservedForFutureUse
            }
        }

    }

    enum class SpinDownStatus(var bits: Int) {
        reservedForFutureUse(0x00),
        spinDownRequested(0x01),
        success(0x02),
        error(0x03),
        stopPedaling(0x04);

        companion object {
            fun getFromBits(bits: Int): SpinDownStatus {
                for (field in values()) {
                    if (field.bits == bits) {
                        return field
                    }
                }
                return reservedForFutureUse
            }
        }

    }


    enum class ControlOpCode(var bits: Int) {
        requestControl(0x00), reset(0x01), setTargetSpeed(0x02), setTargetInclincation(0x03), setTargetResistanceLevel(0x04), setTargetPower(0x05), setTargetHeartRate(0x06), startOrResume(0x07), stopOrPause(0x08), setTargetedExpendedEnergy(0x09), setTargetedNumberOfSteps(0x0A), setTargetedNumberOfStrides(0x0B), setTargetedDistance(0x0C), setTargetedTrainingTime(0x0D), setTargetedTimeInTwoHeartRateZones(0x0E), setTargetedTimeInThreeHeartRateZones(0x0F), setTargetedTimeInFiveHeartRateZones(0x10), setIndoorBikeSimulationParameters(0x11), setWheelCircumference(0x12), spinDownControl(0x13), setTargetedCadence(0x14), responseCode(0x80), unknown(0xFF);

        companion object {
            fun getFromBits(bits: Int): ControlOpCode {
                for (field in values()) {
                    if (field.bits == bits) {
                        return field
                    }
                }
                return unknown
            }
        }

    }

    enum class ResultCode(var bits: Int) {
        reserved(0x00), success(0x01), opCodeNotSupported(0x02), invalidParameter(0x03), operationFailed(0x04), controlNotPermitted(0x05);

        companion object {
            fun getFromBits(bits: Int): ResultCode {
                for (field in values()) {
                    if (field.bits == bits) {
                        return field
                    }
                }
                return reserved
            }
        }

    }

    class Features(rawFlags: Int):FlagStruct(rawFlags)

    class TargetSettingFeatures(rawFlags: Int):FlagStruct(rawFlags)

    class TrainingStatus(rawFlags: Int) : FlagStruct(rawFlags) {
        var status = TrainingStatusField.other
        var statusString: String? = null
    }

    class ControlPointResponse {
        var requestOpCode = ControlOpCode.unknown
        var resultCode = ResultCode.opCodeNotSupported

        //        Target Speed Params when the request is SpinDownController
        var targetSpeedLow = 0.0
        var targetSpeedHigh = 0.0
    }

    class IndoorBikeSimulationParameters {
        var windSpeed = 0.0
        var grade = 0.0
        var crr = 0.0
        var crw = 0.0

        constructor()

        constructor(windSpeed: Double, grade: Double, crr: Double, crw: Double) {
            this.windSpeed = windSpeed
            this.grade = grade
            this.crr = crr
            this.crw = crw
        }

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is IndoorBikeSimulationParameters) return false
            return abs(windSpeed - other.windSpeed) <= EPSILON
                    && abs(grade - other.grade) <= EPSILON
                    && abs(crr - other.crr) <= EPSILON
                    && abs(crw - other.crw) <= EPSILON
        }

        override fun hashCode(): Int {
            var result = windSpeed.hashCode()
            result = 31 * result + grade.hashCode()
            result = 31 * result + crr.hashCode()
            result = 31 * result + crw.hashCode()
            return result
        }
    }


    class SupportedResistanceLevelRange {
        var minimumResistanceLevel = 0.0
        var maximumResistanceLevel = 0.0
        var minimumIncrement = 0.0
    }

    class SupportedPowerRange {
        var minimumPower = 0
        var maximumPower = 0
        var minumumIncrement = 0
    }

    class MachineStatusMessage {
        var opCode = MachineStatusOpCode.reservedForFutureUse
        var spinDownStatus: SpinDownStatus? = null
        var spinDownTime = 0.0
        var targetPower = 0
        var targetResistanceLevel = 0.0
        var targetSimParameters: IndoorBikeSimulationParameters? = null
    }


    companion object {
        fun readFeatures(bytes: ByteArray): FtmsPair<Int, Int> {
            val rawMachine = bytes[0].toInt() or (bytes[1].toInt() shl 8) or (bytes[2].toInt() shl 16) or (bytes[3].toInt() shl 24)
            val rawFeatures = bytes[4].toInt() or (bytes[5].toInt() shl 8) or (bytes[6].toInt() shl 16) or (bytes[7].toInt() shl 24)
            return FtmsPair(rawMachine, rawFeatures)
        }

        fun readTrainingStatus(bytes: ByteArray): TrainingStatus {
            val flags = bytes[0].toInt()
            val status = TrainingStatus(flags)
            if (bytes.size > 1) {
                status.status = TrainingStatusField.getFromBits(bytes[0].toInt())
            } else {
                status.status = TrainingStatusField.other
            }
            if (TrainerStatusFlags.TrainingStatusStringPresent in status) {
                // ToDo; parse bytes 2-16 into a string (UTF8)
                status.statusString = "ToDo"
            }
            return status
        }

        fun readControlPointResponse(bytes: ByteArray): ControlPointResponse? {
            var response: ControlPointResponse? = null
            if (bytes.size > 2 && ((bytes[0].toInt() and 0xFF) == ControlOpCode.responseCode.bits)) {
                response = ControlPointResponse()
                response.requestOpCode = ControlOpCode.getFromBits(bytes[1].toInt() and 0xFF)
                response.resultCode = ResultCode.getFromBits(bytes[2].toInt() and 0xFF)
                if (response.resultCode == ResultCode.success && response.requestOpCode == ControlOpCode.spinDownControl) {
//                If success and spindown control response, the target high / low speeds are tacked onto the end
                    Log.d("spindown", "success")
                    if (bytes.size > 6) {
                        response.targetSpeedLow = ((bytes[3].toInt() and 0xFF) or
                                ((bytes[4].toInt() and 0xFF) shl 8)).toDouble() / 100.0

                        response.targetSpeedHigh = ((bytes[5].toInt() and 0xFF) or
                                ((bytes[6].toInt() and 0xFF) shl 8)).toDouble() / 100.0
                    }
                }
            }
            return response
        }

        fun setIndoorBikeSimulationParameters(params: IndoorBikeSimulationParameters): ByteArray {
            // windSpeed = meters / second  res 0.001
            // grade = percentage           res 0.01
            // crr = unitless               res 0.0001
            // cw = kg / meter              res 0.01
            val mpsN = (params.windSpeed * 1000).toInt()
            val gradeN = (params.grade * 100).toInt()
            val crrN = (params.crr * 10000).toInt()
            val crwN = (params.crw * 100).toInt()
            return byteArrayOf(
                    ControlOpCode.setIndoorBikeSimulationParameters.bits.toByte(),
                    (mpsN and 0xff).toByte(),
                    (mpsN ushr 8 and 0xFF).toByte(),
                    (gradeN and 0xFF).toByte(),
                    (gradeN ushr 8 and 0xFF).toByte(),
                    crrN.toByte(),
                    crwN.toByte()
            )
        }

        fun requestControl(): ByteArray {
            return byteArrayOf(ControlOpCode.requestControl.bits.toByte())
        }

        fun reset(): ByteArray {
            return byteArrayOf((ControlOpCode.reset.bits and 0xFF).toByte())
        }

        fun startOrResume(): ByteArray {
            return byteArrayOf((ControlOpCode.startOrResume.bits and 0xFF).toByte())
        }

        fun stop(): ByteArray {
            return byteArrayOf(
                    (ControlOpCode.stopOrPause.bits and 0xFF).toByte(),
                    0x01.toByte()
            )
        }

        fun pause(): ByteArray {
            return byteArrayOf(
                    (ControlOpCode.stopOrPause.bits and 0xFF).toByte(),
                    0x02.toByte()
            )
        }

        fun setTargetResistanceLevel(level: Double): ByteArray {
//        level = unitless      res 0.1
            val levelN = level.toInt() * 10
            return byteArrayOf(
                    (ControlOpCode.stopOrPause.bits and 0xFF).toByte(),
                    (levelN and 0xFF).toByte(),
                    (levelN ushr 8 and 0xFF).toByte()
            )
        }

        fun setTargetPower(watts: Int): ByteArray {
            return byteArrayOf(
                    ControlOpCode.setTargetPower.bits.toByte(),
                    (watts and 0xFF).toByte(),
                    (watts ushr 8 and 0xFF).toByte()
            )
        }

        fun startSpinDownControl(): ByteArray {
            return byteArrayOf(
                    ControlOpCode.spinDownControl.bits.toByte(),
                    0x01.toByte()
            )
        }

        fun ignoreSpinDownControlRequest(): ByteArray {
            return byteArrayOf(
                    ControlOpCode.spinDownControl.bits.toByte(),
                    0x02.toByte()
            )
        }

        fun readIndoorBikeData(bytes: ByteArray): IndoorBikeData? {
            var bikeData: IndoorBikeData? = null
            if (bytes.isNotEmpty()) {
                var index = 0

                val rawFlags: Int = (bytes[index++].toInt() and 0xFF) or
                        ((bytes[index++].toInt() and 0xFF) shl 8)

                bikeData = IndoorBikeData(rawFlags)

                if (IndoorBikeDataFlags.MoreData !in bikeData) {
                    val value: Int = (bytes[index++].toInt() and 0xFF) or
                            ((bytes[index++].toInt() and 0xFF) shl 8)

                    bikeData.instantaneousSpeed = value.toDouble() / 100.0
                }
                if (IndoorBikeDataFlags.AverageSpeedPresent in bikeData) {
                    val value: Int = (bytes[index++].toInt() and 0xFF) or
                            ((bytes[index++].toInt() and 0xFF) shl 8)

                    bikeData.averageSpeed = value.toDouble() / 2.0
                }
                if (IndoorBikeDataFlags.InstantaneousCadencePresent in bikeData) {
                    val value: Int = (bytes[index++].toInt() and 0xFF) or
                            ((bytes[index++].toInt() and 0xFF) shl 8)

                    bikeData.instantaneousCadence = value.toDouble() / 2.0
                }
                if (IndoorBikeDataFlags.AverageCadencePresent in bikeData) {
                    val value: Int = (bytes[index++].toInt() and 0xFF) or
                            ((bytes[index++].toInt() and 0xFF) shl 8)

                    bikeData.averageCadence = value.toDouble() / 2.0
                }
                if (IndoorBikeDataFlags.TotalDistancePresent in bikeData) {
                    val value: Int = (bytes[index++].toInt() and 0xFF) or
                            ((bytes[index++].toInt() and 0xFF) shl 8) or
                            ((bytes[index++].toInt() and 0xFF) shl 16)

                    bikeData.totalDistance = value
                }
                if (IndoorBikeDataFlags.ResistanceLevelPresent in bikeData) {
                    val value: Int = (bytes[index++].toInt() and 0xFF) or
                            ((bytes[index++].toInt() and 0xFF) shl 8)

                    bikeData.resistanceLevel = value
                }
                if (IndoorBikeDataFlags.InstantaneousPowerPresent in bikeData) {
                    val value: Int = (bytes[index++].toInt() and 0xFF) or
                            ((bytes[index++].toInt() and 0xFF) shl 8)

                    bikeData.instantaneousPower = value
                }
                if (IndoorBikeDataFlags.AveragePowerPresent in bikeData) {
                    val value: Int = (bytes[index++].toInt() and 0xFF) or
                            ((bytes[index++].toInt() and 0xFF) shl 8)

                    bikeData.averagePower = value
                }
                if (IndoorBikeDataFlags.ExpendedEnergyPresent in bikeData) {
                    bikeData.totalEnergy = (bytes[index++].toInt() and 0xFF) or
                            ((bytes[index++].toInt() and 0xFF) shl 8)

                    bikeData.energyPerHour = (bytes[index++].toInt() and 0xFF) or
                            ((bytes[index++].toInt() and 0xFF) shl 8)

                    bikeData.energyPerMinute = (bytes[index++].toInt() and 0xFF)
                }
                if (IndoorBikeDataFlags.HeartRatePresent in bikeData) {
                    bikeData.heartRate = (bytes[index++].toInt() and 0xFF)
                }
                if (IndoorBikeDataFlags.MetabolicEquivalentPresent in bikeData) {
                    val value: Int = (bytes[index++].toInt() and 0xFF)
                    bikeData.metabolicEquivalent = value.toDouble() / 10.0
                }
                if (IndoorBikeDataFlags.ElapsedTimePresent in bikeData) {
                    bikeData.elapsedTime = (bytes[index++].toInt() and 0xFF) or
                            ((bytes[index++].toInt() and 0xFF) shl 8)
                }
                if (IndoorBikeDataFlags.RemainingTimePresent in bikeData) {
                    bikeData.remainingTime = (bytes[index++].toInt() and 0xFF) or
                            ((bytes[index].toInt() and 0xFF) shl 8)
                }
            }
            return bikeData
        }

        fun readSupportedResistanceLevelRange(bytes: ByteArray): SupportedResistanceLevelRange? {
            var response: SupportedResistanceLevelRange? = null
            if (bytes.isNotEmpty()) {
                response = SupportedResistanceLevelRange()
                val value1: Int = (bytes[0].toInt() and 0xFF) or ((bytes[1].toInt() and 0xFF) shl 8)
                val value2: Int = (bytes[2].toInt() and 0xFF) or ((bytes[3].toInt() and 0xFF) shl 8)
                val value3: Int = (bytes[4].toInt() and 0xFF) or ((bytes[5].toInt() and 0xFF) shl 8)
                response.minimumResistanceLevel = value1.toDouble() / 10.0
                response.maximumResistanceLevel = value2.toDouble() / 10.0
                response.minimumIncrement = value3.toDouble() / 10.0
            }
            return response
        }

        fun readSupportedPowerRange(bytes: ByteArray): SupportedPowerRange? {
            var response: SupportedPowerRange? = null
            if (bytes.isNotEmpty()) {
                response = SupportedPowerRange()
                response.minimumPower = (bytes[0].toInt() and 0xFF) or ((bytes[1].toInt() and 0xFF) shl 8)
                response.maximumPower = (bytes[2].toInt() and 0xFF) or ((bytes[3].toInt() and 0xFF) shl 8)
                response.minumumIncrement = (bytes[4].toInt() and 0xFF) or ((bytes[5].toInt() and 0xFF) shl 8)
            }
            return response
        }

        fun readMachineStatus(bytes: ByteArray): MachineStatusMessage {
            val message = MachineStatusMessage()
            if (bytes.isNotEmpty()) {
                message.opCode = MachineStatusOpCode.getFromBits(bytes[0].toInt() and 0xFF)
            }
            when (message.opCode) {
                MachineStatusOpCode.reservedForFutureUse -> {
                }
                MachineStatusOpCode.reset -> {
                }
                MachineStatusOpCode.stoppedOrPausedByUser -> {
                    if (bytes.size > 1) {
//                    0x01 = stop
//                    0x02 = pause
                    }
                }
                MachineStatusOpCode.stoppedBySafetyKey -> {
                }
                MachineStatusOpCode.startedOrResumedByUser -> {
                }
                MachineStatusOpCode.targetSpeedChanged -> {
                    if (bytes.size > 2) {
//                    UInt 16 km / hour w/ res 0.01
//                    message.targetSpeed = UInt16(bytes[1]) | UInt16(bytes[2]) << 8
                    }
                }
                MachineStatusOpCode.targetInclineChanged -> {
                    if (bytes.size > 2) {
//                    Int16 percent w/ res 0.1
//                    message.targetIncline = Int16(bytes[1]) | Int16(bytes[2]) << 8
                    }
                }
                MachineStatusOpCode.targetResistancLevelChanged -> {
                    if (bytes.size > 2) {
//                    ??? the spec cannot be correct here
//                    If we go by the Supported Resistance Level Range characteristic,
//                    this value *should* be a SInt16 w/ res 0.1
                        message.targetResistanceLevel = ((bytes[1].toInt() and 0xFF) or ((bytes[2].toInt() and 0xFF) shl 8)).toDouble()
                    }
                }
                MachineStatusOpCode.targetPowerChanged -> {
                    if (bytes.size > 2) {
//                    Int16 watts w/ res 1
                        message.targetPower = (bytes[1].toInt() and 0xFF) or ((bytes[2].toInt() and 0xFF) shl 8)
                    }
                }
                MachineStatusOpCode.targetHeartRateChanged -> {
                    if (bytes.size > 1) {
//                    UInt8 bpm w/ res 1
//                    message.targetHeartRate = bytes[1]
                    }
                }
                MachineStatusOpCode.targetedExpendedEnergyChanged -> {
                    if (bytes.size > 2) {
//                    UInt16 cals w/ res 1
//                    message.targetedExpendedEnergy = UInt16(bytes[1]) | UInt16(bytes[2]) << 8
                    }
                }
                MachineStatusOpCode.targetedNumberOfStepsChanged -> {
                    if (bytes.size > 2) {
//                    UInt16 steps w/ res 1
//                    message.targetedNumberOfSteps = UInt16(bytes[1]) | UInt16(bytes[2]) << 8
                    }
                }
                MachineStatusOpCode.targetedNumberOfStridesChanged -> {
                    if (bytes.size > 2) {
//                    UInt16 strides w/ res 1
//                    message.targetedNumberOfStrides = UInt16(bytes[1]) | UInt16(bytes[2]) << 8
                    }
                }
                MachineStatusOpCode.targetedDistanceChanged -> {
                    if (bytes.size > 3) {
//                    UInt24 meters w/ res 1
//                    message.targetedTrainingTime = UInt16(bytes[1]) | UInt16(bytes[2]) << 8
                    }
                }
                MachineStatusOpCode.targetedTrainingTimeChanged -> {
                    if (bytes.size > 2) {
//                    UInt16 seconds w/ res 1
                    }
                }
                MachineStatusOpCode.targetedTimeInTwoHeartRateZonesChanged -> {
                }
                MachineStatusOpCode.targetedTimeInThreeHeartRateZonesChanged -> {
                }
                MachineStatusOpCode.targetedTimeInFiveHeartRateZonesChanged -> {
                }
                MachineStatusOpCode.indoorBikeSimulationParametersChanged -> {
                    if (bytes.size > 6) {
                        val windSpeed = ((bytes[1].toInt() and 0xFF) or ((bytes[2].toInt() and 0xFF) shl 8)).toDouble() / 1000.0
                        val grade = ((bytes[3].toInt() and 0xFF) or ((bytes[4].toInt() and 0xFF) shl 8)).toDouble() / 100.0
                        val crr = ((bytes[5].toInt() and 0xFF)).toDouble() / 10000.0
                        val cwr = ((bytes[6].toInt() and 0xFF)).toDouble() / 100.0
                        message.targetSimParameters = IndoorBikeSimulationParameters(windSpeed, grade, crr, cwr)
                    }
                }
                MachineStatusOpCode.wheelCircumferenceChanged -> {
                    if (bytes.size > 2) {
//                    UInt16 mm w/ res 0.1
//                    message.wheelCircumferenceChanged = UInt16(bytes[1]) | UInt16(bytes[2]) << 8
                    }
                }
                MachineStatusOpCode.spinDownStatus -> {
                    if (bytes.size > 1) {
                        message.spinDownStatus = SpinDownStatus.getFromBits(bytes[1].toInt() and 0xFF)
                        if (message.spinDownStatus == SpinDownStatus.success && bytes.size > 3) {
//                        Milliseconds attached, convert to seconds
                            message.spinDownTime = ((bytes[2].toInt() and 0xFF) or ((bytes[3].toInt() and 0xFF) shl 8)).toDouble() / 1000.0
                        }
                        if (message.spinDownStatus == SpinDownStatus.error && bytes.size > 3) {
//                        Milliseconds attached, convert to seconds
                            message.spinDownTime = ((bytes[2].toInt() and 0xFF) or ((bytes[3].toInt() and 0xFF) shl 8)).toDouble() / 1000.0
                        }
                    }
                }
                MachineStatusOpCode.targetedCadenceChanged -> {
                    if (bytes.size > 2) {
//                    UInt16 rpms w/ res 0.5
//                    message.targetedCadence = UInt16(bytes[1]) | UInt16(bytes[2]) << 8
                    }
                }
                MachineStatusOpCode.controlPermissionLost -> {
                }
            }
            return message
        }
    }
}