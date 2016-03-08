package org.exoplatform.tool;

import org.exoplatform.App;
import org.exoplatform.model.PlatformInfo;
import org.exoplatform.model.Server;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by paristote on 3/10/16.
 */
public class PlatformUtils {

    private static PlatformInfo platformInfo;

    private static String platformDomain;

    public static void init(String domain, PlatformInfo info) {
        if (domain == null || info == null)
            throw new IllegalArgumentException("Domain and Info parameters must not be null.");

        platformInfo = info;

        try {
            URL urlDomain = new URL(domain);
            platformDomain = Server.format(urlDomain);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Incorrect domain parameter: "+domain, e);
        }
    }

    public static void reset() {
        platformInfo = null;
        platformDomain = null;
    }

    public static String getUserHomeJcrFolderPath() {
        if (platformInfo == null || platformDomain == null)
            throw new NullPointerException("Incorrect Platform domain or info attributes. Use PlatformUtils.init().");

        StringBuilder b = new StringBuilder(platformDomain)
                .append(App.Platform.DOCUMENT_JCR_PATH)
                .append("/")
                .append(platformInfo.currentRepoName)
                .append("/")
                .append(platformInfo.defaultWorkSpaceName)
                .append(platformInfo.userHomeNodePath);

        return b.toString();

    }

    public static PlatformInfo getPlatformInfo() {
        return platformInfo;
    }

    public static String getPlatformDomain() {
        return platformDomain;
    }
}
