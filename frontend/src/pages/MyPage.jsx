import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import BookCard from "../components/BookCard";
import BookSearchPanel from "../components/BookSearchPanel";
import { useAuth } from "../App";

function formatDate(value) {
  if (!value) {
    return "";
  }

  const date = Array.isArray(value)
    ? new Date(value[0], value[1] - 1, value[2], value[3] ?? 0, value[4] ?? 0, value[5] ?? 0)
    : new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(date);
}

function sourceLabel(source) {
  if (source === "CONTENT_BASED") {
    return "개인화 추천";
  }
  if (source === "TRENDING") {
    return "급상승 추천";
  }
  return source ?? "추천";
}

function EmptyState({ message, actionLabel, to }) {
  return (
    <div className="rounded-lg border border-dashed border-[#E5E7EB] bg-[#F9FAFB] p-5 text-sm text-[#6B7280]">
      <p>{message}</p>
      <Link
        className="mt-4 inline-flex rounded-md bg-[#1E2A38] px-4 py-2 text-sm font-semibold text-white transition hover:bg-[#27384a]"
        to={to}
      >
        {actionLabel}
      </Link>
    </div>
  );
}

function toMyPageBook(book) {
  return {
    ...book,
    reason: book.reason ?? book.recommendationReason,
    recommendationReason: book.recommendationReason ?? book.reason,
  };
}

export default function MyPage() {
  const { accessToken, currentUser, logout } = useAuth();
  const [myPage, setMyPage] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    let ignore = false;

    async function loadMyPage() {
      setIsLoading(true);
      setErrorMessage("");

      try {
        const response = await fetch("/api/me/mypage", {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        });

        if (response.status === 401) {
          logout();
          return;
        }

        if (!response.ok) {
          throw new Error("마이페이지 정보를 불러오지 못했습니다.");
        }

        const data = await response.json();
        if (!ignore) {
          setMyPage(data);
        }
      } catch (error) {
        if (!ignore) {
          setErrorMessage(error instanceof Error ? error.message : "알 수 없는 오류가 발생했습니다.");
        }
      } finally {
        if (!ignore) {
          setIsLoading(false);
        }
      }
    }

    if (accessToken) {
      loadMyPage();
    } else {
      setIsLoading(false);
    }

    return () => {
      ignore = true;
    };
  }, [accessToken, logout]);

  const user = myPage?.user ?? currentUser;
  const interestProfiles = myPage?.interests ?? [];
  const readBooks = myPage?.readBooks ?? [];
  const savedBooks = myPage?.savedBooks ?? [];
  const recommendationHistory = myPage?.recommendationHistory ?? [];

  async function addBookInteraction(book, type) {
    const response = await fetch(`/api/me/books/${book.id}/interactions`, {
      body: JSON.stringify({ type }),
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      method: "POST",
    });

    if (response.status === 401) {
      logout();
      throw new Error("로그인이 만료되었습니다. 다시 로그인해 주세요.");
    }

    if (!response.ok) {
      throw new Error("책 상태를 저장하지 못했습니다.");
    }

    const normalizedBook = toMyPageBook(book);
    setMyPage((current) => {
      if (!current) {
        return current;
      }

      if (type === "READ") {
        const hasReadBook = (current.readBooks ?? []).some((item) => String(item.id) === String(book.id));
        return {
          ...current,
          readBooks: hasReadBook ? current.readBooks : [normalizedBook, ...(current.readBooks ?? [])],
        };
      }

      const hasSavedBook = (current.savedBooks ?? []).some((item) => String(item.id) === String(book.id));
      return {
        ...current,
        savedBooks: hasSavedBook ? current.savedBooks : [normalizedBook, ...(current.savedBooks ?? [])],
      };
    });
  }

  return (
    <section className="mx-auto w-full max-w-7xl px-5 py-8">
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-[320px_1fr]">
        <aside className="rounded-lg border border-[#E5E7EB] bg-white p-6 shadow-sm">
          <p className="text-sm font-semibold text-[#4CAF50]">마이페이지</p>
          <h1 className="mt-2 text-3xl font-bold leading-tight text-[#1E2A38]">{user.nickname}님의 독서 취향</h1>
          <p className="mt-4 text-sm leading-6 text-[#6B7280]">
            관심 분야, 읽은 책, 추천 히스토리를 바탕으로 지금 읽을 만한 교양서를 정리합니다.
          </p>

          <div className="mt-6 space-y-3 rounded-lg bg-[#F5F3EF] p-4">
            <div>
              <p className="text-xs font-medium text-[#6B7280]">이메일</p>
              <p className="mt-1 text-sm font-semibold text-[#1E2A38]">{user.email}</p>
            </div>
            <div>
              <p className="text-xs font-medium text-[#6B7280]">계정 상태</p>
              <p className="mt-1 text-sm font-semibold text-[#4CAF50]">{user.status}</p>
            </div>
          </div>

          <div className="mt-6 grid grid-cols-3 gap-2">
            {[
              ["관심", interestProfiles.length],
              ["읽은 책", readBooks.length],
              ["추천", recommendationHistory.length],
            ].map(([label, value]) => (
              <div className="rounded-lg border border-[#E5E7EB] p-3 text-center" key={label}>
                <p className="text-xl font-bold text-[#1E2A38]">{value}</p>
                <p className="mt-1 text-xs text-[#6B7280]">{label}</p>
              </div>
            ))}
          </div>
        </aside>

        <div className="space-y-6">
          {isLoading ? (
            <div className="rounded-lg border border-[#E5E7EB] bg-white p-5 text-sm text-[#6B7280] shadow-sm">
              마이페이지 정보를 불러오는 중입니다.
            </div>
          ) : null}

          {errorMessage ? (
            <div className="rounded-lg border border-[#E5E7EB] bg-white p-5 text-sm text-[#6B7280] shadow-sm">
              {errorMessage}
            </div>
          ) : null}

          <section className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
            <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <p className="text-sm font-semibold text-[#1E2A38]">관심 분야</p>
                <h2 className="mt-2 text-2xl font-bold text-[#1E2A38]">추천에 반영되는 독서 취향</h2>
              </div>
              <Link
                className="inline-flex rounded-md border border-[#E5E7EB] px-3 py-2 text-sm font-semibold text-[#1E2A38] transition hover:border-[#1E2A38]"
                to="/onboarding?section=interests"
              >
                관심 분야 수정
              </Link>
            </div>

            {!isLoading && interestProfiles.length === 0 ? (
              <div className="mt-5">
                <EmptyState
                  actionLabel="관심 분야 선택"
                  message="관심 분야를 선택하면 추천 이유가 더 구체적으로 바뀝니다."
                  to="/onboarding?section=interests"
                />
              </div>
            ) : (
              <div className="mt-5 grid grid-cols-1 gap-4 md:grid-cols-3">
                {interestProfiles.map((interest) => (
                  <article className="rounded-lg border border-[#E5E7EB] p-4" key={interest.label}>
                    <div className="flex items-center justify-between gap-3">
                      <h3 className="font-bold text-[#1E2A38]">{interest.label}</h3>
                      <span className="text-sm font-bold text-[#F59E0B]">{interest.score}%</span>
                    </div>
                    <div className="mt-3 h-2 rounded-full bg-[#F5F3EF]">
                      <div className="h-2 rounded-full bg-[#4CAF50]" style={{ width: `${interest.score}%` }} />
                    </div>
                    <p className="mt-3 text-sm leading-6 text-[#6B7280]">{interest.description}</p>
                  </article>
                ))}
              </div>
            )}
          </section>

          <section className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
            <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <p className="text-sm font-semibold text-[#1E2A38]">읽은 책</p>
                <h2 className="mt-2 text-2xl font-bold text-[#1E2A38]">취향 분석에 사용된 책</h2>
              </div>
              <Link
                className="inline-flex rounded-md border border-[#E5E7EB] px-3 py-2 text-sm font-semibold text-[#1E2A38] transition hover:border-[#1E2A38]"
                to="/onboarding?section=read-books"
              >
                읽은 책 수정
              </Link>
            </div>
            <BookSearchPanel
              actionLabel="읽은 책 추가"
              disabledIds={readBooks.map((book) => book.id)}
              emptyMessage="읽은 책으로 추가할 검색 결과가 없습니다."
              onSelect={(book) => addBookInteraction(book, "READ")}
              selectedIds={readBooks.map((book) => book.id)}
              selectedLabel="이미 읽은 책"
              title="읽은 책 추가 검색"
            />
            {!isLoading && readBooks.length === 0 ? (
              <div className="mt-5">
                <EmptyState
                  actionLabel="읽은 책 추가"
                  message="읽은 책을 추가하면 취향 분석에 반영됩니다."
                  to="/onboarding?section=read-books"
                />
              </div>
            ) : (
              <div className="mt-5 grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
                {readBooks.map((book) => (
                  <BookCard book={book} key={book.id} />
                ))}
              </div>
            )}
          </section>

          <section className="grid grid-cols-1 gap-6 xl:grid-cols-[1fr_360px]">
            <div className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
              <p className="text-sm font-semibold text-[#1E2A38]">추천 히스토리</p>
              <h2 className="mt-2 text-2xl font-bold text-[#1E2A38]">왜 추천됐는지 남기는 기록</h2>
              {!isLoading && recommendationHistory.length === 0 ? (
                <div className="mt-5">
                  <EmptyState
                    actionLabel="오늘의 추천 보기"
                    message="홈에서 개인화 추천이 만들어지면 추천 이유와 함께 이곳에 남습니다."
                    to="/"
                  />
                </div>
              ) : (
                <ol className="mt-5 divide-y divide-[#E5E7EB]">
                  {recommendationHistory.map((history) => (
                    <li className="py-4 first:pt-0 last:pb-0" key={history.id}>
                      <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                        <div className="min-w-0">
                          <Link className="font-bold text-[#1E2A38] hover:underline" to={`/books/${history.bookId}`}>
                            {history.title}
                          </Link>
                          <p className="mt-1 break-keep text-sm leading-6 text-[#6B7280]">{history.reason}</p>
                        </div>
                        {formatDate(history.generatedAt) ? (
                          <span className="shrink-0 rounded-full border border-[#E5E7EB] px-3 py-1 text-xs text-[#6B7280]">
                            {formatDate(history.generatedAt)}
                          </span>
                        ) : null}
                      </div>
                      <div className="mt-3 flex flex-wrap items-center gap-2">
                        <span className="rounded-full bg-[#4CAF50]/10 px-2 py-1 text-xs font-semibold text-[#2E7D32]">
                          {sourceLabel(history.source)}
                        </span>
                        <span className="text-xs font-semibold text-[#6B7280]">추천 점수 {history.score}</span>
                      </div>
                    </li>
                  ))}
                </ol>
              )}
            </div>

            <aside className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
              <p className="text-sm font-semibold text-[#F59E0B]">저장한 책</p>
              <h2 className="mt-2 text-xl font-bold text-[#1E2A38]">다음에 읽을 후보</h2>
              <BookSearchPanel
                actionLabel="저장하기"
                disabledIds={savedBooks.map((book) => book.id)}
                emptyMessage="저장할 검색 결과가 없습니다."
                onSelect={(book) => addBookInteraction(book, "SAVE")}
                selectedIds={savedBooks.map((book) => book.id)}
                selectedLabel="이미 저장됨"
                title="저장한 책 추가 검색"
              />
              {!isLoading && savedBooks.length === 0 ? (
                <div className="mt-5">
                  <EmptyState
                    actionLabel="읽을 책 찾기"
                    message="나중에 읽을 책은 책 상세에서 저장할 수 있습니다."
                    to="/rankings"
                  />
                </div>
              ) : (
                <div className="mt-5 space-y-4">
                  {savedBooks.map((book) => (
                    <div className="border-b border-[#E5E7EB] pb-4 last:border-b-0 last:pb-0" key={book.id}>
                      <p className="font-semibold text-[#1E2A38]">{book.title}</p>
                      <p className="mt-1 text-sm text-[#6B7280]">{book.author}</p>
                      <p className="mt-2 text-xs font-semibold text-[#4CAF50]">{book.recommendationReason}</p>
                    </div>
                  ))}
                </div>
              )}
            </aside>
          </section>
        </div>
      </div>
    </section>
  );
}
