import http from "@/shared/api/http-client";

export const getAllMovies = async () => {
  const res = await http.get("/movies");
  return res.data;
};

export const getMovie = async (id) => {
  const res = await http.get(`/movies/${id}`);
  return res.data;
};

export const addFromKinopoisk = async (kinopoiskId) => {
  const res = await http.post("/movies/addFromKinopoisk", null, {
    params: { kinopoiskId },
  });
  return res.data;
};

export const deleteMovie = async (id) => {
  if (!id) {
    throw new Error("Нет ID фильма для удаления");
  }
  const res = await http.delete(`/movies/${id}`);
  return res.data;
};

export const getAllGenres = async () => {
  const res = await http.get("/genres");
  return res.data;
};
