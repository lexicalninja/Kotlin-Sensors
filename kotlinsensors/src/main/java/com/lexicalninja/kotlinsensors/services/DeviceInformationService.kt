package com.lexicalninja.kotlinsensors.services

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import com.lexicalninja.kotlinsensors.*
import java.lang.ref.WeakReference

class DeviceInformationService(gattService: BluetoothGattService, sensor: WeakReference<out BleSensor>)
    : BleService(gattService, sensor) {
    class Factory : IServiceFactory {
        override val uuid = "0000180A-0000-1000-8000-00805F9B34FB"
        override fun create(gattService: BluetoothGattService, sensor: WeakReference<out BleSensor>): BleService {
            return DeviceInformationService(gattService, sensor)
        }

        override val characteristicTypes: MutableMap<String, ICharacteristicFactory> = mutableMapOf(
                SoftwareRevision.factory().uuid to SoftwareRevision.factory(),
                ManufacturerName.factory().uuid to ManufacturerName.factory(),
                ModelNumber.factory().uuid to ModelNumber.factory(),
                SerialNumber.factory().uuid to SerialNumber.factory(),
                HardwareRevision.factory().uuid to HardwareRevision.factory(),
                SystemID.factory().uuid to SystemID.factory(),
                FirmwareRevision.factory().uuid to FirmwareRevision.factory()
                )
    }

    companion object {
        fun factory() = Factory()
    }

    val manufacturerName: ManufacturerName?
        get() = characteristic()
    val modelNumber: ModelNumber?
        get() = characteristic()
    val serialNumber: SerialNumber?
        get() = characteristic()
    val hardwareRevision: HardwareRevision?
        get() = characteristic()
    val softwareRevision: SoftwareRevision?
        get() = characteristic()
    val systemID: SystemID?
        get() = characteristic()
    val firmwareRevision: FirmwareRevision?
        get() = characteristic()

    open class SystemID(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : UTF8Characteristic(service, gattCharacteristic) {
        class Factory : ICharacteristicFactory {
            override val uuid = "00002A23-0000-1000-8000-00805F9B34FB"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return SystemID(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }

        init {
            readValue()
        }
    }

    open class ModelNumber(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : UTF8Characteristic(service, gattCharacteristic) {
        class Factory : ICharacteristicFactory {
            override val uuid = "00002A24-0000-1000-8000-00805F9B34FB"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return ModelNumber(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }
    }

    open class SerialNumber(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : UTF8Characteristic(service, gattCharacteristic) {
        class Factory : ICharacteristicFactory {
            override val uuid = "00002A25-0000-1000-8000-00805F9B34FB"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return SerialNumber(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }
    }

    open class FirmwareRevision(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : UTF8Characteristic(service, gattCharacteristic) {
        class Factory : ICharacteristicFactory {
            override val uuid = "00002A26-0000-1000-8000-00805F9B34FB"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return FirmwareRevision(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }
    }

    open class HardwareRevision(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : UTF8Characteristic(service, gattCharacteristic) {
        class Factory : ICharacteristicFactory {
            override val uuid = "00002A27-0000-1000-8000-00805F9B34FB"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return HardwareRevision(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }
    }

    open class SoftwareRevision(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : UTF8Characteristic(service, gattCharacteristic) {
        class Factory : ICharacteristicFactory {
            override val uuid = "00002A28-0000-1000-8000-00805F9B34FB"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return SoftwareRevision(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }
    }

    open class ManufacturerName(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic)
        : UTF8Characteristic(service, gattCharacteristic) {
        class Factory : ICharacteristicFactory {
            override val uuid = "00002A29-0000-1000-8000-00805F9B34FB"
            override fun create(service: WeakReference<BleService>, gattCharacteristic: BluetoothGattCharacteristic): BleCharacteristic {
                return ManufacturerName(service, gattCharacteristic)
            }
        }

        companion object {
            fun factory() = Factory()
        }
    }
}