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

        LOG.debug(article);

        Assertions.assertTrue(article.contains("Gemeinsam mit fünf Amtskollegen aus anderen EU-Ländern will Bundesinnenminister"));
        Assertions.assertTrue(article.contains("Die wichtigsten Routen für irreguläre Migranten führen über das östliche und zentrale"));

        Assertions.assertTrue(article.contains("Der innenpolitische Sprecher der Grünen-Fraktion, Marcel Emmerich, meint: \"Die Streichung des Verbindungselements ist ein herzloser Angriff auf Schutzsuchende, Familien und Kinder, die in Länder ohne jede persönliche Bindung abgeschoben werden sollen.\""));
    }

    @Test
    public void heise(){
        final String article = JsoupParser.getArticle("https://www.heise.de/news/Oracle-309-Sicherheitsupdates-fuer-alle-moeglichen-Produkte-10490492.html");

        LOG.debug(article);

        Assertions.assertTrue(article.contains("Oracle hat in der Nacht zum Mittwoch seinen quartalsweise stattfindenden \"Critical Patch Update\" genannten Patchday begangen. Dabei hat das Unternehmen 309 Sicherheitspatches für Produkte quer durch sein Portfolio veröffentlicht."));
        Assertions.assertTrue(article.contains("Produkte Updates bereitstehen, die noch \"Premier Support\" erhalten oder in der erweiterten Support-Phase sind"));

    }

    @Test
    public void bw(){
        final String article = JsoupParser.getArticle("https://www.baden-wuerttemberg.de/de/service/presse/pressemitteilung/pid/land-startet-neues-informationssystem-zum-waldbrandmanagement");
        LOG.info(article);



    }

    @Test
    public void swr(){
        final String article = JsoupParser.getArticle("https://www.swr.de/swrkultur/wissen/warum-schmeckt-aufgewaermtes-essen-besser-100.html");

        LOG.debug(article);


        Assertions.assertTrue(article.contains("sich das Kollagen im Fleisch weiter und der Braten wird umso zarter."));

    }

    @Test
    public void noContent(){
        final  String article = JsoupParser.getArticle("https://peertube.heise.de/w/mmhBXfZVAMFQigoaV5KWMu");

        Assertions.assertNull(article);
    }

    @Test
    public void deutschlandfunk(){
        final String article = JsoupParser.getArticle("https://www.deutschlandfunk.de/elektronische-patientenakte-vorteile-nachteile-kritik-widerspruch-100.html");

        Assertions.assertTrue(article.contains("wird als Opt-out-Verfahren bezeichnet. Alternativ können bestimmte Befunde und Labor"));

        final String article2 = JsoupParser.getArticle("https://www.deutschlandfunk.de/who-warnt-vor-risiko-einer-weltweiten-chikungunya-epidemie-uebertragung-durch-stechmuecken-112.html");

        Assertions.assertTrue(article2.contains("Chikungunya ist eine durch Stechmücken übertragene Virusinfektion, die vor allem in tropischen und subtropischen Regionen vorkommt. Durch den Klimawandel breiten sich die das Virus übertragenden Mücken aber zunehmend aus. Die Erkrankung verursacht hohes Fieber und starke Gelenkschmerzen. Schwere Verläufe sind selten und treten insbesondere bei älteren oder vorerkrankten Menschen auf."));

    }

    @Test
    public void t3n(){
        final String article = JsoupParser.getArticle("https://t3n.de/news/chatgpt-ecommerce-shopping-1698203/");

        Assertions.assertTrue(article.contains("PT-interne Lösung abgewickelt. Inwieweit hier sämtliche Payment-Service-Provider eingebunden werden (die ja händlerseitig bereits existieren) o"));
    }

    @Test
    public void winfuture(){
        final String article = JsoupParser.getArticle("https://winfuture.de/news,152477.html");

        Assertions.assertTrue(article.contains("ch vor ungewollten Upgrades zu schützen - eine häufig empfohlene 'Schutzmaßnahme' für Anwender, die nicht auf Windows 11 wechseln wollen. Dass Microsoft seine Anforderungen ge"));
    }

}
