package com.bank.yankiservice.repository;

import com.bank.yankiservice.model.YankiWallet;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import org.springframework.data.repository.reactive.RxJava2CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface YankiWalletRepository extends RxJava2CrudRepository<YankiWallet, String> {
    Maybe<YankiWallet> findByPhoneNumber(String phoneNumber);
    Maybe<YankiWallet> findByPhoneNumberAndDocumentNumber(String phoneNumber, String documentNumber);
    Maybe<YankiWallet> findByDocumentNumber(String documentNumber);
    Maybe<YankiWallet> findByLinkedCard(String linkedCard);
    Maybe<YankiWallet> findByImei(String imei);
    default Completable update(YankiWallet yankiWallet) {
        return findById(yankiWallet.getId())
                .flatMapSingle(existingWallet -> save(yankiWallet))
                .ignoreElement();
    }
}