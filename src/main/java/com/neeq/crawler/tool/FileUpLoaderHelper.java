package com.neeq.crawler.tool;

import com.neeq.crawler.dependence.Config;
import com.neeq.crawler.io.FileUploader;
import com.neeq.crawler.io.impl.LocalFileUploader;
import com.neeq.crawler.io.impl.OSSFileUploader;

/**
 * Created by bj on 16/7/19.
 */
public class FileUpLoaderHelper {
    public static FileUploader fileUploader;

    static {
        String fileSystem = Config.get("file.system", "local");
        if (fileSystem.equals("local")) {
            fileUploader = new LocalFileUploader();
        } else if (fileSystem.equals("oss")) {
            fileUploader = new OSSFileUploader();
        } else {
            throw new RuntimeException("错误的文件系统配置:" + fileSystem);
        }
    }

    public FileUploader getInstance() {
        return fileUploader;
    }
}
