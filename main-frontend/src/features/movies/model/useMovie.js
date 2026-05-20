import { useQuery } from "@tanstack/react-query";
import { mapMovie } from "@/entities/movie";
import { getMovie } from "@/features/movies/api/moviesApi";

export const useMovie = (id) =>
  useQuery({
    queryKey: ["movie", id],
    queryFn: async () => {
      if (!id) throw new Error("No id provided");
      const data = await getMovie(id);
      return mapMovie(data);
    },
    enabled: !!id,
    staleTime: 1000 * 60 * 5,
  });
