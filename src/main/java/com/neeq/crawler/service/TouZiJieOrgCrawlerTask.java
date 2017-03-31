package com.neeq.crawler.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neeq.crawler.Constant;
import com.neeq.crawler.io.FileUploader;
import com.neeq.crawler.push.PushQueue;
import com.neeq.crawler.task.TaskOptions;
import com.neeq.crawler.tool.HttpManager;
import org.apache.http.client.methods.HttpGet;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 投资界投资机构数据
 * Created by kidbei on 16/6/20.
 */
public class TouZiJieOrgCrawlerTask extends BasicClientCrawlerTask {


    private final Logger log = LoggerFactory.getLogger(TouZiJieOrgCrawlerTask.class);


    private final String DOMAIN_URL = "http://zdb.pedaily.cn/industry.shtml?_0.562299768115526";
    private final String TAG_URL_PRE = "http://zdb.pedaily.cn/company/";

    private PushQueue pushQueue;
    private FileUploader fileUploader;


    private boolean fullCrawed = false;


    //领域  代码-名称
    private final Map<String, String> tagMap = new HashMap<String, String>() {
        {
            this.put("h5537", "互联网");
            this.put("h4496", "电子商务");
            this.put("h5335", "电子支付");
            this.put("h4230", "C2C");
            this.put("h6271", "B2B");
            this.put("h884", "B2C");
            this.put("h5538", "网络营销");
            this.put("h5539", "资讯门户");
            this.put("h5540", "搜索");
            this.put("h2448", "广告代理商及网络营销服务机构");
            this.put("h3316", "网络服务");
            this.put("h4636", "网络游戏");
            this.put("h5632", "网络社区");
            this.put("h339", "网上招聘");
            this.put("h1206", "即时通讯");
            this.put("h1658", "网络旅游");
            this.put("h1894", "其它网络服务");
            this.put("h2466", "网络视频");
            this.put("h2524", "网络教育");
            this.put("h5737", "电信及增值业务");
            this.put("h3599", "电信运营");
            this.put("h2948", "移动运营商");
            this.put("h3319", "固网运营商");
            this.put("h5184", "虚拟运营商及其他");
            this.put("h4823", "无线互联网服务");
            this.put("h2407", "手机浏览器");
            this.put("h2541", "手机阅读");
            this.put("h2888", "移动IM");
            this.put("h3019", "无线音乐");
            this.put("h3286", "手机SNS");
            this.put("h1179", "无线搜索");
            this.put("h1598", "位置服务");
            this.put("h1686", "手机邮箱");
            this.put("h759", "手机游戏");
            this.put("h910", "无线营销");
            this.put("h5750", "手机购物");
            this.put("h5927", "移动互联网门户");
            this.put("h4212", "其它无限互联网服务");
            this.put("h4050", "固网增值服务");
            this.put("h3718", "视频点播");
            this.put("h6140", "网络购物");
            this.put("h806", "其它固网增值服务");
            this.put("h1740", "电信设备及终端");
            this.put("h2981", "通信软件");
            this.put("h448", "通信设备");
            this.put("h5626", "其它电信设备及终端");
            this.put("h5679", "通信终端");
            this.put("h1102", "其他电信业务及技术");
            this.put("h690", "IT");
            this.put("h686", "硬件");
            this.put("h688", "电脑硬件");
            this.put("h689", "电脑外设");
            this.put("h3852", "网络设备");
            this.put("h4991", "其它IT相关");
            this.put("h687", "软件");
            this.put("h6097", "应用软件");
            this.put("h4680", "基础软件");
            this.put("h1507", "其它软件服务");
            this.put("h5055", "IT服务");
            this.put("h6199", "IT咨询");
            this.put("h2062", "计算机与网络安全服务");
            this.put("h2741", "托管服务");
            this.put("h3216", "软件外包");
            this.put("h2287", "其它IT服务");
            this.put("h119", "连锁及零售");
            this.put("h2696", "餐饮");
            this.put("h4567", "酒店");
            this.put("h6137", "其它连锁及零售");
            this.put("h4334", "零售");
            this.put("h1154", "能源及矿产");
            this.put("h6448", "黑色金属矿采选");
            this.put("h1142", "电力、燃气及水的生产和供应业");
            this.put("h446", "煤炭开采和洗选");
            this.put("h3633", "非金属矿采选");
            this.put("h694", "石油和天然气开采");
            this.put("h2625", "有色金属矿采选");
            this.put("h5318", "冶炼/加工");
            this.put("h19", "其它能源及矿产");
            this.put("h1381", "广播电视及数字电视");
            this.put("h203", "运营商");
            this.put("h2751", "内容提供商");
            this.put("h5970", "终端设备及技术服务");
            this.put("h820", "网络传输");
            this.put("h136", "无线广播电视传输服务");
            this.put("h3425", "有线广播电视传输服务");
            this.put("h1907", "其它广播电视及数字电视");
            this.put("h2164", "娱乐传媒");
            this.put("h2794", "影视制作及发行");
            this.put("h4439", "电影制作与发行");
            this.put("h3758", "电视制作与发行");
            this.put("h2306", "电影放映");
            this.put("h2202", "户外媒体");
            this.put("h5863", "户外平面广告");
            this.put("h4963", "楼宇电视");
            this.put("h587", "户外LED");
            this.put("h2774", "移动电视");
            this.put("h5715", "娱乐与休闲");
            this.put("h1250", "动漫");
            this.put("h5493", "其它娱乐与休闲");
            this.put("h4716", "传统媒体");
            this.put("h5735", "出版业");
            this.put("h6526", "杂志");
            this.put("h3788", "报刊");
            this.put("h5558", "广告创意/代理");
            this.put("h6361", "广告代理");
            this.put("h3630", "广告创意");
            this.put("h1833", "媒介购买");
            this.put("h4318", "文化传播");
            this.put("h1629", "其它广告创意/代理");
            this.put("h2359", "物流");
            this.put("h243", "物流设备制造");
            this.put("h2226", "配送及仓储");
            this.put("h2434", "物流管理");
            this.put("h3720", "其它物流相关");
            this.put("h4631", "日常用品");
            this.put("h3664", "包装");
            this.put("h6541", "烟草");
            this.put("h281", "旅游");
            this.put("h5017", "造纸");
            this.put("h5498", "交通运输");
            this.put("h2649", "印刷");
            this.put("h371", "咨询");
            this.put("h3388", "其它其它");
            this.put("h2869", "教育与培训");
            this.put("h6439", "学历教育");
            this.put("h810", "专业培训");
            this.put("h3131", "职业教育");
            this.put("h4659", "其它教育与培训");
            this.put("h2947", "清洁技术");
            this.put("h4401", "环保");
            this.put("h1985", "新材料");
            this.put("h3537", "新能源");
            this.put("h6224", "其它清洁技术");
            this.put("h3052", "农/林/牧/渔");
            this.put("h1043", "农业");
            this.put("h364", "农业种植");
            this.put("h4505", "农业加工");
            this.put("h2436", "林业");
            this.put("h3904", "畜牧业");
            this.put("h6029", "渔业");
            this.put("h3362", "金融");
            this.put("h5597", "保险");
            this.put("h1181", "人寿保险");
            this.put("h4751", "保险辅助服务");
            this.put("h5302", "非人寿保险");
            this.put("h5897", "银行");
            this.put("h737", "国有银行");
            this.put("h1721", "商业银行");
            this.put("h3610", "其它银行");
            this.put("h2473", "金融服务");
            this.put("h2354", "金融租赁");
            this.put("h962", "金融信息服务");
            this.put("h1563", "金融信托与管理");
            this.put("h6370", "财务/审计/法律顾问");
            this.put("h5333", "其它金融服务");
            this.put("h2016", "证券");
            this.put("h3103", "证券分析与咨询");
            this.put("h6055", "证券经纪与交易");
            this.put("h4173", "证券市场管理");
            this.put("h6505", "其它证券");
            this.put("h3456", "食品&饮料");
            this.put("h1397", "食品制造业");
            this.put("h57", "米面制品制造");
            this.put("h191", "速冻食品制造");
            this.put("h3483", "液体乳及乳制品制造");
            this.put("h5527", "方便面及其他方便食品制造");
            this.put("h2461", "其它食品制造业");
            this.put("h1032", "食品加工");
            this.put("h470", "食用植物油加工");
            this.put("h2087", "水产品冷冻加工");
            this.put("h4873", "肉制品及副产品加工");
            this.put("h3032", "其它食品加工");
            this.put("h2959", "饮料制造业");
            this.put("h3950", "碳酸饮料制造");
            this.put("h4519", "酒制造");
            this.put("h1842", "其它饮料制造业");
            this.put("h3601", "半导体");
            this.put("h2426", "IC测试与封装");
            this.put("h38", "IC设计");
            this.put("h485", "IC设备制造");
            this.put("h2963", "其它半导体");
            this.put("h3622", "生物技术/医疗健康");
            this.put("h2381", "医药");
            this.put("h477", "化学药品原药制造业");
            this.put("h946", "动物用药品制造业");
            this.put("h2515", "中药材及中成药加工业");
            this.put("h4017", "生物制药");
            this.put("h4511", "化学药品制剂制造业");
            this.put("h2822", "其它医药");
            this.put("h4550", "保健品");
            this.put("h3299", "生物工程");
            this.put("h5617", "医疗服务");
            this.put("h3964", "医疗设备");
            this.put("h984", "其它生物技术/医疗健康");
            this.put("h4023", "机械制造");
            this.put("h3462", "电器机械及器材制造");
            this.put("h729", "电工器械制造");
            this.put("h1017", "家用电器制造");
            this.put("h3215", "照明器具制造");
            this.put("h5943", "电机制造");
            this.put("h5691", "其它电器机械及器材制造");
            this.put("h1520", "仪器仪表制造");
            this.put("h1045", "专用仪器仪表制造业");
            this.put("h3330", "通用仪器仪表制造业");
            this.put("h5754", "电子测量仪器制造业");
            this.put("h1502", "其它机械制造");
            this.put("h4238", "化工原料及加工");
            this.put("h6369", "日用化学");
            this.put("h1249", "新材料");
            this.put("h356", "化工原料生产");
            this.put("h5149", "农药及肥料");
            this.put("h5993", "其它化工原料及加工");
            this.put("h4971", "建筑/工程");
            this.put("h5357", "房屋和土木工程");
            this.put("h3458", "家具");
            this.put("h2825", "建材");
            this.put("h6270", "其它建筑/工程");
            this.put("h4973", "房地产");
            this.put("h2602", "物业管理");
            this.put("h6402", "房地产中介服务");
            this.put("h1343", "房地产开发经营");
            this.put("h1152", "其它房地产");
            this.put("h5044", "汽车");
            this.put("h2106", "汽车制造");
            this.put("h3520", "汽车维修");
            this.put("h5645", "汽车租赁");
            this.put("h5776", "汽车销售渠道");
            this.put("h1087", "其它汽车");
            this.put("h5643", "纺织及服装");
            this.put("h5851", "电子及光电设备");
            this.put("h2416", "电子设备");
            this.put("h2693", "电子元件及组件制造");
            this.put("h4428", "集成电路制造");
            this.put("h5295", "电子工业专用设备制造");
            this.put("h175", "印制电路板制造");
            this.put("h890", "其它电子设备");
            this.put("h1683", "光电");
            this.put("h2795", "光通信");
            this.put("h5517", "其它光电");
            this.put("h6272", "激光");
            this.put("h60", "材料与元器件");
            this.put("h486", "光存储");
            this.put("h1442", "光电显示器");
            this.put("h2181", "其他电子产品");
            this.put("h3754", "数字化电子产品");
            this.put("h1773", "电源");
            this.put("h1906", "其它其他电子产品");
        }
    };


    private final LinkedBlockingQueue<String> tagQueue = new LinkedBlockingQueue<>();


    public TouZiJieOrgCrawlerTask(PushQueue pushQueue, FileUploader fileUploader) {
        this.pushQueue = pushQueue;
        this.fileUploader = fileUploader;
    }


    @Override
    public void next() {


        try {

            if (tagQueue.isEmpty()) {
                for (String code : tagMap.keySet()) {
                    tagQueue.put(code);
                }
                if (fullCrawed) {
                    willStop = true;

                }
            }


            int page = 1;

            String code = tagQueue.poll();

            String url = TAG_URL_PRE + code + "/" + page;

            HttpGet get = new HttpGet(url);
            HttpManager.config(get);

            while (true) {
                Elements lis = fetchForList(get);
                if (lis == null || lis.isEmpty()) {
                    page = 1;
                    break;
                }

                for (Element li : lis) {
                    try {
                        String imgUrl = li.select(".img a img").first().attr("src");

                        InputStream is = new URL(imgUrl).openStream();
                        imgUrl = fileUploader.upload(is, UUID.randomUUID().toString(), 0);

                        String name = li.select(".txt h3 a").first().text();
                        String investStr = li.select(".txt h3 span").text();
                        String investType = null;  //投资性质
                        String investStep = null;  //投资阶段
                        if (investStr != null) {
                            String[] ss = investStr.split("/");
                            investType = ss[0].trim();
                            if (ss.length > 1) {
                                investStep = ss[1].trim();
                            }
                        }

                        //简称
                        String simpleName = li.select(".txt span a").first().text();

                        String infoUrl = li.select(".txt span a").first().attr("href");
                        HttpGet infoGet = new HttpGet(infoUrl);
                        HttpManager.config(infoGet);

                        Document infoDoc = getForDocPage(infoGet, 0);

                        Elements infoLis = infoDoc.select(".news-show.company-show .box-caption ul li");
                        //成立时间 ：
                        String cts = null;
                        if (infoLis.get(0).text().indexOf("：") != -1) {
                            try {
                                cts = infoLis.get(0).text().split("：")[1];
                            } catch (Exception e) {

                            }
                        }
                        //机构总部
                        String place = null;
                        if (infoLis.get(1).text().indexOf("：") != -1) {
                            try {
                                cts = infoLis.get(1).text().split("：")[1];
                            } catch (Exception e) {

                            }
                        }
                        //注册地点
                        String rePlace = null;
                        if (infoLis.get(2).text().indexOf("：") != -1) {
                            try {
                                cts = infoLis.get(2).text().split("：")[1];
                            } catch (Exception e) {

                            }
                        }
                        //官方网站
                        String website = infoLis.get(3).child(0).text();

                        Element contentEl = infoDoc.select(".news-show.company-show .box-content").first();
                        contentEl.getElementsByClass("caption").remove();
                        contentEl.getElementsByClass("jiathis_style_24x24").remove();
                        //描述
                        String desc = contentEl.html();


                        JSONObject result = new JSONObject()
                                .fluentPut("name", name)
                                .fluentPut("investType", investType)//投资性质
                                .fluentPut("investStep", investStep)//投资阶段
                                .fluentPut("simpleName", simpleName)//简称
                                .fluentPut("cts", cts)   //创办时间
                                .fluentPut("desc", desc) //描述
                                .fluentPut("place", place)   //地址
                                .fluentPut("rePlace", rePlace)   //注册地址
                                .fluentPut("website", website)  //网站
                                .fluentPut("imgUrl", imgUrl);    //logo


                        //抓取管理团队信息
                        Elements mLis = infoDoc.select(".list-pics li");
                        if (mLis != null && !mLis.isEmpty()) {
                            JSONArray mArray = new JSONArray();

                            for (Element mli : mLis) {
                                JSONObject manager = new JSONObject();

                                String mUrl = mli.child(0).attr("href");

                                HttpGet mGet = new HttpGet(mUrl.replace(" ","%20")); //url中对空格的处理
                                HttpManager.config(mGet);

                                Document mDoc = getForDocPage(mGet, 0);

                                //人名
                                String pName = mDoc.select(".people-show .people .u-info .txt h1").first().text();
                                //头像
                                String avatar = mDoc.select(".people-show .people .u-info .img img").first().attr("src");
                                avatar = fileUploader.upload(new URL(avatar).openStream(), UUID.randomUUID().toString(), 0);
                                //人物title
                                String pTitle = mDoc.select(".people-show .people .u-info .txt p").first().text();
                                Element pDescEl = mDoc.select(".people-show .people .box-content").first();
                                pDescEl.getElementsByClass("caption").remove();
                                //人物简介
                                String pDesc = pDescEl.html();


                                //解析职业经历
                                Element tb = mDoc.select(".box-fix-c.index-focus .box.box-border-top .zdb-people-table").first();
                                if (tb != null) {
                                    Elements trs = tb.select("tbody tr");
                                    if (trs != null && !trs.isEmpty()) {
                                        JSONArray exps = new JSONArray();
                                        trs.remove(0);
                                        for (Element tr : trs) {
                                            JSONObject exp = new JSONObject();
                                            String company = tr.child(0).text();    //所在公司
                                            String post = tr.child(1).text();       //职位
                                            String date = tr.child(2).text();       //日期
                                            exp.fluentPut("company", company)
                                                    .fluentPut("post", post)
                                                    .fluentPut("date", date);
                                            exps.add(exp);
                                        }
                                        manager.put("exps", exps);   //经历
                                    }
                                }

                                manager.fluentPut("pName", pName)    //姓名
                                        .fluentPut("pTitle", pTitle) //title
                                        .fluentPut("pDesc", pDesc)  //简介
                                        .fluentPut("avatar", avatar);//头像
                                mArray.add(manager);
                            }

                            result.put("managers", mArray);//管理团队数组
                        }


                        pushQueue.sendToQueue(Constant.Topic.INVEST_ORG_TOUZIJIE_TOPIC,result.toJSONString().getBytes());
                        if (log.isDebugEnabled()) {
                            log.debug("抓取到投资界的投资机构:{}", result.toJSONString());
                        }

                    } catch (Exception e) {
                        log.error("解析机构出错", e);
                    }
                }

                page += 1;
            }

        } catch (Exception e) {
            log.error("抓取投资界投资机构数据失败", e);
        }
    }


    private Elements fetchForList(HttpGet get) {
        try {

            Document doc = getForDocPage(get, 0);

            return doc.select(".company-list .news-list li");
        } catch (Exception e) {
            return null;
        }
    }


    @Override
    public String taskId() {
        return "投资界投资机构数据";
    }


    @Override
    public boolean repeat() {
        return true;
    }


    @Override
    public long repeatAfterTime() {
        return 1000 * 60 * 60 * 12;
    }

    @Override
    public TaskOptions options() {
        return new TaskOptions().setPeriod(1000 * 10).setTimeUnit(TimeUnit.MILLISECONDS);
    }

}
