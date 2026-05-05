import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../App";

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
  return status ?? "미참여";
}

function actionNotice(room, currentUser) {
  if (!currentUser) return "참여와 인증은 로그인 후 이용할 수 있습니다.";
  if (room.canJoin || room.canCancel || room.canCheckIn) return "";
  if (room.myParticipationStatus === "COMPLETED") return "인증을 완료한 모각독입니다.";
  if (room.myParticipationStatus === "CANCELED") return "참여를 취소한 모각독입니다.";
  if (room.status === "RECRUITING" && room.participantCount >= room.maxParticipants) return "정원이 모두 찼습니다.";
  if (room.status === "IN_PROGRESS") return "이미 시작된 모각독입니다.";
  if (room.status === "ENDED") return "종료된 모각독입니다.";
  return "";
}

function formatDateTime(value) {
  if (!value) return "";
  return new Date(value).toLocaleString("ko-KR", {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function durationLabel(minutes) {
  const value = Number(minutes ?? 0);
  if (value >= 60 && value % 60 === 0) return `${value / 60}시간`;
  return `${value}분`;
}

function formatSchedule(room) {
  const schedules = Array.isArray(room.schedules) ? room.schedules : [];
  if (schedules.length > 0) {
    return schedules
      .map((schedule) => `${schedule.dayLabel} ${String(schedule.scheduledTime ?? "").slice(0, 5)} · ${durationLabel(schedule.durationMinutes)}`)
      .join(" / ");
  }
  return "일정 없음";
}

export default function ReadingRoomDetailPage() {
  const { roomId } = useParams();
  const { accessToken, currentUser, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [room, setRoom] = useState(null);
  const [note, setNote] = useState("");
  const [progress, setProgress] = useState("");
  const [status, setStatus] = useState("loading");
  const [message, setMessage] = useState("");
  const [pending, setPending] = useState("");
  const [shareText, setShareText] = useState("");
  const [shareMessage, setShareMessage] = useState("");
  const [sharing, setSharing] = useState(false);

  async function loadRoom() {
    setStatus("loading");
    setMessage("");
    try {
      const response = await fetch(`/api/reading-rooms/${roomId}`, {
        headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : {},
      });
      if (response.status === 401 && accessToken) {
        logout();
        return;
      }
      if (!response.ok) {
        throw new Error(response.status === 404 ? "모각독 방을 찾을 수 없습니다." : "모각독 방을 불러오지 못했습니다.");
      }
      setRoom(await response.json());
      setStatus("ready");
    } catch (error) {
      setRoom(null);
      setStatus("error");
      setMessage(error instanceof Error ? error.message : "모각독 방을 불러오지 못했습니다.");
    }
  }

  useEffect(() => {
    loadRoom();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roomId, accessToken]);

  async function authedRequest(path, options = {}) {
    if (!accessToken || !currentUser) {
      navigate("/login", { state: { from: location } });
      return null;
    }
    const response = await fetch(path, {
      ...options,
      headers: {
        Authorization: `Bearer ${accessToken}`,
        ...(options.body ? { "Content-Type": "application/json" } : {}),
        ...(options.headers ?? {}),
      },
    });
    if (response.status === 401) {
      logout();
      navigate("/login", { state: { from: location } });
      return null;
    }
    if (!response.ok) {
      const error = await response.json().catch(() => null);
      throw new Error(error?.message || "요청을 처리하지 못했습니다.");
    }
    return response.json();
  }

  async function joinRoom() {
    setPending("join");
    setMessage("");
    try {
      const data = await authedRequest(`/api/reading-rooms/${roomId}/participants`, { method: "POST" });
      if (data?.room) {
        setRoom(data.room);
        setMessage("모각독에 참여했습니다.");
      }
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "참여하지 못했습니다.");
    } finally {
      setPending("");
    }
  }

  async function cancelParticipation() {
    setPending("cancel");
    setMessage("");
    try {
      const data = await authedRequest(`/api/reading-rooms/${roomId}/participants/me`, { method: "DELETE" });
      if (data?.room) {
        setRoom(data.room);
        setMessage("참여를 취소했습니다.");
      }
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "참여 취소에 실패했습니다.");
    } finally {
      setPending("");
    }
  }

  async function startRoom() {
    setPending("start");
    setMessage("");
    try {
      const data = await authedRequest(`/api/reading-rooms/${roomId}/start`, { method: "POST" });
      if (data) {
        setRoom(data);
        setMessage("모각독을 시작했습니다.");
      }
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "모각독을 시작하지 못했습니다.");
    } finally {
      setPending("");
    }
  }

  async function cancelRoom() {
    setPending("room-cancel");
    setMessage("");
    try {
      const data = await authedRequest(`/api/reading-rooms/${roomId}`, { method: "DELETE" });
      if (data) {
        setRoom(data);
        setMessage("모각독을 취소했습니다.");
      }
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "모각독을 취소하지 못했습니다.");
    } finally {
      setPending("");
    }
  }

  async function checkIn(event) {
    event.preventDefault();
    setPending("checkin");
    setMessage("");
    try {
      const data = await authedRequest(`/api/reading-rooms/${roomId}/checkins`, {
        method: "POST",
        body: JSON.stringify({
          note: note.trim() || null,
          progress: progress.trim() || null,
        }),
      });
      if (data?.room) {
        setRoom(data.room);
        setNote("");
        setProgress("");
        setMessage("인증을 완료했습니다.");
      }
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "인증하지 못했습니다.");
    } finally {
      setPending("");
    }
  }

  async function shareReadingRoom() {
    if (!room?.myParticipationStatus || room.myParticipationStatus !== "COMPLETED") {
      setShareMessage("인증을 완료한 뒤에만 공유할 수 있습니다.");
      return;
    }
    setSharing(true);
    setShareMessage("");
    try {
      const response = await fetch("/api/social/posts", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          postType: "READING_ROOM",
          sourceInteractionId: room.id,
          content: shareText.trim() || `${room.title}\n${room.book?.title ?? ""}`.trim(),
          visibility: "PUBLIC",
          idempotencyKey: `reading-room-share-${room.id}-${Date.now()}`,
        }),
      });
      if (response.status === 401) {
        logout();
        navigate("/login", { state: { from: location } });
        return;
      }
      if (!response.ok) {
        const error = await response.json().catch(() => null);
        throw new Error(error?.message || "모각독 공유에 실패했습니다.");
      }
      setShareMessage("모각독이 공유되었습니다.");
      setShareText("");
    } catch (error) {
      setShareMessage(error instanceof Error ? error.message : "모각독 공유에 실패했습니다.");
    } finally {
      setSharing(false);
    }
  }

  if (status === "loading") {
    return <section className="mx-auto w-full max-w-5xl px-5 py-8"><div className="rounded-lg border border-[#E5E7EB] bg-white p-5 text-sm text-[#6B7280]">모각독 방을 불러오는 중입니다.</div></section>;
  }

  if (!room) {
    return <section className="mx-auto w-full max-w-5xl px-5 py-8"><div className="rounded-lg border border-[#E5E7EB] bg-white p-5 text-sm text-[#6B7280]">{message}</div></section>;
  }

  const notice = actionNotice(room, currentUser);

  return (
    <section className="mx-auto w-full max-w-5xl px-5 py-8">
      <Link className="text-sm font-semibold text-[#6B7280] hover:text-[#1E2A38]" to="/reading-rooms">모각독 목록</Link>
      <div className="mt-5 rounded-lg border border-[#E5E7EB] bg-white p-6 shadow-sm">
        <span className="rounded-full bg-[#4CAF50]/10 px-3 py-1 text-xs font-semibold text-[#2E7D32]">{statusLabel(room.status)}</span>
        <h1 className="mt-4 text-3xl font-bold text-[#1E2A38]">{room.title}</h1>
        <p className="mt-2 text-sm text-[#6B7280]">방장 {room.hostNickname}</p>
        <div className="mt-5 grid grid-cols-1 gap-3 sm:grid-cols-3">
          <div className="rounded-md border border-[#E5E7EB] p-4">
            <p className="text-xs font-semibold text-[#6B7280]">책</p>
            <p className="mt-2 text-sm font-bold text-[#1E2A38]">{room.book?.title}</p>
          </div>
          <div className="rounded-md border border-[#E5E7EB] p-4">
            <p className="text-xs font-semibold text-[#6B7280]">일정</p>
            <p className="mt-2 text-sm text-[#1E2A38]">{formatSchedule(room)}</p>
            {room.startedAt ? <p className="mt-1 text-sm text-[#6B7280]">실제 시작 {formatDateTime(room.startedAt)}</p> : null}
            {room.startedAt ? <p className="text-sm text-[#6B7280]">예상 종료 {formatDateTime(room.endAt)}</p> : null}
          </div>
          <div className="rounded-md border border-[#E5E7EB] p-4">
            <p className="text-xs font-semibold text-[#6B7280]">참여</p>
            <p className="mt-2 text-sm font-bold text-[#1E2A38]">{room.participantCount}/{room.maxParticipants}명</p>
            <p className="mt-1 text-sm text-[#6B7280]">내 상태 {participationLabel(room.myParticipationStatus)}</p>
          </div>
        </div>
        <p className="mt-5 leading-7 text-[#111827]">{room.description || "설명 없음"}</p>
        <div className="mt-6 flex flex-wrap gap-2">
          {!currentUser ? <Link className="rounded-md bg-[#1E2A38] px-4 py-2 text-sm font-semibold text-white" to="/login" state={{ from: location }}>로그인 후 참여</Link> : null}
          {room.canJoin ? <button className="rounded-md bg-[#1E2A38] px-4 py-2 text-sm font-semibold text-white disabled:opacity-60" disabled={pending === "join"} type="button" onClick={joinRoom}>참여하기</button> : null}
          {room.canCancel ? <button className="rounded-md border border-[#E5E7EB] px-4 py-2 text-sm font-semibold text-[#1E2A38] disabled:opacity-60" disabled={pending === "cancel"} type="button" onClick={cancelParticipation}>참여 취소</button> : null}
          {room.mine && room.status === "RECRUITING" ? <button className="rounded-md bg-[#4CAF50] px-4 py-2 text-sm font-semibold text-white disabled:opacity-60" disabled={pending === "start"} type="button" onClick={startRoom}>{pending === "start" ? "시작 중" : "모각독 시작"}</button> : null}
          {room.mine && room.status !== "CANCELED" ? <button className="rounded-md border border-red-200 px-4 py-2 text-sm font-semibold text-red-600 disabled:opacity-60" disabled={pending === "room-cancel"} type="button" onClick={cancelRoom}>{pending === "room-cancel" ? "취소 중" : "모각독 취소"}</button> : null}
          <Link className="rounded-md border border-[#E5E7EB] px-4 py-2 text-sm font-semibold text-[#1E2A38]" to={`/books/${room.book?.id}`}>책 상세</Link>
        </div>
        {notice ? <p className="mt-4 text-sm font-medium text-[#6B7280]">{notice}</p> : null}
        {message ? <p className="mt-4 text-sm font-medium text-[#6B7280]">{message}</p> : null}
      </div>

      {room.canCheckIn ? (
        <form className="mt-5 rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm" onSubmit={checkIn}>
          <h2 className="text-xl font-bold text-[#1E2A38]">종료 후 인증</h2>
          <label className="mt-4 block text-sm font-semibold text-[#1E2A38]">
            한 줄 인증
            <textarea className="mt-2 min-h-24 w-full rounded-md border border-[#E5E7EB] px-3 py-2 text-sm" maxLength={300} value={note} onChange={(event) => setNote(event.target.value)} />
          </label>
          <label className="mt-3 block text-sm font-semibold text-[#1E2A38]">
            읽은 분량
            <input className="mt-2 w-full rounded-md border border-[#E5E7EB] px-3 py-2 text-sm" maxLength={100} value={progress} onChange={(event) => setProgress(event.target.value)} />
          </label>
          <button className="mt-4 rounded-md bg-[#4CAF50] px-4 py-2 text-sm font-semibold text-white disabled:opacity-60" disabled={pending === "checkin" || (!note.trim() && !progress.trim())} type="submit">인증 완료</button>
        </form>
      ) : null}

      {room.myParticipationStatus === "COMPLETED" ? (
        <section className="mt-5 rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
          <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <p className="text-sm font-semibold text-[#4CAF50]">공유하기</p>
              <h2 className="mt-1 text-xl font-bold text-[#1E2A38]">모각독 인증 게시물</h2>
            </div>
          </div>
          <textarea
            className="mt-4 min-h-24 w-full rounded-md border border-[#E5E7EB] px-3 py-2 text-sm"
            maxLength={1000}
            value={shareText}
            onChange={(event) => setShareText(event.target.value)}
            placeholder={`${room.title}\n${room.book?.title ?? ""}`}
          />
          <div className="mt-3 flex flex-wrap items-center gap-3">
            <button
              className="rounded-md bg-[#1E2A38] px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
              disabled={sharing}
              type="button"
              onClick={shareReadingRoom}
            >
              모각독 공유
            </button>
            <Link className="rounded-md border border-[#E5E7EB] px-4 py-2 text-sm font-semibold text-[#1E2A38]" to="/social">
              피드 보기
            </Link>
          </div>
          {shareMessage ? <p className="mt-3 text-sm font-medium text-[#6B7280]">{shareMessage}</p> : null}
        </section>
      ) : null}
    </section>
  );
}
