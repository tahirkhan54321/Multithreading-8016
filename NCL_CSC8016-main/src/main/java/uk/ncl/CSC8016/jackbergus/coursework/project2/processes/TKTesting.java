package uk.ncl.CSC8016.jackbergus.coursework.project2.processes;

import uk.ncl.CSC8016.jackbergus.slides.semaphores.scheduler.Pair;

import java.sql.Array;
import java.util.*;

public class TKTesting {

    public static void main(String[] args) {

        /* initialise objects needed for threads
            RainforestShop
            ClientLifecycle
         */

        //RainforestShop
        Collection<String> client_ids = new ArrayList<String>();
        client_ids.add("client1");
        client_ids.add("client2");
        client_ids.add("client3");

        Map<String, Pair<Double, Integer>> available_products = new HashMap<String, Pair<Double, Integer>>();
        Pair<Double, Integer> pair1 = new Pair<>(3.14, 7);
        Pair<Double, Integer> pair2 = new Pair<>(2.71, 10);
        Pair<Double, Integer> pair3 = new Pair<>(1.61, 5);
        available_products.put("apple", pair1);
        available_products.put("banana", pair2);
        available_products.put("grapes", pair3);

        boolean isGlobalLock = false;

        RainforestShop rainforestShop = new RainforestShop(client_ids, available_products, isGlobalLock);

        //ClientLifecycle
        String username = "user1";
        int items_to_pick_up = 1;
        double shelfing_prob = 0.4;
        double total_available_money = 50.0;
        int rng_seed = 5;

        ClientLifecycle client1 = new ClientLifecycle(username, rainforestShop, items_to_pick_up, shelfing_prob,
                total_available_money, rng_seed);

        String username2 = "user2";
        int items_to_pick_up2 = 6;
        double shelfing_prob2 = 0.2;
        double total_available_money2 = 100.0;
        int rng_seed2 = 6;

        ClientLifecycle client2 = new ClientLifecycle(username2, rainforestShop, items_to_pick_up2, shelfing_prob2,
                total_available_money2, rng_seed2);

    }
}
