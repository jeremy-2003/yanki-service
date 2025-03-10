package com.bank.yankiservice.event;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.bank.yankiservice.dto.bootcoin.KafkaValidationRequest;
import com.bank.yankiservice.dto.bootcoin.KafkaValidationResponse;
import com.bank.yankiservice.model.YankiWallet;
import com.bank.yankiservice.repository.YankiWalletRepository;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
@ExtendWith(MockitoExtension.class)
class BootCoinAssociationConsumerTest {
    @InjectMocks
    private BootCoinAssociationConsumer consumer;
    @Mock
    private YankiWalletRepository yankiWalletRepository;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    private KafkaValidationRequest validRequest;
    private KafkaValidationRequest invalidRequest;
    @BeforeEach
    void setUp() {
        validRequest = new KafkaValidationRequest();
        validRequest.setEventId("event123");
        validRequest.setDocumentNumber("12345678");
        validRequest.setPhoneNumber("987654321");
        invalidRequest = new KafkaValidationRequest();
        invalidRequest.setEventId("event456");
        invalidRequest.setDocumentNumber("87654321");
        invalidRequest.setPhoneNumber("912345678");
    }
    @Test
    void testValidateYankiAssociation_Success() {
        when(yankiWalletRepository.findByPhoneNumberAndDocumentNumber(anyString(), anyString()))
                .thenReturn(Maybe.just(new YankiWallet()));
        boolean result = consumer.validateYanki(validRequest.getDocumentNumber(), validRequest.getPhoneNumber());
        assertTrue(result);
    }
    @Test
    void testValidateYankiAssociation_Failure() {
        when(yankiWalletRepository.findByPhoneNumberAndDocumentNumber(anyString(), anyString()))
                .thenReturn(Maybe.empty());
        boolean result = consumer.validateYanki(invalidRequest.getDocumentNumber(), invalidRequest.getPhoneNumber());
        assertFalse(result);
    }
    @Test
    void testKafkaListener_Success() {
        when(yankiWalletRepository.findByPhoneNumberAndDocumentNumber(anyString(), anyString()))
                .thenReturn(Maybe.just(new YankiWallet()));
        KafkaValidationRequest request = new KafkaValidationRequest();
        request.setEventId("event789");
        request.setDocumentNumber("11112222");
        request.setPhoneNumber("999888777");
        consumer.validateYankiAssociation(request);
        verify(kafkaTemplate).send(eq("bootcoin.validation.response"), eq("event789"),
                argThat(response -> {
                    KafkaValidationResponse res = (KafkaValidationResponse) response;
                    return res.isSuccess() && res.getErrorMessage() == null;
                })
        );
    }
    @Test
    void testKafkaListener_Failure() {
        when(yankiWalletRepository.findByPhoneNumberAndDocumentNumber(anyString(), anyString()))
                .thenReturn(Maybe.empty());
        KafkaValidationRequest request = new KafkaValidationRequest();
        request.setEventId("event999");
        request.setDocumentNumber("33334444");
        request.setPhoneNumber("666555444");
        consumer.validateYankiAssociation(request);
        verify(kafkaTemplate).send(eq("bootcoin.validation.response"), eq("event999"),
                argThat(response -> {
                    KafkaValidationResponse res = (KafkaValidationResponse) response;
                    return !res.isSuccess() && "Yanki validation failed".equals(res.getErrorMessage());
                })
        );
    }
}