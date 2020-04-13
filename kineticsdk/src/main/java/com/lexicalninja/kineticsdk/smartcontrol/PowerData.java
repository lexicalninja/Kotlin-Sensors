package com.lexicalninja.kineticsdk.smartcontrol;

public class PowerData {

    public enum ControlMode {
        ERG(0x00),
        Fluid(0x01),
        Resistance(0x02),
        Simulation(0x03);

        private final int code;

        ControlMode(int code) {
            this.code = code;
        }

        public static ControlMode fromInt(int i) {
            for (ControlMode b : ControlMode.values()) {
                if (b.code == i) {
                    return b;
                }
            }
            return ERG;
        }

        byte getByte() {
            return (byte) code;
        }
    }

    public ControlMode mode;
    public int power;
    public double speedKPH;
    public double cadenceRPM;
    public int targetResistance;

}

