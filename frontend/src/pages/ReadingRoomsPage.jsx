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

const dayOptions = [
  [1, "일요일"],
  [2, "월요일"],
  [3, "화요일"],
  [4, "수요일"],
  [5, "목요일"],
  [6, "금요일"],
  [7, "토요일"],
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

function formatSchedule(room) {
  const schedules = Array.isArray(room.schedules) ? room.schedules : [];
  if (schedules.length > 0) {
    return schedules
      .map((schedule) => {
        const duration = Number(schedule.durationMinutes ?? 0);
        const durationText = duration >= 60 && duration % 60 === 0 ? `${duration / 60}시간` : `${duration}분`;
        return `${schedule.dayLabel} ${String(schedule.scheduledTime ?? "").slice(0, 5)} · ${durationText}`;
      })
      .join(" / ");
  }
  return "일정 없음";
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
          <p>{formatSchedule(room)}</p>
          {room.startedAt ? <p className="mt-1">시작 {formatDateTime(room.startedAt)}</p> : null}
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

function defaultSchedule() {
  return {
    dayOfWeek: 2,
    scheduledTime: "20:00",
    durationMinutes: "60",
  };
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

function createRoomErrorMessage(message) {
  if (message === "Daily reading room creation limit exceeded.") return "하루에 만들 수 있는 모각독 방 수를 초과했습니다.";
  if (message === "Schedule time must be in the future.") return "스터디 일정은 현재보다 이후여야 합니다.";
  if (message === "Start time must be in the future.") return "시작 시간은 현재보다 이후여야 합니다.";
  if (message === "End time must be after start time.") return "종료 시간은 시작 시간보다 이후여야 합니다.";
  if (message === "Reading room must be at least 20 minutes.") return "모각독은 최소 20분 이상이어야 합니다.";
  if (message === "Duration is required.") return "진행 시간을 입력해 주세요.";
  if (message === "Duration must be positive.") return "진행 시간은 20분 이상으로 입력해 주세요.";
  if (message === "Max participants must be between 2 and 30.") return "최대 인원은 2명에서 30명 사이로 입력해 주세요.";
  if (message === "Book not found.") return "선택한 책을 찾을 수 없습니다. 다시 검색해 선택해 주세요.";
  return message || "모각독 방을 만들지 못했습니다.";
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
  const [createPending, setCreatePending] = useState(false);
  const [form, setForm] = useState(() => ({
    title: "",
    description: "",
    schedules: [defaultSchedule()],
    maxParticipants: "5",
  }));

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

  function updateSchedule(index, field, value) {
    setForm((current) => ({
      ...current,
      schedules: current.schedules.map((schedule, scheduleIndex) => (
        scheduleIndex === index ? { ...schedule, [field]: value } : schedule
      )),
    }));
  }

  function addSchedule() {
    setForm((current) => ({
      ...current,
      schedules: [...current.schedules, defaultSchedule()],
    }));
  }

  function removeSchedule(index) {
    setForm((current) => ({
      ...current,
      schedules: current.schedules.filter((_, scheduleIndex) => scheduleIndex !== index),
    }));
  }

  function validateCreateForm() {
    const maxParticipants = Number(form.maxParticipants);

    if (!selectedBook?.id) return "책을 먼저 선택해 주세요.";
    if (!form.title.trim()) return "방 제목을 입력해 주세요.";
    if (!form.schedules.length) return "요일별 일정을 1개 이상 추가해 주세요.";

    for (const schedule of form.schedules) {
      const durationMinutes = Number(schedule.durationMinutes);
      if (!schedule.dayOfWeek || !schedule.scheduledTime) {
        return "요일과 시작 시각을 입력해 주세요.";
      }
      if (!Number.isInteger(durationMinutes) || durationMinutes < 20) {
        return "진행 시간은 20분 이상으로 입력해 주세요.";
      }
    }

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
    setCreatePending(true);
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
          schedules: form.schedules.map((schedule) => ({
            dayOfWeek: Number(schedule.dayOfWeek),
            scheduledTime: schedule.scheduledTime,
            durationMinutes: Number(schedule.durationMinutes),
          })),
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
        throw new Error(createRoomErrorMessage(error?.message));
      }
      const room = await response.json();
      navigate(`/reading-rooms/${room.id}`);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "모각독 방을 만들지 못했습니다.");
    } finally {
      setCreatePending(false);
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

        <form className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm" noValidate onSubmit={createRoom}>
          <h2 className="text-lg font-bold text-[#1E2A38]">방 만들기</h2>
          <div className="mt-3 rounded-md border border-[#E5E7EB] bg-white px-3 py-2 text-sm text-[#6B7280]">
            <div className="flex items-center justify-between gap-3">
              <span className="min-w-0 truncate font-semibold text-[#1E2A38]">
                {selectedBook ? `${selectedBook.title}${selectedBook.author ? ` · ${selectedBook.author}` : ""}` : "함께 읽을 책 선택"}
              </span>
              {selectedBook ? (
                <button
                  className="shrink-0 rounded-md border border-[#E5E7EB] px-2 py-1 text-xs font-semibold text-[#6B7280]"
                  type="button"
                  onClick={() => setSelectedBook(null)}
                >
                  해제
                </button>
              ) : null}
            </div>
            <BookSearchPanel
              actionLabel="선택"
              className="mt-3"
              emptyMessage="검색 결과가 없습니다."
              floatingResults
              onSelect={(book) => {
                const normalized = normalizeBook(book);
                setSelectedBook(normalized);
              }}
              reserveMessageSpace
              selectedIds={selectedBook?.id ? [selectedBook.id] : []}
              selectedLabel="선택됨"
              title="책 검색"
            />
          </div>
          <label className="mt-3 block text-sm font-semibold text-[#1E2A38]">
            제목
            <input className="mt-2 w-full rounded-md border border-[#E5E7EB] px-3 py-2 text-sm" maxLength={100} required value={form.title} onChange={(event) => updateForm("title", event.target.value)} />
          </label>
          <label className="mt-3 block text-sm font-semibold text-[#1E2A38]">
            설명
            <textarea className="mt-2 min-h-20 w-full rounded-md border border-[#E5E7EB] px-3 py-2 text-sm" maxLength={500} value={form.description} onChange={(event) => updateForm("description", event.target.value)} />
          </label>
          <div className="mt-3">
            <div className="flex items-center justify-between gap-3">
              <p className="text-sm font-semibold text-[#1E2A38]">요일별 일정</p>
              <button className="rounded-md border border-[#E5E7EB] px-3 py-2 text-xs font-semibold text-[#1E2A38]" type="button" onClick={addSchedule}>
                일정 추가
              </button>
            </div>
            <div className="mt-2 space-y-3">
              {form.schedules.map((schedule, index) => (
                <div className="rounded-md border border-[#E5E7EB] bg-[#F9FAFB] p-3" key={`${index}-${schedule.dayOfWeek}-${schedule.scheduledTime}`}>
                  <div className="grid grid-cols-1 gap-3">
                    <label className="block text-xs font-semibold text-[#6B7280]">
                      요일
                      <select className="mt-1 w-full rounded-md border border-[#E5E7EB] bg-white px-3 py-2 text-sm text-[#1E2A38]" value={schedule.dayOfWeek} onChange={(event) => updateSchedule(index, "dayOfWeek", Number(event.target.value))}>
                        {dayOptions.map(([value, label]) => (
                          <option key={value} value={value}>{label}</option>
                        ))}
                      </select>
                    </label>
                    <label className="block text-xs font-semibold text-[#6B7280]">
                      시작 시각
                      <input className="mt-1 w-full rounded-md border border-[#E5E7EB] px-3 py-2 text-sm text-[#1E2A38]" required type="time" value={schedule.scheduledTime} onChange={(event) => updateSchedule(index, "scheduledTime", event.target.value)} />
                    </label>
                    <label className="block text-xs font-semibold text-[#6B7280]">
                      진행 시간(분)
                      <input className="mt-1 w-full rounded-md border border-[#E5E7EB] px-3 py-2 text-sm text-[#1E2A38]" min="20" required type="number" value={schedule.durationMinutes} onChange={(event) => updateSchedule(index, "durationMinutes", event.target.value)} />
                    </label>
                    <button className="w-full rounded-md border border-[#E5E7EB] bg-white px-3 py-2 text-xs font-semibold text-[#6B7280] disabled:opacity-50" disabled={form.schedules.length === 1} type="button" onClick={() => removeSchedule(index)}>
                      일정 삭제
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
          <label className="mt-3 block text-sm font-semibold text-[#1E2A38]">
            최대 인원
            <input className="mt-2 w-full rounded-md border border-[#E5E7EB] px-3 py-2 text-sm" max="30" min="2" required type="number" value={form.maxParticipants} onChange={(event) => updateForm("maxParticipants", event.target.value)} />
          </label>
          <button className="mt-5 w-full rounded-md bg-[#1E2A38] px-4 py-2 text-sm font-semibold text-white disabled:opacity-60" disabled={createPending} type="submit">
            {createPending ? "개설 중" : accessToken ? "모각독 열기" : "로그인 후 모각독 열기"}
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
