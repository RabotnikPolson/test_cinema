import http from "@/shared/api/http-client";

export const listRecommendationsByTab = async (type, movieId, limit = 15) => {
  const response = await http.get(`/api/recommendations/movie/${movieId}/tabs/${type}?limit=${limit}`);
  return response.data;
};

export const listKazakhstanRecommendations = async (limit = 15) => {
  const response = await http.get(`/api/recommendations/kazakhstan?limit=${limit}`);
  return response.data;
};

export const listRightRail = async (movieId, limit = 15) => {
  const response = await http.get(`/api/recommendations/movie/${movieId}?limit=${limit}`);
  return response.data;
};

export const getSmartFeed = async () => {
  const response = await http.get("/api/recommendations/feed");
  return response.data;
};

export const listBecauseYouLiked = async (limit = 15) => {
  const response = await http.get(`/api/recommendations/because-you-liked?limit=${limit}`);
  return response.data;
};

export const listTrending = async (weekly = false) => {
  const response = await http.get(`/api/recommendations/trending?weekly=${weekly}`);
  return response.data;
};