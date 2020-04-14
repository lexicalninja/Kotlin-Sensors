package com.lexicalninja.kotlinsensors.serializers

import com.lexicalninja.kotlinsensors.FlagStruct
import com.lexicalninja.kotlinsensors.hasFlag
import kotlin.experimental.and
import kotlin.experimental.or

/**
 * Created by Saxton on 7/14/16.
 */
class CyclingSpeedCadenceSerializer {

    object MeasurementFlags {
        const val wheelRevolutionDataPresent = 0x1
        const val crankRevolutionDataPresent = 0x2
    }

    class Features(rawFeatures: Int) :
        FlagStruct(rawFeatures) {
        val isWheelRevolutionDataSupported: Boolean
            get() = contains(wheelRevolutionDataSupported)

        val isCrankRevolutionDataSupported: Boolean
            get() = contains(crankRevolutionDataSupported)

        val isMultipleSensorLocationsSupported: Boolean
            get() = contains(multipleSensorLocationsSupported)

        companion object {
            const val wheelRevolutionDataSupported = 0x1
            const val crankRevolutionDataSupported = 0x2
            const val multipleSensorLocationsSupported = 0x4
        }
    }

    class MeasurementData : CyclingMeasurementData {
        override var timeStamp = 0.0
        override var cumulativeWheelRevolutions: Int? = null
        override var lastWheelEventTime: Short? = null
        override var cumulativeCrankRevolutions: Int? = null
        override var lastCrankEventTime: Int? = null
    }

    companion object {
        fun readFeatures(bytes: ByteArray): Features {
            val rawFeatures = (bytes[0].toInt() and 0xFF or (bytes[1].toInt() or 0xFF) shl 8)
            return Features(rawFeatures)
        }

        fun readMeasurement(bytes: ByteArray): MeasurementData {
            val measurement = MeasurementData()
            var index = 0
            val rawFlags = bytes[index++]
            if (rawFlags.hasFlag(MeasurementFlags.wheelRevolutionDataPresent)) {
                measurement.cumulativeWheelRevolutions = (bytes[index++].toInt() and 0xFF) or (bytes[index++].toInt() and 0xFF shl 8) or (bytes[index++].toInt() and 0xFF shl 16) or (bytes[index++].toInt() and 0xFF shl 24)
                measurement.lastWheelEventTime = (bytes[index++].toInt() and 0xff or (bytes[index++].toInt() and 0xff shl 8)).toShort()
            }
            if (rawFlags.hasFlag(MeasurementFlags.crankRevolutionDataPresent)) {
                measurement.cumulativeCrankRevolutions = bytes[index++].toInt() and 0xFF or (bytes[index++].toInt() and 0xFF shl 8)
                measurement.lastCrankEventTime = bytes[index++].toInt() and 0xFF or (bytes[index].toInt() and 0xFF shl 8)
            }
            measurement.timeStamp = System.currentTimeMillis().toDouble()
            return measurement
        }
    }

}