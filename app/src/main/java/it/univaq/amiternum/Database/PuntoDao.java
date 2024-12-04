package it.univaq.amiternum.Database;


import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import it.univaq.amiternum.Model.Punto;

import java.util.List;

@Dao
public abstract class PuntoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insert(List<Punto> punti);

    @Update
    public abstract void update(Punto punto);

    @Query("DELETE FROM punti")
    public abstract void deleteAll();

    @Query("SELECT * FROM punti ORDER BY luogo DESC")
    public abstract List<Punto> findAll();
}
