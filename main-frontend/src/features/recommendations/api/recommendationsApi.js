import http from "@/shared/api/http-client";

export const listRecommendationsByTab = async (type, movieId, limit = 15) => {
  const res = await http.get(`/api/recommendations/movie/${movieId}/tabs/${type}`, { params: { limit } });
  return res.data;
};

export const listRightRail = async (movieId, limit = 15) => {
  const res = await http.get(`/api/recommendations/movie/${movieId}`, { params: { limit } });
  return res.data;
};

// userId ignored — backend resolves from JWT
export const getSmartFeed = async (_userId, limit = 10) => {
  const res = await http.get("/api/recommendations/feed", { params: { limit } });
  return res.data;
};

// userId ignored — backend resolves from JWT
export const listBecauseYouLiked = async (_userId, limit = 15) => {
  const res = await http.get("/api/recommendations/because-you-liked", { params: { limit } });
  return res.data;
};

export const listTrending = async () => {
  const res = await http.get("/trending");
  // Java /trending returns [{movieId, title, posterUrl, score, isDomestic}]
  // wrap to match existing {recommendations:[]} shape used by HomePage
  return { recommendations: Array.isArray(res.data) ? res.data : [] };
};

export const listHeroMovies = async () => {
  const res = await http.get("/trending/hero");
  return res.data;
};

// userId not sent — backend resolves from JWT; filters forwarded via proxy to AI service
export const listKazakhstanMovies = async ({ userId, limit = 20, genre, yearFrom, yearTo, sortBy = "relevance" } = {}) => {
  const params = { limit };
  if (genre) params.genre = genre;
  if (yearFrom) params.yearFrom = yearFrom;
  if (yearTo) params.yearTo = yearTo;
  if (sortBy) params.sortBy = sortBy;
  const res = await http.get("/api/recommendations/kazakhstan", { params });
  return res.data;
};

export const listKazakhstanGenres = async () => {
  const res = await http.get("/api/recommendations/kazakhstan/genres");
  return res.data;
};
