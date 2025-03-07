package com.bank.yankiservice.controller;

import com.bank.yankiservice.dto.BaseResponse;
import com.bank.yankiservice.dto.cardlink.CardLinkRequestedEvent;
import com.bank.yankiservice.dto.login.LoginRequest;
import com.bank.yankiservice.dto.transaction.YankiTransactionRequest;
import com.bank.yankiservice.dto.yanki.YankiWalletRequest;
import com.bank.yankiservice.model.YankiWallet;
import com.bank.yankiservice.security.JwtProvider;
import com.bank.yankiservice.service.YankiService;
import com.google.common.net.HttpHeaders;
import io.reactivex.Maybe;
import io.reactivex.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/yanki")
@RequiredArgsConstructor
@Slf4j
public class YankiController {
    private final YankiService yankiService;
    private final JwtProvider jwtProvider;

    @PostMapping("/login")
    public Single<ResponseEntity<BaseResponse<String>>> login(@RequestBody LoginRequest request) {
        return yankiService.validateUser(request.getPhoneNumber(), request.getDocumentNumber())
                .map(isValid -> {
                    if (isValid) {
                        String token = jwtProvider.generateToken(request.getPhoneNumber());
                        return ResponseEntity.ok(new BaseResponse<>(200, "Success login", token));
                    } else {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(new BaseResponse<>(401, "Invalid credentials", null));
                    }
                });
    }
    @PostMapping("/associate-card")
    public Single<BaseResponse<CardLinkRequestedEvent>> associateCard(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @RequestBody CardLinkRequestedEvent request) {

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Single.just(new BaseResponse<>(401, "Missing or invalid token", null));
        }
        String token = authorizationHeader.substring(7);
        String authenticatedPhoneNumber = jwtProvider.getUsernameFromToken(token);
        if (!authenticatedPhoneNumber.equals(request.getPhoneNumber())) {
            return Single.just(new BaseResponse<>(403, "You can " +
                "only make the association from your registered phone number", null));
        }
        return yankiService.associateCard(request.getPhoneNumber(),
            request.getCardNumber(),
            request.getDocumentNumber())
                .andThen(Single.just(new BaseResponse<>(HttpStatus.OK.value(),
                    "Card link request successfully sent",
                    request)))
                .onErrorReturn(e -> new BaseResponse<>(HttpStatus.BAD_REQUEST.value(),
                    e.getMessage(), null));
    }

    @PostMapping("/transaction")
    public Single<ResponseEntity<BaseResponse<Object>>> processTransaction(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @RequestBody YankiTransactionRequest request) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Single.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BaseResponse<>(401, "Missing or invalid token", null)));
        }
        String token = authorizationHeader.substring(7);
        String authenticatedPhoneNumber = jwtProvider.getUsernameFromToken(token);
        if (!authenticatedPhoneNumber.equals(request.getSenderPhoneNumber())) {
            return Single.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new BaseResponse<>(403, "You can only " +
                        "make transactions from your registered phone number", null)));
        }
        return yankiService.processYankiTransaction(
                request.getSenderPhoneNumber(),
                request.getReceiverPhoneNumber(),
                request.getAmount()
        ).toSingleDefault(
                ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), "Transaction processed successfully", null))
        ).onErrorReturn(error -> {
            log.error("Error processing transaction: {}", error.getMessage());
            BaseResponse<Object> errorResponse = new BaseResponse<>(
                HttpStatus.BAD_REQUEST.value(),
                error.getMessage(), null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        });
    }
    @PostMapping("/register")
    public Single<ResponseEntity<BaseResponse<YankiWallet>>> registerWallet(
            @Valid @RequestBody YankiWalletRequest request) {
        return yankiService.registerWallet(request)
                .map(response -> ResponseEntity.status(
                        response.getStatus()).
                        body(response))
                .onErrorReturn(error -> {
                    BaseResponse<YankiWallet> errorResponse = new BaseResponse<>(
                        HttpStatus.BAD_REQUEST.value(),
                        error.getMessage(), null);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
                });
    }
    @GetMapping("/{id}")
    public Single<BaseResponse<YankiWallet>> getWalletById(@PathVariable String id) {
        return yankiService.getWalletById(id)
                .map(wallet -> new BaseResponse<>(HttpStatus.OK.value(), "Wallet found", wallet))
                .switchIfEmpty(Single.just(new BaseResponse<>(HttpStatus.NOT_FOUND.value(), "Wallet not found", null)));
    }
    @GetMapping("/phone/{phoneNumber}")
    public Single<BaseResponse<YankiWallet>> getWalletByPhoneNumber(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @PathVariable String phoneNumber) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Single.just(new BaseResponse<>(401, "Missing or invalid token", null));
        }
        String token = authorizationHeader.substring(7);
        String authenticatedPhoneNumber = jwtProvider.getUsernameFromToken(token);
        if (!authenticatedPhoneNumber.equals(phoneNumber)) {
            return Single.just(new BaseResponse<>(403, "You can only view your own wallet", null));
        }
        return yankiService.getWalletByPhoneNumber(phoneNumber)
                .map(wallet -> new BaseResponse<>(HttpStatus.OK.value(), "Wallet found", wallet))
                .switchIfEmpty(Single.just(new BaseResponse<>(HttpStatus.NOT_FOUND.value(), "Wallet not found", null)));
    }
    @GetMapping("/document/{documentNumber}")
    public Single<BaseResponse<YankiWallet>> getWalletByDocument(@PathVariable String documentNumber) {
        return yankiService.getWalletByDocument(documentNumber)
                .map(wallet -> new BaseResponse<>(HttpStatus.OK.value(), "Wallet found", wallet))
                .switchIfEmpty(Single.just(new BaseResponse<>(HttpStatus.NOT_FOUND.value(), "Wallet not found", null)));
    }
    @PutMapping("/{id}")
    public Single<BaseResponse<YankiWallet>> updateWallet(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @PathVariable String id,
            @RequestBody YankiWalletRequest request) {

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Single.just(new BaseResponse<YankiWallet>(401, "Missing or invalid token", null));
        }

        String token = authorizationHeader.substring(7);
        String authenticatedPhoneNumber = jwtProvider.getUsernameFromToken(token);

        return yankiService.getWalletById(id)
                .flatMap(wallet -> {
                    if (!wallet.getPhoneNumber().equals(authenticatedPhoneNumber)) {
                        return Maybe.just(new BaseResponse<YankiWallet>(403,
                            "You can only update your own wallet",
                            null));
                    }
                    return yankiService.updateWallet(id, request)
                            .map(updatedWallet -> new BaseResponse<YankiWallet>(
                                    HttpStatus.OK.value(),
                                    "Wallet updated successfully",
                                    updatedWallet
                            ))
                            .toMaybe();
                })
                .switchIfEmpty(Maybe.just(new BaseResponse<YankiWallet>(
                        HttpStatus.NOT_FOUND.value(),
                        "Wallet not found",
                        null
                )))
                .toSingle();
    }
    @DeleteMapping("/{id}")
    public Single<BaseResponse<Object>> deleteWallet(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @PathVariable String id) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Single.just(new BaseResponse<>(401, "Missing or invalid token", null));
        }
        String token = authorizationHeader.substring(7);
        String authenticatedPhoneNumber = jwtProvider.getUsernameFromToken(token);

        return yankiService.getWalletById(id)
                .flatMap(wallet -> {
                    if (!wallet.getPhoneNumber().equals(authenticatedPhoneNumber)) {
                        return Maybe.just(new BaseResponse<>(403,
                            "You can only delete your own wallet",
                            null));
                    }
                    return yankiService.deleteWallet(id)
                            .toSingle(() -> new BaseResponse<>(HttpStatus.OK.value(),
                                "Wallet deleted successfully",
                                null))
                            .toMaybe();
                })
                .switchIfEmpty(Maybe.just(new BaseResponse<>(HttpStatus.NOT_FOUND.value(), "Wallet not found", null)))
                .toSingle();
    }

}
