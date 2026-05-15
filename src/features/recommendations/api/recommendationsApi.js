import aiHttp from "@/shared/api/ai-http-client";

export const listRecommendationsByTab = async (type, movieId, limit = 15) => {
  const response = await aiHttp.get(`/api/v1/recommend/tab/${type}/${movieId}?limit=${limit}`);
  return response.data;
};

export const listDomesticRecommendations = async (limit = 15) => {
  const response = await aiHttp.get(`/api/v1/recommend/tab/domestic?limit=${limit}`);
  return response.data;
};

// Legacy support
export const listRightRail = async (movieId, limit = 15) => {
  const response = await aiHttp.get(`/api/v1/recommend/tab/hybrid/${movieId}?limit=${limit}`);
  return response.data;
};

export const getSmartFeed = async (userId) => {
  const url = userId
    ? `/api/v1/recommend/feed/${userId}`
    : `/api/v1/stats`;
  const response = await aiHttp.get(url);
  return response.data;
};
