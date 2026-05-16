import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  addFavorite,
  getFavoritesByUser,
  removeFavorite,
} from "@/features/favorites/api/favoritesApi";

export function useFavorites(userId) {
  const qc = useQueryClient();
  const queryKey = ["favorites", userId];

  const favorites = useQuery({
    queryKey,
    queryFn: () => getFavoritesByUser(userId),
    enabled: !!userId,
  });

  const add = useMutation({
    mutationFn: (movieId) => addFavorite(userId, movieId),
    onSuccess: () => qc.invalidateQueries({ queryKey }),
  });

  const remove = useMutation({
    mutationFn: (movieId) => removeFavorite(userId, movieId),
    onSuccess: () => qc.invalidateQueries({ queryKey }),
  });

  return {
    ...favorites,
    add: add.mutateAsync,
    remove: remove.mutateAsync,
  };
}
