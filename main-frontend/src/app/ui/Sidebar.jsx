import React from "react";
import { Link, useLocation } from "react-router-dom";
import { useAuth } from "@/features/auth";
import {
  Home,
  Film,
  Heart,
  History,
  User,
  Settings,
  CreditCard,
  Plus,
  BarChart3,
  FilmIcon
} from "lucide-react";
import "@/shared/styles/components/Sidebar.css";

export default function Sidebar({ isOpen = false, onClose }) {
  const { isAdmin } = useAuth();
  const location = useLocation();

  const items = [
    { to: "/", label: "Главная", icon: Home },
    { to: "/genres", label: "Жанры", icon: Film },
    { to: "/favorites", label: "Избранное", icon: Heart },
    { to: "/history", label: "История", icon: History },
    { to: "/profile", label: "Профиль", icon: User },
    { to: "/settings", label: "Настройки", icon: Settings },
    { to: "/subscription", label: "Подписка", icon: CreditCard },
  ];

  if (isAdmin) {
    items.push({ to: "/admin/movies", label: "Фильмы", icon: FilmIcon });
    items.push({ to: "/add-movie", label: "Добавить", icon: Plus });
    items.push({ to: "/analytics", label: "Аналитика", icon: BarChart3 });
  }

  return (
    <nav className={`sidebar ${isOpen ? 'open' : ''}`}>
      <div className="sidebar-content">
        <ul className="sidebar-nav">
          {items.map((item) => {
            const Icon = item.icon;
            const isActive = location.pathname === item.to;

            return (
              <li key={item.to} className="sidebar-item">
                <Link
                  className={`sidebar-link ${isActive ? 'active' : ''}`}
                  to={item.to}
                >
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
