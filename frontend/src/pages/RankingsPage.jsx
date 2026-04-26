import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { books as fallbackBooks, categories as fallbackCategories } from "../data/books";

const periods = [
  { label: "일간", value: "daily" },
  { label: "주간", value: "weekly" },
  { label: "월간", value: "monthly" },
];

function formatRankDate(rankDate) {
  if (!rankDate) {
    return "최신 기준";
  }

  return `${rankDate} 기준`;
}

function normalizeRankingBook(book) {
  return {
    ...book,
    growth: book.growth ?? book.growthRate,
    reason: book.reason ?? book.recommendationReason,
  };
}

export default function RankingsPage() {
  const [categories, setCategories] = useState(fallbackCategories);
  const [selectedCategory, setSelectedCategory] = useState("전체");
  const [selectedPeriod, setSelectedPeriod] = useState("weekly");
  const [rankings, setRankings] = useState(fallbackBooks);
  const [isLoadingCategories, setIsLoadingCategories] = useState(true);
  const [isLoadingRankings, setIsLoadingRankings] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    let isActive = true;

    async function loadCategories() {
      setIsLoadingCategories(true);

      try {
        const response = await fetch("/api/books/categories");

        if (!response.ok) {
          throw new Error("카테고리를 불러오지 못했습니다.");
        }

        const data = await response.json();
        if (isActive) {
          setCategories(["전체", ...data]);
        }
      } catch {
        if (isActive) {
          setCategories(fallbackCategories);
        }
      } finally {
        if (isActive) {
          setIsLoadingCategories(false);
        }
      }
    }

    loadCategories();

    return () => {
      isActive = false;
    };
  }, []);

  useEffect(() => {
    let isActive = true;
    const params = new URLSearchParams({
      category: selectedCategory,
      period: selectedPeriod,
      limit: "20",
    });

    async function loadRankings() {
      setIsLoadingRankings(true);
      setErrorMessage("");

      try {
        const response = await fetch(`/api/books/rankings?${params.toString()}`);

        if (!response.ok) {
          throw new Error("랭킹을 불러오지 못했습니다.");
        }

        const data = await response.json();
        if (isActive) {
          setRankings(data.map(normalizeRankingBook));
        }
      } catch (error) {
        if (isActive) {
          const filteredFallback =
            selectedCategory === "전체" ? fallbackBooks : fallbackBooks.filter((book) => book.category === selectedCategory);
          setRankings(filteredFallback);
          setErrorMessage(error.message);
        }
      } finally {
        if (isActive) {
          setIsLoadingRankings(false);
        }
      }
    }

    loadRankings();

    return () => {
      isActive = false;
    };
  }, [selectedCategory, selectedPeriod]);

  const activePeriodLabel = useMemo(
    () => periods.find((period) => period.value === selectedPeriod)?.label ?? "주간",
    [selectedPeriod],
  );
  const rankDateLabel = formatRankDate(rankings[0]?.rankDate);

  return (
    <section className="mx-auto w-full max-w-7xl px-5 py-8">
      <div className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-sm font-semibold text-[#F59E0B]">전체 랭킹</p>
            <h1 className="mt-2 text-3xl font-bold text-[#1E2A38]">교양 독서 기준 TOP 리스트</h1>
            <p className="mt-3 max-w-2xl text-sm leading-6 text-[#6B7280]">
              수험서와 전공서를 제외하고 조회, 저장, 상승률을 반영한 읽을 만한 책 순위입니다.
            </p>
          </div>

          <div className="inline-flex w-full rounded-lg border border-[#E5E7EB] bg-[#F5F3EF] p-1 sm:w-auto">
            {periods.map((period) => (
              <button
                className={`min-h-10 flex-1 rounded-md px-4 text-sm font-semibold transition sm:flex-none ${
                  selectedPeriod === period.value ? "bg-[#1E2A38] text-white shadow-sm" : "text-[#6B7280] hover:text-[#1E2A38]"
                }`}
                key={period.value}
                onClick={() => setSelectedPeriod(period.value)}
                type="button"
              >
                {period.label}
              </button>
            ))}
          </div>
        </div>

        <div className="mt-5 flex flex-wrap gap-2">
          {categories.map((category) => (
            <button
              className={`min-h-10 rounded-full border px-4 text-sm font-medium transition ${
                selectedCategory === category
                  ? "border-[#1E2A38] bg-[#1E2A38] text-white"
                  : "border-[#E5E7EB] bg-white text-[#6B7280] hover:border-[#1E2A38] hover:text-[#1E2A38]"
              }`}
              disabled={isLoadingCategories}
              key={category}
              onClick={() => setSelectedCategory(category)}
              type="button"
            >
              {category}
            </button>
          ))}
        </div>
      </div>

      <div className="mt-5 rounded-lg border border-[#E5E7EB] bg-white shadow-sm">
        <div className="flex flex-col gap-2 border-b border-[#E5E7EB] p-5 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-sm font-semibold text-[#1E2A38]">
              {selectedCategory} · {activePeriodLabel} 랭킹
            </p>
            <p className="mt-1 text-sm text-[#6B7280]">{rankDateLabel}</p>
          </div>
          <p className="text-sm text-[#6B7280]">최대 20위까지 표시합니다.</p>
        </div>

        {errorMessage ? (
          <div className="border-b border-[#E5E7EB] bg-[#F5F3EF] px-5 py-3 text-sm text-[#6B7280]">
            API 응답을 받지 못해 임시 데이터를 표시합니다. {errorMessage}
          </div>
        ) : null}

        {isLoadingRankings ? (
          <div className="grid grid-cols-1 gap-0 divide-y divide-[#E5E7EB]">
            {Array.from({ length: 5 }).map((_, index) => (
              <div className="flex gap-4 p-5" key={index}>
                <div className="h-14 w-14 shrink-0 rounded-md bg-[#E5E7EB]" />
                <div className="min-w-0 flex-1 space-y-3">
                  <div className="h-4 w-2/3 rounded bg-[#E5E7EB]" />
                  <div className="h-3 w-1/3 rounded bg-[#E5E7EB]" />
                  <div className="h-3 w-1/2 rounded bg-[#E5E7EB]" />
                </div>
              </div>
            ))}
          </div>
        ) : null}

        {!isLoadingRankings && rankings.length === 0 ? (
          <div className="p-8 text-center">
            <p className="text-base font-bold text-[#1E2A38]">표시할 랭킹이 없습니다</p>
            <p className="mt-2 text-sm text-[#6B7280]">선택한 카테고리와 기간의 데이터가 준비되면 이곳에 표시합니다.</p>
          </div>
        ) : null}

        {!isLoadingRankings && rankings.length > 0 ? (
          <ol className="divide-y divide-[#E5E7EB]">
            {rankings.map((book, index) => {
              const rank = book.rankPosition ?? index + 1;
              const isTopRank = rank <= 3;

              return (
                <li key={book.id}>
                  <Link className="group flex gap-4 p-4 transition hover:bg-[#F5F3EF] sm:gap-5 sm:p-5" to={`/books/${book.id}`}>
                    <div
                      className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-md text-lg font-bold ${
                        isTopRank ? "bg-[#F59E0B] text-white" : "border border-[#E5E7EB] text-[#1E2A38]"
                      }`}
                    >
                      {rank}
                    </div>

                    <div className="min-w-0 flex-1">
                      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                        <div className="min-w-0">
                          <h2 className="truncate text-base font-bold text-[#1E2A38] group-hover:underline">{book.title}</h2>
                          <p className="mt-1 text-sm text-[#6B7280]">{book.author}</p>
                        </div>
                        <div className="flex shrink-0 items-center gap-2">
                          <span className="rounded-full border border-[#E5E7EB] px-2 py-1 text-xs text-[#6B7280]">{book.category}</span>
                          {book.growth ? (
                            <span className="rounded-full bg-[#F59E0B]/10 px-2 py-1 text-xs font-bold text-[#F59E0B]">
                              {book.growth}
                            </span>
                          ) : null}
                        </div>
                      </div>

                      <div className="mt-3 flex flex-wrap gap-x-4 gap-y-2 text-xs text-[#6B7280]">
                        <span>조회 {book.views}</span>
                        <span>저장 {book.saves}</span>
                        <span>{formatRankDate(book.rankDate)}</span>
                      </div>
                      <p className="mt-3 line-clamp-2 text-sm leading-6 text-[#6B7280]">{book.reason}</p>
                    </div>
                  </Link>
                </li>
              );
            })}
          </ol>
        ) : null}
      </div>
    </section>
  );
}
