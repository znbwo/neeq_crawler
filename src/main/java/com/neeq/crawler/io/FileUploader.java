package com.neeq.crawler.io;

import java.io.File;
import java.io.InputStream;

/**
 * Created by kidbei on 16/5/23.
 */
public interface FileUploader {


    String upload(File file);


    String upload(InputStream is,String fileName);


    String upload(InputStream is,String fileName,int errorIndex);
}
