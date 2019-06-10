package com.logcreator.producer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Rastgele Şehir İsmi Üretir
 *
 * @author Niray Aslan
 */
public class CityGenerator {

    public static String getCityName() {
        List<String> cities = new ArrayList<>();
        Random rand = new Random();
        
        cities.add("Istanbul");
        cities.add("Tokyo");
        cities.add("Moskow");
        cities.add("Beijing");
        cities.add("London");

        int choice = rand.nextInt(cities.size());
        return cities.get(choice);
    }

}
