import { useQuery } from "@tanstack/react-query";
import { getAllGenres } from "@/features/movies/api/moviesApi";

export const useGenres = () =>
  useQuery({
    queryKey: ["genres"],
    queryFn: getAllGenres,
    staleTime: 1000 * 60 * 10,
  });
