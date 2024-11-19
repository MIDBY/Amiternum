package it.univaq.amiternum.Utility;

import com.aspose.threed.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import it.univaq.amiternum.Model.Oggetto3D;

public class Converter {

    public static void convertToGltf(Oggetto3D oggetto, ConversionCallback callback) {
        new Thread(() -> {
            try {
                // Download the .obj file
                ObjLoadOptions options = new ObjLoadOptions();
                options.setEnableMaterials(true);
                //.setFlipCoordinateSystem(true);
                Scene scene = new Scene();
                try (InputStream inputStream = getInputStreamFile(oggetto.getObjUrlFile())) {
                    scene = Scene.fromStream(inputStream, options);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                /*
                byte[] mtlData = downloadFile(oggetto.getMtlUrlFile());

                // Download each texture file
                String[] textureUrls = oggetto.getImgUrlFiles();
                byte[][] textureData = new byte[textureUrls.length][];
                for (int i = 0; i < textureUrls.length; i++) {
                    textureData[i] = downloadFile(textureUrls[i]);
                }
*/
                // Create a temporary directory to store the files
                File tempDir = new File(System.getProperty("java.io.tmpdir"), "aspose_temp");
                if (!tempDir.exists()) {
                    tempDir.mkdir();
                }
/*
                // Save the downloaded files to temporary files
                File mtlFile = new File(urlFileName(oggetto.getMtlUrlFile()));
                writeToFile(mtlFile, mtlData);

                // Save texture files
                for (int i = 0; i < textureData.length; i++) {
                    File textureFile = new File(urlFileName(textureUrls[i]));
                    writeToFile(textureFile, textureData[i]);
                }
                options.getLookupPaths().add(mtlFile.getPath());
*/
                // Prepare GLTF save options
                GltfSaveOptions saveOptions = new GltfSaveOptions(FileFormat.GLTF2);
                saveOptions.setEmbedAssets(true);  // Embed textures directly in GLTF

                // Generate output filename
                //String outputPath = tempDir.getAbsolutePath() + File.separator + "model.gltf";
                byte[] gltfData;
                try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                    scene.save(stream, saveOptions);
                    gltfData = stream.toByteArray();
                }
                // Clean up temporary files
                //deleteDirectory(tempDir);
                if(gltfData != null)
                    callback.onConversionComplete(gltfData);

            } catch (Exception e) {
                e.printStackTrace();
                callback.onConversionFailed();
            }
        }).start();
    }

    // Method to download a file from a URL
    private static byte[] downloadFile(String fileUrl) throws IOException {
        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        connection.connect();

        try (InputStream inputStream = connection.getInputStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        }
    }

    private static InputStream getInputStreamFile(String fileUrl) throws IOException {
        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        connection.connect();
        if(connection.getResponseCode() != HttpURLConnection.HTTP_OK)
            throw new IOException("Http error code: " + connection.getResponseMessage());
        else
            return connection.getInputStream();
    }

    // Method to write byte array to a file
    private static void writeToFile(File file, byte[] data) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
    }

    // Method to delete a directory and its contents
    private static void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        directory.delete();
    }

    private static String urlFileName(String url) {
        String[] splitted = url.split("/");
        return splitted[splitted.length -1];
    }
}