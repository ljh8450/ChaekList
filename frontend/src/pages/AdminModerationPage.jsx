import { useEffect, useState } from "react";
import { useAuth } from "../App";

const statusOptions = [
  ["PENDING", "대기"],
  ["REVIEWED", "검토 완료"],
  ["REJECTED", "반려"],
];

export default function AdminModerationPage() {
  const { accessToken, currentUser, logout } = useAuth();
  const [resolvedRole, setResolvedRole] = useState(currentUser?.role ?? "");
  const [reports, setReports] = useState([]);
  const [selectedReport, setSelectedReport] = useState(null);
  const [events, setEvents] = useState([]);
  const [filter, setFilter] = useState("");
  const [status, setStatus] = useState("loading");
  const [message, setMessage] = useState("");
  const [notice, setNotice] = useState({ audience: "ALL", userId: "", title: "", message: "" });
  const [review, setReview] = useState({ status: "REVIEWED", memo: "", nicknameAction: "" });

  async function request(path, options = {}) {
    const response = await fetch(path, {
      ...options,
      headers: {
        Authorization: `Bearer ${accessToken}`,
        ...(options.body ? { "Content-Type": "application/json" } : {}),
      },
    });
    if (response.status === 401) {
      logout();
    }
    return response;
  }

  useEffect(() => {
    let ignore = false;
    async function resolveRole() {
      if (!accessToken) {
        return;
      }
      if (currentUser?.role) {
        setResolvedRole(currentUser.role);
        return;
      }
      const response = await fetch("/api/auth/me", {
        headers: { Authorization: `Bearer ${accessToken}` },
      });
      if (response.status === 401) {
        logout();
        return;
      }
      if (!response.ok) {
        return;
      }
      const user = await response.json();
      if (!ignore) {
        setResolvedRole(user.role ?? "");
      }
    }
    resolveRole();
    return () => {
      ignore = true;
    };
  }, [accessToken, currentUser?.role, logout]);

  async function loadReports(nextFilter = filter) {
    setStatus("loading");
    setMessage("");
    const params = new URLSearchParams({ limit: "50" });
    if (nextFilter) {
      params.set("status", nextFilter);
    }
    const response = await request(`/api/admin/social/reports?${params.toString()}`);
    if (!response.ok) {
      setStatus("error");
      setMessage("신고 목록을 불러오지 못했습니다.");
      return;
    }
    const data = await response.json();
    setReports(Array.isArray(data) ? data : []);
    setStatus("ready");
  }

  async function openReport(report) {
    setSelectedReport(report);
    setReview({ status: report.status ?? "REVIEWED", memo: "", nicknameAction: "" });
    const response = await request(`/api/admin/social/reports/${report.id}/events`);
    if (response.ok) {
      const data = await response.json();
      setEvents(Array.isArray(data) ? data : []);
    } else {
      setEvents([]);
    }
  }

  useEffect(() => {
    if (accessToken && resolvedRole === "ADMIN") {
      loadReports(filter);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [accessToken, filter, resolvedRole]);

  async function updateReport(event) {
    event.preventDefault();
    if (!selectedReport) {
      return;
    }
    const body = {
      status: review.status,
      memo: review.memo || null,
      nicknameAction: review.nicknameAction || null,
    };
    const response = await request(`/api/admin/social/reports/${selectedReport.id}`, {
      body: JSON.stringify(body),
      method: "PATCH",
    });
    if (!response.ok) {
      setMessage("신고 처리 내용을 저장하지 못했습니다.");
      return;
    }
    const data = await response.json();
    setSelectedReport(data);
    setMessage("신고 처리 내용을 저장했습니다.");
    await loadReports(filter);
    await openReport(data);
  }

  async function hidePost(hidden) {
    if (!selectedReport || selectedReport.targetType !== "POST") {
      return;
    }
    const response = await request(`/api/admin/social/posts/${selectedReport.targetId}/hide`, {
      body: hidden ? JSON.stringify({ reason: review.memo || "관리자 숨김" }) : undefined,
      method: hidden ? "POST" : "DELETE",
    });
    setMessage(response.ok ? (hidden ? "게시글을 숨김 처리했습니다." : "게시글 숨김을 해제했습니다.") : "게시글 숨김 상태를 변경하지 못했습니다.");
  }

  async function createNotice(event) {
    event.preventDefault();
    const body = {
      audience: notice.audience,
      userId: notice.audience === "USER" ? Number(notice.userId) : null,
      title: notice.title,
      message: notice.message,
    };
    const response = await request("/api/admin/notifications/service", {
      body: JSON.stringify(body),
      method: "POST",
    });
    if (!response.ok) {
      setMessage("서비스 공지를 생성하지 못했습니다.");
      return;
    }
    const data = await response.json();
    setNotice({ audience: "ALL", userId: "", title: "", message: "" });
    setMessage(`서비스 공지를 ${data.deliveredCount ?? 0}명에게 보냈습니다.`);
  }

  if (!resolvedRole) {
    return (
      <section className="mx-auto w-full max-w-4xl px-5 py-8">
        <div className="rounded-lg border border-[#E5E7EB] bg-white p-5 text-sm text-[#6B7280] shadow-sm">관리자 권한을 확인하는 중입니다.</div>
      </section>
    );
  }

  if (resolvedRole !== "ADMIN") {
    return (
      <section className="mx-auto w-full max-w-4xl px-5 py-8">
        <div className="rounded-lg border border-[#E5E7EB] bg-white p-5 text-sm text-[#6B7280] shadow-sm">관리자 권한이 필요합니다.</div>
      </section>
    );
  }

  return (
    <section className="mx-auto grid w-full max-w-7xl grid-cols-1 gap-6 px-5 py-8 lg:grid-cols-[360px_1fr]">
      <aside className="space-y-5">
        <div className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
          <p className="text-sm font-semibold text-[#4CAF50]">관리자</p>
          <h1 className="mt-2 text-2xl font-bold text-[#1E2A38]">Moderation</h1>
          <div className="mt-4 flex flex-wrap gap-2">
            <button className={`rounded-md px-3 py-2 text-sm font-semibold ${filter === "" ? "bg-[#1E2A38] text-white" : "border border-[#E5E7EB] text-[#6B7280]"}`} type="button" onClick={() => setFilter("")}>
              전체
            </button>
            {statusOptions.map(([value, label]) => (
              <button className={`rounded-md px-3 py-2 text-sm font-semibold ${filter === value ? "bg-[#1E2A38] text-white" : "border border-[#E5E7EB] text-[#6B7280]"}`} key={value} type="button" onClick={() => setFilter(value)}>
                {label}
              </button>
            ))}
          </div>
        </div>

        <form className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm" onSubmit={createNotice}>
          <h2 className="text-lg font-bold text-[#1E2A38]">서비스 공지</h2>
          <div className="mt-4 space-y-3">
            <select className="w-full rounded-md border border-[#E5E7EB] px-3 py-2 text-sm" value={notice.audience} onChange={(event) => setNotice((current) => ({ ...current, audience: event.target.value }))}>
              <option value="ALL">전체 사용자</option>
              <option value="USER">특정 사용자</option>
            </select>
            {notice.audience === "USER" ? (
              <input className="w-full rounded-md border border-[#E5E7EB] px-3 py-2 text-sm" placeholder="사용자 ID" value={notice.userId} onChange={(event) => setNotice((current) => ({ ...current, userId: event.target.value }))} />
            ) : null}
            <input className="w-full rounded-md border border-[#E5E7EB] px-3 py-2 text-sm" maxLength={100} placeholder="제목" value={notice.title} onChange={(event) => setNotice((current) => ({ ...current, title: event.target.value }))} />
            <textarea className="min-h-24 w-full resize-y rounded-md border border-[#E5E7EB] px-3 py-2 text-sm" maxLength={255} placeholder="내용" value={notice.message} onChange={(event) => setNotice((current) => ({ ...current, message: event.target.value }))} />
            <button className="w-full rounded-md bg-[#1E2A38] px-4 py-2 text-sm font-semibold text-white" type="submit">
              공지 발송
            </button>
          </div>
        </form>
      </aside>

      <div className="space-y-5">
        {message ? <div className="rounded-lg border border-[#E5E7EB] bg-white p-4 text-sm text-[#6B7280]">{message}</div> : null}
        <section className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-bold text-[#1E2A38]">신고 목록</h2>
            <span className="text-sm font-semibold text-[#6B7280]">{reports.length}건</span>
          </div>
          {status === "loading" ? <p className="mt-4 text-sm text-[#6B7280]">신고 목록을 불러오는 중입니다.</p> : null}
          <div className="mt-4 divide-y divide-[#E5E7EB]">
            {reports.map((report) => (
              <button className="block w-full px-0 py-3 text-left" key={report.id} type="button" onClick={() => openReport(report)}>
                <div className="flex flex-wrap items-center gap-2">
                  <span className="font-semibold text-[#1E2A38]">#{report.id}</span>
                  <span className="rounded-full bg-[#F5F3EF] px-2 py-1 text-xs font-semibold text-[#6B7280]">{report.targetType}</span>
                  <span className="rounded-full bg-[#F1F8F2] px-2 py-1 text-xs font-semibold text-[#2F6F3E]">{report.status}</span>
                </div>
                <p className="mt-2 text-sm text-[#6B7280]">{report.reason} · 신고자 {report.reporterNickname}</p>
              </button>
            ))}
          </div>
        </section>

        {selectedReport ? (
          <section className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
            <h2 className="text-xl font-bold text-[#1E2A38]">신고 상세 #{selectedReport.id}</h2>
            <div className="mt-4 grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
              <Info label="대상" value={`${selectedReport.targetType} ${selectedReport.targetId}`} />
              <Info label="상태" value={selectedReport.status} />
              <Info label="사유" value={selectedReport.reason} />
              <Info label="신고자" value={selectedReport.reporterNickname} />
            </div>
            <p className="mt-4 rounded-md bg-[#F9FAFB] p-3 text-sm leading-6 text-[#6B7280]">{selectedReport.detail ?? "상세 내용이 없습니다."}</p>

            <form className="mt-5 space-y-3" onSubmit={updateReport}>
              <select className="w-full rounded-md border border-[#E5E7EB] px-3 py-2 text-sm" value={review.status} onChange={(event) => setReview((current) => ({ ...current, status: event.target.value }))}>
                {statusOptions.map(([value, label]) => (
                  <option key={value} value={value}>{label}</option>
                ))}
              </select>
              {selectedReport.targetType === "USER_NICKNAME" ? (
                <select className="w-full rounded-md border border-[#E5E7EB] px-3 py-2 text-sm" value={review.nicknameAction} onChange={(event) => setReview((current) => ({ ...current, nicknameAction: event.target.value }))}>
                  <option value="">닉네임 액션 없음</option>
                  <option value="REQUIRE_CHANGE">변경 요청</option>
                  <option value="DISMISS">조치 없음</option>
                </select>
              ) : null}
              <textarea className="min-h-24 w-full resize-y rounded-md border border-[#E5E7EB] px-3 py-2 text-sm" placeholder="운영자 메모" value={review.memo} onChange={(event) => setReview((current) => ({ ...current, memo: event.target.value }))} />
              <div className="flex flex-wrap gap-2">
                <button className="rounded-md bg-[#1E2A38] px-4 py-2 text-sm font-semibold text-white" type="submit">저장</button>
                {selectedReport.targetType === "POST" ? (
                  <>
                    <button className="rounded-md border border-[#E5E7EB] px-4 py-2 text-sm font-semibold text-[#991B1B]" type="button" onClick={() => hidePost(true)}>게시글 숨김</button>
                    <button className="rounded-md border border-[#E5E7EB] px-4 py-2 text-sm font-semibold text-[#1E2A38]" type="button" onClick={() => hidePost(false)}>숨김 해제</button>
                  </>
                ) : null}
              </div>
            </form>

            <div className="mt-6">
              <h3 className="text-base font-bold text-[#1E2A38]">처리 이력</h3>
              <div className="mt-3 space-y-2">
                {events.length === 0 ? <p className="text-sm text-[#6B7280]">처리 이력이 없습니다.</p> : null}
                {events.map((event) => (
                  <div className="rounded-md border border-[#E5E7EB] bg-[#F9FAFB] p-3 text-sm" key={event.id}>
                    <p className="font-semibold text-[#1E2A38]">{event.eventType}</p>
                    <p className="mt-1 text-[#6B7280]">{event.fromStatus ?? "-"} → {event.toStatus ?? "-"}</p>
                    {event.memo ? <p className="mt-1 text-[#6B7280]">{event.memo}</p> : null}
                  </div>
                ))}
              </div>
            </div>
          </section>
        ) : null}
      </div>
    </section>
  );
}

function Info({ label, value }) {
  return (
    <div className="rounded-md border border-[#E5E7EB] bg-[#F9FAFB] p-3">
      <p className="text-xs font-semibold text-[#6B7280]">{label}</p>
      <p className="mt-1 font-semibold text-[#1E2A38]">{value}</p>
    </div>
  );
}
