package it.univaq.amiternum.Utility;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class InternetRequest {

    //USO HTTP URL CONNECTION github-server pointDataset
    public static void asyncRequestPunto(OnRequest listener){
        new Thread(()->{
            String data = doRequest("GET", "https://MIDBY.github.io/Amiternum-project/pointDataset.json", listener);
            if (data != null && !data.isEmpty()){
                if(listener != null)
                    listener.onRequestCompleted(data);
            } else
                listener.onRequestFailed();
        }).start();
    }

    public static void asyncRequestOggetto(OnRequest listener){
        new Thread(()->{
            String data = doRequest("GET","http://med-quad.univaq.it/univaq/vr/script.php", listener);
            if (data != null && !data.isEmpty()){
                if(listener != null)
                    listener.onRequestCompleted(data);
            } else
                listener.onRequestFailed();
        }).start();
    }


    private static String doRequest(String method, String address, OnRequest listener){
        HttpURLConnection connection = null;
        try{
            URL url = new URL(address);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK){
                InputStream in = connection.getInputStream();
                StringBuilder sb = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;

                int length = 0;
                try {
                    length = Integer.parseInt(connection.getHeaderField("Content-length"));
                } catch (NumberFormatException ignored) {
                    length = 1000;
                }

                while((line = reader.readLine()) != null) {
                    sb.append(line);
                    int percentage = sb.length() * 100 / length;
                    if(listener != null)
                        listener.onRequestUpdate(percentage);
                }
                return sb.toString();
            } else {
                connection.getErrorStream();
                Log.d("CONNECTION","errore nella connessione");
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if (connection != null)
                connection.disconnect();
        }

        return null;
    }
}
