package com.tearsdeepmind;

import com.tearsdeepmind.dto.SubscriberDto;
import com.tearsdeepmind.entity.SubscriberEntity;
import com.tearsdeepmind.repository.SubscriberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SubscriberIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubscriberRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JavaMailSender javaMailSender;

    @Test
    public void testSubscriberLifecycle() throws Exception {
        String email = "test@example.com";
        SubscriberDto dto = new SubscriberDto(email, "Test User", true);

        // 1. Create
        mockMvc.perform(post("/api/v1/subscribers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.isActive").value(true));

        // 2. List
        mockMvc.perform(get("/api/v1/subscribers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == '" + email + "')]" ).exists());

        // 3. Toggle Status
        mockMvc.perform(patch("/api/v1/subscribers/" + email + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isActive\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));

        // 4. Verify in Repo
        List<SubscriberEntity> active = repository.findByIsActiveTrue();
        assertFalse(active.stream().anyMatch(s -> s.getEmail().equals(email)));

        // 5. Delete
        mockMvc.perform(delete("/api/v1/subscribers/" + email))
                .andExpect(status().isNoContent());

        assertFalse(repository.findById(email).isPresent());
    }
}
