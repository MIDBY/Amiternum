package it.univaq.amiternum.Utility;

public interface ConversionCallback {

    void onConversionComplete(byte[] gltfData);
    void onConversionFailed();
}
