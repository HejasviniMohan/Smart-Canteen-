package com.canteen.slots;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/canteen")
@RequiredArgsConstructor
@Tag(name = "Canteen", description = "Live status and pickup slots (public)")
public class CanteenController {

    private final SlotService slots;

    @GetMapping("/status")
    @Operation(summary = "Whether the canteen is open, how busy it is, and the next free slot")
    public SlotService.CanteenStatus status() {
        return slots.status();
    }

    @GetMapping("/slots")
    @Operation(summary = "Upcoming pickup slots with remaining capacity")
    public List<SlotService.SlotView> upcoming() {
        return slots.upcoming();
    }
}
