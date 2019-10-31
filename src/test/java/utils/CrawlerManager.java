package utils;

/**
 * @program: Rocket-search
 * @author: Kailai Chen
 * @create: 2019-10-16 00:35
 **/

// Multithread version:
// 1. since the most time consuming part is the getUrls(url) function,
// we can use a Master/slave approach to parallel processing the getUrls(url),
// 2. The benefit of also making the read write of result set parallel
// doesn’t justify synch overhead. So, prefer just the master thread to do it.


import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*
    http://scrumbucket.org/tutorials/neo4j-site-crawler/part-2-create-multi-threaded-crawl-manager/
 */
public class CrawlerManager {
    private final int THREAD_COUNT = 10;
    private final int PAUSE_TIME = 1000;

    // 因为hashset每个元素的组成还是链表，单纯的用hashset可能会造成链表循环
    private Set<String> result = new CopyOnWriteArraySet<>();
    private List<Future<List<String>>> futures = new CopyOnWriteArrayList<>();
//    private ExecutorService executor = Executors.newFixedThreadPool()

    /**
     * 线程池的大小按照经验的估算：
     *  - 如果是CPU密集型应用，则线程池大小设置为N+1
     *  - 如果是IO密集型应用，则线程池大小设置为2N+1
     * 在IO优化中，这样的估算公式可能更适合：
     *  最佳线程数目 = （（线程等待时间+线程CPU时间）/线程CPU时间 ）* CPU数目
     *  因为明显，线程等待时间所占比例越高，需要越多线程。CPU所占时间越高需要越少线程。
     */
    /**
     * 阿里开发手册强制使用 ThreadPoolExecutor，用newFixedThreadPool有一定风险
     * 因为其内部的LinkedBlockingQueue默认使用Integer.MAX_VALUE，有oom的风险
     * also，some link will be rejected when the pool is full
     */
    private ExecutorService executor = Executors.newFixedThreadPool(4);
//    private ExecutorService executor =
    private static Map<String, List<String>> connectedUrls;

    public List<String> crawl(String url) {
        submitUrl(url);
        while(checkCrawlerResult());
        executor.shutdown();
        return new ArrayList<>(result);

    }

    // 这个方法有一个问题就是我们的max_thread_num 和 LinkedBlockingQueue 可能会无法容纳这么多的link

    private boolean checkCrawlerResult() {
        List<String> newUrls = new ArrayList<>();
        Iterator<Future<List<String>>> iterator = futures.iterator();
        while (iterator.hasNext()) {
            Future<List<String>> future = iterator.next();
            futures.remove(future);
            try {
                newUrls.addAll(future.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        for (String url : newUrls) {
            submitUrl(url);
        }

        return futures.size() > 0 || newUrls.size() > 0;
    }


    private List<String> getUrls(String url) {
        return connectedUrls.getOrDefault(url, new ArrayList<String>());
    }

    private void submitUrl(String url) {
        if (!result.contains(url)) {
            result.add(url);
            Crawler crawler = new Crawler(url);
            Future<List<String>> future = executor.submit(crawler);
            futures.add(future);
        }
    }

    private class Crawler implements Callable<List<String>> {
        private String url;
        public Crawler(String url) {
            this.url = url;
        }

        @Override
        public List<String> call() throws Exception {
            Thread.sleep(PAUSE_TIME);
            return getUrls(url);
        }
    }

    public static void main(String[] args) {
        connectedUrls = new HashMap<>();
        List<String> aChildren = new ArrayList<>();
        aChildren.add("b");
        aChildren.add("c");
        aChildren.add("d");
        aChildren.add("e");
        List<String> bChildren = new ArrayList<>();
        bChildren.add("k");
        bChildren.add("m");
        bChildren.add("d");
        bChildren.add("z");
        List<String> kChildren = new ArrayList<>();
        kChildren.add("o");
        kChildren.add("j");
        kChildren.add("e");
        kChildren.add("z");
        connectedUrls.put("a", aChildren);
        connectedUrls.put("b", bChildren);
        connectedUrls.put("k", kChildren);

        CrawlerManager crawlerManager = new CrawlerManager();
        System.out.println(crawlerManager.crawl("a"));
    }
}
