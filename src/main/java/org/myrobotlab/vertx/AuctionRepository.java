package org.myrobotlab.vertx;

import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;

import java.math.BigDecimal;
import java.util.Optional;

public class AuctionRepository {

    private SharedData sharedData;

    public AuctionRepository(SharedData sharedData) {
        this.sharedData = sharedData;
    }

    public Optional<Auction> getById(String auctionId) {
        LocalMap<String, String> auctionSharedData = this.sharedData.getLocalMap(auctionId);
        return Optional.of(auctionSharedData)
            .filter(m -> !m.isEmpty())
            .map(this::convertToAuction);
    }

    public void save(Auction auction) {
        LocalMap<String, String> auctionSharedData = this.sharedData.getLocalMap(auction.getId());

        auctionSharedData.put("id", auction.getId());
        auctionSharedData.put("price", auction.getPrice().toString());
    }

    private Auction convertToAuction(LocalMap<String, String> auction) {
        return new Auction(
            auction.get("id"),
            new BigDecimal(auction.get("price"))
        );
    }
}
