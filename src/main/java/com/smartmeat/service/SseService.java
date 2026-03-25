package com.smartmeat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class SseService {

    // 25-minute timeout — frontend reconnects automatically if it expires
    private static final long SSE_TIMEOUT_MS = 25 * 60 * 1000L;

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong();
    private final ObjectMapper objectMapper;

    public SseEmitter register() {
        Long id = counter.incrementAndGet();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitters.put(id, emitter);

        // Remove emitter on any terminal event — prevents memory leaks
        emitter.onCompletion(() -> {
            emitters.remove(id);
            log.debug("SSE client {} disconnected (completion). Active: {}", id, emitters.size());
        });
        emitter.onTimeout(() -> {
            emitters.remove(id);
            log.debug("SSE client {} timed out. Active: {}", id, emitters.size());
        });
        emitter.onError(e -> {
            emitters.remove(id);
            log.debug("SSE client {} error: {}. Active: {}", id, e.getMessage(), emitters.size());
        });

        // Send initial connected event
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (Exception e) {
            emitters.remove(id);
            log.debug("SSE client {} failed on connect: {}", id, e.getMessage());
        }

        log.info("SSE client {} connected. Total active: {}", id, emitters.size());
        return emitter;
    }

    /**
     * Heartbeat sent every 20 seconds to all connected clients.
     * Prevents Tomcat/nginx from closing idle connections,
     * and lets the frontend detect stale connections quickly.
     */
    @Scheduled(fixedDelay = 20_000)
    public void heartbeat() {
        if (emitters.isEmpty()) return;
        emitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
            } catch (Exception e) {
                emitter.completeWithError(e);
                emitters.remove(id);
            }
        });
        log.debug("SSE heartbeat sent to {} client(s)", emitters.size());
    }

    @Async
    public void broadcast(String eventName, Object data) {
        if (emitters.isEmpty()) return;
        String json;
        try {
            json = objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("SSE serialization error for event '{}': {}", eventName, e.getMessage());
            return;
        }
        emitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(json));
            } catch (Exception e) {
                emitter.completeWithError(e);
                emitters.remove(id);
            }
        });
        log.debug("SSE '{}' broadcast to {} client(s)", eventName, emitters.size());
    }

    // ── Convenience broadcast methods ─────────────────────────────────────────
    public void newOrder(Object data)         { broadcast("new_order",     data); }
    public void orderStatusChanged(Object d)  { broadcast("order_status",  d);    }
    public void lowStockAlert(Object data)    { broadcast("low_stock",     data); }
    public void newReview(Object data)        { broadcast("new_review",    data); }
}