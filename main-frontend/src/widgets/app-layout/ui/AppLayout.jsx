import React, { useEffect, useState } from "react";
import { MessageCircle } from "lucide-react";
import { Link, Outlet, useLocation } from "react-router-dom";
import Footer from "@/widgets/footer/ui/Footer";
import Header from "@/widgets/header/ui/Header";
import Sidebar from "@/widgets/sidebar/ui/Sidebar";
import { WelcomeModal } from "@/shared/ui";
import "@/widgets/app-layout/ui/AppLayout.css";

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
    setSidebarOpen((value) => !value);
  };

  const closeSidebar = () => {
    setSidebarOpen(false);
  };

  return (
    <div className="app-layout">
      {!hideUI && <Header onMenuClick={toggleSidebar} />}

      {!hideUI && (
        <>
          <Sidebar isOpen={sidebarOpen} />
          {!isDesktop && sidebarOpen && (
            <div className="sidebar-overlay open" onClick={closeSidebar} />
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
        <Link to="/bot" className="chatbot-action" aria-label="AI-помощник">
          <MessageCircle size={20} />
          <span className="chatbot-action-text">AI-помощник</span>
        </Link>
      )}
      {!hideUI && <WelcomeModal />}
    </div>
  );
}
