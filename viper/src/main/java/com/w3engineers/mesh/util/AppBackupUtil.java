package com.w3engineers.mesh.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.w3engineers.mesh.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class AppBackupUtil {

    /**
     * get application info and preparing a apk and save it in local storage
     *
     * @param context - Need an application context for getting package name
     * @return - saved apk path
     */
    @Nullable
    public static String backupApkAndGetPath(@NonNull Context context) {


        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List pkgAppsList = context.getPackageManager().queryIntentActivities(mainIntent, 0);
        String backupFolder = ".backup";

        for (Object object : pkgAppsList) {

            ResolveInfo resolveInfo = (ResolveInfo) object;
            File appFile = new File(resolveInfo.activityInfo.applicationInfo.publicSourceDir);

            try {


                String file_name = resolveInfo.loadLabel(context.getPackageManager()).toString();


                if (file_name.equalsIgnoreCase(context.getString(R.string.app_name)) &&
                        appFile.toString().contains(context.getPackageName())) {

                    // we are concating file name with time stamp for generating unique path.
                    file_name += System.currentTimeMillis();


                    File file = new File(Environment.getExternalStorageDirectory().toString() + "/" +
                            context.getString(R.string.app_name));
                    file.mkdirs();
                    // Preparing a backup apk folder and it is hidden
                    File backUpFolder = new File(file.getAbsolutePath() + "/" + backupFolder);

                  /*  if (backUpFolder.exists()) {
                        boolean isDeleted = backUpFolder.delete();
                        MeshLog.i("Is backup folder deleted: " + isDeleted);
                    }*/

                    backUpFolder.mkdirs();
                    backUpFolder = new File(backUpFolder.getPath() + "/" + file_name + ".apk");
                    backUpFolder.createNewFile();

                    InputStream in = new FileInputStream(appFile);
                    OutputStream out = new FileOutputStream(backUpFolder);

                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();

                    return backUpFolder.getAbsolutePath();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }
}
