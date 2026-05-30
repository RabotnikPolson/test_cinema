import { useQuery } from "@tanstack/react-query";
import {
  listRightRail,
  getSmartFeed,
  listRecommendationsByTab,
  listBecauseYouLiked,
  listTrending,
  listKazakhstanMovies,
  listKazakhstanGenres,
} from "@/features/recommendations/api/recommendationsApi";

export const useRecommendationsTab = (type, movieId, limit = 15) =>
  useQuery({
    queryKey: ["recommendationsTab", type, movieId, limit],
    queryFn: () => listRecommendationsByTab(type, movieId, limit),
    enabled: !!movieId && !!type,
    staleTime: 5 * 60 * 1000,
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
    queryFn: () => getSmartFeed(userId),
    enabled: !!userId,
  });

export const useBecauseYouLiked = (userId, limit = 15) =>
  useQuery({
    queryKey: ["recommendationsBecauseYouLiked", userId, limit],
    queryFn: () => listBecauseYouLiked(userId, limit),
    enabled: !!userId,
  });

export const useTrending = (weekly = false) =>
  useQuery({
    queryKey: ["recommendationsTrending", weekly],
    queryFn: () => listTrending(weekly),
  });

export const useKazakhstanMovies = (filters = {}) =>
  useQuery({
    queryKey: ["kazakhstanMovies", filters],
    queryFn: () => listKazakhstanMovies(filters),
    staleTime: 5 * 60 * 1000,
  });

export const useKazakhstanGenres = () =>
  useQuery({
    queryKey: ["kazakhstanGenres"],
    queryFn: () => listKazakhstanGenres(),
    staleTime: 30 * 60 * 1000,
  });
