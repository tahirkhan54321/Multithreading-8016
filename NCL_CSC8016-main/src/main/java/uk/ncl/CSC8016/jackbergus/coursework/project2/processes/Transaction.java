package uk.ncl.CSC8016.jackbergus.coursework.project2.processes;

import uk.ncl.CSC8016.jackbergus.coursework.project2.utils.BasketResult;
import uk.ncl.CSC8016.jackbergus.coursework.project2.utils.Item;

import java.util.*;

/*
The transaction class makes use of the Rainforest class, not the other way around.
So RainforestShop methods facilitate transactions.
Transactions make use of RainforestShop methods to complete their transactions.
 */

public class Transaction {
    private RainforestShop s;
    private String username;
    private UUID uuid;
    private LinkedList<Item> basket;

    Transaction(RainforestShop s, String username, UUID uuid) {
        this.s = s;
        this.username = username;
        this.uuid = uuid;
        basket = new LinkedList<>();
    }

    public String getUsername() {
        return username;
    }

    public UUID getUuid() {
        return uuid;
    }

    RainforestShop getSelf() {
        return s;
    }

    /**
     * Lists all the available items in the current rainforestShop
     * @return
     */
    public List<String> getAvailableItems()  {
        if (s == null || (uuid == null)) return Collections.emptyList();
        return s.getAvailableItems(this);
    }

    /**
     * This will call the logout method of the RainforestShop class, logic needs to be implemented there
     * @return
     */
    public boolean logout() {
        if (s == null || (uuid == null)) return false;
        return s.logout(this);
    }

    /**
     * This will basket the product, logic needs to be implemented in RainforestShop class
     * @param name the name of the product
     * @return boolean if successfully added
     */
    public boolean basketProduct(String name) {
        if (s == null || (uuid == null)) return false;
        Optional<Item> item = s.basketProductByName(this, name);
        item.ifPresent(basket::add);
        return item.isPresent();
    }

    /**
     * if it exists, for every item in the basket (immutable LinkedList), add these to an arraylist and return
     * @return
     */
    public List<Item> getUnmutableBasket() {
        if (s == null || (uuid == null)) return Collections.emptyList();
        List<Item> elements = new ArrayList<>();
        for (var x : basket)
            elements.add(x);
        return elements;
    }

    /**
     * shelf a product, logic needs to be implemented in rainforestShop
     * @param name
     * @return
     */
    public boolean shelfProduct(Item name) {
        if (s == null || (uuid == null)) return false;
        boolean result = s.shelfProduct(this, name);
        if (result) basket.remove(name);
        return result;
    }

    /**
     * check out, logic needs to be implemented in rainforestShop
     * @param total_available_money
     * @return
     */
    public BasketResult basketCheckout(double total_available_money) {
        if (s == null || (uuid == null)) return null;
        return s.basketCheckout(this, total_available_money);
    }

    /**
     * clears linkedlist
     */
    void clearBasket() {
        basket.clear();
    }

    /**
     * nullifies references to the RainforestShop object
     */
    void invalidateTransaction() {
        s = null;
        username = null;
        uuid = null;
        basket.clear();
    }

}
