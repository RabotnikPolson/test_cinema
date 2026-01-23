package com.cinema.testcinema.test;

import com.cinema.testcinema.auth.dto.LoginRequest;
import com.cinema.testcinema.auth.dto.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TestAuth {

    public static String registerAndLogin(
            MockMvc mockMvc,
            ObjectMapper om,
            String email,
            String username,
            String password
    ) throws Exception {

        // register
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(
                                new RegisterRequest(email, username, password)
                        )))
                .andExpect(status().isCreated());

        // login
        var res = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(
                                new LoginRequest(email, password)
                        )))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = om.readTree(res.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }
}
