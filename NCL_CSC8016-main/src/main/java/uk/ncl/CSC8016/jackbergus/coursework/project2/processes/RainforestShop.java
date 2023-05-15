package uk.ncl.CSC8016.jackbergus.coursework.project2.processes;

import uk.ncl.CSC8016.jackbergus.coursework.project2.utils.BasketResult;
import uk.ncl.CSC8016.jackbergus.coursework.project2.utils.Item;
import uk.ncl.CSC8016.jackbergus.coursework.project2.utils.MyUUID;
import uk.ncl.CSC8016.jackbergus.slides.semaphores.scheduler.Pair;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.concurrent.locks.*;


public class RainforestShop {

    /// For correctly implementing the server, please consider that

    //LOCKS
    Lock loginLock = new ReentrantLock();

    private final boolean isGlobalLock;
    //added volatile to supplierStopped variable as it is accessed/modified by supplier stopping methods
    // supplierStopped is only accessed within RainforestShop
    private volatile boolean supplierStopped;
    private Set<String> allowed_clients; //usernames of clients, see login method
    public HashMap<UUID, String> UUID_to_user;

    //represents a hashmap of the availability or withdrawability of Items in the Product Monitor objects
    //string is item name, ProductMonitor is keeping track of the availability of that product in that queue of items
    //Product monitor keeps a linkedlist of available items or withdrawn items
    //An available item is on the shelf
    //A withdrawn item is off the shelf (can be re-shelved)
    private volatile HashMap<String, ProductMonitor> available_withdrawn_products;
    private HashMap<String, Double> productWithCost = new HashMap<>(); //the product name and its associated cost
    private volatile Queue<String> currentEmptyItem;


    public boolean isGlobalLock() {
        return isGlobalLock;
    }

    /**
     * Please replace this string with your student ID, so to ease the marking process
     *
     * @return Your student id!
     */
    public String studentId() {
        return "220613983";
    }


    /**
     * @param client_ids         Collection of registered client names that can set up the communication
     * @param available_products Map associating each product name to its cost and the initial number of available items on the shop
     * @param isGlobalLock       Might be used (but not strictly required) To remark whether your solution uses a
     *                           pessimistic transaction (isGlobalLock=true) or an optimistic one (isGlobalLock=false)
     */
    public RainforestShop(Collection<String> client_ids,
                          Map<String, Pair<Double, Integer>> available_products,
                          boolean isGlobalLock) {
        supplierStopped = true;
        currentEmptyItem = new LinkedBlockingQueue<>();
        this.isGlobalLock = isGlobalLock;
        allowed_clients = new HashSet<>();
        if (client_ids != null) allowed_clients.addAll(client_ids);
        this.available_withdrawn_products = new HashMap<>();
        UUID_to_user = new HashMap<>();
        // If the available_products parameter is not null, iterate over its entries using a for-each loop.
        // Each entry represents a product and its corresponding cost and quantity.
        if (available_products != null) {
            for (var x : available_products.entrySet()) {
                // If the key of the current entry is equal to "@stop!", the loop continues to the next iteration.
                if (x.getKey().equals("@stop!")) continue;
                //add each available 'product key' and 'key value of pair' to the productWithCost Hashmap
                productWithCost.put(x.getKey(), x.getValue().key);
                var p = new ProductMonitor();
                for (int i = 0; i < x.getValue().value; i++) {
                    // Creates a new instance of Item with the product key, cost, and a generated UUID.
                    // It adds the item to the ProductMonitor p using the addAvailableProduct method.
                    p.addAvailableProduct(new Item(x.getKey(), x.getValue().key, MyUUID.next()));
                }
                //put the Item's key and the product monitor object into the available_withdrawn_products hashmap
                this.available_withdrawn_products.put(x.getKey(), p);
            }
        }
    }

    /**
     * Performing a user log-in. To generate a transaction ID, please use the customary Java method
     * <p>
     * UUID uuid = UUID.randomUUID();
     *
     * @param username Username that wants to login
     * @return A non-empty transaction if the user is logged in for the first time, and he hasn't other instances of itself running at the same time
     * In all the other cases, thus including the ones where the user is not registered, this returns an empty transaction
     */
    //Optional definition: A container object which may or may not contain a non-null value.
    //If a value is present, isPresent() will return true and get() will return the value. (MDN)
    public Optional<Transaction> login(String username) {
        Optional<Transaction> result = Optional.empty();
        //CRITICAL SECTION - starts here due to read of allowed_clients and write of UUID_to_user
        //Note that I tried read/write locks but got a deadlock
        //I think I need to make UUID_to_user thread safe data structures
        loginLock.lock();
        try {
            if (allowed_clients.contains(username)) {
                //if the username is on the allowed clients list, generate a UUID, put into UUID_to_user hashmap
                UUID uuid = UUID.randomUUID();
                UUID_to_user.put(uuid, username);
                //valid Transaction object has been created and is available.
                result = Optional.of(new Transaction(this, username, uuid));
            }
        } finally {
            loginLock.unlock();
        }
        return result;
    }

    /**
     * This method should be accessible only to the transaction and not to the public!
     * Logs out the client iff. there was a transaction that was started with a given UUID and that was associated to
     * a given user
     *
     * @param transaction
     * @return false if the transaction is null or whether that was not created by the system
     */
    boolean logout(Transaction transaction) {
        /* The algorithm should do the following:
            1. (DONE) verify the transaction's UUID and username against UUID_to_user
            2. (N/A) perhaps verify transaction's rainforestShop matches the current instance? /NO, one single rainforest shop
                2.1. (DONE) verify that the user is currently logged in, cannot logout if not (return false?),
                2.2. (DONE) cannot logout multiple times on same transaction (UUID is not in UUID_to_user, return false?)
            3. (DONE) for every Item in the transaction's basket, loop through and re-shelf using ProductMonitor's doShelf
            4. (DONE) clear basket from transaction class
            5. (DONE) invalidate method from transaction class
            6. (N/A) logout method from transaction class //NO, this will catch up in the recursive call
                (See transaction logout, s.logout).
            Needs to meet 1-5, not including 2 to return true.
            TEST: Passed (after fixing not-null)
         */
        boolean result = false;
        // TODO: Implement the remaining part!
        loginLock.lock();
        try {
            if (!allowed_clients.contains(transaction.getUsername()) ||
                    !UUID_to_user.containsKey(transaction.getUuid())) {
                return result;
            }
        } finally {
            loginLock.unlock();
        }
        for (Item item : transaction.getUnmutableBasket()) {
            ProductMonitor pm = available_withdrawn_products.get(item.productName);
            if (pm != null) {
                pm.doShelf(item);
            }
        }
        transaction.invalidateTransaction();
        result = true;
        return result;
    }

    /**
     * Lists all of the items that were not basketed and that are still on the shelf
     *
     * @param transaction
     * @return
     */
    List<String> getAvailableItems(Transaction transaction) {
        /* Note that I think this is items in available_withdrawn_products
           I believe the algorithm should do the following:
            1. (DONE) verify the transaction's UUID and username against UUID_to_user
            2. (DONE) verify that the user is logged in
            3. (N/A) perhaps verify transaction's rainforestShop matches the current instance? /NO, one single rainforest shop
            4. (DONE) iterate over available_withdrawn_products (AWP)
            5. (DONE) for each entry in AWP, use getAvailableItems (Set<String>) method from PM
            6. (DONE) for each getAvailableItems, add them to the List
            TEST: Passed
         */
        List<String> ls = Collections.emptyList();
        // TODO: Implement the remaining part!
        loginLock.lock();
        try {
            if (!allowed_clients.contains(transaction.getUsername()) ||
                    !UUID_to_user.containsKey(transaction.getUuid())) {
                return ls;
            }
        } finally {
            loginLock.unlock();
            ls = new ArrayList<>();
        }
        for (Map.Entry<String, ProductMonitor> entry : available_withdrawn_products.entrySet()) {
            ls.addAll(entry.getValue().getAvailableItems());
        }
        return ls;
    }

    /**
     * If a product can be basketed from the shelf, then a specific instance of the product on the shelf is returned
     *
     * @param transaction User reference
     * @param name        Product name picked from the shelf
     * @return Whether the item to be basketed is available or not
     */
    Optional<Item> basketProductByName(Transaction transaction, String name) {
        AtomicReference<Optional<Item>> result = new AtomicReference<>(Optional.empty());
        if (transaction.getSelf() == null || (transaction.getUuid() == null)) return result.get();
        /*
        Note that the transaction validation has already been taken care of.
        Note that this is not actually adding to the basket
           I believe the algorithm should do the following:
            1. (DONE) look up the name key in AWP, extract its value PM
            2. (DONE) use getAvailableItem on that PM
            3. (DONE) assign/set this to result
            4. (DONE) Handle error for null coming from Testing i.e. the other result from getAvailableItem if not available
            TEST: Passed (after fixing not-null)
            Reference: the .set method usage for AtomicReference came from Stack Overflow:
            https://stackoverflow.com/questions/3964211/when-to-use-atomicreference-in-java
              Original Author - andersoj, Stack Overflow
              Modifying Author – Tahir Khan
         */
            // TODO: Implement the remaining part!
        else {
            ProductMonitor pm = available_withdrawn_products.get(name);
            if (pm != null) {
                result.set(pm.getAvailableItem());
            }
        }
        return result.get();
    }

    /**
     * If the current transaction has withdrawn one of the objects from the shelf and put it inside its basket,
     * then the transaction shall be also able to replace the object back where it was (on its shelf)
     *
     * @param transaction Transaction that basketed the object
     * @param object      Object to be reshelved
     * @return Returns true if the object existed before and if that was basketed by the current thread, returns false otherwise
     */
    boolean shelfProduct(Transaction transaction, Item object) {
        /* Note that the transaction validation has already been taken care of
            I believe the algo should do the following:
            1. (DONE) verify that the Item object is in the transaction's basket
            2. (DONE) if not in transaction's basket, return false
            3. (DONE) if in transaction's basket then doShelf on object
                3.1. (DONE) extract the product monitor for this item
                3.2. (DONE) if not null then doShelf on it
            4. (DONE) return true
            TEST: Passed
         */
        boolean result = false;
        if (transaction.getSelf() == null || (transaction.getUuid() == null)) return false;
            // TODO: Implement the remaining part!
        else {
            if (!transaction.getUnmutableBasket().contains(object)) {
                return false;
            } else {
                ProductMonitor pm = available_withdrawn_products.get(object.productName);
                if (pm != null) {
                    pm.doShelf(object);
                }
                result = true;
            }
        }
        return result;
    }

    /**
     * Stops the food supplier by sending a specific message. (assumption) Please observe that no product shall be named @stop!
     */
    public void stopSupplier() {
        /* The specific message it needs to send is "@stop!", this will stop the SupplierLifecycle run method
            currentEmptyItem is a volatile Queue of Strings
         */
        // TODO: Provide a correct concurrent implementation!
        // I have made currentEmptyItem volatile for thread safety
        currentEmptyItem.add("@stop!");
    }

    /**
     * The supplier acknowledges that it was stopped, and updates its internal state. The monitor also receives confirmation
     *
     * @param stopped Boolean variable from the supplier
     */
    public void supplierStopped(AtomicBoolean stopped) {
        // TODO: Provide a correct concurrent implementation!
        //supplierStopped is a volatile boolean so this should be thread safe
        supplierStopped = true;
        stopped.set(true);
    }

    /**
     * the shop supplier (SupplierLifecycle) might be notified that some products are missing (getNextMissingItem)
     * <p>
     * The supplier invokes this method when it needs to know that a new product shall be made ready available.
     * <p>
     * This method should be blocking (if currentEmptyItem is empty, then this should wait until currentEmptyItem
     * contains at least one element and, in that occasion, then returns the first element being available)
     *
     * @return
     */
    public String getNextMissingItem() {
        // TODO: Provide a correct concurrent implementation!
        //currentEmptyItem is volatile so this should be thread safe
        supplierStopped = false;
        while (currentEmptyItem.isEmpty()) {

        };
        return currentEmptyItem.remove();
    }


    /**
     * This method is invoked by the Supplier to refurbish the shop of n products of a given time (current item)
     *
     * @param n           Number of elements to be placed
     * @param currentItem Type of elements to be placed
     */
    public void refurbishWithItems(int n, String currentItem) {
        // Note: this part of the implementation is completely correct!
        Double cost = productWithCost.get(currentItem);
        if (cost == null) return;
        for (int i = 0; i < n; i++) {
            available_withdrawn_products.get(currentItem).addAvailableProduct(new Item(currentItem, cost, MyUUID.next()));
        }
    }

    /**
     * This operation purchases all the elements available on the basket
     *
     * @param transaction           Transaction containing the current withdrawn elements from the shelf (and therefore basketed)
     * @param total_available_money How much money can the client spend at maximum
     * @return
     */
    public BasketResult basketCheckout(Transaction transaction, double total_available_money) {
        // Note: this part of the implementation is completely correct!
        BasketResult result = null;
        if (UUID_to_user.getOrDefault(transaction.getUuid(), "").equals(transaction.getUsername())) {
            var b = transaction.getUnmutableBasket();
            double total_cost = (0.0);
            List<Item> currentlyPurchasable = new ArrayList<>();
            List<Item> currentlyUnavailable = new ArrayList<>();
            for (Map.Entry<String, List<Item>> entry : b.stream().collect(Collectors.groupingBy(x -> x.productName)).entrySet()) {
                String k = entry.getKey();
                List<Item> v = entry.getValue();
                total_cost += available_withdrawn_products.get(k).updatePurchase(productWithCost.get(k), v, currentlyPurchasable, currentlyUnavailable);
            }
            if ((total_cost > total_available_money)) {
                for (Map.Entry<String, List<Item>> entry : b.stream().collect(Collectors.groupingBy(x -> x.productName)).entrySet()) {
                    String k = entry.getKey();
                    List<Item> v = entry.getValue();
                    available_withdrawn_products.get(k).makeAvailable(v);
                }
                currentlyUnavailable.clear();
                currentlyPurchasable.clear();
                total_cost = (0.0);
            } else {
                Set<String> s = new HashSet<>();
                for (Map.Entry<String, List<Item>> entry : b.stream().collect(Collectors.groupingBy(x -> x.productName)).entrySet()) {
                    String k = entry.getKey();
                    List<Item> v = entry.getValue();
                    if (available_withdrawn_products.get(k).completelyRemove(v))
                        s.add(k);
                }
                currentEmptyItem.addAll(s);
            }
            result = new BasketResult(currentlyPurchasable, currentlyUnavailable, total_available_money, total_cost, total_available_money - total_cost);
        }
        return result;
    }
}
