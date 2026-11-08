package com.canteen.slots;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
@ConfigurationProperties("app.canteen")
@Getter @Setter
public class CanteenProperties {
    private LocalTime open;
    private LocalTime close;
    private int slotMinutes;
    private int slotCapacity;
    private int minLeadMinutes;
    private int minutesPerOrder;
}
