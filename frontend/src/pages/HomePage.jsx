import { Link } from "react-router-dom";
import BookCard from "../components/BookCard";
import { books, categoryRankings, trendingBooks } from "../data/books";
import { useAuth } from "../App";

export default function HomePage() {
  const { currentUser } = useAuth();
  const recommendation = currentUser ? books[1] : books[0];

  return (
    <div className="mx-auto w-full max-w-7xl px-5 py-8 lg:py-10">
      <section className="grid grid-cols-1 gap-6 lg:grid-cols-[1.1fr_1.4fr_0.9fr]">
        <div className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
          <p className="text-sm font-semibold text-[#F59E0B]">급상승 도서</p>
          <h2 className="mt-2 text-2xl font-bold text-[#1E2A38]">지금 사람들이 가장 많이 읽고 있는 책</h2>
          <div className="mt-5 space-y-3">
            {trendingBooks.slice(0, 3).map((book, index) => (
              <BookCard book={book} key={book.id} rank={index + 1} variant="trending" />
            ))}
          </div>
        </div>

        <div className="rounded-lg border border-[#E5E7EB] bg-white p-6 shadow-sm">
          <p className="text-sm font-semibold text-[#4CAF50]">오늘의 추천</p>
          <h1 className="mt-2 text-3xl font-bold leading-tight text-[#1E2A38]">
            {currentUser ? `${currentUser.nickname}님에게 필요한 책` : "로그인하지 않아도 책 탐색은 바로 시작됩니다"}
          </h1>
          <p className="mt-4 text-sm leading-6 text-[#6B7280]">
            {currentUser
              ? "관심 분야와 최근 탐색 흐름을 바탕으로 추천 이유를 함께 제공합니다."
              : "현재 인기 책, 급상승 책, 카테고리별 랭킹을 먼저 확인하고 필요할 때 로그인하세요."}
          </p>
          <div className="mt-6">
            <BookCard book={recommendation} />
          </div>
          <div className="mt-5 rounded-lg border border-[#4CAF50]/30 bg-[#4CAF50]/10 p-4">
            <p className="text-sm font-bold text-[#1E2A38]">추천 이유</p>
            <p className="mt-2 text-sm leading-6 text-[#6B7280]">{recommendation.reason}</p>
          </div>
          {!currentUser ? (
            <Link className="mt-5 inline-flex rounded-md bg-[#1E2A38] px-4 py-3 text-sm font-semibold text-white" to="/login">
              로그인하고 개인화 추천 보기
            </Link>
          ) : null}
        </div>

        <aside className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
          <p className="text-sm font-semibold text-[#1E2A38]">카테고리별 랭킹</p>
          <h2 className="mt-2 text-xl font-bold text-[#1E2A38]">교양 독서 기준 TOP 리스트</h2>
          <div className="mt-5 space-y-4">
            {categoryRankings.map((group) => (
              <div className="border-b border-[#E5E7EB] pb-4 last:border-b-0 last:pb-0" key={group.category}>
                <div className="flex items-center justify-between">
                  <p className="font-semibold text-[#1E2A38]">{group.category}</p>
                  <Link className="text-xs font-semibold text-[#6B7280] hover:text-[#1E2A38]" to="/categories">
                    더보기
                  </Link>
                </div>
                <p className="mt-2 text-sm text-[#6B7280]">{group.books[0]?.title ?? "준비 중"}</p>
              </div>
            ))}
          </div>
        </aside>
      </section>

      <section className="mt-8 rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-sm font-semibold text-[#1E2A38]">현재 인기 책</p>
            <h2 className="mt-2 text-2xl font-bold text-[#1E2A38]">비로그인 사용자도 볼 수 있는 인기 도서</h2>
          </div>
          <Link className="text-sm font-semibold text-[#1E2A38] hover:underline" to="/rankings">
            전체 랭킹 보기
          </Link>
        </div>
        <div className="mt-5 grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          {books.map((book, index) => (
            <BookCard book={book} key={book.id} rank={index + 1} />
          ))}
        </div>
      </section>
    </div>
  );
}
