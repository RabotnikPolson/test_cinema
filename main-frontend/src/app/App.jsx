import React, { Suspense, lazy, useEffect, useRef } from "react";
import { Navigate, Route, Routes, useLocation, useNavigationType } from "react-router-dom";
import { AdminRoute } from "@/features/auth";
import AppLayout from "@/widgets/app-layout/ui/AppLayout";
import { ThemeProvider } from "@/shared/hooks";
import "@/app/styles/theme.css";

const HomePage = lazy(() => import("@/pages/home/ui/HomePage"));
const GenresPage = lazy(() => import("@/pages/genres/ui/GenresPage"));
const AdminMoviesPage = lazy(() => import("@/pages/admin/ui/AdminMoviesPage"));
const AdminAnalyticsPage = lazy(() => import("@/pages/admin/ui/AdminAnalyticsPage"));
const AdminShopManager = lazy(() => import("@/pages/admin/ui/AdminShopManager"));
const UserActivityPage = lazy(() => import("@/pages/admin/ui/UserActivityPage"));
const FavoritesPage = lazy(() => import("@/pages/favorites/ui/FavoritesPage"));
const HistoryPage = lazy(() => import("@/pages/history/ui/HistoryPage"));
const MovieDetailsPage = lazy(() => import("@/pages/movie-details/ui/MovieDetailsPage"));
const MovieWatchPage = lazy(() => import("@/pages/movie-watch/ui/MovieWatchPage"));
const MovieReviewsPage = lazy(() => import("@/pages/movie-reviews/ui/MovieReviewsPage"));
const ChatBotPage = lazy(() => import("@/pages/chat-bot/ui/ChatBotPage"));
const LoginPage = lazy(() => import("@/pages/auth/ui/LoginPage"));
const RegisterPage = lazy(() => import("@/pages/auth/ui/RegisterPage"));
const SettingsPage = lazy(() => import("@/pages/settings/ui/SettingsPage"));
const SubscriptionPage = lazy(() => import("@/pages/subscription/ui/SubscriptionPage"));
const ProfilePage = lazy(() => import("@/pages/user-profile/ui/ProfilePage"));
const ShopPage = lazy(() => import("@/pages/shop/ui/ShopPage"));

// Отключаем браузерное авто-восстановление скролла — управляем им сами
if ("scrollRestoration" in window.history) {
  window.history.scrollRestoration = "manual";
}

function ScrollManager() {
  const location = useLocation();
  const navType = useNavigationType();
  const restoringRef = useRef(false);

  // Сохраняем позицию при скролле — но НЕ во время программного восстановления
  useEffect(() => {
    const key = location.key;
    let rafId;
    const save = () => {
      if (restoringRef.current) return;
      cancelAnimationFrame(rafId);
      rafId = requestAnimationFrame(() => {
        sessionStorage.setItem(`scroll:${key}`, String(Math.round(window.scrollY)));
      });
    };
    window.addEventListener("scroll", save, { passive: true });
    return () => {
      window.removeEventListener("scroll", save);
      cancelAnimationFrame(rafId);
    };
  }, [location.key]);

  // Навигация: восстановить позицию (POP) либо прокрутить наверх (PUSH/REPLACE)
  useEffect(() => {
    if (navType !== "POP") {
      window.scrollTo({ top: 0, behavior: "instant" });
      return undefined;
    }

    const targetY = Number(sessionStorage.getItem(`scroll:${location.key}`));
    if (!targetY) return undefined;

    restoringRef.current = true;
    let cancelled = false;
    const startedAt = performance.now();

    const stop = () => {
      cancelled = true;
      restoringRef.current = false;
      window.removeEventListener("wheel", stop);
      window.removeEventListener("touchstart", stop);
      window.removeEventListener("keydown", stop);
    };

    // Если пользователь сам начал листать — прекращаем восстановление, не мешаем ему
    window.addEventListener("wheel", stop, { passive: true });
    window.addEventListener("touchstart", stop, { passive: true });
    window.addEventListener("keydown", stop);

    // Крутим пока реально не достигнем цели (контент грузится асинхронно), максимум 3с
    const tick = () => {
      if (cancelled) return;
      window.scrollTo({ top: targetY, behavior: "instant" });
      const reached = Math.abs(window.scrollY - targetY) <= 2;
      if (reached || performance.now() - startedAt > 3000) {
        stop();
        return;
      }
      requestAnimationFrame(tick);
    };
    requestAnimationFrame(tick);

    return stop;
  }, [location.key, navType]);

  return null;
}

export default function App() {
  return (
    <ThemeProvider>
      <ScrollManager />
      <Suspense fallback={<div style={{ padding: 20 }}>Загрузка...</div>}>
        <Routes>
          <Route element={<AppLayout />}>
            <Route index element={<HomePage />} />
            <Route path="genres" element={<GenresPage />} />
            <Route path="favorites" element={<FavoritesPage />} />
            <Route path="history" element={<HistoryPage />} />
            <Route path="profile" element={<ProfilePage />} />
            <Route path="settings" element={<SettingsPage />} />
            <Route path="subscription" element={<SubscriptionPage />} />
            <Route path="shop" element={<ShopPage />} />

            <Route path="movie/:id" element={<MovieDetailsPage />} />
            <Route path="movie/:id/watch" element={<MovieWatchPage />} />
            <Route path="movie/:id/reviews" element={<MovieReviewsPage />} />
            <Route path="bot" element={<ChatBotPage />} />

            <Route
              path="admin/movies"
              element={
                <AdminRoute>
                  <AdminMoviesPage />
                </AdminRoute>
              }
            />
            <Route
              path="analytics"
              element={
                <AdminRoute>
                  <AdminAnalyticsPage />
                </AdminRoute>
              }
            />
            <Route
              path="admin/shop"
              element={
                <AdminRoute>
                  <AdminShopManager />
                </AdminRoute>
              }
            />
            <Route path="activity/:username" element={<UserActivityPage />} />

            <Route path="login" element={<LoginPage />} />
            <Route path="register" element={<RegisterPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </Suspense>
    </ThemeProvider>
  );
}

//deleted some sources