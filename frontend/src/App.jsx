import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { BrowserRouter, Navigate, Route, Routes, useLocation } from "react-router-dom";
import Footer from "./components/Footer";
import Header from "./components/Header";
import BookDetailPage from "./pages/BookDetailPage";
import CategoriesPage from "./pages/CategoriesPage";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import MyPage from "./pages/MyPage";
import OnboardingPage from "./pages/OnboardingPage";
import RankingsPage from "./pages/RankingsPage";
import SignupPage from "./pages/SignupPage";

const AUTH_STORAGE_KEY = "chaeklist.auth";

const demoUser = {
  id: 1,
  email: "reader@chaeklist.kr",
  nickname: "quiet-reader",
  status: "ACTIVE",
};

const AuthContext = createContext(null);

function loadStoredAuth() {
  try {
    const storedAuth = localStorage.getItem(AUTH_STORAGE_KEY);
    return storedAuth ? JSON.parse(storedAuth) : null;
  } catch {
    return null;
  }
}

function storeAuthSession(session) {
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
}

function clearAuthSession() {
  localStorage.removeItem(AUTH_STORAGE_KEY);
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used inside AuthContext.Provider");
  }

  return context;
}

function RequireAuth({ children }) {
  const { currentUser, isAuthReady } = useAuth();
  const location = useLocation();

  if (!isAuthReady) {
    return (
      <section className="mx-auto w-full max-w-5xl px-5 py-8">
        <div className="rounded-lg border border-[#E5E7EB] bg-white p-6 text-sm text-[#6B7280] shadow-sm">
          로그인 상태를 확인하는 중입니다.
        </div>
      </section>
    );
  }

  if (!currentUser) {
    return <Navigate replace state={{ from: location }} to="/login" />;
  }

  return children;
}

function AppRoutes() {
  return (
    <div className="flex min-h-screen flex-col bg-[#F5F3EF] text-[#111827]">
      <Header />
      <main className="flex-1">
        <Routes>
          <Route element={<HomePage />} path="/" />
          <Route element={<LoginPage />} path="/login" />
          <Route element={<SignupPage />} path="/signup" />
          <Route element={<RankingsPage />} path="/rankings" />
          <Route element={<CategoriesPage />} path="/categories" />
          <Route element={<BookDetailPage />} path="/books/:bookId" />
          <Route
            element={
              <RequireAuth>
                <OnboardingPage />
              </RequireAuth>
            }
            path="/onboarding"
          />
          <Route
            element={
              <RequireAuth>
                <MyPage />
              </RequireAuth>
            }
            path="/mypage"
          />
          <Route element={<Navigate replace to="/" />} path="*" />
        </Routes>
      </main>
      <Footer />
    </div>
  );
}

export default function App() {
  const [authSession, setAuthSession] = useState(() => loadStoredAuth());
  const [isAuthReady, setIsAuthReady] = useState(() => !loadStoredAuth()?.accessToken);

  useEffect(() => {
    const storedAuth = loadStoredAuth();

    if (!storedAuth?.accessToken) {
      setIsAuthReady(true);
      return;
    }

    let ignore = false;

    async function restoreAuth() {
      try {
        const response = await fetch("/api/auth/me", {
          headers: {
            Authorization: `Bearer ${storedAuth.accessToken}`,
          },
        });

        if (!response.ok) {
          throw new Error("Stored token is invalid.");
        }

        const user = await response.json();
        const restoredSession = {
          ...storedAuth,
          user,
        };

        if (!ignore) {
          setAuthSession(restoredSession);
          storeAuthSession(restoredSession);
        }
      } catch {
        if (!ignore) {
          setAuthSession(null);
          clearAuthSession();
        }
      } finally {
        if (!ignore) {
          setIsAuthReady(true);
        }
      }
    }

    restoreAuth();

    return () => {
      ignore = true;
    };
  }, []);

  const authValue = useMemo(
    () => ({
      accessToken: authSession?.accessToken ?? "",
      currentUser: authSession?.user ?? null,
      demoUser,
      isAuthReady,
      login(session) {
        setAuthSession(session);
        storeAuthSession(session);
      },
      logout() {
        setAuthSession(null);
        clearAuthSession();
      },
    }),
    [authSession, isAuthReady],
  );

  return (
    <AuthContext.Provider value={authValue}>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AuthContext.Provider>
  );
}
