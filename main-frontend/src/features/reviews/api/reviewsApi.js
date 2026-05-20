import http from "@/shared/api/http-client";

export async function listByMovie(movieId, { page = 0, size = 5, sort = "createdAt,desc" } = {}) {
    const { data } = await http.get(`/reviews/movie/${movieId}`, {
        params: { page, size, sort },
    });

    // Spring Page JSON -> нормальная форма для UI
    const items = Array.isArray(data?.content) ? data.content : Array.isArray(data?.items) ? data.items : [];
    const total =
        typeof data?.totalElements === "number"
            ? data.totalElements
            : typeof data?.total === "number"
                ? data.total
                : items.length;

    return {
        items,
        total,
        page: typeof data?.number === "number" ? data.number : page,
        size: typeof data?.size === "number" ? data.size : size,
        totalPages: typeof data?.totalPages === "number" ? data.totalPages : 1,
    };
}

export async function createReview({ movieId, content, parentId = null }) {
    const body = { movieId, content };
    // parentId добавляем только если реально нужен (иначе не шлём мусор)
    if (parentId !== null && parentId !== undefined) body.parentId = parentId;

    const { data } = await http.post("/reviews", body);
    return data;
}

export async function updateReview(reviewId, { content }) {
    const { data } = await http.put(`/reviews/${reviewId}`, { content });
    return data;
}

export async function deleteReview(reviewId) {
    const { data } = await http.delete(`/reviews/${reviewId}`);
    return data;
}
