package com.neeq.crawler;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

/**
 * Created by kidbei on 16/5/24.
 */
public class Jsoup {


    @Test
    public void test() {
        try {
            Document doc = org.jsoup.Jsoup.connect("http://www.qiushibaike.com/pic/").userAgent("Mozilla/5.0").get();

            Elements imgs = doc.getElementsByTag("img");
            for (Element img : imgs) {
                img.attr("src","http://www.baidu.com");
            }

            System.out.println(doc.html());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
