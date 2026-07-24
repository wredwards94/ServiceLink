package com.wesleyedwards.ServiceLink.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One entry in a ticket's audit timeline. CREATED/DELETED rows carry no field
 * detail; MODIFIED rows describe a single field's old -> new change.
 */
public record TicketHistoryEntryDto(int revision,
                                    @JsonFormat(pattern = "MM/dd/yyyy hh:mm a")
                                    LocalDateTime timestamp,
                                    String actorName,
                                    UUID actorId,
                                    String type,
                                    String field,
                                    String oldValue,
                                    String newValue) {}
