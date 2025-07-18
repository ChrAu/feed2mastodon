package com.hexix;

import io.quarkus.logging.Log;
import org.jboss.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.StringJoiner;

public class JsoupParser {
    final static Logger LOG = Logger.getLogger(JsoupParser.class);


    private JsoupParser(){

    }
    private final static String TAGESSCHAU_CSS_QUERY ="#content article > *:not(div.meldungsfooter)";


    public static String getArticle(String url){
        Document doc = null;
        String cssQuery;

        if(url == null || url.isEmpty()){
            return null;
        }

        if(url.contains("tagesschau.de")){
            cssQuery = TAGESSCHAU_CSS_QUERY;
        }else{
            return null;
        }


        try {
            doc = Jsoup.connect(url).get();
            LOG.info(doc.title());
            Elements article = doc.select(cssQuery);
            StringJoiner sj = new StringJoiner("\n");

            for (Element element : article) {
                sj.add(element.text());
            }


            LOG.debug(sj);
            return sj.toString();

        } catch (IOException e) {
            Log.warn("", e);
        }

        return null;

    }

}
