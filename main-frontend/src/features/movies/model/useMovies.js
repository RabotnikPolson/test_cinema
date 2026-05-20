import { useQuery } from "@tanstack/react-query";
import { mapMovie } from "@/entities/movie";
import { getAllMovies } from "@/features/movies/api/moviesApi";

export const useMovies = () =>
  useQuery({
    queryKey: ["movies"],
    queryFn: async () => {
      const data = await getAllMovies();
      return Array.isArray(data) ? data.map(mapMovie) : [];
    },
    staleTime: 1000 * 60 * 2,
  });
