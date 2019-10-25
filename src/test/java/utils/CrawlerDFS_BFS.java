package utils;

/**
 * @author: Kailai Chen
 * @create: 2019-10-16 09:37
 **/

import java.util.*;

/**
 * 1.The most time consuming part:
 *      The part that the thread visit the url and get back urls
 *      Network latency, web page content parser and processing
 * 2.Helpful improvement to improve crawl speed
 *      - use httpClientPool to reuse tcp connection
 *      - set timeout to stop from some unreachable crawls
 *          - connection request timeout
 *          - establish connection timeout
 *          - socket response timeout
 *      - use multi-thread to implement crawl
 * 3.Define should visit scope:
 *      - use set to filter duplicate url
 *      - type of urls e.g. the one without ending in .pdf
 *      - size of the result
 */

public class CrawlerDFS_BFS {
    private static Map<String, List<String>> connectedUrls;

    public List<String> BFS(String url) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        visited.add(url);
        queue.add(url);

        while (!queue.isEmpty()) {
            String currUrl = queue.poll();
            List<String> children = getUrls(currUrl);
            for (String nUrl : children) {
                if (!visited.contains(nUrl)) {
                    queue.add(nUrl);
                    visited.add(nUrl);
                }
            }
        }

        return new ArrayList<>(visited);
    }

    public List<String> DFS(String url) {
        Set<String> result = new HashSet<>();
        DFSHelper(url, result);

        return new ArrayList<>(result);
    }

    private void DFSHelper(String url, Set<String> result) {
        if (url == null || result.contains(url)) return;

        result.add(url);
        List<String> childrenUrls = getUrls(url);
        for(String childUrl: childrenUrls) {
            DFSHelper(childUrl, result);
        }
    }

    private List<String> getUrls(String url) {
        return connectedUrls.getOrDefault(url, new ArrayList<String>());
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

        CrawlerDFS_BFS crawler = new CrawlerDFS_BFS();
        System.out.println(crawler.BFS("a"));
        System.out.println(crawler.DFS("a"));

    }

}
