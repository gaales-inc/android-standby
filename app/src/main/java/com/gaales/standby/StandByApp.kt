package com.gaales.standby

import android.app.Application
import com.gaales.standby.data.StandBySettings
import com.gaales.standby.service.ChargingMonitorService

class StandByApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ChargingMonitorService.createChannels(this)
        if (StandBySettings.get(this).enabled) ChargingMonitorService.start(this)
    }
}
