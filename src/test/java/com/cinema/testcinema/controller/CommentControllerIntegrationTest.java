package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.comment.CommentCreateRequest;
import com.cinema.testcinema.dto.comment.CommentReactionRequest;
import com.cinema.testcinema.dto.comment.CommentUpdateRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommentControllerIntegrationTest {

    private static final long USER_ID = 1000L;
    private static final long SECOND_USER_ID = 2000L;
    private static final long ADMIN_ID = 3000L;
    private static final long MOVIE_ID = 5000L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM comment_reactions");
        jdbcTemplate.update("DELETE FROM comments");
        jdbcTemplate.update("DELETE FROM review_reactions");
        jdbcTemplate.update("DELETE FROM reviews");
        jdbcTemplate.update("DELETE FROM ratings");
        jdbcTemplate.update("DELETE FROM movies WHERE id = ?", MOVIE_ID);
        jdbcTemplate.update("DELETE FROM email_verification_tokens");
        jdbcTemplate.update("DELETE FROM user_profiles");
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id IN (?,?,?)", USER_ID, SECOND_USER_ID, ADMIN_ID);
        jdbcTemplate.update("DELETE FROM users WHERE id IN (?,?,?)", USER_ID, SECOND_USER_ID, ADMIN_ID);

        jdbcTemplate.update("INSERT INTO movies (id, title) VALUES (?, ?) ON CONFLICT (id) DO UPDATE SET title = EXCLUDED.title",
                MOVIE_ID, "Comments Movie");

        long userRoleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE role_name = 'ROLE_USER'", Long.class);
        long adminRoleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE role_name = 'ROLE_ADMIN'", Long.class);

        insertUser(USER_ID, "comment-user@test.local", "comment-user");
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?) ON CONFLICT DO NOTHING", USER_ID, userRoleId);

        insertUser(SECOND_USER_ID, "comment-second@test.local", "comment-second");
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?) ON CONFLICT DO NOTHING", SECOND_USER_ID, userRoleId);

        insertUser(ADMIN_ID, "comment-admin@test.local", "comment-admin");
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?) ON CONFLICT DO NOTHING", ADMIN_ID, userRoleId);
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?) ON CONFLICT DO NOTHING", ADMIN_ID, adminRoleId);
    }

    private void insertUser(long id, String email, String username) {
        jdbcTemplate.update(
                "INSERT INTO users (id, email, username, password_hash, created_at, enabled) " +
                        "VALUES (?, ?, ?, ?, now(), TRUE) " +
                        "ON CONFLICT (id) DO UPDATE SET email = EXCLUDED.email, username = EXCLUDED.username",
                id, email, username, "$2a$10$abcdefghijklmnopqrstuv"
        );
    }

    @Test
    @DisplayName("Создание корневого комментария и ответа")
    void createRootAndReply() throws Exception {
        long rootId = createComment(USER_ID, "Корневой комментарий", null);
        long replyId = createComment(SECOND_USER_ID, "Ответ на комментарий", rootId);

        assertThat(replyId).isPositive();

        mockMvc.perform(get("/comments/" + rootId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replies.length()").value(1))
                .andExpect(jsonPath("$.replies[0].parentId").value(rootId));
    }

    @Test
    @DisplayName("Комментарии по фильму возвращаются с реакциями и ответами")
    void listCommentsWithReactions() throws Exception {
        long rootId = createComment(USER_ID, "Комментарий", null);
        createComment(SECOND_USER_ID, "Ответ", rootId);

        CommentReactionRequest reactionRequest = new CommentReactionRequest("👍");
        mockMvc.perform(post("/comments/" + rootId + "/reactions")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(SECOND_USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reactionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].emoji").value("👍"))
                .andExpect(jsonPath("$[0].count").value(1));

        mockMvc.perform(get("/comments/movie/" + MOVIE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].replies.length()").value(1))
                .andExpect(jsonPath("$.content[0].reactions[0].emoji").value("👍"))
                .andExpect(jsonPath("$.content[0].reactions[0].count").value(1));
    }

    @Test
    @DisplayName("Редактирование и удаление комментариев проверяет права")
    void updateAndDeletePermissions() throws Exception {
        long commentId = createComment(USER_ID, "Исходный текст", null);

        CommentUpdateRequest updateRequest = new CommentUpdateRequest("Новый текст");
        mockMvc.perform(put("/comments/" + commentId)
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(SECOND_USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/comments/" + commentId)
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.edited").value(true))
                .andExpect(jsonPath("$.content").value("Новый текст"));

        mockMvc.perform(delete("/comments/" + commentId)
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(SECOND_USER_ID)).roles("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/comments/" + commentId)
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(ADMIN_ID)).roles("ADMIN", "USER")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/comments/" + commentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Реакции на комментарии переключаются и суммируются")
    void commentReactionsToggle() throws Exception {
        long commentId = createComment(USER_ID, "Текст", null);

        CommentReactionRequest request = new CommentReactionRequest("🔥");
        mockMvc.perform(post("/comments/" + commentId + "/reactions")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].count").value(1));

        mockMvc.perform(post("/comments/" + commentId + "/reactions")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(post("/comments/" + commentId + "/reactions")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/comments/" + commentId + "/reactions")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(SECOND_USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].count").value(2));
    }

    private long createComment(long userId, String content, Long parentId) throws Exception {
        CommentCreateRequest request = new CommentCreateRequest(MOVIE_ID, content, parentId);
        MvcResult result = mockMvc.perform(post("/comments")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(userId)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }
}
