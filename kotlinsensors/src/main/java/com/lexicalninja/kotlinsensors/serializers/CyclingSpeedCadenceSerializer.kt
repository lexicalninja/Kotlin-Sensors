package com.lexicalninja.kotlinsensors.serializers

/**
 * Created by Saxton on 7/14/16.
 */
object CyclingSpeedCadenceSerializer {
    fun readFeatures(bytes: ByteArray): Features {
        val rawFeatures =
            (bytes[0] and 0xFF or (bytes[1] or 0xFF) shl 8) as Short
        return CyclingSpeedCadenceSerializer().Features(rawFeatures)
    }

    fun readMeasurement(bytes: ByteArray): MeasurementData {
        val measurement: MeasurementData =
            CyclingSpeedCadenceSerializer().MeasurementData()
        var index = 0
        val rawFlags = bytes[index++]
        if (rawFlags and MeasurementFlags.wheelRevolutionDataPresent == MeasurementFlags.wheelRevolutionDataPresent.toInt()) {
            measurement.cumulativeWheelRevolutions =
                bytes[index++].toInt() and 0xFF or (bytes[index++]
                    .toInt() and 0xFF shl 8) or (bytes[index++].toInt() and 0xFF shl 16) or (bytes[index++].toInt() and 0xFF shl 24)
            measurement.lastWheelEventTime =
                (bytes[index++] and 0xff or (bytes[index++] and 0xff shl 8))
        }
        if (rawFlags and MeasurementFlags.crankRevolutionDataPresent == MeasurementFlags.crankRevolutionDataPresent.toInt()) {
            measurement.cumulativeCrankRevolutions =
                bytes[index++] and 0xFF or (bytes[index++].toShort() and 0xFF) shl 8
            measurement.lastCrankEventTime =
                bytes[index++] and 0xFF or (bytes[index].toShort() and 0xFF) shl 8
        }
        measurement.timestamp = System.currentTimeMillis().toDouble()
        return measurement
    }

    open inner class FlagStruct {
        var rawFlags = 0
        operator fun contains(flagPosition: Int): Boolean {
            return rawFlags and (1L shl flagPosition).toInt() != 0L
        }

        constructor() {}
        constructor(rawFlags: Int) {
            this.rawFlags = rawFlags
        }
    }

    object MeasurementFlags {
        const val wheelRevolutionDataPresent: Byte = 0x1
        const val crankRevolutionDataPresent: Byte = 0x2
    }

    inner class Features(private val rawFeatures: Int) :
        FlagStruct() {
        val isWheelRevolutionDataSupported: Boolean
            get() = rawFeatures and (wheelRevolutionDataSupported and 0xFF) == wheelRevolutionDataSupported

        val isCrankRevolutionDataSupported: Boolean
            get() = rawFeatures and (crankRevolutionDataSupported and 0xFF) == crankRevolutionDataSupported

        val isMultipleSensorLocationsSupported: Boolean
            get() = rawFeatures and multipleSensorLocationsSupported == multipleSensorLocationsSupported

        companion object {
            const val wheelRevolutionDataSupported = 0x1
            const val crankRevolutionDataSupported = 0x2
            const val multipleSensorLocationsSupported = 0x4
        }

    }

    inner class MeasurementData : CyclingMeasurementData {
        var timestamp = 0.0
        var cumulativeWheelRevolutions: Int? = null
        var lastWheelEventTime: Short? = null
        var cumulativeCrankRevolutions: Int? = null
        var lastCrankEventTime: Int? = null
        override fun getTimeStamp(): Double {
            return timestamp
        }

        override fun getCumulativeCrankRevolutions(): Int? {
            return cumulativeCrankRevolutions
        }

        override fun getCumulativeWheelRevolutions(): Int? {
            return cumulativeWheelRevolutions
        }

        override fun getLastCrankEventTime(): Int? {
            return lastCrankEventTime
        }

        override fun getLastWheelEventTime(): Short? {
            return lastWheelEventTime
        }
    }
}