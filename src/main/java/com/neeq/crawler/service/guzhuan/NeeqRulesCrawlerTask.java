package com.neeq.crawler.service.guzhuan;

import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.io.FileUploader;
import com.neeq.crawler.push.PushQueue;
import com.neeq.crawler.task.TaskOptions;
import com.neeq.crawler.tool.HttpManager;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by bj on 16/7/19.
 */
public class NeeqRulesCrawlerTask extends BasicNeeqLawsRulesCrawlerTask {
    private static String url = "http://www.neeq.com.cn";
    static String queryurl = url + "/info/list.do?callback=mm";

    public NeeqRulesCrawlerTask(PushQueue pushQueue, CoopRedis redis, FileUploader fileUploader) {
        super(pushQueue, redis, fileUploader);
    }


    @Override
    public String taskId() {
        return "股转系统部门规章";
    }

    public static void main(String[] args) throws IOException {

    }

    @Override
    public HttpPost getHttpPost(int pageIndex) throws UnsupportedEncodingException {
        HttpPost post = new HttpPost(queryurl);
        List<NameValuePair> params = new ArrayList<NameValuePair>() {
            {
                this.add(new BasicNameValuePair("page", pageIndex + ""));
                this.add(new BasicNameValuePair("pageSize", "10"));//pageSize:"10"
                this.add(new BasicNameValuePair("keywords", ""));//keywords:""
                this.add(new BasicNameValuePair("nodeId", "106"));//nodeId:"105"
            }
        };
        post.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
        HttpManager.config(post);
        return post;
    }


    public TaskOptions options() {
        return new TaskOptions()
//                .setPeriod(1000 * 60 * 60 * 24)
                .setPeriod(1000 * 1)
                .setTimeUnit(TimeUnit.MILLISECONDS);
    }
}
