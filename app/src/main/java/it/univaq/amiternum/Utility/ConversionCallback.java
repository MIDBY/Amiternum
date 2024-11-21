package it.univaq.amiternum.Utility;
//TODO: elimina interfaccia
public interface ConversionCallback {

    void onConversionComplete(byte[] gltfData);
    void onConversionFailed();
}
