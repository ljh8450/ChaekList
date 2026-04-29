import { useEffect, useState } from "react";
import BookCard from "../components/BookCard";
import { categoryRankings as fallbackCategoryRankings } from "../data/books";

function normalizeBook(book) {
  return {
    ...book,
    growth: book.growth ?? book.growthRate,
    reason: book.reason ?? book.recommendationReason,
  };
}

function normalizeGroup(group) {
  return {
    ...group,
    books: (group.books ?? []).map(normalizeBook),
  };
}

export default function CategoriesPage() {
  const [categoryRankings, setCategoryRankings] = useState(fallbackCategoryRankings.map(normalizeGroup));
  const [status, setStatus] = useState("loading");
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    let ignore = false;

    async function loadCategoryRankings() {
      setStatus("loading");
      setErrorMessage("");

      try {
        const categoriesResponse = await fetch("/api/books/categories");

        if (!categoriesResponse.ok) {
          throw new Error("카테고리 목록을 불러오지 못했습니다.");
        }

        const categories = await categoriesResponse.json();
        const groups = await Promise.all(
          (Array.isArray(categories) ? categories : []).map(async (category) => {
            const params = new URLSearchParams({
              period: "weekly",
              limit: "6",
            });
            const response = await fetch(`/api/books/categories/${encodeURIComponent(category)}/rankings?${params.toString()}`);

            if (!response.ok) {
              throw new Error("카테고리 랭킹을 불러오지 못했습니다.");
            }

            const books = await response.json();
            return {
              category,
              books: (Array.isArray(books) ? books : []).map(normalizeBook),
            };
          }),
        );

        if (!ignore) {
          setCategoryRankings(groups);
          setStatus("ready");
        }
      } catch (error) {
        if (!ignore) {
          setCategoryRankings(fallbackCategoryRankings.map(normalizeGroup));
          setErrorMessage(error instanceof Error ? error.message : "카테고리 데이터를 불러오지 못했습니다.");
          setStatus("fallback");
        }
      }
    }

    loadCategoryRankings();

    return () => {
      ignore = true;
    };
  }, []);

  const isLoading = status === "loading";

  return (
    <section className="mx-auto w-full max-w-7xl px-5 py-8">
      <div className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
        <p className="text-sm font-semibold text-[#1E2A38]">카테고리별 랭킹</p>
        <h1 className="mt-2 text-3xl font-bold text-[#1E2A38]">관심 분야별로 읽을 책을 찾습니다</h1>
        <p className="mt-3 max-w-2xl text-sm leading-6 text-[#6B7280]">
          교양 독서에 맞는 카테고리만 모아 빠르게 비교할 수 있도록 정리했습니다.
        </p>
      </div>

      {errorMessage ? (
        <div className="mt-5 rounded-lg border border-[#E5E7EB] bg-white px-4 py-3 text-sm text-[#6B7280] shadow-sm">
          API 응답을 받지 못해 임시 데이터를 표시합니다. {errorMessage}
        </div>
      ) : null}

      {isLoading ? (
        <div className="mt-6 rounded-lg border border-[#E5E7EB] bg-white p-5 text-sm text-[#6B7280] shadow-sm">
          카테고리 랭킹을 불러오는 중입니다.
        </div>
      ) : null}

      {!isLoading && categoryRankings.length === 0 ? (
        <div className="mt-6 rounded-lg border border-[#E5E7EB] bg-white p-8 text-center shadow-sm">
          <p className="text-base font-bold text-[#1E2A38]">표시할 카테고리 랭킹이 없습니다</p>
          <p className="mt-2 text-sm text-[#6B7280]">활성 카테고리와 랭킹 데이터가 준비되면 이곳에 표시합니다.</p>
        </div>
      ) : null}

      {!isLoading && categoryRankings.length > 0 ? (
        <div className="mt-6 space-y-6">
          {categoryRankings.map((group) => (
            <section className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm" key={group.category}>
              <div className="flex flex-col gap-1 sm:flex-row sm:items-end sm:justify-between">
                <div>
                  <p className="text-sm font-semibold text-[#4CAF50]">{group.category}</p>
                  <h2 className="text-xl font-bold text-[#1E2A38]">{group.category} 분야 TOP 도서</h2>
                </div>
                <p className="text-sm text-[#6B7280]">주간 랭킹 기준</p>
              </div>
              {group.books.length ? (
                <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
                  {group.books.map((book, index) => (
                    <BookCard book={book} key={book.id} rank={book.rankPosition ?? index + 1} />
                  ))}
                </div>
              ) : (
                <p className="mt-4 rounded-lg border border-[#E5E7EB] p-4 text-sm text-[#6B7280]">
                  이 분야의 랭킹 데이터가 아직 없습니다.
                </p>
              )}
            </section>
          ))}
        </div>
      ) : null}
    </section>
  );
}
