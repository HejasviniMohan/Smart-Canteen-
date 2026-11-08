package com.canteen.util;

import com.canteen.repository.DailyTokenCounterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** Produces counter tokens that restart each day: A001..A100, B001..B100, and so on. */
@Component
@RequiredArgsConstructor
public class TokenGenerator {

    private static final int BLOCK_SIZE = 100;

    private final DailyTokenCounterRepository counters;

    public String nextToken(LocalDate businessDate) {
        return format(counters.next(businessDate));
    }

    static String format(int number) {
        int zeroBased = number - 1;
        char letter = (char) ('A' + (zeroBased / BLOCK_SIZE) % 26);
        return String.format("%c%03d", letter, zeroBased % BLOCK_SIZE + 1);
    }
}
