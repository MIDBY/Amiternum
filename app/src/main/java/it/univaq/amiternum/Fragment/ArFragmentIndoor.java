package it.univaq.amiternum.Fragment;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;

import it.univaq.amiternum.Database.DB;
import it.univaq.amiternum.Model.Oggetto3D;
import it.univaq.amiternum.R;
import it.univaq.amiternum.Utility.ConversionCallback;
import it.univaq.amiternum.Utility.Converter;
import it.univaq.amiternum.Utility.GetData;
import it.univaq.amiternum.Utility.OnRequest;
import it.univaq.amiternum.Utility.Pref;
import it.univaq.amiternum.helpers.CameraHelper;

public class ArFragmentIndoor extends Fragment {

    //TODO: Scegliere QRCode o riconoscimento immagini?
    //          -QrCode tramite libreria zxing, inserendo un'activity tra mainActivity e IndoorActivity
    //          -Riconoscitore immagine tramite AugmentedImageDatabase nel sito ufficiale https://developers.google.com/ar/develop/java/augmented-images/guide?hl=it
    //TODO: Video nel mainActivity non mostra la barra dei controlli sul cellulare

    private ArSceneView arSceneView;
    private Session session;
    private ArrayList<Oggetto3D> oggetti = new ArrayList<>();
    private Oggetto3D oggetto;
    private String oggettoPath;
    private boolean first = true;
    boolean created = false;

    private final ActivityResultLauncher<String> launcherCamera = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), result -> { if(!result)Toast.makeText(requireContext(), getString(R.string.authRequestText),Toast.LENGTH_SHORT).show(); }
    );

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> { if(result.getContents() != null) oggetto = handleQRCode(result.getContents());
    });


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.ar_fragment,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!CameraHelper.checkCameraPermission(requireContext())){
            CameraHelper.permission(requireContext(), launcherCamera);
        }
        arSceneView = view.findViewById(R.id.ar_scene_viewFragment);

        try {
            session = new Session(requireContext());
            Config config = new Config(session);
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            config.setDepthMode(Config.DepthMode.AUTOMATIC);
            session.configure(config);
            arSceneView.setupSession(session);
        } catch (UnavailableArcoreNotInstalledException | UnavailableApkTooOldException |
                 UnavailableSdkTooOldException | UnavailableDeviceNotCompatibleException e) {
            e.printStackTrace();
            Log.d("ARSession", "Problema nella creazione della sessione");
        }
        if(Pref.load(requireContext(),"firstAccess",true)){
            oggetti = GetData.downloadOggetto(requireContext());
        } else {
            new Thread(() -> oggetti = (ArrayList<Oggetto3D>) DB.getInstance(requireContext()).getOggettoDao().findAll()).start();
        }

        view.findViewById(R.id.launchScanner).setOnClickListener(v -> startQrCodeScanner());
    }



    private void onUpdateFrame(FrameTime frameTime) {
        Frame frame = arSceneView.getArFrame();

        if (frame == null) {
            return;
        }

        if(frame.getCamera().getTrackingState() != TrackingState.TRACKING)
            return;

        if (!created) {
            //CREAZIONE NODO ORIGINE
            float[] position = {0, 0, 0, (float) -0.75};
            float[] rotation = { 0, 0, 0, 1};
            Anchor originAnchor = session.createAnchor(new Pose(position, rotation));
            AnchorNode originAnchorNode = new AnchorNode(originAnchor);
            arSceneView.getScene().addChild(originAnchorNode);


            if (oggetti != null && oggetto != null) {
                if(first) {
                    Converter.convertToGltf(oggetto.getObjUrlFile(), oggetto.getMtlUrlFile(), oggetto.getImgUrlFiles(), new ConversionCallback() {
                        @Override
                        public void onConversionComplete(String outputPath) {
                            requireActivity().runOnUiThread(() -> oggettoPath = outputPath);
                        }

                        @Override
                        public void onConversionFailed() {
                            requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Errore nel caricamento dell'oggetto 3D", Toast.LENGTH_LONG).show());
                        }
                    });
                    first = !first;
                }
                if(oggettoPath != null) {
                    ModelRenderable.builder()
                            .setSource(requireContext(), Uri.parse(oggettoPath))
                            .setRegistryId(oggetto.getId())
                            .build()
                            .thenAccept(modelRenderable -> {
                                Node node = new Node();
                                node.setRenderable(modelRenderable);
                                node.setWorldPosition(new Vector3(0f, 0f, -1f));
                                arSceneView.getScene().addChild(node);
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
        if (session != null) {
            arSceneView.getScene().removeOnUpdateListener(this::onUpdateFrame);
            session.close();
            session = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (arSceneView != null) {
            try {
                arSceneView.resume();
                arSceneView.getScene().addOnUpdateListener(this::onUpdateFrame);
            } catch (CameraNotAvailableException e) {
                e.printStackTrace();
                Log.e("ARCore", "Errore durante la ripresa della sessione, arSceneView", e);
            }
        }
        if (session != null) {
            try {
                session.resume();
            } catch (CameraNotAvailableException e) {
                e.printStackTrace();
                Log.e("ARCore", "Errore durante la ripresa della sessione", e);
            }
        }
    }
/*
    @Override
    public void onLocationChanged(@NonNull Location location) {
        //rendering
        for (int j = arSceneView.getScene().getChildren().size()-1; j>=1 ; j--) {
            Node childNode = arSceneView.getScene().getChildren().get(j);
            arSceneView.getScene().removeChild(childNode);
        }
        created = false;
    }*/

    public void startQrCodeScanner() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setCaptureActivity(CaptureActivity.class);
        options.setPrompt("Scan a QR code");
        options.setCameraId(0);
        options.setBeepEnabled(false);
        barcodeLauncher.launch(options);
    }

    private Oggetto3D handleQRCode(String qrCodeData) {
        int id = Integer.parseInt(qrCodeData.split("-")[0]);
        for(Oggetto3D o : oggetti)
            if(o.getId() == id)
                return o;
        return null;
    }
}
