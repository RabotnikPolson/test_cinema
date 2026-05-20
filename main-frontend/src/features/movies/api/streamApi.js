import http from "@/shared/api/http-client";

export async function getStream(movieId, params = {}) {
  const { data } = await http.get(`/stream/${movieId}`, { params });
  return data;
}

