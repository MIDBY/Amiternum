package it.univaq.amiternum.Utility;

public interface OnRequest {

    void onRequestCompleted(String data);
    void onRequestUpdate(int progress);
    void onRequestFailed();
}
