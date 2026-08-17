package uz.infosec.risk.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.infosec.risk.domain.ControlType;
import uz.infosec.risk.error.ConflictException;
import uz.infosec.risk.repository.*;
import uz.infosec.risk.web.dto.RegistryDtos.*;
import uz.infosec.risk.web.dto.RiskDtos.AttachControlRequest;
import uz.infosec.risk.web.dto.RiskDtos.RiskRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds a coherent demo dataset so the platform can be shown to someone
 * without hand-entering thirty records first.
 *
 * <p>The content is taken from the real workbook this platform replaced, so
 * the numbers on the dashboard and the matrix are the ones the risk team would
 * actually recognise.
 *
 * <p><b>It goes through the normal services, not the repositories.</b> That
 * matters: codes are generated, DREAD scores are clamped and summed, ratings
 * are derived, and every risk is classified by the same engine a real user
 * would drive. Inserting rows directly would let demo data exist in states the
 * application can never produce - and would quietly stop exercising the
 * recalculation path.
 */
@Service
public class DemoDataService {

    /** What the seed created, so the UI can report it. */
    public record DemoDataSummary(int infoSystems, int assets, int threats,
                                  int controls, int risks, int controlLinks) {
    }

    private final InfoSystemService infoSystemService;
    private final AssetService assetService;
    private final ThreatService threatService;
    private final ControlService controlService;
    private final RiskService riskService;

    private final InfoSystemRepository infoSystemRepository;
    private final AssetRepository assetRepository;
    private final ThreatRepository threatRepository;
    private final ControlRepository controlRepository;
    private final RiskRepository riskRepository;

    public DemoDataService(InfoSystemService infoSystemService,
                           AssetService assetService,
                           ThreatService threatService,
                           ControlService controlService,
                           RiskService riskService,
                           InfoSystemRepository infoSystemRepository,
                           AssetRepository assetRepository,
                           ThreatRepository threatRepository,
                           ControlRepository controlRepository,
                           RiskRepository riskRepository) {
        this.infoSystemService = infoSystemService;
        this.assetService = assetService;
        this.threatService = threatService;
        this.controlService = controlService;
        this.riskService = riskService;
        this.infoSystemRepository = infoSystemRepository;
        this.assetRepository = assetRepository;
        this.threatRepository = threatRepository;
        this.controlRepository = controlRepository;
        this.riskRepository = riskRepository;
    }

    /**
     * Refuses when a user-visible registry already holds data.
     *
     * <p>This is the whole safety model. Demo records mixed into a real risk
     * register would be very hard to unpick later - a risk officer cannot tell
     * a seeded row from one a colleague entered. Better to make the caller
     * clear the database deliberately.
     */
    @Transactional
    public DemoDataSummary seed() {
        // Counts only the registries a user can actually see and empty from the
        // interface. Info systems are deliberately NOT counted: they used to be,
        // and because they were invisible in the UI at the time, clearing every
        // visible registry still left the seed permanently blocked with no way
        // out. A guard must never depend on data the user cannot reach.
        long existing = assetRepository.count() + threatRepository.count()
                + controlRepository.count() + riskRepository.count();
        if (existing > 0) {
            throw ConflictException.of("demo.dataNotEmpty");
        }

        // ---- information systems (from the workbook's hidden inventory sheet)
        Long sysEks = infoSystem(new InfoSystemRequest(
                "ЦР ЕКС",
                "Информация о межбанковских электронных платежах между банками РУз",
                "HP ProLiant DL380p Gen9: dc1-26-34\nHP ProLiant DL380p Gen9: dc2-28-30",
                "Система проведения межбанковских платежей",
                "ФО", "дсп", "В", "В", "ежедневно",
                "Департамент платежных систем, Департамент монетарных операций",
                "Департамент платежных систем"));

        Long sysIabs = infoSystem(new InfoSystemRequest(
                "Интегрированная автоматизированная банковская система (ИАБС)",
                "Информация о деятельности подразделений ЦБ и его клиентов",
                "IBM 8284-22A: dc1-26-23\nIBM 8284-22A: dc2-28-23",
                "Автоматизация бизнес-процессов ЦБ",
                "ФО", "дсп", "В", "В", "ежедневно",
                "Департамент бухгалтерского учета и отчетности, Операционный департамент",
                "Департамент бухгалтерского учета и отчетности"));

        Long sysIhbs = infoSystem(new InfoSystemRequest(
                "Информационное хранилище банковской системы (ИХБС)",
                "Внутренняя банковская система получения информации о банковской системе",
                "IBM 8284-22A: sr1-04-33\nLenovo SR590: 1/1809",
                "Обобщенные данные в банковской системе",
                "ФО", "дсп", "Н", "В", "ежедневно",
                "Департамент платежных систем, Департамент монетарных операций",
                "Департамент бухгалтерского учета и отчетности"));

        // ---- key information assets, spread across criticality levels so the
        //      matrix and the gauges have something to show
        Long aGrki = assetService.create(new AssetRequest(
                "Государственный реестр кредитной информации",
                "В масштабе республики", "Konfidensial ma'lumot",
                "Критичная", "IS4", sysEks)).id();

        Long aIabs = assetService.create(new AssetRequest(
                "ИАБС", "В масштабе банка", "Bank siri",
                "Высокая", "IS3", sysIabs)).id();

        Long aIhbs = assetService.create(new AssetRequest(
                "ИХБС", "В масштабе банка", "Xizmat uchun",
                "Средняя", "IS2", sysIhbs)).id();

        Long aPortal = assetService.create(new AssetRequest(
                "Внутренний портал", "В масштабе банка", "Ochiq ma'lumot",
                "Низкая", "IS1", null)).id();

        // ---- threats, with the DREAD scores from the workbook
        Long tCapacity = threat("Некорректное прогнозирование потребности в мощности оборудования",
                2, 2, 4, 3, 2);                       // 13 -> rating 3
        Long tOrgChange = threat("Изменения в организационной структуре",
                3, 1, 1, 2, 2);                       // 9  -> rating 2
        Long tProcurement = threat("Несвоевременная закупка необходимого оборудования",
                4, 5, 5, 2, 2);                       // 18 -> rating 4
        Long tChannel = threat("Отказ основного канала связи",
                4, 3, 4, 3, 4);                       // 18 -> rating 4
        Long tAccess = threat("Несанкционированный доступ к персональным данным",
                5, 4, 5, 5, 5);                       // 24 -> rating 5
        Long tBackup = threat("Отсутствие проверки восстановления из резервных копий",
                2, 1, 2, 2, 3);                       // 10 -> rating 2

        // ---- control catalog (the workbook's Риск-контроль sheet)
        Long cPlanning = control("Внедрение формального процесса планирования и прогнозирования "
                + "нагрузки на ИТ-инфраструктуру", "Снижение", "0.20", false);
        Long cScaling = control("Использование масштабируемых виртуализованных ресурсов",
                "Снижение", "0.50", false);
        Long cAudit = control("Регулярный аудит текущей и прогнозной вычислительной нагрузки",
                "Снижение", "0.20", true);
        Long cChangeMgmt = control("Внедрение процедуры управления изменениями (Change Management) "
                + "с оценкой ИБ-рисков", "Снижение", "0.30", false);
        Long cOwners = control("Назначение ответственных за соблюдение ИБ-процессов при "
                + "реорганизациях", "Снижение", "0.30", true);
        Long cRedundancy = control("Резервный канал связи с автоматическим переключением",
                "Снижение", "0.40", true);
        Long cMfa = control("Многофакторная аутентификация для доступа к персональным данным",
                "Снижение", "0.60", true);
        Long cRestoreTest = control("Ежеквартальное тестирование восстановления из резервных копий",
                "Снижение", "0.40", false);

        // ---- risks, each an asset x threat pair
        int links = 0;

        Long r1 = risk(aGrki, tCapacity, "Дефицит вычислительных мощностей",
                "Рост нагрузки свыше 80% в течение месяца",
                "Департамент ИТ", "Снижение", "Укладывается в срок", LocalDate.now().plusMonths(4));
        links += attach(r1, List.of(cAudit), ControlType.IMPLEMENTED);
        links += attach(r1, List.of(cPlanning, cScaling), ControlType.PLANNED);

        Long r2 = risk(aIabs, tOrgChange, "Нарушение ИБ-процессов при реорганизации",
                "Изменения в штатном расписании без пересмотра прав доступа",
                "Служба ИБ", "Снижение", "Задержка", LocalDate.now().minusMonths(1));
        links += attach(r2, List.of(cOwners), ControlType.IMPLEMENTED);
        links += attach(r2, List.of(cChangeMgmt), ControlType.PLANNED);

        Long r3 = risk(aIhbs, tProcurement, "Простой из-за отсутствия оборудования",
                "Срыв сроков поставки более чем на 30 дней",
                "Департамент ИТ", "Принятие", "Проблема", LocalDate.now().minusMonths(2));

        Long r4 = risk(aGrki, tChannel, "Недоступность канала связи для ГРКИ",
                "Более двух обрывов связи в месяц",
                "Служба ИБ", "Снижение", "Укладывается в срок", LocalDate.now().plusMonths(2));
        links += attach(r4, List.of(cRedundancy), ControlType.IMPLEMENTED);

        Long r5 = risk(aGrki, tAccess, "Утечка персональных данных заемщиков",
                "Аномальные выгрузки из реестра",
                "Служба ИБ", "Снижение", "Укладывается в срок", LocalDate.now().plusMonths(1));
        links += attach(r5, List.of(cMfa), ControlType.IMPLEMENTED);

        Long r6 = risk(aPortal, tBackup, "Невозможность восстановления внутреннего портала",
                "Отсутствие успешных тестов восстановления за квартал",
                "Департамент ИТ", "Снижение", "Выполнено", LocalDate.now().minusMonths(3));
        links += attach(r6, List.of(cRestoreTest), ControlType.PLANNED);

        return new DemoDataSummary(
                (int) infoSystemRepository.count(), 4, 6, 8, 6, links);
    }

    // ------------------------------------------------------------ helpers

    /**
     * Reuses an information system with the same name rather than creating a
     * second copy, so re-seeding after clearing the visible registries does not
     * pile up duplicates of the inventory.
     */
    private Long infoSystem(InfoSystemRequest request) {
        return infoSystemRepository.findAll().stream()
                .filter(existing -> request.name().equals(existing.getName()))
                .findFirst()
                .map(uz.infosec.risk.domain.InfoSystem::getId)
                .orElseGet(() -> infoSystemService.create(request).id());
    }

    private Long threat(String description, int d, int r, int e, int a, int dmg) {
        return threatService.create(new ThreatRequest(description, d, r, e, a, dmg)).id();
    }

    private Long control(String name, String method, String pct, boolean implemented) {
        return controlService.create(new ControlRequest(
                name, null, method, new BigDecimal(pct), implemented)).id();
    }

    private Long risk(Long assetId, Long threatId, String name, String indicators,
                      String owner, String method, String status, LocalDate deadline) {
        return riskService.create(new RiskRequest(
                assetId, threatId, name, indicators, owner, method, status, deadline, null)).id();
    }

    private int attach(Long riskId, List<Long> controlIds, ControlType type) {
        List<Long> attached = new ArrayList<>();
        for (Long controlId : controlIds) {
            riskService.attachControl(riskId, new AttachControlRequest(controlId, type));
            attached.add(controlId);
        }
        return attached.size();
    }
}
