package com.kinetic.fit.kotlinsensors

import android.app.Activity
import com.kinetic.fit.ui.settings.sensors.NotificationActivity
import no.nordicsemi.android.dfu.DfuBaseService

class DfuService: DfuBaseService() {
    override fun getNotificationTarget(): Class<out Activity> {
        return NotificationActivity::class.java
    }
}