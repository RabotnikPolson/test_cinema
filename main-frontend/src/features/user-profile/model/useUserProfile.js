import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/features/auth";
import http from "@/shared/api/http-client";

export const useUserProfile = (username) => {
  const qc = useQueryClient();
  const { user } = useAuth();
  const resolvedUsername = username || user?.username || null;
  const isMe = !username || user?.username === username;
  const key = ["user", resolvedUsername || "me", "profile"];

  const query = useQuery({
    queryKey: key,
    queryFn: async () => {
      if (!resolvedUsername && !isMe) {
        return null;
      }

      const url = isMe
        ? "/profile/me"
        : `/profile/${encodeURIComponent(resolvedUsername)}`;
      const res = await http.get(url);
      return res.data;
    },
    enabled: isMe ? !!user : !!resolvedUsername,
  });

  const mutation = useMutation({
    mutationFn: async (payload) => {
      const res = await http.put("/profile/me", payload);
      return res.data;
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: key });
    },
  });

  return {
    ...query,
    saveProfile: mutation.mutateAsync,
    saveStatus: mutation.status,
    saveError: mutation.error,
  };
};
