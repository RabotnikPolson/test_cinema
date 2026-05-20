import React, { useState, useEffect } from "react";
import { Link, Outlet, useLocation } from "react-router-dom";
import Header from "@/app/ui/Header";
import Sidebar from "@/app/ui/Sidebar";
import Footer from "@/app/ui/Footer";
import "@/shared/styles/layout/AppLayout.css";

export default function AppLayout() {
  const loc = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [isDesktop, setIsDesktop] = useState(window.innerWidth >= 1200);

  const hideUI = /^\/(login|register)(\/|$)/.test(loc.pathname);

  useEffect(() => {
    const handleResize = () => {
      setIsDesktop(window.innerWidth >= 1200);
      if (window.innerWidth >= 1200) {
        setSidebarOpen(false);
      }
    };

    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  const toggleSidebar = () => {
    setSidebarOpen(!sidebarOpen);
  };

  const closeSidebar = () => {
    setSidebarOpen(false);
  };

  return (
    <div className="app-layout">
      {!hideUI && <Header onMenuClick={toggleSidebar} />}

      {!hideUI && (
        <>
          <Sidebar isOpen={sidebarOpen} onClose={closeSidebar} />
          {!isDesktop && sidebarOpen && (
            <div className="sidebar-overlay" onClick={closeSidebar} />
          )}
        </>
      )}

      <main className={`main-content ${!hideUI && isDesktop ? "with-sidebar" : ""}`}>
        <React.Suspense fallback={<div className="loading">Загрузка...</div>}>
          <Outlet />
        </React.Suspense>
        {!hideUI && <Footer />}
      </main>

      {!hideUI && (
        <Link to="/bot" className="chatbot-action" aria-label="Чат-помощник">
          <span className="chatbot-action-text">Чат-помощник</span>
        </Link>
      )}
    </div>
  );
}
