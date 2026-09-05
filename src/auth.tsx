import { createContext, useContext, useState, type ReactNode } from "react";
import { api, clearSession, getUser, setSession, type AuthUser } from "./api";

interface AuthCtx {
  user: AuthUser | null;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, name?: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthCtx | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => getUser());

  const login = async (email: string, password: string) => {
    const res = await api.login(email, password);
    setSession(res.token, res.user);
    setUser(res.user);
  };

  const register = async (email: string, password: string, name?: string) => {
    const res = await api.register(email, password, name);
    setSession(res.token, res.user);
    setUser(res.user);
  };

  const logout = () => {
    clearSession();
    setUser(null);
  };

  return <AuthContext.Provider value={{ user, login, register, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthCtx {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
