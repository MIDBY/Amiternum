package it.univaq.amiternum.Utility;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.aspose.threed.FileFormat;
import com.aspose.threed.GltfSaveOptions;
import com.aspose.threed.Material;
import com.aspose.threed.Scene;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import it.univaq.amiternum.Database.DB;
import it.univaq.amiternum.Model.Oggetto3D;
import it.univaq.amiternum.Model.Punto;
import it.univaq.amiternum.R;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GetData {

    static Dialog dialog;
    static Dialog dialog2;
    static ArrayList<Punto> punti = new ArrayList<>();
    static ArrayList<Oggetto3D> oggetti = new ArrayList<>();

    public static ArrayList<Punto> loadPunto(Context context){
        if(punti.isEmpty()) {
            new Thread(() -> {
                punti.addAll(DB.getInstance(context).getPuntoDao().findAll());
            }).start();
            while (punti.isEmpty()){}
        }
        return punti;
    }

    public static ArrayList<Oggetto3D> loadOggetto(Context context){
        if(oggetti.isEmpty()) {
            new Thread(() -> oggetti.addAll(DB.getInstance(context).getOggettoDao().findAll())).start();
            while (oggetti.isEmpty()) {}
        }
        return oggetti;
    }

    public static ArrayList<Punto> downloadPunto(Context context){
        dialog = new Dialog(context.getApplicationContext());
        dialog.setContentView(R.layout.progress_layout);
        ProgressBar bar = dialog.findViewById(R.id.progressBar);
        bar.setProgress(0);
        bar.setMax(100);
        dialog.setCancelable(true);
        //dialog.show();
        InternetRequest.asyncRequestPunto(new OnRequest() {
            @Override
            public void onRequestCompleted(String data) {
                try{
                    JSONArray array = new JSONArray(data);
                    for(int i = 0; i < array.length(); i++) {
                        JSONObject item = array.getJSONObject(i);
                        Punto punto = Punto.parseJson(item);
                        punti.add(punto);
                    }
                    DB.getInstance(context).getPuntoDao().insert(punti);
                    dialog.dismiss();
                } catch (JSONException e) {
                    e.printStackTrace();
                    onParseFailed();
                }
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

        return punti;
    }

    public static ArrayList<Oggetto3D> downloadOggetto(Context context){
        dialog2 = new Dialog(context);
        dialog2.setContentView(R.layout.progress_layout);
        ProgressBar bar = dialog2.findViewById(R.id.progressBar);
        bar.setProgress(0);
        dialog2.setCancelable(true);
        //dialog2.show();
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
                    e.printStackTrace();
                    onParseFailed();
                }
                DB.getInstance(context).getOggettoDao().insert(oggetti);
                dialog2.dismiss();
            }

            @Override
            public void onRequestUpdate(int progress) {
                ProgressBar bar = dialog2.findViewById(R.id.progressBar);
                bar.setProgress(progress);
                TextView text = dialog2.findViewById(R.id.progressText);
                text.setText(progress + " %");

            }

            @Override
            public void onRequestFailed() {
                Log.d("CONNECTION FAILED","problemi nella richiesta");
                dialog2.dismiss();
            }

            public void onParseFailed() {
                Log.d("PARSE FAILED", "problemi nel parsing");
                dialog2.dismiss();
            }
        });

        return oggetti;
    }
}
