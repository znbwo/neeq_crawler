package com.neeq.crawler.consumer.tianyangcha;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.neeq.crawler.Constant;
import com.neeq.crawler.MyBrowserDriver;
import com.neeq.crawler.consumer.DefaultKafkaConsumer;
import com.neeq.crawler.consumer.HttpHelper;
import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.io.FileUploader;
import com.neeq.crawler.push.PushQueue;
import com.neeq.crawler.push.impl.KafkaPushQueue;
import com.neeq.crawler.tool.HttpManager;
import com.neeq.crawler.tool.RedisHelper;
import com.neeq.crawler.tool.WebClientHelper;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.DateUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.Date;
import java.util.Set;

/**
 * 获取消息队列里面的公司数据
 * Created by kidbei on 16/6/29.
 */
public class CompanyTianyanchaConsumer extends DefaultKafkaConsumer {
    private final static Logger log = LoggerFactory.getLogger(CompanyTianyanchaConsumer.class);
    private PushQueue pushQueue = new KafkaPushQueue();
    private CoopRedis redis = RedisHelper.redis;
    private FileUploader fileUploader;
    public JSONObject result = new JSONObject();
    private String sendTopic = Constant.Topic.TIANYANCHA_SITE_TOPIC;
    private String redisKey = Constant.Redis.TIANYANCHA_SITE_QUEUE;
    private volatile boolean finished = false;
    private String baseUrl = "http://www.tianyancha.com";
    private Tianyangcha tianyangcha;

    public CompanyTianyanchaConsumer() {
        this(Constant.Topic.NEEQ_COMPANY_TOPIC);
    }

    public CompanyTianyanchaConsumer(String topic) {
        super(topic);
        tianyangcha = new Tianyangcha();
    }

    @Override
    public void consume(String topic, byte[] data) {
        JSONObject json = JSONObject.parseObject(new String(data));
        String cpName = json.getJSONObject("baseinfo").getString("name");
        try {
            log.info(" kafka consume:" + cpName);
            String md5 = cpName + DateUtils.formatDate(new Date(), "YYYY-MM-dd");
            if (!RedisHelper.existInSet(redis, redisKey, md5)) {
                try {
                    crawPage(cpName);
                    pushQueue.sendToQueue(sendTopic, result.toJSONString().getBytes());
                    redis.insertToSet(redisKey, md5);
                    if (log.isDebugEnabled()) {
                        log.info("抓到天眼查数据{}", result.toJSONString());
                    }
                } catch (Exception e) {
                    log.error("抓取天眼查数据{}失败", cpName, e);
                }
            } else {
                log.info("天眼查数据{}已存在", cpName);
            }
        } catch (Exception e) {
            log.error("json is {}", json, e);
        } finally {
            finished = true;
        }
    }

    public void crawPage(String cpName) {
        String jsonUrl = "http://www.tianyancha.com/suggest/" + cpName + ".json";
        HttpGet get = HttpManager.getGet(jsonUrl);
        Tianyangcha.configGet(get);
        String companyNum = null;

        try {
            companyNum = HttpHelper.getForStringPage(tianyangcha.client, get, 0, 0, tianyangcha.encoding);
            JSONArray data = JSONObject.parseObject(companyNum).getJSONArray("data");
            if (data != null && data.size() > 0) {
                JSONObject suggest = data.getJSONObject(0);
                if (suggest != null && suggest.getString("name").equalsIgnoreCase(cpName)) {

                    repair(tianyangcha.driver);
                    String id = suggest.getString("id");
                    String url = baseUrl + "/company/" + id;
                    tianyangcha.driver.get(url);
                    Thread.sleep(1000 * 3);
                    Document doc = Jsoup.parse(tianyangcha.driver.getPageSource());


                    tianyangcha.getBaseInfo(doc);
                    tianyangcha.getBranchInfo(doc);
                    tianyangcha.getInvestInfo(doc);
                    tianyangcha.getManagerInfo(doc);
                    tianyangcha.getStockholder(doc);
                    tianyangcha.getBrand(doc);
                    tianyangcha.getReport();
                    tianyangcha.getLawsuit();
                    tianyangcha.getPatent();
                    tianyangcha.getBid();
                    tianyangcha.getBond();
                    tianyangcha.getChanInfo();
                    tianyangcha.getCopyright();
                    tianyangcha.getCour();
                    tianyangcha.getEmploye();
                    tianyangcha.getIcpInfo();

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                Thread.sleep(1000 * Tianyangcha.randomWaitTime(4));
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            crawPage(cpName);
        } finally {
            result = tianyangcha.result;
        }
    }


    public static Page getHtmlPage(String url, int timeOut, int errorIndex) {
        WebClient webClient = WebClientHelper.getWebClient(false);
        try {
            Page page = webClient.getPage(url);
            webClient.waitForBackgroundJavaScript(1000 * timeOut);
//            boolean needRepair = isNeedRepair(page.getWebResponse().getContentAsString());
//            if (needRepair) {
//                repair();
//            }
            return page;
        } catch (Throwable e) {
            log.warn("get url {} error {}", url, errorIndex, e);
        } finally {
            webClient.close();
        }
        if (errorIndex < 3) {
            errorIndex++;
            return getHtmlPage(url, timeOut, errorIndex);
        }
        return null;
    }

    static String getHtmlStr(String url, int timeOut, int errorIndex) {
        Page htmlPage = getHtmlPage(url, timeOut, errorIndex);
        if (htmlPage != null) {
            return htmlPage.getWebResponse().getContentAsString();
        } else return null;
    }

    static Document getHtmlDoc(String url, int timeOut, int errorIndex) {
        String htmlStr = getHtmlStr(url, timeOut, errorIndex);
        if (htmlStr != null) {
            return Jsoup.parse(htmlStr);
        } else return null;
    }

    static JSONObject getJson(String url, int errorIndex) {
        String string = "";
        try {
            string = getHtmlStr(url, 0, errorIndex);
            return JSONObject.parseObject(string);
        } catch (Exception e) {
            log.warn("{} error to json ,the url is {} ,times is {}", string, url, errorIndex, e);
        }
        if (errorIndex < 3) {
            errorIndex++;
            return getJson(url, errorIndex);
        }
        return null;
    }

    /**
     * 不再使用
     *
     * @return
     */
    public String getInfo() {
        String sr = "";
        JSONObject 基本信息 = result.getJSONObject("基本信息");
        if (基本信息 != null) {
            sr = 基本信息.getString("名称") + " " + 基本信息.getString("电话") + " " + 基本信息.getString("邮箱") + " " + 基本信息.getString("网址") + "\n";
        }
        return sr;
    }

    /**
     * 欺骗网站
     * 进行正常浏览行为
     */
    public static void repair(RemoteWebDriver driver) {
        MyBrowserDriver.repairByBrower(driver);
        System.out.println("the request is repair ...");
    }

    private static boolean isNeedRepair(String html) {
        return html.contains("<script src=\"http://static.geetest.com/");
    }


    public static void main(String[] args)  {
        CompanyTianyanchaConsumer companyTianyanchaConsumer = new CompanyTianyanchaConsumer();
        Jedis jedis = RedisHelper.redis.getJedis();
        Set<String> cpNames = RedisHelper.redis.getSetMembers("neeq_company");
        if (cpNames != null && cpNames.size() > 0) {
            for (String cpName : cpNames) {
                companyTianyanchaConsumer.crawPage(cpName);
                RedisHelper.redis.insertToSet("tianyancha_site_topic", companyTianyanchaConsumer.result.toJSONString());
                jedis.srem("neeq_company", cpName);
            }
        }
    }
}