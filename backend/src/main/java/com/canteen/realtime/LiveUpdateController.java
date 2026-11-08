package com.canteen.realtime;

import com.canteen.controller.OrderController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@Tag(name = "Live updates", description = "Server-Sent Events")
public class LiveUpdateController {

    private final LiveUpdateService service;

    @GetMapping(value = "/api/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream of changes to my orders (all orders for STAFF/ADMIN)")
    public SseEmitter stream(Authentication auth) {
        return service.connect(auth.getName(), OrderController.isStaff(auth));
    }
}
