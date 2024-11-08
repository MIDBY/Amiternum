package it.univaq.amiternum.helpers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.ActivityCompat;

public class CameraHelper {
    public static void permission(Context context, ActivityResultLauncher<String> launcher){
        int checkCamera = ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA);
        if (checkCamera != PackageManager.PERMISSION_GRANTED){
            launcher.launch(Manifest.permission.CAMERA);
        }
    }
    public static boolean checkCameraPermission(Context context){
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
}
