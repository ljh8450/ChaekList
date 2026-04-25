import { useMemo, useState } from "react";
import BookCard from "../components/BookCard";
import { books, categories } from "../data/books";

export default function RankingsPage() {
  const [selectedCategory, setSelectedCategory] = useState(categories[0]);
  const rankingBooks = useMemo(
    () =>
      selectedCategory === "전체"
        ? books
        : books.filter((book) => book.category === selectedCategory),
    [selectedCategory],
  );

  return (
    <section className="mx-auto w-full max-w-7xl px-5 py-8">
      <div className="flex flex-col gap-4 rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
        <div>
          <p className="text-sm font-semibold text-[#F59E0B]">전체 랭킹</p>
          <h1 className="mt-2 text-3xl font-bold text-[#1E2A38]">교양 독서 기준 TOP 리스트</h1>
        </div>
        <div className="flex flex-wrap gap-2">
          {categories.map((category) => {
            const selected = category === selectedCategory;

            return (
              <button
                aria-pressed={selected}
                className={`rounded-full border px-3 py-2 text-sm font-medium transition ${
                  selected
                    ? "border-[#1E2A38] bg-[#1E2A38] text-white"
                    : "border-[#E5E7EB] text-[#6B7280] hover:border-[#1E2A38] hover:text-[#1E2A38]"
                }`}
                key={category}
                onClick={() => setSelectedCategory(category)}
                type="button"
              >
                {category}
              </button>
            );
          })}
        </div>
      </div>

      {rankingBooks.length ? (
        <div className="mt-5 grid grid-cols-1 gap-4 md:grid-cols-2">
          {rankingBooks.map((book, index) => (
            <BookCard book={book} key={book.id} rank={index + 1} variant="trending" />
          ))}
        </div>
      ) : (
        <div className="mt-5 rounded-lg border border-[#E5E7EB] bg-white p-6 text-sm text-[#6B7280] shadow-sm">
          선택한 카테고리에 표시할 책이 없습니다.
        </div>
      )}
    </section>
  );
}
