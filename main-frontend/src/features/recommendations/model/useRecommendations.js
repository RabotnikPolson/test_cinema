import { useQuery } from "@tanstack/react-query";
import {
  listRightRail,
  getSmartFeed,
  listRecommendationsByTab,
  listKazakhstanRecommendations,
  listBecauseYouLiked,
  listTrending,
} from "@/features/recommendations/api/recommendationsApi";

export const useRecommendationsTab = (type, movieId, limit = 15) =>
  useQuery({
    queryKey: ["recommendationsTab", type, movieId, limit],
    queryFn: () => listRecommendationsByTab(type, movieId, limit),
    enabled: !!movieId && !!type,
    staleTime: 5 * 60 * 1000, // 5 min
  });

export const useKazakhstanRecommendations = (limit = 15) =>
  useQuery({
    queryKey: ["recommendationsKazakhstan", limit],
    queryFn: () => listKazakhstanRecommendations(limit),
  });

export const useRightRail = (movieId, limit = 15) =>
  useQuery({
    queryKey: ["rightRail", movieId, limit],
    queryFn: () => listRightRail(movieId, limit),
    enabled: !!movieId,
  });

export const useSmartFeed = (userId) =>
  useQuery({
    queryKey: ["smartFeed", userId],
    queryFn: () => getSmartFeed(),
    enabled: !!userId,
  });

export const useBecauseYouLiked = (userId, limit = 15) =>
  useQuery({
    queryKey: ["recommendationsBecauseYouLiked", userId, limit],
    queryFn: () => listBecauseYouLiked(limit),
    enabled: !!userId,
  });

export const useTrending = (weekly = false) =>
  useQuery({
    queryKey: ["recommendationsTrending", weekly],
    queryFn: () => listTrending(weekly),
  });
