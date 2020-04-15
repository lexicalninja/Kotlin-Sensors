package com.lexicalninja.kotlinsensors.serializers

/**
 * Created by Saxton on 7/21/16.
 */
interface CyclingMeasurementData {
    var timestamp: Double
    var cumulativeCrankRevolutions: Int?
    var cumulativeWheelRevolutions: Int?
    var lastCrankEventTime: Int?
    var lastWheelEventTime: Short?
}