package com.bank.yankiservice.event;

import com.bank.yankiservice.dto.bootcoin.KafkaValidationRequest;
import com.bank.yankiservice.dto.bootcoin.KafkaValidationResponse;
import com.bank.yankiservice.repository.YankiWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BootCoinAssociationConsumer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final YankiWalletRepository yankiWalletRepository;
    @KafkaListener(topics = "bootcoin.yanki.association", groupId = "yanki-service-group")
    public void validateYankiAssociation(KafkaValidationRequest request) {
        boolean isValid = validateYanki(request.getDocumentNumber(), request.getPhoneNumber());
        KafkaValidationResponse response = new KafkaValidationResponse(
                request.getEventId(),
                isValid,
                isValid ? null : "Yanki validation failed"
        );
        kafkaTemplate.send("bootcoin.validation.response", request.getEventId(), response);
    }
    public boolean validateYanki(String documentNumber, String phoneNumber) {
        return yankiWalletRepository.findByPhoneNumberAndDocumentNumber(phoneNumber, documentNumber)
            .map(wallet -> true)
            .defaultIfEmpty(false)
            .blockingGet();
    }
}
