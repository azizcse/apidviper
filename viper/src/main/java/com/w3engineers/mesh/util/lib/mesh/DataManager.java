package com.w3engineers.mesh.util.lib.mesh;
 
/*
============================================================================
Copyright (C) 2019 W3 Engineers Ltd. - All Rights Reserved.
Unauthorized copying of this file, via any medium is strictly prohibited
Proprietary and confidential
============================================================================
*/

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Build;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.FileProvider;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.ext.strom.util.helper.Toaster;
import com.w3engineers.mesh.BuildConfig;
import com.w3engineers.mesh.R;
import com.w3engineers.mesh.ViperCommunicator;
import com.w3engineers.mesh.application.data.AppDataObserver;
import com.w3engineers.mesh.application.data.local.DataPlanConstants;
import com.w3engineers.mesh.application.data.local.db.SharedPref;
import com.w3engineers.mesh.application.data.local.helper.PreferencesHelperDataplan;
import com.w3engineers.mesh.application.data.model.BroadcastEvent;
import com.w3engineers.mesh.application.data.model.DataAckEvent;
import com.w3engineers.mesh.application.data.model.DataEvent;
import com.w3engineers.mesh.application.data.model.FilePendingEvent;
import com.w3engineers.mesh.application.data.model.FileProgressEvent;
import com.w3engineers.mesh.application.data.model.FileReceivedEvent;
import com.w3engineers.mesh.application.data.model.FileTransferEvent;
import com.w3engineers.mesh.application.data.model.PeerAdd;
import com.w3engineers.mesh.application.data.model.PeerRemoved;
import com.w3engineers.mesh.application.data.model.PermissionInterruptionEvent;
import com.w3engineers.mesh.application.data.model.ServiceDestroyed;
import com.w3engineers.mesh.application.data.model.ServiceUpdate;
import com.w3engineers.mesh.application.data.model.TransportInit;
import com.w3engineers.mesh.application.data.model.UserInfoEvent;
import com.w3engineers.mesh.application.data.model.WalletCreationEvent;
import com.w3engineers.mesh.application.data.model.WalletLoaded;
import com.w3engineers.mesh.databinding.DialogServiceAppInstallProgressBinding;
import com.w3engineers.mesh.util.AppBackupUtil;
import com.w3engineers.mesh.util.CommonUtil;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.DialogUtil;
import com.w3engineers.mesh.util.MeshApp;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.util.NetworkMonitor;
import com.w3engineers.mesh.util.TSAppInstaller;
import com.w3engineers.mesh.util.Util;
import com.w3engineers.meshrnd.ITmCommunicator;
import com.w3engineers.models.BroadcastData;
import com.w3engineers.models.FileData;
import com.w3engineers.models.MessageData;
import com.w3engineers.models.PendingContentInfo;
import com.w3engineers.models.UserInfo;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class DataManager {

    private ITmCommunicator mTmCommunicator;
    private ViperCommunicator mViperCommunicator;
    private Context mContext;
    private UserInfo userInfo;
    private String signalServerUrl;
    private String appTokenName;
    private String DEVICE_NAME = "xiaomi";

    private static DataManager mDataManager;
    private boolean isAlreadyToPlayStore = false;

    // For App update
    private String appDownloadId = "";
    private String appPath;
    private boolean isAppUpdating;
    private static DialogServiceAppInstallProgressBinding binding;
    private static AlertDialog dialog;
    private android.support.v7.app.AlertDialog appUpdateDialog;
    private AlertDialog newAppUpdateDialog;

    /**
     * It has first part is file id and second part is file path
     */
    private HashMap<String, String> mAppUpdatePathMap;

    private DataManager() {
        mAppUpdatePathMap = new HashMap<>();
        //Prevent form the reflection api.
        if (mDataManager != null) {
            throw new RuntimeException("Use on() method to get the single instance of this class.");
        }
    }

    public static DataManager on() {
        if (mDataManager == null) {
            synchronized (DataManager.class) {
                if (mDataManager == null) mDataManager = new DataManager();
            }
        }
        return mDataManager;
    }

    /**
     * Start the ClientLibraryService class
     *
     * @param context
     */
    public void doBindService(Context context, UserInfo userInfo, String appTokenName) {
        this.mContext = context;
        this.userInfo = userInfo;
        this.appTokenName = appTokenName;
    }

    public void startMeshService() {
        checkAndBindService();
    }

    public String getAppTokenName() {
        return appTokenName;
    }

    public void stopService() {
        mContext.unbindService(serviceConnection);
    }


    /**
     * <h1>Note:</h1>
     *
     * <h1>Author: Azizul Islam</h1>
     *
     * <p>Purpose to check service connection with a time interval.
     * If TeleMeshService is not installed then notify user to install
     * the service app. In future we will show an alert dialog with play store
     * link to install the service app</p>
     */
    private void checkAndBindService() {
        HandlerUtil.postBackground(new Runnable() {
            @Override
            public void run() {
                if (mTmCommunicator == null) {
                    boolean isSuccess = initServiceConnection();

                    if (isSuccess) {
                        Toaster.showShort("Bind service connection successful");
                        return;
                    }

                    if (CommonUtil.isEmulator()) {
                        isAlreadyToPlayStore = true;
                    }

                    HandlerUtil.postBackground(this, 5000);

                    if (!isAlreadyToPlayStore) {
                        showConfirmationPopUp();
                    }
                    isAlreadyToPlayStore = true;
                }
            }
        });
    }

    private void showConfirmationPopUp() {

        DialogUtil.showConfirmationDialog(MeshApp.getCurrentActivity(),
                mContext.getResources().getString(R.string.install_ts),
                mContext.getResources().getString(R.string.need_ts),
                mContext.getString(R.string.cancel),
                mContext.getString(R.string.yes),
                new DialogUtil.DialogButtonListener() {
                    @Override
                    public void onClickPositive() {
                        checkConnectionAndStartDownload();
//                        gotoPlayStore();
                        isAlreadyToPlayStore = true;
                    }

                    @Override
                    public void onCancel() {

                    }

                    @Override
                    public void onClickNegative() {
                        isAlreadyToPlayStore = false;
                    }
                });
    }

    private void gotoPlayStore() {
        final String appPackageName = "com.w3engineers.meshservice";
        //final String appPackageName = "com.w3engineers.banglabrowser";
        try {
            mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }

    // Please don't remove this method. It is needed in our release time
    private void checkConnectionAndStartDownload() {
        // Todo we have to remove below implementation
        Util.isConnected(isConnected ->
                HandlerUtil.postForeground(() -> {
                    if (isConnected) {
                        TSAppInstaller.downloadApkFile(mContext, SharedPref.read(Constant.PreferenceKeys.APP_DOWNLOAD_LINK), null);
                    } else {
                        isAlreadyToPlayStore = false;
                        Toaster.showShort("Internet connection not available");
                    }
                })

        );

        // Todo we hav eto replace ore change below implementation at proper section
        /*if (NetworkMonitor.isOnline()) {
            TSAppInstaller.downloadApkFile(mContext, SharedPref.read(Constant.PreferenceKeys.APP_DOWNLOAD_LINK), NetworkMonitor.getNetwork());
        } else {
            isAlreadyToPlayStore = false;
            Toaster.showShort("Internet connection not available");
        }*/
    }

    private void showPermissionPopUp() {
        Toaster.showLong("showpopup");
        MeshLog.v("mContext  " + mContext);
        if (mContext instanceof Activity) {
            MeshLog.v("yes");
        } else {
            MeshLog.v("no");
        }


        DialogUtil.showConfirmationDialog(MeshApp.getCurrentActivity(),
                mContext.getResources().getString(R.string.permission),
                mContext.getResources().getString(R.string.permission_message),
                mContext.getString(R.string.later),
                mContext.getString(R.string.allow),
                new DialogUtil.DialogButtonListener() {
                    @Override
                    public void onClickPositive() {
                        launchServiceApp();
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }

                    @Override
                    public void onCancel() {

                    }

                    @Override
                    public void onClickNegative() {

                    }
                });
    }

    private void launchServiceApp() {
        Intent intent = mContext.getPackageManager().getLaunchIntentForPackage("com.w3engineers.meshservice");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }


    /**
     * Bind to the remote service
     */
    public boolean initServiceConnection() {
        if (mTmCommunicator == null) {
            Intent intent = new Intent(ITmCommunicator.class.getName());
            /*this is service name that is associated with server end*/
            intent.setAction("service.viper_server");

            /*From 5.0 annonymous intent calls are suspended so replacing with server app's package name*/
            intent.setPackage("com.w3engineers.meshservice");
            // binding to remote service
            return mContext.bindService(intent, serviceConnection, Service.BIND_AUTO_CREATE);
        } else {
            return false;
        }
    }

    public void allowMissingPermission(List<String> missingPermission) {
        try {
            if (mTmCommunicator != null) {
                MeshLog.v("mTmCommunicator.allowPermissions(missingPermission);");
                mTmCommunicator.allowPermissions(missingPermission);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    /**
     * Initializing with remote connection
     */
    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.e("service_status", "onServiceConnected");
            mTmCommunicator = ITmCommunicator.Stub.asInterface(binder);

            try {

//                int configVersion = PreferencesHelperDataplan.on().getConfigVersion();
//                userInfo.setConfigVersion(configVersion);

                boolean status;
                if (CommonUtil.isEmulator()) {
                    status = true;
                } else {
                    mTmCommunicator.startTeleMeshService(viperCommunicator, mContext.getPackageName(), userInfo);
                }

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mTmCommunicator = null;
            Log.v("service_status", "onServiceDisconnected");
        }
    };


    /**
     * <h1>Note:
     * <p>This is client side IPC callback
     * pass this callback when client lib bind with TelemeshService
     * We don't need to bind both service each other. this causes some unnecessary
     * Exception like DeadObject exception. Sometime Server pass data through
     * IPC but Client side does not receive the message.
     * So client side Service and client app will perform its own functionality.
     * Service will keep app data manager alive to receive message </p>
     * </h1>
     */
    private ViperCommunicator.Stub viperCommunicator = new ViperCommunicator.Stub() {
        @Override
        public void onPeerAdd(String peerId) throws RemoteException {
            DataManager.this.onPeerAdd(peerId);
        }

        @Override
        public void onPeerRemoved(String nodeId) throws RemoteException {
            DataManager.this.onPeerRemoved(nodeId);
        }

        @Override
        public void onRemotePeerAdd(String peerId) throws RemoteException {
            DataManager.this.onPeerAdd(peerId);
        }

        @Override
        public void onDataReceived(String senderId, byte[] frameData) throws RemoteException {
            DataManager.this.onDataReceived(senderId, frameData);
        }

        @Override
        public void onAckReceived(String messageId, int status) {
            DataManager.this.onAckReceived(messageId, status);
        }

        @Override
        public void onReceiveLog(String text) throws RemoteException {
            if (BuildConfig.DEBUG) {
                Intent intent = new Intent("com.w3engineers.meshservice.DEBUG_MESSAGE");
                intent.putExtra("value", text);
                MeshApp.getContext().sendBroadcast(intent);
            }

            DataManager.this.writeLogIntoTxtFile(text, true);
        }

        @Override
        public void onUserInfoReceive(List<UserInfo> userInfoList) throws RemoteException {
            DataManager.this.onGetUserInfo(userInfoList);
        }

        @Override
        public void destroyFullService() throws RemoteException {
            serviceDestroyed();
        }

        @Override
        public void setServiceForeground(boolean isForeGround) throws RemoteException {

        }

        @Override
        public void onStartTeleMeshService(boolean isSuccess, String nodeId, String message) throws RemoteException {
            MeshLog.v("onStartTeleMeshService " + isSuccess + " , " + nodeId + " , " + message);
            DataManager.this.onWalletCreationEvent(isSuccess, nodeId, message);
        }

        @Override
        public void onTeleServiceStarted(boolean isSuccess, String nodeId, String pubKey, String message) throws RemoteException {
            onTransportInit(nodeId, pubKey, isSuccess, message);
        }

        @Override
        public void onServiceUpdateNeeded(boolean isNeeded) throws RemoteException {
            DataManager.this.onServiceUpdateNeeded(isNeeded);
        }

        @Override
        public void onInterruption(int hardwareState, List<String> permissions) throws RemoteException {
            onInterruptionAction(hardwareState, permissions);
        }

        @Override
        public void receiveOtherAppVersion(FileData fileData) throws RemoteException {
            checkVersionWithOthers(fileData);
        }

        /**
         * This method call when old version request to app update.
         * This means This app is the latest version holder
         * @param receiverId String requester Id
         * @throws RemoteException
         */
        @Override
        public void onAppUpdateRequest(String receiverId) throws RemoteException {
            String apkPath = AppBackupUtil.backupApkAndGetPath(mContext);
            String metaData = "";

            FileData fileData = new FileData().setReceiverID(receiverId).setFilePath(apkPath)
                    .setMsgMetaData(metaData.getBytes()).setAppToken(mContext.getPackageName());
            String fileResponse = mTmCommunicator.sendInAppUpdateFile(fileData);
            if (fileResponse != null) {

                try {
                    JSONObject jsonObject = new JSONObject(fileResponse);
                    boolean success =  jsonObject.getBoolean("success");
                    String msg = jsonObject.getString("msg");
                    if (success){
                        mAppUpdatePathMap.put(msg, apkPath);
                    } else {
                        HandlerUtil.postForeground(() -> {
                            Toaster.showShort(msg);
                        });

                    }
                } catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        }

        @Override
        public void onFileProgress(String fileTransferId, int percentProgress) throws RemoteException {
            if (appDownloadId.equals(fileTransferId)) {
                updateProgress(percentProgress);
            }
            DataManager.this.onFileProgress(fileTransferId, percentProgress);
        }

        @Override
        public void onFileTransferFinish(String fileTransferId) throws RemoteException {

            if (mAppUpdatePathMap.containsKey(fileTransferId)) {
                String mainFilePath = mAppUpdatePathMap.remove(fileTransferId);
                deleteBackUpApkFile(mainFilePath);
                // It is only in app update part
                return;
            }

            if (appDownloadId.equals(fileTransferId)) {
                // Todo dismiss progress dialog {tariqul} and install app
                closeDialog("App downloaded successfully");
                isAppUpdating = false;

                showAppInstaller();
            }
            DataManager.this.onFileTransferFinish(fileTransferId, true, "");
        }

        @Override
        public void onFileTransferError(String fileTransferId, String errorMessage) throws RemoteException {

            if (mAppUpdatePathMap.containsKey(fileTransferId)) {
                String mainFilePath = mAppUpdatePathMap.remove(fileTransferId);
                deleteBackUpApkFile(mainFilePath);
                // It is only in app update part
                return;
            }

            if (appDownloadId.equals(fileTransferId)) {
                // Todo dismiss progress dialog {tariqul} and show error
                closeDialog("An error occurred");
                isAppUpdating = false;
            }
            DataManager.this.onFileTransferFinish(fileTransferId, false, errorMessage);
        }

        @Override
        public void onFileReceiveStarted(FileData fileData) throws RemoteException {
            // Todo Identify it is app update section or file messaging section
            // Now we are checking the file path that contains ".apk" extension or not. If .apk extension
            // then it is app update process

            if (fileData.getFilePath().endsWith(".apk")) {
                if (!isAppUpdating) {
                    appDownloadId = fileData.getFileTransferId();
                    isAppUpdating = true;
                    appPath = fileData.getFilePath();
                    showProgressDialog();
                }
            } else {
                DataManager.this.onFileReceiveStarted(fileData);
            }
        }

        @Override
        public void onPendingFileReceive(PendingContentInfo pendingContentInfo) throws RemoteException {

            FilePendingEvent event = new FilePendingEvent();
            event.setContentId(pendingContentInfo.getContentId());
            event.setContentPath(pendingContentInfo.getContentPath());
            event.setSenderId(pendingContentInfo.getSenderId());

            event.setProgress(pendingContentInfo.getProgress());
            event.setState(pendingContentInfo.getState());
            event.setContentMetaInfo(pendingContentInfo.getContentMetaInfo());
            event.setIncoming(pendingContentInfo.isIncoming());

            AppDataObserver.on().sendObserverData(event);
        }

        @Override
        public void receiveBroadcast(BroadcastData broadcastData) throws RemoteException {
            BroadcastEvent broadcastEvent = new BroadcastEvent();
            broadcastEvent.setBroadcastId(broadcastData.getBroadcastId())
                    .setMetaData(broadcastData.getMetaData())
                    .setContentPath(broadcastData.getContentPath())
                    .setLatitude(broadcastData.getLatitude())
                    .setLongitude(broadcastData.getLongitude())
                    .setRange(broadcastData.getRange())
                    .setExpiryTime(broadcastData.getExpiryTime());
            AppDataObserver.on().sendObserverData(broadcastEvent);
        }
    };


    private void checkVersionWithOthers(FileData fileData) {
        openNewAppUpdateDialog(fileData);
       /* HandlerUtil.postForeground(() -> {
            int myVersion = SharedPref.readInt(Constant.PreferenceKeys.APP_VERSION);

            if (myVersion < otherAppVersion) {
                if (!isAppUpdating) {
                    Context context = MeshApp.getCurrentActivity();

                    if (appUpdateDialog != null && appUpdateDialog.isShowing()) {
                        appUpdateDialog.dismiss();
                    }

                    String appName = context.getString(R.string.app_name);

                    appUpdateDialog = DialogUtil.showConfirmationDialog(context,
                            "Update " + appName + "?",
                            appName + " recommends that you update to latest version",
                            "Later",
                            "Update",
                            new DialogUtil.DialogButtonListener() {
                                @Override
                                public void onClickPositive() {
                                    if (mTmCommunicator != null) {
                                        try {
                                            mTmCommunicator.sendAppUpdateRequest(receiverId, appTokenName);
                                        } catch (RemoteException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                @Override
                                public void onCancel() {

                                }

                                @Override
                                public void onClickNegative() {

                                }
                            });
                }
            }
        });*/

    }

    private void openNewAppUpdateDialog(FileData fileData) {

        HandlerUtil.postForeground(() -> {
            int myVersionCode = SharedPref.readInt(Constant.PreferenceKeys.APP_VERSION);
            String myVersionName = SharedPref.read(Constant.PreferenceKeys.APP_VERSION_NAME);


            if (myVersionCode < fileData.getAppVersion()) {
                if (!isAppUpdating) {
                    Context context = MeshApp.getCurrentActivity();

                    String appName = context.getString(R.string.app_name);

                    LayoutInflater inflater = LayoutInflater.from(context);
                    View view = inflater.inflate(R.layout.dialog_app_update, null);

                    if (newAppUpdateDialog != null && newAppUpdateDialog.isShowing()) {
                        newAppUpdateDialog.dismiss();
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setView(view);

                    newAppUpdateDialog = builder.create();

                    TextView clientAppName = view.findViewById(R.id.text_view_app_name);
                    TextView clientAppSize = view.findViewById(R.id.text_view_update_size);
                    TextView clientAppCurrentVersion = view.findViewById(R.id.text_view_current_version);
                    TextView clientAppAvailableVersion = view.findViewById(R.id.text_view_available_version);

                    TextView message = view.findViewById(R.id.text_view_message);
                    TextView update = view.findViewById(R.id.text_view_update);
                    TextView later = view.findViewById(R.id.text_view_later);

                    clientAppName.setText(appUpdateAppDialogInfoGenerator(appName));
                    clientAppSize.setText(appUpdateAppDialogInfoGenerator(fileData.getAppSize()));
                    clientAppCurrentVersion.setText(appUpdateAppDialogInfoGenerator(myVersionName));
                    clientAppAvailableVersion.setText(appUpdateAppDialogInfoGenerator(fileData.getVersionName()));

                    Spanned messageText = Html.fromHtml("An update version of the <b>" + "<font color='#083480'>" + appName + "</font>"
                            + "</b> is available from your peers");

                    message.setText(messageText);

                    later.setOnClickListener(view1 -> newAppUpdateDialog.dismiss());

                    update.setOnClickListener(view12 -> {
                        if (mTmCommunicator != null) {
                            try {
                                fileData.setAppToken(appTokenName);
                                mTmCommunicator.sendAppUpdateRequest(fileData);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                        newAppUpdateDialog.dismiss();
                    });


                    newAppUpdateDialog.setCancelable(false);
                    newAppUpdateDialog.setCanceledOnTouchOutside(false);

                    newAppUpdateDialog.show();
                }
            }
        });

    }

/*    public void stopService() {
        mContext.unbindService(serviceConnection);
    }*/

    /**
     * To send any type of data
     *
     * @param messageData
     */
    public void sendData(MessageData messageData) throws RemoteException {
        mTmCommunicator.sendData(messageData);
    }

    public void sendBroadcastData(BroadcastData broadcastData) throws RemoteException {
        mTmCommunicator.sendLocalBroadcast(broadcastData);
    }

    /**
     * This method is for sending any file to other user
     *
     * @return It will return file message id
     * @throws RemoteException
     */
    public String sendFileMessage(FileData fileData) throws RemoteException {
        return mTmCommunicator.sendFile(fileData);
    }

    public void sendFileResumeRequest(FileData fileData) throws RemoteException {
        mTmCommunicator.sendFileResumeRequest(fileData);
    }

    public void removeSendContent(FileData fileData) throws RemoteException {
        mTmCommunicator.removeSendContent(fileData);
    }

    /**
     * To get the int value for connection type
     *
     * @param nodeID
     * @return
     */
    public int getLinkTypeById(String nodeID) throws RemoteException {
        if (mTmCommunicator != null) {
            return mTmCommunicator.getLinkTypeById(nodeID);
        }
        return 0;
    }

    public void saveUserInfo(UserInfo userInfo) throws RemoteException {
        if (mTmCommunicator != null) {

//            int configVersion = PreferencesHelperDataplan.on().getConfigVersion();
//            userInfo.setConfigVersion(configVersion);

            this.userInfo = userInfo;

            mTmCommunicator.saveUserInfo(userInfo);
        }
    }

    public void saveOtherUserInfo(UserInfo userInfo) throws RemoteException {
        if (mTmCommunicator != null) {
            mTmCommunicator.saveOtherUserInfo(userInfo);
        }
    }

    public void triggerForReSyncConfiguration() {
        try {
            if (mTmCommunicator != null) {
                mTmCommunicator.triggerReSyncConfiguration(appTokenName);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public int getMeshUserRole() {
        try {
            if (mTmCommunicator != null) {
                return mTmCommunicator.getUserMeshRole();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return -1;
    }


    /**
     * called when new peer is added
     *
     * @param peerId
     */
    public void onPeerAdd(String peerId) {

        MeshLog.e("discover peer id: " + peerId);

        PeerAdd peerAdd = new PeerAdd();
        peerAdd.peerId = peerId;

        AppDataObserver.on().sendObserverData(peerAdd);

    }

    /**
     * called when a peer leave or removed
     *
     * @param nodeId
     */
    public void onPeerRemoved(String nodeId) {
        PeerRemoved peerRemoved = new PeerRemoved();
        peerRemoved.peerId = nodeId;

        AppDataObserver.on().sendObserverData(peerRemoved);
    }

    /**
     * called when any kind of data receive
     *
     * @param senderId
     * @param frameData
     */
    public void onDataReceived(String senderId, byte[] frameData) {
        try {
            DataEvent dataEvent = new DataEvent();

            dataEvent.peerId = senderId;
            dataEvent.data = frameData;

            AppDataObserver.on().sendObserverData(dataEvent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * called when any kind of ack is received
     *
     * @param messageId
     * @param status
     */
    public void onAckReceived(String messageId, int status) {
        MeshLog.v("onAckReceived " + messageId + "  " + status);
        DataAckEvent dataAckEvent = new DataAckEvent();
        dataAckEvent.dataId = messageId;
        dataAckEvent.status = status;

        AppDataObserver.on().sendObserverData(dataAckEvent);
    }

    public void openWalletCreateUI() {
        try {
            mTmCommunicator.openWalletCreationUI(appTokenName);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void serviceDestroyed() {
        ServiceDestroyed serviceDestroyed = new ServiceDestroyed();
        serviceDestroyed.isFullDestroyed = true;
        AppDataObserver.on().sendObserverData(serviceDestroyed);
    }

    public void onGetUserInfo(List<UserInfo> userInfoList) {

        MeshLog.e("user info list receive in data manager");

        for (UserInfo userInfo : userInfoList) {

            UserInfoEvent userInfoEvent = new UserInfoEvent();

            userInfoEvent.setAddress(userInfo.getAddress());
            userInfoEvent.setAvatar(userInfo.getAvatar());
            userInfoEvent.setUserName(userInfo.getUserName());
            userInfoEvent.setRegTime(userInfo.getRegTime());

            AppDataObserver.on().sendObserverData(userInfoEvent);
        }
    }

    public void setServiceForeground(boolean isForeground) {
        try {
            if (viperCommunicator != null) {
                viperCommunicator.setServiceForeground(isForeground);
            }

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public List<String> getInternetSellers() throws RemoteException {
        if (mTmCommunicator == null) {
            MeshLog.v("mTmCommunicator null");
            return new ArrayList<>();
        }
        return mTmCommunicator.getInternetSellers(appTokenName);
    }

    public void checkConnectionStatus(String userId) throws RemoteException {
        if (mTmCommunicator != null) {
            mTmCommunicator.isLocalUseConnected(userId);
        }
    }

    public int checkUserConnectivityStatus(String userId) throws RemoteException {
        if (mTmCommunicator != null) {
            return mTmCommunicator.checkUserConnectivityStatus(userId);
        }
        return 0;
    }

    public void stopMesh() {
        MeshLog.v("stop mesh is called");
        try {
            mTmCommunicator.stopMesh();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void restartMesh(int newRole) {
        MeshLog.v("sellerMode dm" + newRole);
        if (mTmCommunicator == null) {
            MeshLog.v("mTmCommunicator null");
            checkAndBindService();
        } else {
            try {

                mTmCommunicator.restartMesh(newRole);
            } catch (RemoteException e) {
                if (e.getCause() instanceof DeadObjectException) {
                    mTmCommunicator = null;
                    checkAndBindService();
                } else {
                    e.printStackTrace();
                }
            }
        }
    }

    public void destroyMeshService() {
        try {
            if (mTmCommunicator != null) {
                mTmCommunicator.destroyService();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void resetCommunicator() {
        mTmCommunicator = null;
    }

    private void onInterruptionAction(int hardwareState, List<String> permissions) {

        PermissionInterruptionEvent permissionInterruptionEvent = new PermissionInterruptionEvent();
        permissionInterruptionEvent.hardwareState = hardwareState;
        permissionInterruptionEvent.permissions = permissions;

        // Here we just removed the sending observer. This dialog will handle this dialog will show from viper end
        HandlerUtil.postForeground(() -> showPermissionEventAlert(permissionInterruptionEvent.hardwareState,
                permissionInterruptionEvent.permissions, MeshApp.getCurrentActivity()));
        // AppDataObserver.on().sendObserverData(permissionInterruptionEvent);
    }

    private void onTransportInit(String nodeId, String publicKey, boolean success, String msg) {

        MeshLog.v("onTransportInit dtm " + nodeId);
        TransportInit transportInit = new TransportInit();
        transportInit.nodeId = nodeId;
        transportInit.publicKey = publicKey;
        transportInit.success = success;
        transportInit.msg = msg;

        AppDataObserver.on().sendObserverData(transportInit);
    }

    private void onServiceUpdateNeeded(boolean isNeeded) {
        ServiceUpdate serviceUpdate = new ServiceUpdate();
        serviceUpdate.isNeeded = isNeeded;
        if (isNeeded) {
            showServiceUpdateAvailable(MeshApp.getCurrentActivity());
        }

        // We just removed to send service update available client app. Viper will handle it own
        //AppDataObserver.on().sendObserverData(serviceUpdate);
    }

    private void onWalletCreationEvent(boolean status, String nodeId, String message) {

        if (!TextUtils.isEmpty(nodeId)) {
            SharedPref.write(Constant.PreferenceKeys.ADDRESS, nodeId);
        }

        if (status) {
            WalletLoaded walletLoaded = new WalletLoaded();
            walletLoaded.walletAddress = nodeId;
            walletLoaded.success = status;

            AppDataObserver.on().sendObserverData(walletLoaded);

        } else {

            WalletCreationEvent walletCreationEvent = new WalletCreationEvent();
            walletCreationEvent.successStatus = status;
            walletCreationEvent.nodeId = nodeId;
            walletCreationEvent.statusMessage = message;

            AppDataObserver.on().sendObserverData(walletCreationEvent);

        }
    }

    public void openWalletActivity(byte[] pictureData) {
        try {
            if (mTmCommunicator != null) {
                mTmCommunicator.openWalletUI(appTokenName, pictureData);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void openDataPlan() {
        try {
            if (mTmCommunicator != null) {
                mTmCommunicator.openDataplanUI(appTokenName);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    public void writeLogIntoTxtFile(String text, boolean isAppend) {
        try {
            String sdCard = Constant.Directory.PARENT_DIRECTORY + Constant.Directory.MESH_LOG;
            File directory = new File(sdCard);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            if (Constant.CURRENT_LOG_FILE_NAME == null) {
                Constant.CURRENT_LOG_FILE_NAME = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date()) + ".txt";
            }
            File file = new File(directory, Constant.CURRENT_LOG_FILE_NAME);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fOut = new FileOutputStream(file, isAppend);

            OutputStreamWriter osw = new
                    OutputStreamWriter(fOut);

            osw.write("\n" + text);
            //  osw.append(text)
            osw.flush();
            osw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onFileProgress(String fileMessageId, int percentProgress) {
        FileProgressEvent event = new FileProgressEvent();
        event.setFileMessageId(fileMessageId);
        event.setPercentage(percentProgress);
        AppDataObserver.on().sendObserverData(event);
    }

    private void onFileTransferFinish(String fileMessageId, boolean isSuccess, String errorMessage) {
        FileTransferEvent event = new FileTransferEvent();
        event.setFileMessageId(fileMessageId);
        event.setSuccess(isSuccess);
        event.setErrorMessage(errorMessage);
        AppDataObserver.on().sendObserverData(event);
    }

    private void onFileReceiveStarted(FileData fileData) {
        FileReceivedEvent event = new FileReceivedEvent();
        event.setFileMessageId(fileData.getFileTransferId());
        event.setFilePath(fileData.getFilePath());
        event.setSourceAddress(fileData.getSourceAddress());
        event.setMetaData(fileData.getMsgMetaData());
        AppDataObserver.on().sendObserverData(event);
    }

    private void showProgressDialog() {
        HandlerUtil.postForeground(() -> {
            Context context = MeshApp.getCurrentActivity();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            LayoutInflater inflater = LayoutInflater.from(context);
            binding = DataBindingUtil.inflate(inflater, R.layout.dialog_service_app_install_progress, null, false);

            binding.textViewTitle.setText(String.format(context.getString(R.string.app_update_progress_title), context.getString(R.string.app_name)));

            builder.setView(binding.getRoot());
            dialog = builder.create();
            dialog.setCancelable(false);
            dialog.show();
        });

    }

    private void updateProgress(int progress) {
        HandlerUtil.postForeground(() -> {
            if (dialog != null && dialog.isShowing()) {
                binding.progressBar.setProgress(progress);
            }
        });

    }

    private void closeDialog(String message) {
        HandlerUtil.postForeground(() -> {
            Toaster.showShort(message);
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        });
    }

    /**
     * This method is responsible for showing install dialog
     */
    private void showAppInstaller() {
        File destinationFile = new File(appPath);
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String packageName = mContext.getPackageName() + ".provider";
            Uri apkUri = FileProvider.getUriForFile(mContext, packageName, destinationFile);
            intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            Log.d("InAppUpdateTest", "app uri: " + apkUri.getPath());
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Log.d("InAppUpdateTest", "app install process start");
        } else {
            Uri apkUri = Uri.fromFile(destinationFile);
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        MeshApp.getCurrentActivity().startActivity(intent);
    }

    /*
     *Permission interruption dialog
     *  */

    private void showPermissionEventAlert(int hardwareEvent, List<String> permissions, Activity activity) {

        if (activity == null) return;
        android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        @SuppressLint("InflateParams")
        View dialogView = inflater.inflate(R.layout.alert_hardware_permission, null);
        dialogBuilder.setView(dialogView);

        android.app.AlertDialog alertDialog = dialogBuilder.create();

        TextView title = dialogView.findViewById(R.id.interruption_title);
        TextView message = dialogView.findViewById(R.id.interruption_message);
        Button okay = dialogView.findViewById(R.id.okay_button);

        String finalTitle = "", finalMessage = "";

        boolean isPermission = false;

        String event = "";
        if (permissions == null || permissions.isEmpty()) {

            if (hardwareEvent == DataPlanConstants.INTERRUPTION_EVENT.USER_DISABLED_BT) {
                event = "Bluetooth";
            } else if (hardwareEvent == DataPlanConstants.INTERRUPTION_EVENT.USER_DISABLED_WIFI) {
                event = "Wifi";
            } else if (hardwareEvent == DataPlanConstants.INTERRUPTION_EVENT.LOCATION_PROVIDER_OFF) {
                event = "Location ";
            }

            if (!TextUtils.isEmpty(event)) {
                finalMessage = String.format(mContext.getString(R.string.hardware_interruption),
                        event, activity.getString(R.string.app_name));
                finalTitle = String.format(mContext.getString(R.string.interruption_title), "Hardware");
            }

        } else {

            for (String permission : permissions) {
                if (!TextUtils.isEmpty(permission)) {
                    if (permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        event = "Location";
                    } else if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        event = "Storage";
                    }
                }
            }

            if (!TextUtils.isEmpty(event)) {
                finalMessage = String.format(mContext.getString(R.string.permission_interruption),
                        event, activity.getString(R.string.app_name));
                finalTitle = String.format(mContext.getString(R.string.interruption_title), "Permission");
            }

            isPermission = true;

        }

        boolean finalIsPermission = isPermission;
        okay.setOnClickListener(v -> {
            if (isPermissionNeeded(DEVICE_NAME)) {
                showPermissionPopupForXiaomi(activity);
            } else if (finalIsPermission) {
                DataManager.on().allowMissingPermission(permissions);
                alertDialog.dismiss();
            } else {
                alertDialog.dismiss();
            }
        });

        if (!TextUtils.isEmpty(finalTitle) && !TextUtils.isEmpty(finalMessage)) {
            title.setText(finalTitle);
            message.setText(finalMessage);

            alertDialog.setCancelable(false);
            alertDialog.show();
        }
    }

    private boolean isPermissionNeeded(String deviceName) {
        String manufacturer = android.os.Build.MANUFACTURER;
        boolean isPermissionNeeded = false;
        try {

            if (deviceName.equalsIgnoreCase(manufacturer)) {
                isPermissionNeeded = !SharedPref.readBoolean(Constant.PreferenceKeys.IS_SETTINGS_PERMISSION_DONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return isPermissionNeeded;
    }

    private void showPermissionPopupForXiaomi(Activity activity) {
        if (activity == null) return;
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(activity);
        builder.setCancelable(false);
        builder.setTitle(Html.fromHtml("<b>" + "<font color='#FF7F27'>Please allow permissions</font>" + "</b>"));
        builder.setMessage(mContext.getString(R.string.permission_xiomi));
        builder.setPositiveButton(Html.fromHtml("<b>" + mContext.getString(R.string.ok) + "<b>"), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                SharedPref.write(Constant.PreferenceKeys.IS_SETTINGS_PERMISSION_DONE, true);
                activity.startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 100);
            }
        });
        builder.create();
        builder.show();
    }

    /*
     * Service update dialog
     * */

    public void showServiceUpdateAvailable(Activity activity) {
        if (activity == null) return;

        activity.runOnUiThread(() -> {
            android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(activity);
            builder.setCancelable(false);
            builder.setTitle(Html.fromHtml("<b>" + activity.getString(R.string.service_app_alert_title_text) + "</b>"));
            builder.setMessage(activity.getString(R.string.service_app_update_message));
            builder.setPositiveButton(Html.fromHtml("<b>" + activity.getString(R.string.button_postivive) + "<b>"), (dialog, arg1) -> {

                Intent intent = activity.getPackageManager().getLaunchIntentForPackage("com.w3engineers.meshservice");
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(intent);
                }
            });

            builder.setNegativeButton(Html.fromHtml("<b>" + activity.getString(R.string.button_later) + "<b>"), (dialog, arg1) -> {

            });

            builder.setCancelable(false);
            builder.create();
            builder.show();
        });
    }

    private void deleteBackUpApkFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            boolean isDeleted = file.delete();

            Log.i("BackupFileDelete", "Deleted " + isDeleted);
        }
    }

    private String appUpdateAppDialogInfoGenerator(String value) {
        return " " + value;
    }
}
