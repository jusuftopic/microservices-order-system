package org.example.commons.event.utils;

import java.time.Duration;

public class KafkaUtils {

    public static Duration calculateBackoff(int retryCount) {

        return switch (retryCount) {

            case 1 -> Duration.ofSeconds(3);

            case 2 -> Duration.ofSeconds(10);

            case 3 -> Duration.ofSeconds(30);

            case 4 -> Duration.ofMinutes(1);

            case 5 -> Duration.ofMinutes(5);

            default -> Duration.ofMinutes(15);
        };
    }
}
