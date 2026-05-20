import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createReview,
  deleteReview,
  listByMovie,
  updateReview,
} from "@/features/reviews/api/reviewsApi";
import { rateMovie } from "@/features/reviews/api/ratingsApi";

const reviewKeys = {
  all: ['reviews'],
  movie: (movieId) => [...reviewKeys.all, 'movie', movieId],
  list: (movieId, page, size) => [...reviewKeys.movie(movieId), { page, size }]
};

export function useReviewsByMovie(movieId, page = 0, size = 5) {
  return useQuery({
    queryKey: reviewKeys.list(movieId, page, size),
    enabled: !!movieId,
    queryFn: () => listByMovie(movieId, { page, size, sort: "createdAt,desc" }),
  });
}

export function useReviewMutations(movieId) {
  const qc = useQueryClient();

  const invalidate = async () => {
    if (!movieId) return;
    await qc.invalidateQueries({ queryKey: reviewKeys.movie(movieId) });
  };

  const create = useMutation({
    mutationFn: async ({ content, score }) => {
      await rateMovie(movieId, score);
      return createReview({ movieId, content });
    },
    onSuccess: invalidate,
  });

  const update = useMutation({
    mutationFn: async ({ id, content, score }) => {
      await rateMovie(movieId, score);
      return updateReview(id, { content });
    },
    onSuccess: invalidate,
  });

  const remove = useMutation({
    mutationFn: async (id) => deleteReview(id),
    onSuccess: invalidate,
  });

  return {
    createReview: create,
    updateReview: update,
    deleteReview: remove, 
  };
}
