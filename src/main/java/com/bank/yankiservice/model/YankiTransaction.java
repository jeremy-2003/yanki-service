package com.bank.yankiservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nonapi.io.github.classgraph.json.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "yanki_transaction")
public class YankiTransaction {
    @Id
    private String id;
    private String senderPhoneNumber;
    private String receiverPhoneNumber;
    private BigDecimal amount;
    private String status;
    private String timestamp;
}
