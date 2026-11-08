package com.canteen.repository;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDate;

@Repository
public class DailyTokenCounterRepository {

    @PersistenceContext
    private EntityManager em;

    /** Atomically returns the next number for a date, starting at 1. Safe across instances. */
    public int next(LocalDate businessDate) {
        Number value = (Number) em.createNativeQuery("""
                        INSERT INTO daily_token_counters (business_date, last_value)
                        VALUES (:date, 1)
                        ON CONFLICT (business_date)
                        DO UPDATE SET last_value = daily_token_counters.last_value + 1
                        RETURNING last_value
                        """)
                .setParameter("date", businessDate)
                .getSingleResult();
        return value.intValue();
    }
}
