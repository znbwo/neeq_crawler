package com.neeq.crawler.tool;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Created by bj on 16/7/8.
 */
public class ElementHelper {
    /**
     * 防止TD缺失,单个字段不影响整条数据
     *
     * @param trs
     * @param parent
     * @param child
     * @return
     */
    public static String getSafeText(Elements trs, int parent, int child) {
        try {
            return trs.get(parent).child(child).text();
        } catch (Exception e) {
            return null;
        }
    }

    public static String getSafeText(Elements trs, int index) {
        try {
            return trs.get(index).text();
        } catch (Exception e) {
            return null;
        }
    }

    public static String getSafeText(List<HtmlElement> list, int index) {
        try {
            HtmlElement element = list.get(index);
            if (element != null) {
                return element.getTextContent();
            }
        } catch (Exception e) {

        }
        return "null";
    }

    public static String getSafeWebElementText(WebElement element, int index) {
        if (element != null) {
            return element.getText();
        }

        return "null";
    }
}
