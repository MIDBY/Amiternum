package it.univaq.amiternum.Fragment;

import static com.google.zxing.integration.android.IntentIntegrator.REQUEST_CODE;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import it.univaq.amiternum.Database.DB;
import it.univaq.amiternum.Model.Oggetto3D;
import it.univaq.amiternum.R;
import it.univaq.amiternum.Utility.GetData;
import it.univaq.amiternum.Utility.InternetRequest;
import it.univaq.amiternum.Utility.OnRequest;
import it.univaq.amiternum.Utility.Pref;

public class ListFragment extends Fragment {

    private RecyclerView recyclerView;
    private final List<Oggetto3D> oggetti = new ArrayList<>();
    private final Adapter adapter = new Adapter(oggetti);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((TextView) view.findViewById(R.id.titleHomepage)).setText(R.string.subtitle);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setAdapter(adapter);

        if(oggetti.isEmpty()) {
            if (Pref.load(requireContext(), "firstAccess", true)) {
                Dialog dialog = new Dialog(requireContext());
                dialog.setContentView(R.layout.progress_layout);
                ProgressBar bar = dialog.findViewById(R.id.progressBar);
                bar.setProgress(0);
                bar.setMax(100);
                dialog.show();
                InternetRequest.asyncRequestOggetto(new OnRequest() {
                    @Override
                    public void onRequestCompleted(String data) {
                        try{
                            JSONArray array = new JSONArray(data);
                            for(int i = 0; i < array.length(); i++) {
                                JSONObject item = array.getJSONObject(i);
                                Oggetto3D oggetto = Oggetto3D.parseJson(item);
                                oggetti.add(oggetto);
                            }
                        } catch (JSONException e) {
                            onParseFailed();
                        }
                        DB.getInstance(requireContext()).getOggettoDao().insert(oggetti);
                        dialog.dismiss();
                        recyclerView.post(adapter::notifyDataSetChanged);
                    }

                    @Override
                    public void onRequestUpdate(int progress) {
                        ProgressBar bar = dialog.findViewById(R.id.progressBar);
                        bar.setProgress(progress);
                        TextView text = dialog.findViewById(R.id.progressText);
                        text.setText(progress + " %");
                    }

                    @Override
                    public void onRequestFailed() {
                        Log.d("CONNECTION FAILED","problemi nella richiesta");
                        dialog.dismiss();
                    }

                    public void onParseFailed() {
                        Log.d("PARSE FAILED", "problemi nel parsing");
                        dialog.dismiss();
                    }
                });
                GetData.downloadPunto(requireContext());
                Pref.save(requireContext(), "firstAccess", false);
            } else {
                new Thread(() -> {
                    oggetti.addAll(DB.getInstance(getContext()).getOggettoDao().findAll());
                    recyclerView.post(adapter::notifyDataSetChanged);
                }).start();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }
}
