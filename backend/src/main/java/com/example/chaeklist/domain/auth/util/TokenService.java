package com.example.chaeklist.domain.auth.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.example.chaeklist.domain.auth.dto.TokenPair;
import com.example.chaeklist.domain.auth.entity.UserAccount;
import com.example.chaeklist.global.auth.AuthenticatedUser;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final long ACCESS_TOKEN_SECONDS = 60 * 30;
	private static final long REFRESH_TOKEN_SECONDS = 60 * 60 * 24 * 14;
	private static final String JWT_HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
	private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
	private static final Pattern JSON_STRING_FIELD = Pattern.compile("\"%s\"\\s*:\\s*\"([^\"]*)\"");
	private static final Pattern JSON_NUMBER_FIELD = Pattern.compile("\"%s\"\\s*:\\s*(\\d+)");

	private final SecureRandom secureRandom = new SecureRandom();
	private final byte[] signingKey = createSigningKey();
	private final JdbcTemplate jdbcTemplate;

	public TokenService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public TokenPair issueTokens(UserAccount user) {
		return new TokenPair(
				createToken("access", user, ACCESS_TOKEN_SECONDS),
				createToken("refresh", user, REFRESH_TOKEN_SECONDS)
		);
	}

	public AuthenticatedUser validateAccessToken(String token) {
		String[] parts = token == null ? new String[0] : token.split("\\.");
		if (parts.length != 3) {
			throw new TokenException("Invalid bearer token.");
		}

		String unsignedToken = parts[0] + "." + parts[1];
		if (!MessageDigest.isEqual(sign(unsignedToken).getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
			throw new TokenException("Invalid bearer token signature.");
		}

		String payload = decode(parts[1]);
		Map<String, String> claims = Map.of(
				"sub", readStringClaim(payload, "sub"),
				"email", readStringClaim(payload, "email"),
				"nickname", readStringClaim(payload, "nickname"),
				"status", readStringClaim(payload, "status"),
				"role", readStringClaim(payload, "role"),
				"type", readStringClaim(payload, "type"),
				"exp", readNumberClaim(payload, "exp")
		);

		if (!"access".equals(claims.get("type"))) {
			throw new TokenException("Access token is required.");
		}

		if (Long.parseLong(claims.get("exp")) < Instant.now().getEpochSecond()) {
			throw new TokenException("Access token expired.");
		}

		return findActiveUser(Long.parseLong(claims.get("sub")));
	}

	private AuthenticatedUser findActiveUser(long userId) {
		try {
			AuthenticatedUser user = jdbcTemplate.queryForObject("""
					SELECT id, email, nickname, status, role
					FROM users
					WHERE id = ?
					""",
					(resultSet, rowNumber) -> new AuthenticatedUser(
							resultSet.getLong("id"),
							resultSet.getString("email"),
							resultSet.getString("nickname"),
							resultSet.getString("status"),
							resultSet.getString("role")
					),
					userId
			);
			if (user == null || !"ACTIVE".equals(user.status())) {
				throw new TokenException("Active account is required.");
			}
			return user;
		} catch (EmptyResultDataAccessException exception) {
			throw new TokenException("Active account is required.");
		}
	}

	private String createToken(String type, UserAccount user, long expiresInSeconds) {
		Instant now = Instant.now();
		String encodedHeader = encode(JWT_HEADER);
		String encodedPayload = encode("""
				{"sub":"%d","email":"%s","nickname":"%s","status":"%s","role":"%s","type":"%s","iat":%d,"exp":%d,"jti":"%s"}"""
				.formatted(
						user.id(),
						escapeJson(user.email()),
						escapeJson(user.nickname()),
						escapeJson(user.status()),
						escapeJson(user.role()),
						type,
						now.getEpochSecond(),
						now.plusSeconds(expiresInSeconds).getEpochSecond(),
						UUID.randomUUID()
				));
		String unsignedToken = encodedHeader + "." + encodedPayload;
		return unsignedToken + "." + sign(unsignedToken);
	}

	private byte[] createSigningKey() {
		byte[] key = new byte[32];
		secureRandom.nextBytes(key);
		return key;
	}

	private String encode(String value) {
		return BASE64_URL_ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	private String decode(String value) {
		try {
			return new String(BASE64_URL_DECODER.decode(value), StandardCharsets.UTF_8);
		} catch (IllegalArgumentException exception) {
			throw new TokenException("Invalid bearer token payload.");
		}
	}

	private String sign(String value) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(new SecretKeySpec(signingKey, HMAC_ALGORITHM));
			return BASE64_URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception exception) {
			throw new IllegalStateException("Token signing is unavailable.", exception);
		}
	}

	private String readStringClaim(String payload, String name) {
		Matcher matcher = Pattern.compile(JSON_STRING_FIELD.pattern().formatted(name)).matcher(payload);
		if (!matcher.find()) {
			throw new TokenException("Missing token claim: " + name);
		}
		return matcher.group(1);
	}

	private String readNumberClaim(String payload, String name) {
		Matcher matcher = Pattern.compile(JSON_NUMBER_FIELD.pattern().formatted(name)).matcher(payload);
		if (!matcher.find()) {
			throw new TokenException("Missing token claim: " + name);
		}
		return matcher.group(1);
	}

	private String escapeJson(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	public static class TokenException extends RuntimeException {

		public TokenException(String message) {
			super(message);
		}
	}
}
