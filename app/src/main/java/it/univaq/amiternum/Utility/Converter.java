package it.univaq.amiternum.Utility;

import com.aspose.threed.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class Converter {

    public static void convertToGltf(String objUrl, String mtlUrl, String[] textureUrls, ConversionCallback callback) {
        new Thread(() -> {
            try {
                // Download the .obj file
                byte[] objData = downloadFile(objUrl);
                byte[] mtlData = downloadFile(mtlUrl);
                byte[][] textureData = new byte[textureUrls.length][];

                // Download each texture file
                for (int i = 0; i < textureUrls.length; i++) {
                    textureData[i] = downloadFile(textureUrls[i]);
                }

                // Create a temporary directory to store the files
                File tempDir = new File(System.getProperty("java.io.tmpdir"), "aspose_temp");
                if (!tempDir.exists()) {
                    tempDir.mkdir();
                }

                // Save the downloaded files to temporary files
                File objFile = new File(tempDir, "model.obj");
                File mtlFile = new File(tempDir, "model.mtl");
                writeToFile(objFile, objData);
                writeToFile(mtlFile, mtlData);

                // Save texture files
                for (int i = 0; i < textureData.length; i++) {
                    File textureFile = new File(tempDir, "texture" + i + ".jpeg");
                    writeToFile(textureFile, textureData[i]);
                }

                // Load the scene from the .obj file
                Scene scene = new Scene();
                scene.open(objFile.getPath());

                // Prepare GLTF save options
                GltfSaveOptions saveOptions = new GltfSaveOptions(FileFormat.GLTF);
                saveOptions.setEmbedAssets(true);  // Embed textures directly in GLTF
                saveOptions.setPrettyPrint(true);    // Readable JSON output

                // Generate output filename
                String outputPath = tempDir.getAbsolutePath() + File.separator + "model.gltf";

                // Save the scene as GLTF
                scene.save(outputPath, saveOptions);

                // Clean up temporary files
                deleteDirectory(tempDir);

                callback.onConversionComplete(outputPath);

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
}