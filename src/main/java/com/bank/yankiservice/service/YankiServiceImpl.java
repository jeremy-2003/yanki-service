package com.bank.yankiservice.service;

import com.bank.yankiservice.dto.BaseResponse;
import com.bank.yankiservice.dto.cardlink.CardLinkRequestedEvent;
import com.bank.yankiservice.dto.transaction.YankiTransactionEvent;
import com.bank.yankiservice.dto.yanki.YankiWalletRequest;
import com.bank.yankiservice.model.YankiWallet;
import com.bank.yankiservice.repository.YankiWalletRepository;
import com.bank.yankiservice.util.ValidationUtil;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class YankiServiceImpl implements YankiService {
    private final YankiWalletRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public Completable associateCard(String phoneNumber,
                                     String cardNumber,
                                     String documentNumber) {
        return repository.findByDocumentNumber(documentNumber)
            .flatMapCompletable(yankiWallet -> {
                if (yankiWallet == null) {
                    return Completable.error(new RuntimeException("Wallet not found"));
                }
                CardLinkRequestedEvent event = new CardLinkRequestedEvent(phoneNumber,
                    cardNumber,
                    documentNumber,
                    yankiWallet.getBalance());
                kafkaTemplate.send("yanki.card.link.requested", event);
                log.info("Card association event sent: {}", event);
                return Completable.complete();
            });
    }
    public Single<Boolean> validateUser(String phoneNumber, String documentNumber) {
        return repository.findByPhoneNumberAndDocumentNumber(phoneNumber, documentNumber)
                .map(wallet -> true)
                .switchIfEmpty(Single.just(false));
    }
    @Override
    public Completable processYankiTransaction(String senderPhoneNumber,
                                               String receiverPhoneNumber,
                                               BigDecimal amount) {
        log.info("Processing Yanki transaction");
        if (senderPhoneNumber.equals(receiverPhoneNumber)) {
            log.error("Sender and receiver phone numbers cannot be the same");
            return Completable.error(new IllegalStateException("Sender and receiver phone numbers must be different"));
        }
        return getWalletByPhoneNumber(senderPhoneNumber)
                .switchIfEmpty(Single.error(new IllegalStateException("Sender wallet not found")))
                .zipWith(
                        getWalletByPhoneNumber(receiverPhoneNumber)
                                .switchIfEmpty(Single.error(new IllegalStateException("Receiver wallet not found"))),
                        Pair::of
                )
                .flatMapCompletable(pair -> {
                    YankiWallet sender = pair.getFirst();
                    YankiWallet receiver = pair.getSecond();
                    if (sender.getBalance().compareTo(amount) < 0) {
                        log.error("Insufficient " +
                            "funds in sender wallet {} for transaction of {}", sender.getId(), amount);
                        return Completable.error(new IllegalStateException("Insufficient funds for transaction"));
                    }
                    YankiTransactionEvent event = new YankiTransactionEvent(
                            null,
                            sender.getPhoneNumber(),
                            receiver.getPhoneNumber(),
                            sender.getLinkedCard(),
                            receiver.getLinkedCard(),
                            amount
                    );
                    kafkaTemplate.send("yanki.transaction.requested", event);
                    log.info("Event sent to Transaction-Service: {}", event);
                    return Completable.complete();
                });
    }
    @Override
    public Single<BaseResponse<YankiWallet>> registerWallet(YankiWalletRequest request) {
        return Single.zip(
                repository.findByPhoneNumber(request.getPhoneNumber()).count().map(count -> count == 0),
                repository.findByDocumentNumber(request.getDocumentNumber()).count().map(count -> count == 0),
                repository.findByImei(request.getImei()).count().map(count -> count == 0),
                Triple::of
        ).flatMap(result -> {
            boolean isPhoneUnique = result.getLeft();
            boolean isDocumentUnique = result.getMiddle();
            boolean isImeiUnique = result.getRight();
            ValidationUtil.validatePhoneNumber(request.getPhoneNumber());
            ValidationUtil.validateDocumentNumber(request.getDocumentNumber());
            ValidationUtil.validateImei(request.getImei());
            ValidationUtil.validateEmail(request.getEmail());
            if (!isPhoneUnique) {
                return Single.error(new IllegalArgumentException("Phone number is already registered"));
            }
            if (!isDocumentUnique) {
                return Single.error(new IllegalArgumentException("Document number is already registered"));
            }
            if (!isImeiUnique) {
                return Single.error(new IllegalArgumentException("IMEI is already registered"));
            }
            YankiWallet wallet = YankiWallet.builder()
                    .phoneNumber(request.getPhoneNumber())
                    .documentNumber(request.getDocumentNumber())
                    .imei(request.getImei())
                    .email(request.getEmail())
                    .balance(BigDecimal.ZERO)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            return repository.save(wallet)
                    .map(savedWallet -> new BaseResponse<>(
                            HttpStatus.CREATED.value(),
                            "Wallet created successfully",
                            savedWallet
                    ));
        }).onErrorResumeNext(throwable -> Single.just(new BaseResponse<>(
                HttpStatus.BAD_REQUEST.value(),
                throwable.getMessage(),
                null
        )));
    }
    @Override
    public Maybe<YankiWallet> getWalletById(String id) {
        return repository.findById(id);
    }
    @Override
    public Maybe<YankiWallet> getWalletByPhoneNumber(String phoneNumber) {
        return repository.findByPhoneNumber(phoneNumber);
    }
    @Override
    public Maybe<YankiWallet> getWalletByDocument(String documentNumber) {
        return repository.findByDocumentNumber(documentNumber);
    }
    @Override
    public Single<YankiWallet> updateWallet(String id, YankiWalletRequest request) {
        return repository.findById(id)
                .switchIfEmpty(Single.error(new RuntimeException("Wallet not found")))
                .flatMap(existingWallet -> {
                    existingWallet.setPhoneNumber(request.getPhoneNumber());
                    existingWallet.setDocumentNumber(request.getDocumentNumber());
                    existingWallet.setImei(request.getImei());
                    existingWallet.setEmail(request.getEmail());
                    existingWallet.setUpdatedAt(LocalDateTime.now());
                    return repository.save(existingWallet);
                });
    }
    @Override
    public Completable deleteWallet(String id) {
        return repository.findById(id)
                .switchIfEmpty(Maybe.error(new RuntimeException("Wallet not found")))
                .flatMapCompletable(repository::delete);
    }
}
