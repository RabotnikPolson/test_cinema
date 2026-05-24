import React from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "@/features/auth/model/useAuth";

export default function AdminRoute({ children }) {
  const { isAuthenticated, isAdmin, isLoading } = useAuth();

  if (isLoading) {
    return <div className="loading">Загрузка...</div>;
  }

  if (!isAuthenticated || !isAdmin) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}
