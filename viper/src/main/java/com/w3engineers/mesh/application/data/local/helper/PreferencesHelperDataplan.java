package com.w3engineers.mesh.application.data.local.helper;


import com.w3engineers.mesh.application.data.local.db.SharedPref;

public class PreferencesHelperDataplan {


    private static final String CONFIG_VERSION = "CONFIG_VERSION";
    private static final String TOKEN_GUIDE_VERSION = "TOKEN_GUIDE_VERSION";
    private static final String PER_MB_TKN_VALUE = "PER_MB_TKN_VALUE";
    private static final String MAX_POINT_FOR_RMESH = "MAX_POINT_FOR_RMESH";
    private static final String RMESH_PER_POINT = "RMESH_PER_POINT";

    private static final String WALLET_RMESH_AVAILABLE = "WALLET_RMESH_AVAILABLE";
    private static final String RMESH_INFO_TEXT = "RMESH_INFO_TEXT";
    private static final String RMESH_OWNER_ADDRESS = "RMESH_OWNER_ADDRESS";
    private static final String MAINNET_NETWORK_TYPE = "MAINNET_NETWORK_TYPE";

    private PreferencesHelperDataplan() {

    }

    private static PreferencesHelperDataplan sInstance;

    synchronized public static PreferencesHelperDataplan on() {
        if (sInstance == null) {
            sInstance = new PreferencesHelperDataplan();
        }
        return sInstance;
    }

}
