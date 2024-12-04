package it.univaq.amiternum.Utility;

import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import it.univaq.amiternum.Database.DB;
import it.univaq.amiternum.Model.Oggetto3D;
import it.univaq.amiternum.Model.Punto;
import java.util.ArrayList;

public class GetData {

    static ArrayList<Punto> punti = new ArrayList<>();
    static ArrayList<Oggetto3D> oggetti = new ArrayList<>();

    public static ArrayList<Punto> downloadPunto(Context context){
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
                } catch (JSONException e) {
                    e.printStackTrace();
                    onParseFailed();
                }
            }

            @Override
            public void onRequestFailed() {
                Log.d("CONNECTION FAILED","problemi nella richiesta");
            }

            public void onParseFailed() {
                Log.d("PARSE FAILED", "problemi nel parsing");
            }
        });

        return punti;
    }

    public static ArrayList<Oggetto3D> downloadOggetto(Context context){
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
            }

            @Override
            public void onRequestFailed() {
                Log.d("CONNECTION FAILED","problemi nella richiesta");
            }

            public void onParseFailed() {
                Log.d("PARSE FAILED", "problemi nel parsing");
            }
        });

        return oggetti;
    }
}
