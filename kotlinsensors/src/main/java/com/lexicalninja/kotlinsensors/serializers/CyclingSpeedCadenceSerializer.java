package com.lexicalninja.kotlinsensors.serializers;

import androidx.annotation.Nullable;

/**
 * Created by Saxton on 7/14/16.
 */
public class CyclingSpeedCadenceSerializer {
    class FlagStruct {
        int rawFlags;

        public boolean contains(int flagPosition) {
            return ((rawFlags & (1L << flagPosition)) != 0);
        }

        public FlagStruct() {
        }

        public FlagStruct(int rawFlags) {
            this.rawFlags = rawFlags;
        }
    }

    public class MeasurementFlags {

        final static byte wheelRevolutionDataPresent = 0x1;
        final static byte crankRevolutionDataPresent = 0x2;
    }

    public class Features extends FlagStruct {

        private int rawFeatures;

        public final static int wheelRevolutionDataSupported = 0x1;
        public final static int crankRevolutionDataSupported = 0x2;
        public final static int multipleSensorLocationsSupported = 0x4;

        public Features(int rawFeatures) {
            this.rawFeatures = rawFeatures;
        }

        public boolean isWheelRevolutionDataSupported() {
            return ((rawFeatures & (wheelRevolutionDataSupported & 0xFF)) == wheelRevolutionDataSupported);
        }

        public boolean isCrankRevolutionDataSupported() {
            return ((rawFeatures & (crankRevolutionDataSupported & 0xFF)) == crankRevolutionDataSupported);
        }

        public boolean isMultipleSensorLocationsSupported() {
            return ((rawFeatures & multipleSensorLocationsSupported) == multipleSensorLocationsSupported);
        }
    }

    public class MeasurementData implements CyclingMeasurementData {
        public double timestamp = 0;
        @Nullable
        public Integer cumulativeWheelRevolutions;
        @Nullable
        public Short lastWheelEventTime;
        @Nullable
        public Integer cumulativeCrankRevolutions;
        @Nullable
        public Integer lastCrankEventTime;

        public MeasurementData() {
            super();
        }

        @Override
        public double getTimeStamp() {
            return this.timestamp;
        }

        @Override
        @Nullable
        public Integer getCumulativeCrankRevolutions() {
            return this.cumulativeCrankRevolutions;
        }

        @Override
        @Nullable
        public Integer getCumulativeWheelRevolutions() {
            return cumulativeWheelRevolutions;
        }

        @Override
        @Nullable
        public Integer getLastCrankEventTime() {
            return lastCrankEventTime;
        }

        @Override
        @Nullable
        public Short getLastWheelEventTime() {
            return lastWheelEventTime;
        }
    }

    public static Features readFeatures(byte[] bytes) {
        short rawFeatures = (short) ((bytes[0] & 0xFF) | (bytes[1] | 0xFF) << 8);
        return new CyclingSpeedCadenceSerializer().new Features(rawFeatures);
    }

    public static MeasurementData readMeasurement(byte[] bytes) {
        MeasurementData measurement = new CyclingSpeedCadenceSerializer().new MeasurementData();

        int index = 0;
        byte rawFlags = bytes[index++];

        if ((rawFlags & MeasurementFlags.wheelRevolutionDataPresent) == MeasurementFlags.wheelRevolutionDataPresent) {
            measurement.cumulativeWheelRevolutions = ((int) bytes[index++] & 0xFF) | (((int) bytes[index++] & 0xFF) << 8) | (((int) bytes[index++] & 0xFF) << 16) | (((int) bytes[index++] & 0xFF) << 24);
            measurement.lastWheelEventTime = (short) ((bytes[index++] & 0xff) | ((bytes[index++] & 0xff) << 8));
        }

        if ((rawFlags & MeasurementFlags.crankRevolutionDataPresent) == MeasurementFlags.crankRevolutionDataPresent) {
            measurement.cumulativeCrankRevolutions = ((bytes[index++] & 0xFF) | ((short) bytes[index++] & 0xFF) << 8);
            measurement.lastCrankEventTime = ((bytes[index++] & 0xFF) | ((short) bytes[index] & 0xFF) << 8);

        }

        measurement.timestamp = System.currentTimeMillis();

        return measurement;
    }
}
