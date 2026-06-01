import React, { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useMutation } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { Eye, EyeOff, Lock, Mail } from "lucide-react";
import { useAuth } from "@/features/auth";
import "@/pages/auth/ui/Auth.css";

export default function LoginPage() {
  const { t } = useTranslation();
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
              <h1 className="auth-title display">{t("auth.loginTitle")}</h1>
              <p className="auth-subtitle body">{t("auth.loginSubtitle")}</p>
            </div>

            {mutation.isError ? (
              <div className="auth-error">{t("auth.loginError")}</div>
            ) : null}

            <form onSubmit={handleSubmit} className="auth-form">
              <div className="form-group">
                <label className="form-label label">{t("auth.emailLabel")}</label>
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
                <label className="form-label label">{t("auth.passwordLabel")}</label>
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
                    onClick={() => setShowPassword((value) => !value)}
                    aria-label={showPassword ? t("auth.hidePassword") : t("auth.showPassword")}
                  >
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
              </div>

              <button className="auth-button btn btn-primary" type="submit" disabled={mutation.isPending}>
                {mutation.isPending ? t("auth.loggingIn") : t("auth.loginButton")}
              </button>
            </form>

            <div className="auth-footer">
              <p className="auth-switch body">
                {t("auth.noAccount")}{" "}
                <Link to="/register" state={{ from }} className="auth-link">
                  {t("auth.registerLink")}
                </Link>
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
