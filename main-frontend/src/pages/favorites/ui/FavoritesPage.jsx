import React, { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/features/auth";
import {
  addFavorite,
  getFavoritesByUser,
  removeFavorite,
} from "@/features/favorites";
import { useMovies } from "@/features/movies";
import { useUserProfile } from "@/features/user-profile";
import { MovieGrid } from "@/shared/ui";
import "@/shared/styles/pages/Favorites.css";

const localKey = (user = "guest") => `favorites_${user}`;

export default function FavoritesPage() {
  const qc = useQueryClient();
  const { data: movies = [] } = useMovies();
  const { user } = useAuth();
  const username = user?.username ?? null;
  const { data: profile } = useUserProfile();
  const userId = profile?.userId ?? null;

  const {
    data: remoteFavsRaw = [],
    isLoading: favsLoading,
  } = useQuery({
    queryKey: ["favorites", userId],
    queryFn: async () => {
      if (!userId) {
        return [];
      }

      const data = await getFavoritesByUser(userId);
      return Array.isArray(data) ? data.map((item) => item.movieId) : [];
    },
    enabled: !!username && !!userId,
    staleTime: 30000,
  });

  const addMut = useMutation({
    mutationFn: (movieId) => addFavorite(userId, movieId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["favorites", userId] }),
  });

  const delMut = useMutation({
    mutationFn: (movieId) => removeFavorite(userId, movieId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["favorites", userId] }),
  });

  const [localFavs, setLocalFavs] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem(localKey()) || "[]");
    } catch {
      return [];
    }
  });

  useEffect(() => {
    if (!username) {
      try {
        localStorage.setItem(localKey(), JSON.stringify(localFavs));
      } catch {}
    }
  }, [localFavs, username]);

  const favIds = username && userId ? remoteFavsRaw : localFavs;
  const favMovies = movies.filter((movie) => favIds.includes(movie.id));

  const toggle = (movieId) => {
    if (username && userId) {
      if (remoteFavsRaw.includes(movieId)) {
        delMut.mutate(movieId);
      } else {
        addMut.mutate(movieId);
      }
      return;
    }

    setLocalFavs((prev) =>
      prev.includes(movieId)
        ? prev.filter((value) => value !== movieId)
        : [movieId, ...prev]
    );
  };

  return (
    <div className="container favorites-page">
      <h1>Избранное</h1>

      {username && favsLoading && <div className="loading">Загрузка...</div>}

      {favMovies.length === 0 ? (
        <div className="empty">Пусто</div>
      ) : (
        <MovieGrid movies={favMovies} onToggleFavorite={toggle} />
      )}
    </div>
  );
}
