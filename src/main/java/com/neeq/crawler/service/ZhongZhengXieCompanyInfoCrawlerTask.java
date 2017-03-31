package com.neeq.crawler.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neeq.crawler.Constant;
import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.dependence.Md5Helper;
import com.neeq.crawler.mail.SendBy163Mail;
import com.neeq.crawler.push.PushQueue;
import com.neeq.crawler.task.TaskOptions;
import com.neeq.crawler.tool.HttpManager;
import com.neeq.crawler.tool.RedisHelper;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by bj on 16/6/27.
 */
public class ZhongZhengXieCompanyInfoCrawlerTask extends BasicClientCrawlerTask {

    private final Logger log = LoggerFactory.getLogger(ZhongZhengXieCompanyInfoCrawlerTask.class);
    private PushQueue pushQueue;
    private CoopRedis redis;
    private List<Entity> entities;
    private List<NameValuePair> params;
    private int index;

    private String baseUrl = "http://jg.sac.net.cn/pages/publicity/resource!search.action";
    private String baseUrl1 = "http://jg.sac.net.cn/pages/publicity/resource!list.action";

    private JSONObject result;

    private final String redisKey = Constant.Redis.COMPANY_INFO_ZZX_QUEUE;
    private final String kafkaTopic = Constant.Topic.COMPANY_INFO_ZZX_TOPIC;

    public ZhongZhengXieCompanyInfoCrawlerTask(PushQueue pushQueue, CoopRedis redis) {
        this.pushQueue = pushQueue;
        this.redis = redis;
    }

    public static void main(String[] args) {
        String date = DateUtils.formatDate(new Date(), "YYYY-MM-dd");
        System.out.println(date);
    }

    @Override
    public void next() {
        try {
            if (entities == null) {
                initEntities();//
            } else {
                if (index < entities.size()) {
                    params = new ArrayList<>();
                    result = new JSONObject();
                    Entity entity = entities.get(index);
                    String md5 = Md5Helper.getMd5(entity.cpName + DateUtils.formatDate(new Date(), "YYYY-MM-dd"));
                    if (RedisHelper.existInSet(redis, redisKey, md5)) {
                        if (log.isDebugEnabled()) {
                            log.debug(taskId() + " {} is exists", entity.cpName);
                        }
                    } else {
                        try {
                            fetchBaseInfo(entity);
                            fetchBranchCompany(entity);
                            fetchSalesDept(entity);
                            fetchManagerInfo(entity);
                            redis.insertToSet(redisKey, md5);
                            pushQueue.sendToQueue(kafkaTopic, result.toJSONString().getBytes());
                            if (log.isDebugEnabled()) {
                                log.debug(taskId() + ":{}", result.toJSONString());
                            }
                        } catch (Exception e) {
                            log.error("抓取{}失败,公司名={}", taskId(), entity.cpName, e);
                        }
                    }
                    index++;
                } else {
                    willStop = true;
                }
            }

        } catch (Exception e) {
            log.error("抓取{}失败,公司名={}", taskId(), e);
        }
    }


    private void fetchManagerInfo(Entity entity) throws UnsupportedEncodingException {
        HttpPost post = HttpManager.getPost(baseUrl);
        JSONArray managerArray = new JSONArray();
        params.clear();
        params.add(new BasicNameValuePair("filter_EQS_aoi_id", entity.AOI_ID));//filter_EQS_aoi_id	798
        params.add(new BasicNameValuePair("sqlkey", "publicity"));//sqlkey	publicity
        params.add(new BasicNameValuePair("sqlval", "EXECUTIVE_LIST"));//sqlval	EXECUTIVE_LIST
        HttpManager.configPost(post, params);
        String page = getForStringPage(post, 0);
        JSONArray array = JSONObject.parseArray(page);
        for (int i = 0; i < array.size(); i++) {
            JSONObject object = array.getJSONObject(i);
            JSONObject copy = new JSONObject();
            copy.put("名称", object.getString("EI_NAME"));
            copy.put("性别", object.getString("GC_ID"));
            copy.put("现任职务", object.getString("EI_CURRENT_POSITION"));
            copy.put("任职起始时间", object.getString("EI_OFFICE_DATE"));
            managerArray.add(copy);
        }
        result.put("高管信息", managerArray);
    }

    private void fetchBranchCompany(Entity entity) throws UnsupportedEncodingException {
        HttpPost post = HttpManager.getPost(baseUrl1);
        JSONArray branchArray = new JSONArray();
        params.clear();
        params.add(new BasicNameValuePair("filter_LIKES_mboi_branch_full_name", ""));
        params.add(new BasicNameValuePair("filter_LIKES_mboi_off_address", ""));
        params.add(new BasicNameValuePair("filter_EQS_aoi_id", entity.AOI_ID));
        params.add(new BasicNameValuePair("page.searchFileName", "publicity"));
        params.add(new BasicNameValuePair("page.sqlKey", "PAG_BRANCH_ORG"));
        params.add(new BasicNameValuePair("page.sqlCKey", "SIZE_BRANCH_ORG"));
        params.add(new BasicNameValuePair("_search", "false"));
        params.add(new BasicNameValuePair("nd", new Date().getTime() + ""));
        params.add(new BasicNameValuePair("page.pageSize", "15"));
        params.add(new BasicNameValuePair("page.orderBy", "MATO_UPDATE_DATE"));
        params.add(new BasicNameValuePair("page.order", "desc"));
        params.add(new BasicNameValuePair("page.pageNo", "1"));
        HttpManager.configPost(post, params);
        String page = getForStringPage(post, 0);
        JSONObject pageObject = JSONObject.parseObject(page);
        JSONArray array = pageObject.getJSONArray("result");

        for (int i = 0; i < array.size(); i++) {
            JSONObject json = array.getJSONObject(i);
            JSONObject copy = copyBranchCompanyJson(json);
            branchArray.add(copy);
        }
        while (pageObject.getString("hasNext").equalsIgnoreCase("true")) {
            NameValuePair remove = params.remove(params.size() - 1);
            params.add(new BasicNameValuePair(remove.getName(), (Integer.valueOf(remove.getValue()) + 1) + ""));
            HttpManager.configPost(post, params);
            pageObject = JSONObject.parseObject(getForStringPage(post, 0));
            JSONArray inarray = pageObject.getJSONArray("result");
            for (int i = 0; i < inarray.size(); i++) {
                JSONObject json = inarray.getJSONObject(i);
                JSONObject copy = copyBranchCompanyJson(json);
                branchArray.add(copy);
            }
        }
        result.put("分公司信息", branchArray);

    }


    private void fetchSalesDept(Entity entity) throws UnsupportedEncodingException {
        HttpPost post = HttpManager.getPost(baseUrl1);
        JSONArray salesArray = new JSONArray();
        params.clear();
        params.add(new BasicNameValuePair("filter_LIKES_msdi_name", ""));//filter_LIKES_msdi_name
        params.add(new BasicNameValuePair("filter_LIKES_msdi_reg_address", ""));//filter_LIKES_msdi_reg_address
        params.add(new BasicNameValuePair("filter_EQS_aoi_id", entity.AOI_ID));
        params.add(new BasicNameValuePair("page.searchFileName", "publicity"));
        params.add(new BasicNameValuePair("page.sqlKey", "PAG_SALES_DEPT"));
        params.add(new BasicNameValuePair("page.sqlCKey", "SIZE_SALES_DEPT"));
        params.add(new BasicNameValuePair("_search", "false"));
        params.add(new BasicNameValuePair("nd", new Date().getTime() + ""));
        params.add(new BasicNameValuePair("page.pageSize", "15"));
        params.add(new BasicNameValuePair("page.orderBy", "MATO_UPDATE_DATE"));
        params.add(new BasicNameValuePair("page.order", "desc"));
        params.add(new BasicNameValuePair("page.pageNo", "1"));
        HttpManager.configPost(post, params);
        String page = getForStringPage(post, 0);
        JSONObject pageObject = JSONObject.parseObject(page);
        JSONArray array = pageObject.getJSONArray("result");
        for (int i = 0; i < array.size(); i++) {
            JSONObject json = array.getJSONObject(i);
            JSONObject copy = copySalesDeptJson(json);
            salesArray.add(copy);
        }
        while (pageObject.getString("hasNext").equalsIgnoreCase("true")) {
            NameValuePair remove = params.remove(params.size() - 1);
            params.add(new BasicNameValuePair(remove.getName(), (Integer.valueOf(remove.getValue()) + 1) + ""));
            HttpManager.configPost(post, params);
            pageObject = JSONObject.parseObject(getForStringPage(post, 0));
            JSONArray inarray = pageObject.getJSONArray("result");
            for (int i = 0; i < inarray.size(); i++) {
                JSONObject json = inarray.getJSONObject(i);
                JSONObject copy = copySalesDeptJson(json);
                salesArray.add(copy);
            }
        }
        result.put("营业部信息", salesArray);

    }

    private JSONObject copyBranchCompanyJson(JSONObject json) {
        JSONObject copy = new JSONObject();
        copy.put("分公司名", json.getString("MBOI_BRANCH_FULL_NAME"));
        copy.put("负责人", json.getString("MBOI_PERSON_IN_CHARGE"));
        copy.put("业务范围", json.getString("MBOI_BUSINESS_SCOPE"));
        copy.put("办公地址", json.getString("MBOI_OFF_ADDRESS"));
        copy.put("分公司电话", json.getString("MBOI_CS_TEL"));
        return copy;
    }

    private JSONObject copySalesDeptJson(JSONObject json) {
        JSONObject copy = new JSONObject();
        copy.put("营业部名", json.getString("MSDI_NAME"));
        copy.put("地址", json.getString("MSDI_REG_ADDRESS"));
        copy.put("负责人", json.getString("MBOI_BUSINESS_SCOPE"));
        copy.put("注册地址", json.getString("MSDI_REG_PCC"));
        copy.put("客户服务与投诉电话", json.getString("MSDI_CS_TEL"));
        copy.put("所在地证监局投诉电话", json.getString("MSDI_ZJJ_COMPLAINTS_TEL"));
        return copy;
    }

    private void fetchBaseInfo(Entity entity) throws UnsupportedEncodingException {
        HttpPost post = HttpManager.getPost(baseUrl);
        params.clear();
        params.add(new BasicNameValuePair("filter_EQS_aoi_id", entity.AOI_ID));//filter_EQS_aoi_id	798
        params.add(new BasicNameValuePair("sqlkey", "publicity"));//sqlkey	publicity
        params.add(new BasicNameValuePair("sqlval", "SELECT_ZQ_REG_INFO"));//sqlval	SELECT_ZQ_REG_INFO
        HttpManager.configPost(post, params);
        String page = getForStringPage(post, 0);
        JSONObject object = JSONArray.parseArray(page).getJSONObject(0);//基本信息(不包含资格信息)
        result.clear();
        JSONObject baseJson = new JSONObject();
        baseJson.fluentPut("公司名", entity.cpName)//公司名
                .fluentPut("法人", object.getString("MRI_LEGAL_REPRESENTATIVE"))//法人
                .fluentPut("电话", object.getString("MRI_CUSTOMER_SERVICE_TEL"))//电话
                .fluentPut("注册资本", object.getString("MRI_REG_CAPITAL"))//注册资本
                .fluentPut("网址", object.getString("MRI_COM_WEBSITE"))//网址
                .fluentPut("注册地址", object.getString("MRI_INFO_REG"))//注册地址
                .fluentPut("办公地", object.getString("MRI_OFFICE_ADDRESS"))//办公地
                .fluentPut("经营证券业务许可证编号", object.getString("MRI_LICENSE_CODE"))//经营证券业务许可证编号
                .fluentPut("办公地邮编", object.getString("MRI_OFFICE_ZIP_CODE"))//办公地邮编
                .fluentPut("公司邮箱", object.getString("MRI_EMAIL"));//公司邮箱
        result.put("基本信息", baseJson);
        params.clear();
        params.add(new BasicNameValuePair("filter_EQS_aoi_id", entity.AOI_ID));//filter_EQS_aoi_id	798
        params.add(new BasicNameValuePair("sqlkey", "publicity"));//sqlkey	publicity
        params.add(new BasicNameValuePair("sqlval", "SEARCH_ZQGS_QUALIFATION"));//sqlval	SEARCH_ZQGS_QUALIFATION
        HttpManager.configPost(post, params);
        String zgPage = getForStringPage(post, 0); //营业资格page
        JSONArray zgArray = JSONArray.parseArray(zgPage);
        JSONArray qualificationArray = new JSONArray();
        for (int i = 0; i < zgArray.size(); i++) {
            String ptsc_name = zgArray.getJSONObject(i).getString("PTSC_NAME");//资格名
            qualificationArray.add(ptsc_name);
        }
        result.put("资格信息", qualificationArray);//资格数组


    }

    private void initEntities() throws UnsupportedEncodingException, MessagingException {
        HttpPost post = HttpManager.getPost(baseUrl);
        entities = new ArrayList<>();
        post = HttpManager.getPost(baseUrl);
        params = new ArrayList<>();
        params.add(new BasicNameValuePair("filter_EQS_O#otc_id", "01"));
//        params.add(new BasicNameValuePair("filter_EQS_O#otc_id", ""));
        params.add(new BasicNameValuePair("filter_EQS_O#sac_id", ""));
        params.add(new BasicNameValuePair("filter_LIKES_aoi_name", ""));
        params.add(new BasicNameValuePair("sqlkey", "publicity"));
        params.add(new BasicNameValuePair("sqlval", "ORG_BY_TYPE_INFO"));
        HttpManager.configPost(post, params);
        String page = getForStringPage(post, 0);
        JSONArray result = JSONArray.parseArray(page);
        for (int i = 0; i < result.size(); i++) {
            JSONObject resultJSONObject = result.getJSONObject(i);
            String aoi_id = resultJSONObject.getString("AOI_ID");
            String aoi_name = resultJSONObject.getString("AOI_NAME");
            entities.add(new Entity(aoi_name, aoi_id));
        }
        if (log.isDebugEnabled()) {
            if (entities.size() > 0) {
                log.debug(taskId() + ":{}", "信息初始化完成");
            } else {
                log.debug(taskId() + ":{}", "信息初始化失败");
                SendBy163Mail.getInstance().sendMail(taskId() + ":信息初始化失败");

            }

        }


    }


    @Override
    public String toString() {
        return taskId();
    }

    @Override
    public String taskId() {
        return "中证协证券公司信息公示";
    }


    @Override
    public TaskOptions options() {
        return new TaskOptions().setPeriod(1000 * 6).setTimeUnit(TimeUnit.MILLISECONDS);
    }


    @Override
    public boolean repeat() {
        return true;
    }


    @Override
    public long repeatAfterTime() {
        return 1000 * 60 * 60 * 12;
    }

    private class Entity {
        private String cpName;//公司名
        private String AOI_ID;

        public Entity(String cpName, String AOI_ID) {
            this.cpName = cpName;
            this.AOI_ID = AOI_ID;
        }
    }
}
