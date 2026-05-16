import { useQuery, useMutation } from "@tanstack/react-query";
import http from "@/shared/api/http-client";

export const useSubscription = username => {
  const q = useQuery({
    queryKey: ["user", username, "subscription"],
    queryFn: async () => {
      const res = await http.get(
        `/users/${encodeURIComponent(username)}/subscription`
      );
      return res.data;
    },
    enabled: !!username,
  });

  const subscribe = useMutation({
    mutationFn: async payload => {
      const res = await http.post(
        `/users/${encodeURIComponent(username)}/subscription`,
        payload
      );
      return res.data;
    },
  });

  const cancel = useMutation({
    mutationFn: async () => {
      const res = await http.delete(
        `/users/${encodeURIComponent(username)}/subscription`
      );
      return res.data;
    },
  });

  return { ...q, subscribe: subscribe.mutateAsync, cancel: cancel.mutateAsync };
};
