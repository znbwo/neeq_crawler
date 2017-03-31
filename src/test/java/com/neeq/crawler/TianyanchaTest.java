package com.neeq.crawler;

import com.alibaba.fastjson.JSONObject;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.neeq.crawler.tool.HttpManager;
import com.neeq.crawler.tool.WebClientHelper;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

/**
 * Created by bj on 16/7/4.
 */
public class TianyanchaTest {
    public static void main(String[] args) throws IOException {
        String cpName = "中科软科技股份有限公司";
        String url = "http://www.tianyancha.com/suggest/" + cpName + ".json";
        HttpGet get = HttpManager.getGet(url);
        get.addHeader("Cookie", "TYCID=73968a213edc4290be58f7ca9a26c320; tnet=118.244.254.10; Hm_lvt_e92c8d65d92d534b0fc290df538b4758=1467603564,1467603622,1467603628,1467603653; Hm_lpvt_e92c8d65d92d534b0fc290df538b4758=1467603653; _pk_id.1.e431=233b12fe319bd78c.1467603281.1.1467604419.1467603281.; _pk_ref.1.e431=%5B%22%22%2C%22%22%2C1467603281%2C%22https%3A%2F%2Fwww.baidu.com%2Flink%3Furl%3DRC_ujQb0KUdyVk5Xo0_ElEUFE4qrkgq7FDA9QKQRqO-IWhg3KwA2MpU8OEci98OC%26wd%3D%26eqid%3Daba62e0800073f82000000035779d94f%22%5D; _pk_ses.1.e431=*; token=cfa599a689024d0694fc81997ca2fbec; _utm=a4a70c2dc6864d9ea5e6a65ff9bf9a2e");
        get.addHeader("Referer", "http://www.tianyancha.com/search/%E4%B8%AD%E7%A7%91%E8%BD%AF%E7%A7%91%E6%8A%80%E8%82%A1%E4%BB%BD%E6%9C%89%E9%99%90%E5%85%AC%E5%8F%B8");//Referer:"http://www.tianyancha.com/search/%E4%B8%AD%E7%A7%91%E8%BD%AF%E7%A7%91%E6%8A%80%E8%82%A1%E4%BB%BD%E6%9C%89%E9%99%90%E5%85%AC%E5%8F%B8"
        get.addHeader("Tyc-From", "normal");//Tyc-From:"normal"
        get.addHeader("CheckError", "check");//CheckError:"check"
        get.addHeader("Host", "www.tianyancha.com");//Host:"www.tianyancha.com"
        CloseableHttpClient client = HttpManager.getClient();
        CloseableHttpResponse response = client.execute(get);
        String string = IOUtils.toString(response.getEntity().getContent());
        JSONObject suggest = JSONObject.parseObject(string).getJSONArray("data").getJSONObject(0);
        if (suggest.getString("name").equalsIgnoreCase(cpName)) {
            String companyUrl = "http://www.tianyancha.com/company/";
            String id = suggest.getString("id");
            //http://www.tianyancha.com/company/6726986
//            HttpGet cpGet = HttpManager.getGet(companyUrl + id);
            WebClient wc = WebClientHelper.getWebClient(false);
//            wc.getOptions().setCssEnabled(false); //禁用css支持
//            wc.getOptions().setJavaScriptEnabled(true); //启用JS解释器，默认为true
//            wc.getOptions().setThrowExceptionOnScriptError(false); //js运行错误时，是否抛出异常
//            wc.getOptions().setTimeout(60000); //设置连接超时时间 ，这里是10S。如果为0，则无限期等待
//            wc.setJavaScriptTimeout(3600 * 1000);
//            wc.getOptions().setRedirectEnabled(true);
//            wc.getOptions().setThrowExceptionOnScriptError(true);
//            wc.getOptions().setThrowExceptionOnFailingStatusCode(true);
//            wc.getOptions().setTimeout(3600*1000);
//            HtmlPage page = wc.getPage(companyUrl + id);
            HtmlPage page = wc.getPage("http://www.tianyancha.com/company/6726986");
            wc.waitForBackgroundJavaScript(1000 * 8L);
//            Page jsonPage = wc.getPage("http://www.tianyancha.com/search/中科软科技股份有限公司.json");
            String pageXml = page.asXml(); // 以xml的形式获取响应文本

            /**jsoup解析文档*/
            Document doc = org.jsoup.Jsoup.parse(pageXml, "http://www.tianyancha.com");
            Elements elements = doc.select("div[ng-if=company.investorList.length>0]:not(#nav-main-investment)");
            // <span ng-if="company.updateTime" style="font-size: 12px; color: #aaa; border-top: 1px dashed #ccc;" class="ng-binding ng-scope"> 上次/2016.05.25 </span>
            String text1 = doc.select("span[ng-if=company.updateTime]").text();
            String desc = doc.select("meta[name=description]").attr("content");
            getBaseInfo(doc);
        }
    }

    static void getBaseInfo(Document doc) {
        Elements company_content = doc.select("div.row.b-c-white.company-content");
        JSONObject jsonObject = new JSONObject();
        Elements table = company_content.select("table");
        jsonObject.put("法定代表人", table.first().select(".td-legalPersonName-value a").text());
        jsonObject.put("注册资本", table.first().select(".td-regCapital-value").text());
        jsonObject.put("状态", table.first().select(".td-regStatus-value").text());
        jsonObject.put("注册时间", table.first().select(".td-regTime-value").text());
        Elements spans = table.last().select("td.basic-td div.c8 span.ng-binding");
        jsonObject.put("行业", spans.first().text());
        jsonObject.put("工商注册号", spans.get(1).text());
        jsonObject.put("企业类型", spans.get(2).text());
        jsonObject.put("组织机构代码", spans.get(3).text());
        jsonObject.put("营业期限", spans.get(4).text());
        jsonObject.put("登记机关", spans.get(5).text());
        jsonObject.put("核准日期", spans.get(6).text());
        jsonObject.put("统一信用代码", spans.get(7).text());
        jsonObject.put("注册地址", spans.get(8).text());
        jsonObject.put("经营范围", spans.last().text());
        System.out.println(jsonObject.toJSONString());
    }
}
