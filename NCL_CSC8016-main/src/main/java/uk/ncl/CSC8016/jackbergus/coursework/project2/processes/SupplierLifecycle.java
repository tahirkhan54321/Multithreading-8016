package uk.ncl.CSC8016.jackbergus.coursework.project2.processes;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * When shop supplier is notified by RainforestShop that items are missing, it will run its lifecycle
 * Essentially, this is restocking the shop with items that have been deemed missing by RainforestShop
 *
 *
 * Each time an allowed user logs in, a non-empty transaction will be created with an unique transaction ID.
 * The unavailability of the product shall be confirmed not while basketing the items, rather than after purchasing those.
 * At this stage, the shop supplier (SupplierLifecycle) might be notified that some products are missing (getNextMissingItem)
 * and refurbish the show with a non-zero amount of products of the same type (refurbishWithItems).
 * For simplicity sake, the refurbishWithItems method will be in charge of creating the number of desired product and
 * to place them on the shelf.
 */

public class SupplierLifecycle implements Runnable {

    private final RainforestShop s;

    private volatile boolean hasRetrievedOneProduct;
    private AtomicBoolean stopped;
    private final Random rng;

    /**
     * constructor
     * @param s
     */
    public SupplierLifecycle(RainforestShop s) {
        this.s = s;
        this.rng = new Random(0);
        hasRetrievedOneProduct = false;
        stopped = new AtomicBoolean(false);
    }

    /**
     * starts the supplier thread
     * @return
     */
    public Thread startThread() {
        stopped = new AtomicBoolean(false);
        var t = new Thread(this);
        t.start();
        return t;
    }

    /**
     * runs the supplier lifecycle
     */
    @Override
    public void run() {
        while (true) {
            String product = s.getNextMissingItem();
            if (product.equals("@stop!")) {
                s.supplierStopped(stopped);
                return;
            }
            hasRetrievedOneProduct = true;
            int howManyItems = this.rng.nextInt(1, 6);
            s.refurbishWithItems(howManyItems, product);
        }
    }

    /**
     * returns if a product has been produced according to the supplier lifecycle run
     * @return
     */
    public boolean hasAProductBeenProduced() {
        return hasRetrievedOneProduct;
    }

    /**
     * has it been stopped?
     * @return
     */
    public boolean isStopped() {
        return stopped.get();
    }
}
