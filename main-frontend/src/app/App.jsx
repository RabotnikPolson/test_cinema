import React, { Suspense, lazy } from "react";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { AdminRoute } from "@/features/auth";
import { AppLayout } from "@/app/ui";
import { ThemeProvider } from "@/shared/hooks";
import "@/shared/styles/theme.css";

const HomePage = lazy(() => import("@/pages/home/ui/HomePage"));
const GenresPage = lazy(() => import("@/pages/genres/ui/GenresPage"));
const AddMoviePage = lazy(() => import("@/pages/admin/ui/AddMoviePage"));
const AdminMoviesPage = lazy(() => import("@/pages/admin/ui/AdminMoviesPage"));
const AdminAnalyticsPage = lazy(() => import("@/pages/admin/ui/AdminAnalyticsPage"));
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

function ScrollToTop() {
  const location = useLocation();

  React.useEffect(() => {
    window.scrollTo(0, 0);
  }, [location.pathname]);

  return null;
}

export default function App() {
  return (
    <ThemeProvider>
      <ScrollToTop />
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

            <Route path="movie/:id" element={<MovieDetailsPage />} />
            <Route path="movie/:id/watch" element={<MovieWatchPage />} />
            <Route path="movie/:id/reviews" element={<MovieReviewsPage />} />
            <Route path="bot" element={<ChatBotPage />} />

            <Route
              path="add-movie"
              element={
                <AdminRoute>
                  <AddMoviePage />
                </AdminRoute>
              }
            />
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
