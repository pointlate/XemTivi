package com.ltnstore.titvi

import android.app.Application

class XemTiviApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    companion object {
        lateinit var instance: XemTiviApp
            private set
    }
}