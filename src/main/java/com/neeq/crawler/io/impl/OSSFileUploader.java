package com.neeq.crawler.io.impl;

import com.aliyun.oss.OSSClient;
import com.neeq.crawler.dependence.Config;
import com.neeq.crawler.io.FileUploader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;

/**
 *
 * 上传到阿里云
 * Created by kidbei on 16/5/23.
 */
public class OSSFileUploader implements FileUploader{

    private final Logger log = LoggerFactory.getLogger(OSSFileUploader.class);


    private String accessKey = Config.get("oss.accesskey");
    private String secretKey = Config.get("oss.secretkey");
    private String bucketName = Config.get("oss.bucket");
    private String endpoint = Config.get("oss.endpoint");

    private OSSClient ossClient;


    public OSSFileUploader() {
        ossClient = new OSSClient(endpoint,accessKey,secretKey);
    }


    public OSSFileUploader(String bucketName) {
        this.bucketName = bucketName;
    }



    @Override
    public String upload(File file) {
        ossClient.putObject(bucketName,file.getName(),file);
        return "http://" + bucketName + "." + endpoint + "/" + file.getName();
    }


    @Override
    public String upload(InputStream is,String fileName) {
        ossClient.putObject(bucketName,fileName,is);
        return "http://" + bucketName + "." + endpoint + "/" + fileName;
    }



    @Override
    public String upload(InputStream is, String fileName, int errorIndex) {
        try{
            return upload(is,fileName);
        } catch(Exception e) {
            int count = errorIndex + 1;

            log.warn("upload file {} error,count:{}",fileName,count);

            if (count < 3) {
                return upload(is,fileName,count);
            } else {
                return null;
            }
        }
    }



}
