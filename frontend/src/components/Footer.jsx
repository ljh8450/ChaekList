import { Link } from "react-router-dom";

export default function Footer() {
  return (
    <footer className="border-t border-[#E5E7EB] bg-white">
      <div className="mx-auto flex w-full max-w-7xl flex-col gap-5 px-5 py-8 text-sm text-[#6B7280] md:flex-row md:items-center md:justify-between">
        <div>
          <p className="font-bold text-[#1E2A38]">ChaekList</p>
          <p className="mt-2">읽을 책을 찾는 시간을 줄이고, 지금 읽어야 할 책을 보여주는 서비스</p>
          <p className="mt-1 text-xs">랭킹은 조회, 저장, 리뷰, 최근 상승 흐름을 기준으로 구성합니다.</p>
        </div>
        <nav className="flex flex-wrap gap-4">
          <Link className="hover:text-[#1E2A38]" to="/">
            서비스 홈
          </Link>
          <Link className="hover:text-[#1E2A38]" to="/rankings">
            랭킹
          </Link>
          <Link className="hover:text-[#1E2A38]" to="/categories">
            카테고리
          </Link>
          <span>문의</span>
          <span>© 2026 ChaekList</span>
        </nav>
      </div>
    </footer>
  );
}
