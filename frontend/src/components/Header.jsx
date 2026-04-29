import { useEffect, useState } from "react";
import { Link, NavLink } from "react-router-dom";
import { useAuth } from "../App";

const navItems = [
  { label: "홈", to: "/" },
  { label: "랭킹", to: "/rankings" },
  { label: "카테고리", to: "/categories" },
];

export default function Header() {
  const { accessToken, currentUser, logout } = useAuth();
  const [primaryBadgeLabel, setPrimaryBadgeLabel] = useState("");

  useEffect(() => {
    let ignore = false;

    async function loadPrimaryBadge() {
      if (!accessToken || !currentUser) {
        setPrimaryBadgeLabel("");
        return;
      }

      try {
        const response = await fetch("/api/me/reading-growth/primary-badge", {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        });

        if (response.status === 401) {
          logout();
          return;
        }

        if (!response.ok) {
          throw new Error("Failed to load primary badge.");
        }

        const badge = await response.json();
        if (!ignore) {
          setPrimaryBadgeLabel(badge.label ?? "");
        }
      } catch {
        if (!ignore) {
          setPrimaryBadgeLabel("");
        }
      }
    }

    loadPrimaryBadge();

    return () => {
      ignore = true;
    };
  }, [accessToken, currentUser, logout]);

  return (
    <header className="sticky top-0 z-10 border-b border-[#E5E7EB] bg-white/95 backdrop-blur">
      <div className="mx-auto grid w-full max-w-7xl grid-cols-1 gap-4 px-5 py-4 lg:grid-cols-[1fr_minmax(280px,420px)_1fr] lg:items-center">
        <div className="flex items-center justify-start gap-2">
          <Link className="inline-flex items-center" to="/" aria-label="ChaekList 홈">
            <img className="h-10 w-10" src="/logo.svg" alt="ChaekList" />
          </Link>
          <nav className="hidden items-center sm:flex">
            {navItems.map((item, index) => (
              <div className="flex items-center" key={item.to}>
                {index > 0 ? <span className="mx-1 h-4 w-px bg-[#D1D5DB]" aria-hidden="true" /> : null}
                <NavLink
                  className={({ isActive }) =>
                    `rounded-md px-3 py-2 text-sm font-medium transition ${
                      isActive ? "bg-[#1E2A38] text-white" : "text-[#6B7280] hover:bg-[#F5F3EF] hover:text-[#1E2A38]"
                    }`
                  }
                  to={item.to}
                >
                  {item.label}
                </NavLink>
              </div>
            ))}
          </nav>
        </div>

        <div className="flex justify-center">
          <label className="relative block w-full min-w-0 max-w-md">
            <span className="sr-only">책 검색</span>
            <input
              className="w-full rounded-md border border-[#E5E7EB] bg-[#F5F3EF] px-3 py-2 text-sm outline-none transition placeholder:text-[#6B7280] focus:border-[#1E2A38] focus:bg-white focus:ring-2 focus:ring-[#1E2A38]/10"
              placeholder="책 제목, 저자, 키워드 검색"
              type="search"
            />
          </label>
        </div>

        <div className="flex items-center gap-2 lg:justify-end">
            {currentUser ? (
              <>
                {primaryBadgeLabel ? (
                  <span className="hidden rounded-full bg-[#4CAF50]/10 px-3 py-1 text-xs font-semibold text-[#2E7D32] sm:inline-flex">
                    {primaryBadgeLabel}
                  </span>
                ) : null}
                <Link className="rounded-md border border-[#E5E7EB] px-3 py-2 text-sm font-medium text-[#1E2A38]" to="/mypage">
                  {currentUser.nickname}
                </Link>
                <button
                  className="rounded-md bg-[#1E2A38] px-3 py-2 text-sm font-semibold text-white transition hover:bg-[#27384a]"
                  type="button"
                  onClick={logout}
                >
                  로그아웃
                </button>
              </>
            ) : (
              <>
                <Link className="rounded-md border border-[#E5E7EB] px-3 py-2 text-sm font-medium text-[#1E2A38]" to="/login">
                  로그인
                </Link>
                <Link className="rounded-md bg-[#1E2A38] px-3 py-2 text-sm font-semibold text-white transition hover:bg-[#27384a]" to="/signup">
                  회원가입
                </Link>
              </>
            )}
        </div>

        <nav className="flex gap-2 sm:hidden">
          {navItems.map((item) => (
            <NavLink
              className={({ isActive }) =>
                `flex-1 rounded-md px-3 py-2 text-center text-sm font-medium ${
                  isActive ? "bg-[#1E2A38] text-white" : "bg-[#F5F3EF] text-[#6B7280]"
                }`
              }
              key={item.to}
              to={item.to}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </div>
    </header>
  );
}
