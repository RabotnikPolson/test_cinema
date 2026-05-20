import React, { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useMutation } from "@tanstack/react-query";
import { useAuth } from "@/features/auth";
import { Eye, EyeOff, Mail, Lock } from "lucide-react";
import "@/shared/styles/pages/Auth.css";

export default function LoginPage() {
  const nav = useNavigate();
  const loc = useLocation();
  const { login } = useAuth();
  const [form, setForm] = useState({
    email: "",
    password: "",
  });
  const [showPassword, setShowPassword] = useState(false);

  const from = loc.state?.from?.pathname || "/";

  const mutation = useMutation({
    mutationFn: (payload) => login(payload),
    onSuccess: () => {
      nav(from, { replace: true });
    },
  });

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    mutation.mutate({ ...form });
  };

  return (
    <div className="auth-page">
      <div className="auth-backdrop">
        <div className="auth-container">
          <div className="auth-card glass">
            <div className="auth-header">
              <h1 className="auth-title display">Вход</h1>
              <p className="auth-subtitle body">Добро пожаловать обратно в CineVerse</p>
            </div>

            {mutation.isError && (
              <div className="auth-error">
                <span className="error-icon">⚠️</span>
                Не удалось войти. Проверьте email и пароль.
              </div>
            )}

            <form onSubmit={handleSubmit} className="auth-form">
              <div className="form-group">
                <label className="form-label label">Email</label>
                <div className="input-wrapper">
                  <Mail size={18} className="input-icon" />
                  <input
                    className="auth-input"
                    type="email"
                    name="email"
                    placeholder="your@email.com"
                    value={form.email}
                    onChange={handleChange}
                    autoComplete="email"
                    required
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label label">Пароль</label>
                <div className="input-wrapper">
                  <Lock size={18} className="input-icon" />
                  <input
                    className="auth-input"
                    type={showPassword ? "text" : "password"}
                    name="password"
                    placeholder="••••••••"
                    value={form.password}
                    onChange={handleChange}
                    autoComplete="current-password"
                    required
                  />
                  <button
                    type="button"
                    className="password-toggle"
                    onClick={() => setShowPassword(!showPassword)}
                    aria-label={showPassword ? "Скрыть пароль" : "Показать пароль"}
                  >
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
              </div>

              <button className="auth-button btn btn-primary" type="submit" disabled={mutation.isPending}>
                {mutation.isPending ? "Входим..." : "Войти"}
              </button>
            </form>

            <div className="auth-footer">
              <p className="auth-switch body">
                Нет аккаунта?{" "}
                <Link to="/register" state={{ from }} className="auth-link">
                  Зарегистрироваться
                </Link>
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
