package com.w3engineers.mesh.util;

import java.io.IOException;

public class Util {

    public interface ConnectionCheck {
        void onConnectionCheck(boolean isConnected);
    }

    public static void isConnected(ConnectionCheck connectionCheck) {
        new Thread(() -> {
            try {
                final String command = "ping -c 1 google.com";
                boolean isSuccess = Runtime.getRuntime().exec(command).waitFor() == 0;
                connectionCheck.onConnectionCheck(isSuccess);
            } catch (InterruptedException | IOException e) {
                connectionCheck.onConnectionCheck(false);
            }
        }).start();
    }
}
