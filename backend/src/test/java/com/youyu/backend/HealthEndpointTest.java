package com.youyu.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(
        classes = YouyuBackendApplication.class,
        properties = {
                "management.endpoints.web.exposure.include=health",
                "management.endpoint.health.show-components=always",
                "management.endpoint.health.show-details=never",
                "management.health.db.enabled=true",
                "management.health.diskspace.enabled=true",
                "management.health.redis.enabled=false"
        }
)
class HealthEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void compatibilityHealthEndpointRemainsAvailable() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.database.status").value("UP"))
                .andExpect(jsonPath("$.data.database.url").doesNotExist())
                .andExpect(jsonPath("$.data.database.username").doesNotExist());
    }

    @Test
    void actuatorHealthExposesOnlyMinimumComponents() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"))
                .andExpect(jsonPath("$.components.diskSpace.status").value("UP"));

        int metricsStatus = mockMvc.perform(get("/actuator/metrics"))
                .andReturn()
                .getResponse()
                .getStatus();

        int prometheusStatus = mockMvc.perform(get("/actuator/prometheus"))
                .andReturn()
                .getResponse()
                .getStatus();

        assertNotEquals(200, metricsStatus);
        assertNotEquals(200, prometheusStatus);
    }
}
