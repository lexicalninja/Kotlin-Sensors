package com.lexicalninja.kineticsdk.smartcontrol;


import com.lexicalninja.kineticsdk.exceptions.InvalidDataException;

public class SmartControl {

    public static class DeviceInformation {
        public static final String UUID = "0000180a-0000-1000-8000-00805f9b34fb";

        public static class Characteristics {
            public static final String SYSTEM_ID_UUID = "00002a23-0000-1000-8000-00805f9b34fb";
            public static final String FIRMWARE_REVISION_STRING_UUID = "00002a26-0000-1000-8000-00805f9b34fb";
        }
    }

    public static class PowerService {
        public static final String UUID = "E9410200-B434-446B-B5CC-36592FC4C724";

        public static class Characteristics {
            public static final String POWER_UUID = "E9410201-B434-446B-B5CC-36592FC4C724";
            public static final String CONFIG_UUID = "E9410202-B434-446B-B5CC-36592FC4C724";
            public static final String CONTROL_POINT_UUID = "E9410203-B434-446B-B5CC-36592FC4C724";
        }
    }

    public static PowerData ProcessPowerData(byte[] data) throws InvalidDataException {
        return DataProcessor.ProcessPowerData(data);
    }

    public static ConfigData ProcessConfigurationData(byte[] data) throws InvalidDataException {
        return DataProcessor.ProcessConfigurationData(data);
    }

    public static byte[] StartCalibrationCommandData() throws InvalidDataException {
        return CommandFactory.StartCalibrationCommandData();
    }

    public static byte[] StopCalibrationCommandData() throws InvalidDataException {
        return CommandFactory.StopCalibrationCommandData();
    }

    public static byte[] SetERGMode(int target) throws InvalidDataException {
        return CommandFactory.SetERGMode(target);
    }

    public static byte[] SetFluidMode(int level) throws InvalidDataException {
        return CommandFactory.SetFluidMode(level);
    }

    public static byte[] SetResistanceMode(float resistance) throws InvalidDataException {
        return CommandFactory.SetResistanceMode(resistance);
    }

    public static byte[] SetSimulationMode(float weight, float rollingCoeff, float windCoeff, float grade, float windSpeed) throws InvalidDataException {
        return CommandFactory.SetSimulationMode(weight, rollingCoeff, windCoeff, grade, windSpeed);
    }

    public static byte[] FirmwareUpdateChunk(byte[] firmwareData, FirmwarePosition position, byte[] systemId) {
        return CommandFactory.FirmwareUpdateChunk(firmwareData, position, systemId);
    }

}
