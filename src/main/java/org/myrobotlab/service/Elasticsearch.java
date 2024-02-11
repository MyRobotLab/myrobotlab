package org.myrobotlab.service;

import java.io.IOException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.ElasticsearchConfig;
import org.slf4j.Logger;

import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;
import pl.allegro.tech.embeddedelasticsearch.PopularProperties;

public class Elasticsearch extends Service<ElasticsearchConfig>
{

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Elasticsearch.class);

  transient EmbeddedElastic elastic = null;

  public Elasticsearch(String n, String id) {
    super(n, id);
  }

  public void install() throws IOException, InterruptedException {
    install("6.4.2");
  }

  public void install(String version) throws IOException, InterruptedException {
    // version[5.0.0]
    // [z3SpE04] version[5.0.0], pid[19688], <= can i get process handle ?

    // mkdirs Elasticsearch/elastic/<- mabye not name as its a server port
    // (single instance)

    elastic = EmbeddedElastic.builder().withElasticVersion(version)
        .withSetting(PopularProperties.TRANSPORT_TCP_PORT, 9350)
        .withSetting(PopularProperties.CLUSTER_NAME, "mrl_cluster")
        // .withPlugin("analysis-stempel")
        .withIndex("index")
        /*
         * .withIndex("cars", IndexSettings.builder() .withType("car",
         * getSystemResourceAsStream("car-mapping.json")) .build())
         * 
         * .withIndex("books", IndexSettings.builder()
         * .withType(PAPER_BOOK_INDEX_TYPE,
         * getSystemResourceAsStream("paper-book-mapping.json"))
         * .withType("audio_book",
         * getSystemResourceAsStream("audio-book-mapping.json"))
         * .withSettings(getSystemResourceAsStream("elastic-settings.json"))
         * .build())
         */
        .build().start();

    elastic.createIndex("test");
  }

  public void release() {
    // stops server, destroys index, removes directories
    elastic.stop();
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Elasticsearch elastic = (Elasticsearch) Runtime.start("elastic", "Elasticsearch");
      elastic.install();
      // Runtime.start("servo", "Servo");
      // Runtime.start("gui", "SwingGui");
      elastic.release();

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}

// yum yum ...