package uk.ncl.CSC8016.jackbergus.coursework.project2.utils;

import java.math.BigInteger;
import java.util.Objects;
import java.util.UUID;

/**
 * The class determining an item being sold.
 * This is a placeholder for a productname and cost association
 */
public class Item implements Comparable<Item> {
    public final String productName;
    public final double cost;

    public final BigInteger id;

    public Item(String productName, double cost, BigInteger id) {
        this.productName = productName;
        this.cost = cost;
        this.id = id;
    }

    /**
     * A normal string representation of the item
     */
    @Override
    public String toString() {
        return "The "+id+"-th " + productName+" costs "+this.cost+"£";
    }

    /**
     * override of the equals method
     * can be used for comparison of two items
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return Double.compare(item.cost, cost) == 0 && Objects.equals(productName, item.productName) && id.compareTo(item.id) == 0;
    }

    /**
     * and override of the hashcode
     *
     * @return
     */
    @Override
    public int hashCode() {
        return Objects.hash(productName, id, cost);
    }

    /**
     * an override of comparTo to compare two objects
     * @param o the object to be compared.
     * @return
     */
    @Override
    public int compareTo(Item o) {
        if (this == o) return 0;
        int cmp = productName.compareTo(o.productName);
        if (cmp != 0) return cmp;
        cmp = id.compareTo(o.id);
        if (cmp != 0) return cmp;
        return Double.compare(cost, o.cost);
    }
}
