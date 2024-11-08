package it.univaq.amiternum.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import it.univaq.amiternum.Database.DB;
import it.univaq.amiternum.Model.Oggetto3D;
import it.univaq.amiternum.R;
import it.univaq.amiternum.Utility.GetData;
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
                GetData.downloadPunto(requireContext());
                ListFragment.this.oggetti.addAll(GetData.downloadOggetto(requireContext()));
                Pref.save(requireContext(), "firstAccess", false);
                adapter.notifyDataSetChanged();
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
