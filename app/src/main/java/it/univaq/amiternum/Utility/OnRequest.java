package it.univaq.amiternum.Utility;

public interface OnRequest {

    void onRequestCompleted(String data);
    void onRequestFailed();
}
