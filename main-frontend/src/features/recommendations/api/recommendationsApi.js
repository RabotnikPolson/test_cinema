import aiHttp from "@/shared/api/ai-http-client";

export const listRecommendationsByTab = async (type, movieId, limit = 15) => {
  const response = await aiHttp.get(`/api/v1/recommendations/tab/${type}/${movieId}?limit=${limit}`);
  return response.data;
};

export const listKazakhstanRecommendations = async (limit = 15) => {
  const response = await aiHttp.get(`/api/v1/recommendations/tab/kazakhstan?limit=${limit}`);
  return response.data;
};

// Legacy support
export const listRightRail = async (movieId, limit = 15) => {
  const response = await aiHttp.get(`/api/v1/recommendations/movie/${movieId}?limit=${limit}`);
  return response.data;
};

export const getSmartFeed = async (userId) => {
  const url = userId
    ? `/api/v1/recommendations/feed?user_id=${userId}`
    : `/api/v1/recommendations/feed`;
  const response = await aiHttp.get(url);
  return response.data;
};

export const listBecauseYouLiked = async (userId, limit = 15) => {
  const response = await aiHttp.get(`/api/v1/recommendations/tab/because-you-liked?user_id=${userId}&limit=${limit}`);
  return response.data;
};

export const listTrending = async (weekly = false) => {
  const endpoint = weekly ? "/api/v1/trending/weekly" : "/api/v1/trending";
  const response = await aiHttp.get(endpoint);
  return response.data;
};
