// src/hooks/useTheme.js
import React, { createContext, useContext, useEffect, useState } from "react";

const Ctx = createContext({ theme: "dark", setTheme: () => {} });

export function ThemeProvider({ children }) {
  const system =
    typeof window !== "undefined" &&
    window.matchMedia?.("(prefers-color-scheme: light)").matches
      ? "light"
      : "dark";

  const [theme, setTheme] = useState(() => {
    try {
      return localStorage.getItem("theme") || system;
    } catch {
      return system;
    }
  });

  useEffect(() => {
    document.documentElement.setAttribute("data-theme", theme);
    try {
      localStorage.setItem("theme", theme);
    } catch {}
  }, [theme]);

  return React.createElement(Ctx.Provider, { value: { theme, setTheme } }, children);
}

export const useTheme = () => useContext(Ctx);