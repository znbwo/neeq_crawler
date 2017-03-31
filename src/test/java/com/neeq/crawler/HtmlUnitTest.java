package com.neeq.crawler;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Assert;

import java.io.IOException;

/**
 * Created by bj on 16/7/5.
 */
public class HtmlUnitTest {
    public static void main(String[] args) throws IOException {
        /**HtmlUnit请求web页面*/
        WebClient wc = new WebClient();
        wc.getOptions().setJavaScriptEnabled(true); //启用JS解释器，默认为true
        wc.getOptions().setCssEnabled(false); //禁用css支持
        wc.getOptions().setThrowExceptionOnScriptError(false); //js运行错误时，是否抛出异常
        wc.getOptions().setTimeout(10000); //设置连接超时时间 ，这里是10S。如果为0，则无限期等待
//        wc.getOptions().se
        HtmlPage page = wc.getPage("http://cq.qq.com/baoliao/detail.htm?294064");
        String pageXml = page.asXml(); //以xml的形式获取响应文本

        /**jsoup解析文档*/
        Document doc = org.jsoup.Jsoup.parse(pageXml, "http://cq.qq.com");

//        Element pv = doc.select(".view-num").first();
        Elements select = doc.select(".sp1 .view-num");
        Element pv =  select.first();
        System.out.println(pv.text());
        Assert.assertTrue(pv.text().contains("浏览"));

        System.out.println("Thank God!");
    }
}
