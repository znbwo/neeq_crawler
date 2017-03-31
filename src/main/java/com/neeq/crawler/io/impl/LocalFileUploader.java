package com.neeq.crawler.io.impl;

import com.neeq.crawler.dependence.Config;
import com.neeq.crawler.io.FileUploader;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Created by kidbei on 16/5/31.
 */
public class LocalFileUploader implements FileUploader {

    private final Logger log = LoggerFactory.getLogger(LocalFileUploader.class);

    private String PATH = Config.get("local.file.path","/data/upfiles");



    @Override
    public String upload(File file) {

        InputStream is = null;
        try{
            String path = PATH + File.separator + file.getName();

            is = new FileInputStream(file);

            File newFile = new File(path);
            FileOutputStream fos = new FileOutputStream(newFile);
            IOUtils.write(IOUtils.toByteArray(is),fos);

            return path;
        } catch(Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }

    }

    @Override
    public String upload(InputStream is, String fileName) {

        try{
            String path = PATH + File.separator + fileName;

            File file = new File(path);
            if (!file.getParentFile().exists()) {
                file.mkdirs();
            }
            FileOutputStream fos = new FileOutputStream(file);
            IOUtils.write(IOUtils.toByteArray(is),fos);

            return path;
        } catch(Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
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
