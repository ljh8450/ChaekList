import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../App";
import BookCard from "../components/BookCard";
import { books, categoryRankings, keywordTrends as fallbackKeywordTrends, trendingBooks } from "../data/books";

const coverPalette = {
  경제: "bg-[#4CAF50]",
  인문: "bg-[#1E2A38]",
  소설: "bg-[#6B7280]",
  에세이: "bg-[#9CA3AF]",
  자기계발: "bg-[#F59E0B]",
};

function withDisplayDefaults(book) {
  if (!book) {
    return null;
  }

  return {
    ...book,
    cover: book.cover ?? coverPalette[book.category] ?? "bg-[#1E2A38]",
    growth: book.growth ?? book.growthRate,
    reason: book.reason ?? book.recommendationReason ?? "랭킹 지표와 교양 필터링 기준을 반영한 책입니다.",
  };
}

function createFallbackHome(currentUser) {
  return {
    personalized: Boolean(currentUser),
    todayRecommendation: withDisplayDefaults(currentUser ? books[1] : books[0]),
    popularBooks: books.map(withDisplayDefaults),
    trendingBooks: trendingBooks.map(withDisplayDefaults),
    categoryRankings: categoryRankings.map((group) => ({
      ...group,
      books: group.books.map(withDisplayDefaults),
    })),
  };
}

function normalizeHomeResponse(data, fallbackHome) {
  const hasTodayRecommendation = data && Object.prototype.hasOwnProperty.call(data, "todayRecommendation");

  return {
    personalized: Boolean(data?.personalized),
    todayRecommendation: hasTodayRecommendation ? withDisplayDefaults(data.todayRecommendation) : fallbackHome.todayRecommendation,
    popularBooks: (Array.isArray(data?.popularBooks) ? data.popularBooks : fallbackHome.popularBooks).map(withDisplayDefaults),
    trendingBooks: (Array.isArray(data?.trendingBooks) ? data.trendingBooks : fallbackHome.trendingBooks).map(withDisplayDefaults),
    categoryRankings: (Array.isArray(data?.categoryRankings) ? data.categoryRankings : fallbackHome.categoryRankings).map((group) => ({
      ...group,
      books: (group.books ?? []).map(withDisplayDefaults),
    })),
  };
}

function normalizeKeywordTrend(trend) {
  return {
    ...trend,
    books: (trend.books ?? []).map(withDisplayDefaults),
  };
}

export default function HomePage() {
  const { accessToken, currentUser, isAuthReady, logout } = useAuth();
  const fallbackHome = useMemo(() => createFallbackHome(currentUser), [currentUser]);
  const [home, setHome] = useState(fallbackHome);
  const [keywordTrends, setKeywordTrends] = useState(fallbackKeywordTrends.map(normalizeKeywordTrend));
  const [status, setStatus] = useState("loading");
  const [trendStatus, setTrendStatus] = useState("loading");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    setHome(fallbackHome);
  }, [fallbackHome]);

  useEffect(() => {
    if (!isAuthReady) {
      return undefined;
    }

    let ignore = false;

    async function loadHome() {
      setStatus("loading");
      setNotice("");

      try {
        const isPersonalHome = Boolean(currentUser && accessToken);
        let response = await fetch(isPersonalHome ? "/api/me/home" : "/api/home", {
          headers: isPersonalHome ? { Authorization: `Bearer ${accessToken}` } : {},
        });

        if (response.status === 401 && isPersonalHome) {
          logout();
          response = await fetch("/api/home");
        }

        if (!response.ok) {
          throw new Error("홈 데이터를 불러오지 못했습니다.");
        }

        const data = await response.json();
        if (!ignore) {
          setHome(normalizeHomeResponse(data, fallbackHome));
          setStatus("ready");
        }
      } catch {
        if (!ignore) {
          setHome(fallbackHome);
          setStatus("fallback");
          setNotice("서버 홈 데이터를 불러오지 못해 임시 데이터를 표시합니다.");
        }
      }
    }

    loadHome();

    return () => {
      ignore = true;
    };
  }, [accessToken, currentUser, fallbackHome, isAuthReady, logout]);

  useEffect(() => {
    let ignore = false;

    async function loadKeywordTrends() {
      setTrendStatus("loading");

      try {
        const params = new URLSearchParams({
          limit: "3",
          booksPerKeyword: "2",
        });
        const response = await fetch(`/api/books/trends/keywords?${params.toString()}`);

        if (!response.ok) {
          throw new Error("키워드 트렌드를 불러오지 못했습니다.");
        }

        const data = await response.json();
        if (!ignore) {
          setKeywordTrends((Array.isArray(data) ? data : []).map(normalizeKeywordTrend));
          setTrendStatus("ready");
        }
      } catch {
        if (!ignore) {
          setKeywordTrends(fallbackKeywordTrends.map(normalizeKeywordTrend));
          setTrendStatus("fallback");
        }
      }
    }

    loadKeywordTrends();

    return () => {
      ignore = true;
    };
  }, []);

  const recommendation = home.todayRecommendation;
  const isLoading = status === "loading";
  const isTrendLoading = trendStatus === "loading";

  return (
    <div className="mx-auto w-full max-w-7xl px-5 py-8 lg:py-10">
      {notice ? (
        <div className="mb-5 rounded-lg border border-[#E5E7EB] bg-white px-4 py-3 text-sm text-[#6B7280] shadow-sm">
          {notice}
        </div>
      ) : null}

      <section className="grid grid-cols-1 gap-6 lg:grid-cols-[1.1fr_1.4fr_0.9fr]">
        <div className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
          <p className="text-sm font-semibold text-[#F59E0B]">급상승 도서</p>
          <h2 className="mt-2 text-2xl font-bold text-[#1E2A38]">지금 사람들이 많이 읽는 책</h2>
          <div className="mt-5 space-y-3">
            {isLoading ? (
              <p className="rounded-lg border border-[#E5E7EB] p-4 text-sm text-[#6B7280]">급상승 도서를 불러오는 중입니다.</p>
            ) : (
              home.trendingBooks.length ? (
                home.trendingBooks.slice(0, 3).map((book, index) => (
                  <BookCard book={book} key={book.id} rank={book.rankPosition ?? index + 1} variant="trending" />
                ))
              ) : (
                <p className="rounded-lg border border-[#E5E7EB] p-4 text-sm text-[#6B7280]">
                  아직 급상승 도서 데이터가 없습니다.
                </p>
              )
            )}
          </div>
        </div>

        <div className="rounded-lg border border-[#E5E7EB] bg-white p-6 shadow-sm">
          <div className="flex flex-wrap items-center gap-2">
            <p className="text-sm font-semibold text-[#4CAF50]">오늘의 추천</p>
            {home.personalized ? (
              <span className="rounded-full border border-[#4CAF50]/30 bg-[#4CAF50]/10 px-2 py-1 text-xs font-semibold text-[#2E7D32]">
                개인화 적용
              </span>
            ) : null}
          </div>
          <h1 className="mt-2 text-3xl font-bold leading-tight text-[#1E2A38]">
            {currentUser ? `${currentUser.nickname}님에게 필요한 책` : "로그인하지 않아도 책 탐색은 바로 시작됩니다"}
          </h1>
          <p className="mt-4 text-sm leading-6 text-[#6B7280]">
            {currentUser
              ? "관심 분야, 독서 목적, 읽은 책, 저장한 책을 바탕으로 오늘의 추천과 추천 이유를 함께 보여줍니다."
              : "현재 인기 책, 급상승 책, 카테고리별 랭킹을 먼저 확인하고 필요할 때 로그인하세요."}
          </p>
          <div className="mt-6">
            {isLoading ? (
              <div className="rounded-lg border border-[#E5E7EB] bg-white p-4 text-sm text-[#6B7280] shadow-sm">
                추천 도서를 불러오는 중입니다.
              </div>
            ) : recommendation ? (
              <BookCard book={recommendation} />
            ) : (
              <div className="rounded-lg border border-[#E5E7EB] bg-white p-4 text-sm text-[#6B7280] shadow-sm">
                관심 분야, 읽은 책, 저장한 책 기록이 쌓이면 개인 추천이 표시됩니다.
              </div>
            )}
          </div>
          <div className="mt-5 rounded-lg border border-[#4CAF50]/30 bg-[#4CAF50]/10 p-4">
            <p className="text-sm font-bold text-[#1E2A38]">추천 이유</p>
            <p className="mt-2 text-sm leading-6 text-[#6B7280]">
              {isLoading ? "추천 근거를 확인하는 중입니다." : recommendation?.reason ?? "추천 근거가 준비되지 않았습니다."}
            </p>
          </div>
          {!currentUser ? (
            <Link className="mt-5 inline-flex rounded-md bg-[#1E2A38] px-4 py-3 text-sm font-semibold text-white" to="/login">
              로그인하고 개인 추천 보기
            </Link>
          ) : null}
        </div>

        <aside className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
          <p className="text-sm font-semibold text-[#1E2A38]">카테고리별 랭킹</p>
          <div className="mt-2 flex items-center justify-between gap-3">
            <h2 className="text-xl font-bold text-[#1E2A38]">교양 독서 기준 TOP 리스트</h2>
            <Link className="shrink-0 text-xs font-semibold text-[#6B7280] hover:text-[#1E2A38]" to="/categories">
              더보기
            </Link>
          </div>
          <div className="mt-5 space-y-4">
            {isLoading ? (
              <p className="text-sm text-[#6B7280]">카테고리 랭킹을 불러오는 중입니다.</p>
            ) : (
              home.categoryRankings.length ? (
                home.categoryRankings.map((group) => (
                  <div className="border-b border-[#E5E7EB] pb-4 last:border-b-0 last:pb-0" key={group.category}>
                    <p className="font-semibold text-[#1E2A38]">{group.category}</p>
                    <p className="mt-2 line-clamp-1 text-sm text-[#6B7280]">{group.books[0]?.title ?? "준비 중"}</p>
                  </div>
                ))
              ) : (
                <p className="rounded-lg border border-[#E5E7EB] p-4 text-sm text-[#6B7280]">
                  아직 카테고리 랭킹 데이터가 없습니다.
                </p>
              )
            )}
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
          {isLoading ? (
            <p className="rounded-lg border border-[#E5E7EB] p-4 text-sm text-[#6B7280] md:col-span-2 xl:col-span-3">
              인기 도서를 불러오는 중입니다.
            </p>
          ) : (
            home.popularBooks.length ? (
              home.popularBooks.map((book, index) => <BookCard book={book} key={book.id} rank={book.rankPosition ?? index + 1} />)
            ) : (
              <p className="rounded-lg border border-[#E5E7EB] p-4 text-sm text-[#6B7280] md:col-span-2 xl:col-span-3">
                아직 인기 책 데이터가 없습니다.
              </p>
            )
          )}
        </div>
      </section>

      <section className="mt-8 rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-sm font-semibold text-[#4CAF50]">키워드 트렌드</p>
            <h2 className="mt-2 text-2xl font-bold text-[#1E2A38]">요즘 함께 읽히는 주제</h2>
          </div>
          <Link className="text-sm font-semibold text-[#1E2A38] hover:underline" to="/categories">
            분야별로 더 보기
          </Link>
        </div>

        {isTrendLoading ? (
          <p className="mt-5 rounded-lg border border-[#E5E7EB] p-4 text-sm text-[#6B7280]">
            키워드 트렌드를 불러오는 중입니다.
          </p>
        ) : null}

        {trendStatus === "fallback" ? (
          <p className="mt-5 rounded-lg border border-[#E5E7EB] bg-[#F5F3EF] p-4 text-sm text-[#6B7280]">
            API 응답을 받지 못해 임시 키워드 트렌드를 표시합니다.
          </p>
        ) : null}

        {!isTrendLoading && keywordTrends.length === 0 ? (
          <p className="mt-5 rounded-lg border border-[#E5E7EB] p-4 text-sm text-[#6B7280]">
            아직 키워드 트렌드 데이터가 없습니다.
          </p>
        ) : null}

        {!isTrendLoading && keywordTrends.length > 0 ? (
          <div className="mt-5 grid grid-cols-1 gap-4 lg:grid-cols-3">
            {keywordTrends.map((trend) => (
              <article className="rounded-lg border border-[#E5E7EB] p-4" key={trend.keyword}>
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-xs font-semibold text-[#6B7280]">#{trend.keyword}</p>
                    <h3 className="mt-1 text-lg font-bold text-[#1E2A38]">{trend.bookCount}권이 연결된 주제</h3>
                  </div>
                  <span className="rounded-full bg-[#F59E0B]/10 px-2 py-1 text-xs font-bold text-[#F59E0B]">
                    {trend.trendScore}
                  </span>
                </div>
                <div className="mt-4 space-y-3">
                  {trend.books.slice(0, 2).map((book) => (
                    <Link
                      className="block rounded-md border border-[#E5E7EB] p-3 transition hover:border-[#1E2A38]"
                      key={book.id}
                      to={`/books/${book.id}`}
                    >
                      <p className="line-clamp-1 text-sm font-bold text-[#1E2A38]">{book.title}</p>
                      <p className="mt-1 text-xs text-[#6B7280]">{book.author}</p>
                    </Link>
                  ))}
                </div>
              </article>
            ))}
          </div>
        ) : null}
      </section>
    </div>
  );
}
