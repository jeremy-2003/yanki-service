package com.bank.yankiservice.service;


import com.bank.yankiservice.dto.BaseResponse;
import com.bank.yankiservice.dto.yanki.YankiWalletRequest;
import com.bank.yankiservice.model.YankiWallet;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;

import java.math.BigDecimal;

public interface YankiService {
    Completable associateCard(String phoneNumber, String cardNumber, String documentNumber);
    Completable processYankiTransaction(String senderPhoneNumber, String receiverPhoneNumber, BigDecimal amount);
    Single<BaseResponse<YankiWallet>> registerWallet(YankiWalletRequest request);
    Maybe<YankiWallet> getWalletById(String id);
    Maybe<YankiWallet> getWalletByPhoneNumber(String phoneNumber);
    Maybe<YankiWallet> getWalletByDocument(String documentNumber);
    Single<YankiWallet> updateWallet(String id, YankiWalletRequest request);
    Completable deleteWallet(String id);
    Single<Boolean> validateUser(String phoneNumber, String documentNumber);
}
