package com.neeq.crawler.tool;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Created by bj on 16/7/12.
 */
public class StringHelper {

    public static String formatElements(Elements elements) {
        StringBuffer industry = new StringBuffer();       //受投资方所属行业
        for (Element element : elements) {
            if (!industry.toString().isEmpty()) {
                industry.append(",");
            }
            industry.append(element.text());
        }
        return industry.toString();
    }
}
