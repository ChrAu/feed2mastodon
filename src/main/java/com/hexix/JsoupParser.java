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
import java.util.Set;
import java.util.StringJoiner;

public class JsoupParser {
    final static Logger LOG = Logger.getLogger(JsoupParser.class);
    private static final String SWR_CSS_QUERY = "main h1.headline, main .detail-body p.lead, main .bodytext h2, main .bodytext p, main .bodytext figure.quote";
    private static final String DEUTSCHLANDFUNK_CSS_QUERY = "article.b-article > header > *:not(.article-header-actions, .article-header-meta), .article-details-text, .article-details-title";
    private static final String WINFUTURE_CSS_QUERY = "#news_content strong.article-intro, #news_content h2, #news_content div.teaser_img_container + script + div.mb14 + h2 + br + br,#news_content div.teaser_img_container + script + div.mb14 + h2 + br + br + div.primis_widget + br, #news_content div.teaser_img_container + script + div.mb14 + h2 + br + br + div.primis_widget + br + br + h2, #news_content div.teaser_img_container + script + div.mb14 + h2 + br + br + div.primis_widget + br + br + h2 + br + br, #news_content div.teaser_img_container + script + div.mb14 + h2 + br + br + div.primis_widget + br + br + h2 + br + br + script + div.ws_contentAd300 + br + br, #news_content div.teaser_img_container + script + div.mb14 + h2 + br + br + div.primis_widget + br + br + h2 + br + br + script + div.ws_contentAd300 + br + br + h2, #news_content div.teaser_img_container + script + div.mb14 + h2 + br + br + div.primis_widget + br + br + h2 + br + br + script + div.ws_contentAd300 + br + br + h2 + br + br, #news_content div.summary_box, #news_content div.changelog_list, #news_content";
    private static final String NDR_CSS_QUERY = "article p, article h2";
    private static final String T_ONLINE_CSS_QUERY = "article div[data-testid=\"StreamLayout.Stream\"] p, article div[data-testid=\"StreamLayout.Stream\"] h3, article div[data-testid=\"StreamLayout.Stream\"] ul:not([data-testid=\"RelatedArticles.List\"]) li";
    private static final String DOMREPTOTAL_CSS_QUERY = "article .post-title, article .entry-inner p";
    private static final String NTV_CSS_QUERY = "article.article .article__text p, article.article .article__text h2";


    private JsoupParser(){

    }
    private final static String TAGESSCHAU_CSS_QUERY ="#content article > *:not(div.meldungsfooter)";
    private final static String ZDF_HEUTE_CSS_QUERY = "main > div";
    private final static String HEISE_CSS_QUERY = "article > *:not(p.printversion__back-to-article)";

    private final static String BW_CSS_QUERY="article > header, article > .article__body";

    private final static String T3N_CSS_QUERY = "div.c-entry > div > p:not(.tg-crosslinks), div.c-entry > p:not(.tg-crosslinks), div.c-entry h2";

    private final static String DEFAULT_CSS_QUERY = "article";

    private final static Set<String> blacklist = Set.of();


    public static String getArticle(String url){
        Document doc = null;
        String cssQuery;

        try {

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
        }else if(url.contains("t3n.de")){
            cssQuery = T3N_CSS_QUERY;
        }else if(url.contains("winfuture.de")){
            cssQuery = WINFUTURE_CSS_QUERY;
        }else if(url.contains("ndr.de")){
            cssQuery = NDR_CSS_QUERY;
        }else if(url.contains("t-online.de")){
            cssQuery = T_ONLINE_CSS_QUERY;
        }else if(url.contains("domreptotal.com")){
            cssQuery = DOMREPTOTAL_CSS_QUERY;
        }else if(url.contains("ntv.de") || url.contains("n-tv.de")){
            cssQuery = NTV_CSS_QUERY;
        }else{

            if(blacklist.stream().anyMatch(url::contains)){
                return null;
            }

            cssQuery = DEFAULT_CSS_QUERY;
        }



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


        } catch (Exception e) {
            Log.error("Fehler beim laden der URL: " + url, e);
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
