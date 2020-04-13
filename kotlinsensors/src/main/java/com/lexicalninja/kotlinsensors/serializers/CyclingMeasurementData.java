package com.lexicalninja.kotlinsensors.serializers;

import androidx.annotation.Nullable;

/**
 * Created by Saxton on 7/21/16.
 */
public interface CyclingMeasurementData {
    public double getTimeStamp();

    @Nullable
    public Integer getCumulativeCrankRevolutions();

    @Nullable
    public Integer getCumulativeWheelRevolutions();

    @Nullable
    public Integer getLastCrankEventTime();

    @Nullable
    public Short getLastWheelEventTime();
}
