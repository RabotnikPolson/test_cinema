import React from "react";
import { Link, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  BarChart3,
  CreditCard,
  Film,
  FilmIcon,
  Heart,
  History,
  Home,
  Settings,
  ShoppingBag,
  User,
} from "lucide-react";
import { useAuth } from "@/features/auth";
import "@/widgets/sidebar/ui/Sidebar.css";

function matchesPath(pathname, target) {
  if (target === "/") {
    return pathname === "/";
  }

  return pathname === target || pathname.startsWith(`${target}/`);
}

export default function Sidebar({ isOpen = false }) {
  const { isAdmin } = useAuth();
  const location = useLocation();
  const { t } = useTranslation();

  const items = [
    { to: "/", label: t("nav.home"), icon: Home },
    { to: "/genres", label: t("nav.allMovies"), icon: Film },
    { to: "/favorites", label: t("nav.favorites"), icon: Heart },
    { to: "/history", label: t("nav.history"), icon: History },
    { to: "/profile", label: t("nav.profile"), icon: User },
    { to: "/settings", label: t("nav.settings"), icon: Settings },
    { to: "/subscription", label: t("nav.subscription"), icon: CreditCard },
    { to: "/shop", label: t("nav.shop"), icon: ShoppingBag },
  ];

  if (isAdmin) {
    items.push({ to: "/admin/movies", label: t("nav.adminMovies"), icon: FilmIcon });
    items.push({ to: "/analytics", label: t("nav.analytics"), icon: BarChart3 });
  }

  return (
    <nav className={`sidebar ${isOpen ? "open" : ""}`} aria-label={t("nav.ariaMain")}>
      <div className="sidebar-content">
        <ul className="sidebar-nav">
          {items.map((item) => {
            const Icon = item.icon;
            const isActive = matchesPath(location.pathname, item.to);

            return (
              <li key={item.to} className="sidebar-item">
                <Link className={`sidebar-link ${isActive ? "active" : ""}`} to={item.to}>
                  <Icon size={20} className="sidebar-icon" />
                  <span className="sidebar-label">{item.label}</span>
                </Link>
              </li>
            );
          })}
        </ul>
      </div>
    </nav>
  );
}
