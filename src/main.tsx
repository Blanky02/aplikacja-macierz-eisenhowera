import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { I18nProvider } from "./i18n";
import { AuthProvider, useAuth } from "./auth";
import { Navbar } from "./components/Navbar";
import { AuthPage } from "./pages/AuthPage";
import { MatrixPage } from "./pages/MatrixPage";
import { StatsPage } from "./pages/StatsPage";
import "./styles.css";

function Protected({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

export default function App() {
  return (
    <I18nProvider>
      <AuthProvider>
        <BrowserRouter>
          <Navbar />
          <main className="app-main">
            <Routes>
              <Route path="/login" element={<AuthPage mode="login" />} />
              <Route path="/register" element={<AuthPage mode="register" />} />
              <Route path="/" element={<Protected><MatrixPage /></Protected>} />
              <Route path="/stats" element={<Protected><StatsPage /></Protected>} />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </main>
        </BrowserRouter>
      </AuthProvider>
    </I18nProvider>
  );
}

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
