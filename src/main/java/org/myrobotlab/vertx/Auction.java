package org.myrobotlab.vertx;

import java.math.BigDecimal;

public class Auction {

    private final String id;
    private final BigDecimal price;

    public Auction(String id, BigDecimal price) {
        this.id = id;
        this.price = price;
    }

    public Auction(String auctionId) {
        this(auctionId, BigDecimal.ZERO);
    }

    public String getId() {
        return id;
    }

    public BigDecimal getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return "Auction{" +
                "id='" + id + '\'' +
                ", price=" + price +
                '}';
    }
}
