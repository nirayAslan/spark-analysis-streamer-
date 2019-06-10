package com.logcreator.producer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/**
 * 2 saniye'de bir rastgele bir şehirden log üretip konsola basan, dosyaya yazan
 * ve Kafka'ya ileten basit bir spring boot uygulaması.(Log4j <3)
 * @author Niray Aslan
 */
@SpringBootApplication
public class Application implements ApplicationRunner {

    private static final Logger logger = LogManager.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        while (true) {
            Thread.sleep(2000);//2 saniye bekletiyoruz.
            String cityName = CityGenerator.getCityName();//Rastgele bir şehir aldık.
            logger.info("{} {}", cityName, "Hello-From-"+cityName);
        }
    }

}
