package com.alexandre.solicitations.solicitation;

import static org.assertj.core.api.Assertions.assertThat;

import com.alexandre.solicitations.solicitation.dto.AddressRequest;
import com.alexandre.solicitations.solicitation.dto.BasicInfoRequest;
import com.alexandre.solicitations.solicitation.dto.ConfirmationRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class StepValidationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void textIsMeasuredAfterTrim() {
        assertThat(invalidFields(new BasicInfoRequest(ServiceType.MAINTENANCE, "  ab  ", "  too short  ")))
                .containsExactlyInAnyOrder("title", "description");
        assertThat(invalidFields(
                        new BasicInfoRequest(ServiceType.MAINTENANCE, "  Leak  ", "Water leaking under the sink")))
                .isEmpty();
    }

    @Test
    void cepAcceptsMaskAndNumberAcceptsLetters() {
        assertThat(invalidFields(new AddressRequest("01310-100", "12A", null))).isEmpty();
        assertThat(invalidFields(new AddressRequest("01310100", "12A", "  "))).isEmpty();
        assertThat(invalidFields(new AddressRequest("0131-0100", "", null))).containsExactlyInAnyOrder("cep", "number");
    }

    @Test
    void highPriorityNeedsEstimatedValueOfAtLeast100() {
        assertThat(invalidFields(confirmation(Priority.HIGH, "99.99")))
                .containsExactly("estimatedValueValidForPriority");
        assertThat(invalidFields(confirmation(Priority.HIGH, "100"))).isEmpty();
        assertThat(invalidFields(confirmation(Priority.LOW, "0"))).isEmpty();
    }

    @Test
    void confirmationRules() {
        ConfirmationRequest request =
                new ConfirmationRequest(Priority.LOW, LocalDate.now().minusDays(1), BigDecimal.valueOf(-1), false);

        assertThat(invalidFields(request))
                .containsExactlyInAnyOrder("preferredDate", "estimatedValue", "termsAccepted");
    }

    private static ConfirmationRequest confirmation(Priority priority, String value) {
        return new ConfirmationRequest(priority, LocalDate.now(), new BigDecimal(value), true);
    }

    private Set<String> invalidFields(Object request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }
}
