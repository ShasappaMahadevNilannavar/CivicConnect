package com.civicconnect;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class CivicConnectApplicationTests {

    @Test
    void contextLoads() {
        // Verifies Spring context initializes with all beans, entities, repositories, and services
    }
}
