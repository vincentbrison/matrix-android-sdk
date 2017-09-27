package org.matrix.androidsdk;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import org.matrix.androidsdk.util.Log;

public final class UserAgentProvider {

    private volatile static String userAgent = null;

    private UserAgentProvider() {}

    public static String getUserAgent() {
        if (userAgent == null) {
            throw new IllegalStateException();
        }
        return userAgent;
    }

    public static void init(Context context) {
        userAgent = makeUserAgent(context);
    }

    private static String makeUserAgent(Context appContext) {
        String userAgent;
        String appName = "";
        String appVersion = "";

        if (null != appContext) {
            try {
                PackageManager pm = appContext.getPackageManager();
                ApplicationInfo appInfo = pm.getApplicationInfo(appContext
                                                                    .getApplicationContext()
                                                                    .getPackageName(), 0);
                appName = pm.getApplicationLabel(appInfo).toString();

                PackageInfo pkgInfo = pm.getPackageInfo(appContext
                                                            .getApplicationContext()
                                                            .getPackageName(), 0);
                appVersion = pkgInfo.versionName;
            } catch (Exception e) {
                Log.e(
                    OkHttpClientProvider.class.getSimpleName(),
                    "## initUserAgent() : failed " + e.getMessage()
                );
            }
        }

        userAgent = System.getProperty("http.agent");

        // cannot retrieve the application version
        if (TextUtils.isEmpty(appName) || TextUtils.isEmpty(appVersion)) {
            if (userAgent == null) {
                userAgent = "Java" + System.getProperty("java.version");
            }
            return userAgent;
        }

        // if there is no user agent or cannot parse it
        if ((null == userAgent)
            || (userAgent.lastIndexOf(")") == -1)
            || (userAgent.indexOf("(") == -1)) {
            userAgent = appName + "/" + appVersion + " (MatrixAndroidSDK " +
                BuildConfig.VERSION_NAME + ")";
        } else {
            // update
            userAgent = appName + "/" + appVersion + " " +
                userAgent.substring(userAgent.indexOf("("), userAgent.lastIndexOf(")") - 1) +
                "; MatrixAndroidSDK " + BuildConfig.VERSION_NAME + ")";
        }
        return userAgent;
    }
}
