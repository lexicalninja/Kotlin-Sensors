package com.lexicalninja.kotlinsensors.serializers

/**
 * Created by Saxton on 7/14/16.
 */
object CyclingSerializer {
    fun readSensorLocation(bytes: ByteArray): SensorLocation {
        return SensorLocation.values()[bytes[0].toInt()]
    }

    fun calculateWheelKPH(
        current: CyclingMeasurementData,
        previous: CyclingMeasurementData,
        wheelCircumferenceCM: Double,
        wheelTimeResolution: Int
    ): Double? {
        val CM_PER_KM = 0.00001
        val MINS_PER_HOUR = 60.0
        var cwr1 = 0
        var cwr2 = 0
        var lwet1: Short = 0
        var lwet2: Short = 0
        val wheelRevsDelta: Int
        var wheeltimeDelta: Short = 0
        val wheelTimeSeconds: Double
        val wheelRPM: Double
        cwr1 = if (current.cumulativeCrankRevolutions != null) {
            current.cumulativeWheelRevolutions!!
        } else {
            return null
        }
        cwr2 = if (previous.cumulativeWheelRevolutions != null) {
            previous.cumulativeWheelRevolutions!!
        } else {
            return null
        }
        lwet1 = if (current.lastWheelEventTime != null) {
            current.lastWheelEventTime!!
        } else {
            return null
        }
        lwet2 = if (previous.lastWheelEventTime != null) {
            previous.lastWheelEventTime!!
        } else {
            return null
        }
        wheelRevsDelta = deltaWithRollover(cwr1, cwr2, Int.MAX_VALUE)
        wheeltimeDelta = deltaWithRollover(
            lwet1.toInt(),
            lwet2.toInt(),
            Short.MAX_VALUE.toInt()
        ).toShort()
        wheelTimeSeconds = wheeltimeDelta.toDouble() / wheelTimeResolution.toDouble()
        if (wheelTimeSeconds > 0.0) {
            wheelRPM = wheelRevsDelta.toDouble() / (wheelTimeSeconds / 60.0)
            return wheelRPM * wheelCircumferenceCM * CM_PER_KM * MINS_PER_HOUR
        }
        return 0.0
    }

    fun calculateCrankRPM(
        current: CyclingMeasurementData,
        previous: CyclingMeasurementData
    ): Double? {
        var ccr1 = 0
        var ccr2 = 0
        var lcet1 = 0
        var lcet2 = 0
        val crankRevsDelta: Int
        val crankTimeDelta: Int
        val crankTimeSeconds: Double
        ccr1 = if (current.cumulativeCrankRevolutions != null) {
            current.cumulativeCrankRevolutions!!
        } else {
            return null
        }
        ccr2 = if (previous.cumulativeCrankRevolutions != null) {
            previous.cumulativeCrankRevolutions!!
        } else {
            return null
        }
        lcet1 = if (current.lastCrankEventTime != null) {
            current.lastCrankEventTime!!
        } else {
            return null
        }
        lcet2 = if (previous.lastCrankEventTime != null) {
            previous.lastCrankEventTime!!
        } else {
            return null
        }
        crankRevsDelta =
            deltaWithRollover(ccr1, ccr2, Short.MAX_VALUE * 2)
        crankTimeDelta =
            deltaWithRollover(lcet1, lcet2, Short.MAX_VALUE * 2)
        crankTimeSeconds = crankTimeDelta / 1024.0
        return if (crankTimeSeconds > 0) {
            crankRevsDelta / (crankTimeSeconds / 60.0)
        } else 0.0
    }

    private fun deltaWithRollover(now: Int, last: Int, max: Int): Int {
        return if (last > now) max - last + now else now - last
    }

    enum class SensorLocation {
        other,  //0
        TopOfShoe,  //1
        InShoe,  //2
        Hip,  //3
        FrontWheel,  //4
        LeftCrank,  //5
        RightCrank,  //6
        LeftPedal,  //7
        RightPedal,  //8
        FrontHub,  //9
        RearDropout,  //10
        Chainstay,  //11
        RearWheel,  //12
        RearHub,  //13
        Chest,  //14
        Spider,  //15
        ChainRing //16
    }
}