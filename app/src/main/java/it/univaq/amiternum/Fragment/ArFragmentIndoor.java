package it.univaq.amiternum.Fragment;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
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
import androidx.fragment.app.Fragment;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.assets.RenderableSource;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Light;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;

import it.univaq.amiternum.Database.DB;
import it.univaq.amiternum.Model.Oggetto3D;
import it.univaq.amiternum.R;
import it.univaq.amiternum.Utility.GetData;
import it.univaq.amiternum.Utility.Pref;
import it.univaq.amiternum.helpers.CameraHelper;

public class ArFragmentIndoor extends Fragment {

    private ArFragment arFragment;
    private ArrayList<Oggetto3D> oggetti = new ArrayList<>();
    private Oggetto3D oggetto;
    private int secondsElapsed = 0;
    private Dialog dialog;

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
                    Toast.makeText(requireContext(), getString(R.string.tapScreen),Toast.LENGTH_SHORT).show();
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
                renderText(view, getString(R.string.tapScreen));
            }
        }

        arFragment = (ArFragment) getChildFragmentManager().findFragmentById(R.id.ar_fragment);
        arFragment.setOnTapArPlaneListener(((hitResult, plane, motionEvent) -> {
            if (oggetto != null && oggetto.getGltfUrlFile() != null && !oggetto.getGltfUrlFile().isEmpty())
                placeModel(hitResult.createAnchor());
            else
                renderText(view, getString(R.string.scanText));
        }));

        view.findViewById(R.id.launchScanner).setOnClickListener(v -> startQrCodeScanner());
    }

    private void placeModel(Anchor anchor) {
        for (int j = arFragment.getArSceneView().getScene().getChildren().size()-1; j>=1 ; j--) {
            Node childNode = arFragment.getArSceneView().getScene().getChildren().get(j);
            arFragment.getArSceneView().getScene().removeChild(childNode);
        }

        if(oggetto.getId() % 2 != 0) {
            Handler handler = new Handler();
            secondsElapsed = 0;
            dialog = new Dialog(requireContext());
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
            ModelRenderable.builder()
                    .setSource(requireContext(),
                            RenderableSource.builder()
                                    .setSource(requireContext(), Uri.parse(oggetto.getGltfUrlFile()), RenderableSource.SourceType.GLTF2)
                                    .build()
                    ).setRegistryId(oggetto.getGltfUrlFile())
                    .build()
                    .thenAccept(renderable -> {
                        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                        dialog.dismiss();
                        handler.removeCallbacksAndMessages(null);

                        Light light = Light.builder(Light.Type.DIRECTIONAL)
                                .setColor(new Color(android.graphics.Color.WHITE))
                                .setShadowCastingEnabled(true)
                                .setIntensity(200f)
                                .build();
                        AnchorNode anchorNode = new AnchorNode(anchor);
                        anchorNode.setRenderable(renderable);
                        anchorNode.setLight(light);
                        anchorNode.setLocalScale(new Vector3(1f, 1f, 1f));
                        arFragment.getArSceneView().getScene().addChild(anchorNode);
                    })
                    .exceptionally(throwable -> {
                        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                        dialog.dismiss();
                        handler.removeCallbacksAndMessages(null);

                        Log.e("ARModel", "Unable to load model: " + throwable.getMessage());
                        renderText(requireView(), getString(R.string.errorModelLoad));
                        return null;
                    });
        } else {
            TextView textViewCicle = (TextView) getLayoutInflater().inflate(R.layout.ar_textview, null);
            String textString = "<bold>Titolo:</bold> <small>" + oggetto.getNome() + "</small><br>\n" + "<bold>Descrizione:</bold> <small>" + oggetto.getDescrizione() + "</small>";
            textViewCicle.setText(Html.fromHtml(textString, Html.FROM_HTML_MODE_LEGACY));

            ViewRenderable.builder()
                    .setView(requireContext(), textViewCicle)
                    .build()
                    .thenAccept(renderable -> {
                        Node textViewNodeCycle = new Node();
                        Pose worldPose = anchor.getPose();
                        float scaleFactor = 1.5f;
                        textViewNodeCycle.setRenderable(renderable);
                        textViewNodeCycle.setLocalScale(new Vector3(scaleFactor, scaleFactor, scaleFactor));
                        float[] deviceRotation = worldPose.getRotationQuaternion();
                        Quaternion quaternion = new Quaternion(deviceRotation[0], deviceRotation[1], deviceRotation[2], deviceRotation[3]);
                        Vector3 direction = Vector3.subtract(textViewNodeCycle.getWorldPosition(),
                                new Vector3(worldPose.tx(), worldPose.ty(), worldPose.tz()));
                        Quaternion lookRotation = Quaternion.lookRotation(direction, Vector3.up());
                        Quaternion finalRotation = Quaternion.multiply(quaternion.inverted(), lookRotation);
                        textViewNodeCycle.setWorldRotation(finalRotation);
                        AnchorNode anchorNodeCycle = new AnchorNode(anchor);
                        anchorNodeCycle.addChild(textViewNodeCycle);
                        arFragment.getArSceneView().getScene().addChild(anchorNodeCycle);
                    });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        arFragment.onPause();
        if(dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            Toast.makeText(requireContext(), getString(R.string.impatientText), Toast.LENGTH_SHORT).show();
        }
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

    private Oggetto3D handleQRCode(String qrCodeData) {
        oggetto = null;
        int id = Integer.parseInt(qrCodeData.split("-")[0]);
        for(Oggetto3D o : oggetti)
            if(o.getId() == id)
                return o;
        return null;
    }

    private void renderText(View view, String text) {
        TextView banner = view.findViewById(R.id.objectToScanText);
        banner.setText(text);
        AlphaAnimation alphaAnim = new AlphaAnimation(1.0f, 0.0f);
        alphaAnim.setStartOffset(2000);                        // start in 5 seconds
        alphaAnim.setDuration(3000);
        alphaAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                banner.setVisibility(View.VISIBLE);
            }

            public void onAnimationEnd(Animation animation) {
                banner.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        banner.setAnimation(alphaAnim);
    }
}
