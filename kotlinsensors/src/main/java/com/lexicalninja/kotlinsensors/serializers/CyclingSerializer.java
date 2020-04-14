package com.lexicalninja.kotlinsensors.serializers;

import androidx.annotation.Nullable;

/**
 * Created by Saxton on 7/14/16.
 */
public class CyclingSerializer {

    public enum
    SensorLocation {
        other,      //0
        TopOfShoe,  //1
        InShoe,     //2
        Hip,        //3
        FrontWheel, //4
        LeftCrank,  //5
        RightCrank, //6
        LeftPedal,  //7
        RightPedal, //8
        FrontHub,   //9
        RearDropout,//10
        Chainstay,  //11
        RearWheel,  //12
        RearHub,    //13
        Chest,      //14
        Spider,     //15
        ChainRing   //16
    }

    public static SensorLocation readSensorLocation(byte[] bytes) {
        return SensorLocation.values()[(int) bytes[0]];
    }

    @Nullable
    public static Double calculateWheelKPH(CyclingMeasurementData current,
                                           CyclingMeasurementData previous,
                                           Double wheelCircumferenceCM,
                                           Integer wheelTimeResolution) {
        double CM_PER_KM = 0.00001;
        double MINS_PER_HOUR = 60.0;

        int cwr1 = 0;
        int cwr2 = 0;
        short lwet1 = 0;
        short lwet2 = 0;
        int wheelRevsDelta;
        short wheeltimeDelta = 0;
        double wheelTimeSeconds;
        double wheelRPM;


        if (current.getCumulativeWheelRevolutions() != null) {
            cwr1 = current.getCumulativeWheelRevolutions();
        } else {
            return null;
        }
        if (previous.getCumulativeWheelRevolutions() != null) {
            cwr2 = previous.getCumulativeWheelRevolutions();
        } else {
            return null;
        }
        if (current.getLastWheelEventTime() != null) {
            lwet1 = current.getLastWheelEventTime();
        } else {
            return null;
        }
        if (previous.getLastWheelEventTime() != null) {
            lwet2 = previous.getLastWheelEventTime();
        } else {
            return null;
        }

        wheelRevsDelta = deltaWithRollover(cwr1, cwr2, Integer.MAX_VALUE);
        wheeltimeDelta = (short) deltaWithRollover(lwet1, lwet2, Short.MAX_VALUE);

        wheelTimeSeconds = (double) wheeltimeDelta / (double) wheelTimeResolution;

        if (wheelTimeSeconds > 0.0) {
            wheelRPM = (double) wheelRevsDelta / (wheelTimeSeconds / 60.0);
            return wheelRPM * wheelCircumferenceCM * CM_PER_KM * MINS_PER_HOUR;
        }

        return 0.0;
    }

    @Nullable
    public static Double calculateCrankRPM(CyclingMeasurementData current,
                                           CyclingMeasurementData previous) {
        int ccr1 = 0;
        int ccr2 = 0;
        int lcet1 = 0;
        int lcet2 = 0;
        int crankRevsDelta;
        int crankTimeDelta;
        double crankTimeSeconds;

        if (current.getCumulativeCrankRevolutions() != null) {
            ccr1 = current.getCumulativeCrankRevolutions();
        } else {
            return null;
        }
        if (previous.getCumulativeCrankRevolutions() != null) {
            ccr2 = previous.getCumulativeCrankRevolutions();
        } else {
            return null;
        }
        if (current.getLastCrankEventTime() != null) {
            lcet1 = current.getLastCrankEventTime();
        } else {
            return null;
        }
        if (previous.getLastCrankEventTime() != null) {
            lcet2 = previous.getLastCrankEventTime();
        } else {
            return null;
        }

        crankRevsDelta =  deltaWithRollover(ccr1, ccr2, Short.MAX_VALUE * 2) ;
        crankTimeDelta =  deltaWithRollover(lcet1, lcet2, Short.MAX_VALUE * 2);

        crankTimeSeconds = crankTimeDelta / 1024.0;
        if (crankTimeSeconds > 0) {
            return crankRevsDelta / (crankTimeSeconds / 60.0);
        }

        return 0.0;
    }

    private static int deltaWithRollover(int now, int last, int max) {
        return last > now ? max - last + now : now - last;
    }

}
