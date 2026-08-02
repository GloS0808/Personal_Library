package com.personallibrary.app

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

object GoogleServicesUtils {

    fun isGooglePlayServicesAvailable(context: Context): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
        return resultCode == ConnectionResult.SUCCESS
    }

    fun checkAndShowErrorDialog(context: Context): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                // This will show the system dialog to update/enable GMS
                // We wrap it in a try-catch to avoid crashes on extremely restricted devices
                try {
                    googleApiAvailability.getErrorDialog(
                        context as androidx.appcompat.app.AppCompatActivity,
                        resultCode,
                        9000
                    )?.show()
                } catch (e: Exception) {
                    return false
                }
            }
            return false
        }
        return true
    }
}
