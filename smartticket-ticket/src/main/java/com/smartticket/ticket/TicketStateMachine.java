package com.smartticket.ticket;

import com.smartticket.common.TicketStatus;
import java.util.*;

public class TicketStateMachine {
    private static final Map<TicketStatus, Set<TicketStatus>> TRANSITIONS = Map.of(
        TicketStatus.CREATED, Set.of(TicketStatus.ANALYZING),
        TicketStatus.ANALYZING, Set.of(TicketStatus.RESOLVED, TicketStatus.WAITING_REVIEW, TicketStatus.FAILED),
        TicketStatus.WAITING_REVIEW, Set.of(TicketStatus.RESOLVED, TicketStatus.FAILED),
        TicketStatus.RESOLVED, Set.of(TicketStatus.CLOSED),
        TicketStatus.FAILED, Set.of(TicketStatus.ANALYZING),
        TicketStatus.CLOSED, Set.of()
    );

    public static boolean canTransit(TicketStatus from, TicketStatus to) {
        if (from == null || to == null) return false;
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}
