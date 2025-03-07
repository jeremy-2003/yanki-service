package com.bank.yankiservice.dto.yanki;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class YankiWalletRequest {
    private String phoneNumber;
    private String documentNumber;
    private String imei;
    private String email;
}