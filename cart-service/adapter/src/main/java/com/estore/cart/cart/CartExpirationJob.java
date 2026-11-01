package com.estore.cart.cart;

import com.estore.cart.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartExpirationJob {
    private final CartService cartService;

    @Scheduled(fixedDelayString = "${cart.expiration.check-interval-millis}")
    public void expireInactiveCarts() {
        int expiredCarts = cartService.expireInactiveCarts();
        if (expiredCarts > 0) {
            log.info("Marked {} inactive carts as expired", expiredCarts);
        }
    }

    @Scheduled(cron = "${cart.expiration.cleanup-cron}")
    public void deleteExpiredCarts() {
        long deletedCarts = cartService.deleteExpiredCarts();
        if (deletedCarts > 0) {
            log.info("Deleted {} expired carts past the retention period", deletedCarts);
        }
    }
}
