import BookCard from "../components/BookCard";
import { categoryRankings } from "../data/books";

export default function CategoriesPage() {
  return (
    <section className="mx-auto w-full max-w-7xl px-5 py-8">
      <div className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
        <p className="text-sm font-semibold text-[#1E2A38]">카테고리별 랭킹</p>
        <h1 className="mt-2 text-3xl font-bold text-[#1E2A38]">관심 분야별로 읽을 책을 찾습니다</h1>
        <p className="mt-3 max-w-2xl text-sm leading-6 text-[#6B7280]">
          교양 독서에 맞는 카테고리만 모아 빠르게 비교할 수 있도록 정리했습니다.
        </p>
      </div>

      <div className="mt-6 space-y-6">
        {categoryRankings.map((group) => (
          <section className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm" key={group.category}>
            <h2 className="text-xl font-bold text-[#1E2A38]">{group.category}</h2>
            <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
              {group.books.map((book, index) => (
                <BookCard book={book} key={book.id} rank={index + 1} />
              ))}
            </div>
          </section>
        ))}
      </div>
    </section>
  );
}
