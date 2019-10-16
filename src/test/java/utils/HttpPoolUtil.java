package utils;

import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;

/**
 * @program: Rocket-search
 * @author: Kailai Chen
 * @create: 2019-10-15 17:12
 **/
public class HttpPoolUtil {
    public static void main(String[] args) {

        System.out.println(httpGet("http://www.baidu.com", 6000));

    }

    /** 全局连接池对象 */
    /**
     * PoolingHttpClientConnectionManager是个复杂的类，它管理着连接池，可以同时为很多
     * 线程提供http连接请求。当请求一个新的连接时，如果连接池有可用的持久连接，连接管理器
     * 就会使用其中一个，而不会创建。
     */
    private static final PoolingHttpClientConnectionManager connManager =
            new PoolingHttpClientConnectionManager();
    /**
     * 静态代码块配置连接池信息
     */
    static {

        // 设置最大连接数
        connManager.setMaxTotal(200);
        // 设置每个路由默认连接数
        // setDefaultMaxPerRoute 是根据连接到的主机对setMaxTotal的一个划分
        // 我连接到http://sishuok.com 和 http://qq.com时，到每个主机的并发最多只有20；即加起来是40
        connManager.setDefaultMaxPerRoute(20);
    }



    /**
     * GET请求
     *
     * @param url 请求地址
     * @param timeOut 超时时间
     * @return
     */
    public static String httpGet(String url, Integer timeOut) {
        String msg = "-1";

        // 获取客户端连接对象
        CloseableHttpClient httpClient = getHttpClient(timeOut);
        // 创建GET请求对象
        HttpGet httpGet = new HttpGet(url);

        CloseableHttpResponse response = null;

        try {
            // 执行请求
            response = httpClient.execute(httpGet);
            // 获取响应实体
            HttpEntity entity = response.getEntity();
            // 获取响应信息
            msg = EntityUtils.toString(entity, "UTF-8");
        } catch (ClientProtocolException e) {
            System.err.println("协议错误");
            e.printStackTrace();
        } catch (ParseException e) {
            System.err.println("解析错误");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IO错误");
            e.printStackTrace();
        } finally {
            if (null != response) {
                try {
                    response.close();
                } catch (IOException e) {
                    System.err.println("释放链接错误");
                    e.printStackTrace();
                }
            }
        }

        return msg;
    }

    /**
     * 获取带超市属性的Http客户端连接对象
     * @param timeOut 超时时间
     * @return Http客户端连接对象
     */
    public static CloseableHttpClient getHttpClient(Integer timeOut) {
        // 创建Http请求配置参数
        RequestConfig requestConfig = RequestConfig.custom()
                // 从连接池获取连接的最长时间 （连接不够用的超市时间）
                .setConnectionRequestTimeout(timeOut)
                // 创建连接的最长时间
                .setConnectTimeout(timeOut)
                // 响应的最长时间，即获取数据传输的最长时间
                .setSocketTimeout(timeOut)
                .build();

        /**
         *  测出超时重试机制为了防止超时不生效而设置
         *  如果直接放回false,不重试
         *  这里会根据情况进行判断是否重试
         */
        HttpRequestRetryHandler retry = new HttpRequestRetryHandler() {
            @Override
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                if (executionCount >= 3) {// 如果已经重试了3次，就放弃
                    return false;
                }
                if (exception instanceof NoHttpResponseException) {// 如果服务器丢掉了连接，那么就重试
                    return true;
                }
                if (exception instanceof SSLHandshakeException) {// 不要重试SSL握手异常
                    return false;
                }
                if (exception instanceof InterruptedIOException) {// 超时
                    return true;
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
        };

        // 创建httpClient
        return HttpClients.custom()
                // 把请求相关的超时信息设置到连接客户端
                .setDefaultRequestConfig(requestConfig)
                // 把请求重试设置到连接客户端
                .setRetryHandler(retry)
                // 给ConnectionManager配置连接池
                .setConnectionManager(connManager)
                .build();
    }
}
