import { NavLink, Link } from "react-router-dom";
import { useAuth } from "../App";

const navItems = [
  { label: "홈", to: "/" },
  { label: "랭킹", to: "/rankings" },
  { label: "카테고리", to: "/categories" },
];

export default function Header() {
  const { currentUser, logout } = useAuth();

  return (
    <header className="sticky top-0 z-10 border-b border-[#E5E7EB] bg-white/95 backdrop-blur">
      <div className="mx-auto flex w-full max-w-7xl flex-col gap-4 px-5 py-4 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex items-center justify-between gap-4">
          <Link className="text-xl font-bold text-[#1E2A38]" to="/">
            ChaekList
          </Link>
          <nav className="hidden items-center gap-2 sm:flex">
            {navItems.map((item) => (
              <NavLink
                className={({ isActive }) =>
                  `rounded-md px-3 py-2 text-sm font-medium transition ${
                    isActive ? "bg-[#1E2A38] text-white" : "text-[#6B7280] hover:bg-[#F5F3EF] hover:text-[#1E2A38]"
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

        <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
          <label className="relative block min-w-0 sm:w-72">
            <span className="sr-only">책 검색</span>
            <input
              className="w-full rounded-md border border-[#E5E7EB] bg-[#F5F3EF] px-3 py-2 text-sm outline-none transition placeholder:text-[#6B7280] focus:border-[#1E2A38] focus:bg-white focus:ring-2 focus:ring-[#1E2A38]/10"
              placeholder="책 제목, 저자, 키워드 검색"
              type="search"
            />
          </label>

          <div className="flex items-center gap-2">
            {currentUser ? (
              <>
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
