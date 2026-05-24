import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useMutation } from "@tanstack/react-query";
import { Eye, EyeOff, Lock, Mail, User2 } from "lucide-react";
import { useAuth } from "@/features/auth";
import "@/pages/auth/ui/Auth.css";

export default function RegisterPage() {
  const nav = useNavigate();
  const { register } = useAuth();
  const [form, setForm] = useState({
    email: "",
    username: "",
    password: "",
  });
  const [showPassword, setShowPassword] = useState(false);

  const mutation = useMutation({
    mutationFn: (payload) => register(payload),
    onSuccess: () => {
      nav("/", { replace: true });
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
              <h1 className="auth-title display">Регистрация</h1>
              <p className="auth-subtitle body">Создайте аккаунт и сохраните персональные подборки.</p>
            </div>

            {mutation.isError ? (
              <div className="auth-error">Не удалось создать аккаунт. Попробуйте ещё раз.</div>
            ) : null}

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
                <label className="form-label label">Логин</label>
                <div className="input-wrapper">
                  <User2 size={18} className="input-icon" />
                  <input
                    className="auth-input"
                    type="text"
                    name="username"
                    placeholder="username"
                    value={form.username}
                    onChange={handleChange}
                    autoComplete="username"
                    minLength={3}
                    maxLength={50}
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
                    autoComplete="new-password"
                    minLength={6}
                    required
                  />
                  <button
                    type="button"
                    className="password-toggle"
                    onClick={() => setShowPassword((value) => !value)}
                    aria-label={showPassword ? "Скрыть пароль" : "Показать пароль"}
                  >
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
              </div>

              <button className="auth-button btn btn-primary" type="submit" disabled={mutation.isPending}>
                {mutation.isPending ? "Создаём..." : "Создать аккаунт"}
              </button>
            </form>

            <div className="auth-footer">
              <p className="auth-switch body">
                Уже есть аккаунт?{" "}
                <Link to="/login" className="auth-link">
                  Войти
                </Link>
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
