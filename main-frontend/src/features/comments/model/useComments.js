import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  listRootComments,
  getCommentCount,
  getCommentWithReplies,
  createComment,
  updateComment,
  deleteComment,
  reactComment,
} from "@/features/comments/api/commentsApi";

/**
 * Корневые комментарии
 */
export function useRootComments({ movieId, order }) {
  return useQuery({
    queryKey: ["comments", movieId, order],
    queryFn: () => listRootComments({ movieId, order }),
    enabled: !!movieId,
  });
}

/**
 * Ответы к комментарию
 */
export function useReplies(commentId) {
  return useQuery({
    queryKey: ["comment", commentId, "replies"],
    queryFn: () => getCommentWithReplies(commentId),
    enabled: !!commentId,
  });
}

/**
 * Счётчик комментариев
 */
export function useCommentCount(movieId) {
  return useQuery({
    queryKey: ["comments", movieId, "count"],
    queryFn: () => getCommentCount(movieId),
    enabled: !!movieId,
  });
}

/**
 * Мутации
 */
export function useCommentMutations(movieId) {
  const qc = useQueryClient();

  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ["comments", movieId] });
    qc.invalidateQueries({ queryKey: ["comments", movieId, "count"] });
  };

  return {
    create: useMutation({
      mutationFn: createComment,
      onSuccess: invalidate,
    }),
    update: useMutation({
      mutationFn: ({ id, payload }) => updateComment(id, payload),
      onSuccess: invalidate,
    }),
    remove: useMutation({
      mutationFn: deleteComment,
      onSuccess: invalidate,
    }),
    react: useMutation({
      mutationFn: ({ id, emoji }) => reactComment(id, emoji),
      onSuccess: invalidate,
    }),
  };
}
