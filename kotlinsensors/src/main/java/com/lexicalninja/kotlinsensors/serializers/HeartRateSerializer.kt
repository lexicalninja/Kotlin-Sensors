package com.lexicalninja.kotlinsensors.serializers

class HeartRateSerializer {
    data class MeasurementData @ExperimentalUnsignedTypes constructor(var heartRate: UInt = UInt.MIN_VALUE,
                                                                      var energyExpended: UInt? = null,
                                                                      var rrInterval: UInt? = null,
                                                                      var contactStatus: ContactStatus = ContactStatus.NotSupported
    ) {

        enum class ContactStatus { NotSupported, NotDetected, Detected }
    }

    enum class BodySensorLocation {
        Other,      //0
        Chest,      //1
        Wrist,      //2
        Finger,     //3
        Hand,       //4
        EarLobe,    //5
        Foot,       //6
    }




    companion object {
        @ExperimentalUnsignedTypes
        val mask: UInt = 0xFF.toUInt()

        @ExperimentalUnsignedTypes
        fun readMeasurement(bytes: UByteArray): MeasurementData {
            val measurement = MeasurementData()
            if(bytes.size >= 2) {
                var index = 0
                val flags = bytes[index++]

                if(flags and 1.toUByte() == UByte.MIN_VALUE){
                    measurement.heartRate = bytes[index++].toUInt() and mask
                } else if(bytes.size > 2){
                    measurement.heartRate = (bytes[index++].toUInt() and mask) or (bytes[index++].toUInt() and mask shl 8)
                }

                val contactStatusBits = (flags.toInt() and 0x06) shr 1
                if(contactStatusBits == 2) {
                    measurement.contactStatus = MeasurementData.ContactStatus.NotDetected
                } else if(contactStatusBits == 3) {
                    measurement.contactStatus = MeasurementData.ContactStatus.Detected
                }
                if(flags.toInt() and 0x08 == 0x08 && bytes.size > 4) {
                    measurement.energyExpended = (bytes[index++].toUInt() and mask) or (bytes[index++].toUInt() and mask shl 8)
                }
                if(flags.toInt() and 0x10 == 0x10 && bytes.size > 6) {
                    measurement.rrInterval = (bytes[index++].toUInt() and mask) or (bytes[index].toUInt() and mask shl 8)
                }
            }
            return measurement
        }

        fun readSensorLocation(bytes: ByteArray): BodySensorLocation? {
            return if(bytes.isEmpty()) null
            else BodySensorLocation.values()[bytes[0].toInt()]
        }

        fun writeResetEnergyExpended():ByteArray = byteArrayOf(0x01)
    }
}