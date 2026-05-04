import { Link, useParams } from "react-router-dom";
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

export default function PublicProfilePage() {
  const { userId } = useParams();
  const { accessToken } = useAuth();
  const [profile, setProfile] = useState(null);
  const [posts, setPosts] = useState([]);
  const [filter, setFilter] = useState("ALL");
  const [status, setStatus] = useState("loading");
  const [postsStatus, setPostsStatus] = useState("idle");

  useEffect(() => {
    let ignore = false;
    async function loadProfile() {
      setStatus("loading");
      const response = await fetch(`/api/users/${userId}/public-profile`);
      if (!response.ok) {
        if (!ignore) setStatus("error");
        return;
      }
      const data = await response.json();
      if (!ignore) {
        setProfile(data);
        setStatus("ready");
      }
    }
    loadProfile();
    return () => {
      ignore = true;
    };
  }, [userId]);

  useEffect(() => {
    let ignore = false;
    async function loadPosts() {
      setPostsStatus("loading");
      const params = new URLSearchParams({ limit: "30" });
      if (filter !== "ALL") {
        params.set("type", filter);
      }
      const response = await fetch(`/api/users/${userId}/social/posts?${params.toString()}`, {
        headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : {},
      });
      if (!response.ok) {
        if (!ignore) {
          setPosts([]);
          setPostsStatus("error");
        }
        return;
      }
      const data = await response.json();
      if (!ignore) {
        setPosts(Array.isArray(data) ? data : []);
        setPostsStatus("ready");
      }
    }
    if (profile) {
      loadPosts();
    }
    return () => {
      ignore = true;
    };
  }, [accessToken, filter, profile, userId]);

  return (
    <section className="mx-auto w-full max-w-5xl px-5 py-8">
      {status === "loading" ? (
        <div className="rounded-lg border border-[#E5E7EB] bg-white p-5 text-sm text-[#6B7280] shadow-sm">공개 프로필을 불러오는 중입니다.</div>
      ) : null}
      {status === "error" ? (
        <div className="rounded-lg border border-[#E5E7EB] bg-white p-5 text-sm text-[#6B7280] shadow-sm">공개 프로필을 찾을 수 없습니다.</div>
      ) : null}
      {profile ? (
        <div className="rounded-lg border border-[#E5E7EB] bg-white p-6 shadow-sm">
          <p className="text-sm font-semibold text-[#4CAF50]">공개 프로필</p>
          <h1 className="mt-2 flex flex-wrap items-center gap-2 text-3xl font-bold text-[#1E2A38]">
            <PrimaryBadge badge={profile.primaryBadge} />
            <span>{profile.nickname}</span>
          </h1>
          <div className="mt-5 grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div className="rounded-lg border border-[#E5E7EB] bg-[#F9FAFB] p-4">
              <p className="text-xs font-semibold text-[#6B7280]">공개 게시글</p>
              <p className="mt-2 text-2xl font-bold text-[#1E2A38]">{profile.publicPostCount ?? 0}개</p>
            </div>
            <div className="rounded-lg border border-[#E5E7EB] bg-[#F9FAFB] p-4">
              <p className="text-xs font-semibold text-[#6B7280]">성장 요약</p>
              <p className="mt-2 text-sm leading-6 text-[#1E2A38]">{profile.growthSummary ?? "공개하지 않았습니다."}</p>
            </div>
          </div>
          <Link className="mt-5 inline-flex rounded-md bg-[#1E2A38] px-4 py-2 text-sm font-semibold text-white" to="/social">
            공개 피드로 이동
          </Link>
        </div>
      ) : null}

      {profile ? (
        <section className="mt-5 rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-sm font-semibold text-[#4CAF50]">공개 활동</p>
              <h2 className="mt-2 text-xl font-bold text-[#1E2A38]">공개 게시글</h2>
            </div>
            <div className="flex flex-wrap gap-2">
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
          {postsStatus === "loading" ? <p className="mt-4 text-sm text-[#6B7280]">공개 게시글을 불러오는 중입니다.</p> : null}
          {postsStatus === "error" ? <p className="mt-4 text-sm text-[#6B7280]">공개 게시글을 불러오지 못했습니다.</p> : null}
          {postsStatus !== "loading" && posts.length === 0 ? <p className="mt-4 rounded-md border border-[#E5E7EB] bg-[#F9FAFB] p-4 text-sm text-[#6B7280]">공개 게시글이 없습니다.</p> : null}
          <div className="mt-4 space-y-3">
            {posts.map((post) => (
              <SocialPostCard key={post.id} post={post} compact />
            ))}
          </div>
        </section>
      ) : null}
    </section>
  );
}

function PrimaryBadge({ badge }) {
  if (!badge?.label) {
    return null;
  }
  return (
    <span
      className="inline-flex max-w-full items-center rounded-full border border-[#D7E7D9] bg-[#F1F8F2] px-2.5 py-1 text-xs font-semibold text-[#2F6F3E]"
      title={badge.description ?? badge.label}
    >
      {badge.label}
    </span>
  );
}
