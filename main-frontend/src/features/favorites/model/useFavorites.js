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
    mutationFn: (movieId) => addFavorite(userId, Number(movieId)),
    onMutate: async (movieId) => {
      await qc.cancelQueries({ queryKey });
      const previousFavorites = qc.getQueryData(queryKey);
      qc.setQueryData(queryKey, (old = []) => [
        ...old,
        { movieId: Number(movieId) },
      ]);
      return { previousFavorites };
    },
  });

  const remove = useMutation({
    mutationFn: (movieId) => removeFavorite(userId, Number(movieId)),
    onMutate: async (movieId) => {
      await qc.cancelQueries({ queryKey });
      const previousFavorites = qc.getQueryData(queryKey);
      const newFavorites = (previousFavorites || []).filter(
        (item) => Number(item?.movieId ?? item) !== Number(movieId)
      );
      qc.setQueryData(queryKey, newFavorites);
      return { previousFavorites };
    },
  });

  return {
    ...favorites,
    add,
    remove,
  };
}
