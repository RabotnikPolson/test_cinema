import { useQuery } from "@tanstack/react-query";
import {
  listRightRail,
  getSmartFeed,
  listRecommendationsByTab,
  listDomesticRecommendations,
} from "@/features/recommendations/api/recommendationsApi";

export const useRecommendationsTab = (type, movieId, limit = 15) =>
  useQuery({
    queryKey: ["recommendationsTab", type, movieId, limit],
    queryFn: () => listRecommendationsByTab(type, movieId, limit),
    enabled: !!movieId && !!type,
    staleTime: 5 * 60 * 1000, // 5 min
  });

export const useDomesticRecommendations = (limit = 15) =>
  useQuery({
    queryKey: ["recommendationsDomestic", limit],
    queryFn: () => listDomesticRecommendations(limit),
  });

export const useRightRail = (movieId, limit = 15) =>
  useQuery({
    queryKey: ["rightRail", movieId, limit],
    queryFn: () => listRightRail(movieId, limit),
    enabled: !!movieId,
  });

export const useSmartFeed = () =>
  useQuery({
    queryKey: ["smartFeed"],
    queryFn: () => getSmartFeed(),
  });
