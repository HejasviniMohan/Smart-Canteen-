package com.canteen.util;

import com.canteen.repository.OrderRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class TokenGenerator {

    private final OrderRepository orderRepo;
    private static final String[] PREFIXES = {"A", "B", "C", "D", "E"};
    private final AtomicLong counter = new AtomicLong(0);

    @PostConstruct
    public void init() {
        orderRepo.findTopByOrderByIdDesc()
                .ifPresent(order -> {
                    String tn = order.getTokenNumber();
                    if (tn != null && tn.length() > 1) {
                        try {
                            long num = Long.parseLong(tn.substring(1));
                            counter.set(num);
                        } catch (NumberFormatException ignored) {}
                    }
                });
    }

    public synchronized String nextToken() {
        long count = counter.incrementAndGet();
        String prefix = PREFIXES[(int) ((count - 1) / 100) % PREFIXES.length];
        return prefix + count;
    }
}