import { Link, useSearchParams } from "react-router-dom";
import { useEffect, useState } from "react";

const tabs = [
  ["all", "전체"],
  ["books", "책"],
  ["keywords", "키워드"],
  ["posts", "게시물"],
  ["users", "유저"],
];

export default function SearchResultsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const query = searchParams.get("query") ?? "";
  const type = searchParams.get("type") ?? "all";
  const [result, setResult] = useState(null);
  const [status, setStatus] = useState("idle");
  const [message, setMessage] = useState("");

  useEffect(() => {
    let ignore = false;

    async function search() {
      if (query.trim().length < 2) {
        setResult(null);
        setStatus("idle");
        setMessage("검색어는 2자 이상 입력해 주세요.");
        return;
      }
      setStatus("loading");
      setMessage("");
      try {
        const params = new URLSearchParams({ query, type, limit: "8" });
        const response = await fetch(`/api/search?${params.toString()}`);
        if (!response.ok) {
          throw new Error("검색 결과를 불러오지 못했습니다.");
        }
        const data = await response.json();
        if (!ignore) {
          setResult(data);
          setStatus("ready");
        }
      } catch (error) {
        if (!ignore) {
          setResult(null);
          setStatus("error");
          setMessage(error instanceof Error ? error.message : "검색 결과를 불러오지 못했습니다.");
        }
      }
    }

    search();
    return () => {
      ignore = true;
    };
  }, [query, type]);

  function changeType(nextType) {
    setSearchParams({ query, type: nextType });
  }

  return (
    <section className="mx-auto w-full max-w-5xl px-5 py-8">
      <div className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
        <h1 className="break-keep text-2xl font-bold text-[#1E2A38]">"{query}" 검색 결과</h1>
        <div className="mt-4 flex flex-wrap gap-2">
          {tabs.map(([value, label]) => (
            <button
              className={`rounded-md px-3 py-2 text-sm font-semibold ${
                type === value ? "bg-[#1E2A38] text-white" : "border border-[#E5E7EB] text-[#6B7280]"
              }`}
              key={value}
              type="button"
              onClick={() => changeType(value)}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      {status === "loading" ? <div className="mt-5 rounded-lg border border-[#E5E7EB] bg-white p-5 text-sm text-[#6B7280]">검색 중입니다.</div> : null}
      {message ? <div className="mt-5 rounded-lg border border-[#E5E7EB] bg-white p-5 text-sm text-[#6B7280]">{message}</div> : null}

      <div className="mt-5 space-y-5">
        {(result?.sections ?? []).map((section) => (
          <section className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm" key={section.type}>
            <h2 className="text-lg font-bold text-[#1E2A38]">{sectionTitle(section.type)}</h2>
            {section.items.length === 0 ? (
              <p className="mt-3 text-sm text-[#6B7280]">결과가 없습니다.</p>
            ) : (
              <div className="mt-4 space-y-3">
                {section.items.map((item) => (
                  <Link
                    className="block rounded-md border border-[#E5E7EB] p-4 transition hover:border-[#1E2A38]"
                    key={`${item.type}-${item.id}`}
                    to={itemPath(item)}
                  >
                    <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
                      <p className="flex min-w-0 flex-wrap items-center gap-2 font-bold text-[#1E2A38]">
                        {item.type === "user" ? <PrimaryBadge badge={item.primaryBadge} /> : null}
                        <span>{item.title}</span>
                      </p>
                      <span className="text-xs font-semibold text-[#6B7280]">{item.type}</span>
                    </div>
                    <p className="mt-2 break-keep text-sm leading-6 text-[#6B7280]">{item.summary}</p>
                  </Link>
                ))}
              </div>
            )}
          </section>
        ))}
      </div>
    </section>
  );
}

function sectionTitle(type) {
  if (type === "books") return "책";
  if (type === "keywords") return "키워드";
  if (type === "posts") return "공개 게시물";
  if (type === "users") return "공개 유저";
  return type;
}

function itemPath(item) {
  if (item.type === "post") {
    return "/social";
  }
  return item.detailPath;
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
