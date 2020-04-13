package com.lexicalninja.kineticsdk.smartcontrol;

import com.lexicalninja.kineticsdk.exceptions.InvalidDataException;

import java.util.concurrent.ThreadLocalRandom;

class CommandFactory {

    static byte[] StartCalibrationCommandData() throws InvalidDataException {
        byte[] command = new byte[3];
        command[0] = ControlType.SpindownCalibration;
        command[1] = 0x01;
        command[2] = nonce();
        return hashCommand(command);
    }

    static byte[] StopCalibrationCommandData() throws InvalidDataException {
        byte[] command = new byte[3];
        command[0] = ControlType.SpindownCalibration;
        command[1] = 0x00;
        command[2] = nonce();
        return hashCommand(command);
    }

    static byte[] SetERGMode(int target) throws InvalidDataException {
        byte[] command = new byte[5];
        command[0] = ControlType.SetPerformance;
        command[1] = PowerData.ControlMode.ERG.getByte();
        command[2] = (byte) (target >> 8);
        command[3] = (byte) (target >> 0);
        command[4] = nonce();
        return hashCommand(command);
    }

    static byte[] SetFluidMode(int level) throws InvalidDataException {
        byte[] command = new byte[4];
        command[0] = ControlType.SetPerformance;
        command[1] = PowerData.ControlMode.Fluid.getByte();
        command[2] = (byte) Math.max(0, Math.min(9, level));
        command[3] = nonce();
        return hashCommand(command);
    }

    static byte[] SetResistanceMode(float resistance) throws InvalidDataException {
        resistance = Math.max(0, Math.min(1, resistance));
        int normalized = Math.round(65535 * resistance);

        byte[] command = new byte[5];
        command[0] = ControlType.SetPerformance;
        command[1] = PowerData.ControlMode.Resistance.getByte();
        command[2] = (byte) (normalized >> 8);
        command[3] = (byte) (normalized >> 0);
        command[4] = nonce();

        return hashCommand(command);
    }

    static byte[] SetSimulationMode(float weight, float rollingCoeff, float windCoeff, float grade, float windSpeed) throws InvalidDataException {
        byte[] command = new byte[13];
        command[0] = ControlType.SetPerformance;
        command[1] = PowerData.ControlMode.Simulation.getByte();

        // weight is in KGs ... multiply by 100 to get 2 points of precision
        int weight100 = (int) Math.round(Math.min(655.36, weight) * 100);
        command[2] = (byte) (weight100 >> 8);
        command[3] = (byte) (weight100 >> 0);

        // Rolling coeff is < 1. multiply by 10,000 to get 5 points of precision
        // coeff cannot be larger than 6.5536 otherwise it rolls over ...
        int rr10000 = (int) Math.round(Math.min(6.5536, rollingCoeff) * 10000);
        command[4] = (byte) (rr10000 >> 8);
        command[5] = (byte) (rr10000 >> 0);

        // Wind coeff is typically < 1. multiply by 10,000 to get 5 points of precision
        // coeff cannot be larger than 6.5536 otherwise it rolls over ...
        int wr10000 = (int) Math.round(Math.min(6.5536, windCoeff) * 10000);
        command[6] = (byte) (wr10000 >> 8);
        command[7] = (byte) (wr10000 >> 0);

        // Grade is between -45.0 and 45.0
        // Mulitply by 100 to get 2 points of precision
        int grade100 = Math.round(Math.max(-45, Math.min(45, grade)) * 100);
        command[8] = (byte) (grade100 >> 8);
        command[9] = (byte) (grade100 >> 0);

        // windspeed is in meters / second. convert to CM / second
        int windSpeedCM = Math.round(windSpeed * 100);
        command[10] = (byte) (windSpeedCM >> 8);
        command[11] = (byte) (windSpeedCM >> 0);

        command[12] = nonce();

        return hashCommand(command);
    }

    static byte[] FirmwareUpdateChunk(byte[] firmware, FirmwarePosition position, byte[] systemId) {
        int pos = position.getPosition();
        int payloadSize = Math.min(17, firmware.length - pos);
        byte[] writeData = new byte[payloadSize + 3];
        writeData[0] = ControlType.Firmware;

        // high bit indicates the start of the firmware update, bit 6 is reserved, and the low 6 bits are a packet sequence number
        writeData[1] = (byte) ((pos == 0) ? 0x80 : ((pos / 17) & 0x3F));
        for (int index = 0; index < payloadSize; index++, pos++) {
            writeData[index + 2] = firmware[pos];
        }
        writeData[payloadSize + 2] = nonce();

        int hashSeed = 0x42;
        if (systemId != null) {
            hashSeed = DataProcessor.hash8WithSeed(0, systemId);
        }
        int hash = DataProcessor.hash8WithSeed(hashSeed, writeData[payloadSize + 2] & 0xFF);
        for (int index = 0; index < payloadSize + 2; index++) {
            byte temp = writeData[index];
            writeData[index] ^= hash;
            hash = DataProcessor.hash8WithSeed(hash, temp & 0xFF);
        }
        position.setPosition(pos);
        return writeData;
    }

    private static byte nonce() {
        return (byte) (ThreadLocalRandom.current().nextInt(0, 256) & 0xFF);
    }

    private static byte[] hashCommand(byte[] command) {
        int hash = DataProcessor.hash8WithSeed(0x42, command[command.length - 1] & 0xFF);
        for (int index = 0; index < command.length - 1; index++) {
            byte temp = command[index];
            command[index] ^= hash;
            hash = DataProcessor.hash8WithSeed(hash, temp & 0xFF);
        }
        return command;
    }

    private static class ControlType {
        private static final byte SetPerformance = 0x00;
        private static final byte Firmware = 0x01;
        private static final byte MotorSpeed = 0x02;
        private static final byte SpindownCalibration = 0x03;
        private static final byte AntiRattle = 0x04;
    }
}
