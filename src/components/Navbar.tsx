import { useEffect, useState } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import { useI18n, type Lang } from "../i18n";
import { useAuth } from "../auth";

export function Navbar() {
  const { t, lang, setLang } = useI18n();
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [theme, setTheme] = useState<"dark" | "light">(() =>
    (localStorage.getItem("em_theme") as "dark" | "light") || "dark"
  );

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem("em_theme", theme);
  }, [theme]);

  return (
    <nav className="navbar">
      <div className="navbar-inner">
        <Link to="/" className="brand">
          <span className="brand-logo">
            <span /><span /><span /><span />
          </span>
          {t("appName")}
        </Link>

        {user && (
          <div className="nav-links">
            <NavLink to="/" end className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}>
              {t("matrix")}
            </NavLink>
            <NavLink to="/stats" className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}>
              {t("stats")}
            </NavLink>
          </div>
        )}

        <div className="nav-spacer" />

        {user && <span className="nav-user" title={user.email}>{user.name}</span>}

        <button
          className="icon-btn"
          title={t("lang")}
          onClick={() => setLang((lang === "pl" ? "en" : "pl") as Lang)}
        >
          {lang === "pl" ? "🇵🇱" : "🇬🇧"}
        </button>
        <button
          className="icon-btn"
          title={t("theme")}
          onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
        >
          {theme === "dark" ? "🌙" : "☀️"}
        </button>

        {user && (
          <button
            className="icon-btn"
            title={t("logout")}
            onClick={() => {
              logout();
              navigate("/login");
            }}
          >
            ↪
          </button>
        )}
      </div>
    </nav>
  );
}
