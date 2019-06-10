package com.example.spark.streamer;

import static com.datastax.spark.connector.japi.CassandraJavaUtil.*;
import static com.datastax.spark.connector.japi.CassandraJavaUtil.mapToRow;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.*;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import scala.Tuple2;

/**
 * Spark ile Kafka sunucusunu dinleyip, buradan gelen logları analiz ederek,
 * dashboard'da yayınlanmak üzere tekrar kafka'nın başka bir topiğine ileten ve
 * eş zamanlı olarak Cassandra veritabanına kaydeden küçük bir uygulama.
 *
 * @author Niray Aslan
 */
public class Application {

    private static final Pattern LINE = Pattern.compile("\\r?\\n");

    public static void main(String[] args) throws Exception {

        Cluster cluster;
        Session session;
        cluster = Cluster.builder().addContactPoint("localhost").build();
        session = cluster.connect();
        session.execute("CREATE KEYSPACE IF NOT EXISTS  teb WITH replication = {"
                + " 'class': 'SimpleStrategy', "
                + " 'replication_factor': '1' "
                + "};");
        session.execute("CREATE table if not exists teb.logdetails ("
                + "time_stamp text primary key,"
                + "city text,"
                + "level text,"
                + "message text);");
        SparkConf sparkConf = new SparkConf().setAppName("SparkStreamerApp");
        //Cassandra'ya bağlanmak için gerekli ayarları burada yapıyoruz.
        sparkConf.set("spark.cassandra.connection.host", "localhost");
        sparkConf.set("spark.cassandra.connection.port", "9042");
        sparkConf.setMaster("local[2]");
        //60 saniye'de bir analiz sonuçlarını stream edeceğiz
        JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, Durations.seconds(60));
        /**
         * Not: 2 tane topic oluşturduk kafka'da unrefined: Logların analiz
         * edilmemiş ham hali burada yayınlanıyor. refined: Logların analiz
         * edilmiş ve json şekline dönüştürülmüş hali burada yayınlanıyor.
         *
         */
        //Kafka'dan yayın alabilmek için gerekli ayarlar
        Set<String> topicsSet = new HashSet<>(Arrays.asList("unrefined"));
        Map<String, Object> kafkaParams = new HashMap<>();
        kafkaParams.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        kafkaParams.put(ConsumerConfig.GROUP_ID_CONFIG, "group1");
        kafkaParams.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        kafkaParams.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        //Kafka'ya analiz ettiğimiz verileri producer rolünde yayınlamak için bilgiler
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("acks", "1");
        props.put("retries", "3");
        props.put("linger.ms", 5);
        JavaInputDStream<ConsumerRecord<String, String>> messages = KafkaUtils.createDirectStream(
                jssc,
                LocationStrategies.PreferConsistent(),
                ConsumerStrategies.Subscribe(topicsSet, kafkaParams));
        // Satırları alıp, satırlarda gelen şehirlerin sayısını buluyoruz.
        //Dashboard'a stream edilcek veriler burada işlenir.
        JavaDStream<String> lines = messages.map(ConsumerRecord::value);
        JavaDStream<String> words = lines.flatMap(x -> Arrays.asList(LINE.split(x)).iterator());
        JavaPairDStream<String, Integer> wordCounts = words.mapToPair(
                new PairFunction<String, String, Integer>() {
            @Override
            public Tuple2<String, Integer> call(String word) throws Exception {
                //contains('x') == word değişkenin içerisinde "X" kelimesi var mı?
                if (word.contains("Istanbul")) {
                    return new Tuple2<>("Istanbul", 1);
                } else if (word.contains("Tokyo")) {
                    return new Tuple2<>("Tokyo", 1);
                } else if (word.contains("Moskow")) {
                    return new Tuple2<>("Moskow", 1);
                } else if (word.contains("Beijing")) {
                    return new Tuple2<>("Beijing", 1);
                } else if (word.contains("London")) {
                    return new Tuple2<>("London", 1);
                } else {
                    return new Tuple2<>("other logs", 1);
                }
            }
        }
        ).reduceByKey((i1, i2) -> i1 + i2);
        //Tüm satırları alıp cassandra'ya kaydetmek için böyle bir yol izledim
        //Yaptığı şey,tüm satırları al ve (satır,tekrarsayısı) şeklinde aktar.
        //Hile yapılan nokta her satır bir kere tekrar edilecek nasıl olsa timeStamp'ten dolayı
        //FIXME: daha güzel bir yöntemi olmalı, burada tekrar sayısına ihtiyacımız yok ki!
        JavaPairDStream<String, Integer> logDetailCounts = words.mapToPair(
                new PairFunction<String, String, Integer>() {
            @Override
            public Tuple2<String, Integer> call(String word) throws Exception {

                return new Tuple2<>(word, 1);
            }
        }
        ).reduceByKey((i1, i2) -> i1 + i2);
        //Burada bir iterasyon oluşturup gelen tüm analiz edilmiş verileri kafka'da refined
        //topigine iletiyoruz.
        wordCounts.foreachRDD(new VoidFunction<JavaPairRDD<String, Integer>>() {
            @Override
            public void call(JavaPairRDD<String, Integer> arg0) throws Exception {
                Map<String, Integer> wordCountMap = arg0.collectAsMap();
                //FIXME:Json şeklinde iletmek bir hile yapmak gerekti bunu düzeltebilirim.
                String json = "[{\"city\":" + "\"" + "starting" + "\"" + "," + "\"count\":" + 1 + "}";
                for (String key : wordCountMap.keySet()) {
                    json += ",{\"city\":" + "\"" + key + "\"" + "," + "\"count\":" + wordCountMap.get(key) + "}";
                }
                json += "]";
                publishToKafka(json, "refined", props);
            }
        });
        //Burada yine bir iterasyon gelen analiz edilmemiş ham verileri de split()
        //methodu ile ayırarak Cassandra'ya kaydediyoruz.
        logDetailCounts.foreachRDD(new VoidFunction<JavaPairRDD<String, Integer>>() {
            @Override
            public void call(JavaPairRDD<String, Integer> arg0) throws Exception {
                Map<String, Integer> wordCountMap = arg0.collectAsMap();
                List<LogDetail> products = new ArrayList<>();
                for (String key : wordCountMap.keySet()) {
                    String[] wordList = key.split(" ");
                    products.add(LogDetail.newInstance(wordList[0] + wordList[1], wordList[2],
                            wordList[3], wordList[4]));
                }
                JavaRDD<LogDetail> productsRDD = jssc.sparkContext().parallelize(products);
                //"teb" = veritabanı ismimiz(Cassandra'da KeySpace deniliyor)
                //"logdetails" = tablomuz(Cassandra'da Columnt Family deniliyor)
                javaFunctions(productsRDD).writerBuilder("teb", "logdetails", mapToRow(LogDetail.class)).saveToCassandra();
            }
        });
        wordCounts.print();
        jssc.start();
        jssc.awaitTermination();
    }

    /**
     * Kafka'da Producer olarak iletim yapmaya yardımcı method
     *
     * @param json = Analiz Edilmiş Veri
     * @param topic = Hangi topic'e yazacağım(Topic ismi)
     * @param props = Main methodunda tanımladığım properties'ler(Producer için)
     */
    public static void publishToKafka(String json, String topic, Properties props) {
        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(props);

        try {
            producer.send(new ProducerRecord<String, String>(topic, json));
            System.out.println("Event : " + json + " published successfully to kafka!!");
        } catch (Exception e) {
            System.out.println("Problem while publishing the event to kafka : " + e.getMessage());
        }
        producer.close();
    }

    /**
     * Static bir model class'ı oluşturdum. Cassandra'ya anlamlı veri modelleri
     * kaydedebilmek için
     */
    public static class LogDetail implements Serializable {

        private String timeStamp;
        private String level;
        private String city;
        private String message;

        public static LogDetail newInstance(String timeStamp, String level, String city, String message) {
            LogDetail detail = new LogDetail();
            detail.setTimeStamp(timeStamp);
            detail.setLevel(level);
            detail.setCity(city);
            detail.setMessage(message);
            return detail;
        }

        public String getTimeStamp() {
            return timeStamp;
        }

        public void setTimeStamp(String timeStamp) {
            this.timeStamp = timeStamp;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

    }
}
