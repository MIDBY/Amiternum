package it.univaq.amiternum.Database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import it.univaq.amiternum.Model.Oggetto3D;
import it.univaq.amiternum.Model.Punto;

@Database(entities = {Punto.class, Oggetto3D.class}, version = 2)

public abstract class DB extends RoomDatabase {

    public abstract PuntoDao getPuntoDao();
    public abstract OggettoDao getOggettoDao();

    private static volatile DB instance = null;

    public static synchronized DB getInstance(Context context){
        if(instance == null){
            synchronized (DB.class){
                if(instance == null) {
                    instance = Room.databaseBuilder(context, DB.class, "RoomDB.db").build();
                }
            }
        }
        return instance;
    }
}
