package com.lexicalninja.kotlinsensors.serializers

/**
 * Created by Saxton on 7/21/16.
 */
interface CyclingMeasurementData {
    val timeStamp: Double
    val cumulativeCrankRevolutions: Int?
    val cumulativeWheelRevolutions: Int?
    val lastCrankEventTime: Int?
    val lastWheelEventTime: Short?
}