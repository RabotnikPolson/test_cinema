import http from "@/shared/api/http-client";

export const getFavoritesByUser = async (userId) => {
  const res = await http.get(`/watchlists/user/${userId}`);
  return res.data;
};

export const addFavorite = async (userId, movieId) => {
  const res = await http.post("/watchlists", {
    userId,
    movieId,
  });
  return res.data;
};

export const removeFavorite = async (userId, movieId) => {
  const res = await http.delete("/watchlists", {
    params: { userId, movieId },
  });
  return res.data;
};
