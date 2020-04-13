package com.lexicalninja.kotlinsensors.serializers;

import android.util.Log;

import org.jetbrains.annotations.Nullable;

import com.kinetic.fit.kotlinsensors.FtmsPair;

/**
 * Created by Saxton on 1/9/18.
 */

public class FitnessMachineSerializer {
    private static final double EPSILON = 0.000000119;

    private class FlagStruct {
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

    public class MachineFeatures extends FlagStruct {
        public MachineFeatures(int rawFlags) {
            this.rawFlags = rawFlags;
        }

        public static final int AverageSpeedSupported = (0);
        public static final int CadenceSupported = (1);
        public static final int TotalDistanceSupported = (2);
        public static final int InclinationSupported = (3);
        public static final int ElevationGainSupported = (4);
        public static final int PaceSupported = (5);
        public static final int StepCountSupported = (6);
        public static final int ResistanceLevelSupported = (7);
        public static final int StrideCountSupported = (8);
        public static final int ExpendedEnergySupported = (9);
        public static final int HeartRateMeasurementSupported = (10);
        public static final int MetabolicEquivalentSupported = (11);
        public static final int ElapsedTimeSupported = (12);
        public static final int RemainingTimeSupported = (13);
        public static final int PowerMeasurementSupported = (14);
        public static final int ForceOnBeltAndPowerOutputSupported = (15);
        public static final int UserDataRetentionSupported = (16);
    }

    public class TargetSettingFeatures extends FlagStruct {
        public TargetSettingFeatures(int rawFlags) {
            this.rawFlags = rawFlags;
        }

        public static final int SpeedTargetSettingSupported = (0);
        public static final int InclinationTargetSettingSupported = (1);
        public static final int ResistanceTargetSettingSupported = (2);
        public static final int PowerTargetSettingSupported = (3);
        public static final int HeartRateTargetSettingSupported = (4);
        public static final int TargetedExpendedEnergyConfigurationSupported = (5);
        public static final int TargetedStepNumberConfigurationSupported = (6);
        public static final int TargetedStrideNumberConfigurationSupported = (7);
        public static final int TargetedDistanceConfigurationSupported = (8);
        public static final int TargetedTrainingTimeConfigurationSupported = (9);
        public static final int TargetedTimeInTwoHeartRateZonesConfigurationSupported = (10);
        public static final int TargetedTimeInThreeHeartRateZonesConfigurationSupported = (11);
        public static final int TargetedTimeInFiveHeartRateZonesConfigurationSupported = (12);
        public static final int IndoorBikeSimulationParametersSupported = (13);
        public static final int WheelCircumferenceConfigurationSupported = (14);
        public static final int SpinDownControlSupported = (15);
        public static final int TargetedCadenceConfigurationSupported = (16);
    }

    public static FtmsPair<Integer, Integer> readFeatures(byte[] bytes) {
        int rawMachine = (int) bytes[0] | ((int) bytes[1] << 8) | ((int) bytes[2] << 16) | ((int) bytes[3] << 24);
        int rawFeatures = (int) bytes[4] | ((int) bytes[5] << 8) | ((int) bytes[6] << 16) | ((int) bytes[7] << 24);
        return new FtmsPair<>(rawMachine, rawFeatures);
    }

    public class TrainerStatusFlags extends FlagStruct {
        static final byte TrainingStatusStringPresent = (0);
        static final byte ExtendedStringPresent = (2);
    }

    public enum TrainingStatusField {
        other(0x00),
        idle(0x01),
        warmingUp(0x02),
        lowIntensityInterval(0x03),
        highIntensityInterval(0x04),
        recoveryInterval(0x05),
        isometric(0x06),
        heartRateControl(0x07),
        fitnessTest(0x08),
        speedOutsideControlRegionLow(0x09),
        speedOutsideControlRegionHigh(0x0A),
        coolDown(0x0B),
        wattControl(0x0C),
        manualMode(0x0D),
        preWorkout(0x0E),
        postWorkout(0x0F);

        public int bits;

        TrainingStatusField(int bits) {
            this.bits = bits;
        }

        public static TrainingStatusField getFromBits(int bits) {
            for (TrainingStatusField field : TrainingStatusField.values()) {
                if (field.bits == bits) {
                    return field;
                }
            }
            return other;
        }
    }

    public class TrainingStatus {
        public TrainingStatus() {
        }

        public TrainerStatusFlags flags = new TrainerStatusFlags();
        public TrainingStatusField status = TrainingStatusField.other;
        public String statusString;
    }

    public static TrainingStatus readTrainingStatus(byte[] bytes) {
        TrainingStatus status = new FitnessMachineSerializer().new TrainingStatus();
        status.flags.rawFlags = bytes[0];
        if (bytes.length > 1) {
            status.status = TrainingStatusField.getFromBits(bytes[0]);
        } else {
            status.status = TrainingStatusField.other;
        }
        if (status.flags.contains(TrainerStatusFlags.TrainingStatusStringPresent)) {
            // ToDo; parse bytes 2-16 into a string (UTF8)
            status.statusString = "ToDo";
        }
        return status;
    }

    public enum ControlOpCode {
        requestControl(0x00),
        reset(0x01),
        setTargetSpeed(0x02),
        setTargetInclincation(0x03),
        setTargetResistanceLevel(0x04),
        setTargetPower(0x05),
        setTargetHeartRate(0x06),
        startOrResume(0x07),
        stopOrPause(0x08),
        setTargetedExpendedEnergy(0x09),
        setTargetedNumberOfSteps(0x0A),
        setTargetedNumberOfStrides(0x0B),
        setTargetedDistance(0x0C),
        setTargetedTrainingTime(0x0D),
        setTargetedTimeInTwoHeartRateZones(0x0E),
        setTargetedTimeInThreeHeartRateZones(0x0F),
        setTargetedTimeInFiveHeartRateZones(0x10),
        setIndoorBikeSimulationParameters(0x11),
        setWheelCircumference(0x12),
        spinDownControl(0x13),
        setTargetedCadence(0x14),
        responseCode(0x80),
        unknown(0xFF);

        public int bits;

        ControlOpCode(int bits) {
            this.bits = bits;
        }

        public static ControlOpCode getFromBits(int bits) {
            for (ControlOpCode field : ControlOpCode.values()) {
                if (field.bits == bits) {
                    return field;
                }
            }
            return unknown;
        }
    }

    public enum ResultCode {
        reserved(0x00),
        success(0x01),
        opCodeNotSupported(0x02),
        invalidParameter(0x03),
        operationFailed(0x04),
        controlNotPermitted(0x05);

        public int bits;

        ResultCode(int bits) {
            this.bits = bits;
        }

        public static ResultCode getFromBits(int bits) {
            for (ResultCode field : ResultCode.values()) {
                if (field.bits == bits) {
                    return field;
                }
            }
            return reserved;
        }
    }

    public class ControlPointResponse {
        ControlOpCode requestOpCode = ControlOpCode.unknown;
        ResultCode resultCode = ResultCode.opCodeNotSupported;

        //        Target Speed Params when the request is SpinDownController
        public double targetSpeedLow;
        public double targetSpeedHigh;
    }

    public static @Nullable
    ControlPointResponse readControlPointResponse(byte[] bytes) {
        ControlPointResponse response = null;
        if (bytes.length > 2 && (bytes[0] & 0xFF) == ControlOpCode.responseCode.bits) {
            response = new FitnessMachineSerializer().new ControlPointResponse();
            response.requestOpCode = ControlOpCode.getFromBits(bytes[1] & 0xFF);
            response.resultCode = ResultCode.getFromBits(bytes[2] & 0xFF);
            if (response.resultCode == ResultCode.success && response.requestOpCode == ControlOpCode.spinDownControl) {
//                If success and spindown control response, the target high / low speeds are tacked onto the end
                Log.d("spindown", "success");
                if (bytes.length > 6) {
                    response.targetSpeedLow = (double) ((bytes[3] & 0xFF) | ((bytes[4] & 0xFF) << 8)) / 100.0;
                    response.targetSpeedHigh = (double) ((bytes[5] & 0xFF) | ((bytes[6] & 0xFF) << 8)) / 100.0;
                }
            }
        }
        return response;
    }

    public class IndoorBikeSimulationParameters {

        public double windSpeed;
        public double grade;
        public double crr;
        public double crw;

        public IndoorBikeSimulationParameters() {
        }

        public IndoorBikeSimulationParameters(double windSpeed, double grade, double crr, double crw) {
            this.windSpeed = windSpeed;
            this.grade = grade;
            this.crr = crr;
            this.crw = crw;
        }

        public boolean equals(IndoorBikeSimulationParameters obj) {
            return Math.abs(this.windSpeed - obj.windSpeed) <= EPSILON &&
                    Math.abs(this.grade - obj.grade) <= EPSILON &&
                    Math.abs(this.crr - obj.crr) <= EPSILON &&
                    Math.abs(this.crw - obj.crw) <= EPSILON;
        }
    }

    public static byte[] setIndoorBikeSimulationParameters(IndoorBikeSimulationParameters params) {
        // windSpeed = meters / second  res 0.001
        // grade = percentage           res 0.01
        // crr = unitless               res 0.0001
        // cw = kg / meter              res 0.01
        int mpsN = (int) (params.windSpeed * 1000);
        int gradeN = (int) (params.grade * 100);
        int crrN = (int) (params.crr * 10000);
        int crwN = (int) (params.crw * 100);
        return new byte[]{
                (byte) ControlOpCode.setIndoorBikeSimulationParameters.bits,
                (byte) (mpsN & 0xff),
                (byte) ((mpsN >>> 8) & 0xFF),
                (byte) (gradeN & 0xFF),
                (byte) ((gradeN >>> 8) & 0xFF),
                (byte) crrN,
                (byte) crwN
        };
    }

    public static byte[] requestControl() {
        return new byte[]{(byte) (ControlOpCode.requestControl.bits)};
    }

    public static byte[] reset() {
        return new byte[]{(byte) (ControlOpCode.reset.bits & 0xFF)};
    }

    public static byte[] startOrResume() {
        return new byte[]{(byte) (ControlOpCode.startOrResume.bits & 0xFF)};
    }

    public static byte[] stop() {
        return new byte[]{
                (byte) (ControlOpCode.stopOrPause.bits & 0xFF),
                (byte) 0x01
        };
    }

    public static byte[] pause() {
        return new byte[]{
                (byte) (ControlOpCode.stopOrPause.bits & 0xFF),
                (byte) 0x02
        };
    }

    public static byte[] setTargetResistanceLevel(double level) {
//        level = unitless      res 0.1
        int levelN = (int) level * 10;
        return new byte[]{
                (byte) (ControlOpCode.stopOrPause.bits & 0xFF),
                (byte) (levelN & 0xFF),
                (byte) ((levelN >>> 8) & 0xFF)
        };
    }

    public static byte[] setTargetPower(int watts) {
        return new byte[]{
                (byte) ControlOpCode.setTargetPower.bits,
                (byte) (watts & 0xFF),
                (byte) ((watts >>> 8) & 0xFF)
        };
    }

    public static byte[] startSpinDownControl() {
        return new byte[]{
                (byte) ControlOpCode.spinDownControl.bits,
                (byte) 0x01
        };
    }

    public static byte[] ignoreSpinDownControlRequest() {
        return new byte[]{
                (byte) ControlOpCode.spinDownControl.bits,
                (byte) 0x02
        };
    }

    public class IndoorBikeDataFlags {
        public int rawFlags;

        public static final int MoreData = (0);
        public static final int AverageSpeedPresent = (1);
        public static final int InstantaneousCadencePresent = (2);
        public static final int AverageCadencePresent = (3);
        public static final int TotalDistancePresent = (4);
        public static final int ResistanceLevelPresent = (5);
        public static final int InstantaneousPowerPresent = (6);
        public static final int AveragePowerPresent = (7);
        public static final int ExpendedEnergyPresent = (8);
        public static final int HeartRatePresent = (9);
        public static final int MetabolicEquivalentPresent = (10);
        public static final int ElapsedTimePresent = (11);
        public static final int RemainingTimePresent = (12);

        public IndoorBikeDataFlags() {
            this.rawFlags = 0;
        }

        public IndoorBikeDataFlags(int rawFlags) {
            this.rawFlags = rawFlags;
        }

        boolean contains(int flagPosition) {
            return ((rawFlags & (1L << flagPosition)) != 0);
        }

    }

    public class IndoorBikeData {
        public IndoorBikeDataFlags flags = new IndoorBikeDataFlags();

        public Double instantaneousSpeed;
        public Double averageSpeed;
        public Double instantaneousCadence;
        public Double averageCadence;
        public Integer totalDistance;
        public Integer resistanceLevel;
        public Integer instantaneousPower;
        public Integer averagePower;
        public Integer totalEnergy;
        public Integer energyPerHour;
        public Integer energyPerMinute;
        public Integer heartRate;
        public Double metabolicEquivalent;
        public Integer elapsedTime;
        public Integer remainingTime;

        public IndoorBikeData() {
        }
    }

    public static @Nullable
    IndoorBikeData readIndoorBikeData(byte[] bytes) {
        IndoorBikeData bikeData = null;
        if (bytes.length > 0) {
            bikeData = new FitnessMachineSerializer().new IndoorBikeData();
            int index = 0;

            int rawFlags = (bytes[index++] & 0xFF) | ((bytes[index++] & 0xFF) << 8);
            bikeData.flags = new FitnessMachineSerializer().new IndoorBikeDataFlags(rawFlags);

            if (!bikeData.flags.contains(IndoorBikeDataFlags.MoreData)) {
                int value = ((bytes[index++] & 0xFF) | ((bytes[index++] & 0xFF) << 8));
                bikeData.instantaneousSpeed = (double) value / 100.0;
            }
            if (bikeData.flags.contains(IndoorBikeDataFlags.AverageSpeedPresent)) {
                int value = ((bytes[index++] & 0xFF) | ((bytes[index++] & 0xFF) << 8));
                bikeData.averageSpeed = (double) value / 2.0;
            }
            if (bikeData.flags.contains(IndoorBikeDataFlags.InstantaneousCadencePresent)) {
                int value = ((bytes[index++] & 0xFF) | ((bytes[index++] & 0xFF) << 8));
                bikeData.instantaneousCadence = (double) value / 2.0;
            }
            if (bikeData.flags.contains(IndoorBikeDataFlags.AverageCadencePresent)) {
                int value = ((bytes[index++] & 0xFF) | ((bytes[index++] & 0xFF) << 8));
                bikeData.averageCadence = (double) value / 2.0;
            }
            if (bikeData.flags.contains(IndoorBikeDataFlags.TotalDistancePresent)) {
                int value = ((bytes[index++] & 0xFF) | ((bytes[index++] & 0xFF) << 8) | ((bytes[index++] & 0xFF) << 16));
                bikeData.totalDistance = value;
            }
            if (bikeData.flags.contains(IndoorBikeDataFlags.ResistanceLevelPresent)) {
                int value = ((bytes[index++] & 0xFF) | ((bytes[index++] & 0xFF) << 8));
                bikeData.resistanceLevel = value;
            }
            if (bikeData.flags.contains(IndoorBikeDataFlags.InstantaneousPowerPresent)) {
                int value = ((bytes[index++] & 0xFF) | ((bytes[index++] & 0xFF) << 8));
                bikeData.instantaneousPower = value;
            }
            if (bikeData.flags.contains(IndoorBikeDataFlags.AveragePowerPresent)) {
                int value = ((bytes[index++] & 0xFF) | ((bytes[index++] & 0xFF) << 8));
                bikeData.averagePower = value;
            }
            if (bikeData.flags.contains(IndoorBikeDataFlags.ExpendedEnergyPresent)) {
                bikeData.totalEnergy = ((bytes[index++] & 0xFF) | ((bytes[index++] & 0xFF) << 8));
                bikeData.energyPerHour = ((bytes[index++] & 0xFF) | ((bytes[index++] & 0xFF) << 8));
                bikeData.energyPerMinute = (bytes[index++] & 0xFF);
            }
            if (bikeData.flags.contains(IndoorBikeDataFlags.HeartRatePresent)) {
                bikeData.heartRate = (bytes[index++] & 0xFF);
            }
            if (bikeData.flags.contains(IndoorBikeDataFlags.MetabolicEquivalentPresent)) {
                int value = (bytes[index++] & 0xFF);
                bikeData.metabolicEquivalent = (double) value / 10.0;
            }
            if (bikeData.flags.contains(IndoorBikeDataFlags.ElapsedTimePresent)) {
                bikeData.elapsedTime = ((bytes[index++] & 0xFF) | ((bytes[index++] & 0xFF) << 8));
            }
            if (bikeData.flags.contains(IndoorBikeDataFlags.RemainingTimePresent)) {
                bikeData.remainingTime = ((bytes[index++] & 0xFF) | ((bytes[index] & 0xFF) << 8));
            }
        }

        return bikeData;
    }

    public class SupportedResistanceLevelRange {
        public double minimumResistanceLevel = 0;
        public double maximumResistanceLevel = 0;
        public double minimumIncrement = 0;

        public SupportedResistanceLevelRange() {
        }
    }

    public static @Nullable
    SupportedResistanceLevelRange readSupportedResistanceLevelRange(byte[] bytes) {
        SupportedResistanceLevelRange response = null;
        if (bytes.length > 0) {
            response = new FitnessMachineSerializer().new SupportedResistanceLevelRange();
            int value1 = ((bytes[0] & 0xFF) | ((bytes[1] & 0xFF) << 8));
            int value2 = ((bytes[2] & 0xFF) | ((bytes[3] & 0xFF) << 8));
            int value3 = ((bytes[4] & 0xFF) | ((bytes[5] & 0xFF) << 8));
            response.minimumResistanceLevel = (double) value1 / 10.0;
            response.maximumResistanceLevel = (double) value2 / 10.0;
            response.minimumIncrement = (double) value3 / 10.0;
        }
        return response;
    }

    public class SupportedPowerRange {
        public int minimumPower = 0;
        public int maximumPower = 0;
        public int minumumIncrement = 0;

        public SupportedPowerRange() {
        }
    }

    public static SupportedPowerRange readSupportedPowerRange(byte[] bytes) {
        SupportedPowerRange response = null;
        if (bytes.length > 0) {
            response = new FitnessMachineSerializer().new SupportedPowerRange();
            response.minimumPower = ((bytes[0] & 0xFF) | ((bytes[1] & 0xFF) << 8));
            response.maximumPower = ((bytes[2] & 0xFF) | ((bytes[3] & 0xFF) << 8));
            response.minumumIncrement = ((bytes[4] & 0xFF) | ((bytes[5] & 0xFF) << 8));
        }
        return response;
    }

    public enum MachineStatusOpCode {
        reservedForFutureUse(0x00),
        reset(0x01),
        stoppedOrPausedByUser(0x02),
        stoppedBySafetyKey(0x03),
        startedOrResumedByUser(0x04),
        targetSpeedChanged(0x05),
        targetInclineChanged(0x06),
        targetResistancLevelChanged(0x07),
        targetPowerChanged(0x08),
        targetHeartRateChanged(0x09),
        targetedExpendedEnergyChanged(0x0A),
        targetedNumberOfStepsChanged(0x0B),
        targetedNumberOfStridesChanged(0x0C),
        targetedDistanceChanged(0x0D),
        targetedTrainingTimeChanged(0x0E),
        targetedTimeInTwoHeartRateZonesChanged(0x0F),
        targetedTimeInThreeHeartRateZonesChanged(0x10),
        targetedTimeInFiveHeartRateZonesChanged(0x11),
        indoorBikeSimulationParametersChanged(0x12),
        wheelCircumferenceChanged(0x13),
        spinDownStatus(0x14),
        targetedCadenceChanged(0x15),
        controlPermissionLost(0xFF);

        public int bits;

        MachineStatusOpCode(int bits) {
            this.bits = bits;
        }

        public static MachineStatusOpCode getFromBits(int bits) {
            for (MachineStatusOpCode field : MachineStatusOpCode.values()) {
                if (field.bits == bits) {
                    return field;
                }
            }
            return reservedForFutureUse;
        }
    }

    public enum SpinDownStatus {
        reservedForFutureUse(0x00),
        spinDownRequested(0x01),
        success(0x02),
        error(0x03),
        stopPedaling(0x04);

        public int bits;

        SpinDownStatus(int bits) {
            this.bits = bits;
        }

        public static SpinDownStatus getFromBits(int bits) {
            for (SpinDownStatus field : SpinDownStatus.values()) {
                if (field.bits == bits) {
                    return field;
                }
            }
            return reservedForFutureUse;
        }
    }

    public class MachineStatusMessage {
        public MachineStatusOpCode opCode = MachineStatusOpCode.reservedForFutureUse;
        public SpinDownStatus spinDownStatus;
        public double spinDownTime;
        public int targetPower;
        public double targetResistanceLevel;
        public IndoorBikeSimulationParameters targetSimParameters;
    }

    public static MachineStatusMessage readMachineStatus(byte[] bytes) {
        MachineStatusMessage message = new FitnessMachineSerializer().new MachineStatusMessage();

        if (bytes.length > 0) {
            message.opCode = MachineStatusOpCode.getFromBits(bytes[0] & 0xFF);
        }
//        TODO do something with each case
        switch (message.opCode) {
            case reservedForFutureUse:
                break;
            case reset:
                break;
            case stoppedOrPausedByUser: {
                if (bytes.length > 1) {
//                    0x01 = stop
//                    0x02 = pause
                }
                break;
            }
            case stoppedBySafetyKey:
                break;
            case startedOrResumedByUser:
                break;
            case targetSpeedChanged: {
                if (bytes.length > 2) {
//                    UInt 16 km / hour w/ res 0.01
//                    message.targetSpeed = UInt16(bytes[1]) | UInt16(bytes[2]) << 8
                }
                break;
            }
            case targetInclineChanged: {
                if (bytes.length > 2) {
//                    Int16 percent w/ res 0.1
//                    message.targetIncline = Int16(bytes[1]) | Int16(bytes[2]) << 8
                }
                break;
            }
            case targetResistancLevelChanged: {
                if (bytes.length > 2) {
//                    ??? the spec cannot be correct here
//                    If we go by the Supported Resistance Level Range characteristic,
//                    this value *should* be a SInt16 w/ res 0.1
                    message.targetResistanceLevel = (double) ((bytes[1] & 0xFF) | ((bytes[2] & 0xFF) << 8));
                }
                break;
            }
            case targetPowerChanged: {
                if (bytes.length > 2) {
//                    Int16 watts w/ res 1
                    message.targetPower = (bytes[1] & 0xFF) | ((bytes[2] & 0xFF) << 8);
                }
                break;
            }
            case targetHeartRateChanged: {
                if (bytes.length > 1) {
//                    UInt8 bpm w/ res 1
//                    message.targetHeartRate = bytes[1]
                }
                break;
            }
            case targetedExpendedEnergyChanged: {
                if (bytes.length > 2) {
//                    UInt16 cals w/ res 1
//                    message.targetedExpendedEnergy = UInt16(bytes[1]) | UInt16(bytes[2]) << 8
                }
                break;
            }
            case targetedNumberOfStepsChanged: {
                if (bytes.length > 2) {
//                    UInt16 steps w/ res 1
//                    message.targetedNumberOfSteps = UInt16(bytes[1]) | UInt16(bytes[2]) << 8
                }
                break;
            }
            case targetedNumberOfStridesChanged: {
                if (bytes.length > 2) {
//                    UInt16 strides w/ res 1
//                    message.targetedNumberOfStrides = UInt16(bytes[1]) | UInt16(bytes[2]) << 8
                }
                break;
            }
            case targetedDistanceChanged: {
                if (bytes.length > 3) {
//                    UInt24 meters w/ res 1
//                    message.targetedTrainingTime = UInt16(bytes[1]) | UInt16(bytes[2]) << 8
                }
                break;
            }
            case targetedTrainingTimeChanged: {
                if (bytes.length > 2) {
//                    UInt16 seconds w/ res 1
                }
                break;
            }
            case targetedTimeInTwoHeartRateZonesChanged:
                break;
            case targetedTimeInThreeHeartRateZonesChanged:
                break;
            case targetedTimeInFiveHeartRateZonesChanged:
                break;
            case indoorBikeSimulationParametersChanged: {
                if (bytes.length > 6) {
                    double windSpeed = (double) ((bytes[1] & 0xFF) | ((bytes[2] & 0xFF) << 8)) / 1000.0;
                    double grade = (double) ((bytes[3] & 0xFF) | ((bytes[4] & 0xFF) << 8)) / 100.0;
                    double crr = (double) (bytes[5] & 0xFF) / 10000.0;
                    double cwr = (double) (bytes[6] & 0xFF) / 100.0;
                    message.targetSimParameters = new FitnessMachineSerializer().new IndoorBikeSimulationParameters(windSpeed, grade, crr, cwr);
                }
            }
            break;
            case wheelCircumferenceChanged: {
                if (bytes.length > 2) {
//                    UInt16 mm w/ res 0.1
//                    message.wheelCircumferenceChanged = UInt16(bytes[1]) | UInt16(bytes[2]) << 8
                }
                break;
            }
            case spinDownStatus: {
                if (bytes.length > 1) {
                    message.spinDownStatus = SpinDownStatus.getFromBits(bytes[1] & 0xFF);
                    if (message.spinDownStatus == SpinDownStatus.success && bytes.length > 3) {
//                        Milliseconds attached, convert to seconds
                        message.spinDownTime = (double) (((bytes[2] & 0xFF) | (bytes[3] & 0xFF) << 8)) / 1000.0;
                    }
                    if (message.spinDownStatus == SpinDownStatus.error && bytes.length > 3) {
//                        Milliseconds attached, convert to seconds
                        message.spinDownTime = (double) (((bytes[2] & 0xFF) | (bytes[3] & 0xFF) << 8)) / 1000.0;
                    }

                }
                break;
            }
            case targetedCadenceChanged: {
                if (bytes.length > 2) {
//                    UInt16 rpms w/ res 0.5
//                    message.targetedCadence = UInt16(bytes[1]) | UInt16(bytes[2]) << 8
                }
                break;
            }
            case controlPermissionLost:
                break;
        }
        return message;
    }

}
