package com.example.chaeklist;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
	private static final int MIN_PASSWORD_LENGTH = 8;
	private static final int MAX_NICKNAME_LENGTH = 50;

	private final UserAccountRepository userAccountRepository;

	public AuthService(UserAccountRepository userAccountRepository) {
		this.userAccountRepository = userAccountRepository;
	}

	@Transactional
	public AuthResponse signup(SignupRequest request) {
		String email = normalizeEmail(request.email());
		String nickname = normalizeNickname(request.nickname());
		String password = normalizePassword(request.password());

		validateEmail(email);
		validateNickname(nickname);
		validatePassword(password);

		if (userAccountRepository.existsByEmail(email)) {
			throw new AuthException("이미 가입된 이메일입니다.");
		}

		if (userAccountRepository.existsByNickname(nickname)) {
			throw new AuthException("이미 사용 중인 닉네임입니다.");
		}

		UserAccountEntity user = createUser(email, nickname, password);
		return AuthResponse.from(user);
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		String email = normalizeEmail(request.email());
		String password = normalizePassword(request.password());

		validateEmail(email);
		validatePassword(password);

		UserAccountEntity user = userAccountRepository.findByEmail(email).orElse(null);
		if (user == null || !user.getPasswordHash().equals(hashPassword(password))) {
			throw new AuthException("이메일 또는 비밀번호가 올바르지 않습니다.");
		}

		if (!"ACTIVE".equals(user.getStatus())) {
			throw new AuthException("활성 상태의 계정만 로그인할 수 있습니다.");
		}

		return AuthResponse.from(user);
	}

	private UserAccountEntity createUser(String email, String nickname, String password) {
		UserAccountEntity user = new UserAccountEntity(email, nickname, hashPassword(password), "ACTIVE");
		return userAccountRepository.save(user);
	}

	private void validateEmail(String email) {
		if (email.isBlank()) {
			throw new AuthException("이메일을 입력해 주세요.");
		}

		if (!EMAIL_PATTERN.matcher(email).matches()) {
			throw new AuthException("올바른 이메일 형식이 아닙니다.");
		}
	}

	private void validateNickname(String nickname) {
		if (nickname.isBlank()) {
			throw new AuthException("닉네임을 입력해 주세요.");
		}

		if (nickname.length() > MAX_NICKNAME_LENGTH) {
			throw new AuthException("닉네임은 50자 이하로 입력해 주세요.");
		}
	}

	private void validatePassword(String password) {
		if (password.length() < MIN_PASSWORD_LENGTH) {
			throw new AuthException("비밀번호는 8자 이상이어야 합니다.");
		}
	}

	private String normalizeEmail(String email) {
		return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
	}

	private String normalizeNickname(String nickname) {
		return nickname == null ? "" : nickname.trim();
	}

	private String normalizePassword(String password) {
		return password == null ? "" : password;
	}

	private String hashPassword(String password) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] bytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder(bytes.length * 2);
			for (byte value : bytes) {
				builder.append(String.format("%02x", value));
			}
			return builder.toString();
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("Password hashing is unavailable.", exception);
		}
	}

	static class AuthException extends RuntimeException {

		AuthException(String message) {
			super(message);
		}
	}
}
