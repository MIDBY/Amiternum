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
import com.google.ar.sceneform.assets.RenderableSource;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;

import it.univaq.amiternum.Database.DB;
import it.univaq.amiternum.Model.Oggetto3D;
import it.univaq.amiternum.R;
import it.univaq.amiternum.Utility.Converter;
import it.univaq.amiternum.Utility.GetData;
import it.univaq.amiternum.Utility.Pref;
import it.univaq.amiternum.helpers.CameraHelper;
import it.univaq.amiternum.helpers.LocationHelper;

public class ArFragmentIndoor extends Fragment {

    private ArFragment arFragment;
    private Session session;
    private ArrayList<Oggetto3D> oggetti = new ArrayList<>();
    private Oggetto3D oggetto;
    boolean created = false;
    private AlertDialog dialog;
    private final ActivityResultLauncher<String> launcherCamera = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), result -> {
                if(!result)
                    Toast.makeText(requireContext(), getString(R.string.authRequestText),Toast.LENGTH_SHORT).show();
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
        return inflater.inflate(R.layout.activity_indoor,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
        }

        if(Pref.load(requireContext(),"firstAccess",true)) {
            oggetti = GetData.downloadOggetto(requireContext());
        } else {
            new Thread(() -> oggetti = (ArrayList<Oggetto3D>) DB.getInstance(requireContext()).getOggettoDao().findAll()).start();
        }
        if (!CameraHelper.checkCameraPermission(requireContext()))
            launcherCamera.launch(Manifest.permission.CAMERA);

        arFragment = (ArFragment) getChildFragmentManager().findFragmentById(R.id.ar_scene_viewFragment);
        arFragment.setOnTapArPlaneListener(((hitResult, plane, motionEvent) -> placeModel(hitResult.createAnchor())));

        view.findViewById(R.id.launchScanner).setOnClickListener(v -> startQrCodeScanner());
    }

    private void placeModel(Anchor anchor) {
        ModelRenderable.builder()
                .setSource(requireContext(),
                        RenderableSource.builder()
                                .setSource(requireContext(), Uri.parse(oggetto.getResourcePath()), RenderableSource.SourceType.GLTF2)
                                .build()
                ).setRegistryId(oggetto.getResourcePath())
                .build()
                .thenAccept(renderable -> {
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setRenderable(renderable);
                    arFragment.getArSceneView().getScene().addChild(anchorNode);
                })
                .exceptionally(throwable -> {
                    Log.e("ARModel", "Unable to load model: " + throwable.getMessage());
                    Toast.makeText(requireContext(), "Unable to load model", Toast.LENGTH_LONG).show();
                    return null;
                });
    }

    @Override
    public void onPause() {
        super.onPause();
        arFragment.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        arFragment.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        arFragment.onResume();
    }

    public void startQrCodeScanner() {
        oggetto = null;
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
        builder.setMessage("Ricevo l'oggetto richiesto dalla galassia vicina");
        dialog = builder.create();
        dialog.show();
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        if(oggetto.getResourcePath() == null || oggetto.getResourcePath().isEmpty())
            Converter.downloadResource(requireContext(), oggetto);
    }

    private Oggetto3D handleQRCode(String qrCodeData) {
        int id = Integer.parseInt(qrCodeData.split("-")[0]);
        for(Oggetto3D o : oggetti)
            if(o.getId() == id)
                return o;
        return null;
    }
}
