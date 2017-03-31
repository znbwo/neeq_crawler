package com.neeq.crawler.tool;

import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.ProxyConfig;
import com.gargoylesoftware.htmlunit.WebClient;
import com.neeq.crawler.consumer.HttpHelper;
import com.neeq.crawler.dependence.CoopRedis;
import org.apache.http.HttpHost;

/**
 * Created by bj on 16/7/5.
 */
public class WebClientHelper {
    private static CoopRedis redis = RedisHelper.redis;

    private WebClientHelper() {
    }

    private static class SingletonHolder {
        public final static WebClient instance = new WebClient();

        static {
            instance.getOptions().setCssEnabled(false); //禁用css支持
            instance.getOptions().setJavaScriptEnabled(true); //启用JS解释器，默认为true
            instance.getOptions().setThrowExceptionOnScriptError(false); //js运行错误时，是否抛出异常
            instance.getOptions().setTimeout(10000); //设置连接超时时间 ，这里是10S。如果为0，则无限期等待
//            instance.setJavaScriptTimeout(3600 * 1000);
            instance.getOptions().setRedirectEnabled(true);
            instance.setAjaxController(new NicelyResynchronizingAjaxController());
            instance.getOptions().setPrintContentOnFailingStatusCode(false);

//            instance.getOptions().setThrowExceptionOnScriptError(true);
//            instance.getOptions().setThrowExceptionOnFailingStatusCode(true);
            //性能优化
//            JavaScriptEngine sriptEngine = instance.getJavaScriptEngine();
//            HtmlUnitContextFactory factory = sriptEngine.getContextFactory();
//            Context context = factory.enterContext();
//            context.setOptimizationLevel(0);
        }


    }

    public static WebClient getWebClient(boolean useProxy) {
        if (useProxy) {
            changeIp("http");
        }
        return SingletonHolder.instance;
    }


    private static void changeIp(String schema) {
        HttpHost proxy = HttpHelper.getProxy(schema);
        if (proxy != null) {
            ProxyConfig config = new ProxyConfig(proxy.toHostString(), proxy.getPort());
            SingletonHolder.instance.getOptions().setProxyConfig(config);
        }
    }

}
