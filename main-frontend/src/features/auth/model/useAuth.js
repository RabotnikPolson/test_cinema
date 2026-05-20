// src/hooks/useAuth.js
import React, {
  createContext,
  useContext,
  useEffect,
  useState,
  useCallback,
} from "react";

import http from "@/shared/api/http-client";
import {
  login as apiLogin,
  register as apiRegister,
  logout as apiLogout,
  refresh as apiRefresh,
} from "@/features/auth/api/authApi";

const AuthContext = createContext(null);

const LS_ACCESS = "accessToken";
const LS_REFRESH = "refreshToken";
const LS_USER = "authUser";

function safeGet(key) {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function safeSet(key, value) {
  try {
    if (value === null || value === undefined) {
      localStorage.removeItem(key);
    } else {
      localStorage.setItem(key, value);
    }
  } catch {}
}

function readAuthFromStorage() {
  const accessToken = safeGet(LS_ACCESS);
  const refreshToken = safeGet(LS_REFRESH);
  const rawUser = safeGet(LS_USER);

  let user = null;
  if (rawUser) {
    try {
      user = JSON.parse(rawUser);
    } catch {
      user = null;
    }
  }

  return {
    accessToken: accessToken || null,
    refreshToken: refreshToken || null,
    user,
  };
}

function isAdminProfile(profile, email) {
  if (!profile && !email) return false;

  if (typeof email === "string") {
    const normalized = email.trim().toLowerCase();
    if (["orka@example.com", "admin@example.com", "root@example.com"].includes(normalized)) {
      return true;
    }
  }

  const maybe = profile || {};
  const adminFlags = [
    maybe.isAdmin,
    maybe.admin,
    maybe.is_admin,
    maybe.role,
    maybe.roles,
    maybe.authorities,
    maybe.permissions,
  ];

  for (const item of adminFlags) {
    if (typeof item === "boolean" && item === true) return true;
    if (typeof item === "string" && item.toLowerCase().includes("admin")) return true;
    if (Array.isArray(item) && item.some((v) => String(v).toLowerCase().includes("admin"))) return true;
    if (item && typeof item === "object") {
      const values = Object.values(item);
      if (values.some((v) => String(v || "").toLowerCase().includes("admin"))) return true;
    }
  }

  return false;
}

async function fetchProfileApi() {
  const { data } = await http.get("/profile/me");
  const username = data.nickname || data.username || data.email;
  return {
    username,
    email: data.email,
    avatarUrl: data.avatarUrl || null,
    raw: data,
  };
}

export function AuthProvider({ children }) {
  const [{ user, accessToken, refreshToken }, setAuthState] = useState(
    () => readAuthFromStorage()
  );
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (accessToken) {
      http.defaults.headers.common["Authorization"] = `Bearer ${accessToken}`;
    } else {
      try {
        delete http.defaults.headers.common.Authorization;
      } catch {}
    }
  }, [accessToken]);

  const writeAuth = useCallback((next) => {
    if (!next || !next.accessToken || !next.user) {
      safeSet(LS_ACCESS, null);
      safeSet(LS_REFRESH, null);
      safeSet(LS_USER, null);
      setAuthState({ user: null, accessToken: null, refreshToken: null });
      return;
    }

    safeSet(LS_ACCESS, next.accessToken);
    safeSet(LS_REFRESH, next.refreshToken || "");
    safeSet(LS_USER, JSON.stringify(next.user));

    setAuthState({
      user: next.user,
      accessToken: next.accessToken,
      refreshToken: next.refreshToken || null,
    });
  }, []);

  const loadProfileWithToken = useCallback(
    async (tokens) => {
      const { accessToken: at, refreshToken: rt } = tokens;

      safeSet(LS_ACCESS, at);
      http.defaults.headers.common["Authorization"] = `Bearer ${at}`;

      const profile = await fetchProfileApi();

      const userObj = {
        username: profile.username,
        email: profile.email,
        avatarUrl: profile.avatarUrl,
        profile: profile.raw,
      };

      writeAuth({
        user: userObj,
        accessToken: at,
        refreshToken: rt || null,
      });

      try {
        window.dispatchEvent(
          new CustomEvent("app:login", {
            detail: { username: userObj.username },
          })
        );
      } catch {}

      return userObj;
    },
    [writeAuth]
  );

  const login = useCallback(
    async ({ email, password }) => {
      setIsLoading(true);
      setError(null);
      try {
        const res = await apiLogin({ email, password });
        await loadProfileWithToken({
          accessToken: res.accessToken,
          refreshToken: res.refreshToken,
        });
      } catch (e) {
        setError(e);
        throw e;
      } finally {
        setIsLoading(false);
      }
    },
    [loadProfileWithToken]
  );

  const register = useCallback(
    async ({ email, username, password }) => {
      setIsLoading(true);
      setError(null);
      try {
        const res = await apiRegister({ email, username, password });
        await loadProfileWithToken({
          accessToken: res.accessToken,
          refreshToken: res.refreshToken,
        });
      } catch (e) {
        setError(e);
        throw e;
      } finally {
        setIsLoading(false);
      }
    },
    [loadProfileWithToken]
  );

  const logout = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      await apiLogout();
    } catch {} finally {
      setIsLoading(false);
    }

    writeAuth(null);

    try {
      window.dispatchEvent(new CustomEvent("app:logout"));
    } catch {}
  }, [writeAuth]);

  const refresh = useCallback(async () => {
    if (!refreshToken) return;
    try {
      const res = await apiRefresh(refreshToken);
      writeAuth({
        user,
        accessToken: res.accessToken,
        refreshToken: res.refreshToken,
      });
    } catch (e) {
      await logout();
    }
  }, [refreshToken, user, writeAuth, logout]);

  useEffect(() => {
    const handler = (e) => {
      if (!e.key || [LS_ACCESS, LS_REFRESH, LS_USER].includes(e.key)) {
        const next = readAuthFromStorage();
        setAuthState(next);

        if (!next.user) {
          try {
            window.dispatchEvent(new CustomEvent("app:logout"));
          } catch {}
        } else {
          try {
            window.dispatchEvent(
              new CustomEvent("app:login", {
                detail: { username: next.user.username },
              })
            );
          } catch {}
        }
      }
    };

    window.addEventListener("storage", handler);
    return () => window.removeEventListener("storage", handler);
  }, []);

  const isAdmin = Boolean(
    isAdminProfile(user?.profile, user?.email || user?.username) ||
    isAdminProfile(user?.raw, user?.email || user?.username) ||
    isAdminProfile(user?.roles, user?.email || user?.username)
  );

  const value = {
    user,
    isAuthenticated: !!user,
    isAdmin,
    isLoading,
    error,
    login,
    register,
    logout,
    refresh,
  };

  // БЕЗ JSX: чистый JS
  return React.createElement(AuthContext.Provider, { value }, children);
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return ctx;
}
