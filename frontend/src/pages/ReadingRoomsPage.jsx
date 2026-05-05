import { useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../App";
import BookSearchPanel from "../components/BookSearchPanel";

const statusOptions = [
  ["", "전체"],
  ["RECRUITING", "모집 중"],
  ["IN_PROGRESS", "진행 중"],
  ["ENDED", "종료"],
];

function statusLabel(status) {
  if (status === "RECRUITING") return "모집 중";
  if (status === "IN_PROGRESS") return "진행 중";
  if (status === "ENDED") return "종료";
  if (status === "CANCELED") return "취소";
  return status ?? "";
}

function participationLabel(status) {
  if (status === "JOINED") return "참여 중";
  if (status === "CANCELED") return "취소됨";
  if (status === "COMPLETED") return "인증 완료";
  return status ?? "";
}

function formatDateTime(value) {
  if (!value) return "";
  return new Date(value).toLocaleString("ko-KR", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function RoomCard({ room }) {
  return (
    <article className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <span className="rounded-full bg-[#4CAF50]/10 px-3 py-1 text-xs font-semibold text-[#2E7D32]">
            {statusLabel(room.status)}
          </span>
          <h2 className="mt-3 text-xl font-bold text-[#1E2A38]">{room.title}</h2>
          <p className="mt-2 text-sm text-[#6B7280]">
            {room.book?.title} · {room.book?.author}
          </p>
          <p className="mt-3 text-sm leading-6 text-[#6B7280]">{room.description || "설명 없음"}</p>
        </div>
        <div className="shrink-0 text-sm text-[#6B7280] sm:text-right">
          <p>{formatDateTime(room.startAt)}</p>
          <p className="mt-1">~ {formatDateTime(room.endAt)}</p>
          <p className="mt-3 font-semibold text-[#1E2A38]">
            {room.participantCount}/{room.maxParticipants}명
          </p>
        </div>
      </div>
      <div className="mt-5 flex flex-wrap items-center gap-2">
        {room.myParticipationStatus ? (
          <span className="rounded-md border border-[#E5E7EB] px-3 py-2 text-sm text-[#6B7280]">
            내 상태: {participationLabel(room.myParticipationStatus)}
          </span>
        ) : null}
        <Link className="rounded-md bg-[#1E2A38] px-4 py-2 text-sm font-semibold text-white" to={`/reading-rooms/${room.id}`}>
          상세 보기
        </Link>
      </div>
    </article>
  );
}

function toLocalInputValue(date) {
  const pad = (value) => String(value).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function normalizeBook(book) {
  if (!book) {
    return null;
  }
  return {
    id: String(book.id ?? book.bookId ?? ""),
    title: book.title ?? "",
    author: book.author ?? "",
    category: book.category ?? book.categoryName ?? "",
  };
}

export default function ReadingRoomsPage() {
  const { accessToken, currentUser, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const query = useMemo(() => new URLSearchParams(location.search), [location.search]);
  const initialBookId = query.get("bookId") ?? "";
  const [rooms, setRooms] = useState([]);
  const [status, setStatus] = useState("");
  const [filterBookId, setFilterBookId] = useState(initialBookId);
  const [filterBook, setFilterBook] = useState(null);
  const [selectedBook, setSelectedBook] = useState(null);
  const [loadState, setLoadState] = useState("loading");
  const [message, setMessage] = useState("");
  const [form, setForm] = useState(() => {
    const start = new Date(Date.now() + 60 * 60 * 1000);
    const end = new Date(Date.now() + 2 * 60 * 60 * 1000);
    return {
      title: "",
      description: "",
      startAt: toLocalInputValue(start),
      endAt: toLocalInputValue(end),
      maxParticipants: "5",
    };
  });

  async function loadRooms(nextStatus = status, nextBookId = filterBookId) {
    setLoadState("loading");
    setMessage("");
    const params = new URLSearchParams({ limit: "30" });
    if (nextStatus) params.set("status", nextStatus);
    if (nextBookId) params.set("bookId", nextBookId);
    try {
      const response = await fetch(`/api/reading-rooms?${params.toString()}`, {
        headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : {},
      });
      if (response.status === 401 && accessToken) {
        logout();
        return;
      }
      if (!response.ok) {
        throw new Error("모각독 목록을 불러오지 못했습니다.");
      }
      const data = await response.json();
      setRooms(Array.isArray(data) ? data : []);
      setLoadState("ready");
    } catch (error) {
      setRooms([]);
      setLoadState("error");
      setMessage(error instanceof Error ? error.message : "모각독 목록을 불러오지 못했습니다.");
    }
  }

  useEffect(() => {
    loadRooms(status, filterBookId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status, filterBookId, accessToken]);

  useEffect(() => {
    let ignore = false;

    async function loadSelectedBook() {
      if (!initialBookId) {
        return;
      }

      try {
        const response = await fetch(`/api/books/${initialBookId}`);
        if (!response.ok) {
          return;
        }
        const data = await response.json();
        if (!ignore) {
          setFilterBook(normalizeBook(data));
          setSelectedBook(normalizeBook(data));
        }
      } catch {
        // 책 상세에서 넘어오지 않은 경우는 무시한다.
      }
    }

    loadSelectedBook();

    return () => {
      ignore = true;
    };
  }, [initialBookId]);

  function updateForm(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  function validateCreateForm() {
    const startAt = new Date(form.startAt);
    const endAt = new Date(form.endAt);
    const maxParticipants = Number(form.maxParticipants);

    if (!selectedBook?.id) return "책을 먼저 선택해 주세요.";
    if (!form.title.trim()) return "방 제목을 입력해 주세요.";
    if (!form.startAt || !form.endAt || Number.isNaN(startAt.getTime()) || Number.isNaN(endAt.getTime())) {
      return "시작 시간과 종료 시간을 입력해 주세요.";
    }
    if (startAt <= new Date()) return "시작 시간은 현재보다 이후여야 합니다.";
    if (endAt <= startAt) return "종료 시간은 시작 시간보다 이후여야 합니다.";

    const durationMinutes = (endAt.getTime() - startAt.getTime()) / 60000;
    if (durationMinutes < 20) return "모각독은 최소 20분 이상이어야 합니다.";
    if (durationMinutes > 240) return "모각독은 최대 4시간까지 열 수 있습니다.";
    if (!Number.isInteger(maxParticipants) || maxParticipants < 2 || maxParticipants > 30) {
      return "최대 인원은 2명에서 30명 사이로 입력해 주세요.";
    }

    return "";
  }

  async function createRoom(event) {
    event.preventDefault();
    const validationMessage = validateCreateForm();
    if (validationMessage) {
      setMessage(validationMessage);
      return;
    }
    if (!accessToken || !currentUser) {
      navigate("/login", { state: { from: location } });
      return;
    }
    setMessage("");
    try {
      const response = await fetch("/api/reading-rooms", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          bookId: Number(selectedBook.id),
          title: form.title.trim(),
          description: form.description.trim() || null,
          startAt: form.startAt,
          endAt: form.endAt,
          maxParticipants: Number(form.maxParticipants),
          idempotencyKey: `room-${Date.now()}`,
        }),
      });
      if (response.status === 401) {
        logout();
        navigate("/login", { state: { from: location } });
        return;
      }
      if (!response.ok) {
        const error = await response.json().catch(() => null);
        throw new Error(error?.message || "모각독 방을 만들지 못했습니다.");
      }
      const room = await response.json();
      navigate(`/reading-rooms/${room.id}`);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "모각독 방을 만들지 못했습니다.");
    }
  }

  return (
    <section className="mx-auto grid w-full max-w-7xl grid-cols-1 gap-6 px-5 py-8 lg:grid-cols-[320px_1fr]">
      <aside className="space-y-5">
        <div className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
          <p className="text-sm font-semibold text-[#4CAF50]">비대면 함께 읽기</p>
          <h1 className="mt-2 text-2xl font-bold text-[#1E2A38]">모각독</h1>
          <p className="mt-3 text-sm leading-6 text-[#6B7280]">같은 책을 정해진 시간에 각자 읽고, 종료 후 한 줄 인증을 남깁니다.</p>
          <Link className="mt-5 inline-flex rounded-md border border-[#E5E7EB] px-4 py-2 text-sm font-semibold text-[#1E2A38]" to="/me/reading-rooms">
            내 모각독 보기
          </Link>
        </div>

        <form className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm" onSubmit={createRoom}>
          <h2 className="text-lg font-bold text-[#1E2A38]">방 만들기</h2>
          <BookSearchPanel
            actionLabel="선택"
            emptyMessage="검색 결과가 없습니다."
            onSelect={(book) => {
              const normalized = normalizeBook(book);
              setSelectedBook(normalized);
            }}
            selectedIds={selectedBook?.id ? [selectedBook.id] : []}
            selectedLabel="선택됨"
            title="책 검색"
          />
          {selectedBook ? (
            <div className="mt-4 rounded-md border border-[#E5E7EB] bg-[#F9FAFB] p-3 text-sm text-[#1E2A38]">
              <p className="font-semibold">{selectedBook.title}</p>
              <p className="mt-1 text-[#6B7280]">{selectedBook.author}</p>
              <button
                className="mt-3 rounded-md border border-[#E5E7EB] px-3 py-2 text-xs font-semibold text-[#6B7280]"
                type="button"
                onClick={() => setSelectedBook(null)}
              >
                선택 해제
              </button>
            </div>
          ) : (
            <p className="mt-3 text-sm text-[#6B7280]">책 제목을 검색해 선택해 주세요.</p>
          )}
          <label className="mt-3 block text-sm font-semibold text-[#1E2A38]">
            제목
            <input className="mt-2 w-full rounded-md border border-[#E5E7EB] px-3 py-2 text-sm" maxLength={100} required value={form.title} onChange={(event) => updateForm("title", event.target.value)} />
          </label>
          <label className="mt-3 block text-sm font-semibold text-[#1E2A38]">
            설명
            <textarea className="mt-2 min-h-20 w-full rounded-md border border-[#E5E7EB] px-3 py-2 text-sm" maxLength={500} value={form.description} onChange={(event) => updateForm("description", event.target.value)} />
          </label>
          <label className="mt-3 block text-sm font-semibold text-[#1E2A38]">
            시작 시간
            <input className="mt-2 w-full rounded-md border border-[#E5E7EB] px-3 py-2 text-sm" required type="datetime-local" value={form.startAt} onChange={(event) => updateForm("startAt", event.target.value)} />
          </label>
          <label className="mt-3 block text-sm font-semibold text-[#1E2A38]">
            종료 시간
            <input className="mt-2 w-full rounded-md border border-[#E5E7EB] px-3 py-2 text-sm" required type="datetime-local" value={form.endAt} onChange={(event) => updateForm("endAt", event.target.value)} />
          </label>
          <label className="mt-3 block text-sm font-semibold text-[#1E2A38]">
            최대 인원
            <input className="mt-2 w-full rounded-md border border-[#E5E7EB] px-3 py-2 text-sm" max="30" min="2" required type="number" value={form.maxParticipants} onChange={(event) => updateForm("maxParticipants", event.target.value)} />
          </label>
          <button className="mt-5 w-full rounded-md bg-[#1E2A38] px-4 py-2 text-sm font-semibold text-white disabled:opacity-60" disabled={!selectedBook} type="submit">
            {accessToken ? "모각독 열기" : "로그인 후 모각독 열기"}
          </button>
        </form>
      </aside>

      <div className="space-y-5">
        <div className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
          <div className="rounded-md border border-[#E5E7EB] bg-white px-3 py-2 text-sm text-[#6B7280]">
            <div className="flex items-center justify-between gap-3">
              <span className="min-w-0 truncate font-semibold text-[#1E2A38]">
                {filterBook ? `${filterBook.title}${filterBook.author ? ` · ${filterBook.author}` : ""}` : "모각독 찾기"}
              </span>
              {filterBook ? (
                <button
                  className="shrink-0 rounded-md border border-[#E5E7EB] px-2 py-1 text-xs font-semibold text-[#6B7280]"
                  type="button"
                  onClick={() => {
                    setFilterBook(null);
                    setFilterBookId("");
                  }}
                >
                  해제
                </button>
              ) : null}
            </div>
            <BookSearchPanel
              actionLabel="필터"
              actionSlot={
                <select className="w-full shrink-0 rounded-md border border-[#E5E7EB] bg-white px-3 py-3 text-sm sm:w-44" value={status} onChange={(event) => setStatus(event.target.value)}>
                  {statusOptions.map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              }
              className="mt-3"
              emptyMessage="검색 결과가 없습니다."
              floatingResults
              onSelect={(book) => {
                const normalized = normalizeBook(book);
                setFilterBook(normalized);
                setFilterBookId(normalized?.id ?? "");
              }}
              reserveMessageSpace
              selectedIds={filterBook?.id ? [filterBook.id] : []}
              selectedLabel="적용됨"
              title="모각독 검색"
            />
          </div>
        </div>

        {message ? <div className="rounded-lg border border-[#E5E7EB] bg-white p-4 text-sm text-[#6B7280]">{message}</div> : null}
        {loadState === "loading" ? <div className="rounded-lg border border-[#E5E7EB] bg-white p-5 text-sm text-[#6B7280]">모각독 목록을 불러오는 중입니다.</div> : null}
        {loadState !== "loading" && rooms.length === 0 ? <div className="rounded-lg border border-[#E5E7EB] bg-white p-5 text-sm text-[#6B7280]">조건에 맞는 모각독이 없습니다.</div> : null}
        <div className="space-y-4">
          {rooms.map((room) => (
            <RoomCard key={room.id} room={room} />
          ))}
        </div>
      </div>
    </section>
  );
}
