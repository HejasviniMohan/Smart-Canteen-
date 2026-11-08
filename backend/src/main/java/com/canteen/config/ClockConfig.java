package com.canteen.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

/**
 * The canteen runs on local time wherever the server is hosted. Without this, a UTC host
 * would report the wrong "today" and offer pickup slots at the wrong hours.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.timezone}") String timezone) {
        // Also covers LocalDateTime.now() in entity lifecycle callbacks.
        TimeZone.setDefault(TimeZone.getTimeZone(timezone));
        return Clock.system(ZoneId.of(timezone));
    }
}
