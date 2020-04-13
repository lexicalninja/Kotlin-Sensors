package com.lexicalninja.kotlinsensors

import android.os.ParcelUuid

// base uuid string 0000xxxx-0000-1000-8000-00805F9B34FB
const val APPLICATION_IN_BACKGROUND = "APPLICATION_IN_BACKGROUND"
const val APPLICATION_IN_FOREGROUND = "APPLICATION_IN_FOREGROUND"
const val START_SENSOR_SCAN = "START_SENSOR_SCAN"
const val BLE_SERVICE_MASK = "0000FFFF-0000-0000-0000-000000000000"
const val POWER_UUID = "E9410100-B434-446B-B5CC-36592FC4C724"
const val FTMS_UUID = "00001826-0000-1000-8000-00805f9b34fb"
const val NOTIFICATION_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb"
val BLE_SERVICE_MASK_UUID: ParcelUuid = ParcelUuid.fromString(BLE_SERVICE_MASK)
val xFF = 0xFF.toByte()

fun SystemIdToString(systemId: ByteArray): String {
    val sb = StringBuilder()
    for (b in systemId) {
        sb.append(String.format("%02x", b))
    }
    return sb.toString()
}
//class FlagStruct {
//    internal var rawFlags: Int = 0
//
//    operator fun contains(flagPosition: Int): Boolean {
//        return rawFlags.toLong() and (1L shl flagPosition) != 0L
//    }
//
//    constructor() {}
//
//    constructor(rawFlags: Int) {
//        this.rawFlags = rawFlags
//    }
//}