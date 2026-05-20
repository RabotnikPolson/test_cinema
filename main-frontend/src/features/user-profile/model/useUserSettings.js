import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/features/auth";
import http from "@/shared/api/http-client";

export const useUserSettings = (username) => {
  const qc = useQueryClient();
  const { user } = useAuth();
  const resolvedUsername = username || user?.username || null;
  const isMe = !!resolvedUsername && user?.username === resolvedUsername;
  const key = ["user", resolvedUsername || "me", "settings"];

  const query = useQuery({
    queryKey: key,
    queryFn: async () => {
      if (!isMe) {
        return null;
      }

      const res = await http.get("/settings/me");
      return res.data;
    },
    enabled: isMe,
  });

  const mutation = useMutation({
    mutationFn: async (payload) => {
      const res = await http.put("/settings/me", payload);
      return res.data;
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: key });
    },
  });

  return {
    ...query,
    save: mutation.mutateAsync,
    saveStatus: mutation.status,
    saveError: mutation.error,
  };
};
