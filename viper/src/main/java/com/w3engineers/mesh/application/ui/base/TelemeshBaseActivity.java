package com.w3engineers.mesh.application.ui.base;


/*
 * ============================================================================
 * Copyright (C) 2019 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * ============================================================================
 */

import com.w3engineers.ext.strom.application.ui.base.BaseActivity;
import com.w3engineers.mesh.application.data.BaseServiceLocator;
import com.w3engineers.mesh.util.lib.mesh.DataManager;


/**
 * Currently this Activity only serves to show service notification only.
 * This is a base to adapt common tasks for RM at activity layer over the time
 * https://code.leftofthedot.com/azim/android-framework/issues/5
 */
public abstract class TelemeshBaseActivity extends BaseActivity {

    public abstract BaseServiceLocator a();

    @Override
    public void startUI() {
        startMeshService();
    }

    @Override
    public void onPause() {
        super.onPause();
        setServiceForeground(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        setServiceForeground(false);
    }

    /**
     * Exposed to child Activity so that showing service behavior can be modified as desired
     * @param isForeground
     */
    public void setServiceForeground(boolean isForeground) {

        DataManager.on().setServiceForeground(isForeground);

    }

    private void startMeshService() {

        BaseServiceLocator baseServiceLocator = a();

        if (baseServiceLocator != null) {
            baseServiceLocator.initViper();
        }
    }
}
