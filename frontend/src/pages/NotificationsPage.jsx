import { useEffect, useState } from "react";
import { useAuth } from "../App";

const notificationLabels = {
  LIKE: "좋아요",
  REPORT_STATUS: "신고 처리",
  SERVICE: "서비스 공지",
};

function formatDate(value) {
  if (!value) {
    return "";
  }
  const date = Array.isArray(value)
    ? new Date(value[0], value[1] - 1, value[2], value[3] ?? 0, value[4] ?? 0)
    : new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "";
  }
  return new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium", timeStyle: "short" }).format(date);
}

export default function NotificationsPage() {
  const { accessToken, logout } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [status, setStatus] = useState("loading");
  const [message, setMessage] = useState("");

  async function loadNotifications() {
    setStatus("loading");
    setMessage("");
    const response = await fetch("/api/me/notifications?limit=50", {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    if (response.status === 401) {
      logout();
      return;
    }
    if (!response.ok) {
      setStatus("error");
      setMessage("알림을 불러오지 못했습니다.");
      return;
    }
    const data = await response.json();
    setNotifications(Array.isArray(data) ? data : []);
    setStatus("ready");
  }

  useEffect(() => {
    if (accessToken) {
      loadNotifications();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [accessToken]);

  async function markRead(notificationId) {
    const response = await fetch(`/api/me/notifications/${notificationId}/read`, {
      headers: { Authorization: `Bearer ${accessToken}` },
      method: "PATCH",
    });
    if (response.ok) {
      await loadNotifications();
    }
  }

  return (
    <section className="mx-auto w-full max-w-4xl px-5 py-8">
      <div className="rounded-lg border border-[#E5E7EB] bg-white p-6 shadow-sm">
        <p className="text-sm font-semibold text-[#4CAF50]">내 알림</p>
        <h1 className="mt-2 text-3xl font-bold text-[#1E2A38]">알림</h1>
        <p className="mt-3 text-sm leading-6 text-[#6B7280]">좋아요, 신고 처리 상태, 서비스 공지를 확인합니다.</p>
      </div>

      {message ? <div className="mt-5 rounded-lg border border-[#E5E7EB] bg-white p-4 text-sm text-[#6B7280]">{message}</div> : null}
      {status === "loading" ? <div className="mt-5 rounded-lg border border-[#E5E7EB] bg-white p-5 text-sm text-[#6B7280]">알림을 불러오는 중입니다.</div> : null}
      {status !== "loading" && notifications.length === 0 ? <div className="mt-5 rounded-lg border border-[#E5E7EB] bg-white p-5 text-sm text-[#6B7280]">알림이 없습니다.</div> : null}

      <div className="mt-5 space-y-3">
        {notifications.map((notification) => (
          <article
            className={`rounded-lg border p-4 shadow-sm ${
              notification.read ? "border-[#E5E7EB] bg-white" : "border-[#4CAF50]/40 bg-[#F1F8F2]"
            }`}
            key={notification.id}
          >
            <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <span className="rounded-full bg-white px-2 py-1 text-xs font-semibold text-[#2F6F3E]">
                    {notificationLabels[notification.notificationType] ?? notification.notificationType}
                  </span>
                  {!notification.read ? <span className="text-xs font-semibold text-[#4CAF50]">읽지 않음</span> : null}
                </div>
                <h2 className="mt-2 text-base font-bold text-[#1E2A38]">{notification.title}</h2>
                <p className="mt-2 break-keep text-sm leading-6 text-[#6B7280]">{notification.message}</p>
                <p className="mt-2 text-xs text-[#6B7280]">{formatDate(notification.createdAt)}</p>
              </div>
              {!notification.read ? (
                <button
                  className="rounded-md border border-[#E5E7EB] px-3 py-2 text-sm font-semibold text-[#1E2A38]"
                  type="button"
                  onClick={() => markRead(notification.id)}
                >
                  읽음
                </button>
              ) : null}
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
