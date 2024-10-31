package com.w3engineers.mesh.util.lib.mesh;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.util.Log;

import com.w3engineers.mesh.application.data.local.db.SharedPref;
import com.w3engineers.mesh.application.ui.util.FileStoreUtil;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.NetworkMonitor;
import com.w3engineers.models.BroadcastData;
import com.w3engineers.models.FileData;
import com.w3engineers.models.MessageData;
import com.w3engineers.models.UserInfo;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

public class ViperClient {

    private static Context mContext;
    private static String usersName;
    private static int avatar;
    private static String appToken;

    private static ViperClient mViperClient;

    private ViperClient() {
        //Prevent form the reflection api.
        if (mViperClient != null) {
            throw new RuntimeException("Use on() method to get the single instance of this class.");
        }
    }

    private ViperClient(Context context, String appToken, String userName, int avatar) {
        this.mContext = context;
        this.appToken = appToken;
        this.usersName = userName;
        this.avatar = avatar;
    }

    public static ViperClient on(Context context, String userName) {
        if (context == null) {
            throw new NullPointerException("Context is null");
        }
        return on(context, context.getPackageName(), userName, avatar);
    }

    public static ViperClient on(Context context, String userName, int avatar) {
        if (context == null) {
            throw new NullPointerException("Context is null");
        }
        return on(context, context.getPackageName(), userName, avatar);
    }

    public static ViperClient on(Context context, String appToken, String userName) {
        if (context == null) {
            throw new NullPointerException("Context is null");
        }
        return on(context, appToken, userName, avatar);
    }

    public static ViperClient on(Context context, String appToken, String userName, int avatar) {
        if (context == null) {
            throw new NullPointerException("Context is null");
        }

        if (mViperClient == null) {
            synchronized (ViperClient.class) {
                if (mViperClient == null)
                    mViperClient = new ViperClient(context, appToken, userName, avatar);
            }
        }
        setConfig(context);
        return mViperClient;
    }

    private static void setConfig(Context context) {

//        startNetworkMonitor();

        String authName = ViperCredentials.getInstance().getAuthUserName();
        String authPass = ViperCredentials.getInstance().getAuthPassword();
        String downloadLink = ViperCredentials.getInstance().getFileRepoLink();

        int appVersion = 0;
        String appVersionName = "";
        String appSize = "0 MB";
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            appVersion = pInfo.versionCode;
            appVersionName = pInfo.versionName;

            long size = new File(context.getPackageManager().getApplicationInfo(
                    pInfo.packageName, 0).publicSourceDir).length();

            if (size > 0) {
                appSize = getFileSize(size);
            }

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        SharedPref.write(Constant.PreferenceKeys.AUTH_USER_NAME, authName);
        SharedPref.write(Constant.PreferenceKeys.AUTH_PASSWORD, authPass);
        SharedPref.write(Constant.PreferenceKeys.APP_DOWNLOAD_LINK, downloadLink);

        //Todo Tariqul remove below shared pref data if new approach work

        SharedPref.write(Constant.PreferenceKeys.APP_VERSION, appVersion);
        SharedPref.write(Constant.PreferenceKeys.APP_VERSION_NAME, appVersionName);
        SharedPref.write(Constant.PreferenceKeys.APP_SIZE, appSize);

        String userAddress = SharedPref.read(Constant.PreferenceKeys.ADDRESS);

        UserInfo userInfo = new UserInfo();

        userInfo.setAvatar(avatar);
        userInfo.setAddress(userAddress);
        userInfo.setUserName(usersName);
        userInfo.setAppToken(appToken);

        // For App update app info
        userInfo.setVersionCode(appVersion);
        userInfo.setVersionName(appVersionName);
        userInfo.setAppSize(appSize);

        DataManager.on().doBindService(mContext, userInfo, appToken);

        DataManager.on().startMeshService();
    }

    public static void startNetworkMonitor() {
        NetworkMonitor.start(mContext, Constant.SOCKET_URL, (isOnline, network, isWiFi) -> {
            Log.v("***********************", "***********************");
            if (network != null) {
                Log.v("onNetworkAvailable", isOnline + " " + network.toString() + (isWiFi ? " wifi" : " cellular"));
            } else {
                Log.v("onNetworkAvailable", isOnline + " ");
            }
            Log.v("***********************", "***********************");
        });
    }

    // APIs for supporting client app

    public void sendMessage(String senderId, String receiverId, String messageId, byte[] data, boolean isNotificationNeeded) throws RemoteException {
        MessageData messageData = new MessageData().setSenderID(senderId).setReceiverID(receiverId)
                .setMessageID(messageId).setMsgData(data).setNotificationNeeded(isNotificationNeeded)
                .setAppToken(mContext.getPackageName());
        DataManager.on().sendData(messageData);
    }

    public void sendBroadcastData(BroadcastData broadcastData) throws RemoteException {
        DataManager.on().sendBroadcastData(broadcastData);
    }

    public String sendFileMessage(FileData fileData) throws RemoteException {
        return DataManager.on().sendFileMessage(fileData);
    }

    public void sendFileResumeRequest(String contentId, byte[] metaData) throws RemoteException {
        FileData fileData = new FileData().setContentId(contentId).setMsgMetaData(metaData)
                            .setAppToken(mContext.getPackageName());

        DataManager.on().sendFileResumeRequest(fileData);
    }

    public void removeSendContent(String contentId) throws RemoteException {
        FileData fileData = new FileData().setContentId(contentId).setAppToken(mContext.getPackageName());
        DataManager.on().removeSendContent(fileData);
    }

    public void openWalletCreationUI() {
        DataManager.on().openWalletCreateUI();
    }

    public void checkConnectionStatus(String nodeID) {
        try {
            DataManager.on().checkConnectionStatus(nodeID);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public int checkUserConnectivityStatus(String userId) {
        try {
            return DataManager.on().checkUserConnectivityStatus(userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getLinkTypeById(String nodeID) throws RemoteException {
        return DataManager.on().getLinkTypeById(nodeID);
    }

    public void updateMyInfo(String usersName) {
        try {
            UserInfo userInfo = new UserInfo();

            String myAddress = SharedPref.read(Constant.PreferenceKeys.ADDRESS);

            userInfo.setAddress(myAddress);
            userInfo.setAvatar(avatar);
            userInfo.setUserName(usersName);
            userInfo.setAppToken(DataManager.on().getAppTokenName());

            DataManager.on().saveUserInfo(userInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateMyInfo(String usersName, int avatar) {
        try {
            UserInfo userInfo = new UserInfo();

            String myAddress = SharedPref.read(Constant.PreferenceKeys.ADDRESS);

            userInfo.setAddress(myAddress);
            userInfo.setAvatar(avatar);
            userInfo.setUserName(usersName);
            userInfo.setAppToken(DataManager.on().getAppTokenName());

            DataManager.on().saveUserInfo(userInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateUserInfo(String userAddress, String usersName, int avatar) {
        try {
            UserInfo userInfo = new UserInfo();

            userInfo.setAddress(userAddress);
            userInfo.setAvatar(avatar);
            userInfo.setUserName(usersName);
            userInfo.setAppToken(DataManager.on().getAppTokenName());

            DataManager.on().saveOtherUserInfo(userInfo);

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void updateUserInfo(String userAddress, String usersName) {
        try {
            UserInfo userInfo = new UserInfo();

            userInfo.setAddress(userAddress);
            userInfo.setUserName(usersName);
            userInfo.setAppToken(DataManager.on().getAppTokenName());

            DataManager.on().saveOtherUserInfo(userInfo);

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void stopMesh() {
        DataManager.on().stopMesh();
    }

    public void restartMesh() {
        int currentRole = DataManager.on().getMeshUserRole();
        DataManager.on().restartMesh(currentRole);
    }

    public void destroyMeshService() {
        DataManager.on().stopService();
        DataManager.on().destroyMeshService();
        DataManager.on().resetCommunicator();
    }

    public void resetViperInstance() {
        mViperClient = null;
    }

    public List<String> getInternetSellers() throws RemoteException {
        return DataManager.on().getInternetSellers();
    }

    public String requestPointGuideline() {
        return FileStoreUtil.getGuideline(mContext);
    }

    public void sendPointGuidelineForUpdate(String guideLine) {
        FileStoreUtil.writeTokenGuideline(mContext, guideLine);
    }

    public void allowMissingPermission(List<String> missingPermission) {
        DataManager.on().allowMissingPermission(missingPermission);
    }

    public void openWalletActivity(byte[] avatarData) {
        DataManager.on().openWalletActivity(avatarData);
    }

    public void openDataPlanActivity() {
        DataManager.on().openDataPlan();
    }

    private static String getFileSize(long size) {
        if (size <= 0)
            return "0";

        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
