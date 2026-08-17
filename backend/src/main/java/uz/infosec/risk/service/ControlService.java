package uz.infosec.risk.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.infosec.risk.domain.Control;
import uz.infosec.risk.error.NotFoundException;
import uz.infosec.risk.repository.ControlRepository;
import uz.infosec.risk.web.dto.RegistryDtos.ControlRequest;
import uz.infosec.risk.web.dto.RegistryDtos.ControlResponse;

import java.math.RoundingMode;

/** Риск-контроль catalog. */
@Service
public class ControlService {

    private final ControlRepository controlRepository;
    private final RiskRecalculationService recalculation;
    private final CodeGenerator codeGenerator;

    public ControlService(ControlRepository controlRepository,
                          RiskRecalculationService recalculation,
                          CodeGenerator codeGenerator) {
        this.controlRepository = controlRepository;
        this.recalculation = recalculation;
        this.codeGenerator = codeGenerator;
    }

    @Transactional(readOnly = true)
    public Page<ControlResponse> search(String query, Pageable pageable) {
        return controlRepository.search(query, pageable).map(ControlService::toDto);
    }

    @Transactional(readOnly = true)
    public ControlResponse findById(Long id) {
        return toDto(load(id));
    }

    @Transactional
    public ControlResponse create(ControlRequest request) {
        Control control = new Control();
        control.setCode(codeGenerator.next(Control.CODE_PREFIX, controlRepository.findAllCodes()));
        apply(control, request);
        return toDto(controlRepository.save(control));
    }

    @Transactional
    public ControlResponse update(Long id, ControlRequest request) {
        Control control = load(id);
        apply(control, request);
        // reduction_pct feeds every chain this control takes part in.
        recalculation.recalculateForControl(control.getId());
        return toDto(control);
    }

    @Transactional
    public void delete(Long id) {
        controlRepository.delete(load(id));
    }

    private void apply(Control control, ControlRequest request) {
        control.setName(request.name());
        control.setDescription(request.description());
        control.setTreatmentMethod(request.treatmentMethod());
        // Normalise to the column's scale so 0.2 and 0.20 are stored identically
        // and comparisons in tests and reports behave predictably.
        control.setReductionPct(request.reductionPct().setScale(2, RoundingMode.HALF_UP));
        control.setImplemented(request.implemented());
    }

    private Control load(Long id) {
        return controlRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("entity.control", id));
    }

    public static ControlResponse toDto(Control c) {
        return new ControlResponse(c.getId(), c.getCode(), c.getName(), c.getDescription(),
                c.getTreatmentMethod(), c.getReductionPct(), c.isImplemented(),
                c.getCreatedAt(), c.getCreatedBy(), c.getUpdatedAt(), c.getUpdatedBy());
    }
}
