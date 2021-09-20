package com.ds.test;

import static android.content.Context.LOCATION_SERVICE;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.PowerManager;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

public class UtilsMethods {

    public static boolean isGPSEnabled(Context context){
        LocationManager service = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        return service.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @TargetApi(23)
    public static boolean isDozing(Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return powerManager.isDeviceIdleMode() && !powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
        } else {
            return false;
        }
    }

    public static boolean checkPermissions(Context context, String[] permissions) {
        boolean isOK = false;
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                isOK = false;
                break;
            }
            else { isOK = true; }
        }

        return isOK;
    }
}
