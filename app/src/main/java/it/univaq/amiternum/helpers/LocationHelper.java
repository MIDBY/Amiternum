package it.univaq.amiternum.helpers;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.ActivityCompat;

import it.univaq.amiternum.R;

public class LocationHelper {
    private LocationManager manager;
    private final ActivityResultLauncher<String> launcher;

    public LocationHelper(ActivityResultLauncher<String> launcher) {
        this.launcher = launcher;
    }

    public void start(Context context, LocationListener listener) {
        manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        int checkFine = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        int checkCoarse = ActivityCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION);
        if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showSettingAlert(context);
        }
        if (checkCoarse == PackageManager.PERMISSION_GRANTED && checkFine==PackageManager.PERMISSION_GRANTED){
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,10000,10, listener);
            manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,10000,10, listener);
        } else {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }

    }

    public void stop(LocationListener listener){
        manager.removeUpdates(listener);
    }

    private void showSettingAlert(Context context) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle(context.getString(R.string.titleSettingAlert));
        alertDialog.setMessage(context.getString(R.string.messageSettingAlert));
        alertDialog.setPositiveButton(context.getString(R.string.okSettingAlert), (dialogInterface, i) -> context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
        alertDialog.setNegativeButton(context.getString(R.string.cancelSettingAlert), (dialogInterface, i) -> {
            alertDialog.setMessage(context.getString(R.string.enableGPSRequest));
        });
        alertDialog.show();
    }
}
