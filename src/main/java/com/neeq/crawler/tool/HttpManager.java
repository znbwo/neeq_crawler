package com.neeq.crawler.tool;

import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Created by kidbei on 16/4/25.
 */
public class HttpManager {


//    private static PoolingHttpClientConnectionManager cm;
//    private static BasicHttpClientConnectionManager proxyCm;
//    private static HttpRequestRetryHandler httpRequestRetryHandler;


    static {
//        ConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();
//        LayeredConnectionSocketFactory sslsf = SSLConnectionSocketFactory.getSocketFactory();
//        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
//                .register("http", plainsf)
//                .register("https", sslsf)
//                .build();
//        cm = new PoolingHttpClientConnectionManager(registry);
//        // 将最大连接数增加到200
//        cm.setMaxTotal(50);
//        // 将每个路由基础的连接增加到20
//        cm.setDefaultMaxPerRoute(10);


//        proxyCm = new BasicHttpClientConnectionManager(registry);
        // 将最大连接数增加到200
//        proxyCm.setMaxTotal(3);
//        proxyCm.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(1000 * 10).setSoKeepAlive(true).build());
//        proxyCm.setDefaultMaxPerRoute(10);

        //请求重试处理
//        httpRequestRetryHandler = (exception, executionCount, context) -> {
//            if (executionCount >= 5) {// 如果已经重试了5次，就放弃
//                return false;
//            }
//            if (exception instanceof NoHttpResponseException) {// 如果服务器丢掉了连接，那么就重试
//                return true;
//            }
//            if (exception instanceof SSLHandshakeException) {// 不要重试SSL握手异常
//                return false;
//            }
//            if (exception instanceof InterruptedIOException) {// 超时
//                return false;
//            }
//            if (exception instanceof UnknownHostException) {// 目标服务器不可达
//                return false;
//            }
//            if (exception instanceof ConnectTimeoutException) {// 连接被拒绝
//                return false;
//            }
//            if (exception instanceof SSLException) {// ssl握手异常
//                return false;
//            }
//
//            HttpClientContext clientContext = HttpClientContext.adapt(context);
//            HttpRequest request = clientContext.getRequest();
//            // 如果请求是幂等的，就再次尝试
//            if (!(request instanceof HttpEntityEnclosingRequest)) {
//                return true;
//            }
//            return false;
//        };
    }


    private static class DefaultHttpRequestRetryHandler implements HttpRequestRetryHandler {

        @Override
        public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
            if (executionCount >= 5) {// 如果已经重试了5次，就放弃
                return false;
            }
            if (exception instanceof NoHttpResponseException) {// 如果服务器丢掉了连接，那么就重试
                return true;
            }
            if (exception instanceof SSLHandshakeException) {// 不要重试SSL握手异常
                return false;
            }
            if (exception instanceof InterruptedIOException) {// 超时
                return false;
            }
            if (exception instanceof UnknownHostException) {// 目标服务器不可达
                return false;
            }
            if (exception instanceof ConnectTimeoutException) {// 连接被拒绝
                return false;
            }
            if (exception instanceof SSLException) {// ssl握手异常
                return false;
            }

            HttpClientContext clientContext = HttpClientContext.adapt(context);
            HttpRequest request = clientContext.getRequest();
            // 如果请求是幂等的，就再次尝试
            if (!(request instanceof HttpEntityEnclosingRequest)) {
                return true;
            }
            return false;
        }
    }


    private static PoolingHttpClientConnectionManager defaultPoolManager() {
        PoolingHttpClientConnectionManager cm;
        ConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();
        LayeredConnectionSocketFactory sslsf = SSLConnectionSocketFactory.getSocketFactory();
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", plainsf)
                .register("https", sslsf)
                .build();
        cm = new PoolingHttpClientConnectionManager(registry);
        // 将最大连接数增加到200
        cm.setMaxTotal(50);
        // 将每个路由基础的连接增加到20
        cm.setDefaultMaxPerRoute(10);
        return cm;
    }


    public static BasicHttpClientConnectionManager proxyManager() {

        try {
            TrustManager trustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[]{trustManager}, null);
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(context, NoopHostnameVerifier.INSTANCE);


            ConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();
//            LayeredConnectionSocketFactory sslsf = SSLConnectionSocketFactory.getSocketFactory();
            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", plainsf)
                    .register("https", sslSocketFactory)
                    .build();
            BasicHttpClientConnectionManager proxyCm = new BasicHttpClientConnectionManager(registry);
            // 将最大连接数增加到200
            proxyCm.setSocketConfig(SocketConfig.custom().setSoTimeout(1000 * 10).setSoKeepAlive(true).build());

            return proxyCm;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static CloseableHttpClient getClient() {
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(defaultPoolManager())
                .setRetryHandler(new DefaultHttpRequestRetryHandler())
                .build();
        return httpClient;
    }


    public static CloseableHttpClient getProxyClient(String host, int port) {
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(proxyManager())
                .setRetryHandler(new DefaultHttpRequestRetryHandler())
                .setProxy(new HttpHost(host, port))
                .build();
//        httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
        return httpClient;
    }


    public static void config(HttpRequestBase requestBase) {
        config(requestBase, 1000 * 30);
    }


    public static void config(HttpRequestBase requestBase, int timeout) {
        requestBase.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36");
        requestBase.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        requestBase.setHeader("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");//"en-US,en;q=0.5");
        requestBase.setHeader("Accept-Charset", "ISO-8859-1,utf-8,gbk,gb2312;q=0.7,*;q=0.7");

        // 配置请求的超时设置
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(timeout)
                .setConnectTimeout(timeout)
                .setSocketTimeout(timeout)
                .setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY)
                .build();
        requestBase.setConfig(requestConfig);
    }


    public static HttpPost getPost(String url) {
        HttpPost post = new HttpPost(url);
        HttpManager.config(post);
        return post;
    }

    public static HttpGet getGet(String url) {
        String newUrl = url.replaceAll("　", "");
        HttpGet get = new HttpGet(newUrl);
        HttpManager.config(get);
        return get;
    }

    public static void configPost(HttpPost post, List<NameValuePair> params) throws UnsupportedEncodingException {
        post.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
    }

    public static void configGet(HttpGet get, List<NameValuePair> params) throws UnsupportedEncodingException {
        for (NameValuePair param : params) {
            get.setHeader(param.getName(), param.getValue());
        }
    }

    public static void close(CloseableHttpClient client) {
        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static void close(CloseableHttpResponse response) {
        if (response != null) {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

//
//    public static InputStream downFile(String url, int errorIndex) {
//
//        CloseableHttpResponse response = null;
//        try{
//            HttpGet get = new HttpGet(url);
//            HttpManager.config(get,1000 * 60);
//
//            response = getClient().execute(get);
//
//            return response.getEntity().getContent();
//
//        } catch(Exception e) {
//            int count = errorIndex + 1;
//            if (count < 3) {
//                return downFile(url,count);
//            } else {
//                return null;
//            }
//        } finally {
//            HttpManager.close(response);
//        }
//    }


//    public Header[] getCookie(HttpRequestBase request, int errorIndex) {
//        CloseableHttpResponse response = null;
//        try {
//            response = getClient().execute(request);
//            if (response.getStatusLine().getStatusCode() >= 400) {
//                return null;
//            }
//            return response.getHeaders("Set-Cookie");
//
//        } catch (Exception e) {
//            if (errorIndex < 3) {
//                int count = errorIndex + 1;
//                return getCookie(request, count);
//            }
//        } finally {
//            HttpManager.close(response);
//        }
//        return null;
//    }

    public static void main(String[] args) throws IOException {
//        InputStream is = downFile("https://www.itjuzi.com/images/1eebaaf713d463f679f89a2dbd972044.PNG",0);
//        InputStream is = new URL("http://static.oschina.net/uploads/user/933/1866807_50.png?t=1457791510000").openStream();

//        IOUtils.copy(is,new FileOutputStream(new File("/Users/kidbei/Downloads/test.jpg")));
    }

}
