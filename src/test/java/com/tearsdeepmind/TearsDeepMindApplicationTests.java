package com.tearsdeepmind;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
class TearsDeepMindApplicationTests {

    @Test
    void contextLoads() {
        // Simple sanity check to ensure the Spring context loads correctly
    }

}
