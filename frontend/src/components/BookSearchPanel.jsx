import { useEffect, useRef, useState } from "react";

function normalizeSearchBook(book) {
  return {
    id: book.id ?? book.bookId,
    title: book.title ?? "제목 없음",
    author: book.author ?? "저자 미상",
    category: book.category ?? book.categoryName ?? "교양",
    tag: book.tag ?? "교양 필터 통과",
    recommendationReason: book.recommendationReason ?? book.reason ?? "교양 독서 후보로 확인할 수 있는 책입니다.",
    imageUrl: book.imageUrl ?? book.coverImageUrl,
    views: book.views ?? "0",
    saves: book.saves ?? 0,
    growthRate: book.growthRate,
  };
}

export default function BookSearchPanel({
  actionLabel,
  actionSlot = null,
  className = "",
  disabledIds = [],
  emptyMessage = "검색 결과가 없습니다.",
  floatingResults = false,
  onResults,
  onSelect,
  reserveMessageSpace = false,
  selectedIds = [],
  selectedLabel = "선택됨",
  title = "책 검색",
}) {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState([]);
  const [status, setStatus] = useState("idle");
  const [message, setMessage] = useState("");
  const [pendingBookId, setPendingBookId] = useState("");
  const panelRef = useRef(null);

  const disabledIdSet = new Set(disabledIds.map(String));
  const selectedIdSet = new Set(selectedIds.map(String));

  useEffect(() => {
    if (!floatingResults || results.length === 0) {
      return undefined;
    }

    function handleOutsidePointerDown(event) {
      if (panelRef.current?.contains(event.target)) {
        return;
      }

      setResults([]);
    }

    document.addEventListener("pointerdown", handleOutsidePointerDown);
    return () => {
      document.removeEventListener("pointerdown", handleOutsidePointerDown);
    };
  }, [floatingResults, results.length]);

  async function searchBooks() {
    const trimmedQuery = query.trim();
    if (trimmedQuery.length < 2) {
      setResults([]);
      setStatus("idle");
      setMessage("검색어를 2자 이상 입력해 주세요.");
      return;
    }

    setStatus("loading");
    setMessage("");

    try {
      const response = await fetch(`/api/books/search?query=${encodeURIComponent(trimmedQuery)}&limit=10`);

      if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        throw new Error(data.message ?? "책 검색에 실패했습니다.");
      }

      const data = await response.json();
      const nextResults = data.map(normalizeSearchBook).filter((book) => book.id);
      setResults(nextResults);
      onResults?.(nextResults);
      setMessage(nextResults.length === 0 ? emptyMessage : "");
      setStatus("ready");
    } catch (error) {
      setResults([]);
      if (error instanceof TypeError) {
        setMessage("검색 API에 연결하지 못했습니다. backend 서버 실행 상태를 확인해 주세요.");
      } else {
        setMessage(error instanceof Error ? error.message : "알 수 없는 오류가 발생했습니다.");
      }
      setStatus("error");
    }
  }

  function handleSearchKeyDown(event) {
    if (event.key === "Enter") {
      event.preventDefault();
      searchBooks();
    }
  }

  async function selectBook(book) {
    if (disabledIdSet.has(String(book.id))) {
      return;
    }

    setPendingBookId(book.id);
    setMessage("");

    try {
      await onSelect(book);
      if (floatingResults) {
        setResults([]);
      }
      setMessage(`${book.title}을 반영했습니다.`);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "책을 반영하지 못했습니다.");
    } finally {
      setPendingBookId("");
    }
  }

  return (
    <div ref={panelRef} className={`${className || "mt-5"} relative rounded-lg border border-[#E5E7EB] bg-[#F9FAFB] p-4`}>
      <div className="flex flex-col gap-3 sm:flex-row">
        <label className="min-w-0 flex-1">
          <span className="sr-only">{title}</span>
          <input
            className="w-full rounded-md border border-[#E5E7EB] bg-white px-4 py-3 text-sm text-[#1E2A38] outline-none transition placeholder:text-[#9CA3AF] focus:border-[#1E2A38]"
            onChange={(event) => setQuery(event.target.value)}
            onKeyDown={handleSearchKeyDown}
            placeholder="제목 또는 저자 검색"
            type="search"
            value={query}
          />
        </label>
        <button
          className="rounded-md bg-[#1E2A38] px-4 py-3 text-sm font-semibold text-white transition hover:bg-[#27384a] disabled:cursor-not-allowed disabled:bg-[#9CA3AF]"
          disabled={status === "loading"}
          onClick={searchBooks}
          type="button"
        >
          {status === "loading" ? "검색 중" : "검색"}
        </button>
        {actionSlot}
      </div>

      {reserveMessageSpace ? (
        <div className="mt-3 min-h-5">
          {message ? <p className={`text-sm ${status === "error" ? "text-red-600" : "text-[#6B7280]"}`}>{message}</p> : null}
        </div>
      ) : message ? (
        <p className={`mt-3 text-sm ${status === "error" ? "text-red-600" : "text-[#6B7280]"}`}>{message}</p>
      ) : null}

      {results.length > 0 ? (
        <div
          className={`${
            floatingResults ? "absolute left-0 right-0 top-full z-10 mt-2 max-h-96 overflow-y-auto" : "mt-4"
          } divide-y divide-[#E5E7EB] rounded-md border border-[#E5E7EB] bg-white shadow-sm`}
        >
          {results.map((book) => {
            const isSelected = selectedIdSet.has(String(book.id));
            const isDisabled = disabledIdSet.has(String(book.id));
            const isPending = pendingBookId === book.id;

            return (
              <div className="flex flex-col gap-3 p-4 md:flex-row md:items-center md:justify-between" key={book.id}>
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="font-bold text-[#1E2A38]">{book.title}</p>
                    <span className="rounded-full border border-[#E5E7EB] px-2 py-1 text-xs text-[#6B7280]">{book.category}</span>
                  </div>
                  <p className="mt-1 text-sm text-[#6B7280]">{book.author}</p>
                  <p className="mt-2 line-clamp-2 text-sm leading-6 text-[#6B7280]">{book.recommendationReason}</p>
                </div>
                <button
                  className={`shrink-0 rounded-md px-3 py-2 text-sm font-semibold transition ${
                    isSelected || isDisabled
                      ? "bg-[#E5E7EB] text-[#6B7280]"
                      : "bg-[#4CAF50] text-white hover:bg-[#3f9744]"
                  } disabled:cursor-not-allowed disabled:opacity-70`}
                  disabled={isSelected || isDisabled || Boolean(pendingBookId)}
                  onClick={() => selectBook(book)}
                  type="button"
                >
                  {isPending ? "반영 중" : isSelected || isDisabled ? selectedLabel : actionLabel}
                </button>
              </div>
            );
          })}
        </div>
      ) : null}
    </div>
  );
}
