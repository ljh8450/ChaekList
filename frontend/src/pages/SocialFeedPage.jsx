import { useEffect, useState } from "react";
import SocialPostCard from "../components/SocialPostCard";
import { useAuth } from "../App";

const filters = [
  ["ALL", "전체"],
  ["TEXT", "자유 글"],
  ["READ_BOOK", "읽은 책"],
  ["SAVED_BOOK", "저장"],
  ["RECOMMENDED_BOOK", "추천"],
  ["READING_GROWTH", "성장"],
  ["BADGE", "배지"],
];

const sorts = [
  ["latest", "최신순"],
  ["likes", "좋아요순"],
];

export default function SocialFeedPage() {
  const { accessToken } = useAuth();
  const [posts, setPosts] = useState([]);
  const [filter, setFilter] = useState("ALL");
  const [sort, setSort] = useState("latest");
  const [content, setContent] = useState("");
  const [files, setFiles] = useState([]);
  const [status, setStatus] = useState("loading");
  const [message, setMessage] = useState("");

  async function loadFeed(nextFilter = filter, nextSort = sort) {
    setStatus("loading");
    setMessage("");
    try {
      const params = new URLSearchParams({ limit: "30", sort: nextSort });
      if (nextFilter !== "ALL") {
        params.set("type", nextFilter);
      }
      const response = await fetch(`/api/social/feed?${params.toString()}`, {
        headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : {},
      });
      if (!response.ok) {
        throw new Error("공개 피드를 불러오지 못했습니다.");
      }
      const data = await response.json();
      setPosts(Array.isArray(data) ? data : []);
      setStatus("ready");
    } catch (error) {
      setPosts([]);
      setStatus("error");
      setMessage(error instanceof Error ? error.message : "공개 피드를 불러오지 못했습니다.");
    }
  }

  useEffect(() => {
    loadFeed(filter, sort);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter, sort, accessToken]);

  async function createTextPost(event) {
    event.preventDefault();
    const form = event.currentTarget;
    if (!content.trim() || !accessToken) {
      return;
    }
    setMessage("");
    const response = await fetch("/api/social/posts", {
      body: JSON.stringify({
        postType: "TEXT",
        content: content.trim(),
        visibility: "PUBLIC",
        idempotencyKey: `text-${Date.now()}`,
      }),
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      method: "POST",
    });
    if (!response.ok) {
      setMessage("게시글을 공유하지 못했습니다.");
      return;
    }
    const post = await response.json();
    for (const file of files) {
      const formData = new FormData();
      formData.append("file", file);
      const uploadResponse = await fetch(`/api/social/posts/${post.id}/media`, {
        body: formData,
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        method: "POST",
      });
      if (!uploadResponse.ok) {
        setMessage("게시글은 공유했지만 일부 이미지를 첨부하지 못했습니다.");
        break;
      }
    }
    setContent("");
    setFiles([]);
    form.reset();
    await loadFeed(filter, sort);
  }

  function selectFiles(event) {
    const selectedFiles = Array.from(event.target.files ?? []);
    const acceptedFiles = selectedFiles
      .filter((file) => ["image/jpeg", "image/png", "image/webp"].includes(file.type) && file.size <= 2 * 1024 * 1024)
      .slice(0, 3);
    setFiles(acceptedFiles);
    if (acceptedFiles.length !== selectedFiles.length) {
      setMessage("이미지는 최대 3개, 파일당 2MB 이하의 jpg/png/webp만 첨부할 수 있습니다.");
    } else {
      setMessage("");
    }
  }

  return (
    <section className="mx-auto grid w-full max-w-7xl grid-cols-1 gap-6 px-5 py-8 lg:grid-cols-[280px_1fr]">
      <aside className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
        <p className="text-sm font-semibold text-[#4CAF50]">공개 독서 활동</p>
        <h1 className="mt-2 text-2xl font-bold text-[#1E2A38]">공개 피드</h1>
        <p className="mt-3 break-keep text-sm leading-6 text-[#6B7280]">
          직접 공유한 기록만 공개됩니다. 읽은 책 목록 전체는 공개되지 않습니다.
        </p>
        <div className="mt-5 space-y-2 text-sm text-[#6B7280]">
          <p>공개 데이터는 선택 공유 기준입니다.</p>
          <p>비활성화 계정과 숨김 게시글은 표시하지 않습니다.</p>
        </div>
      </aside>

      <div className="space-y-5">
        <div className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-sm font-semibold text-[#1E2A38]">조용히 공유된 독서 활동</p>
              <h2 className="mt-2 text-2xl font-bold text-[#1E2A38]">책 발견을 돕는 공개 흐름</h2>
            </div>
            <div className="flex flex-wrap gap-2">
              {sorts.map(([value, label]) => (
                <button
                  className={`rounded-md px-3 py-2 text-sm font-semibold ${
                    sort === value ? "bg-[#4CAF50] text-white" : "border border-[#E5E7EB] text-[#6B7280]"
                  }`}
                  key={value}
                  type="button"
                  onClick={() => setSort(value)}
                >
                  {label}
                </button>
              ))}
              {filters.map(([value, label]) => (
                <button
                  className={`rounded-md px-3 py-2 text-sm font-semibold ${
                    filter === value ? "bg-[#1E2A38] text-white" : "border border-[#E5E7EB] text-[#6B7280]"
                  }`}
                  key={value}
                  type="button"
                  onClick={() => setFilter(value)}
                >
                  {label}
                </button>
              ))}
            </div>
          </div>
        </div>

        {accessToken ? (
          <form className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm" onSubmit={createTextPost}>
            <label className="block text-sm font-semibold text-[#1E2A38]" htmlFor="social-text">
              자유 기록 공유
            </label>
            <textarea
              className="mt-3 min-h-28 w-full resize-y rounded-md border border-[#E5E7EB] bg-[#F9FAFB] p-3 text-sm outline-none focus:border-[#1E2A38] focus:bg-white"
              id="social-text"
              maxLength={1000}
              value={content}
              onChange={(event) => setContent(event.target.value)}
              placeholder="책을 읽으며 남기고 싶은 짧은 기록"
            />
            <div className="mt-3 rounded-md border border-dashed border-[#D1D5DB] bg-[#F9FAFB] p-3">
              <label className="block text-sm font-semibold text-[#1E2A38]" htmlFor="social-media">
                이미지 첨부
              </label>
              <input
                className="mt-2 block w-full text-sm text-[#6B7280] file:mr-3 file:rounded-md file:border-0 file:bg-[#1E2A38] file:px-3 file:py-2 file:text-sm file:font-semibold file:text-white"
                id="social-media"
                type="file"
                accept="image/jpeg,image/png,image/webp"
                multiple
                onChange={selectFiles}
              />
              {files.length > 0 ? (
                <div className="mt-3 flex flex-wrap gap-2 text-xs text-[#6B7280]">
                  {files.map((file) => (
                    <span className="rounded-full bg-white px-2 py-1" key={`${file.name}-${file.size}`}>
                      {file.name}
                    </span>
                  ))}
                </div>
              ) : null}
            </div>
            <div className="mt-3 flex items-center justify-between gap-3">
              <p className="text-xs text-[#6B7280]">공개 피드에 표시됩니다. 이미지는 최대 3개까지 첨부됩니다.</p>
              <button className="rounded-md bg-[#1E2A38] px-4 py-2 text-sm font-semibold text-white" type="submit">
                공유
              </button>
            </div>
          </form>
        ) : null}

        {message ? <div className="rounded-lg border border-[#E5E7EB] bg-white p-4 text-sm text-[#6B7280]">{message}</div> : null}
        {status === "loading" ? <div className="rounded-lg border border-[#E5E7EB] bg-white p-5 text-sm text-[#6B7280]">공개 피드를 불러오는 중입니다.</div> : null}
        {status !== "loading" && posts.length === 0 ? <div className="rounded-lg border border-[#E5E7EB] bg-white p-5 text-sm text-[#6B7280]">아직 공개 게시글이 없습니다.</div> : null}
        <div className="space-y-4">
          {posts.map((post) => (
            <SocialPostCard key={post.id} post={post} onChanged={() => loadFeed(filter, sort)} />
          ))}
        </div>
      </div>
    </section>
  );
}
