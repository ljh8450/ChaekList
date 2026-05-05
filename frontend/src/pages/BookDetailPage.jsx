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

const evidenceStyles = {
  CATEGORY: "border-[#4CAF50]/30 bg-[#4CAF50]/10 text-[#2f6f34]",
  KEYWORD: "border-[#F59E0B]/30 bg-[#F59E0B]/10 text-[#9a6207]",
  FILTER: "border-[#1E2A38]/20 bg-[#1E2A38]/5 text-[#1E2A38]",
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
    filterReport: book.filterReport
      ? {
          ...book.filterReport,
          keywords: book.filterReport.keywords ?? [],
        }
      : null,
    recommendationEvidence: book.recommendationEvidence ?? [],
    readingGuide: book.readingGuide ?? null,
    saved: Boolean(book.saved),
    read: Boolean(book.read),
    dismissed: Boolean(book.dismissed),
  };
}

function hasFilterReport(report) {
  return Boolean(report?.status || report?.reason || report?.category || report?.keywords?.length);
}

function hasReadingGuide(readingGuide) {
  return Boolean(readingGuide?.fit || readingGuide?.similarityNote);
}

function FilterReportSection({ report }) {
  if (!hasFilterReport(report)) {
    return null;
  }

  const fields = [
    ["상태", report.status],
    ["대표 분야", report.category],
    ["판단 근거", report.reason],
  ].filter(([, value]) => value);

  return (
    <section className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
      <div>
        <p className="text-sm font-semibold text-[#4CAF50]">교양 필터 리포트</p>
        <h2 className="mt-1 text-xl font-bold text-[#1E2A38]">이 책이 상세 후보에 오른 이유</h2>
      </div>
      <div className="mt-5 grid grid-cols-1 gap-3 sm:grid-cols-3">
        {fields.map(([label, value]) => (
          <div className="rounded-md border border-[#E5E7EB] p-4" key={label}>
            <p className="text-xs font-semibold text-[#6B7280]">{label}</p>
            <p className="mt-2 text-sm font-bold text-[#1E2A38]">{value}</p>
          </div>
        ))}
      </div>
      {report.keywords?.length ? (
        <div className="mt-4 flex flex-wrap gap-2">
          {report.keywords.slice(0, 6).map((keyword) => (
            <span className="rounded-full border border-[#E5E7EB] px-3 py-2 text-sm text-[#6B7280]" key={keyword}>
              {keyword}
            </span>
          ))}
        </div>
      ) : null}
    </section>
  );
}

function RecommendationEvidenceSection({ evidence }) {
  const visibleEvidence = (evidence ?? []).filter((item) => item?.label || item?.description).slice(0, 3);
  if (!visibleEvidence.length) {
    return null;
  }

  return (
    <section className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
      <div>
        <p className="text-sm font-semibold text-[#4CAF50]">추천 근거</p>
        <h2 className="mt-1 text-xl font-bold text-[#1E2A38]">어떤 기준으로 볼 만한 책인가</h2>
      </div>
      <div className="mt-5 grid grid-cols-1 gap-3 md:grid-cols-3">
        {visibleEvidence.map((item, index) => (
          <article
            className={`rounded-md border p-4 ${evidenceStyles[item.type] ?? "border-[#E5E7EB] bg-white text-[#1E2A38]"}`}
            key={`${item.type ?? "EVIDENCE"}-${index}`}
          >
            {item.label ? <h3 className="text-sm font-bold">{item.label}</h3> : null}
            {item.description ? <p className="mt-2 text-sm leading-6 text-[#6B7280]">{item.description}</p> : null}
          </article>
        ))}
      </div>
    </section>
  );
}

function ReadingGuideSection({ readingGuide }) {
  if (!hasReadingGuide(readingGuide)) {
    return null;
  }

  return (
    <section className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
      <div>
        <p className="text-sm font-semibold text-[#4CAF50]">읽을 책 결정 보조</p>
        <h2 className="mt-1 text-xl font-bold text-[#1E2A38]">고르기 전에 볼 포인트</h2>
      </div>
      <div className="mt-5 grid grid-cols-1 gap-3 md:grid-cols-2">
        {readingGuide.fit ? (
          <div className="rounded-md border border-[#E5E7EB] p-4">
            <p className="text-xs font-semibold text-[#6B7280]">맞는 사용자</p>
            <p className="mt-2 text-sm leading-6 text-[#111827]">{readingGuide.fit}</p>
          </div>
        ) : null}
        {readingGuide.similarityNote ? (
          <div className="rounded-md border border-[#E5E7EB] p-4">
            <p className="text-xs font-semibold text-[#6B7280]">비슷한 책과 비교</p>
            <p className="mt-2 text-sm leading-6 text-[#111827]">{readingGuide.similarityNote}</p>
          </div>
        ) : null}
      </div>
    </section>
  );
}

function formatRoomSchedule(room) {
  const schedules = Array.isArray(room.schedules) ? room.schedules : [];
  if (schedules.length > 0) {
    return schedules
      .map((schedule) => {
        const duration = Number(schedule.durationMinutes ?? 0);
        const durationLabel = duration >= 60 && duration % 60 === 0 ? `${duration / 60}시간` : `${duration}분`;
        return `${schedule.dayLabel} ${String(schedule.scheduledTime ?? "").slice(0, 5)} · ${durationLabel}`;
      })
      .join(" / ");
  }
  return "일정 없음";
}

function roomStatusLabel(status) {
  if (status === "RECRUITING") {
    return "모집 중";
  }
  if (status === "IN_PROGRESS") {
    return "진행 중";
  }
  if (status === "ENDED") {
    return "종료";
  }
  return status ?? "";
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
  const [detailImageFailed, setDetailImageFailed] = useState(false);
  const [readingRooms, setReadingRooms] = useState([]);
  const [readingRoomsStatus, setReadingRoomsStatus] = useState("idle");

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

  useEffect(() => {
    setDetailImageFailed(false);
  }, [book?.imageUrl]);

  useEffect(() => {
    let ignore = false;

    async function loadReadingRooms() {
      setReadingRoomsStatus("loading");
      try {
        const response = await fetch(`/api/books/${bookId}/reading-rooms?limit=5`, {
          headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : {},
        });
        if (!response.ok) {
          throw new Error("Reading rooms unavailable.");
        }
        const data = await response.json();
        if (!ignore) {
          setReadingRooms(Array.isArray(data) ? data : []);
          setReadingRoomsStatus("ready");
        }
      } catch {
        if (!ignore) {
          setReadingRooms([]);
          setReadingRoomsStatus("error");
        }
      }
    }

    if (isAuthReady) {
      loadReadingRooms();
    }

    return () => {
      ignore = true;
    };
  }, [accessToken, bookId, isAuthReady]);

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
  const showDetailImage = book.imageUrl && !detailImageFailed;

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
        {showDetailImage ? (
          <img
            alt={`${book.title} 표지`}
            className="aspect-[3/4] w-full rounded-lg object-cover shadow-sm"
            onError={() => setDetailImageFailed(true)}
            src={book.imageUrl}
          />
        ) : (
          <div className={`flex aspect-[3/4] items-end rounded-lg ${book.cover} p-5 text-lg font-bold text-white shadow-sm`}>
            {book.category}
          </div>
        )}
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

      {hasFilterReport(book.filterReport) || book.recommendationEvidence?.length || hasReadingGuide(book.readingGuide) ? (
        <div className="mt-6 grid grid-cols-1 gap-4">
          <FilterReportSection report={book.filterReport} />
          <RecommendationEvidenceSection evidence={book.recommendationEvidence} />
          <ReadingGuideSection readingGuide={book.readingGuide} />
        </div>
      ) : null}

      <section className="mt-6 rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-sm font-semibold text-[#4CAF50]">함께 읽기</p>
            <h2 className="mt-1 text-xl font-bold text-[#1E2A38]">이 책의 모각독</h2>
          </div>
          <Link className="rounded-md bg-[#1E2A38] px-4 py-2 text-sm font-semibold text-white" to={`/reading-rooms?bookId=${bookId}`}>
            이 책으로 모각독 열기
          </Link>
        </div>
        {readingRoomsStatus === "loading" ? (
          <p className="mt-4 text-sm text-[#6B7280]">모각독을 불러오는 중입니다.</p>
        ) : null}
        {readingRoomsStatus !== "loading" && readingRooms.length === 0 ? (
          <p className="mt-4 text-sm text-[#6B7280]">아직 예정된 모각독이 없습니다.</p>
        ) : null}
        {readingRooms.length > 0 ? (
          <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2">
            {readingRooms.map((room) => (
              <Link className="rounded-md border border-[#E5E7EB] p-4 transition hover:border-[#1E2A38]" key={room.id} to={`/reading-rooms/${room.id}`}>
                <span className="rounded-full bg-[#4CAF50]/10 px-3 py-1 text-xs font-semibold text-[#2E7D32]">
                  {roomStatusLabel(room.status)}
                </span>
                <h3 className="mt-3 text-base font-bold text-[#1E2A38]">{room.title}</h3>
                <p className="mt-2 text-sm text-[#6B7280]">{formatRoomSchedule(room)}</p>
                <p className="mt-2 text-sm font-semibold text-[#1E2A38]">
                  {room.participantCount}/{room.maxParticipants}명
                </p>
              </Link>
            ))}
          </div>
        ) : null}
      </section>

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
