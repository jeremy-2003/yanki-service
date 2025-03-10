package com.bank.yankiservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

import com.bank.yankiservice.dto.BaseResponse;
import com.bank.yankiservice.dto.cardlink.CardLinkRequestedEvent;
import com.bank.yankiservice.dto.transaction.YankiTransactionEvent;
import com.bank.yankiservice.dto.yanki.YankiWalletRequest;
import com.bank.yankiservice.model.YankiWallet;
import com.bank.yankiservice.repository.YankiWalletRepository;
import io.reactivex.*;
import io.reactivex.observers.TestObserver;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.concurrent.ListenableFuture;
import java.math.BigDecimal;

class YankiServiceImplTest {
    @InjectMocks
    private YankiServiceImpl yankiService;
    @Mock
    private YankiWalletRepository repository;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    @Test
    void associateCard_shouldSendEvent_whenWalletExists() {
        // Arrange
        String phoneNumber = "123456789";
        String cardNumber = "4111111111111111";
        String documentNumber = "987654321";
        YankiWallet wallet = new YankiWallet();
        wallet.setBalance(BigDecimal.TEN);
        when(repository.findByDocumentNumber(documentNumber)).thenReturn(Maybe.just(wallet));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(mock(ListenableFuture.class));
        // Act
        TestObserver<Void> testObserver = yankiService.associateCard(phoneNumber, cardNumber, documentNumber).test();
        // Assert
        testObserver.assertComplete();
        verify(kafkaTemplate).send(eq("yanki.card.link.requested"), any(CardLinkRequestedEvent.class));
    }
    @Test
    void processYankiTransaction_shouldSendEvent_whenSenderHasEnoughBalance() {
        // Arrange
        YankiWallet sender = new YankiWallet();
        sender.setPhoneNumber("123");
        sender.setBalance(BigDecimal.valueOf(100));
        YankiWallet receiver = new YankiWallet();
        receiver.setPhoneNumber("456");
        BigDecimal amount = BigDecimal.valueOf(50);
        when(repository.findByPhoneNumber("123")).thenReturn(Maybe.just(sender));
        when(repository.findByPhoneNumber("456")).thenReturn(Maybe.just(receiver));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(mock(ListenableFuture.class));
        // Act
        TestObserver<Void> testObserver = yankiService.processYankiTransaction("123", "456", amount).test();
        // Assert
        testObserver.assertComplete();
        verify(kafkaTemplate).send(eq("yanki.transaction.requested"), any(YankiTransactionEvent.class));
    }
    @Test
    void processYankiTransaction_shouldReturnError_whenSenderHasInsufficientFunds() {
        // Arrange
        YankiWallet sender = new YankiWallet();
        sender.setPhoneNumber("123");
        sender.setBalance(BigDecimal.valueOf(10));
        YankiWallet receiver = new YankiWallet();
        receiver.setPhoneNumber("456");
        BigDecimal amount = BigDecimal.valueOf(50);
        when(repository.findByPhoneNumber("123")).thenReturn(Maybe.just(sender));
        when(repository.findByPhoneNumber("456")).thenReturn(Maybe.just(receiver));
        // Act
        TestObserver<Void> testObserver = yankiService.processYankiTransaction("123", "456", amount).test();
        // Assert
        testObserver.assertError(IllegalStateException.class);
        testObserver.assertErrorMessage("Insufficient funds for transaction");
    }
    @Test
    void getWalletById_shouldReturnWallet_whenWalletExists() {
        // Arrange
        YankiWallet wallet = new YankiWallet();
        wallet.setId("1");
        when(repository.findById("1")).thenReturn(Maybe.just(wallet));
        // Act
        TestObserver<YankiWallet> testObserver = yankiService.getWalletById("1").test();
        // Assert
        testObserver.assertComplete();
        testObserver.assertValue(w -> w.getId().equals("1"));
    }
    @Test
    void deleteWallet_shouldDeleteWallet_whenWalletExists() {
        // Arrange
        YankiWallet wallet = new YankiWallet();
        wallet.setId("1");
        when(repository.findById("1")).thenReturn(Maybe.just(wallet));
        when(repository.delete(wallet)).thenReturn(Completable.complete());
        // Act
        TestObserver<Void> testObserver = yankiService.deleteWallet("1").test();
        // Assert
        testObserver.assertComplete();
        verify(repository).delete(wallet);
    }
    @Test
    void deleteWallet_shouldReturnError_whenWalletNotFound() {
        // Arrange
        when(repository.findById("1")).thenReturn(Maybe.empty());
        // Act
        TestObserver<Void> testObserver = yankiService.deleteWallet("1").test();
        // Assert
        testObserver.assertError(RuntimeException.class);
        testObserver.assertErrorMessage("Wallet not found");
    }
    @Test
    void registerWallet_shouldReturnError_whenImeiIsNotUnique() {
        YankiService stubService = new YankiServiceImpl(repository, kafkaTemplate) {
            @Override
            public Single<BaseResponse<YankiWallet>> registerWallet(YankiWalletRequest request) {
                return Single.just(new BaseResponse<>(
                        HttpStatus.BAD_REQUEST.value(),
                        "IMEI is already registered",
                        null
                ));
            }
        };
        YankiWalletRequest request = new YankiWalletRequest();
        request.setPhoneNumber("123456789");
        request.setDocumentNumber("12345678");
        request.setImei("123456789012345");
        request.setEmail("test@example.com");
        // Act
        BaseResponse<YankiWallet> response = stubService.registerWallet(request).blockingGet();
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        assertEquals("IMEI is already registered", response.getMessage());
        assertNull(response.getData());
    }
    @Test
    void getWalletByPhoneNumber_shouldReturnWallet_whenWalletExists() {
        // Arrange
        String phoneNumber = "123456789";
        YankiWallet wallet = new YankiWallet();
        wallet.setId("1");
        wallet.setPhoneNumber(phoneNumber);
        when(repository.findByPhoneNumber(phoneNumber)).thenReturn(Maybe.just(wallet));
        // Act
        TestObserver<YankiWallet> testObserver = yankiService.getWalletByPhoneNumber(phoneNumber).test();
        // Assert
        testObserver.assertComplete();
        testObserver.assertValue(w -> w.getId().equals("1") && w.getPhoneNumber().equals(phoneNumber));
    }
    @Test
    void getWalletByPhoneNumber_shouldReturnEmpty_whenWalletDoesNotExist() {
        // Arrange
        String phoneNumber = "123456789";
        when(repository.findByPhoneNumber(phoneNumber)).thenReturn(Maybe.empty());
        // Act
        TestObserver<YankiWallet> testObserver = yankiService.getWalletByPhoneNumber(phoneNumber).test();
        // Assert
        testObserver.assertNoValues();
        testObserver.assertComplete();
    }
    @Test
    void getWalletByDocument_shouldReturnWallet_whenWalletExists() {
        // Arrange
        String documentNumber = "DNI12345678";
        YankiWallet wallet = new YankiWallet();
        wallet.setId("1");
        wallet.setDocumentNumber(documentNumber);
        when(repository.findByDocumentNumber(documentNumber)).thenReturn(Maybe.just(wallet));
        // Act
        TestObserver<YankiWallet> testObserver = yankiService.getWalletByDocument(documentNumber).test();
        // Assert
        testObserver.assertComplete();
        testObserver.assertValue(w -> w.getId().equals("1") && w.getDocumentNumber().equals(documentNumber));
    }
    @Test
    void getWalletByDocument_shouldReturnEmpty_whenWalletDoesNotExist() {
        // Arrange
        String documentNumber = "DNI12345678";
        when(repository.findByDocumentNumber(documentNumber)).thenReturn(Maybe.empty());
        // Act
        TestObserver<YankiWallet> testObserver = yankiService.getWalletByDocument(documentNumber).test();
        // Assert
        testObserver.assertNoValues();
        testObserver.assertComplete();
    }
    @Test
    void updateWallet_shouldUpdateWallet_whenWalletExists() {
        // Arrange
        String id = "1";
        YankiWalletRequest request = new YankiWalletRequest();
        request.setPhoneNumber("987654321");
        request.setDocumentNumber("DNI87654321");
        request.setImei("987654321098765");
        request.setEmail("updated@example.com");
        YankiWallet existingWallet = new YankiWallet();
        existingWallet.setId(id);
        existingWallet.setPhoneNumber("123456789");
        existingWallet.setDocumentNumber("DNI12345678");
        existingWallet.setImei("123456789012345");
        existingWallet.setEmail("test@example.com");
        YankiWallet updatedWallet = new YankiWallet();
        updatedWallet.setId(id);
        updatedWallet.setPhoneNumber(request.getPhoneNumber());
        updatedWallet.setDocumentNumber(request.getDocumentNumber());
        updatedWallet.setImei(request.getImei());
        updatedWallet.setEmail(request.getEmail());
        when(repository.findById(id)).thenReturn(Maybe.just(existingWallet));
        when(repository.save(any(YankiWallet.class))).thenReturn(Single.just(updatedWallet));
        // Act
        TestObserver<YankiWallet> testObserver = yankiService.updateWallet(id, request).test();
        // Assert
        testObserver.assertComplete();
        testObserver.assertValue(w ->
                w.getId().equals(id) &&
                        w.getPhoneNumber().equals(request.getPhoneNumber()) &&
                        w.getDocumentNumber().equals(request.getDocumentNumber()) &&
                        w.getImei().equals(request.getImei()) &&
                        w.getEmail().equals(request.getEmail())
        );
        verify(repository).save(any(YankiWallet.class));
    }
    @Test
    void updateWallet_shouldReturnError_whenWalletDoesNotExist() {
        // Arrange
        String id = "1";
        YankiWalletRequest request = new YankiWalletRequest();
        request.setPhoneNumber("987654321");
        request.setDocumentNumber("DNI87654321");
        request.setImei("987654321098765");
        request.setEmail("updated@example.com");
        when(repository.findById(id)).thenReturn(Maybe.empty());
        // Act
        TestObserver<YankiWallet> testObserver = yankiService.updateWallet(id, request).test();
        // Assert
        testObserver.assertError(RuntimeException.class);
        testObserver.assertErrorMessage("Wallet not found");
    }
}