package com.alexandre.solicitations.solicitation;

import com.alexandre.solicitations.audit.Audit;
import com.alexandre.solicitations.cep.CepAddress;
import com.alexandre.solicitations.cep.ViaCepClient;
import com.alexandre.solicitations.shared.exception.ConflictException;
import com.alexandre.solicitations.shared.exception.ForbiddenException;
import com.alexandre.solicitations.shared.exception.NotFoundException;
import com.alexandre.solicitations.solicitation.dto.AddressRequest;
import com.alexandre.solicitations.solicitation.dto.BasicInfoRequest;
import com.alexandre.solicitations.solicitation.dto.ConfirmationRequest;
import com.alexandre.solicitations.solicitation.dto.SolicitationResponse;
import com.alexandre.solicitations.solicitation.search.SolicitationSearchService;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The CLIENT side: create a draft, save each step, submit. Only the owner reaches a solicitation. */
@Service
@RequiredArgsConstructor
public class SolicitationService {

    private static final List<String> STEPS = List.of("basicInfo", "address", "confirmation");

    private final SolicitationRepository repository;
    private final ViaCepClient viaCepClient;
    private final SolicitationSearchService searchService;
    private final Validator validator;

    @Transactional
    public SolicitationResponse create(UUID clientId) {
        return save(new Solicitation(clientId));
    }

    @Transactional(readOnly = true)
    public SolicitationResponse get(UUID id, UUID clientId) {
        return SolicitationResponse.from(findOwned(id, clientId));
    }

    @Transactional
    public SolicitationResponse updateBasicInfo(UUID id, UUID clientId, BasicInfoRequest request) {
        Solicitation solicitation = findOwned(id, clientId);
        solicitation.updateBasicInfo(request.serviceType(), request.title(), request.description());
        return save(solicitation);
    }

    // Invalid CEP or ViaCEP down: the exception propagates and step 2 is not completed
    @Transactional
    public SolicitationResponse updateAddress(UUID id, UUID clientId, AddressRequest request) {
        Solicitation solicitation = findOwned(id, clientId);
        CepAddress found = viaCepClient.lookup(request.cepDigits());
        solicitation.updateAddress(new Address(
                found.cep(),
                found.street(),
                found.neighborhood(),
                found.city(),
                found.state(),
                request.number(),
                request.complement()));
        return save(solicitation);
    }

    @Transactional
    public SolicitationResponse updateConfirmation(UUID id, UUID clientId, ConfirmationRequest request) {
        Solicitation solicitation = findOwned(id, clientId);
        solicitation.updateConfirmation(
                request.priority(), request.preferredDate(), request.estimatedValue(), request.termsAccepted());
        return save(solicitation);
    }

    @Audit("SOLICITATION_SUBMITTED")
    @Transactional
    public SolicitationResponse submit(UUID id, UUID clientId) {
        Solicitation solicitation = findOwned(id, clientId);
        if (!solicitation.isDraft()) {
            throw new ConflictException("Only a DRAFT can be submitted");
        }
        validateAllSteps(solicitation);
        solicitation.submit();
        return save(solicitation);
    }

    /**
     * Runs each step's request validation again on the saved data, so every rule lives in one place (the DTO).
     * Also catches data that became invalid after being saved, like a preferredDate now in the past.
     */
    private void validateAllSteps(Solicitation solicitation) {
        List<String> missingSteps = new ArrayList<>();
        Map<String, String> errors = new TreeMap<>();

        for (int step = 1; step <= STEPS.size(); step++) {
            String name = STEPS.get(step - 1);
            if (solicitation.getCurrentStep() < step) {
                missingSteps.add(name);
                continue;
            }
            validator
                    .validate(savedRequest(solicitation, step))
                    .forEach(violation -> errors.put(name + "." + violation.getPropertyPath(), violation.getMessage()));
        }

        if (!missingSteps.isEmpty() || !errors.isEmpty()) {
            throw new IncompleteSolicitationException(missingSteps, errors);
        }
    }

    private static Object savedRequest(Solicitation s, int step) {
        return switch (step) {
            case 1 -> new BasicInfoRequest(s.getServiceType(), s.getTitle(), s.getDescription());
            case 2 ->
                new AddressRequest(
                        s.getAddress().getCep(),
                        s.getAddress().getNumber(),
                        s.getAddress().getComplement());
            default ->
                new ConfirmationRequest(
                        s.getPriority(), s.getPreferredDate(), s.getEstimatedValue(), s.getTermsAccepted());
        };
    }

    private Solicitation findOwned(UUID id, UUID clientId) {
        Solicitation solicitation =
                repository.findById(id).orElseThrow(() -> new NotFoundException("Solicitation not found"));
        if (!solicitation.belongsTo(clientId)) {
            throw new ForbiddenException("This solicitation belongs to another client");
        }
        return solicitation;
    }

    // Indexed on create, on each saved step and on submit
    private SolicitationResponse save(Solicitation solicitation) {
        Solicitation saved = repository.save(solicitation);
        searchService.index(saved);
        return SolicitationResponse.from(saved);
    }
}
