package com.bank.yankiservice.model;

import lombok.*;
import nonapi.io.github.classgraph.json.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Document(collection = "yanki_wallets")
public class YankiWallet {
    @Id
    private String id;
    private String phoneNumber;
    private String documentNumber;
    private String imei;
    private String email;
    private String linkedCard;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}