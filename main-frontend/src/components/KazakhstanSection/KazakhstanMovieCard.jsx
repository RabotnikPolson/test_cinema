import { MovieCard } from "@/entities/movie";

export default function KazakhstanMovieCard({ movie }) {
  return <MovieCard movie={movie} showFavorite={false} />;
}
