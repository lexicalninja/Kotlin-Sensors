package com.lexicalninja.kotlinsensors.serializers

open class KineticSerializer {
    data class KineticConfig(var systemStatus: Long = 0,
                             var calibrationState: Int = 0,
                             var spindownTime: Long = 0,
                             var firmwareUpdateState: Int = 0,
                             var bleRevision: Int = 0,
                             var antirattleRamp: Int = 0)


    data class KineticControlPointResponse(var requestCode: Int = 0, var result: Int = 0)

    data class KineticDebugData(var mode: KineticMode = KineticMode.Erg,
                                var targetResistance: Long = 0,
                                var actualResistance: Long = 0,
                                var targetPosition: Long = 0,
                                var actualPosition: Long = 0,
                                var tempSensorValue: Long = 0,
                                var tempDieValue: Long = 0,
                                var tempCalculatated: Long = 0,
                                var homeAccuracy: Long = 0,
                                var bleBuild: Int = 0)

    enum class KineticMode { Erg, Position, Simulation }

    companion object {
        fun setDeviceName(deviceName: String): ByteArray {
            return ByteArray(deviceName.length + 1) { i ->
                if (i == 0) 0X09.toByte()
                else deviceName.toByte()
            }
        }

        fun readConfig(bytes: ByteArray): KineticConfig? {
            return if (bytes.size > 7) {
                val config = KineticConfig()
                config.systemStatus = bytes[0].toLong() or bytes[1].toLong() shl 8
                config.calibrationState = bytes[2].toInt()
                config.spindownTime = bytes[3].toLong() or bytes[4].toLong() shl 8
                config.firmwareUpdateState = bytes[5].toInt()
                config.bleRevision = bytes[6].toInt()
                config.antirattleRamp = bytes[7].toInt()
                config
            } else null
        }

        fun readControlPointResponse(bytes: ByteArray): KineticControlPointResponse? {
            return if (bytes.size > 2) {
                val response = KineticControlPointResponse()
                response.requestCode = bytes[1].toInt()
                response.result = bytes[2].toInt()
                response
            } else null
        }

        fun readDebugData(bytes: ByteArray): KineticDebugData? {
            return if (bytes.size > 17) {
                val debug = KineticDebugData()
                if (bytes[0].toInt() in 0 until KineticMode.values().size) {
                    debug.mode = KineticMode.values()[bytes[0].toInt()]
                } else {
                    debug.mode = KineticMode.Erg
                }
                debug.targetResistance = bytes[1].toLong() or bytes[2].toLong() shl 8
                debug.actualResistance = bytes[3].toLong() or bytes[4].toLong() shl 8
                debug.targetPosition = bytes[5].toLong() or bytes[6].toLong() shl 8
                debug.actualPosition = bytes[7].toLong() or bytes[8].toLong() shl 8
                debug.tempSensorValue = bytes[9].toLong() or bytes[10].toLong() shl 8
                debug.tempDieValue = bytes[11].toLong() or bytes[12].toLong() shl 8
                debug.tempCalculatated = bytes[13].toLong() or bytes[14].toLong() shl 8
                debug.homeAccuracy = bytes[15].toLong() or bytes[16].toLong() shl 8
                debug.bleBuild = bytes[17].toInt()
                debug
            } else null
        }
    }
}