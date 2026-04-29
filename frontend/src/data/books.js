export const categories = ["전체", "인문", "경제", "자기계발", "소설", "에세이"];

export const books = [
  {
    id: "slow-reading",
    title: "느리게 읽는 법",
    author: "문서윤",
    category: "인문",
    tag: "교양 필터 통과",
    views: "12.4k",
    saves: 842,
    growth: "+18%",
    reason: "최근 인문 분야에서 저장 수가 빠르게 늘고 있어 추천합니다.",
    summary: "정보가 많은 시대에 문장을 깊이 읽는 감각을 되찾는 방법을 다룹니다.",
    keywords: ["독서", "사유", "집중"],
    cover: "bg-[#1E2A38]",
  },
  {
    id: "quiet-investing",
    title: "조용한 투자 습관",
    author: "서도현",
    category: "경제",
    tag: "주간 상승",
    views: "10.1k",
    saves: 711,
    growth: "+24%",
    reason: "경제 입문 독자들이 많이 저장한 책입니다.",
    summary: "과열된 시장 이슈에서 벗어나 장기적인 투자 습관을 세우는 책입니다.",
    keywords: ["투자", "경제", "습관"],
    cover: "bg-[#4CAF50]",
  },
  {
    id: "attention-design",
    title: "주의의 설계",
    author: "이세린",
    category: "자기계발",
    tag: "급상승",
    views: "9.8k",
    saves: 683,
    growth: "+31%",
    reason: "집중과 루틴에 관심 있는 독자들의 탐색이 늘고 있습니다.",
    summary: "디지털 환경과 업무 사이에서 주의력을 지키는 실용적인 전략을 제안합니다.",
    keywords: ["집중", "루틴", "자기관리"],
    cover: "bg-[#F59E0B]",
  },
  {
    id: "small-city",
    title: "작은 도시의 밤",
    author: "정하린",
    category: "소설",
    tag: "리뷰 증가",
    views: "8.7k",
    saves: 534,
    growth: "+16%",
    reason: "리뷰 이후 상세 페이지 조회가 꾸준히 늘었습니다.",
    summary: "작은 도시에서 서로의 결핍을 알아보는 사람들의 이야기입니다.",
    keywords: ["소설", "관계", "도시"],
    cover: "bg-[#6B7280]",
  },
  {
    id: "daily-sentence",
    title: "하루 한 문장",
    author: "박유진",
    category: "에세이",
    tag: "꾸준한 인기",
    views: "7.9k",
    saves: 489,
    growth: "+11%",
    reason: "짧은 독서 시간을 가진 사용자에게 꾸준히 선택됩니다.",
    summary: "매일 하나의 문장을 붙잡고 하루를 정리하는 에세이입니다.",
    keywords: ["에세이", "문장", "일상"],
    cover: "bg-[#9CA3AF]",
  },
];

export const trendingBooks = [...books]
  .filter((book) => book.growth)
  .sort((first, second) => Number(second.growth.replace(/\D/g, "")) - Number(first.growth.replace(/\D/g, "")));

export const categoryRankings = categories
  .filter((category) => category !== "전체")
  .map((category) => ({
    category,
    books: books.filter((book) => book.category === category),
  }));

export const keywordTrends = [
  {
    keyword: "투자",
    bookCount: 1,
    trendScore: "+24%",
    books: [books[1]],
  },
  {
    keyword: "집중",
    bookCount: 2,
    trendScore: "+31%",
    books: [books[2], books[0]],
  },
  {
    keyword: "문장",
    bookCount: 1,
    trendScore: "+11%",
    books: [books[4]],
  },
];
