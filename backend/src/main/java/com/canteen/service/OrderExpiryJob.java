package com.canteen.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExpiryJob {

    private final OrderService orders;

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void run() {
        try {
            int expired = orders.expireUnpaidOrders();
            if (expired > 0) log.info("Expired {} unpaid orders", expired);
        } catch (OptimisticLockingFailureException e) {
            // A payment arrived at the same moment. The next run re-checks these orders.
            log.debug("Order changed during expiry; retrying next run");
        }
    }
}
