package uk.ncl.CSC8016.jackbergus.coursework.project2.processes;

import uk.ncl.CSC8016.jackbergus.coursework.project2.utils.Item;

import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.locks.*;

/**
 * This class tracks the products which are available and withdrawn
 * integration of concurrency mechanisms in ProductMonitor
 */

/*
    They can still be removed from at any index in a LinkedList, queue is just the facilitating data structure in Java
    Also concurrency and efficiency
    Note that these data structures (available and withdrawn) are accessed concurrently by various methods in RainforestShop
    TODO: how will I ensure that these are thread safe?
    TODO: Seeing as RainforestShop makes use of these in order to run, I think I need to implement re-entrant locks
            on all bits of critical code in this class
    TODO: Do I need to do anything to the data structures?
            Answer - likely not, because locking the methods should make the data structures thread safe
 */
public class ProductMonitor {
    Queue<Item> available;
    Queue<Item> withdrawn;
    Lock availableWithdrawnLock = new ReentrantLock();

    /**
     * Constructor
     */
    public ProductMonitor() {
        available = new LinkedList<>(); //a LinkedList of available items (on the shelf)
        withdrawn = new LinkedList<>(); //a LinkedList of withdrawn items (taken off the shelf)
    }

    /**
     * when reshelved, remove from withdrawn, add to available again
     * @param cls
     */
    public void removeItemsFromUnavailability(Collection<Item> cls) {
        availableWithdrawnLock.lock();
        try {
            for (Item x : cls) {
                if (withdrawn.remove(x))
                    available.add(x);
            }
        } finally {
            availableWithdrawnLock.unlock();
        }
    }

    /**
     * if an item is available, remove it from the available LinkedList and add it to the withdrawn LinkedList
     * @return
     */
    public Optional<Item> getAvailableItem() {
        Optional<Item> o = Optional.empty();
        availableWithdrawnLock.lock();
        try {
            if (!available.isEmpty()) {
                var obj = available.remove();
                if (obj != null) {
                    o = Optional.of(obj);
                    withdrawn.add(o.get());
                }
            }
        } finally {
            availableWithdrawnLock.unlock();
        }
        return o;
    }

    /**
     * reshelf item
     * remove item from withdrawn LinkedList
     * add it back to available LinkedList
     * @param u
     * @return
     */
    public boolean doShelf(Item u) {
        boolean result = false;
        availableWithdrawnLock.lock();
        try {
            if (withdrawn.remove(u)) {
                available.add(u);
                result = true;
            }
        } finally {
            availableWithdrawnLock.unlock();
        }
        return result;
    }

    /**
     * get all the available Items in the available LinkedList
     * @return
     */
    public Set<String> getAvailableItems() {
        Set<String> s;
        availableWithdrawnLock.lock();
        try {
            s = available.stream().map(x -> x.productName).collect(Collectors.toSet());
        } finally {
            availableWithdrawnLock.unlock();
        }
        return s;
    }

    /**
     * add an item into the available LinkedList
     * @param x
     */
    public void addAvailableProduct(Item x) {
        availableWithdrawnLock.lock();
        try {
            available.add(x);
        } finally {
            availableWithdrawnLock.unlock();
        }
    }

    /**
     * Calculates the total cost
     * checks each item in toIterate against items in withdrawn list
     * if withdrawn contains that item, add item to currentlyPurchaseable, otherwise add item to currentlyUnavailable
     * @param aDouble
     * @param toIterate list of items to attempt to purchase (toIterate)
     * @param currentlyPurchasable list of purchase-able items (currentlyPurchasable)
     * @param currentlyUnavailable list of unavailable items (currently Unavailable)
     * @return
     */
    public double updatePurchase(Double aDouble,
                                 List<Item> toIterate,
                                 List<Item> currentlyPurchasable,
                                 List<Item> currentlyUnavailable) {
        double total_cost = 0.0;
        availableWithdrawnLock.lock();
        try {
            for (var x : toIterate) {
                if (withdrawn.contains(x)) {
                    currentlyPurchasable.add(x);
                    total_cost += aDouble;
                } else {
                    currentlyUnavailable.add(x);
                }
            }
        } finally {
            availableWithdrawnLock.unlock();
        }
        return total_cost;
    }

    /**
     * make a list of items available
     * remove each item from withdrawn (if it's in that list)
     * add it to the list of available items
     * @param toIterate
     */
    public void makeAvailable(List<Item> toIterate) {
        availableWithdrawnLock.lock();
        try {
            for (var x : toIterate) {
                if (withdrawn.remove(x)) {
                    available.add(x);
                }
            }
        } finally {
            availableWithdrawnLock.unlock();
        }
    }

    /**
     * remove items in toIterate list from both withdrawn and available lists
     * Item does not exist in the Product Monitor anymore
     * @param toIterate
     * @return
     */
    public boolean completelyRemove(List<Item> toIterate) {
        boolean allEmpty;
        availableWithdrawnLock.lock();
        try {
            for (var x : toIterate) {
                withdrawn.remove(x);
                available.remove(x);
            }
            allEmpty = withdrawn.isEmpty() && available.isEmpty();
        } finally {
            availableWithdrawnLock.unlock();
        }
        return allEmpty;
    }
}
