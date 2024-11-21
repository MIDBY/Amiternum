package it.univaq.amiternum.Model;

import androidx.room.Entity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Entity(tableName = "oggetti", primaryKeys = {"id"})
public class Oggetto3D implements Serializable {
    private int id;
    private String nome;
    private String descrizione;
    private String urlAudio;
    private String urlVideo;
    private String urlFiles;
    //TODO:elimina attributo con sue classi
    private String resourcePath;

    public static Oggetto3D parseJson(JSONObject json){
        Oggetto3D oggetto = new Oggetto3D();
        oggetto.setId(json.optInt("id"));
        oggetto.setNome(json.optString("name"));
        oggetto.setDescrizione(json.optString("description"));
        oggetto.setUrlAudio(json.optString("audio"));
        oggetto.setUrlVideo(json.optString("video"));
        JSONArray filesArray = json.optJSONArray("files");
        if(filesArray != null) {
            StringBuilder files = new StringBuilder();
            for(int i = 0; i < filesArray.length(); i++) {
                files.append(filesArray.optString(i));
                if (i < filesArray.length())
                    files.append(",");
            }
            oggetto.setUrlFiles(String.valueOf(files));
        }
        return oggetto;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getUrlAudio() {
        return urlAudio;
    }

    public void setUrlAudio(String urlAudio) {
        this.urlAudio = urlAudio;
    }

    public String getUrlVideo() {
        return urlVideo;
    }

    public void setUrlVideo(String urlVideo) {
        this.urlVideo = urlVideo;
    }

    public List<String> getUrlFilesAsList() {
        if(urlFiles != null)
            return Arrays.asList(urlFiles.split(","));
        else
            return new ArrayList<>();
    }

    public String getUrlFiles() {
        return urlFiles;
    }

    public void setUrlFiles(String urlFiles) {
        this.urlFiles = urlFiles;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String getFirstUrlFileByExtension(String extension) {
        List<String> files = getUrlFilesAsList();
        if(extension.equals("jpg") || extension.equals("png")) {
            for (String s : files)
                if (s.endsWith("jpg") || s.endsWith("png"))
                    return s;
        } else {
            for (String s : files)
                if (s.endsWith(extension))
                    return s;
        }
        return null;
    }

    public String getObjUrlFile() {
        List<String> files = getUrlFilesAsList();
        for(String s : files) {
            if(s.endsWith(".obj"))
                return s;
        }
        return null;
    }

    public String getMtlUrlFile() {
        List<String> files = getUrlFilesAsList();
        for(String s : files) {
            if(s.endsWith(".mtl"))
                return s;
        }
        return null;
    }

    public String[] getImgUrlFiles() {
        List<String> files = getUrlFilesAsList();

        return files.stream()
            .filter(s -> !s.endsWith(".obj") && !s.endsWith(".mtl"))
            .toArray(String[]::new);
    }

    public String getFileName() {
        String[] splitted = getObjUrlFile().split("/");
        return splitted[splitted.length - 1].split("\\.")[0];
    }
}
