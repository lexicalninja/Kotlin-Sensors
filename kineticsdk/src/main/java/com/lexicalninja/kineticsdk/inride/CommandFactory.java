package com.lexicalninja.kineticsdk.inride;


import com.lexicalninja.kineticsdk.exceptions.InvalidDataException;

class CommandFactory {

    private static class CalibrationParameters {
        // Ticks b/w sensor reading (32 kHz)
        private static final int CalibrationReady = 602;
        private static final int CalibrationStart = 655;
        private static final int CalibrationEnd = 950;
        private static final int CalibrationDebounce = 327;
        private static final ConfigData.SensorUpdateRate UpdateIntervalDefault = ConfigData.SensorUpdateRate.Millis1000;
        private static final ConfigData.SensorUpdateRate UpdateIntervalFast = ConfigData.SensorUpdateRate.Millis250;
    }

    private static class SensorCommands {
        private static final byte SetSpindownParams = 0x01;
        private static final byte SetName = 0x02;
        private static final byte StartCalibration = 0x03;
        private static final byte StopCalibration = 0x04;
        private static final byte SetSpindownTime = 0x05;
    }

    static byte[] StartCalibrationCommandData(byte[] systemId) throws InvalidDataException {
        if (systemId.length != 6) {
            throw new InvalidDataException("Invalid System Id");
        }
        byte[] key = CommandKeyForSystemId(systemId);

        byte[] command = new byte[3];
        command[0] = key[0];
        command[1] = key[1];
        command[2] = SensorCommands.StartCalibration;
        return command;
    }

    static byte[] StopCalibrationCommandData(byte[] systemId) throws InvalidDataException {
        if (systemId.length != 6) {
            throw new InvalidDataException("Invalid System Id");
        }
        byte[] key = CommandKeyForSystemId(systemId);

        byte[] command = new byte[3];
        command[0] = key[0];
        command[1] = key[1];
        command[2] = SensorCommands.StopCalibration;
        return command;
    }

    static byte[] SetSpindownTimeCommandData(double seconds, byte[] systemId) throws InvalidDataException {
        if (systemId.length != 6) {
            throw new InvalidDataException("Invalid System Id");
        }
        byte[] key = CommandKeyForSystemId(systemId);

        byte[] command = new byte[7];
        command[0] = key[0];
        command[1] = key[1];
        command[2] = SensorCommands.SetSpindownTime;

        int spindownTicks = (int) (seconds * 32768);
//        byte[] spindownBytes = BitConverter.GetBytes(spindownTicks);
        command[3] = (byte) (spindownTicks >> 0);
        command[4] = (byte) (spindownTicks >> 8);
        command[5] = (byte) (spindownTicks >> 16);
        command[6] = (byte) (spindownTicks >> 24);

        return command;
    }

    static byte[] ConfigureSensorCommandData(ConfigData.SensorUpdateRate updateRate, byte[] systemId) throws InvalidDataException {
        if (systemId.length != 6) {
            throw new InvalidDataException("Invalid System Id");
        }
        byte[] key = CommandKeyForSystemId(systemId);

        byte[] command = new byte[15];
        command[0] = key[0];
        command[1] = key[1];
        command[2] = SensorCommands.SetSpindownParams;

        command[3] = (byte) (CalibrationParameters.CalibrationReady >> 0);
        command[4] = (byte) (CalibrationParameters.CalibrationReady >> 8);

        command[5] = (byte) (CalibrationParameters.CalibrationStart >> 0);
        command[6] = (byte) (CalibrationParameters.CalibrationStart >> 8);

        command[7] = (byte) (CalibrationParameters.CalibrationEnd >> 0);
        command[8] = (byte) (CalibrationParameters.CalibrationEnd >> 8);

        command[9] = (byte) (CalibrationParameters.CalibrationDebounce >> 0);
        command[10] = (byte) (CalibrationParameters.CalibrationDebounce >> 8);

        int updateDefault = 32;
        switch (updateRate) {
            case Millis1000: {
                updateDefault = 32;
                break;
            }
            case Millis500: {
                updateDefault = 16;
                break;
            }
            case Millis250: {
                updateDefault = 8;
                break;
            }
        }
        command[11] = (byte) (updateDefault >> 0);
        command[12] = (byte) (updateDefault >> 8);

        int updateFast = 8;
        switch (CalibrationParameters.UpdateIntervalFast) {
            case Millis1000: {
                updateFast = 32;
                break;
            }
            case Millis500: {
                updateFast = 16;
                break;
            }
            case Millis250: {
                updateFast = 8;
                break;
            }
        }
        command[13] = (byte) (updateFast >> 0);
        command[14] = (byte) (updateFast >> 8);

        return command;
    }

    static byte[] SetPeripheralNameCommandData(String name, byte[] systemId) throws InvalidDataException {
        if (systemId.length != 6) {
            throw new InvalidDataException("Invalid System Id");
        }
        byte[] nameBytes = name.getBytes();
        if (nameBytes.length < 3 || nameBytes.length > 8) {
            throw new InvalidDataException("Peripheral name must be between 3 and 8 characters");
        }

        byte[] key = CommandKeyForSystemId(systemId);

        byte[] command = new byte[3 + nameBytes.length];
        command[0] = key[0];
        command[1] = key[1];
        command[2] = SensorCommands.SetName;
        System.arraycopy(nameBytes, 0, command, 3, nameBytes.length);
        return command;
    }

    private static byte[] CommandKeyForSystemId(byte[] systemId) throws InvalidDataException {
        if (systemId.length != 6) {
            throw new InvalidDataException("Invalid System Id");
        }
        int sysidx1 = (systemId[3] & 0xFF) % 6;
        int sysidx2 = (systemId[5] & 0xFF) % 6;
        byte[] commandKey = new byte[2];
        commandKey[0] = systemId[sysidx1];
        commandKey[1] = systemId[sysidx2];
        return commandKey;
    }


}
