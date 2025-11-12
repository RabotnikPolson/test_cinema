package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.rating.RatingRequest;
import com.cinema.testcinema.dto.review.ReviewCreateRequest;
import com.cinema.testcinema.dto.review.ReviewUpdateRequest;
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

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RatingReviewIntegrationTest {

    private static final long USER_ID = 100L;
    private static final long SECOND_USER_ID = 200L;
    private static final long ADMIN_ID = 300L;
    private static final long MOVIE_ID = 400L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM reviews");
        jdbcTemplate.update("DELETE FROM ratings");
        jdbcTemplate.update("DELETE FROM movies WHERE id = ?", MOVIE_ID);
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id IN (?,?,?)", USER_ID, SECOND_USER_ID, ADMIN_ID);
        jdbcTemplate.update("DELETE FROM users WHERE id IN (?,?,?)", USER_ID, SECOND_USER_ID, ADMIN_ID);

        jdbcTemplate.update("INSERT INTO movies (id, title) VALUES (?, ?) ON CONFLICT (id) DO UPDATE SET title = EXCLUDED.title",
                MOVIE_ID, "Integration Movie");

        long userRoleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE role_name = 'ROLE_USER'", Long.class);
        long adminRoleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE role_name = 'ROLE_ADMIN'", Long.class);

        insertUser(USER_ID, "user@test.local", "user1");
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?) ON CONFLICT DO NOTHING", USER_ID, userRoleId);

        insertUser(SECOND_USER_ID, "second@test.local", "user2");
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?) ON CONFLICT DO NOTHING", SECOND_USER_ID, userRoleId);

        insertUser(ADMIN_ID, "admin@test.local", "admin-test");
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?) ON CONFLICT DO NOTHING", ADMIN_ID, userRoleId);
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?) ON CONFLICT DO NOTHING", ADMIN_ID, adminRoleId);
    }

    private void insertUser(long id, String email, String username) {
        jdbcTemplate.update(
                "INSERT INTO users (id, email, username, password_hash, created_at, enabled) " +
                        "VALUES (?, ?, ?, ?, now(), TRUE) " +
                        "ON CONFLICT (id) DO UPDATE SET email = EXCLUDED.email, username = EXCLUDED.username",
                id, email, username, "$2a$10$abcdefghijklmnopqrstuv" // dummy bcrypt
        );
    }

    @Test
    @DisplayName("GET /ratings/movie/{id} без токена возвращает 401")
    void ratingsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/ratings/movie/" + MOVIE_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /reviews/movie/{id} без токена доступен")
    void reviewsArePublic() throws Exception {
        mockMvc.perform(get("/reviews/movie/" + MOVIE_ID))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST рейтинга без токена запрещён")
    void ratingMutationRequiresAuth() throws Exception {
        RatingRequest request = new RatingRequest(MOVIE_ID, (short) 8);
        mockMvc.perform(post("/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST отзыва без токена запрещён")
    void reviewMutationRequiresAuth() throws Exception {
        ReviewCreateRequest request = new ReviewCreateRequest(MOVIE_ID, "Great movie", null);
        mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Повторный POST рейтинга обновляет оценку")
    void ratingPostIsUpsert() throws Exception {
        RatingRequest firstRequest = new RatingRequest(MOVIE_ID, (short) 7);
        MvcResult firstResult = mockMvc.perform(post("/ratings")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode firstJson = objectMapper.readTree(firstResult.getResponse().getContentAsString());
        long ratingId = firstJson.get("id").asLong();
        assertThat(firstJson.get("score").asInt()).isEqualTo(7);

        RatingRequest secondRequest = new RatingRequest(MOVIE_ID, (short) 9);
        MvcResult secondResult = mockMvc.perform(post("/ratings")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode secondJson = objectMapper.readTree(secondResult.getResponse().getContentAsString());
        assertThat(secondJson.get("id").asLong()).isEqualTo(ratingId);
        assertThat(secondJson.get("score").asInt()).isEqualTo(9);

        mockMvc.perform(get("/ratings/movie/" + MOVIE_ID)
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(USER_ID)).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].score").value(9));
    }

    @Test
    @DisplayName("Создание отзыва и ответа")
    void createReviewAndReply() throws Exception {
        long reviewId = createReview(USER_ID, "Первый отзыв", null);
        long replyId = createReview(SECOND_USER_ID, "Ответ на отзыв", reviewId);

        assertThat(replyId).isPositive();

        mockMvc.perform(get("/reviews/" + reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replies.length()").value(1))
                .andExpect(jsonPath("$.replies[0].parentId").value(reviewId));
    }

    @Test
    @DisplayName("Шестой ответ на узел запрещён")
    void reviewReplyLimit() throws Exception {
        long reviewId = createReview(USER_ID, "Корневой", null);
        for (int i = 0; i < 5; i++) {
            createReview(SECOND_USER_ID, "Ответ №" + i, reviewId);
        }

        ReviewCreateRequest request = new ReviewCreateRequest(MOVIE_ID, "Ещё ответ", reviewId);
        mockMvc.perform(post("/reviews")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(SECOND_USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Редактирование доступно в течение часа и отмечает edited=true")
    void reviewEditRules() throws Exception {
        long reviewId = createReview(USER_ID, "Свежий отзыв", null);

        ReviewUpdateRequest updateRequest = new ReviewUpdateRequest("Обновлённый текст");
        MvcResult result = mockMvc.perform(put("/reviews/" + reviewId)
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("edited").asBoolean()).isTrue();

        long oldReviewId = createReview(USER_ID, "Старый отзыв", null);
        jdbcTemplate.update("UPDATE reviews SET created_at = ?, updated_at = ? WHERE id = ?",
                Timestamp.from(Instant.now().minusSeconds(7200)),
                Timestamp.from(Instant.now().minusSeconds(7200)),
                oldReviewId);

        ReviewUpdateRequest lateRequest = new ReviewUpdateRequest("Позднее изменение");
        mockMvc.perform(put("/reviews/" + oldReviewId)
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Удаление узла удаляет его поддерево")
    void deleteReviewRemovesSubtree() throws Exception {
        long rootId = createReview(USER_ID, "Корневой", null);
        long firstReply = createReview(SECOND_USER_ID, "Первый ответ", rootId);
        long secondReply = createReview(SECOND_USER_ID, "Второй ответ", rootId);
        long nestedReply = createReview(SECOND_USER_ID, "Вложенный", secondReply);
        createReview(USER_ID, "Третий ответ", rootId);

        mockMvc.perform(delete("/reviews/" + secondReply)
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(SECOND_USER_ID)).roles("USER")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/reviews/" + rootId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replies[*].id").value(org.hamcrest.Matchers.hasItem((int) firstReply)))
                .andExpect(jsonPath("$.replies[*].id").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem((int) secondReply))));

        mockMvc.perform(get("/reviews/" + nestedReply))
                .andExpect(status().isNotFound());
    }

    private long createReview(long userId, String content, Long parentId) throws Exception {
        ReviewCreateRequest request = new ReviewCreateRequest(MOVIE_ID, content, parentId);
        MvcResult result = mockMvc.perform(post("/reviews")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(userId)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }
}
