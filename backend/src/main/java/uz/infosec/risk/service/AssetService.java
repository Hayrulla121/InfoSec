package uz.infosec.risk.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.infosec.risk.domain.Asset;
import uz.infosec.risk.domain.DictType;
import uz.infosec.risk.domain.InfoSystem;
import uz.infosec.risk.error.NotFoundException;
import uz.infosec.risk.repository.AssetRepository;
import uz.infosec.risk.repository.InfoSystemRepository;
import uz.infosec.risk.web.dto.RegistryDtos.AssetRequest;
import uz.infosec.risk.web.dto.RegistryDtos.AssetResponse;

/** Реестр ключевых ИА. */
@Service
public class AssetService {

    private final AssetRepository assetRepository;
    private final InfoSystemRepository infoSystemRepository;
    private final DictionaryService dictionaryService;
    private final RiskRecalculationService recalculation;
    private final CodeGenerator codeGenerator;

    public AssetService(AssetRepository assetRepository,
                        InfoSystemRepository infoSystemRepository,
                        DictionaryService dictionaryService,
                        RiskRecalculationService recalculation,
                        CodeGenerator codeGenerator) {
        this.assetRepository = assetRepository;
        this.infoSystemRepository = infoSystemRepository;
        this.dictionaryService = dictionaryService;
        this.recalculation = recalculation;
        this.codeGenerator = codeGenerator;
    }

    @Transactional(readOnly = true)
    public Page<AssetResponse> search(String query, String infoCategory, String criticality,
                                      String scope, String securityClass, Pageable pageable) {
        return assetRepository.search(
                        query,
                        Filters.orNull(infoCategory),
                        Filters.orNull(criticality),
                        Filters.orNull(scope),
                        Filters.orNull(securityClass),
                        pageable)
                .map(AssetService::toDto);
    }

    @Transactional(readOnly = true)
    public AssetResponse findById(Long id) {
        return toDto(load(id));
    }

    @Transactional
    public AssetResponse create(AssetRequest request) {
        Asset asset = new Asset();
        asset.setCode(codeGenerator.next(Asset.CODE_PREFIX, assetRepository.findAllCodes()));
        apply(asset, request);
        return toDto(assetRepository.save(asset));
    }

    @Transactional
    public AssetResponse update(Long id, AssetRequest request) {
        Asset asset = load(id);
        apply(asset, request);
        // Criticality may have changed, which changes `a` for every risk on
        // this asset.
        recalculation.recalculateForAsset(asset.getId());
        return toDto(asset);
    }

    @Transactional
    public void delete(Long id) {
        assetRepository.delete(load(id));
    }

    private void apply(Asset asset, AssetRequest request) {
        asset.setName(request.name());
        asset.setScope(request.scope());
        asset.setInfoCategory(request.infoCategory());
        asset.setCriticality(request.criticality());
        // Excel column H, VLOOKUP into the Техническая страница. Throws 404 if
        // the label is not a known dictionary value, so a typo cannot produce a
        // silently wrong rating.
        asset.setCriticalityRating(
                dictionaryService.numericValueOf(DictType.ASSET_CRITICALITY, request.criticality()));
        asset.setSecurityClass(request.securityClass());

        if (request.infoSystemId() == null) {
            asset.setInfoSystem(null);
        } else {
            InfoSystem system = infoSystemRepository.findById(request.infoSystemId())
                    .orElseThrow(() -> NotFoundException.of("entity.infoSystem", request.infoSystemId()));
            asset.setInfoSystem(system);
        }
    }

    private Asset load(Long id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("entity.asset", id));
    }

    public static AssetResponse toDto(Asset a) {
        InfoSystem system = a.getInfoSystem();
        return new AssetResponse(a.getId(), a.getCode(), a.getName(), a.getScope(),
                a.getInfoCategory(), a.getCriticality(), a.getCriticalityRating(),
                a.getSecurityClass(),
                system == null ? null : system.getId(),
                system == null ? null : system.getName(),
                a.getCreatedAt(), a.getCreatedBy(), a.getUpdatedAt(), a.getUpdatedBy());
    }
}
