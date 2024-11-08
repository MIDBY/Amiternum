package it.univaq.amiternum.Model;

import androidx.room.Entity;
import org.json.JSONObject;

@Entity(tableName = "punti", primaryKeys = {"latitudine","longitudine"})

public class Punto {
    private String luogo;
    private String descrizione;
    private double latitudine;
    private double longitudine;


    public static Punto parseJson(JSONObject json){
        Punto punto = new Punto();
        punto.setLuogo(json.optString("name"));
        punto.setDescrizione(json.optString("description"));
        punto.setLatitudine(json.optDouble("latitude"));
        punto.setLongitudine(json.optDouble("longitude"));
        return punto;
    }

    public String getLuogo() {
        return luogo;
    }

    public void setLuogo(String luogo) {
        this.luogo = luogo;
    }

    public double getLatitudine() {
        return latitudine;
    }

    public void setLatitudine(double latitudine) {
        this.latitudine = latitudine;
    }

    public double getLongitudine() {
        return longitudine;
    }

    public void setLongitudine(double longitudine) {
        this.longitudine = longitudine;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }
}

