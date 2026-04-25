import { createContext, useContext, useMemo, useState } from "react";
import { BrowserRouter, Navigate, Route, Routes, useLocation } from "react-router-dom";
import Footer from "./components/Footer";
import Header from "./components/Header";
import BookDetailPage from "./pages/BookDetailPage";
import CategoriesPage from "./pages/CategoriesPage";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import MyPage from "./pages/MyPage";
import RankingsPage from "./pages/RankingsPage";
import SignupPage from "./pages/SignupPage";

const demoUser = {
  id: 1,
  email: "reader@chaeklist.kr",
  nickname: "quiet-reader",
  status: "ACTIVE",
};

const AuthContext = createContext(null);

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used inside AuthContext.Provider");
  }

  return context;
}

function RequireAuth({ children }) {
  const { currentUser } = useAuth();
  const location = useLocation();

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
  const [currentUser, setCurrentUser] = useState(null);

  const authValue = useMemo(
    () => ({
      currentUser,
      demoUser,
      login(user) {
        setCurrentUser(user);
      },
      logout() {
        setCurrentUser(null);
      },
    }),
    [currentUser],
  );

  return (
    <AuthContext.Provider value={authValue}>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AuthContext.Provider>
  );
}
