package com.example.chaeklist.domain.readingroom.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReadingRoomNotificationScheduler {

	private static final String START_NOTIFICATION = "READING_ROOM_START";
	private static final String CHECKIN_NOTIFICATION = "READING_ROOM_CHECKIN";

	private final JdbcTemplate jdbcTemplate;

	public ReadingRoomNotificationScheduler(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Scheduled(
			fixedDelayString = "${chaeklist.reading-room.notifications.fixed-delay-ms:60000}",
			initialDelayString = "${chaeklist.reading-room.notifications.initial-delay-ms:60000}"
	)
	@Transactional
	public void sendDueNotifications() {
		sendStartNotifications();
		sendCheckInNotifications();
	}

	private void sendStartNotifications() {
		LocalDateTime now = LocalDateTime.now();
		List<NotificationTarget> targets = jdbcTemplate.query("""
				SELECT session.id AS session_id, rr.id AS room_id, participant.user_id, rr.title
				FROM reading_room_sessions session
				JOIN reading_rooms rr ON rr.id = session.room_id
				JOIN reading_room_participants participant ON participant.room_id = rr.id
				JOIN users u ON u.id = participant.user_id
				LEFT JOIN user_notification_settings settings ON settings.user_id = participant.user_id
				WHERE rr.status <> 'CANCELED'
					AND session.status = 'SCHEDULED'
					AND rr.visibility = 'PUBLIC'
					AND session.scheduled_start_at >= ?
					AND session.scheduled_start_at < ?
					AND participant.status = 'JOINED'
					AND u.status = 'ACTIVE'
					AND COALESCE(settings.service_notifications_enabled, TRUE) = TRUE
					AND NOT EXISTS (
						SELECT 1
						FROM reading_room_admin_hidden hidden
						WHERE hidden.room_id = rr.id
					)
				""",
				(resultSet, rowNumber) -> new NotificationTarget(
						resultSet.getLong("session_id"),
						resultSet.getLong("room_id"),
						resultSet.getLong("user_id"),
						resultSet.getString("title")
				),
				Timestamp.valueOf(now.plusMinutes(29)),
				Timestamp.valueOf(now.plusMinutes(31))
		);
		for (NotificationTarget target : targets) {
			createNotificationIfNew(
					target,
					START_NOTIFICATION,
					"모각독 시작 알림",
					"'%s' 모각독이 곧 시작됩니다.".formatted(target.title())
			);
		}
	}

	private void sendCheckInNotifications() {
		LocalDateTime now = LocalDateTime.now();
		List<NotificationTarget> targets = jdbcTemplate.query("""
				SELECT session.id AS session_id, rr.id AS room_id, participant.user_id, rr.title
				FROM reading_room_sessions session
				JOIN reading_rooms rr ON rr.id = session.room_id
				JOIN reading_room_participants participant ON participant.room_id = rr.id
				JOIN users u ON u.id = participant.user_id
				LEFT JOIN user_notification_settings settings ON settings.user_id = participant.user_id
				WHERE rr.status <> 'CANCELED'
					AND session.status = 'ENDED'
					AND rr.visibility = 'PUBLIC'
					AND session.scheduled_end_at <= ?
					AND participant.status = 'JOINED'
					AND u.status = 'ACTIVE'
					AND COALESCE(settings.service_notifications_enabled, TRUE) = TRUE
					AND NOT EXISTS (
						SELECT 1
						FROM reading_room_admin_hidden hidden
						WHERE hidden.room_id = rr.id
					)
				""",
				(resultSet, rowNumber) -> new NotificationTarget(
						resultSet.getLong("session_id"),
						resultSet.getLong("room_id"),
						resultSet.getLong("user_id"),
						resultSet.getString("title")
				),
				Timestamp.valueOf(now)
		);
		for (NotificationTarget target : targets) {
			createNotificationIfNew(
					target,
					CHECKIN_NOTIFICATION,
					"모각독 인증 요청",
					"'%s' 모각독이 종료되었습니다. 한 줄 인증을 남겨 주세요.".formatted(target.title())
			);
		}
	}

	private void createNotificationIfNew(NotificationTarget target, String notificationType, String title, String message) {
		try {
			jdbcTemplate.update("""
					INSERT INTO reading_room_notification_events (session_id, room_id, user_id, notification_type, created_at)
					VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP(6))
					""", target.sessionId(), target.roomId(), target.userId(), notificationType);
		} catch (DuplicateKeyException exception) {
			return;
		}
		jdbcTemplate.update("""
				INSERT INTO user_notifications (
					user_id, notification_type, target_type, target_id, title, message, created_at
				)
				VALUES (?, 'SERVICE', 'SERVICE', ?, ?, ?, CURRENT_TIMESTAMP(6))
				""", target.userId(), target.roomId(), title, message);
	}

	private record NotificationTarget(long sessionId, long roomId, long userId, String title) {
	}
}
