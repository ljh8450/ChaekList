package com.example.chaeklist.domain.book.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.example.chaeklist.domain.book.entity.Book;
import org.springframework.stereotype.Repository;

@Repository
public class BookRepository {

	private final List<Book> books = List.of(
			new Book(
					"slow-reading",
					"느리게 읽는 힘",
					"문서윤",
					"인문",
					"교양 필터 통과",
					"정보가 많은 시대에 깊이 읽는 감각을 회복하는 방법을 다룹니다.",
					"최근 인문 분야에서 저장 수가 빠르게 늘고 있습니다.",
					"12.4k",
					842,
					18,
					List.of("독서", "사유", "집중")
			),
			new Book(
					"quiet-investing",
					"조용한 투자 습관",
					"한도윤",
					"경제",
					"주간 상승",
					"과열된 시장 뉴스에서 벗어나 장기적인 투자 습관을 세우는 책입니다.",
					"경제 입문 독자들이 많이 저장한 책입니다.",
					"10.1k",
					711,
					24,
					List.of("투자", "경제", "습관")
			),
			new Book(
					"attention-design",
					"주의력 설계",
					"이세린",
					"자기계발",
					"급상승",
					"스마트폰과 업무 사이에서 주의력을 지키는 실용적인 전략을 제안합니다.",
					"도파민, 집중 키워드와 함께 탐색량이 증가했습니다.",
					"9.8k",
					683,
					31,
					List.of("도파민", "집중", "루틴")
			),
			new Book(
					"small-city",
					"작은 도시의 밤",
					"정하린",
					"소설",
					"리뷰 증가",
					"작은 도시에서 서로의 결핍을 알아보는 사람들의 이야기입니다.",
					"리뷰 수와 상세 페이지 조회가 함께 늘었습니다.",
					"8.7k",
					534,
					16,
					List.of("소설", "관계", "도시")
			),
			new Book(
					"daily-sentence",
					"하루 한 문장",
					"박유진",
					"에세이",
					"꾸준한 인기",
					"매일 하나의 문장을 붙잡고 하루를 정리하는 에세이입니다.",
					"짧은 독서 시간을 가진 사용자에게 꾸준히 선택됩니다.",
					"7.9k",
					489,
					11,
					List.of("에세이", "문장", "일상")
			)
	);

	public List<Book> findAll() {
		return books;
	}

	public Optional<Book> findById(String id) {
		return books.stream()
				.filter(book -> book.id().equals(id))
				.findFirst();
	}

	public List<Book> findByCategory(String category) {
		return books.stream()
				.filter(book -> book.category().equals(category))
				.toList();
	}

	public List<Book> findRanking(String category, int limit) {
		return findAll().stream()
				.filter(book -> "전체".equals(category) || book.category().equals(category))
				.sorted(Comparator.comparingInt(Book::saves).reversed())
				.limit(limit)
				.toList();
	}

	public List<Book> findTrending(int limit) {
		return books.stream()
				.sorted(Comparator.comparingInt(Book::growthRate).reversed())
				.limit(limit)
				.toList();
	}
}
