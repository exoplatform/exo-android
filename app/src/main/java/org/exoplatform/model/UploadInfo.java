package org.exoplatform.model;

import org.exoplatform.App;
import org.exoplatform.ServerManagerImpl;
import org.exoplatform.tool.PlatformUtils;


/**
 * Created by paristote on 3/7/16.
 */
public class UploadInfo {

    public String uploadId;

    public DocumentInfo fileToUpload;

    public String domain;

    public String repository;

    public String workspace;

    public String drive;

    public String folder;

    public String jcrUrl;

    public UploadInfo() {
    }

    public UploadInfo(UploadInfo another) {
        this.uploadId = Long.toHexString(System.currentTimeMillis());
        this.repository = another.repository;
        this.workspace = another.workspace;
        this.drive = another.drive;
        this.folder = another.folder;
        this.jcrUrl = another.jcrUrl;
        this.domain = another.domain;
    }

    public void init(SocialActivity postInfo) {

        uploadId = Long.toHexString(System.currentTimeMillis());
        PlatformInfo platformInfo = PlatformUtils.getPlatformInfo();
        repository = platformInfo.currentRepoName;
        workspace = platformInfo.defaultWorkSpaceName;


        if (postInfo.isPublic()) {
            // File will be uploaded in the Public folder of the user's drive
            // e.g. /Users/u___/us___/use___/user/Public/Mobile
            drive = App.Platform.DOCUMENT_PERSONAL_DRIVE_NAME;
            folder = "Public/Mobile";
            StringBuilder jcrUrlBuilder = new StringBuilder();
            jcrUrl = PlatformUtils.getUserHomeJcrFolderPath();
        } else {
            // File will be uploaded in the Documents folder of the space's drive
            // e.g. /Groups/spaces/the_space/Documents/Mobile
            drive = ".spaces." + postInfo.destinationSpace.getOriginalName();
            folder = "Mobile";
            StringBuffer url = new StringBuffer(postInfo.ownerAccount.getUrl().toString())
                    .append(App.Platform.DOCUMENT_JCR_PATH)
                    .append("/")
                    .append(repository)
                    .append("/")
                    .append(workspace)
                    .append("/Groups/spaces/")
                    .append(postInfo.destinationSpace.getOriginalName())
                    .append("/Documents");
            jcrUrl = url.toString();
        }
    }

    public String getUploadedUrl() {
        return new StringBuffer(jcrUrl).append("/").append(folder).append("/").append(fileToUpload.documentName).toString();
    }
}
