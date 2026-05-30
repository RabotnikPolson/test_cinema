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
  const res = await http.delete("/watchlists", {
    params: { userId: Number(userId), movieId: Number(movieId) },
  });
  return res.data;
};
