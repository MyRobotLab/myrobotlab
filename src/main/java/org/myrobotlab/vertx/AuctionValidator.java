package org.myrobotlab.vertx;

public class AuctionValidator {

    private final AuctionRepository repository;

    public AuctionValidator(AuctionRepository repository) {
        this.repository = repository;
    }

    public boolean validate(Auction auction) {
        Auction auctionDatabase = repository.getById(auction.getId())
            .orElseThrow(() -> new AuctionNotFoundException(auction.getId()));

        return auctionDatabase.getPrice().compareTo(auction.getPrice()) == -1;
    }
}
