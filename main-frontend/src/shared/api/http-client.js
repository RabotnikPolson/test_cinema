import axios from "axios";

const BACKENDS = [
  "http://localhost:8080",
  "https://271e5230940a.ngrok-free.app",
];

const host = window.location.hostname;
const baseURL = host.includes("ngrok") ? BACKENDS[1] : BACKENDS[0];

const http = axios.create({
  baseURL,
  timeout: 10000,
  withCredentials: false,
});

// Separate instance for refresh calls — avoids triggering the 401 interceptor again
const refreshHttp = axios.create({ baseURL, timeout: 10000 });

try {
  const token = localStorage.getItem("accessToken");
  if (token) {
    http.defaults.headers.common.Authorization = `Bearer ${token}`;
  }
} catch {}

http.interceptors.request.use(
  (config) => {
    try {
      const token = localStorage.getItem("accessToken");
      if (token) {
        config.headers = config.headers || {};
        if (!config.headers.Authorization) {
          config.headers.Authorization = `Bearer ${token}`;
        }
      }
    } catch {}
    return config;
  },
  (err) => Promise.reject(err)
);

let isRefreshing = false;
let pendingQueue = [];

function processQueue(error, token) {
  pendingQueue.forEach((p) => (error ? p.reject(error) : p.resolve(token)));
  pendingQueue = [];
}

function clearAuthAndRedirect() {
  try {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("authUser");
    localStorage.setItem("app:logout", Date.now().toString());
  } catch {}
  try {
    const path = window.location.pathname;
    if (!path.startsWith("/login") && !path.startsWith("/register")) {
      window.location.href = "/login?expired=1";
    }
  } catch {}
}

http.interceptors.response.use(
  (res) => res,
  async (err) => {
    const original = err.config;
    const status = err?.response?.status;

    // не трогаем не-401 и повторные запросы
    if (status !== 401 || original._retry) {
      return Promise.reject(err);
    }

    const refreshToken = localStorage.getItem("refreshToken");
    if (!refreshToken) {
      clearAuthAndRedirect();
      return Promise.reject(err);
    }

    // если уже идёт обновление — ставим запрос в очередь
    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        pendingQueue.push({ resolve, reject });
      }).then((token) => {
        original.headers.Authorization = `Bearer ${token}`;
        return http(original);
      });
    }

    original._retry = true;
    isRefreshing = true;

    try {
      const { data } = await refreshHttp.post("/auth/refresh", { refreshToken });
      const newAccess = data.accessToken;
      const newRefresh = data.refreshToken;

      localStorage.setItem("accessToken", newAccess);
      if (newRefresh) localStorage.setItem("refreshToken", newRefresh);
      http.defaults.headers.common.Authorization = `Bearer ${newAccess}`;

      processQueue(null, newAccess);
      original.headers.Authorization = `Bearer ${newAccess}`;
      return http(original);
    } catch (refreshErr) {
      processQueue(refreshErr, null);
      clearAuthAndRedirect();
      return Promise.reject(refreshErr);
    } finally {
      isRefreshing = false;
    }
  }
);

export default http;