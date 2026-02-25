package com.tearsdeepmind.service.market;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class VontobelRawTest {
    public static void main(String[] args) {
        try {
            // URL Generica
            String url = "https://markets.vontobel.com/en-ch/products/leverage/turbo-open-end";
            
            System.out.println("Connecting to: " + url);
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();
            
            System.out.println("Page Title: " + doc.title());
            String body = doc.body().text();
            if (body.contains("S&P 500")) {
                 System.out.println("Found 'S&P 500' in text!");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
