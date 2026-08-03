package com.smartqueue.queuemanager.websocket;

import com.smartqueue.queuemanager.entity.Token;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueWebSocketBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    // ── Broadcast updated position to ALL waiting patients in a queue ──────
    public void broadcastQueueUpdate(List<Token> waitingTokens,
                                     Long doctorId,
                                     QueueUpdateEvent.EventType eventType) {
        for (Token token : waitingTokens) {
            QueueUpdateEvent event = QueueUpdateEvent.builder()
                    .eventType(eventType)
                    .tokenId(token.getId())
                    .patientId(token.getPatient().getId())
                    .newQueuePosition(token.getQueuePosition())
                    .newEstimatedWaitMins(token.getEstimatedWaitMins())
                    .doctorId(doctorId)
                    .message(buildMessage(eventType, token.getQueuePosition()))
                    .build();

            // Broadcast to the doctor's queue topic
            messagingTemplate.convertAndSend(
                    "/topic/queue/" + doctorId, event);

            log.debug("Broadcast {} to patient {} — position {} EWT {} mins",
                    eventType, token.getPatient().getId(),
                    token.getQueuePosition(), token.getEstimatedWaitMins());
        }
    }

    // ── Targeted event to a specific patient ─────────────────────────────
    public void sendTargetedEvent(Token token,
                                  Long doctorId,
                                  QueueUpdateEvent.EventType eventType) {
        QueueUpdateEvent event = QueueUpdateEvent.builder()
                .eventType(eventType)
                .tokenId(token.getId())
                .patientId(token.getPatient().getId())
                .newQueuePosition(token.getQueuePosition())
                .newEstimatedWaitMins(token.getEstimatedWaitMins())
                .doctorId(doctorId)
                .message(buildMessage(eventType, token.getQueuePosition()))
                .build();

        messagingTemplate.convertAndSend("/topic/queue/" + doctorId, event);

        log.info("Targeted {} sent to patient {}", eventType, token.getPatient().getId());
    }

    private String buildMessage(QueueUpdateEvent.EventType type, int position) {
        return switch (type) {
            case QUEUE_ADVANCED  -> "Queue advanced. You are now at position " + position;
            case POSITION_UPDATED -> "Queue updated. Your new position is " + position;
            case YOUR_TURN_SOON  -> "Your turn is coming soon! Only 2 patients ahead.";
            case CALLED_NOW      -> "It's your turn! Please proceed to the doctor.";
            case NO_SHOW         -> "A patient was skipped. You moved up to position " + position;
            case TOKEN_CANCELLED -> "A patient cancelled. You moved up to position " + position;
        };
    }
}