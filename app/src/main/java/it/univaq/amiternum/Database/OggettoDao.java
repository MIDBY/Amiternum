package it.univaq.amiternum.Database;


import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.ArrayList;
import java.util.List;

import it.univaq.amiternum.Model.Oggetto3D;

@Dao
public abstract class OggettoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insert(List<Oggetto3D> oggetti);

    @Update
    public abstract void update(Oggetto3D oggetto);

    @Query("DELETE FROM oggetti")
    public abstract void deleteAll();

    @Query("SELECT * FROM oggetti ORDER BY id")
    public abstract List<Oggetto3D> findAll();
}
