package it.univaq.amiternum.Fragment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import it.univaq.amiternum.Database.DB;
import it.univaq.amiternum.Model.Punto;
import it.univaq.amiternum.R;
import it.univaq.amiternum.Utility.GetData;
import it.univaq.amiternum.Utility.Pref;
import it.univaq.amiternum.helpers.LocationHelper;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback, LocationListener, SensorEventListener {

    private List<Punto> punti = new ArrayList<>();
    private Location userLocation;
    private GoogleMap map;
    private TextView textView;

    private final ActivityResultLauncher<String> launcher = registerForActivityResult (
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if(result){
                        locationHelper.start(requireContext(), MapFragment.this::onLocationChanged);
                    } else {
                        Toast.makeText(requireContext(),"Autorizzazione necessaria",Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private final LocationHelper locationHelper = new LocationHelper(launcher);
    private SensorManager mSensorManager;
    private Sensor mRotationSensor;
    private Marker myMarker;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.map_fragment,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        locationHelper.start(requireContext(), this::onLocationChanged);

        SupportMapFragment fragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapTestFragment);
        textView = view.findViewById(R.id.textView);

        mSensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        if(fragment != null) {
            fragment.getMapAsync(this);
        }

        if(Pref.load(requireContext(),"firstAccess",true)){
            punti = GetData.downloadPunto(requireContext());
        } else {
            new Thread(() -> {
                punti = DB.getInstance(requireContext()).getPuntoDao().findAll();
            }).start();
        }

        FloatingActionButton FAB = view.findViewById(R.id.floatingActionButton);
        FAB.setOnClickListener(v -> {
            if(myMarker != null) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(myMarker.getPosition().latitude, myMarker.getPosition().longitude), 19f));
                if(userLocation != null) {
                    LatLng latLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());

                    if (punti != null && !punti.isEmpty()) {
                        Pair<String, Float> posto = new Pair<>(punti.get(0).getLuogo(), distanceBetween(latLng, new LatLng(punti.get(0).getLatitudine(), punti.get(0).getLongitudine())));
                        for (Punto p : punti) {
                            float dist = distanceBetween(latLng, new LatLng(p.getLatitudine(), p.getLongitudine()));
                            if (dist < posto.second) {
                                posto = new Pair<>(p.getLuogo(), dist);
                            }
                        }
                        Toast.makeText(requireContext(), getString(R.string.distanceText1) + " " + posto.second + getString(R.string.distanceText2)+ " " + posto.first, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR && myMarker!=null) {
            float[] rotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, sensorEvent.values);

            float[] orientation = new float[3];
            SensorManager.getOrientation(rotationMatrix, orientation);

            float bearing = (float) Math.toDegrees(orientation[0]);
            myMarker.setRotation(bearing);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (map == null){
            return;
        }
        userLocation = location;
        if (myMarker == null) {
            MarkerOptions options = new MarkerOptions();
            options.title(getString(R.string.myPosition));
            options.position(new LatLng(location.getLatitude(), location.getLongitude()));
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker30)).anchor(0.5f, 0.5f);
            myMarker = map.addMarker(options);
        } else {
            myMarker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
        }

        if(userLocation.hasAltitude()){
            String text =   getString(R.string.latitudeText)+ " " + userLocation.getLatitude() + "°\n" +
                            getString(R.string.longitudeText)+ " " + userLocation.getLongitude() + "°\n" +
                            getString(R.string.altitudeText)+ " " + Math.round(userLocation.getAltitude()) + getString(R.string.altitudeMeasuring);
            textView.setText(text);
        } else{
            String text =   getString(R.string.latitudeText)+ " " + userLocation.getLatitude() + "°\n" +
                            getString(R.string.longitudeText)+ " " + userLocation.getLongitude() + "°";
            textView.setText(text);
        }

        LatLng latLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());

        myMarker.setPosition(new LatLng(userLocation.getLatitude(), userLocation.getLongitude()));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20f));
    }

    public static float distanceBetween(LatLng first, LatLng second) {
        float[] distance = new float[1];
        Location.distanceBetween(first.latitude, first.longitude, second.latitude, second.longitude, distance);
        return Math.round(distance[0]);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        locationHelper.start(requireContext(),this::onLocationChanged);
        map = googleMap;
        mSensorManager.registerListener(this, mRotationSensor, SensorManager.SENSOR_DELAY_NORMAL);

        UiSettings mapSettings;
        mapSettings = map.getUiSettings();
        mapSettings.setZoomControlsEnabled(true);
        mapSettings.setScrollGesturesEnabled(true);
        mapSettings.setTiltGesturesEnabled(true);
        mapSettings.setMyLocationButtonEnabled(true);

        if (punti != null) {
            for (Punto p : punti) {
                MarkerOptions markerOptions=new MarkerOptions();
                markerOptions.position(new LatLng(p.getLatitudine(),p.getLongitudine()));
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN));
                markerOptions.title(p.getLuogo());
                requireActivity().runOnUiThread(()-> map.addMarker(markerOptions));
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationHelper.stop(this::onLocationChanged);
        mSensorManager.unregisterListener(this, mRotationSensor);
    }

    @Override
    public void onResume() {
        super.onResume();
        locationHelper.start(requireContext(),this::onLocationChanged);
        mSensorManager.registerListener(this, mRotationSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        locationHelper.stop(this::onLocationChanged);
        mSensorManager.unregisterListener(this, mRotationSensor);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        locationHelper.stop(this::onLocationChanged);
    }
}
