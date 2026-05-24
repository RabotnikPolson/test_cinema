import React, { useEffect, useState } from "react";
import { Heart } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/features/auth";
import { getFavoritesByUser } from "@/features/favorites";
import { useMovies } from "@/features/movies";
import { useUserProfile } from "@/features/user-profile";
import { MovieGrid } from "@/shared/ui";
import { guestFavoritesChangedEvent, readGuestFavorites } from "@/shared/utils";
import "@/pages/favorites/ui/Favorites.css";

export default function FavoritesPage() {
  const { data: movies = [] } = useMovies();
  const { user } = useAuth();
  const username = user?.username ?? null;
  const { data: profile } = useUserProfile();
  const userId = profile?.userId ?? null;
  const [guestFavIds, setGuestFavIds] = useState(() => readGuestFavorites());

  useEffect(() => {
    if (username) {
      return undefined;
    }

    const syncGuestFavorites = (event) => {
      setGuestFavIds(event?.detail?.ids || readGuestFavorites());
    };

    window.addEventListener(guestFavoritesChangedEvent, syncGuestFavorites);
    return () => window.removeEventListener(guestFavoritesChangedEvent, syncGuestFavorites);
  }, [username]);

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
      return Array.isArray(data) ? data.map((item) => String(item.movieId)) : [];
    },
    enabled: !!username && !!userId,
    staleTime: 30000,
  });

  const favIds = username && userId ? remoteFavsRaw : guestFavIds;
  const favMovies = movies.filter((movie) => favIds.includes(String(movie.id)));

  return (
    <div className="container favorites-page">
      <div className="favorites-hero">
        <div>
          <div className="favorites-eyebrow">Ваша коллекция</div>
          <h1>Избранное</h1>
          <p>
            Сохраняйте фильмы, к которым хочется вернуться вечером или показать друзьям.
          </p>
        </div>
        <div className="favorites-count">
          <Heart size={18} />
          <span>{favMovies.length}</span>
        </div>
      </div>

      {username && favsLoading ? <div className="favorites-state">Загрузка...</div> : null}

      {!favsLoading && favMovies.length === 0 ? (
        <div className="favorites-empty">
          <Heart size={36} />
          <h2>Пока пусто</h2>
          <p>Нажмите на кнопку добавления на любой карточке, и фильм появится здесь.</p>
        </div>
      ) : null}

      {favMovies.length > 0 ? <MovieGrid movies={favMovies} /> : null}
    </div>
  );
}
