package io.github.ompc.dashscope4j.test;

import io.github.ompc.dashscope4j.Usage;
import org.junit.jupiter.api.Assertions;

import java.util.concurrent.atomic.AtomicLong;

public class UsageAssertions {

    public static void assertUsage(Usage usage) {
        Assertions.assertNotNull(usage.items());

        final var totalRef = new AtomicLong();
        usage.items().forEach(item-> {
            Assertions.assertNotNull(item.name());
            Assertions.assertTrue(item.cost()>=0);
            totalRef.addAndGet(item.cost());
        });

        Assertions.assertEquals(totalRef.get(), usage.total());
    }

}
