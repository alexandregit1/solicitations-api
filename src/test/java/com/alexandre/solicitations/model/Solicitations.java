package com.alexandre.solicitations.model;

import com.alexandre.solicitations.model.enums.BrazilianState;
import com.alexandre.solicitations.model.enums.Priority;
import com.alexandre.solicitations.model.enums.ServiceType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Test data: solicitations at each stage. */
public final class Solicitations {

    private Solicitations() {}

    public static Solicitation draft(UUID clientId) {
        return new Solicitation(clientId);
    }

    public static Solicitation complete(UUID clientId, BrazilianState state) {
        Solicitation solicitation = new Solicitation(clientId);
        solicitation.updateBasicInfo(
                ServiceType.INSTALLATION, "Air conditioning", "Install two split units in the office");
        solicitation.updateAddress(
                new Address("01310100", "Avenida Paulista", "Bela Vista", "São Paulo", state, "1000", null));
        solicitation.updateConfirmation(Priority.MEDIUM, LocalDate.now().plusDays(7), BigDecimal.valueOf(500), true);
        return solicitation;
    }

    public static Solicitation submitted(UUID clientId, BrazilianState state) {
        Solicitation solicitation = complete(clientId, state);
        solicitation.submit();
        return solicitation;
    }
}
