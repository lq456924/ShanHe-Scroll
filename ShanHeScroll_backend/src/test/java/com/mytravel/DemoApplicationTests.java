package com.mytravel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DemoApplicationTests {

    @Test
    void shouldCreateApplicationInstance() {
        DemoApplication app = new DemoApplication();
        assertNotNull(app);
    }
}
