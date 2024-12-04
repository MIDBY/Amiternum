package it.univaq.amiternum.Fragment;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import it.univaq.amiternum.Database.DB;
import android.Manifest;
import it.univaq.amiternum.Model.Punto;
import it.univaq.amiternum.R;
import it.univaq.amiternum.Utility.GetData;
import it.univaq.amiternum.Utility.Pref;
import it.univaq.amiternum.helpers.CameraHelper;
import it.univaq.amiternum.helpers.LocationHelper;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;

import java.util.ArrayList;

public class ArFragmentOutdoor extends Fragment implements LocationListener {

    private ArSceneView arSceneView;
    private Session session;
    private ArrayList<Punto> punti = new ArrayList<>();
    boolean created = false;

    private final ActivityResultLauncher<String> launcher = registerForActivityResult (
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if(result){
                        locationHelper.start(requireContext(), ArFragmentOutdoor.this::onLocationChanged);
                        if(CameraHelper.checkGeospatialArCorePermissions(requireContext())) {
                            if (session == null)
                                startArCoreSession();
                        } else
                            launcherCamera.launch(Manifest.permission.CAMERA);
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.authRequestText), Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );
    private final LocationHelper locationHelper = new LocationHelper(launcher);
    private final ActivityResultLauncher<String> launcherCamera = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), result -> {
                if(!result) {
                    Toast.makeText(requireContext(), getString(R.string.authRequestText), Toast.LENGTH_SHORT).show();
                } else
                    if(CameraHelper.checkGeospatialArCorePermissions(requireContext())) {
                        if (session == null)
                            startArCoreSession();
                    } else
                        locationHelper.start(requireContext(), ArFragmentOutdoor.this::onLocationChanged);
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_outdoor_ar,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        locationHelper.start(requireContext(), ArFragmentOutdoor.this::onLocationChanged);
        arSceneView = view.findViewById(R.id.ar_scene_viewFragment);
        if (CameraHelper.checkCameraPermission(requireContext())) {
            if (CameraHelper.checkGeospatialArCorePermissions(requireContext()))
                startArCoreSession();
        } else
            launcherCamera.launch(Manifest.permission.CAMERA);

        if(Pref.load(requireContext(),"firstAccess",true))
            punti = GetData.downloadPunto(requireContext());
        else
            new Thread(() -> punti = (ArrayList<Punto>) DB.getInstance(requireContext()).getPuntoDao().findAll()).start();
    }

    private void onUpdateFrame(FrameTime frameTime) {
        Frame frame = arSceneView.getArFrame();

        if (frame == null) {
            return;
        }

        if(session.getEarth().getTrackingState() != TrackingState.TRACKING){
            Log.d("ARSession", "Stato di tracciamento: " + session.getEarth().getTrackingState());
            return;
        }

        if (!created) {

            //CREAZIONE NODO ORIGINE
            double originLatitude = session.getEarth().getCameraGeospatialPose().getLatitude();
            double originLongitude = session.getEarth().getCameraGeospatialPose().getLongitude();
            double originAltitude = Math.max(0, session.getEarth().getCameraGeospatialPose().getAltitude());

            Anchor originAnchor = session.getEarth().createAnchor(originLatitude,originLongitude,originAltitude,0f,0f,0f,1f);
            AnchorNode originAnchorNode = new AnchorNode(originAnchor);
            arSceneView.getScene().addChild(originAnchorNode);

            if (punti != null){
                for (int i = 0; i < punti.size(); i++) {
                    LinearLayout infoPanel = (LinearLayout) getLayoutInflater().inflate(R.layout.info_panel, null);
                    ((TextView) infoPanel.findViewById(R.id.titleTextViewAr)).setText(punti.get(i).getLuogo());
                    ((TextView) infoPanel.findViewById(R.id.subtitleTextViewAr)).setText(punti.get(i).getDescrizione());

                    int index = i;

                    ViewRenderable.builder()
                        .setView(requireContext(), infoPanel)
                        .build()
                        .thenAccept(renderable -> {
                            double latitude = punti.get(index).getLatitudine();
                            double longitude = punti.get(index).getLongitudine();
                            double altitudeCycle = Math.max(0, session.getEarth().getCameraGeospatialPose().getAltitude());
                            Pose worldPose = session.getEarth().getPose(latitude, longitude, altitudeCycle, 0f, 0f, 0f, 1f);
                            Anchor anchorCycle = session.createAnchor(worldPose);
                            Node textViewNodeCycle = new Node();
                            float dist = distanceBetween(new LatLng(originLatitude, originLongitude), new LatLng(latitude, longitude));
                            float scaleFactor = calculateScale(dist);
                            textViewNodeCycle.setRenderable(renderable);
                            textViewNodeCycle.setLocalScale(new Vector3(scaleFactor, scaleFactor, scaleFactor));
                            float[] deviceRotation = worldPose.getRotationQuaternion();
                            Quaternion quaternion = new Quaternion(deviceRotation[0], deviceRotation[1], deviceRotation[2], deviceRotation[3]);
                            Vector3 direction = Vector3.subtract(textViewNodeCycle.getWorldPosition(),
                                    new Vector3(worldPose.tx(), worldPose.ty(), worldPose.tz()));
                            Quaternion lookRotation = Quaternion.lookRotation(direction, Vector3.up());
                            Quaternion finalRotation = Quaternion.multiply(quaternion.inverted(), lookRotation);
                            textViewNodeCycle.setWorldRotation(finalRotation);
                            AnchorNode anchorNodeCycle = new AnchorNode(anchorCycle);
                            anchorNodeCycle.addChild(textViewNodeCycle);
                            originAnchorNode.addChild(anchorNodeCycle);
                        });
                }
                created = true;
            }
        }
    }

    public static float distanceBetween(LatLng first, LatLng second) {
        float[] distance = new float[1];
        Location.distanceBetween(first.latitude, first.longitude, second.latitude, second.longitude, distance);
        return Math.round(distance[0]);
    }

    private float calculateScale(float distance) {
        if(distance <= 5)
            return 1f;
        if(5 < distance && distance <= 15)
            return 2.5f;
        if(15 < distance && distance <= 30)
            return 5f;
        if(30 < distance && distance <= 70)
            return 12f;
        if(70 < distance && distance <= 100)
            return 20f;
        else
            return 0f;
    }

    @Override
    public void onPause() {
        super.onPause();
        locationHelper.stop(ArFragmentOutdoor.this::onLocationChanged);
        if (arSceneView != null) {
            arSceneView.getScene().removeOnUpdateListener(this::onUpdateFrame);
            arSceneView.pause();
        }
        if (session != null) {
            session.pause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationHelper.stop(ArFragmentOutdoor.this::onLocationChanged);
        if (session != null) {
            arSceneView.getScene().removeOnUpdateListener(this::onUpdateFrame);
            session.close();
            session = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        locationHelper.start(requireContext(), ArFragmentOutdoor.this::onLocationChanged);
        if (arSceneView != null) {
            try {
                arSceneView.resume();
                arSceneView.getScene().addOnUpdateListener(this::onUpdateFrame);
            } catch (CameraNotAvailableException e) {
                Log.e("ARCore", "Errore durante la ripresa della sessione, arSceneView", e);
            }
        }
        if (session != null) {
            try {
                session.resume();
            } catch (CameraNotAvailableException e) {
                Log.e("ARCore", "Errore durante la ripresa della sessione", e);}
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        for (int j = arSceneView.getScene().getChildren().size()-1; j>=1 ; j--) {
            Node childNode = arSceneView.getScene().getChildren().get(j);
            arSceneView.getScene().removeChild(childNode);
        }
        created = false;
    }

    private void startArCoreSession() {
        try {
            session = new Session(getContext());
            Config config = new Config(session);
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            config.setGeospatialMode(Config.GeospatialMode.ENABLED);
            session.configure(config);
            arSceneView.setupSession(session);
        } catch (UnavailableArcoreNotInstalledException | UnavailableApkTooOldException |
                 UnavailableSdkTooOldException | UnavailableDeviceNotCompatibleException e) {
            Log.d("ARSession", "Problema nella creazione della sessione");
        }
    }
}
