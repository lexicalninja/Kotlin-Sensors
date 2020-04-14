package com.lexicalninja.kotlinsensors.serializers

import com.lexicalninja.kotlinsensors.FlagStruct

/**
 * Created by Saxton on 7/27/16.
 */
class CyclingPowerSerializer {

    class MeasurementFlags(rawFlags: Int) : FlagStruct(rawFlags) {
        companion object {
            val pedalPowerBalancePresent = 1
            val AccumulatedTorquePresent = 1 shl 2
            val WheelRevolutionDataPresent = 1 shl 4
            val CrankRevolutionDataPresent = 1 shl 5
            val ExtremeForceMagnitudesPresent = 1 shl 6
            val ExtremeTorqueMagnitudesPresent = 1 shl 7
            val ExtremeAnglesPresent = 1 shl 8
            val TopDeadSpotAnglePresent = 1 shl 9
            val BottomDeadSpotAnglePresent = 1 shl 10
            val AccumulatedEnergyPresent = 1 shl 11
            val OffsetCompensationIndicator = 1 shl 12
        }
    }

    class Features(rawFlags: Int) : FlagStruct(rawFlags) {
        val isWheelRevolutionDataSupported: Boolean
            get() = WheelRevolutionDataSupported in this@Features

        val isCrankRevolutionDataSupported: Boolean
            get() = CrankRevolutionDataSupported in this@Features

        val PedalPowerBalanceSupported = 1
        val AccumulatedTorqueSupported = 1 shl 1
        val WheelRevolutionDataSupported = 1 shl 2
        val CrankRevolutionDataSupported = 1 shl 3
        val ExtremeMagnitudesSupported = 1 shl 4
        val ExtremeAnglesSupported = 1 shl 5
        val TopAndBottomDeadSpotAnglesSupported = 1 shl 6
        val AccumulatedEnergySupported = 1 shl 7
        val OffsetCompensationIndicatorSupported = 1 shl 8
        val OffsetCompensationSupported = 1 shl 9
        val ContentMaskingSupported = 1 shl 10
        val MultipleSensorLocationsSupported = 1 shl 11
        val CrankLengthAdjustmentSupported = 1 shl 12
        val ChainLengthAdjustmentSupported = 1 shl 13
        val ChainWeightAdjustmentSupported = 1 shl 14
        val SpanLengthAdjustmentSupported = 1 shl 15
        val SensorMeasurementContext = 1 shl 16
        val InstantaneousMeasurementDirectionSupported = 1 shl 17
        val FactoryCalibrationDateSupported = 1 shl 18
    }

    class MeasurementData : CyclingMeasurementData {
        override var timeStamp = 0.0
        var instantaneousPower: Short = 0
        var pedalPowerBalance: Byte? = null
        var pedalPowerBalanceReference = false
        var accumulatedTorque: Short? = null
        var timestamp = 0.0
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
            val rawFeatures: Int = (bytes[0].toInt() and 0xFFFF) or (bytes[1].toInt() and 0xFFFF shl 8) or (bytes[2].toInt() and 0xFFFF shl 16) or (bytes[3].toInt() and 0xFFFF shl 24)
            return CyclingPowerSerializer.Features(rawFeatures)
        }

        fun readMeasurement(bytes: ByteArray): MeasurementData? {
            var measurement: MeasurementData? = null
            if (bytes.size > 0) {
                measurement = CyclingPowerSerializer.MeasurementData()
                var index = 0
                val rawFlags: Int = (bytes[index++].toInt() and 0xFF) or (bytes[index++].toInt() and 0xFF shl 8)
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
                    measurement.cumulativeWheelRevolutions = (bytes[index++].toInt() and 0xFFF) or
                            ((bytes[index++].toInt() and 0xFFFF) shl 8) or
                            ((bytes[index++].toInt() and 0xFFFF) shl 16) or
                            ((bytes[index++].toInt() and 0xFFFF) shl 24)
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
                            ((bytes[index].toInt() and 0xF0) shl 4)).toShort()
                    measurement.maximumAngle = (bytes[index++].toInt() and 0xFF or
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

                measurement.timeStamp = System.currentTimeMillis().toDouble()
            }
            return measurement
        }
    }
}