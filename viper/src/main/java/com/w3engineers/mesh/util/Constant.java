package com.w3engineers.mesh.util;

import android.graphics.Color;
import android.os.Environment;

import java.util.UUID;

public class Constant {

    public static String KEY_USER_ID = "mesh_id";
    public static String KEY_USER_NAME = "mesh_name";

    public static String CURRENT_LOG_FILE_NAME;
    public static String SOCKET_URL = "https://signal.telemesh.net";

    public interface MessageStatus {
        int SENDING = 0;
        int SEND = 1;
        int DELIVERED = 2;
        int RECEIVED = 3;
        int FAILED = 4;
        int RECEIVING = 5;
    }

    public interface PreferenceKeys {
        String ADDRESS = "eth_address";
        String AUTH_USER_NAME = "AUTH_USER_NAME";
        String AUTH_PASSWORD = "AUTH_PASSWORD";
        String APP_DOWNLOAD_LINK = "APP_DOWNLOAD_LINK";
        String APP_VERSION = "APP_VERSION";
        String APP_VERSION_NAME = "APP_VERSION_NAME";
        String APP_SIZE = "APP_SIZE";
        String IS_SETTINGS_PERMISSION_DONE = "is_settings_permission_done";
    }

    public interface Directory {
        String PARENT_DIRECTORY = Environment.getExternalStorageDirectory().toString() + "/Telemesh/";
        String MESH_LOG = "/MeshLog/";
    }
}
