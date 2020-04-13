package com.lexicalninja.kineticsdk.smartcontrol;

public class ConfigData {

    public enum CalibrationState {
        NotPerformed(0),
        Initializing(1),
        SpeedUp(2),
        StartCoasting(3),
        Coasting(4),
        SpeedUpDetected(5),
        Complete(10);

        private final int code;

        CalibrationState(int code) {
            this.code = code;
        }

        static CalibrationState fromInt(int i) {
            for (CalibrationState b : CalibrationState.values()) {
                if (b.code == i) { return b; }
            }
            return NotPerformed;
        }
    }

    public CalibrationState calibrationState;
    public double spindownTime;

}
