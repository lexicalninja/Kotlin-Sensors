package com.lexicalninja.kotlinsensors.serializers

/**
 * Created by Saxton on 7/14/16.
 */
class CyclingSpeedCadenceSerializer {

    open class FlagStruct {
        var rawFlags = 0
        operator fun contains(flagPosition: Int): Boolean {
            return (rawFlags and (1L shl flagPosition).toInt()) != 0
        }

        constructor() {}

        constructor(rawFlags: Int) {
            this.rawFlags = rawFlags
        }
    }


    object MeasurementFlags {
        const val wheelRevolutionDataPresent: Int = 0x1
        const val crankRevolutionDataPresent: Int = 0x2
    }

    class Features(private val rawFeatures: Int) : FlagStruct(rawFeatures) {
        val isWheelRevolutionDataSupported: Boolean
            get() = (rawFeatures and (wheelRevolutionDataSupported and 0xFF)) == wheelRevolutionDataSupported

        val isCrankRevolutionDataSupported: Boolean
            get() = (rawFeatures and (crankRevolutionDataSupported and 0xFF)) == crankRevolutionDataSupported

        val isMultipleSensorLocationsSupported: Boolean
            get() = (rawFeatures and (multipleSensorLocationsSupported and 0xFF)) == multipleSensorLocationsSupported

        companion object {
            const val wheelRevolutionDataSupported = 0x1
            const val crankRevolutionDataSupported = 0x2
            const val multipleSensorLocationsSupported = 0x4
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
            val rawFlags = bytes[index++]
            if ((rawFlags.toInt() and MeasurementFlags.wheelRevolutionDataPresent) == MeasurementFlags.wheelRevolutionDataPresent) {
                measurement.cumulativeWheelRevolutions =
                    (bytes[index++].toInt() and 0xFF) or
                            ((bytes[index++].toInt() and 0xFF) shl 8) or
                            ((bytes[index++].toInt() and 0xFF) shl 16) or
                            ((bytes[index++].toInt() and 0xFF) shl 24)
                measurement.lastWheelEventTime = ((bytes[index++].toInt() and 0xff) or
                        ((bytes[index++].toInt() and 0xff) shl 8)).toShort()
            }
            if ((rawFlags.toInt() and MeasurementFlags.crankRevolutionDataPresent) == MeasurementFlags.crankRevolutionDataPresent) {
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