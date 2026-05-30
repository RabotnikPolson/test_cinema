import http from "@/shared/api/http-client";

/**
 * Корневые комментарии по фильму
 * order: new | old | top
 */
export const listRootComments = ({ movieId, page = 0, size = 20, order = "new" }) =>
    http
        .get(`/comments/movie/${movieId}`, {
            params: { page, size, order },
        })
        .then((r) => r.data);

/**
 * Счётчики комментариев (root + total)
 */
export const getCommentCount = (movieId) =>
    http.get(`/comments/movie/${movieId}/count`).then((r) => r.data);

/**
 * Один комментарий + его ответы
 */
export const getCommentWithReplies = (commentId) =>
    http.get(`/comments/${commentId}`).then((r) => r.data);

/**
 * Создать комментарий или ответ
 */
export const createComment = (payload) =>
    http.post("/comments", payload).then((r) => r.data);

/**
 * Обновить комментарий
 */
export const updateComment = (id, payload) =>
    http.put(`/comments/${id}`, payload).then((r) => r.data);

/**
 * Удалить комментарий
 */
export const deleteComment = (id) =>
    http.delete(`/comments/${id}`);

/**
 * Поставить / снять реакцию
 * emoji: 👍 👎 😂 😡 и т.д.
 */
export const reactComment = (id, emoji) =>
    http.post(`/comments/${id}/reactions`, { emoji }).then((r) => r.data);
