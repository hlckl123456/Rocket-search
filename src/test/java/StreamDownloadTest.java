import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
public class StreamDownloadTest {

    @Test
    public void getUriResource() {
        String imgUrl = "https://ws3.sinaimg.cn/large/9150e4e5ly1fhnc60l3cdj204r04qaa4.jpg";

        // 获取连接客户端工具
        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response = null;

        try {

            // 创建POST请求对象
            HttpPost httpPost = new HttpPost(imgUrl);

            /*
             * 添加请求参数
             */
            // 创建请求参数
            List<NameValuePair> list = new LinkedList<>();
            BasicNameValuePair param1 = new BasicNameValuePair("name", "root");
            BasicNameValuePair param2 = new BasicNameValuePair("password", "123456");
            list.add(param1);
            list.add(param2);
            // 使用URL实体转换工具
            UrlEncodedFormEntity entityParam = new UrlEncodedFormEntity(list);
            httpPost.setEntity(entityParam);

            /*
             * 添加请求头信息
             */
            // 浏览器表示
            httpPost.addHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.7.6)");
            // 传输的类型
            httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");

            // 执行请求
            response = httpClient.execute(httpPost);
            // 获得响应的实体对象
            HttpEntity entity = response.getEntity();
            // 获取到响应信息的流
            InputStream is = entity.getContent();
            // 包装成高效流
            BufferedInputStream bis = new BufferedInputStream(is);

            // 写入本地 D 盘
            File file = new File("/Users/Claus/Desktop/牛逼.jpg");
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));

            byte[] bytes = new byte[1024 * 8];
            Integer len = -1;
            while ((len = bis.read(bytes)) != -1) {
                bos.write(bytes, 0, len);
            }

            bos.close();
            bis.close();

        } catch (ClientProtocolException e) {
            System.err.println("Http协议出现问题");
            e.printStackTrace();
        } catch (ParseException e) {
            System.err.println("解析错误");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IO异常");
            e.printStackTrace();
        } finally {
            // 释放连接
            if (null != response) {
                try {
                    response.close();
                    httpClient.close();
                } catch (IOException e) {
                    System.err.println("释放连接出错");
                    e.printStackTrace();
                }
            }
        }
    }
}
