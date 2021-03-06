package com.lexicalninja.kotlinsensors.serializers

import com.lexicalninja.kotlinsensors.FlagStruct

/**
 * Created by Saxton on 7/14/16.
 */
class CyclingSpeedCadenceSerializer {

    object MeasurementFlags {
        const val wheelRevolutionDataPresent: Int = 0
        const val crankRevolutionDataPresent: Int = 1
    }

    class Features(rawFeatures: Int) : FlagStruct(rawFeatures) {
        val isWheelRevolutionDataSupported: Boolean
            get() = wheelRevolutionDataSupported in this

        val isCrankRevolutionDataSupported: Boolean
            get() = crankRevolutionDataSupported in this

        val isMultipleSensorLocationsSupported: Boolean
            get() = multipleSensorLocationsSupported in this

        companion object {
            const val wheelRevolutionDataSupported = 0
            const val crankRevolutionDataSupported = 1
            const val multipleSensorLocationsSupported = 2
        }

    }

    class MeasurementData : CyclingMeasurementData {
        override var cumulativeWheelRevolutions: Int? = null
        override var lastWheelEventTime: Short? = null
        override var cumulativeCrankRevolutions: Int? = null
        override var lastCrankEventTime: Int? = null
        override var timestamp: Double = 0.0

    }

    companion object {
        fun readFeatures(bytes: ByteArray): Features {
            val rawFeatures = (bytes[0].toInt() and 0xFF) or ((bytes[1].toInt() or 0xFF) shl 8)
            return Features(rawFeatures)
        }

        fun readMeasurement(bytes: ByteArray): MeasurementData {
            val measurement = MeasurementData()
            var index = 0
            val rawFlags = FlagStruct(bytes[index++].toInt())
            if (rawFlags.contains(MeasurementFlags.wheelRevolutionDataPresent)) {
                measurement.cumulativeWheelRevolutions =
                    (bytes[index++].toInt() and 0xFF) or
                            ((bytes[index++].toInt() and 0xFF) shl 8) or
                            ((bytes[index++].toInt() and 0xFF) shl 16) or
                            ((bytes[index++].toInt() and 0xFF) shl 24)
                measurement.lastWheelEventTime = ((bytes[index++].toInt() and 0xff) or
                        ((bytes[index++].toInt() and 0xff) shl 8)).toShort()
            }
            if (rawFlags.contains(MeasurementFlags.crankRevolutionDataPresent)) {
                measurement.cumulativeCrankRevolutions = (bytes[index++].toInt() and 0xFF) or
                        ((bytes[index++].toInt() and 0xFF) shl 8)
                measurement.lastCrankEventTime = (bytes[index++].toInt() and 0xFF) or
                        ((bytes[index].toInt() and 0xFF) shl 8)
            }
            measurement.timestamp = System.currentTimeMillis().toDouble()
            return measurement
        }
    }
}