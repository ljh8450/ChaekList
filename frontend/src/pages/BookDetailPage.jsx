import { useEffect, useMemo, useState } from "react";
import { Link, Navigate, useLocation, useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../App";
import BookCard from "../components/BookCard";
import { books } from "../data/books";

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
    keywords: book.keywords ?? [],
    reason: book.reason ?? book.recommendationReason,
    similarBooks: (book.similarBooks ?? []).map(withDisplayDefaults),
    saved: Boolean(book.saved),
    read: Boolean(book.read),
    dismissed: Boolean(book.dismissed),
  };
}

export default function BookDetailPage() {
  const { bookId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { accessToken, currentUser, isAuthReady, logout } = useAuth();
  const fallbackBook = useMemo(() => withDisplayDefaults(books.find((item) => item.id === bookId)), [bookId]);
  const fallbackSimilarBooks = useMemo(
    () =>
      fallbackBook
        ? books
            .filter((item) => item.id !== fallbackBook.id && item.category === fallbackBook.category)
            .slice(0, 3)
            .map(withDisplayDefaults)
        : [],
    [fallbackBook],
  );
  const [book, setBook] = useState(fallbackBook);
  const [status, setStatus] = useState("loading");
  const [errorMessage, setErrorMessage] = useState("");
  const [actionMessage, setActionMessage] = useState("");
  const [pendingAction, setPendingAction] = useState("");

  useEffect(() => {
    let ignore = false;

    async function loadBookDetail() {
      setStatus("loading");
      setErrorMessage("");
      setActionMessage("");

      try {
        let response = await fetch(`/api/books/${bookId}`, {
          headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : {},
        });

        if (response.status === 401 && accessToken) {
          logout();
          response = await fetch(`/api/books/${bookId}`);
        }

        if (response.status === 404) {
          if (!ignore) {
            setBook(fallbackBook);
            setStatus(fallbackBook ? "ready" : "not-found");
          }
          return;
        }

        if (!response.ok) {
          throw new Error("책 상세 정보를 불러오지 못했습니다.");
        }

        const data = await response.json();
        if (!ignore) {
          setBook(withDisplayDefaults(data));
          setStatus("ready");
        }
      } catch (error) {
        if (!ignore) {
          setBook(fallbackBook);
          setErrorMessage(error.message);
          setStatus(fallbackBook ? "ready" : "error");
        }
      }
    }

    if (isAuthReady) {
      loadBookDetail();
    }

    return () => {
      ignore = true;
    };
  }, [accessToken, bookId, fallbackBook, isAuthReady, logout]);

  async function saveInteraction(type) {
    if (!accessToken || !currentUser) {
      navigate("/login", { state: { from: location } });
      return;
    }

    setPendingAction(type);
    setActionMessage("");

    try {
      const response = await fetch(`/api/me/books/${bookId}/interactions`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ type }),
      });

      if (response.status === 401) {
        logout();
        navigate("/login", { state: { from: location } });
        return;
      }

      if (!response.ok) {
        throw new Error("책 행동을 저장하지 못했습니다.");
      }

      const data = await response.json();
      setBook((currentBook) =>
        currentBook
          ? withDisplayDefaults({
              ...currentBook,
              saved: data.saved,
              read: data.read,
              dismissed: data.dismissed,
            })
          : currentBook,
      );
      setActionMessage(toActionMessage(type, data));
    } catch (error) {
      setActionMessage(error instanceof Error ? error.message : "알 수 없는 오류가 발생했습니다.");
    } finally {
      setPendingAction("");
    }
  }

  function toActionMessage(type, data) {
    if (type === "SAVE" && data.saved) {
      return "저장한 책에 추가했습니다.";
    }
    if (type === "UNSAVE" && !data.saved) {
      return "저장한 책에서 제외했습니다.";
    }
    if (type === "READ" && data.read) {
      return "읽은 책으로 표시했습니다.";
    }
    if (type === "DISMISS" && data.dismissed) {
      return "관심 없음으로 표시했습니다.";
    }
    return "상태를 반영했습니다.";
  }

  if (status === "not-found") {
    return <Navigate replace to="/" />;
  }

  if (status === "loading" && !book) {
    return (
      <section className="mx-auto w-full max-w-6xl px-5 py-8">
        <div className="rounded-lg border border-[#E5E7EB] bg-white p-6 text-sm text-[#6B7280] shadow-sm">
          책 상세 정보를 불러오는 중입니다.
        </div>
      </section>
    );
  }

  if (status === "error") {
    return (
      <section className="mx-auto w-full max-w-6xl px-5 py-8">
        <Link className="text-sm font-semibold text-[#6B7280] hover:text-[#1E2A38]" to="/">
          홈으로 돌아가기
        </Link>
        <div className="mt-5 rounded-lg border border-[#E5E7EB] bg-white p-6 text-sm text-[#6B7280] shadow-sm">
          {errorMessage || "책 상세 정보를 불러오지 못했습니다."}
        </div>
      </section>
    );
  }

  const similarBooks = book.similarBooks?.length ? book.similarBooks : fallbackSimilarBooks;
  const isActionDisabled = Boolean(pendingAction) || !isAuthReady;

  return (
    <section className="mx-auto w-full max-w-6xl px-5 py-8">
      <Link className="text-sm font-semibold text-[#6B7280] hover:text-[#1E2A38]" to="/">
        홈으로 돌아가기
      </Link>

      {errorMessage ? (
        <div className="mt-5 rounded-lg border border-[#E5E7EB] bg-white px-4 py-3 text-sm text-[#6B7280] shadow-sm">
          서버 상세 정보를 불러오지 못해 임시 데이터를 표시합니다.
        </div>
      ) : null}

      <div className="mt-5 grid grid-cols-1 gap-6 rounded-lg border border-[#E5E7EB] bg-white p-6 shadow-sm md:grid-cols-[240px_1fr]">
        <div className={`flex aspect-[3/4] items-end rounded-lg ${book.cover} p-5 text-lg font-bold text-white shadow-sm`}>
          {book.category}
        </div>
        <div>
          <p className="text-sm font-semibold text-[#4CAF50]">{book.tag}</p>
          <h1 className="mt-3 text-3xl font-bold leading-tight text-[#1E2A38]">{book.title}</h1>
          <p className="mt-2 text-[#6B7280]">{book.author}</p>
          <div className="mt-4 flex flex-wrap gap-3 text-sm text-[#6B7280]">
            <span>조회 {book.views ?? "0"}</span>
            <span>저장 {book.saves ?? 0}</span>
            {book.growth ? <span className="font-semibold text-[#F59E0B]">{book.growth}</span> : null}
          </div>
          <div className="mt-5 flex flex-wrap gap-2">
            <button
              className={`rounded-md px-4 py-2 text-sm font-semibold transition ${
                book.saved
                  ? "bg-[#1E2A38] text-white hover:bg-[#27384a]"
                  : "border border-[#E5E7EB] text-[#1E2A38] hover:border-[#1E2A38]"
              } disabled:cursor-not-allowed disabled:opacity-60`}
              disabled={isActionDisabled}
              onClick={() => saveInteraction(book.saved ? "UNSAVE" : "SAVE")}
              type="button"
            >
              {pendingAction === "SAVE" || pendingAction === "UNSAVE" ? "저장 중" : book.saved ? "저장됨" : "저장하기"}
            </button>
            <button
              className={`rounded-md px-4 py-2 text-sm font-semibold transition ${
                book.read
                  ? "bg-[#4CAF50] text-white hover:bg-[#3f9744]"
                  : "border border-[#E5E7EB] text-[#1E2A38] hover:border-[#1E2A38]"
              } disabled:cursor-not-allowed disabled:opacity-60`}
              disabled={isActionDisabled || book.read}
              onClick={() => saveInteraction("READ")}
              type="button"
            >
              {pendingAction === "READ" ? "표시 중" : book.read ? "읽은 책" : "읽었어요"}
            </button>
            <button
              className={`rounded-md px-4 py-2 text-sm font-semibold transition ${
                book.dismissed
                  ? "bg-[#6B7280] text-white hover:bg-[#5b6270]"
                  : "border border-[#E5E7EB] text-[#6B7280] hover:border-[#6B7280]"
              } disabled:cursor-not-allowed disabled:opacity-60`}
              disabled={isActionDisabled || book.dismissed}
              onClick={() => saveInteraction("DISMISS")}
              type="button"
            >
              {pendingAction === "DISMISS" ? "표시 중" : book.dismissed ? "관심 없음 표시됨" : "관심 없음"}
            </button>
          </div>
          {actionMessage ? <p className="mt-3 text-sm font-medium text-[#6B7280]">{actionMessage}</p> : null}
          <p className="mt-6 max-w-2xl leading-7 text-[#111827]">{book.summary || "등록된 책 소개가 없습니다."}</p>
          <div className="mt-6 rounded-lg border border-[#4CAF50]/30 bg-[#4CAF50]/10 p-4">
            <p className="text-sm font-bold text-[#1E2A38]">추천 이유</p>
            <p className="mt-2 text-sm leading-6 text-[#6B7280]">{book.reason}</p>
          </div>
          {book.keywords.length ? (
            <div className="mt-5 flex flex-wrap gap-2">
              {book.keywords.map((keyword) => (
                <span className="rounded-full border border-[#E5E7EB] px-3 py-2 text-sm text-[#6B7280]" key={keyword}>
                  {keyword}
                </span>
              ))}
            </div>
          ) : null}
        </div>
      </div>

      {similarBooks.length ? (
        <section className="mt-6">
          <div className="flex flex-col gap-1">
            <p className="text-sm font-semibold text-[#4CAF50]">교양 필터 기반 추천</p>
            <h2 className="text-xl font-bold text-[#1E2A38]">비슷한 책</h2>
          </div>
          <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2">
            {similarBooks.map((item) => (
              <BookCard book={item} key={item.id} />
            ))}
          </div>
        </section>
      ) : null}
    </section>
  );
}
