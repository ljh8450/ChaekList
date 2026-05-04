import { Link, NavLink, useNavigate } from "react-router-dom";
import { useState } from "react";
import { useAuth } from "../App";

const navItems = [
  { label: "홈", to: "/" },
  { label: "랭킹", to: "/rankings" },
  { label: "카테고리", to: "/categories" },
  { label: "모각독", to: "/reading-rooms" },
  { label: "피드", to: "/social" },
];

export default function Header() {
  const { currentUser, logout, primaryBadge } = useAuth();
  const navigate = useNavigate();
  const [query, setQuery] = useState("");
  const primaryBadgeLabel = primaryBadge?.label ?? "";

  function submitSearch(event) {
    event.preventDefault();
    const nextQuery = query.trim();
    if (nextQuery.length < 2) {
      return;
    }
    navigate(`/search?query=${encodeURIComponent(nextQuery)}&type=all`);
  }

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

        <form className="flex justify-center" onSubmit={submitSearch}>
          <label className="relative block w-full min-w-0 max-w-md">
            <span className="sr-only">책 검색</span>
            <input
              className="w-full rounded-md border border-[#E5E7EB] bg-[#F5F3EF] px-3 py-2 text-sm outline-none transition placeholder:text-[#6B7280] focus:border-[#1E2A38] focus:bg-white focus:ring-2 focus:ring-[#1E2A38]/10"
              placeholder="책, 저자, 키워드, 공개 기록 검색"
              type="search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
            />
          </label>
        </form>

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
                <Link className="rounded-md border border-[#E5E7EB] px-3 py-2 text-sm font-medium text-[#1E2A38]" to="/settings">
                  설정
                </Link>
                <Link className="rounded-md border border-[#E5E7EB] px-3 py-2 text-sm font-medium text-[#1E2A38]" to="/notifications">
                  알림
                </Link>
                {currentUser.role === "ADMIN" ? (
                  <Link className="rounded-md border border-[#E5E7EB] px-3 py-2 text-sm font-medium text-[#1E2A38]" to="/admin/moderation">
                    관리
                  </Link>
                ) : null}
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
