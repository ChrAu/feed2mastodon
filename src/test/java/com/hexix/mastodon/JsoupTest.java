package com.hexix.mastodon;

import com.hexix.JsoupParser;
import io.quarkus.test.junit.QuarkusTest;
import org.jboss.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.StringJoiner;

@QuarkusTest
public class JsoupTest {

    final Logger LOG = Logger.getLogger(this.getClass());
    private final static String WIKIPEDIA_CSS_QUERY = "#mw-content-text .mw-parser-output > p:not(:has(> b)):not(:has(> span.geo.noexcerpt))";
    private final static String TAGESSCHAU_CSS_QUERY ="#content article > *:not(div.meldungsfooter)";

    @Test
    public void jsoupWikipedia() throws IOException {
        Document doc = Jsoup.connect("https://de.wikipedia.org/wiki/Europ%C3%A4ische_Union").get();
        LOG.info(doc.title());
        Elements article = doc.select(WIKIPEDIA_CSS_QUERY);

        LOG.debug(article.text());

        Assertions.assertTrue(article.text().contains("Parlament"));
    }


    @Test
    public void jsoupTagesschau() throws IOException {
        Document doc = Jsoup.connect("https://www.tagesschau.de/wissen/gesundheit/prostata-krebs-vorsorge-100.html").get();
        LOG.info(doc.title());
        Elements article = doc.select(TAGESSCHAU_CSS_QUERY);
        StringJoiner sj = new StringJoiner("\n");

        for (Element element : article) {
            sj.add(element.text());
        }


        LOG.debug(sj);

        Assertions.assertTrue(sj.toString().contains("Krebs"));
    }

    @Test
    public void zdfheute(){
        final String article = JsoupParser.getArticle("https://www.zdfheute.de/politik/ausland/dobrindt-eu-asylpolitik-migration-100.html");

        LOG.info(article);

        Assertions.assertTrue(article.contains("Gemeinsam mit fünf Amtskollegen aus anderen EU-Ländern will Bundesinnenminister"));
        Assertions.assertTrue(article.contains("Die wichtigsten Routen für irreguläre Migranten führen über das östliche und zentrale"));

        Assertions.assertTrue(article.contains("Der innenpolitische Sprecher der Grünen-Fraktion, Marcel Emmerich, meint: \"Die Streichung des Verbindungselements ist ein herzloser Angriff auf Schutzsuchende, Familien und Kinder, die in Länder ohne jede persönliche Bindung abgeschoben werden sollen.\""));
    }

    @Test
    public void heise(){
        final String article = JsoupParser.getArticle("https://www.heise.de/news/Oracle-309-Sicherheitsupdates-fuer-alle-moeglichen-Produkte-10490492.html");

        LOG.info(article);

        Assertions.assertTrue(article.contains("Oracle hat in der Nacht zum Mittwoch seinen quartalsweise stattfindenden \"Critical Patch Update\" genannten Patchday begangen. Dabei hat das Unternehmen 309 Sicherheitspatches für Produkte quer durch sein Portfolio veröffentlicht."));
        Assertions.assertTrue(article.contains("Produkte Updates bereitstehen, die noch \"Premier Support\" erhalten oder in der erweiterten Support-Phase sind"));

    }
}
