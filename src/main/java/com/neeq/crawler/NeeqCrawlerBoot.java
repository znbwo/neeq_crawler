package com.neeq.crawler;

import com.neeq.crawler.dependence.Config;
import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.io.FileUploader;
import com.neeq.crawler.io.impl.LocalFileUploader;
import com.neeq.crawler.io.impl.OSSFileUploader;
import com.neeq.crawler.push.PushQueue;
import com.neeq.crawler.push.impl.KafkaPushQueue;
import com.neeq.crawler.service.company_info.ListedCompanyCrawlerTask;
import com.neeq.crawler.task.Crawler;
import com.neeq.crawler.task.CrawlerOptions;

import java.util.concurrent.CountDownLatch;


/**
 * Created by kidbei on 16/5/19.
 */
public class NeeqCrawlerBoot {


    private PushQueue pushQueue = new KafkaPushQueue();
    private int scheduleThreads = Config.getInt("crawler.options.scheduleThreads", 4);
    private CoopRedis redis;
    private FileUploader fileUploader;


    public void start() {

        String fileSystem = Config.get("file.system", "local");
        if (fileSystem.equals("local")) {
            fileUploader = new LocalFileUploader();
        } else if (fileSystem.equals("oss")) {
            fileUploader = new OSSFileUploader();
        } else {
            throw new RuntimeException("错误的文件系统配置:" + fileSystem);
        }

        String host = Config.get("redis.host", "127.0.0.1");
        int port = Config.getInt("redis.port", 6379);

        String redisPasswd = Config.get("redis.password");
        if (redisPasswd == null) {
            redis = new CoopRedis(host, port);
        } else {
            redis = new CoopRedis(host, port, redisPasswd);
        }

        CrawlerOptions options = new CrawlerOptions()
                .setScheduleThreads(scheduleThreads)


//                .addTask(new ProxyIpTask02(redis))
//                .addTask(new ProxyCheckTask(redis))
//                .addTask(new ProxyOkQueueCheckTask(redis))


//                .addTask(new NeeqDicCrawlerTask(pushQueue))//主办券商
                .addTask(new ListedCompanyCrawlerTask(pushQueue))
//                .addTask(new DeletedCompanyCrawlerTask(pushQueue))
//                .addTask(new NoticeCrawlerTask(pushQueue,redis,fileUploader))
//                .addTask(new HostBrokerCrawlerTask(pushQueue))
//                .addTask(new DongFangCaiFuZengFaCrawlerTask(pushQueue,redis))
//                .addTask(new DylyNewsListCrawlerTask(pushQueue,redis,fileUploader))
//                .addTask(new SinaNewsListCrawlerTask(pushQueue,redis,fileUploader))
//                .addTask(new ZhongJinZaiXianNewsCrawlerTask(pushQueue,redis))
//                .addTask(new WaBeiNewsListCrawlerTask(pushQueue,redis,fileUploader))
//                .addTask(new QQNewsListCrawlerTask(pushQueue,fileUploader,redis))
//                .addTask(new FengHuangNewsListCrawler(pushQueue,redis,fileUploader))
//                .addTask(new ApplyNeeqCompanyInfoCrawlerTask(fileUploader,pushQueue))
//                .addTask(new ApplyNeeqCompanyNewsCrawlerTask(pushQueue,fileUploader,redis))
//                .addTask(new PrivateFundManagerPersonListCrawlerTask(pushQueue))
//                .addTask(new TouZiJieNewsListCrawlerTask(pushQueue,redis,fileUploader))
//
//                .addTask(new TouZiJieOrgCrawlerTask(pushQueue,fileUploader))
//                .addTask(new TouZiJieInventsCrawlerTask(pushQueue,redis,fileUploader))
//                .addTask(new TouZiJieBingGouCrawlerTask(pushQueue,redis,fileUploader))
//                .addTask(new TouZiJieMuZiCrawlerTask(pushQueue,redis,fileUploader))

//                .addTask(new ZhongZhengXieCrawlerTask(pushQueue, fileUploader, redis))
//                .addTask(new ZhongZhengXieCompanyInfoCrawlerTask(pushQueue, redis))
//                .addTask(new HeXunCompanyReportCrawlerTask(pushQueue, fileUploader, redis))
//                .addTask(new HeXunNewStockReportCrawlerTask(pushQueue, fileUploader, redis))
//                .addTask(new HeXunIndustryReportCrawlerTask(pushQueue, fileUploader, redis))
//                .addTask(new HeXunMacroReportCrawlerTask(pushQueue, fileUploader, redis))


//                .addTask(new CnInfoShenShiNoticeCrawlerTask(pushQueue, redis, fileUploader))
//                .addTask(new CnInfoHuShiNoticeCrawlerTask(pushQueue, redis, fileUploader))
//                .addTask(new CnInfoHkNoticeCrawlerTask(pushQueue, redis, fileUploader))
//                .addTask(new CnInfoPublicCompanyCrawlerTask())
//                .addTask(new NeeqRulesCrawlerTask(pushQueue, redis, fileUploader))
//                .addTask(new NeeqLawsCrawlerTask(pushQueue, redis, fileUploader))
                ;

        Crawler crawler = new Crawler(options);
        crawler.start();


        CountDownLatch latch = new CountDownLatch(1);
//        new CompanyTianyanchaConsumer().start();
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
//        String s = Config.get("producer.bootstrap.servers", "127.0.0.1:9092");
//        String s = Config.get("producer.bootstrap.servers", "");
//        System.out.println(s);
//        String absolutePath = Config.getRootFile().getAbsolutePath();
//        System.out.println("absolutePath :"+absolutePath);
        new NeeqCrawlerBoot().start();
//        new Test().tianyangcha();
    }
}
