package com.hexix;

import io.quarkus.logging.Log;
import org.jboss.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.StringJoiner;

public class JsoupParser {
    final static Logger LOG = Logger.getLogger(JsoupParser.class);
    private static final String SWR_CSS_QUERY = "main h1.headline, main .detail-body p.lead, main .bodytext h2, main .bodytext p, main .bodytext figure.quote";
    private static final String DEUTSCHLANDFUNK_CSS_QUERY = "article.b-article > header > *:not(.article-header-actions, .article-header-meta), .article-details-text, .article-details-title";


    private JsoupParser(){

    }
    private final static String TAGESSCHAU_CSS_QUERY ="#content article > *:not(div.meldungsfooter)";
    private final static String ZDF_HEUTE_CSS_QUERY = "main > div";
    private final static String HEISE_CSS_QUERY = "article > *:not(p.printversion__back-to-article)";

    private final static String BW_CSS_QUERY="article > header, article > .article__body";


    public static String getArticle(String url){
        Document doc = null;
        String cssQuery;

        if(url == null || url.isEmpty()){
            return null;
        }

        if(url.contains("tagesschau.de")){
            cssQuery = TAGESSCHAU_CSS_QUERY;
        }else if(url.contains("zdfheute.de")){
            cssQuery = ZDF_HEUTE_CSS_QUERY;
        }else if(url.contains("heise.de")){
            try {
                final URI newUri = heiseUrl(url);

                url = newUri.toString();

                cssQuery = HEISE_CSS_QUERY;

            } catch (URISyntaxException e) {
                Log.warn("Wrong URL format", e);
                return null;
            }


        }else if(url.contains("baden-wuerttemberg.de")){
            cssQuery = BW_CSS_QUERY;
        }else if(url.contains("swr.de")){
            cssQuery = SWR_CSS_QUERY;
        }else if(url.contains("deutschlandfunk.de")){
            cssQuery = DEUTSCHLANDFUNK_CSS_QUERY;
        }else{
            return null;
        }


        try {
            doc = Jsoup.connect(url).get();
            LOG.debug(doc.title());
            Elements article = doc.select(cssQuery);
            StringJoiner sj = new StringJoiner("\n");

            for (Element element : article) {
                sj.add(element.text());
            }


            LOG.debug(sj);

            if(!sj.toString().trim().isEmpty()){
                return sj.toString();
            }


        } catch (IOException e) {
            Log.warn("", e);
        }

        return null;

    }

    private static @NotNull URI heiseUrl(final String url) throws URISyntaxException {
        final URI oldURL = new URI(url);
        String existingQuery = oldURL.getQuery();

        String newQuery;
        if (existingQuery == null || existingQuery.isEmpty()) {
            newQuery = "view=print";
        } else {
            // Fall 2: Es gibt bereits Query-Parameter.
            // Der neue Parameter wird mit einem '&' angehängt.
            newQuery = existingQuery + "&" + "view=print";
        }

        // 3. Erzeuge eine neue URI mit den alten Teilen und dem neuen Query-String.
        URI newUri = new URI(
                oldURL.getScheme(),
                oldURL.getAuthority(),
                oldURL.getPath(),
                newQuery,
                oldURL.getFragment()
        );
        return newUri;
    }

}
