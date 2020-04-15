package com.lexicalninja.kotlinsensors.serializers;

import androidx.annotation.Nullable;

/**
 * Created by Saxton on 7/27/16.
 */
public class CyclingPowerSerializer {
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

    public class MeasurementFlags extends FlagStruct{
        public static final int pedalPowerBalancePresent = (1);
        public static final int AccumulatedTorquePresent = (1 << 2);
        public static final int WheelRevolutionDataPresent = (1 << 4);
        public static final int CrankRevolutionDataPresent = (1 << 5);
        public static final int ExtremeForceMagnitudesPresent = (1 << 6);
        public static final int ExtremeTorqueMagnitudesPresent = (1 << 7);
        public static final int ExtremeAnglesPresent = (1 << 8);
        public static final int TopDeadSpotAnglePresent = (1 << 9);
        public static final int BottomDeadSpotAnglePresent = (1 << 10);
        public static final int AccumulatedEnergyPresent = (1 << 11);
        public static final int OffsetCompensationIndicator = (1 << 12);
    }

    public class Features extends FlagStruct {

        public static final int PedalPowerBalanceSupported = (1);
        public static final int AccumulatedTorqueSupported = (1 << 1);
        public static final int WheelRevolutionDataSupported = (1 << 2);
        public static final int CrankRevolutionDataSupported = (1 << 3);
        public static final int ExtremeMagnitudesSupported = (1 << 4);
        public static final int ExtremeAnglesSupported = (1 << 5);
        public static final int TopAndBottomDeadSpotAnglesSupported = (1 << 6);
        public static final int AccumulatedEnergySupported = (1 << 7);
        public static final int OffsetCompensationIndicatorSupported = (1 << 8);
        public static final int OffsetCompensationSupported = (1 << 9);
        public static final int ContentMaskingSupported = (1 << 10);
        public static final int MultipleSensorLocationsSupported = (1 << 11);
        public static final int CrankLengthAdjustmentSupported = (1 << 12);
        public static final int ChainLengthAdjustmentSupported = (1 << 13);
        public static final int ChainWeightAdjustmentSupported = (1 << 14);
        public static final int SpanLengthAdjustmentSupported = (1 << 15);
        public static final int SensorMeasurementContext = (1 << 16);
        public static final int InstantaneousMeasurementDirectionSupported = (1 << 17);
        public static final int FactoryCalibrationDateSupported = (1 << 18);

        public Features(int rawFlags) {
            this.rawFlags = rawFlags;
        }

        public boolean isWheelRevolutionDataSupported() {
            return ((rawFlags & WheelRevolutionDataSupported) == WheelRevolutionDataSupported);
        }

        public boolean isCrankRevolutionDataSupported() {
            return ((rawFlags & CrankRevolutionDataSupported) == CrankRevolutionDataSupported);
        }
    }

    public class MeasurementData implements CyclingMeasurementData {
        public short instantaneousPower = 0;
        public
        @Nullable
        Byte pedalPowerBalance;
        public
        @Nullable
        boolean pedalPowerBalanceReference;
        public
        @Nullable
        Short accumulatedTorque;

        public double timestamp = 0;

        public
        @Nullable
        Integer cumulativeWheelRevolutions;
        public
        @Nullable
        Short lastWheelEventTime;
        public
        @Nullable
        Integer cumulativeCrankRevolutions;
        public
        @Nullable
        Integer lastCrankEventTime;

        public
        @Nullable
        Short maximumForceMagnitude;
        public
        @Nullable
        Short minimumForceMagnitude;
        public
        @Nullable
        Short maximumTorqueMagnitude;
        public
        @Nullable
        Short minimumTorqueMagnitude;
        public
        @Nullable
        Short maximumAngle;
        public
        @Nullable
        Short minimumAngle;
        public
        @Nullable
        Short topDeadSpotAngle;
        public
        @Nullable
        Short bottomDeadSpotAngle;
        public
        @Nullable
        Short accumulatedEnergy;

        @Override
        @Nullable
        public Integer getCumulativeWheelRevolutions() {
            return cumulativeWheelRevolutions;
        }

        @Override
        @Nullable
        public Short getLastWheelEventTime() {
            return lastWheelEventTime;
        }

        @Override
        @Nullable
        public Integer getCumulativeCrankRevolutions() {
            return cumulativeCrankRevolutions;
        }

        @Override
        @Nullable
        public Integer getLastCrankEventTime() {
            return lastCrankEventTime;
        }

        @Override
        public double getTimestamp() {
            return timestamp;
        }

        @Override
        public void setTimestamp(double timestamp) {

        }

        @Override
        public void setCumulativeCrankRevolutions(@org.jetbrains.annotations.Nullable Integer cumulativeCrankRevolutions) {

        }

        @Override
        public void setCumulativeWheelRevolutions(@org.jetbrains.annotations.Nullable Integer cumulativeWheelRevolutions) {

        }

        @Override
        public void setLastCrankEventTime(@org.jetbrains.annotations.Nullable Integer lastCrankEventTime) {

        }

        @Override
        public void setLastWheelEventTime(@org.jetbrains.annotations.Nullable Short lastWheelEventTime) {

        }
    }

    public static Features readFeatures(byte[] bytes) {
        int rawFeatures = ((bytes[0] & 0xFFFF)) | ((bytes[1] & 0xFFFF)) << 8 | ((bytes[2] & 0xFFFF)) << 16 | ((bytes[3] & 0xFFFF) << 24);
        return new CyclingPowerSerializer().new Features(rawFeatures);
    }

    public static @Nullable
    MeasurementData readMeasurement(byte[] bytes) {
        MeasurementData measurement = null;
        if (bytes.length > 0) {
            measurement = new CyclingPowerSerializer().new MeasurementData();

            int index = 0;
            int rawFlags = ((bytes[index++] & 0xFF) | ((bytes[index++] & 0xFF) << 8));

            measurement.instantaneousPower = (short) (((short) (bytes[index++] & 0xFF)) |
                    ((short) bytes[index++] & 0xFF));

            if ((rawFlags & MeasurementFlags.pedalPowerBalancePresent) == MeasurementFlags.pedalPowerBalancePresent) {
                measurement.pedalPowerBalance = bytes[index++];
                measurement.pedalPowerBalanceReference = (rawFlags & 0x2) == 0x2;
            }

            if ((rawFlags & MeasurementFlags.AccumulatedTorquePresent) == MeasurementFlags.AccumulatedTorquePresent&& bytes.length >= index + 1) {
                measurement.accumulatedTorque = (short) (((short) bytes[index++] & 0xFF) |
                        ((short) (bytes[index++] & 0xFF) << 8));
            }

            if ((rawFlags & MeasurementFlags.WheelRevolutionDataPresent) == MeasurementFlags.WheelRevolutionDataPresent&& bytes.length >= index + 6) {
                measurement.cumulativeWheelRevolutions = (((bytes[index++] & 0xFFF)) |
                        ((bytes[index++] & 0xFFFF) << 8) | ((bytes[index++] & 0xFFFF) << 16) |
                        ((bytes[index++] & 0xFFFF) << 24));
                measurement.lastWheelEventTime = (short) ((bytes[index++] & 0xFF) |
                        ((bytes[index++] & 0xFF) << 8));
            }

            if ((rawFlags & MeasurementFlags.CrankRevolutionDataPresent) == MeasurementFlags.CrankRevolutionDataPresent && bytes.length >= index + 4) {
                measurement.cumulativeCrankRevolutions =  ((bytes[index++] & 0xFF) |
                        ((bytes[index++] & 0xFF) << 8));
                measurement.lastCrankEventTime =  ((bytes[index++] & 0xFF) |
                        ((bytes[index++] & 0xFF) << 8));
            }

            if ((rawFlags & MeasurementFlags.ExtremeForceMagnitudesPresent) == MeasurementFlags.ExtremeForceMagnitudesPresent) {
                measurement.maximumForceMagnitude = (short) ((bytes[index++] & 0xFF) |
                        ((bytes[index++] & 0xFF) << 8));
                measurement.minimumForceMagnitude = (short) ((bytes[index++] & 0xFF) |
                        ((bytes[index++] & 0xFF) << 8));
            }

            if ((rawFlags & MeasurementFlags.ExtremeTorqueMagnitudesPresent) == MeasurementFlags.ExtremeTorqueMagnitudesPresent) {
                measurement.maximumTorqueMagnitude = (short) ((bytes[index++] & 0xFF) |
                        ((bytes[index++] & 0xFF) << 8));
                measurement.minimumTorqueMagnitude = (short) ((bytes[index++] & 0xFF) |
                        ((bytes[index++] & 0xFF) << 8));
            }

            if ((rawFlags & MeasurementFlags.ExtremeAnglesPresent) == MeasurementFlags.ExtremeAnglesPresent) {
                measurement.minimumAngle = (short) ((bytes[index++] & 0xFF) |
                        ((bytes[index] & 0xF0) << 4));
                measurement.maximumAngle = (short) ((bytes[index++] & 0xFF) |
                        ((bytes[index++] & 0xFF) << 4));
            }

            if ((rawFlags & MeasurementFlags.TopDeadSpotAnglePresent) == MeasurementFlags.TopDeadSpotAnglePresent) {
                measurement.topDeadSpotAngle = (short) ((bytes[index++] & 0xFF) |
                        ((bytes[index++] & 0xFF) << 8));
            }

            if ((rawFlags & MeasurementFlags.BottomDeadSpotAnglePresent) == MeasurementFlags.BottomDeadSpotAnglePresent) {
                measurement.bottomDeadSpotAngle = (short) ((bytes[index++] & 0xFF) |
                        ((bytes[index++] & 0xFF) << 8));
            }

            if ((rawFlags & MeasurementFlags.AccumulatedEnergyPresent) == MeasurementFlags.AccumulatedEnergyPresent) {
                measurement.accumulatedEnergy = (short) ((bytes[index++] & 0xFF) |
                        ((bytes[index++] & 0xFF) << 8));
            }

            measurement.timestamp = System.currentTimeMillis();
        }
        return measurement;
    }


}