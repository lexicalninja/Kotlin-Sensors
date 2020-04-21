package com.lexicalninja.kotlinsensors.serializers

import com.lexicalninja.kotlinsensors.FlagStruct
import com.lexicalninja.kotlinsensors.serializers.CyclingPowerSerializer.FeaturesFlags.CrankRevolutionDataSupported
import com.lexicalninja.kotlinsensors.serializers.CyclingPowerSerializer.FeaturesFlags.WheelRevolutionDataSupported

/**
 * Created by Saxton on 7/27/16.
 */
class CyclingPowerSerializer {
    object MeasurementFlags {
        const val pedalPowerBalancePresent = 0
        const val AccumulatedTorquePresent = 2
        const val WheelRevolutionDataPresent = 4
        const val CrankRevolutionDataPresent = 5
        const val ExtremeForceMagnitudesPresent = 6
        const val ExtremeTorqueMagnitudesPresent = 7
        const val ExtremeAnglesPresent = 8
        const val TopDeadSpotAnglePresent = 9
        const val BottomDeadSpotAnglePresent = 10
        const val AccumulatedEnergyPresent = 11
        const val OffsetCompensationIndicator = 12
    }

    object FeaturesFlags {
        const val PedalPowerBalanceSupported = 0
        const val AccumulatedTorqueSupported = 1
        const val WheelRevolutionDataSupported = 2
        const val CrankRevolutionDataSupported = 3
        const val ExtremeMagnitudesSupported = 4
        const val ExtremeAnglesSupported = 5
        const val TopAndBottomDeadSpotAnglesSupported = 6
        const val AccumulatedEnergySupported = 7
        const val OffsetCompensationIndicatorSupported = 8
        const val OffsetCompensationSupported = 9
        const val ContentMaskingSupported = 10
        const val MultipleSensorLocationsSupported = 11
        const val CrankLengthAdjustmentSupported = 12
        const val ChainLengthAdjustmentSupported = 13
        const val ChainWeightAdjustmentSupported = 14
        const val SpanLengthAdjustmentSupported = 15
        const val SensorMeasurementContext = 16
        const val InstantaneousMeasurementDirectionSupported = 17
        const val FactoryCalibrationDateSupported = 18
    }

    class Features(rawFlags: Int) : FlagStruct(rawFlags) {
        val isWheelRevolutionDataSupported: Boolean
            get() = WheelRevolutionDataSupported in this

        val isCrankRevolutionDataSupported: Boolean
            get() = CrankRevolutionDataSupported in this

    }

    class MeasurementData : CyclingMeasurementData {
        var instantaneousPower: Short = 0
        var pedalPowerBalance: Byte? = null
        var pedalPowerBalanceReference = false
        var accumulatedTorque: Short? = null
        override var timestamp = 0.0
        override var cumulativeWheelRevolutions: Int? = null
        override var lastWheelEventTime: Short? = null
        override var cumulativeCrankRevolutions: Int? = null
        override var lastCrankEventTime: Int? = null
        var maximumForceMagnitude: Short? = null
        var minimumForceMagnitude: Short? = null
        var maximumTorqueMagnitude: Short? = null
        var minimumTorqueMagnitude: Short? = null
        var maximumAngle: Short? = null
        var minimumAngle: Short? = null
        var topDeadSpotAngle: Short? = null
        var bottomDeadSpotAngle: Short? = null
        var accumulatedEnergy: Short? = null
    }


    companion object {
        fun readFeatures(bytes: ByteArray): Features {
            val rawFeatures: Int = (bytes[0].toInt() and 0xFFFF) or
                    ((bytes[1].toInt() and 0xFFFF) shl 8) or
                    ((bytes[2].toInt() and 0xFFFF) shl 16) or
                    ((bytes[3].toInt() and 0xFFFF) shl 24)
            return Features(rawFeatures)
        }

        fun readMeasurement(bytes: ByteArray): MeasurementData? {
            var measurement: MeasurementData? = null
            if (bytes.isNotEmpty()) {
                measurement = MeasurementData()
                var index = 0
                val rawFlags: Int = (bytes[index++].toInt() and 0xFF) or ((bytes[index++].toInt() and 0xFF) shl 8)

                measurement.instantaneousPower = ((bytes[index++].toInt() and 0xFF) or
                        (bytes[index++].toInt() and 0xFF)).toShort()
                if (rawFlags and MeasurementFlags.pedalPowerBalancePresent == MeasurementFlags.pedalPowerBalancePresent) {
                    measurement.pedalPowerBalance = bytes[index++]

                    measurement.pedalPowerBalanceReference = rawFlags and 0x2 == 0x2
                }
                if (rawFlags and MeasurementFlags.AccumulatedTorquePresent == MeasurementFlags.AccumulatedTorquePresent && bytes.size >= index + 1) {
                    measurement.accumulatedTorque = ((bytes[index++].toInt() and 0xFF) or
                            ((bytes[index++].toInt() and 0xFF) shl 8)).toShort()
                }
                if (rawFlags and MeasurementFlags.WheelRevolutionDataPresent == MeasurementFlags.WheelRevolutionDataPresent && bytes.size >= index + 6) {
                    measurement.cumulativeWheelRevolutions = ((bytes[index++].toInt() and 0xFF)) or
                            (((bytes[index++].toInt() and 0xFF)) shl 8) or
                            (((bytes[index++].toInt() and 0xFF)) shl 16) or
                            (((bytes[index++].toInt() and 0xFF)) shl 24)

                    measurement.lastWheelEventTime = ((bytes[index++].toInt() and 0xFF) or
                            ((bytes[index++].toInt() and 0xFF) shl 8)).toShort()
                }
                if (rawFlags and MeasurementFlags.CrankRevolutionDataPresent == MeasurementFlags.CrankRevolutionDataPresent && bytes.size >= index + 4) {
                    measurement.cumulativeCrankRevolutions = (bytes[index++].toInt() and 0xFF) or
                            ((bytes[index++].toInt() and 0xFF) shl 8)
                    measurement.lastCrankEventTime = (bytes[index++].toInt() and 0xFF) or
                            ((bytes[index++].toInt() and 0xFF) shl 8)
                }
                if (rawFlags and MeasurementFlags.ExtremeForceMagnitudesPresent == MeasurementFlags.ExtremeForceMagnitudesPresent) {
                    measurement.maximumForceMagnitude = ((bytes[index++].toInt() and 0xFF) or
                            ((bytes[index++].toInt() and 0xFF) shl 8)).toShort()
                    measurement.minimumForceMagnitude = ((bytes[index++].toInt() and 0xFF) or
                            ((bytes[index++].toInt() and 0xFF) shl 8)).toShort()
                }
                if (rawFlags and MeasurementFlags.ExtremeTorqueMagnitudesPresent == MeasurementFlags.ExtremeTorqueMagnitudesPresent) {
                    measurement.maximumTorqueMagnitude = ((bytes[index++].toInt() and 0xFF) or
                            ((bytes[index++].toInt() and 0xFF) shl 8)).toShort()
                    measurement.minimumTorqueMagnitude = ((bytes[index++].toInt() and 0xFF) or
                            ((bytes[index++].toInt() and 0xFF) shl 8)).toShort()
                }
                if (rawFlags and MeasurementFlags.ExtremeAnglesPresent == MeasurementFlags.ExtremeAnglesPresent) {
                    measurement.minimumAngle = ((bytes[index++].toInt() and 0xFF) or
                            (bytes[index].toInt() and 0xF0 shl 4)).toShort()
                    measurement.maximumAngle = ((bytes[index++].toInt() and 0xFF) or
                            ((bytes[index++].toInt() and 0xFF) shl 4)).toShort()
                }
                if (rawFlags and MeasurementFlags.TopDeadSpotAnglePresent == MeasurementFlags.TopDeadSpotAnglePresent) {
                    measurement.topDeadSpotAngle = ((bytes[index++].toInt() and 0xFF) or
                            ((bytes[index++].toInt() and 0xFF) shl 8)).toShort()
                }
                if (rawFlags and MeasurementFlags.BottomDeadSpotAnglePresent == MeasurementFlags.BottomDeadSpotAnglePresent) {
                    measurement.bottomDeadSpotAngle = ((bytes[index++].toInt() and 0xFF) or
                            ((bytes[index++].toInt() and 0xFF) shl 8)).toShort()
                }
                if (rawFlags and MeasurementFlags.AccumulatedEnergyPresent == MeasurementFlags.AccumulatedEnergyPresent) {
                    measurement.accumulatedEnergy = ((bytes[index++].toInt() and 0xFF) or
                            ((bytes[index++].toInt() and 0xFF) shl 8)).toShort()
                }
                measurement.timestamp = System.currentTimeMillis().toDouble()
            }
            return measurement
        }
    }


}