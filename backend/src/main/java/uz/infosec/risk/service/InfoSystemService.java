package uz.infosec.risk.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.infosec.risk.domain.InfoSystem;
import uz.infosec.risk.error.ConflictException;
import uz.infosec.risk.error.NotFoundException;
import uz.infosec.risk.repository.AssetRepository;
import uz.infosec.risk.repository.InfoSystemRepository;
import uz.infosec.risk.web.dto.RegistryDtos.InfoSystemRequest;
import uz.infosec.risk.web.dto.RegistryDtos.InfoSystemResponse;

/** Перечень инфосистем Банка. */
@Service
public class InfoSystemService {

    private final InfoSystemRepository infoSystemRepository;
    private final AssetRepository assetRepository;
    private final CodeGenerator codeGenerator;

    public InfoSystemService(InfoSystemRepository infoSystemRepository,
                             AssetRepository assetRepository,
                             CodeGenerator codeGenerator) {
        this.infoSystemRepository = infoSystemRepository;
        this.assetRepository = assetRepository;
        this.codeGenerator = codeGenerator;
    }

    @Transactional(readOnly = true)
    public Page<InfoSystemResponse> search(String query, Pageable pageable) {
        return infoSystemRepository.search(query, pageable).map(InfoSystemService::toDto);
    }

    @Transactional(readOnly = true)
    public InfoSystemResponse findById(Long id) {
        return toDto(load(id));
    }

    @Transactional
    public InfoSystemResponse create(InfoSystemRequest request) {
        InfoSystem system = new InfoSystem();
        system.setCode(codeGenerator.next(InfoSystem.CODE_PREFIX, infoSystemRepository.findAllCodes()));
        apply(system, request);
        return toDto(infoSystemRepository.save(system));
    }

    @Transactional
    public InfoSystemResponse update(Long id, InfoSystemRequest request) {
        InfoSystem system = load(id);
        apply(system, request);
        return toDto(system);
    }

    @Transactional
    public void delete(Long id) {
        InfoSystem system = load(id);
        // Checked explicitly so the user gets a clear message instead of a raw
        // foreign-key violation from the database.
        if (assetRepository.existsByInfoSystemId(id)) {
            throw ConflictException.of("infoSystem.linkedToAssets", system.getCode());
        }
        infoSystemRepository.delete(system);
    }

    private void apply(InfoSystem s, InfoSystemRequest r) {
        s.setName(r.name());
        s.setDescription(r.description());
        s.setHosting(r.hosting());
        s.setUsagePurpose(r.usagePurpose());
        s.setDataFormat(r.dataFormat());
        s.setConfidentiality(r.confidentiality());
        s.setIntegrity(r.integrity());
        s.setAvailability(r.availability());
        s.setUpdateFrequency(r.updateFrequency());
        s.setUsersInfo(r.usersInfo());
        s.setOwner(r.owner());
    }

    private InfoSystem load(Long id) {
        return infoSystemRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("entity.infoSystem", id));
    }

    public static InfoSystemResponse toDto(InfoSystem s) {
        return new InfoSystemResponse(s.getId(), s.getCode(), s.getName(), s.getDescription(),
                s.getHosting(), s.getUsagePurpose(), s.getDataFormat(), s.getConfidentiality(),
                s.getIntegrity(), s.getAvailability(), s.getUpdateFrequency(), s.getUsersInfo(),
                s.getOwner(),
                s.getCreatedAt(), s.getCreatedBy(), s.getUpdatedAt(), s.getUpdatedBy());
    }
}
