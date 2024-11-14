package it.univaq.amiternum.Utility;

public interface ConversionCallback {

    void onConversionComplete(String outputPath);
    void onConversionFailed();
}
