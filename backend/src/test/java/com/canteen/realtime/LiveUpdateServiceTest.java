package com.canteen.realtime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class LiveUpdateServiceTest {

    private static final OrderChangedEvent EVENT =
            new OrderChangedEvent(1L, "owner@test.com", "A001", "PLACED", "PAID");

    /** Behaves as a closed browser tab does: sending fails, and so does complete(). */
    private static class Emitter extends SseEmitter {
        final AtomicBoolean dead = new AtomicBoolean();
        final AtomicInteger delivered = new AtomicInteger();

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            if (dead.get()) throw new IOException("Broken pipe");
            delivered.incrementAndGet();
        }

        @Override
        public synchronized void complete() {
            if (dead.get()) throw new IllegalStateException("AsyncContext already failed");
            super.complete();
        }
    }

    private Emitter connect(LiveUpdateService service, String email, boolean isStaff) {
        Emitter emitter = new Emitter();
        service.register(emitter, email, isStaff);
        emitter.delivered.set(0);
        return emitter;
    }

    @Test
    @DisplayName("REGRESSION: a dead connection doesn't stop other staff from getting the event")
    void deadConnectionDoesNotBlockOthers() {
        LiveUpdateService service = new LiveUpdateService();
        Emitter dead = connect(service, "staff1@test.com", true);
        Emitter alive = connect(service, "staff2@test.com", true);
        dead.dead.set(true);

        service.onOrderChanged(EVENT);

        assertThat(alive.delivered).hasValue(1);
    }

    @Test
    @DisplayName("Dead connections are forgotten, so they aren't tried again")
    void deadConnectionIsDropped() {
        LiveUpdateService service = new LiveUpdateService();
        Emitter dead = connect(service, "staff@test.com", true);
        dead.dead.set(true);
        service.onOrderChanged(EVENT);

        dead.dead.set(false);
        service.onOrderChanged(EVENT);

        assertThat(dead.delivered).hasValue(0);
    }

    @Test
    @DisplayName("An owner who is also staff gets each event once, and other students get nothing")
    void rightPeopleOnce() {
        LiveUpdateService service = new LiveUpdateService();
        Emitter ownerStaff = connect(service, "owner@test.com", true);
        Emitter other = connect(service, "other@test.com", false);

        service.onOrderChanged(EVENT);

        assertThat(ownerStaff.delivered).hasValue(1);
        assertThat(other.delivered).hasValue(0);
    }

    @Test
    @DisplayName("Heartbeat reaches live connections and survives dead ones")
    void heartbeat() {
        LiveUpdateService service = new LiveUpdateService();
        Emitter dead = connect(service, "a@test.com", false);
        Emitter alive = connect(service, "b@test.com", false);
        dead.dead.set(true);

        service.heartbeat();

        assertThat(alive.delivered).hasValue(1);
    }
}
