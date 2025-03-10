package com.bank.yankiservice.controller;

import static org.mockito.Mockito.*;

import com.bank.yankiservice.dto.BaseResponse;
import com.bank.yankiservice.dto.cardlink.CardLinkRequestedEvent;
import com.bank.yankiservice.dto.login.LoginRequest;
import com.bank.yankiservice.dto.transaction.YankiTransactionRequest;
import com.bank.yankiservice.dto.yanki.YankiWalletRequest;
import com.bank.yankiservice.model.YankiWallet;
import com.bank.yankiservice.security.JwtProvider;
import com.bank.yankiservice.service.YankiService;
import io.reactivex.Single;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

class YankiControllerTest {
    @Mock
    private YankiService yankiService;
    @Mock
    private JwtProvider jwtProvider;
    @InjectMocks
    private YankiController yankiController;
    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }
    @Test
    void testLogin_Success() {
        LoginRequest request = new LoginRequest("987654321", "12345678");
        when(yankiService.validateUser(request.getPhoneNumber(), request.getDocumentNumber()))
                .thenReturn(Single.just(true));
        when(jwtProvider.generateToken(request.getPhoneNumber()))
                .thenReturn("mocked-token");
        Single<ResponseEntity<BaseResponse<String>>> response = yankiController.login(request);
        response.test()
                .assertValue(res ->
                    res.getStatusCode() == HttpStatus.OK
                    && res.getBody().getData().equals("mocked-token"));
    }
    @Test
    void testLogin_Failure() {
        LoginRequest request = new LoginRequest("987654321",
            "12345678");
        when(yankiService.validateUser(request.getPhoneNumber(), request.getDocumentNumber()))
                .thenReturn(Single.just(false));
        Single<ResponseEntity<BaseResponse<String>>> response = yankiController.login(request);
        response.test()
                .assertValue(res -> res.getStatusCode() == HttpStatus.UNAUTHORIZED);
    }
    @Test
    void testAssociateCard_Success() {
        String token = "Bearer mocked-token";
        CardLinkRequestedEvent request = new CardLinkRequestedEvent("987654321",
            "1234567890123456",
            "12345678",
            new BigDecimal("0"));
        when(jwtProvider.getUsernameFromToken("mocked-token"))
            .thenReturn("987654321");
        when(yankiService.associateCard(request.getPhoneNumber(),
            request.getCardNumber(),
            request.getDocumentNumber()))
                .thenReturn(Completable.complete());
        Single<BaseResponse<CardLinkRequestedEvent>> response = yankiController.associateCard(token, request);
        response.test()
                .assertValue(res ->
                    res.getStatus() == HttpStatus.OK.value());
    }
    @Test
    void testAssociateCard_InvalidToken() {
        CardLinkRequestedEvent request = new CardLinkRequestedEvent("987654321",
            "1234567890123456",
            "12345678",
            new BigDecimal("0"));
        Single<BaseResponse<CardLinkRequestedEvent>> response =
            yankiController.associateCard(null, request);
        response.test()
                .assertValue(res -> res.getStatus() == HttpStatus.UNAUTHORIZED.value());
    }
    @Test
    void testProcessTransaction_Success() {
        String token = "Bearer mocked-token";
        YankiTransactionRequest request = new YankiTransactionRequest("987654321",
            "987123456",
            BigDecimal.valueOf(100));
        when(jwtProvider.getUsernameFromToken("mocked-token")).thenReturn("987654321");
        when(yankiService.processYankiTransaction(request.getSenderPhoneNumber(),
            request.getReceiverPhoneNumber(),
            request.getAmount()))
                .thenReturn(Completable.complete());
        Single<ResponseEntity<BaseResponse<Object>>> response = yankiController.processTransaction(token, request);
        response.test()
                .assertValue(res -> res.getStatusCode() == HttpStatus.OK);
    }
    @Test
    void testProcessTransaction_InvalidToken() {
        YankiTransactionRequest request = new YankiTransactionRequest("987654321",
            "987123456",
            BigDecimal.valueOf(100));
        Single<ResponseEntity<BaseResponse<Object>>> response =
            yankiController.processTransaction(null, request);
        response.test()
                .assertValue(res -> res.getStatusCode() == HttpStatus.UNAUTHORIZED);
    }
    @Test
    void testGetWalletById_Success() {
        String id = "wallet-123";
        YankiWallet wallet = new YankiWallet(id,
            "987654321",
            "12345678",
            "123456789045345",
            "eo@de.com",
            null,
            new BigDecimal("0"),
            LocalDateTime.now(),
            LocalDateTime.now());
        when(yankiService.getWalletById(id)).thenReturn(Maybe.just(wallet));
        Single<BaseResponse<YankiWallet>> response = yankiController.getWalletById(id);
        response.test()
                .assertValue(res -> res.getStatus() == HttpStatus.OK.value() && res.getData().equals(wallet));
    }
    @Test
    void testGetWalletById_NotFound() {
        String id = "wallet-123";
        when(yankiService.getWalletById(id)).thenReturn(Maybe.empty());
        Single<BaseResponse<YankiWallet>> response = yankiController.getWalletById(id);
        response.test()
                .assertValue(res -> res.getStatus() == HttpStatus.NOT_FOUND.value());
    }
    @Test
    void testDeleteWallet_Success() {
        String token = "Bearer mocked-token";
        String id = "wallet-123";
        YankiWallet wallet = new YankiWallet(id,
            "987654321",
            "12345678",
            "123456789045345",
            "eo@de.com",
            null,
            new BigDecimal("0"),
            LocalDateTime.now(),
            LocalDateTime.now());
        when(jwtProvider.getUsernameFromToken("mocked-token")).thenReturn("987654321");
        when(yankiService.getWalletById(id)).thenReturn(Maybe.just(wallet));
        when(yankiService.deleteWallet(id)).thenReturn(Completable.complete());
        Single<BaseResponse<Object>> response = yankiController.deleteWallet(token, id);
        response.test()
                .assertValue(res -> res.getStatus() == HttpStatus.OK.value());
    }
    @Test
    void testDeleteWallet_NotFound() {
        String token = "Bearer mocked-token";
        String id = "wallet-123";
        when(yankiService.getWalletById(id)).thenReturn(Maybe.empty());
        Single<BaseResponse<Object>> response = yankiController.deleteWallet(token, id);
        response.test()
                .assertValue(res -> res.getStatus() == HttpStatus.NOT_FOUND.value());
    }
    @Test
    void testGetWalletByPhoneNumber_Success() {
        String token = "Bearer mocked-token";
        String phoneNumber = "987654321";
        YankiWallet wallet = new YankiWallet("wallet-123",
            phoneNumber,
            "12345678",
            "1234567890123456",
            "user@email.com",
            null,
            BigDecimal.ZERO,
            LocalDateTime.now(),
            LocalDateTime.now());

        when(jwtProvider.getUsernameFromToken("mocked-token")).thenReturn(phoneNumber);
        when(yankiService.getWalletByPhoneNumber(phoneNumber)).thenReturn(Maybe.just(wallet));

        Single<BaseResponse<YankiWallet>> response = yankiController.getWalletByPhoneNumber(token, phoneNumber);
        response.test()
                .assertValue(res -> res.getStatus() == HttpStatus.OK.value() && res.getData().equals(wallet));
    }

    @Test
    void testGetWalletByPhoneNumber_InvalidToken() {
        String phoneNumber = "987654321";
        Single<BaseResponse<YankiWallet>> response = yankiController.getWalletByPhoneNumber(null, phoneNumber);

        response.test()
                .assertValue(res -> res.getStatus() == HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void testGetWalletByPhoneNumber_Forbidden() {
        String token = "Bearer mocked-token";
        String phoneNumber = "987654321";
        when(jwtProvider.getUsernameFromToken("mocked-token")).thenReturn("123456789"); // Diferente phoneNumber

        Single<BaseResponse<YankiWallet>> response = yankiController.getWalletByPhoneNumber(token, phoneNumber);
        response.test()
                .assertValue(res -> res.getStatus() == HttpStatus.FORBIDDEN.value());
    }

    @Test
    void testGetWalletByPhoneNumber_NotFound() {
        String token = "Bearer mocked-token";
        String phoneNumber = "987654321";

        when(jwtProvider.getUsernameFromToken("mocked-token")).thenReturn(phoneNumber);
        when(yankiService.getWalletByPhoneNumber(phoneNumber)).thenReturn(Maybe.empty());

        Single<BaseResponse<YankiWallet>> response = yankiController.getWalletByPhoneNumber(token, phoneNumber);
        response.test()
                .assertValue(res -> res.getStatus() == HttpStatus.NOT_FOUND.value());
    }
    @Test
    void testGetWalletByDocument_Success() {
        String documentNumber = "12345678";
        YankiWallet wallet = new YankiWallet("wallet-123",
            "987654321",
            documentNumber,
            "1234567890123456",
            "user@email.com",
            null,
            BigDecimal.ZERO,
            LocalDateTime.now(),
            LocalDateTime.now());

        when(yankiService.getWalletByDocument(documentNumber)).thenReturn(Maybe.just(wallet));

        Single<BaseResponse<YankiWallet>> response = yankiController.getWalletByDocument(documentNumber);
        response.test()
                .assertValue(res -> res.getStatus() == HttpStatus.OK.value() && res.getData().equals(wallet));
    }

    @Test
    void testGetWalletByDocument_NotFound() {
        String documentNumber = "12345678";

        when(yankiService.getWalletByDocument(documentNumber)).thenReturn(Maybe.empty());

        Single<BaseResponse<YankiWallet>> response = yankiController.getWalletByDocument(documentNumber);
        response.test()
                .assertValue(res -> res.getStatus() == HttpStatus.NOT_FOUND.value());
    }
    @Test
    void testUpdateWallet_Success() {
        String token = "Bearer mocked-token";
        String id = "wallet-123";
        YankiWalletRequest request = new YankiWalletRequest("987654321",
            "12345678",
            "1234567890123456",
            "new-email@email.com");
        YankiWallet existingWallet = new YankiWallet(id,
            "987654321",
            "12345678",
            "1234567890123456",
            "old@email.com",
            null,
            BigDecimal.ZERO,
            LocalDateTime.now(),
            LocalDateTime.now());
        YankiWallet updatedWallet = new YankiWallet(id,
            "987654321",
            "12345678",
            "1234567890123456",
            "new-email@email.com",
            null,
            BigDecimal.valueOf(100),
            LocalDateTime.now(),
            LocalDateTime.now());

        when(jwtProvider.getUsernameFromToken("mocked-token")).thenReturn("987654321");
        when(yankiService.getWalletById(id)).thenReturn(Maybe.just(existingWallet));
        when(yankiService.updateWallet(id, request)).thenReturn(Single.just(updatedWallet));

        Single<BaseResponse<YankiWallet>> response = yankiController.updateWallet(token, id, request);
        response.test()
                .assertValue(res -> res.getStatus() == HttpStatus.OK.value() && res.getData().equals(updatedWallet));
    }

    @Test
    void testUpdateWallet_MissingOrInvalidToken() {
        String id = "wallet-123";
        YankiWalletRequest request = new YankiWalletRequest("987654321",
            "12345678",
            "1234567890123456",
            "new-email@email.com");

        Single<BaseResponse<YankiWallet>> response = yankiController.updateWallet(null, id, request);
        response.test()
                .assertValue(res -> res.getStatus() == HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void testUpdateWallet_Forbidden() {
        String token = "Bearer mocked-token";
        String id = "wallet-123";
        YankiWalletRequest request = new YankiWalletRequest("987654321",
            "12345678",
            "1234567890123456",
            "new-email@email.com");
        YankiWallet existingWallet = new YankiWallet(id, "123456789",
            "12345678",
            "1234567890123456",
            "old@email.com",
            null,
            BigDecimal.ZERO,
            LocalDateTime.now(),
            LocalDateTime.now());

        when(jwtProvider.getUsernameFromToken("mocked-token")).thenReturn("987654321");
        when(yankiService.getWalletById(id)).thenReturn(Maybe.just(existingWallet));

        Single<BaseResponse<YankiWallet>> response = yankiController.updateWallet(token, id, request);
        response.test()
                .assertValue(res -> res.getStatus() == HttpStatus.FORBIDDEN.value());
    }

    @Test
    void testUpdateWallet_NotFound() {
        String token = "Bearer mocked-token";
        String id = "wallet-123";
        YankiWalletRequest request = new YankiWalletRequest("987654321",
            "12345678",
            "1234567890123456",
            "new-email@email.com");

        when(yankiService.getWalletById(id)).thenReturn(Maybe.empty());

        Single<BaseResponse<YankiWallet>> response = yankiController.updateWallet(token, id, request);
        response.test()
                .assertValue(res -> res.getStatus() == HttpStatus.NOT_FOUND.value());
    }
    @Test
    void testRegisterWallet_Success() {
        YankiWalletRequest request = new YankiWalletRequest("987654321",
            "12345678",
            "1234567890123456",
            "user@email.com");
        BaseResponse<YankiWallet> serviceResponse = new BaseResponse<>(
            HttpStatus.CREATED.value(),
            "Wallet created successfully",
            new YankiWallet("wallet-123",
            "987654321",
            "12345678",
            "1234567890123456",
            "user@email.com",
            null,
            BigDecimal.ZERO,
            LocalDateTime.now(),
            LocalDateTime.now()));
        when(yankiService.registerWallet(request)).thenReturn(Single.just(serviceResponse));
        Single<ResponseEntity<BaseResponse<YankiWallet>>> response = yankiController.registerWallet(request);
        response.test()
                .assertValue(res ->
                        res.getStatusCode() == HttpStatus.CREATED &&
                                res.getBody().getStatus() == HttpStatus.CREATED.value() &&
                                "Wallet created successfully".equals(res.getBody().getMessage()));
    }
    @Test
    void testRegisterWallet_Error() {
        YankiWalletRequest request = new YankiWalletRequest("987654321",
            "12345678",
            "1234567890123456",
            "user@email.com");
        RuntimeException mockException = new RuntimeException("Phone number already exists");
        when(yankiService.registerWallet(request)).thenReturn(Single.error(mockException));
        Single<ResponseEntity<BaseResponse<YankiWallet>>> response = yankiController.registerWallet(request);
        response.test()
                .assertValue(res ->
                        res.getStatusCode() == HttpStatus.BAD_REQUEST &&
                                res.getBody().getStatus() == HttpStatus.BAD_REQUEST.value() &&
                                "Phone number already exists".equals(res.getBody().getMessage()));
    }
    @Test
    void testProcessTransaction_PhoneNumberMismatch() {
        String token = "Bearer mocked-token";
        YankiTransactionRequest request = new YankiTransactionRequest("987654321",
            "987123456",
            BigDecimal.valueOf(100));
        when(jwtProvider.getUsernameFromToken("mocked-token")).thenReturn("123456789"); // Different phone number
        Single<ResponseEntity<BaseResponse<Object>>> response = yankiController.processTransaction(token, request);
        response.test()
                .assertValue(res ->
                        res.getStatusCode() == HttpStatus.FORBIDDEN &&
                                res.getBody().getStatus() == 403 &&
                                ("You can only make transactions from" +
                                    " your registered phone number").equals(res.getBody().getMessage()));
    }
    @Test
    void testProcessTransaction_ServiceError() {
        String token = "Bearer mocked-token";
        YankiTransactionRequest request = new YankiTransactionRequest("987654321",
            "987123456",
            BigDecimal.valueOf(100));
        when(jwtProvider.getUsernameFromToken("mocked-token")).thenReturn("987654321");
        when(yankiService.processYankiTransaction(request.getSenderPhoneNumber(),
            request.getReceiverPhoneNumber(),
            request.getAmount()))
                .thenReturn(Completable.error(new RuntimeException("Insufficient funds")));
        Single<ResponseEntity<BaseResponse<Object>>> response = yankiController.processTransaction(token, request);
        response.test()
                .assertValue(res ->
                        res.getStatusCode() == HttpStatus.BAD_REQUEST &&
                                "Insufficient funds".equals(res.getBody().getMessage()));
    }
    @Test
    void testAssociateCard_PhoneNumberMismatch() {
        String token = "Bearer mocked-token";
        CardLinkRequestedEvent request = new CardLinkRequestedEvent("987654321",
            "1234567890123456",
            "12345678",
            new BigDecimal("0"));
        when(jwtProvider.getUsernameFromToken("mocked-token")).thenReturn("123456789"); // Different phone number
        Single<BaseResponse<CardLinkRequestedEvent>> response = yankiController.associateCard(token, request);
        response.test()
                .assertValue(res ->
                        res.getStatus() == 403 &&
                                ("You can only make the association " +
                                    "from your registered phone number").equals(res.getMessage()));
    }
    @Test
    void testAssociateCard_ServiceError() {
        String token = "Bearer mocked-token";
        CardLinkRequestedEvent request = new CardLinkRequestedEvent("987654321",
            "1234567890123456",
            "12345678",
            new BigDecimal("0"));
        when(jwtProvider.getUsernameFromToken("mocked-token"))
            .thenReturn("987654321");
        when(yankiService.associateCard(request.getPhoneNumber(),
            request.getCardNumber(),
            request.getDocumentNumber()))
                .thenReturn(Completable.error(new RuntimeException("Invalid card number")));
        Single<BaseResponse<CardLinkRequestedEvent>> response = yankiController.associateCard(token, request);
        response.test()
                .assertValue(res ->
                        res.getStatus() == HttpStatus.BAD_REQUEST.value() &&
                                "Invalid card number".equals(res.getMessage()));
    }
    @Test
    void testDeleteWallet_MissingBearerPrefix() {
        String token = "mocked-token";
        String id = "wallet-123";
        Single<BaseResponse<Object>> response = yankiController.deleteWallet(token, id);
        response.test()
                .assertValue(res ->
                        res.getStatus() == 401 &&
                                "Missing or invalid token".equals(res.getMessage()));
    }
    @Test
    void testDeleteWallet_DifferentUser() {
        String token = "Bearer mocked-token";
        String id = "wallet-123";
        YankiWallet wallet = new YankiWallet(id,
            "123456789",
            "12345678",
            "1234567890123456",
            "user@email.com",
            null,
            BigDecimal.ZERO,
            LocalDateTime.now(),
            LocalDateTime.now());
        when(jwtProvider.getUsernameFromToken("mocked-token"))
            .thenReturn("987654321");
        when(yankiService.getWalletById(id)).thenReturn(Maybe.just(wallet));
        Single<BaseResponse<Object>> response = yankiController.deleteWallet(token, id);
        response.test()
                .assertValue(res ->
                        res.getStatus() == 403 &&
                                "You can only delete your own wallet".equals(res.getMessage()));
    }
}