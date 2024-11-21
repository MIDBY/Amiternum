package it.univaq.amiternum.Fragment;

import static com.google.zxing.integration.android.IntentIntegrator.REQUEST_CODE;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.assets.RenderableSource;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import it.univaq.amiternum.Database.DB;
import it.univaq.amiternum.Model.Oggetto3D;
import it.univaq.amiternum.R;
import it.univaq.amiternum.Utility.ConversionCallback;
import it.univaq.amiternum.Utility.Converter;
import it.univaq.amiternum.Utility.GetData;
import it.univaq.amiternum.Utility.Pref;
import it.univaq.amiternum.helpers.CameraHelper;

public class ArFragmentIndoor extends Fragment {

    private ArFragment arFragment;
    private ArrayList<Oggetto3D> oggetti = new ArrayList<>();
    private Oggetto3D oggetto;
    private int secondsElapsed = 0;

    private final String ASSET_ONLINE = "https://github.com/KhronosGroup/glTF-Sample-Models/raw/refs/heads/main/2.0/Fox/glTF/Fox.gltf";
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
                    //TODO: mostra la scritta
                    //Toast.makeText(requireContext(), "Premi sullo schermo per visualizzare l'oggetto",Toast.LENGTH_SHORT).show();
                    //TODO:elimina condizione
                    if(oggetto.getResourcePath() == null || oggetto.getResourcePath().isEmpty())
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

        if(this.getArguments() != null) {
            oggetto = (Oggetto3D) this.getArguments().get("object");
            if(oggetto != null) {
                TextView banner = view.findViewById(R.id.objectToScanText);
                banner.setText("Scansiona il qr code dell'opera: " + oggetto.getNome() + " per continuare");
                AlphaAnimation alphaAnim = new AlphaAnimation(1.0f,0.0f);
                alphaAnim.setStartOffset(2000);                        // start in 5 seconds
                alphaAnim.setDuration(3000);
                alphaAnim.setAnimationListener(new Animation.AnimationListener()
                {
                    @Override
                    public void onAnimationStart(Animation animation) { banner.setVisibility(View.VISIBLE);}

                    public void onAnimationEnd(Animation animation) { banner.setVisibility(View.INVISIBLE); }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                banner.setAnimation(alphaAnim);
            }
        }

        arFragment = (ArFragment) getChildFragmentManager().findFragmentById(R.id.ar_fragment);
        arFragment.setOnTapArPlaneListener(((hitResult, plane, motionEvent) -> {
            if (oggetto != null && oggetto.getResourcePath() != null &&!oggetto.getResourcePath().isEmpty())
                placeModel(hitResult.createAnchor());
            else
                Toast.makeText(requireContext(), "Scansiona un qr code del museo per continuare",Toast.LENGTH_SHORT).show();
        }));

        view.findViewById(R.id.launchScanner).setOnClickListener(v -> startQrCodeScanner());
    }

    private void placeModel(Anchor anchor) {
        for (int j = arFragment.getArSceneView().getScene().getChildren().size()-1; j>=1 ; j--) {
            Node childNode = arFragment.getArSceneView().getScene().getChildren().get(j);
            arFragment.getArSceneView().getScene().removeChild(childNode);
        }
        ModelRenderable.builder()
                .setSource(requireContext(),
                        RenderableSource.builder()
                                //TODO:imposta risorsa del file gltf (oggetto.getFirstUrlFileByExtension("gltf"))
                                //.setSource(requireContext(), Uri.parse("file:///" + oggetto.getResourcePath()), RenderableSource.SourceType.GLTF2)
                                .setSource(requireContext(), Uri.parse(ASSET_ONLINE), RenderableSource.SourceType.GLTF2)
                                .build()
                ).setRegistryId(ASSET_ONLINE)
                .build()
                .thenAccept(renderable -> {
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setRenderable(renderable);
                    anchorNode.setLocalScale(new Vector3(0.3f,0.3f,0.3f));
                    arFragment.getArSceneView().getScene().addChild(anchorNode);
                })
                .exceptionally(throwable -> {
                    Log.e("ARModel", "Unable to load model: " + throwable.getMessage());
                    Toast.makeText(requireContext(), getString(R.string.errorModelLoad), Toast.LENGTH_LONG).show();
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
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setCaptureActivity(CaptureActivity.class);
        options.setPrompt("Scan a museum QR code");
        options.setCameraId(0);
        options.setBeepEnabled(false);
        barcodeLauncher.launch(options);
    }

    //TODO:elimina funzione
    private void handleObject() {
        if(!checkFileFromDirectory()) {
            Handler handler = new Handler();
            secondsElapsed = 0;
            Dialog dialog = new Dialog(requireContext());
            dialog.setContentView(R.layout.progress_layout);
            ((TextView) dialog.findViewById(R.id.progressMessage)).setText(R.string.waitingText);
            TextView counter = dialog.findViewById(R.id.progressText);
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            secondsElapsed++;
                            counter.setText("" + secondsElapsed);
                            handler.postDelayed(this, 1000);
                        }
                    }, 1000);
                }
            });

            dialog.show();
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            Converter.convertToGltf(oggetto, new ConversionCallback() {
                @Override
                public void onConversionComplete(byte[] outputData) {
                    File path = requireContext().getFilesDir();
                    String filename = oggetto.getFileName() + ".gltf";
                    try {
                        FileOutputStream writer = new FileOutputStream(new File(path, filename));
                        writer.write(outputData);
                        writer.close();
                        requireActivity().runOnUiThread(() -> {
                            oggetto.setResourcePath(path + "/" + filename);
                            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                            dialog.dismiss();
                            handler.removeCallbacksAndMessages(null);
                            Toast.makeText(requireContext(), "Premi sullo schermo per visualizzare l'oggetto",Toast.LENGTH_SHORT).show();
                        });
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onConversionFailed() {
                    Toast.makeText(requireContext(), "Errore nel caricamento dell'oggetto " + oggetto.getFileName(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            File path = requireContext().getFilesDir();
            String filename = oggetto.getFileName() + ".gltf";
            oggetto.setResourcePath(path + "/" + filename);
            Toast.makeText(requireContext(), "Premi sullo schermo per visualizzare l'oggetto",Toast.LENGTH_SHORT).show();
        }
    }

    private Oggetto3D handleQRCode(String qrCodeData) {
        oggetto = null;
        int id = Integer.parseInt(qrCodeData.split("-")[0]);
        for(Oggetto3D o : oggetti)
            if(o.getId() == id)
                return o;
        return null;
    }

    //TODO:elimina funzione
    private boolean checkFileFromDirectory() {
        File directory = requireContext().getFilesDir();
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().equals(oggetto.getFileName() + ".gltf")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
