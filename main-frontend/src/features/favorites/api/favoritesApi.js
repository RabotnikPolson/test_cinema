import http from "@/shared/api/http-client";

export const getFavoritesByUser = async (userId) => {
  const res = await http.get(`/watchlists/user/${userId}`);
  return res.data;
};

export const addFavorite = async (userId, movieId) => {
  const payload = {
    userId: Number(userId),
    movieId: Number(movieId),
  };
  console.log("📤 Отправляю addFavorite:", payload);
  const res = await http.post("/watchlists", payload);
  console.log("📥 Ответ addFavorite:", res.data);
  return res.data;
};

export const removeFavorite = async (userId, movieId) => {
  const finalUserId = Number(userId);
  const finalMovieId = Number(movieId);
  // Вариант 1: ID в пути URL
  const url = `/watchlists/${finalUserId}/${finalMovieId}`;
  console.log("📤 Отправляю removeFavorite к", url);
  const res = await http.delete(url);
  console.log("📥 Ответ removeFavorite:", res.data);
  return res.data;
};
