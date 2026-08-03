package com.smartqueue.queuemanager.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueUpdateEvent {

    public enum EventType {
        QUEUE_ADVANCED,      // next patient called, everyone shifts up
        POSITION_UPDATED,    // urgent insert — revised positions
        YOUR_TURN_SOON,      // targeted: 2 patients ahead of this patient
        CALLED_NOW,          // targeted: this patient's turn
        NO_SHOW,             // patient skipped, queue advanced
        TOKEN_CANCELLED      // a token was cancelled, queue tightened
    }

    private EventType eventType;
    private Long tokenId;
    private Long patientId;
    private Integer newQueuePosition;
    private Integer newEstimatedWaitMins;
    private String message;
    private Long doctorId;
}