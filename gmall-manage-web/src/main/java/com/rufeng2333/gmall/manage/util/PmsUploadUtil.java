package com.rufeng2333.gmall.manage.util;

import org.csource.fastdfs.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class PmsUploadUtil {

    public static String uploadImage(MultipartFile multipartFile){

        String imgUrl = "http://192.168.124.129";

        try {
            ClientGlobal.init("tracker.conf");
        } catch (Exception e) {
            e.printStackTrace();
        }

        TrackerClient trackerClient = new TrackerClient();

        TrackerServer trackerServer = null;
        try {
            trackerServer = trackerClient.getConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }

        StorageClient storageClient = new StorageClient(trackerServer,null);

        try {
            byte[] bytes = multipartFile.getBytes();
            String originalFilename = multipartFile.getOriginalFilename();

            int i = originalFilename.lastIndexOf(".");

            String extName = originalFilename.substring(i + 1);


            String[] uploadInfos = storageClient.upload_file(bytes, extName, null);

            for(String uploadInfo:uploadInfos){
                imgUrl += "/"+uploadInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return imgUrl;
    }
}
