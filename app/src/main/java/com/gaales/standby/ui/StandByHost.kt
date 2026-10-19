package com.gaales.standby.ui

import com.gaales.standby.data.StandByNotice

/** What the StandBy UI needs from the surface showing it: the activity, the screen saver or a preview. */
interface StandByHost {
    /** True for the miniature preview in Settings: no gestures, editing or sensors. */
    val isPreview: Boolean get() = false

    /** Leave StandBy. */
    fun exit()

    /** Runs [onUnlocked] once the device is unlocked (iOS asks for Face ID before editing StandBy). */
    fun requestUnlock(onUnlocked: () -> Unit)

    /** Opens the Android Standby app, e.g. to grant a permission a widget needs. */
    fun openApp()

    /** Lets the host dim the display while Night Mode is on. */
    fun onNightModeChanged(night: Boolean) {}

    /** Opens the app that posted a notification. */
    fun openNotification(notice: StandByNotice) {}
}

object PreviewHost : StandByHost {
    override val isPreview = true
    override fun exit() = Unit
    override fun requestUnlock(onUnlocked: () -> Unit) = Unit
    override fun openApp() = Unit
}
