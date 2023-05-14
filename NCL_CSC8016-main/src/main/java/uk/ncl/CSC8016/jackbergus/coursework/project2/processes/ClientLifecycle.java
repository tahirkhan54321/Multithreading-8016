package uk.ncl.CSC8016.jackbergus.coursework.project2.processes;

import uk.ncl.CSC8016.jackbergus.coursework.project2.utils.BasketResult;
import uk.ncl.CSC8016.jackbergus.coursework.project2.utils.UtilityMethods;

import java.util.Random;

/**
 * This class is for the client lifecycle, i.e. how the client interacts with the rainforestShop class
 * login to RainforestShop
 * When a client logs in, a non-empty transaction is created with a unique transaction ID.
 * The availability of a product is confirmed after the purchase is attempted.
 * If a product is found to be unavailable, the shop supplier (SupplierLifecycle) can be notified.
 * The shop supplier can provide information about the next missing item (getNextMissingItem).
 * The shop supplier can refurbish the shop by creating a certain number of products of the same type (refurbishWithItems).
 * The refurbished products are placed on the shelf for future availability.
 *
 */

public class ClientLifecycle implements Runnable {

    private final String username; //client's username
    private final RainforestShop s; //links to rainforestShop session s
    private final int items_to_pick_up; //number of items client wants to pick up
    private final double total_available_money, shelfing_prob; //how much money client has, likelihood of shelving

    private final Random rng; //random number

    private boolean doCheckOut = true; //boolean to facilitate whether client wants to check out or not

    BasketResult l; //client's individual basket

    /**
     * constructor
     * @param username
     * @param s
     * @param items_to_pick_up
     * @param shelfing_prob
     * @param total_available_money
     * @param rng_seed
     */
    public ClientLifecycle(String username,
                           RainforestShop s,
                           int items_to_pick_up,
                           double shelfing_prob,
                           double total_available_money,
                           int rng_seed) {
        this.username = username;
        this.s = s;
        this.items_to_pick_up = items_to_pick_up;
        this.total_available_money = total_available_money;
        this.shelfing_prob = Double.min(1.0, Double.max(0.0, shelfing_prob));
        rng = new Random(rng_seed);
    }

    /**
     * creates a new thread for a client
     * @param doCheckOut
     * @return
     */
    public Thread thread(boolean doCheckOut) {
        this.doCheckOut = doCheckOut;
        return new Thread(this);
    }

    /**
     * gets the result of the basket
     * a string of what the user bought, availability or unavailability, total cost, etc
     * see BasketResult class
     * @return
     */
    public BasketResult getBasketResult() {
        return l;
    }

    /**
     * allows the client to start a new thread, waits for it to finish and then returns basket result
     * @param doCheckOut
     * @return
     * @throws InterruptedException
     */
    public BasketResult startJoinAndGetResult(boolean doCheckOut) throws InterruptedException {
        this.doCheckOut = doCheckOut;
        Thread t = new Thread(this);
        t.start(); t.join();
        return l;
    }

    /**
     * Represents client lifecycle
     * login, pick up items, shelve items, and perform checkout and payment based on
     * the provided parameters and probabilities.
     */
    @Override
    public void run() {
        l = null;
        double nextAfter = Math.nextUp(1.0);
        if (s != null) {
            s.login(username).ifPresent(transaction -> {
                for (int i = 0; i< items_to_pick_up; i++) {
                    var obj = UtilityMethods.getRandomElement(s.getAvailableItems(transaction), rng); //take in a list of items and return a random object from that list
                    if (obj == null) break;
                    if (transaction.basketProduct(obj)) {
                        if (rng.nextDouble(0.0, nextAfter) < shelfing_prob) {
                            if (!transaction.shelfProduct(UtilityMethods.getRandomElement(transaction.getUnmutableBasket(), rng)))
                                throw new RuntimeException("ERROR: I musth be able to shelf a product that I added!");
                        }
                    }
                }
                if (doCheckOut)
                    l = transaction.basketCheckout(total_available_money);
                else
                    l = null;
                transaction.logout();
            });
        }
    }
}
