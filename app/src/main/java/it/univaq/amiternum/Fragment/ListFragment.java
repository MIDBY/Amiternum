package it.univaq.amiternum.Fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((TextView) view.findViewById(R.id.titleHomepage)).setText(R.string.subtitle);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setAdapter(adapter);

        if(oggetti.isEmpty()) {
            if (Pref.load(requireContext(), "firstAccess", true)) {
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
                        recyclerView.post(adapter::notifyDataSetChanged);
                    }

                    @Override
                    public void onRequestFailed() {
                        Log.d("CONNECTION FAILED","problemi nella richiesta");
                    }

                    public void onParseFailed() {
                        Log.d("PARSE FAILED", "problemi nel parsing");
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
