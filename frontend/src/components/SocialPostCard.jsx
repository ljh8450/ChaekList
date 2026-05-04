import { Link } from "react-router-dom";
import { useAuth } from "../App";

const postLabels = {
  READ_BOOK: "읽은 책 공유",
  SAVED_BOOK: "저장한 책 공유",
  RECOMMENDED_BOOK: "추천 발견 공유",
  READING_GROWTH: "독서 성장 카드",
  BADGE: "배지 공유",
  READING_ROOM: "모각독 공유",
  TEXT: "자유 기록",
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
  return new Intl.DateTimeFormat("ko-KR", { month: "2-digit", day: "2-digit" }).format(date);
}

export default function SocialPostCard({ post, onChanged, compact = false }) {
  const { accessToken, currentUser } = useAuth();
  const isMine = Boolean(post.mine ?? (currentUser?.id && post.userId === currentUser.id));
  const likedByMe = Boolean(post.likedByMe);
  const isPrivate = post.visibility === "PRIVATE";
  const label = postLabels[post.postType] ?? post.postType;

  async function request(path, options = {}) {
    if (!accessToken) {
      return;
    }
    const response = await fetch(path, {
      ...options,
      headers: {
        Authorization: `Bearer ${accessToken}`,
        ...(options.body ? { "Content-Type": "application/json" } : {}),
      },
    });
    if (response.ok) {
      onChanged?.();
    }
  }

  return (
    <article className="rounded-lg border border-[#E5E7EB] bg-white p-4 shadow-sm">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            {post.userId ? (
              <span className="inline-flex min-w-0 items-center gap-2">
                <PrimaryBadge badge={post.primaryBadge} />
                <Link className="font-semibold text-[#1E2A38] hover:underline" to={`/users/${post.userId}`}>
                  {post.nickname}
                </Link>
              </span>
            ) : (
              <span className="font-semibold text-[#1E2A38]">{post.nickname ?? "탈퇴한 사용자"}</span>
            )}
            <span className="rounded-full bg-[#F5F3EF] px-2 py-1 text-xs font-semibold text-[#6B7280]">{label}</span>
            {isMine ? <span className="text-xs font-semibold text-[#4CAF50]">내 게시글</span> : null}
          </div>
          <p className="mt-1 text-xs text-[#6B7280]">{formatDate(post.createdAt)}</p>
        </div>
        <div className="flex items-center gap-2 text-xs text-[#6B7280]">
          <span>좋아요 {post.likeCount ?? 0}</span>
        </div>
      </div>

      {post.book ? (
        <Link className="mt-4 flex gap-3 rounded-md border border-[#E5E7EB] bg-[#F9FAFB] p-3 transition hover:border-[#1E2A38]" to={`/books/${post.book.id}`}>
          {post.book.coverImageUrl ? (
            <img className="h-20 w-14 shrink-0 rounded object-cover" src={post.book.coverImageUrl} alt={`${post.book.title} 표지`} />
          ) : (
            <div className="flex h-20 w-14 shrink-0 items-end rounded bg-[#1E2A38] p-2 text-[10px] font-semibold text-white">
              {post.book.category ?? "책"}
            </div>
          )}
          <div className="min-w-0">
            <p className="line-clamp-2 text-sm font-bold text-[#1E2A38]">{post.book.title}</p>
            <p className="mt-1 text-xs text-[#6B7280]">{post.book.author}</p>
            {post.book.category ? <p className="mt-2 text-xs font-semibold text-[#4CAF50]">{post.book.category}</p> : null}
          </div>
        </Link>
      ) : null}

      {post.content ? (
        <p className={`mt-4 whitespace-pre-line break-keep text-sm leading-6 text-[#1E2A38] ${compact ? "line-clamp-3" : ""}`}>
          {post.content}
        </p>
      ) : null}

      {Array.isArray(post.media) && post.media.length > 0 ? (
        <div className="mt-4 grid grid-cols-1 gap-2 sm:grid-cols-3">
          {post.media.map((media) => (
            <a
              className="block overflow-hidden rounded-md border border-[#E5E7EB] bg-[#F9FAFB]"
              href={media.url}
              key={media.id}
              target="_blank"
              rel="noreferrer"
            >
              <img
                className="aspect-[4/3] w-full object-cover"
                src={media.url}
                alt={media.fileName ?? "게시글 이미지"}
                loading="lazy"
              />
            </a>
          ))}
        </div>
      ) : null}

      <div className="mt-4 flex flex-wrap gap-2">
        {accessToken ? (
          <button
            className={`rounded-md border px-3 py-2 text-sm font-semibold transition ${
              likedByMe
                ? "border-[#1E2A38] bg-[#1E2A38] text-white hover:bg-[#27384a]"
                : "border-[#E5E7EB] text-[#1E2A38] hover:border-[#1E2A38]"
            }`}
            type="button"
            onClick={() =>
              request(`/api/social/posts/${post.id}/likes`, {
                method: likedByMe ? "DELETE" : "POST",
              })
            }
          >
            {likedByMe ? "좋아요 취소" : "좋아요"}
          </button>
        ) : null}
        {isMine ? (
          <>
            <button
              className="rounded-md border border-[#E5E7EB] px-3 py-2 text-sm font-semibold text-[#1E2A38] transition hover:border-[#1E2A38]"
              type="button"
              onClick={() =>
                request(`/api/social/posts/${post.id}`, {
                  body: JSON.stringify({ visibility: isPrivate ? "PUBLIC" : "PRIVATE" }),
                  method: "PATCH",
                })
              }
            >
              {isPrivate ? "공개 전환" : "비공개 전환"}
            </button>
            <button
              className="rounded-md border border-[#E5E7EB] px-3 py-2 text-sm font-semibold text-[#B91C1C] transition hover:border-[#B91C1C]"
              type="button"
              onClick={() => request(`/api/social/posts/${post.id}`, { method: "DELETE" })}
            >
              삭제
            </button>
          </>
        ) : accessToken && post.userId ? (
          <>
            <button
              className="rounded-md border border-[#E5E7EB] px-3 py-2 text-sm font-semibold text-[#6B7280] transition hover:border-[#1E2A38]"
              type="button"
              onClick={() =>
                request(`/api/social/posts/${post.id}/reports`, {
                  body: JSON.stringify({ reason: "INAPPROPRIATE_CONTENT" }),
                  method: "POST",
                })
              }
            >
              신고
            </button>
            <button
              className="rounded-md border border-[#E5E7EB] px-3 py-2 text-sm font-semibold text-[#6B7280] transition hover:border-[#1E2A38]"
              type="button"
              onClick={() => request(`/api/users/${post.userId}/blocks`, { method: "POST" })}
            >
              차단
            </button>
          </>
        ) : null}
      </div>
    </article>
  );
}

function PrimaryBadge({ badge }) {
  if (!badge?.label) {
    return null;
  }
  return (
    <span
      className="inline-flex max-w-[10rem] shrink-0 items-center rounded-full border border-[#D7E7D9] bg-[#F1F8F2] px-2 py-0.5 text-[11px] font-semibold text-[#2F6F3E]"
      title={badge.description ?? badge.label}
    >
      {badge.label}
    </span>
  );
}
