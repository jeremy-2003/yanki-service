package com.bank.yankiservice.dto.bootcoin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KafkaValidationResponse {
    private String eventId;
    private boolean success;
    private String errorMessage;
}
