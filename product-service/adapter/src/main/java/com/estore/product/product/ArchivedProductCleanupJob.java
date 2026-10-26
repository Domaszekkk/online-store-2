package com.estore.product.product;

import com.estore.product.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArchivedProductCleanupJob {
    private final ProductService productService;

    @Scheduled(cron = "${product.archive.cleanup-cron}")
    public void deleteExpiredArchivedProducts() {
        long deletedProducts = productService.deleteExpiredArchivedProducts();
        if (deletedProducts > 0) {
            log.info("Deleted {} archived products past the retention period", deletedProducts);
        }
    }
}
