package com.neeq.crawler.consumer;

import com.alibaba.fastjson.JSONArray;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HTMLParserListener;
import com.neeq.crawler.MyBrowserDriver;
import com.neeq.crawler.consumer.tianyangcha.CompanyTianyanchaConsumer;
import com.neeq.crawler.consumer.tianyangcha.Tianyangcha;
import com.neeq.crawler.tool.ElementHelper;
import com.neeq.crawler.tool.RedisHelper;
import com.neeq.crawler.tool.WebClientHelper;
import org.jsoup.Jsoup;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import redis.clients.jedis.Jedis;

import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Created by bj on 16/7/13.
 */
public class Test {
    private static Tianyangcha tianyangcha;
    private static RemoteWebDriver driver;

    @org.junit.Test
    public void test() {
        Tianyangcha tianyangcha = new Tianyangcha();
        MyBrowserDriver.repairByBrower(tianyangcha.driver);
        tianyangcha.driver.get("http://www.tianyancha.com/company/5525330");
        tianyangcha.getBaseInfo(Jsoup.parse(tianyangcha.driver.getPageSource()));
        System.out.println(new Random(6).nextInt(6));
        MyBrowserDriver.repairByBrower(MyBrowserDriver.getSafariDriver());
    }

    public void testcloseReport() throws InterruptedException {
        RemoteWebDriver driver = MyBrowserDriver.getSafariDriver();
        driver.get("http://www.tianyancha.com/company/1286950");
        List<WebElement> divs = driver.findElementsByXPath("//div[@class='report_year_box']");
        JSONArray array = new JSONArray();
        for (WebElement div : divs) {
            div.click();
            Thread.sleep(1500);
            String report = ElementHelper.getSafeWebElementText(driver.findElementByXPath("//div[@class='modal-content']"), 0);
            array.add(report);
            WebElement close = driver.findElementByXPath("//div[@ng-click='cancel()']");
            if (close != null) {
                close.click();
                Thread.sleep(1500);
            }
        }
    }

    @org.junit.Test
    public void testf() throws InterruptedException {
        RemoteWebDriver driver = MyBrowserDriver.getSafariDriver();
        MyBrowserDriver.repairByBrower(driver);
//        Page page = CompanyTianyanchaConsumer.getHtmlPage("http://www.tianyancha.com/company/6726986", 10, 0);
        driver.get("http://www.tianyancha.com/company/150041670");
        Thread.sleep(1000);
        String pagesNum = driver.findElementByXPath("//div[@ng-if='company.lawSuitTotal>0']//div[@class='total ng-binding']").getText();
        int s = Integer.valueOf(pagesNum.replaceAll("\\D+", ""));
//        for (WebElement link : links) {
//            String href = link.getAttribute("href");
//            String lawsuitUrl = "http://www.tianyancha.com/lawsuit/detail" + href.substring(href.lastIndexOf("/")) + ".json";
//            HttpGet get = new HttpGet(lawsuitUrl);
//            Tianyangcha.configGet(get);
//            String jsonStr = HttpHelper.getForStringPage(Tianyangcha.client, get, 0, Tianyangcha.encoding);
//            JSONObject json = JSONObject.parseObject(jsonStr);
//            Tianyangcha.putLawsuitData(json);
//        }
//////div[@ng-if='company.lawSuitTotal>0'] li[@class='pagination-page ng-scope']//a[position()>1]
//        List<WebElement> pages = driver.findElementsByXPath("//div[@ng-if='company.lawSuitTotal>0']//li[@ng-repeat='page in pages track by $index']//a");
//        for (WebElement webElement : pages) {
//            try {
//                webElement.click();
//                Thread.sleep(2000);
//                List<WebElement> links = driver.findElementsByXPath("//a[@event-name='company-detail-lawsuit']");
//                for (WebElement link : links) {
//                    String href = link.getAttribute("href");
//                    String lawsuitUrl = "http://www.tianyancha.com/lawsuit/detail" + href.substring(href.lastIndexOf("/")) + ".json";
//                    HttpGet get = new HttpGet(lawsuitUrl);
//                    Tianyangcha.configGet(get);
//                    String jsonStr = HttpHelper.getForStringPage(Tianyangcha.client, get, 0, 0, Tianyangcha.encoding);
//                    JSONObject json = JSONObject.parseObject(jsonStr);
//                    Tianyangcha.putLawsuitData(json);
//                }
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
    }

    @org.junit.Test
    //涉及诉讼
    public void testf2() throws InterruptedException {
//        RemoteWebDriver driver = Tianyangcha.driver;
//        MyBrowserDriver.repairByBrower(driver);
//        driver.get("http://www.tianyancha.com/company/150041670");
//        Thread.sleep(1000);
//        String pagesNum = driver.findElementByXPath("//div[@ng-if='company.lawSuitTotal>0']//div[@class='total ng-binding']").getText();
//        int s = Integer.valueOf(pagesNum.replaceAll("\\D+", ""));
//        WebElement nextClick = driver.findElementsByXPath("//div[@ng-if='company.lawSuitTotal>0']//li[@ng-if='::directionLinks']//a").get(1);
//        for (int i = 0; i < s; i++) {
//            if (i != 0) {
//                nextClick.click();
//                Thread.sleep(2000);
//            }
//            List<WebElement> links = driver.findElementsByXPath("//a[@event-name='company-detail-lawsuit']");
//            for (WebElement link : links) {
//                String href = link.getAttribute("href");
//                String lawsuitUrl = "http://www.tianyancha.com/lawsuit/detail" + href.substring(href.lastIndexOf("/")) + ".json";
//                HttpGet get = new HttpGet(lawsuitUrl);
//                Tianyangcha.configGet(get);
//                String jsonStr = HttpHelper.getForStringPage(Tianyangcha.client, get, 1, 0, Tianyangcha.encoding);
//                JSONObject json = JSONObject.parseObject(jsonStr);
//                Tianyangcha.putLawsuitData(json);
//            }
//        }
    }

    @org.junit.Test
    //招投标
    public void testf10() throws InterruptedException {
//        String pagesNum = driver.findElementByXPath("//div[@class='bid ng-scope']//div[@class='total ng-binding']").getText();
//        int s = Integer.valueOf(pagesNum.replaceAll("\\D+", ""));
//        WebElement nextClick = driver.findElementsByXPath("//div[@class='bid ng-scope']//li[@ng-if='::directionLinks']//a").get(1);
//        for (int i = 0; i < s; i++) {
//            if (i != 0) {
//                nextClick.click();
//                Thread.sleep(2000);
//            }
//            List<WebElement> links = driver.findElementsByXPath("//p[@ng-click='bidDetailClick();']//a[@class='ng-binding']");
//            for (WebElement link : links) {
//                String href = link.getAttribute("href");
//                String bidUrl = "http://www.tianyancha.com/extend/getCompanyBidByUUID.json?uuid=" + href.substring(href.lastIndexOf("/")+1) ;
//                HttpGet get = new HttpGet(bidUrl);
//                Tianyangcha.configGet(get);
//                String jsonStr = HttpHelper.getForStringPage(Tianyangcha.client, get, 1, 0, Tianyangcha.encoding);
//                JSONObject json = JSONObject.parseObject(jsonStr);
//                System.out.println(json.toJSONString());
//                Tianyangcha.putBidtData(json);
//            }
//        }
    }

    @org.junit.Test
    //法院公告
    public void testf3() throws InterruptedException {
        Tianyangcha tianyangcha = new Tianyangcha();
        RemoteWebDriver driver = tianyangcha.driver;
        MyBrowserDriver.repairByBrower(tianyangcha.driver);
        driver.get("http://www.tianyancha.com/company/150041670");
        Thread.sleep(1000);
        String pagesNum = driver.findElementByXPath("//div[@ng-if='company.Cour.length>0']//div[@class='total ng-binding']").getText();
        int s = Integer.valueOf(pagesNum.replaceAll("\\D+", ""));
        WebElement nextClick = driver.findElementsByXPath("//div[@ng-if='company.Cour.length>0']//li[@ng-if='::directionLinks']//a").get(1);
        for (int i = 0; i < s; i++) {
            if (i != 0) {
                nextClick.click();
                Thread.sleep(2000);
            }
            List<WebElement> links = driver.findElementsByXPath("//span[@ng-click='open(data)']");
            for (WebElement link : links) {
                link.click();
                Thread.sleep(1000);
                WebElement contentElement = driver.findElementByXPath("//div[@class='modal-body ng-scope']");
                String content = contentElement.getText();
                tianyangcha.putCourData(content);
                driver.findElementByXPath("//div[@ng-click='cancel()']").click();
            }
        }
    }

    //patent ng-scope
    @org.junit.Test
//专利信息
    public void testf4() throws InterruptedException {
//        String pagesNum = driver.findElementByXPath("//div[@class='patent ng-scope']//div[@class='total ng-binding']").getText();
//        int s = Integer.valueOf(pagesNum.replaceAll("\\D+", ""));
//        WebElement nextClick = driver.findElementsByXPath("//div[@class='patent ng-scope']//li[@ng-if='::directionLinks']//a").get(1);
//        for (int i = 0; i < s; i++) {
//            if (i != 0) {
//                nextClick.click();
//                Thread.sleep(2000);
//            }
//            List<WebElement> links = driver.findElementsByXPath("//div[@class='patent ng-scope']//span[@ng-click='openPatent(data)']");
//            for (WebElement link : links) {
//                link.click();
//                Thread.sleep(1000);
//                WebElement contentElement = driver.findElementByXPath("//div[@class='modal-body ng-scope']");
//                String content = contentElement.getText();
//                System.out.println(content);
//                Tianyangcha.putPatentData(content);
//                driver.findElementByXPath("//div[@ng-click='cancel()']").click();
//            }
//        }
    }

    //patent ng-scope
    @org.junit.Test
//债券信息
    public void testf9() throws InterruptedException {
        String pagesNum = driver.findElementByXPath("//div[@class='bond_box ng-scope']//div[@class='total ng-binding']").getText();
        int s = Integer.valueOf(pagesNum.replaceAll("\\D+", ""));
        WebElement nextClick = driver.findElementsByXPath("//div[@class='bond_box ng-scope']//li[@ng-if='::directionLinks']//a").get(1);
        for (int i = 0; i < s; i++) {
            if (i != 0) {
                nextClick.click();
                Thread.sleep(2000);
            }
            List<WebElement> links = driver.findElementsByXPath("//div[@class='bond_box ng-scope']//span[@ng-click='bondOpen(bondlist)']");
            for (WebElement link : links) {
                link.click();
                Thread.sleep(1000);
                WebElement contentElement = driver.findElementByXPath("//div[@class='modal-body ng-scope']");
                String content = contentElement.getText();
                System.out.println(content);
                driver.findElementByXPath("//div[@class='modal-content']//div[@ng-click='cancel()']").click();
                tianyangcha.putBondData(content);
            }
        }
    }

    //copyright ng-scope
    @org.junit.Test
//著作权
    public void testf5() throws InterruptedException {
        String pagesNum = driver.findElementByXPath("//div[@class='copyright ng-scope']//div[@class='total ng-binding']").getText();
        int s = Integer.valueOf(pagesNum.replaceAll("\\D+", ""));
        WebElement nextClick = driver.findElementsByXPath("//div[@class='copyright ng-scope']//li[@ng-if='::directionLinks']//a").get(1);
        for (int i = 0; i < s; i++) {
            if (i != 0) {
                nextClick.click();
                Thread.sleep(2000);
            }
            List<WebElement> trs = driver.findElementsByXPath("//tr[@ng-repeat='co in copyRight.copyrightRegList']");
            for (WebElement tr : trs) {
                String content = tr.getText();
                System.out.println(content);
                tianyangcha.putCopyrightData(content);
//                driver.findElementByXPath("//div[@ng-click='cancel()']").click();
            }
        }
    }

    @org.junit.Test
//招聘信息
    public void testf6() throws InterruptedException {
        String pagesNum = driver.findElementByXPath("//div[@class='company-content']//div[@class='total ng-binding']").getText();
        int s = Integer.valueOf(pagesNum.replaceAll("\\D+", ""));
        WebElement nextClick = driver.findElementsByXPath("//div[@class='company-content']//li[@ng-if='::directionLinks']//a").get(1);
        for (int i = 0; i < s; i++) {
            if (i != 0) {
                nextClick.click();
                Thread.sleep(2000);
            }
            List<WebElement> trs = driver.findElementsByXPath("//table[@ng-repeat='job in employe.companyEmploymentList']");
            for (WebElement tr : trs) {
                String content = tr.getText();
                System.out.println(content);
                tianyangcha.putJobData(content);
//                driver.findElementByXPath("//div[@ng-click='cancel()']").click();
            }
        }
    }

    @org.junit.Test
//变更信息
    public void testf7() throws InterruptedException {
//        List<WebElement> detailClick = driver.findElementsByXPath("//div[@ng-if='company.comChanInfoList.length>0']//a[@ng-show='needFolder' and @ng-click='showDetail = btnOnClick(showDetail)']");
//        detailClick.stream().filter(WebElement::isDisplayed).forEach(WebElement::click);
//        List<WebElement> tables = driver.findElementsByXPath("//div[@ng-if='company.comChanInfoList.length>0']//table[@class='staff-table ng-scope']");
//        for (WebElement table : tables) {
//            String content = table.getText().replaceAll("收起", "");
//
////        System.out.println(content);
//            Tianyangcha.putChangeData(content);
//        }
    }

    @org.junit.Test
//网站备案
    public void testf8() throws InterruptedException {
        List<WebElement> tables = driver.findElementsByXPath("//div[@class='company-content ng-scope' and @ng-if='icpInfo&&icpInfo.length>0']//table");
        for (WebElement table : tables) {
            String content = table.getText();

            System.out.println(content);
//            Tianyangcha.putIcpInfoData(content);
        }
    }

    @org.junit.Test
    public void testFinily() {
        while (true) {
            RemoteWebDriver driver = MyBrowserDriver.getSafariDriver();
            driver.get("http://www.tianyancha.com/company/6726986");
            WebElement html = driver.findElementByTagName("html");
            System.out.println(html.getText());
            driver.close();
        }
    }

    public void testListener() {
        WebClient webClient = WebClientHelper.getWebClient(false);
        webClient.setHTMLParserListener(new HTMLParserListener() {
            @Override
            public void error(String message, URL url, String html, int line, int column, String key) {

            }

            @Override
            public void warning(String message, URL url, String html, int line, int column, String key) {

            }
        });
    }

    //    @Before
//    @org.junit.Test
    public void before() throws InterruptedException {
        tianyangcha = new Tianyangcha();
        driver = tianyangcha.driver;
        MyBrowserDriver.repairByBrower(driver);
        driver.get("http://www.tianyancha.com/company/150041670");//腾讯
//        driver.get("http://www.tianyancha.com/company/17385107");//安钢
        Thread.sleep(1000);
        driver.get("http://www.tianyancha.com/company/2347869243");
        String pagesNum = driver.findElementByXPath("//div[@class='company-content']//div[@class='company_pager cl']//div[@class='total ng-binding']").getText();
        int s = Integer.valueOf(pagesNum.replaceAll("\\D+", ""));
        System.out.println(s);
    }

    @org.junit.Test
    public void copy() {
        Set<String> cpNames = RedisHelper.redis.getSetMembers("neeq_company");
        for (String cpName : cpNames) {
            RedisHelper.redis.insertToSet("neeq_company_copy", cpName);
        }

    }

    @org.junit.Test
    public void copy1() {
        Set<String> cpNames = RedisHelper.redis.getSetMembers("neeq_company_copy");
        for (String cpName : cpNames) {
            RedisHelper.redis.insertToSet("neeq_company", cpName);
        }

    }

    @org.junit.Test
    public void tianyangcha() {
        CompanyTianyanchaConsumer companyTianyanchaConsumer = new CompanyTianyanchaConsumer();
        Jedis jedis = RedisHelper.redis.getJedis();
        Set<String> cpNames = RedisHelper.redis.getSetMembers("neeq_company");
        if (cpNames != null && cpNames.size() > 0) {
            for (String cpName : cpNames) {
                companyTianyanchaConsumer.crawPage(cpName);
                RedisHelper.redis.insertToSet("tianyancha_site_topic", companyTianyanchaConsumer.result.toJSONString());
                companyTianyanchaConsumer.result.clear();
                jedis.srem("neeq_company", cpName);
            }
        }
    }

}

