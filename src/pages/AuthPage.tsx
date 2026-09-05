import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { useI18n, type TKey } from "../i18n";
import { useAuth } from "../auth";

export function AuthPage({ mode }: { mode: "login" | "register" }) {
  const { t } = useI18n();
  const { login, register } = useAuth();
  const navigate = useNavigate();
  const [tab, setTab] = useState(mode);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    if (password.length < 6) {
      setError(t("shortPassword"));
      return;
    }
    setBusy(true);
    try {
      if (tab === "login") await login(email, password);
      else await register(email, password, name || undefined);
      navigate("/");
    } catch (err) {
      if ((err as Error).message === "email_taken") setError(t("emailTaken"));
      else setError(tab === "login" ? t("loginFailed") : t("registerFailed"));
    } finally {
      setBusy(false);
    }
  };

  const demoLogin = async () => {
    setBusy(true);
    setError(null);
    try {
      await login("demo@matrix.app", "demo1234");
      navigate("/");
    } catch {
      setError(t("loginFailed"));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth-wrap">
      <div className="auth-card">
        <h1>{t("appName")}</h1>
        <p className="subtitle">{t("tagline")}</p>

        <div className="auth-tabs">
          <button className={`auth-tab ${tab === "login" ? "active" : ""}`} onClick={() => { setTab("login"); setError(null); }}>
            {t("login")}
          </button>
          <button className={`auth-tab ${tab === "register" ? "active" : ""}`} onClick={() => { setTab("register"); setError(null); }}>
            {t("register")}
          </button>
        </div>

        {error && <div className="error-banner">{error}</div>}

        <form onSubmit={submit}>
          {tab === "register" && (
            <div className="field">
              <label>{t("name")}</label>
              <input value={name} onChange={(e) => setName(e.target.value)} autoComplete="name" />
            </div>
          )}
          <div className="field">
            <label>{t("email")}</label>
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" />
          </div>
          <div className="field">
            <label>{t("password")}</label>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required autoComplete={tab === "login" ? "current-password" : "new-password"} />
          </div>
          <button className="btn" style={{ width: "100%", justifyContent: "center" }} disabled={busy}>
            {busy ? "…" : tab === "login" ? t("signIn") : t("signUp")}
          </button>
        </form>

        <div className="demo-hint">
          {t("demoHint")}
          <br />
          <button onClick={demoLogin} disabled={busy}>{t("useDemo")} →</button>
        </div>
      </div>
    </div>
  );
}
