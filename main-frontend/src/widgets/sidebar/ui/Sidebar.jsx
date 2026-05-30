import React from "react";
import { Link, useLocation } from "react-router-dom";
import {
  BarChart3,
  Bot,
  CreditCard,
  Film,
  FilmIcon,
  Heart,
  History,
  Home,
  Plus,
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

  const items = [
    { to: "/", label: "Главная", icon: Home },
    { to: "/genres", label: "Все фильмы", icon: Film },
    { to: "/favorites", label: "Избранное", icon: Heart },
    { to: "/history", label: "История", icon: History },
    { to: "/profile", label: "Профиль", icon: User },
    { to: "/settings", label: "Настройки", icon: Settings },
    { to: "/subscription", label: "Подписка", icon: CreditCard },
    { to: "/shop", label: "Shop", icon: ShoppingBag },
    { to: "/bot", label: "AI-помощник", icon: Bot },
  ];

  if (isAdmin) {
    items.push({ to: "/admin/movies", label: "Фильмы", icon: FilmIcon });
    items.push({ to: "/add-movie", label: "Импорт", icon: Plus });
    items.push({ to: "/analytics", label: "Аналитика", icon: BarChart3 });
  }

  return (
    <nav className={`sidebar ${isOpen ? "open" : ""}`} aria-label="Основная навигация">
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
