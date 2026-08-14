package cvam.dignity.bhumess

import android.app.Application
import android.content.pm.ApplicationInfo
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import cvam.dignity.bhumess.ads.AdConfig

class BhuJiApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val isDebugBuild =
            (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        if (isDebugBuild && AdConfig.TEST_DEVICE_ID.isNotBlank()) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(
                        listOf(AdConfig.TEST_DEVICE_ID)
                    )
                    .build()
            )
        }

        MobileAds.initialize(this)
    }
}