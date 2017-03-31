package com.neeq.crawler.consumer.tianyangcha;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neeq.crawler.MyBrowserDriver;
import com.neeq.crawler.consumer.HttpHelper;
import com.neeq.crawler.dependence.Config;
import com.neeq.crawler.io.FileUploader;
import com.neeq.crawler.io.impl.LocalFileUploader;
import com.neeq.crawler.io.impl.OSSFileUploader;
import com.neeq.crawler.tool.ElementHelper;
import com.neeq.crawler.tool.HttpManager;
import com.neeq.crawler.tool.StringHelper;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Random;

/**
 * Created by bj on 16/7/14.
 */
public class Tianyangcha {
    private final static Logger log = LoggerFactory.getLogger(Tianyangcha.class);
    private FileUploader fileUploader;
    public JSONObject result;
    private String baseUrl = "http://www.tianyancha.com";
    public CloseableHttpClient client;
    public String encoding = "UTF-8";
    public RemoteWebDriver driver;
    public RemoteWebDriver tempDriver;


    public Tianyangcha() {
        result = new JSONObject();
        client = HttpManager.getClient();
        driver = MyBrowserDriver.getSafariDriver();
        tempDriver = MyBrowserDriver.getSafariDriver();
//        driver = MyBrowserDriver.getFirefoxDriver();
//        tempDriver = MyBrowserDriver.getFirefoxDriver();

        String fileSystem = Config.get("file.system", "local");
        if (fileSystem.equals("local")) {
            fileUploader = new LocalFileUploader();
        } else if (fileSystem.equals("oss")) {
            fileUploader = new OSSFileUploader();
        } else {
            throw new RuntimeException("错误的文件系统配置:" + fileSystem);
        }
    }

    public static void configGet(HttpGet get) {
        HttpManager.config(get);
        get.addHeader("Cookie", "TYCID=73968a213edc4290be58f7ca9a26c320; tnet=118.244.254.10; Hm_lvt_e92c8d65d92d534b0fc290df538b4758=1467603564,1467603622,1467603628,1467603653; Hm_lpvt_e92c8d65d92d534b0fc290df538b4758=1467603653; _pk_id.1.e431=233b12fe319bd78c.1467603281.1.1467604419.1467603281.; _pk_ref.1.e431=%5B%22%22%2C%22%22%2C1467603281%2C%22https%3A%2F%2Fwww.baidu.com%2Flink%3Furl%3DRC_ujQb0KUdyVk5Xo0_ElEUFE4qrkgq7FDA9QKQRqO-IWhg3KwA2MpU8OEci98OC%26wd%3D%26eqid%3Daba62e0800073f82000000035779d94f%22%5D; _pk_ses.1.e431=*; token=cfa599a689024d0694fc81997ca2fbec; _utm=a4a70c2dc6864d9ea5e6a65ff9bf9a2e");
        get.addHeader("Referer", "http://www.tianyancha.com/");//Referer:"http://www.tianyancha.com/search/%E4%B8%AD%E7%A7%91%E8%BD%AF%E7%A7%91%E6%8A%80%E8%82%A1%E4%BB%BD%E6%9C%89%E9%99%90%E5%85%AC%E5%8F%B8"
        get.addHeader("Tyc-From", "normal");//Tyc-From:"normal"
        get.addHeader("CheckError", "check");//CheckError:"check"
        get.addHeader("Host", "www.tianyancha.com");//Host:"www.tianyancha.com"
        get.addHeader("Host", "www.tianyancha.com");//Host:"www.tianyancha.com"
    }

    //年报
    public void getReport() throws InterruptedException {
        try {
            ////div[@id='blog_owner_name']
            List<WebElement> links = driver.findElementsByXPath("//div[@ng-if='company.annuRepYearList.length>0']//a[@class='report_year_box']");
            JSONArray array = new JSONArray();
            for (WebElement link : links) {
                String href = link.getAttribute("href");
                tempDriver.get(href);
                String content = tempDriver.findElementByXPath("//div[@class='modal-body report_body col-9']").getText();
                array.add(content);
                Thread.sleep(1000);
            }
            result.put("年报", array);
            debugLog("年报", array.toJSONString());

        } catch (NoSuchElementException e) {
            log.info("年报 无:");
        }
    }
    //商标

    public void getBrand(Document doc) {
        JSONArray infoArr = new JSONArray();
        Elements imgs = doc.select(".brand-content img");
        for (Element img : imgs) {
            String src = img.attr("src");
            String path = "";
            try {
                String[] split = src.split("/");
                path = fileUploader.upload(new URL(src).openStream(), split[split.length - 1], 0);
                infoArr.add(path);
            } catch (IOException e) {
                log.warn("upload file {} error", path, e);

            }
        }
        result.put("商标", infoArr);
        debugLog("商标", infoArr.toJSONString());

    }

    //对外投资
    public void getInvestInfo(Document doc) {
        JSONArray infoArr = new JSONArray();
        Elements divs = doc.select("div.ng-scope[ng-if=company.investList.length>0]:not(#nav-main-investList) div.ng-scope");
        for (Element div : divs) {
            Elements ps = div.select("p");
            String num = ps.last().text().split("：")[1];
            JSONObject json = new JSONObject();
//            String url = baseUrl + ps.first().select("a").attr("href");
//            Document newDoc = getHtmlDoc(url, 6, 0);
//            json.put("投资对象", fetchCompanyBaseInfo(newDoc));
            json.put("投资对象", ElementHelper.getSafeText(ps, 0));
            json.put("投资数额", num);
            infoArr.add(json);
        }
        result.put("对外投资", infoArr);
        debugLog("对外投资", infoArr.toJSONString());
    }

    //分支机构
    public void getBranchInfo(Document doc) {
        JSONArray infoArr = new JSONArray();
        Elements divs = doc.select("div.ng-scope[ng-if=company.branchList.length>0]:not(#nav-main-branch)");
        Elements as = divs.select("a");
        for (Element a : as) {
            String url = baseUrl + a.attr("href");
            String cpName = a.text();
            JSONObject json = new JSONObject();
            try {
                tempDriver.get(url);
                Thread.sleep(1000 * 3);
                Document newDoc = Jsoup.parse(tempDriver.getPageSource());
                json.put("基本信息", fetchCompanyBaseInfo(newDoc));
            } catch (InterruptedException e) {
                e.printStackTrace();
                log.error("分支机构基本信息抓取error", e);
            }
            json.put("分支机构名称", cpName);
            infoArr.add(json);
        }
        result.put("分支机构", infoArr);
        debugLog("分支机构", infoArr.toJSONString());
    }

    /**
     * 涉及诉讼
     */
    public void getLawsuit() throws InterruptedException {
        try {
            String pagesNum = driver.findElementByXPath("//div[@ng-if='company.lawSuitTotal>0']//div[@class='total ng-binding']").getText();
            String numStr = pagesNum.replaceAll("\\D+", "");
            if (!numStr.isEmpty()) {
                int s = Integer.valueOf(numStr);
                WebElement nextClick = driver.findElementsByXPath("//div[@ng-if='company.lawSuitTotal>0']//li[@ng-if='::directionLinks']//a").get(1);
                for (int i = 0; i < s; i++) {
                    if (i != 0) {
                        beforeClick(driver, nextClick);
                        nextClick.click();
                        Thread.sleep(2000);
                    }
                    List<WebElement> links = driver.findElementsByXPath("//a[@event-name='company-detail-lawsuit']");
                    for (WebElement link : links) {
                        String href = link.getAttribute("href");
                        String lawsuitUrl = "http://www.tianyancha.com/lawsuit/detail" + href.substring(href.lastIndexOf("/")) + ".json";
                        HttpGet get = new HttpGet(lawsuitUrl);
                        Tianyangcha.configGet(get);
                        String jsonStr = HttpHelper.getForStringPage(client, get, randomWaitTime(2), 0, encoding);
                        JSONObject json = JSONObject.parseObject(jsonStr);
                        putLawsuitData(json);
                        debugLog("涉及诉讼", jsonStr);
                    }
                }
            }
        } catch (NoSuchElementException e) {
            log.info("涉及诉讼 无:");
        }
    }

    /**
     * 招投标
     */
    public void getBid() throws InterruptedException {
        try {
            String pagesNum = driver.findElementByXPath("//div[@class='bid ng-scope']//div[@class='total ng-binding']").getText();
            String numStr = pagesNum.replaceAll("\\D+", "");
            if (!numStr.equals("")) {
                int s = Integer.valueOf(numStr);
                WebElement nextClick = driver.findElementsByXPath("//div[@class='bid ng-scope']//li[@ng-if='::directionLinks']//a").get(1);
                for (int i = 0; i < s; i++) {
                    if (i != 0) {
                        beforeClick(driver, nextClick);
                        nextClick.click();
                        Thread.sleep(2000);
                    }
                    List<WebElement> links = driver.findElementsByXPath("//p[@ng-click='bidDetailClick();']//a[@class='ng-binding']");
                    for (WebElement link : links) {
                        String href = link.getAttribute("href");
                        String bidUrl = "http://www.tianyancha.com/extend/getCompanyBidByUUID.json?uuid=" + href.substring(href.lastIndexOf("/") + 1);
                        HttpGet get = new HttpGet(bidUrl);
                        Tianyangcha.configGet(get);
                        String jsonStr = HttpHelper.getForStringPage(client, get, 1, 0, encoding);
                        JSONObject json = JSONObject.parseObject(jsonStr);
                        putBidtData(json);
                        debugLog("招投标", jsonStr);
                    }
                }

            }

        } catch (NoSuchElementException e) {
            log.info("招投标 无:");
        }
    }

    /**
     * 法院公告
     */
    public void getCour() throws InterruptedException {
        try {
            String pagesNum = driver.findElementByXPath("//div[@ng-if='company.Cour.length>0']//div[@class='total ng-binding']").getText();
            String numStr = pagesNum.replaceAll("\\D+", "");
            if (!numStr.isEmpty()) {
                int s = Integer.valueOf(numStr);
                WebElement nextClick = driver.findElementsByXPath("//div[@ng-if='company.Cour.length>0']//li[@ng-if='::directionLinks']//a").get(1);
                for (int i = 0; i < s; i++) {
                    if (i != 0) {
                        beforeClick(driver, nextClick);
                        nextClick.click();
                        Thread.sleep(2000);
                    }
                    List<WebElement> links = driver.findElementsByXPath("//span[@ng-click='open(data)']");
                    for (WebElement link : links) {
                        beforeClick(driver, link);
                        link.click();
                        Thread.sleep(1000);
                        WebElement contentElement = driver.findElementByXPath("//div[@class='modal-body ng-scope']");
                        String content = contentElement.getText();
                        putCourData(content);
                        driver.findElementByXPath("//div[@ng-click='cancel()']").click();
                    }
                }
            }
        } catch (NoSuchElementException e) {
            log.info("法院公告 无:");
        }
    }

    /**
     * 专利信息
     *
     * @throws InterruptedException
     */
    public void getPatent() throws InterruptedException {
        try {
            String pagesNum = driver.findElementByXPath("//div[@class='patent ng-scope']//div[@class='total ng-binding']").getText();
            String numStr = pagesNum.replaceAll("\\D+", "");
            if (!numStr.isEmpty()) {
                int s = Integer.valueOf(numStr);
                WebElement nextClick = driver.findElementsByXPath("//div[@class='patent ng-scope']//li[@ng-if='::directionLinks']//a").get(1);
                for (int i = 0; i < s; i++) {
                    if (i != 0) {
                        beforeClick(driver, nextClick);
                        nextClick.click();
                        Thread.sleep(2000);
                    }
                    List<WebElement> rows = driver.findElementsByXPath("//div[@class='patent ng-scope']//div[@class='patentitem ng-scope']//div[@class='row']");
                    List<WebElement> links = driver.findElementsByXPath("//div[@class='patent ng-scope']//span[@ng-click='openPatent(data)']");
                    for (int j = 0; j < rows.size(); j++) {
                        WebElement detailLink = links.get(j);
                        beforeClick(driver, detailLink);
                        detailLink.click();
                        Thread.sleep(1000);
                        WebElement contentElement = driver.findElementByXPath("//div[@class='modal-body ng-scope']");
                        String content = contentElement.getText();
                        JSONObject json = new JSONObject();
                        json.put("基本信息", rows.get(j).getText());
                        json.put("详情", content);
                        putPatentData(json);
                        debugLog("专利信息", json.toJSONString());
                        WebElement closeClick = driver.findElementByXPath("//div[@ng-click='cancel()']");
                        beforeClick(driver, closeClick);
                        closeClick.click();
                    }
                }
            }
        } catch (NoSuchElementException e) {
            log.info("专利信息 无:");
        }
    }

    /**
     * 债券信息
     *
     * @throws InterruptedException
     */
    public void getBond() throws InterruptedException {
        try {
            String pagesNum = driver.findElementByXPath("//div[@class='bond_box ng-scope']//div[@class='total ng-binding']").getText();

            String numStr = pagesNum.replaceAll("\\D+", "");
            if (!numStr.isEmpty()) {
                int s = Integer.valueOf(numStr);
                WebElement nextClick = driver.findElementsByXPath("//div[@class='bond_box ng-scope']//li[@ng-if='::directionLinks']//a").get(1);
                for (int i = 0; i < s; i++) {
                    if (i != 0) {
                        beforeClick(driver, nextClick);
                        nextClick.click();
                        Thread.sleep(2000);
                    }
                    List<WebElement> links = driver.findElementsByXPath("//div[@class='bond_box ng-scope']//span[@ng-click='bondOpen(bondlist)']");
                    for (WebElement link : links) {
                        beforeClick(driver, link);
                        link.click();
                        Thread.sleep(1000);
                        WebElement contentElement = driver.findElementByXPath("//div[@class='modal-body ng-scope']");
                        String content = contentElement.getText();
                        System.out.println(content);
                        driver.findElementByXPath("//div[@class='modal-content']//div[@ng-click='cancel()']").click();
                        putBondData(content);
                        debugLog("债券信息", content);

                    }
                }
            }
        } catch (NoSuchElementException e) {
            log.info("债券信息 无:");
        }
    }

    /**
     * 著作权
     */
    public void getCopyright() throws InterruptedException {
        try {
            String pagesNum = driver.findElementByXPath("//div[@class='copyright ng-scope']//div[@class='total ng-binding']").getText();
            String numStr = pagesNum.replaceAll("\\D+", "");
            if (!numStr.isEmpty()) {
                int s = Integer.valueOf(numStr);
                WebElement nextClick = driver.findElementsByXPath("//div[@class='copyright ng-scope']//li[@ng-if='::directionLinks']//a").get(1);
                for (int i = 0; i < s; i++) {
                    if (i != 0) {
                        beforeClick(driver, nextClick);
                        nextClick.click();
                        Thread.sleep(2000);
                    }
                    List<WebElement> trs = driver.findElementsByXPath("//tr[@ng-repeat='co in copyRight.copyrightRegList']");
                    for (WebElement tr : trs) {
                        String content = tr.getText();
                        putCopyrightData(content);
                        debugLog("著作权", content);
                    }
                }

            }

        } catch (NoSuchElementException e) {
            log.info("著作权 无:");
        }
    }

    /**
     * 招聘信息
     */
    public void getEmploye() throws InterruptedException {
        try {
            String pagesNum = driver.findElementByXPath("//div[@class='company-content']//div[@class='total ng-binding']").getText();
            String numStr = pagesNum.replaceAll("\\D+", "");
            if (!numStr.isEmpty()) {
                int s = Integer.valueOf(numStr);
                WebElement nextClick = driver.findElementsByXPath("//div[@class='company-content']//li[@ng-if='::directionLinks']//a").get(1);
                for (int i = 0; i < s; i++) {
                    if (i != 0) {
                        beforeClick(driver, nextClick);
                        nextClick.click();
                        Thread.sleep(2000);
                    }
                    List<WebElement> trs = driver.findElementsByXPath("//table[@ng-repeat='job in employe.companyEmploymentList']");
                    for (WebElement tr : trs) {
                        String content = tr.getText();
                        System.out.println(content);
                        putJobData(content);
                    }
                }
            }
        } catch (NoSuchElementException e) {
            log.info("招聘信息 无:");
        }
    }

    /**
     * 变更信息
     */
    public void getChanInfo() {
        try {
            List<WebElement> detailClick = driver.findElementsByXPath("//div[@ng-if='company.comChanInfoList.length>0']//a[@ng-show='needFolder' and @ng-click='showDetail = btnOnClick(showDetail)']");
            detailClick.stream().filter(WebElement::isDisplayed).forEach(WebElement::click);
            List<WebElement> tables = driver.findElementsByXPath("//div[@ng-if='company.comChanInfoList.length>0']//table[@class='staff-table ng-scope']");
            for (WebElement table : tables) {
                String content = table.getText().replaceAll("收起", "");

//        System.out.println(content);
                putChangeData(content);
                debugLog("变更信息", content);
            }
        } catch (NoSuchElementException e) {
            log.info("变更信息 无:");
        }
    }

    /**
     * 网站备案
     */
    public void getIcpInfo() {
        try {
            List<WebElement> tables = driver.findElementsByXPath("//div[@class='company-content ng-scope' and @ng-if='icpInfo&&icpInfo.length>0']//table");
            for (WebElement table : tables) {
                String content = table.getText();
//            System.out.println(content);
                putIcpInfoData(content);
            }
        } catch (NoSuchElementException e) {
            log.info("网站备案 无:");
        }
    }

    public void getBaseInfo(Document doc) {
        JSONObject baseInfo = fetchCompanyBaseInfo(doc);
        result.put("基本信息", baseInfo);
        debugLog("基本信息", baseInfo.toJSONString());
    }

    private JSONObject fetchCompanyBaseInfo(Document doc) {
        Elements company_content = doc.select("div.row.b-c-white.company-content");
        Elements table = company_content.select("table");
        JSONObject baseInfo = new JSONObject();
        baseInfo.put("名称", StringHelper.formatElements(doc.select("div.company_info_text p")));
        baseInfo.put("法定代表人", StringHelper.formatElements(table.first().select(".td-legalPersonName-value a")));
        baseInfo.put("注册资本", StringHelper.formatElements(table.first().select(".td-regCapital-value")));
        baseInfo.put("状态", StringHelper.formatElements(table.first().select(".td-regStatus-value")));
        baseInfo.put("注册时间", StringHelper.formatElements(table.first().select(".td-regTime-value")));
        Elements spans = table.last().select("td.basic-td div.c8 span.ng-binding");
        baseInfo.put("行业", ElementHelper.getSafeText(spans, 0));
        baseInfo.put("工商注册号", ElementHelper.getSafeText(spans, 1));
        baseInfo.put("企业类型", ElementHelper.getSafeText(spans, 2));
        baseInfo.put("组织机构代码", ElementHelper.getSafeText(spans, 3));
        baseInfo.put("营业期限", ElementHelper.getSafeText(spans, 4));
        baseInfo.put("登记机关", ElementHelper.getSafeText(spans, 5));
        baseInfo.put("核准日期", ElementHelper.getSafeText(spans, 6));
        baseInfo.put("统一信用代码", ElementHelper.getSafeText(spans, 7));
        baseInfo.put("注册地址", ElementHelper.getSafeText(spans, 8));
        baseInfo.put("经营范围", spans.last().text());
//        baseInfo.put("更新时间", doc.select("span[ng-if=company.updateTime]").text());
//        baseInfo.put("简介", doc.select("meta[name=description]").attr("content"));
        Elements ss = doc.select("div.company_info_text span");
        baseInfo.put("电话", ss.select("span:contains(电话):has(span)").text().split(":")[1]);
        baseInfo.put("网址", ss.select("span:contains(网址) a").attr("href"));
        baseInfo.put("邮箱", ss.select("span:contains(邮箱):has(span)").text().split(":")[1]);
        baseInfo.put("地址", ss.select("span:contains(地址):has(span)").text().split(":")[1]);

        return baseInfo;
    }

    public void getManagerInfo(Document doc) {
        JSONArray managerInfo = new JSONArray();
        Elements tables = doc.select("div.ng-scope[ng-if=company.staffList.length>0]:not(#nav-main-staff) table");
        for (Element table : tables) {
            Elements trs = table.select("tr");
            for (int i = 0; i < trs.first().children().size(); i++) {
                JSONObject json = new JSONObject();
                json.put("姓名", trs.first().child(i).text());
                json.put("职位", trs.get(1).child(i).text());
                managerInfo.add(json);
            }
        }
        result.put("高管人员", managerInfo);
        debugLog("高管人员", managerInfo.toJSONString());
    }

    public void getStockholder(Document doc) {
        JSONArray holderInfo = new JSONArray();
        Elements divs = doc.select("div.ng-scope[ng-if=company.investorList.length>0]:not(#nav-main-investment)");
        Elements ps = divs.select("p");
        for (int i = 0; i < ps.size() / 2; i++) {
            Element a = ps.remove(i);
            Element b = ps.remove(i++);

            JSONObject json = new JSONObject();
            json.put("股东", a.text());
            String text = b.text();
            json.put("投资类型", b.select("span").text());
            json.put("投资数额", text.replace(b.select("span").text(), "").split("：")[1]);
            holderInfo.add(json);
        }
        result.put("股东信息", holderInfo);
        debugLog("股东信息", holderInfo.toJSONString());
    }

    public void putLawsuitData(JSONObject jsonObject) {
        JSONArray 涉及诉讼 = result.getJSONArray("涉及诉讼");
        if (涉及诉讼 == null) {
            涉及诉讼 = new JSONArray();
            result.put("涉及诉讼", 涉及诉讼);
        }
        涉及诉讼.add(jsonObject);
    }

    public void putCourData(String content) {
        JSONArray 法院公告 = result.getJSONArray("法院公告");
        if (法院公告 == null) {
            法院公告 = new JSONArray();
            result.put("法院公告", 法院公告);
        }
        法院公告.add(content);
    }

    public void putCopyrightData(String content) {
        JSONArray 著作权 = result.getJSONArray("著作权");
        if (著作权 == null) {
            著作权 = new JSONArray();
            result.put("著作权", 著作权);
        }
        著作权.add(content);
    }

    public void putPatentData(JSONObject content) {
        JSONArray 专利信息 = result.getJSONArray("专利信息");
        if (专利信息 == null) {
            专利信息 = new JSONArray();
            result.put("专利信息", 专利信息);
        }
        专利信息.add(content);
    }

    public void putJobData(String content) {
        JSONArray 招聘信息 = result.getJSONArray("招聘信息");
        if (招聘信息 == null) {
            招聘信息 = new JSONArray();
            result.put("招聘信息", 招聘信息);
        }
        招聘信息.add(content);
    }

    public void putChangeData(String content) {
        JSONArray 变更信息 = result.getJSONArray("变更信息");
        if (变更信息 == null) {
            变更信息 = new JSONArray();
            result.put("变更信息", 变更信息);
        }
        变更信息.add(content);
    }

    public void putIcpInfoData(String content) {
        JSONArray 网站备案 = result.getJSONArray("网站备案");
        if (网站备案 == null) {
            网站备案 = new JSONArray();
            result.put("网站备案", 网站备案);
        }
        网站备案.add(content);
    }

    public void putBondData(String content) {
        JSONArray 债券信息 = result.getJSONArray("债券信息");
        if (债券信息 == null) {
            债券信息 = new JSONArray();
            result.put("债券信息", 债券信息);
        }
        债券信息.add(content);
    }

    public void putBidtData(JSONObject json) {
        JSONArray 招投标 = result.getJSONArray("招投标");
        if (招投标 == null) {
            招投标 = new JSONArray();
            result.put("招投标", 招投标);
        }
        招投标.add(json);
    }

    public void debugLog(String title, String info) {
//        if (log.isDebugEnabled()) {
//            log.debug("the title is ::{} , the content is ** {} .", title, info);
//        }
        log.info("the title is ::{} , the content is ** {} .", title, info);
        System.out.println("the title is ::" + title + " , the content is ** " + info);
    }

    public static int randomWaitTime(int max) {
        return new Random().nextInt(max) + 1;
    }

    public void destroy() {
        try {
            result.clear();

//            driver.close();

//            driver.quit();
//            tempDriver.close();
//            tempDriver.quit();
        } catch (Exception e) {

        }
    }

    public static void beforeClick(RemoteWebDriver driver, WebElement element) throws InterruptedException {
        if (!element.isDisplayed()) {

            Thread.sleep(5000);
            WebElement close = driver.findElementByXPath("//div[@ng-click='cancel()']");
            if (close != null) {
                close.click();
            }
            Actions action = new Actions(driver);
            action.moveToElement(element).click();
            driver.executeScript("");
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("window.scrollTo(0," + element.getLocation().y + ")");
//        element.click();

        }
    }
}
