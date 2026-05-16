import http from "@/shared/api/http-client";

// Регистрация: email + username + password
// POST /auth/register
export async function register({ email, username, password }) {
  const { data } = await http.post("/auth/register", {
    email,
    username,
    password,
  });
  // ожидаем AuthResponse: { accessToken, refreshToken, tokenType }
  return data;
}

// Логин: email + password
// POST /auth/login
export async function login({ email, password }) {
  const { data } = await http.post("/auth/login", {
    email,
    password,
  });
  return data;
}

// Обновление токена — пока просто функция, автологику можно прикрутить позже
export async function refresh(refreshToken) {
  const { data } = await http.post("/auth/refresh", {
    refreshToken,
  });
  return data;
}

// Logout, если реализован на бэке
export async function logout() {
  try {
    await http.post("/auth/logout");
  } catch {
    // можно игнорировать сетевые/401
  }
}
