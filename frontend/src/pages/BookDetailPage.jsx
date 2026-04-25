import { useEffect, useMemo, useState } from "react";
import { Link, Navigate, useParams } from "react-router-dom";
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
  };
}

export default function BookDetailPage() {
  const { bookId } = useParams();
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

  useEffect(() => {
    let ignore = false;

    async function loadBookDetail() {
      setStatus("loading");
      setErrorMessage("");

      try {
        const response = await fetch(`/api/books/${bookId}`);

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

    loadBookDetail();

    return () => {
      ignore = true;
    };
  }, [bookId, fallbackBook]);

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
