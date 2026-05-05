import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../App";

const filters = [
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
  return status ?? "미참여";
}

function roomStateLabel(room) {
  if (room.canCheckIn) return "인증 필요";
  if (room.myParticipationStatus === "COMPLETED") return "완료";
  if (room.myParticipationStatus === "CANCELED") return "취소";
  if (room.status === "RECRUITING") return "진행 예정";
  if (room.status === "IN_PROGRESS") return "읽는 중";
  if (room.status === "ENDED") return "종료";
  return statusLabel(room.status);
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

export default function MyReadingRoomsPage() {
  const { accessToken, logout } = useAuth();
  const [rooms, setRooms] = useState([]);
  const [filter, setFilter] = useState("");
  const [status, setStatus] = useState("loading");
  const [message, setMessage] = useState("");

  async function loadRooms(nextFilter = filter) {
    setStatus("loading");
    setMessage("");
    const params = new URLSearchParams({ limit: "50" });
    if (nextFilter) params.set("status", nextFilter);
    try {
      const response = await fetch(`/api/me/reading-rooms?${params.toString()}`, {
        headers: { Authorization: `Bearer ${accessToken}` },
      });
      if (response.status === 401) {
        logout();
        return;
      }
      if (!response.ok) {
        throw new Error("내 모각독을 불러오지 못했습니다.");
      }
      setRooms(await response.json());
      setStatus("ready");
    } catch (error) {
      setRooms([]);
      setStatus("error");
      setMessage(error instanceof Error ? error.message : "내 모각독을 불러오지 못했습니다.");
    }
  }

  useEffect(() => {
    loadRooms(filter);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter, accessToken]);

  return (
    <section className="mx-auto w-full max-w-6xl px-5 py-8">
      <div className="flex flex-col gap-4 rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-semibold text-[#4CAF50]">내 독서 약속</p>
          <h1 className="mt-2 text-2xl font-bold text-[#1E2A38]">내 모각독</h1>
        </div>
        <div className="flex flex-wrap gap-2">
          {filters.map(([value, label]) => (
            <button className={`rounded-md px-3 py-2 text-sm font-semibold ${filter === value ? "bg-[#1E2A38] text-white" : "border border-[#E5E7EB] text-[#6B7280]"}`} key={value} type="button" onClick={() => setFilter(value)}>
              {label}
            </button>
          ))}
        </div>
      </div>

      {message ? <div className="mt-5 rounded-lg border border-[#E5E7EB] bg-white p-4 text-sm text-[#6B7280]">{message}</div> : null}
      {status === "loading" ? <div className="mt-5 rounded-lg border border-[#E5E7EB] bg-white p-5 text-sm text-[#6B7280]">내 모각독을 불러오는 중입니다.</div> : null}
      {status !== "loading" && rooms.length === 0 ? <div className="mt-5 rounded-lg border border-[#E5E7EB] bg-white p-5 text-sm text-[#6B7280]">아직 참여한 모각독이 없습니다.</div> : null}
      <div className="mt-5 grid grid-cols-1 gap-4 md:grid-cols-2">
        {rooms.map((room) => (
          <article className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm" key={room.id}>
            <div className="flex items-start justify-between gap-3">
              <div>
                <span className="rounded-full bg-[#4CAF50]/10 px-3 py-1 text-xs font-semibold text-[#2E7D32]">{statusLabel(room.status)}</span>
                <h2 className="mt-3 text-xl font-bold text-[#1E2A38]">{room.title}</h2>
                <p className="mt-2 text-sm text-[#6B7280]">{room.book?.title}</p>
              </div>
              {room.mine ? <span className="rounded-md border border-[#E5E7EB] px-2 py-1 text-xs text-[#6B7280]">방장</span> : null}
            </div>
            <p className="mt-4 text-sm text-[#6B7280]">{formatSchedule(room)}</p>
            {room.startedAt ? <p className="mt-1 text-sm text-[#6B7280]">실제 시작 {formatDateTime(room.startedAt)}</p> : null}
            <div className="mt-3 flex flex-wrap gap-2">
              <span className="rounded-md bg-[#F9FAFB] px-3 py-2 text-sm font-semibold text-[#1E2A38]">{roomStateLabel(room)}</span>
              <span className="rounded-md border border-[#E5E7EB] px-3 py-2 text-sm text-[#6B7280]">내 상태 {participationLabel(room.myParticipationStatus)}</span>
            </div>
            {room.canCheckIn ? <p className="mt-3 rounded-md bg-[#F59E0B]/10 px-3 py-2 text-sm font-semibold text-[#9a6207]">인증이 필요합니다.</p> : null}
            <Link className="mt-4 inline-flex rounded-md bg-[#1E2A38] px-4 py-2 text-sm font-semibold text-white" to={`/reading-rooms/${room.id}`}>
              상세 보기
            </Link>
          </article>
        ))}
      </div>
    </section>
  );
}
