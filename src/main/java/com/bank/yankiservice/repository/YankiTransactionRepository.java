package com.bank.yankiservice.repository;

import com.bank.yankiservice.model.YankiTransaction;
import org.springframework.data.repository.reactive.RxJava2CrudRepository;

public interface YankiTransactionRepository extends RxJava2CrudRepository<YankiTransaction, String> {

}
