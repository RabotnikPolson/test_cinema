import { useQuery } from "@tanstack/react-query";
import { getMovieStream } from "@/features/player/api/playerApi";

export function useMovieStream(movieId) {
  return useQuery({
    queryKey: ["movie-stream", movieId],
    queryFn: () => getMovieStream(movieId),
    enabled: Boolean(movieId),
    staleTime: 1000 * 60 * 30,
  });
}
