package com.canteen.realtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Pushes order changes to connected browsers over Server-Sent Events. Connections live in
 * this instance's memory; running several instances would need Redis pub/sub between them.
 */
@Service
@Slf4j
public class LiveUpdateService {

    /** Clients reconnect automatically, so a timeout just clears out dead connections. */
    private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<String, Set<SseEmitter>> byUser = new ConcurrentHashMap<>();
    private final Set<SseEmitter> staff = new CopyOnWriteArraySet<>();

    public SseEmitter connect(String email, boolean isStaff) {
        return register(new SseEmitter(EMITTER_TIMEOUT_MS), email, isStaff);
    }

    SseEmitter register(SseEmitter emitter, String email, boolean isStaff) {
        byUser.computeIfAbsent(email, k -> new CopyOnWriteArraySet<>()).add(emitter);
        if (isStaff) staff.add(emitter);

        emitter.onCompletion(() -> remove(emitter));
        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> remove(emitter));

        send(emitter, SseEmitter.event().name("connected").data(Map.of("ok", true), MediaType.APPLICATION_JSON));
        return emitter;
    }

    /**
     * AFTER_COMMIT means a rolled-back change is never announced, and the client's refetch
     * is guaranteed to see the new data. @Async keeps a slow client from delaying the request.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderChanged(OrderChangedEvent event) {
        Set<SseEmitter> owner = byUser.getOrDefault(event.ownerEmail(), Set.of());
        for (SseEmitter e : owner) {
            send(e, orderEvent(event));
        }
        for (SseEmitter e : staff) {
            if (!owner.contains(e)) {
                send(e, orderEvent(event));
            }
        }
    }

    /** Stops proxies from closing idle connections, and reveals dead ones. */
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        byUser.values().forEach(set -> set.forEach(e -> send(e, SseEmitter.event().comment("ping"))));
        byUser.values().removeIf(Set::isEmpty);
    }

    private static SseEmitter.SseEventBuilder orderEvent(OrderChangedEvent event) {
        return SseEmitter.event().name("order").data(event, MediaType.APPLICATION_JSON);
    }

    /**
     * One dead connection must never stop the others from getting an event, so this never
     * throws. A failed emitter is only forgotten: calling complete() on it is both needless
     * (the container is already ending that request) and itself throws from this thread.
     */
    private void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (Exception ex) {
            log.debug("Dropping dead live-update connection: {}", ex.getMessage());
            remove(emitter);
        }
    }

    private void remove(SseEmitter emitter) {
        byUser.values().forEach(set -> set.remove(emitter));
        staff.remove(emitter);
    }
}
