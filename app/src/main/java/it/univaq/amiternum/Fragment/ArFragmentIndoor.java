package it.univaq.amiternum.Fragment;

import static com.google.zxing.integration.android.IntentIntegrator.REQUEST_CODE;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.LocationListener;
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
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import it.univaq.amiternum.Database.DB;
import it.univaq.amiternum.Model.Oggetto3D;
import it.univaq.amiternum.R;
import it.univaq.amiternum.Utility.ConversionCallback;
import it.univaq.amiternum.Utility.Converter;
import it.univaq.amiternum.Utility.GetData;
import it.univaq.amiternum.Utility.Pref;
import it.univaq.amiternum.helpers.CameraHelper;
import it.univaq.amiternum.helpers.LocationHelper;

public class ArFragmentIndoor extends Fragment implements LocationListener {

    private ArSceneView arSceneView;
    private Session session;
    private ArrayList<Oggetto3D> oggetti = new ArrayList<>();
    private Oggetto3D oggetto;
    private byte[] oggettoPath;
    boolean created = false;
    private AlertDialog dialog;
    private final ActivityResultLauncher<String> launcher = registerForActivityResult (
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if(result){
                        locationHelper.start(requireContext(), ArFragmentIndoor.this::onLocationChanged);
                        if(CameraHelper.checkGeospatialArCorePermissions(requireContext()) && session == null)
                            startArCoreSession();
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.authRequestText), Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );
    private LocationHelper locationHelper;

    private final ActivityResultLauncher<String> launcherCamera = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), result -> {
                if(!result)
                    Toast.makeText(requireContext(), getString(R.string.authRequestText),Toast.LENGTH_SHORT).show();
                else
                    if(CameraHelper.checkGeospatialArCorePermissions(requireContext()) && session == null)
                        startArCoreSession();
            }
    );

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if(result.getContents() != null) {
                    oggetto = handleQRCode(result.getContents());
                    handleObject();
                }
    });


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.ar_fragment,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        locationHelper = new LocationHelper(launcher);
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
        }
        arSceneView = view.findViewById(R.id.ar_scene_viewFragment);

        if(Pref.load(requireContext(),"firstAccess",true)) {
            oggetti = GetData.downloadOggetto(requireContext());
        } else {
            new Thread(() -> oggetti = (ArrayList<Oggetto3D>) DB.getInstance(requireContext()).getOggettoDao().findAll()).start();
        }
        if (CameraHelper.checkCameraPermission(requireContext())) {
            if(CameraHelper.checkGeospatialArCorePermissions(requireContext()))
                startArCoreSession();
        } else
            launcherCamera.launch(Manifest.permission.CAMERA);

        view.findViewById(R.id.launchScanner).setOnClickListener(v -> startQrCodeScanner());
    }

    private void onUpdateFrame(FrameTime frameTime) {
        Frame frame = arSceneView.getArFrame();

        if (frame == null) {
            return;
        }

        if(session.getEarth().getTrackingState() != TrackingState.TRACKING) {
            Log.d("ARSession", "Stato di tracciamento: " + session.getEarth().getTrackingState());
            return;
        }
        if (!created) {
            double originLatitude = session.getEarth().getCameraGeospatialPose().getLatitude();
            double originLongitude = session.getEarth().getCameraGeospatialPose().getLongitude();
            double originAltitude = Math.max(0, session.getEarth().getCameraGeospatialPose().getAltitude());

            Anchor originAnchor = session.getEarth().createAnchor(originLatitude,originLongitude,originAltitude,0f,0f,0f,1f);
            AnchorNode originAnchorNode = new AnchorNode(originAnchor);
            arSceneView.getScene().addChild(originAnchorNode);

            if (oggetti != null && oggetto != null) {
                if(oggettoPath != null) {
                    dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    dialog.dismiss();
                    File tempFile = new File(requireContext().getFilesDir(),"model.gltf");
                    try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                        outputStream.write(oggettoPath);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    ModelRenderable.builder()
                            .setSource(requireContext(), Uri.parse(tempFile.getAbsolutePath()))
                            .build()
                            .thenAccept(renderable -> {
                                double latitude = originLatitude + 1f;
                                double longitude = originLongitude + 1f;
                                double altitudeCycle = session.getEarth().getCameraGeospatialPose().getAltitude();
                                Pose worldPose = session.getEarth().getPose(latitude, longitude, altitudeCycle, 0f, 0f, 0f, 1f);
                                Anchor anchorCycle = session.createAnchor(worldPose);
                                Node textViewNodeCycle = new Node();
                                float scaleFactor = 3f;
                                textViewNodeCycle.setRenderable(renderable);
                                textViewNodeCycle.setLocalScale(new Vector3(scaleFactor, scaleFactor, scaleFactor));
                                float[] deviceRotation = worldPose.getRotationQuaternion();
                                Quaternion quaternion = new Quaternion(deviceRotation[0], deviceRotation[1], deviceRotation[2], deviceRotation[3]);
                                Vector3 direction = Vector3.subtract(textViewNodeCycle.getWorldPosition(),
                                        new Vector3(worldPose.tx(), worldPose.ty(), worldPose.tz()));
                                Quaternion lookRotation = Quaternion.lookRotation(Vector3.up(), direction);
                                Quaternion finalRotation = Quaternion.multiply(lookRotation, quaternion.inverted());
                                textViewNodeCycle.setWorldRotation(finalRotation);
                                AnchorNode anchorNodeCycle = new AnchorNode(anchorCycle);
                                anchorNodeCycle.addChild(textViewNodeCycle);
                                originAnchorNode.addChild(anchorNodeCycle);
                                created = true;
                            })
                            .exceptionally(throwable -> {
                                Log.e("ARModel", "Unable to load model: " + throwable.getMessage());
                                Toast.makeText(requireContext(), "Unable to load model", Toast.LENGTH_LONG).show();
                                return null;
                            });
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        locationHelper.stop(this::onLocationChanged);
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
        locationHelper.stop(this::onLocationChanged);
        if (session != null) {
            arSceneView.getScene().removeOnUpdateListener(this::onUpdateFrame);
            session.close();
            session = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        locationHelper.start(requireContext(), this::onLocationChanged);
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
                Log.e("ARCore", "Errore durante la ripresa della sessione", e);
            }
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

    public void startQrCodeScanner() {
        oggetto = null;
        oggettoPath = null;
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setCaptureActivity(CaptureActivity.class);
        options.setPrompt("Scan a museum QR code");
        options.setCameraId(0);
        options.setBeepEnabled(false);
        barcodeLauncher.launch(options);
    }

    private void handleObject() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(R.layout.progress_layout);
        builder.setMessage("Ricevo dallo spazio l'oggetto richiesto");
        dialog = builder.create();
        dialog.show();
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        Converter.convertToGltf(oggetto, new ConversionCallback() {
            @Override
            public void onConversionComplete(byte[] outputPath) {
                oggettoPath = outputPath;
            }

            @Override
            public void onConversionFailed() {
                Toast.makeText(requireContext(), "Errore nel caricamento dell'oggetto 3D", Toast.LENGTH_LONG).show();
            }
        });
    }

    private Oggetto3D handleQRCode(String qrCodeData) {
        int id = Integer.parseInt(qrCodeData.split("-")[0]);
        for(Oggetto3D o : oggetti)
            if(o.getId() == id)
                return o;
        return null;
    }

    private void startArCoreSession() {
        try {
            session = new Session(requireContext());
            Config config = new Config(session);
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            config.setGeospatialMode(Config.GeospatialMode.ENABLED);
            session.configure(config);
            arSceneView.setupSession(session);
        } catch (UnavailableArcoreNotInstalledException | UnavailableApkTooOldException |
                 UnavailableSdkTooOldException | UnavailableDeviceNotCompatibleException e) {
            e.printStackTrace();
            Log.d("ARSession", "Problema nella creazione della sessione");
        }
    }
}
