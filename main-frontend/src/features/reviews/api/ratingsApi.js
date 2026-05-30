import http from "@/shared/api/http-client";

export async function rateMovie(movieId, score) {
    // бэк ждёт score 1..10 (Short)
    const { data } = await http.post("/ratings", {
        movieId,
        score: Number(score),
    });
    return data;
}
