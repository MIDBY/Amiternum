package it.univaq.amiternum.helpers;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.ActivityCompat;

import it.univaq.amiternum.R;

public class LocationHelper {
    private LocationManager manager;
    private final ActivityResultLauncher<String> launcher;
    private AlertDialog alertDialog;

    public LocationHelper(ActivityResultLauncher<String> launcher) {
        this.launcher = launcher;
    }

    public void start(Context context, LocationListener listener) {
        manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showSettingAlert(context);
        }
        if(manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }

        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,5000,5, listener);
            manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,5000,5, listener);
        } else {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    public void stop(LocationListener listener){
        if(listener != null)
            manager.removeUpdates(listener);
    }

    private void showSettingAlert(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.titleSettingAlert))
                .setMessage(context.getString(R.string.messageSettingAlert))
                .setPositiveButton(context.getString(R.string.okSettingAlert), (dialogInterface, i) -> { alertDialog.dismiss(); context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)); })
                .setNegativeButton(context.getString(R.string.cancelSettingAlert), (dialogInterface, i) -> alertDialog.setMessage(context.getString(R.string.enableGPSRequest)));
        alertDialog = builder.create();
        alertDialog.show();
    }
}
