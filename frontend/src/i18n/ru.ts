/**
 * Russian UI strings. This file is the SOURCE OF TRUTH for the shape of the
 * dictionary: `uz.ts` is typed as `Dictionary`, so TypeScript refuses to
 * compile if a key here has no Uzbek counterpart.
 *
 * Values are either plain strings or functions when they interpolate. Nothing
 * is looked up by a string key like t('nav.home') — the dictionary is accessed
 * as a normal object, so a typo is a compile error rather than a blank label
 * discovered in production.
 *
 * WHAT IS NOT HERE: anything a user typed (asset names, threat descriptions,
 * control names, comments). Those are data, not interface, and are shown
 * exactly as they were entered whichever language is selected.
 */
export const ru = {
  lang: {
    name: 'Русский',
    switchTo: 'Til / Язык',
  },

  brand: {
    title: 'Оценка рисков',
    subtitle: 'ИТ и ИБ',
    // The system's formal name, shown on the login screen.
    fullTitle: 'Автоматизированная система управления рисками ИТ и ИБ',
  },

  login: {
    prompt: 'Войдите, чтобы продолжить',
    username: 'Логин',
    password: 'Пароль',
    submit: 'Войти',
    submitting: 'Вход…',
    failed: 'Не удалось войти',
    footer: 'Информационная безопасность · внутренняя система',
    // HUD text on the radar backdrop. Localised rather than left as
    // English chrome: a Russian screen with an English word on it is
    // exactly the mixed-language problem the switch exists to remove.
    radarStatus: 'СКАНИРОВАНИЕ',
  },

  nav: {
    modules: 'Модули',
    administration: 'Администрирование',
    home: 'Главная',
    // The Excel sheet is titled "Ma'lumot - Tahdidlar modeli". The original
    // spec asked for sheet names verbatim, but leaving an Uzbek label in the
    // Russian menu is exactly the mixed-language problem this switch exists to
    // remove, so the Russian side gets a Russian name.
    threatModel: 'Модель угроз (DREAD)',
    assets: 'Реестр ключевых ИА',
    threats: 'Реестр угроз',
    riskMatrix: 'Матрица рисков',
    risks: 'Реестр рисков',
    controls: 'Риск-контроль',
    dictionaries: 'Техническая страница',
    users: 'Пользователи',
    infoSystems: 'Перечень инфосистем',
  },

  role: {
    admin: 'Администратор',
    user: 'Пользователь',
  },

  action: {
    add: 'Добавить',
    edit: 'Изменить',
    delete: 'Удалить',
    save: 'Сохранить',
    saving: 'Сохранение…',
    cancel: 'Отмена',
    close: 'Закрыть',
    confirm: 'Подтверждение',
    logout: 'Выйти',
    loading: 'Загрузка…',
    back: 'Назад',
    forward: 'Вперёд',
    actions: 'Действия',
    reset: 'Сбросить',
  },

  sidebar: {
    exportExcel: 'Экспорт в Excel',
    exporting: 'Выгрузка…',
    loadDemo: 'Загрузить демо-данные',
    loadingDemo: 'Загрузка…',
  },

  toast: {
    exported: 'Файл Excel выгружен',
    exportFailed: 'Не удалось выгрузить файл',
    exportedWithWarnings: (details: string) => `Выгружено с замечаниями: ${details}`,
    demoLoaded: (r: number, a: number, t: number) =>
      `Демо-данные загружены: рисков ${r}, активов ${a}, угроз ${t}`,
    demoFailed: 'Не удалось загрузить демо-данные',
  },

  table: {
    empty: 'Нет записей',
    emptyHint: 'Записи появятся здесь после добавления',
    total: (n: number) => `Всего записей: ${n}`,
    page: (current: number, total: number, all: number) =>
      `Стр. ${current} из ${total} · всего ${all}`,
  },

  dashboard: {
    title: 'Главная',
    risks: 'Рисков',
    assets: 'Ключевых ИА',
    threats: 'Угроз',
    controls: 'Контролей',
    implementedPercent: 'Контролей внедрено',
    implementedHint: (impl: number, total: number) => `${impl} из ${total} привязок`,
    overdue: 'Просроченных мероприятий',
    overdueHint: 'срок прошёл, статус ≠ Выполнено',
    distribution: 'Распределение рисков по уровням',
    currentLevel: 'Текущий уровень (после внедрённых)',
    residualLevel: 'Остаточный уровень (+ запланированные)',
    keyAssets: 'Ключевые информационные активы',
    gaugeHint: 'Стрелка показывает наивысший текущий уровень риска по активу.',
    noAssets: 'Активы ещё не зарегистрированы.',
    noRisksForAsset: 'рисков не зарегистрировано',
    assetRisks: (count: number, residual: string | null) =>
      `рисков: ${count}${residual ? ` · остаточный: ${residual}` : ''}`,
    noRisks: 'нет рисков',
  },

  filter: {
    all: 'Все',
    matched: (n: number) => `Найдено: ${n}`,
    reset: (n: number) => `Сбросить фильтры (${n})`,
    title: 'Фильтры по столбцам',
  },

  charts: {
    reductionTitle: 'Снижение риска за счёт контролей',
    reductionNote:
      'Как смещается распределение рисков по мере применения контролей: без контролей → с внедрёнными → с учётом запланированных.',
    stageInherent: 'Без контролей',
    stageCurrent: 'Внедрённые',
    stageResidual: '+ Запланированные',

    timelineTitle: 'Выполнение плана мероприятий',
    timelineNote:
      'Нарастающим итогом по сроку внедрения. Разрыв между линиями — накопленное отставание.',
    timelineDue: 'Срок наступил',
    timelineDone: 'Выполнено',

    treatmentTitle: 'Методы обработки рисков',
    treatmentCenter: 'рисков',
    treatmentNote: 'По полю «Метод обработки риска» в реестре рисков.',

    statusTitle: 'Статус мероприятий',
    statusNote: 'По значению поля «Статус мероприятий» в реестре рисков.',

    risksAxis: 'Количество рисков',
    measuresAxis: 'Количество мероприятий',
    noData: 'Недостаточно данных для построения графика.',
    noDeadlines: 'Ни для одного риска не указан срок внедрения.',
    months: ['янв', 'фев', 'мар', 'апр', 'мая', 'июн', 'июл', 'авг', 'сен', 'окт', 'ноя', 'дек'],
  },

  assets: {
    title: 'Реестр ключевых ИА',
    subtitle: 'Ключевые информационные активы. Рейтинг 1–5 берётся из справочника значимости.',
    add: 'Добавить актив',
    newTitle: 'Новый ключевой ИА',
    editTitle: (code: string) => `Актив ${code}`,
    search: 'Поиск по названию, коду, категории…',
    colId: 'ID',
    colName: 'Название актива',
    colScope: 'Масштаб',
    colCategory: 'Категория информации',
    colCriticality: 'Значимость',
    colRating: 'Рейтинг',
    colSecurityClass: 'Класс защ.',
    fieldName: 'Название актива',
    fieldScope: 'Масштаб системы',
    fieldCategory: 'Категория обрабатываемой информации',
    fieldCriticality: 'Значимость актива',
    fieldSecurityClass: 'Класс защищенности',
    criticalityHint: 'Рейтинг 1–5 рассчитывается автоматически по справочнику',
    deleteConfirm: (code: string, name: string) => `Удалить актив ${code} «${name}»?`,
  },

  threats: {
    title: 'Реестр угроз',
    subtitle: 'Оценка по модели DREAD — сумма и уровень угрозы рассчитываются автоматически.',
    add: 'Добавить угрозу',
    newTitle: 'Новая угроза',
    editTitle: (code: string) => `Угроза ${code}`,
    search: 'Поиск по описанию, коду, уровню…',
    colCode: '#',
    colDescription: 'Угрозы',
    colSum: 'Сумма',
    colLevel: 'Уровень угрозы',
    fieldDescription: 'Описание угрозы',
    dreadLegend: 'Оценка по модели DREAD (0–5)',
    dreadDiscoverability: 'Обнаружение',
    dreadRepeatability: 'Повторение',
    dreadExploitability: 'Эксплуатирование',
    dreadAffectedUsers: 'Масштаб',
    dreadDamage: 'Ущерб',
    previewSum: 'Сумма',
    previewLevel: 'Уровень',
    previewHint: 'Рассчитывается сервером при сохранении',
    deleteConfirm: (code: string) => `Удалить угрозу ${code}?`,
  },

  controls: {
    title: 'Риск-контроль',
    subtitle: 'Каталог мер снижения риска. Проценты перемножаются по цепочке контролей.',
    add: 'Добавить контроль',
    newTitle: 'Новый контроль',
    editTitle: (code: string) => `Контроль ${code}`,
    search: 'Поиск по названию, описанию, методу…',
    colId: 'ID',
    colName: 'Название контроля',
    colDescription: 'Описание',
    colMethod: 'Метод управления',
    colReduction: '% снижения',
    colImplemented: 'Внедрен?',
    fieldName: 'Название контроля',
    fieldDescription: 'Описание контроля',
    fieldMethod: 'Метод управления риском',
    fieldReduction: 'Процент снижения риска',
    reductionHint: 'Снижения перемножаются: два контроля по 50% дают 75%, а не 100%',
    implemented: 'Внедрен',
    yes: 'Да',
    no: 'Нет',
    deleteConfirm: (code: string) => `Удалить контроль ${code}?`,
  },

  risks: {
    title: 'Реестр рисков',
    subtitle: 'Пара «актив + угроза». Уровни пересчитываются при каждом изменении контролей.',
    add: 'Добавить риск',
    newTitle: 'Новый риск',
    editTitle: (code: string) => `Риск ${code}`,
    search: 'Поиск по названию, активу, угрозе, владельцу…',
    colId: 'ID',
    colAsset: 'ИА',
    colThreat: 'Угроза',
    colName: 'Наименование риска',
    colControls: 'Снижающие контроли',
    colCurrent: 'Уровень риска',
    colResidual: 'Остаточный риск',
    colStatus: 'Статус',
    fieldAsset: 'Связанный ИА',
    fieldThreat: 'Связанная угроза',
    fieldName: 'Наименование риска',
    fieldIndicators: 'Индикаторы риска',
    fieldOwner: 'Владелец риска',
    fieldMethod: 'Метод управления риском',
    fieldStatus: 'Статус мероприятий',
    fieldDeadline: 'Финальная дата внедрения мероприятий',
    fieldComment: 'Комментарий',
    pairLocked: 'Пара «актив + угроза» уникальна и не меняется после создания',
    controlsButton: 'Контроли',
    deleteConfirm: (code: string) => `Удалить риск ${code}? Привязки контролей также будут удалены.`,
    matrixFilter: (asset: number | string, threat: number | string) =>
      `Фильтр из матрицы: критичность актива ${asset}, уровень угрозы ${threat}.`,
    emptyCell: 'В этой ячейке матрицы нет рисков',
    assetRating: (n: number) => `рейтинг ${n}`,
    threatScore: (n: number) => `счёт ${n}`,
  },

  drawer: {
    asset: 'Актив',
    threat: 'Угроза',
    owner: 'Владелец',
    score: (n: number | string) => `счёт ${n}`,
    inherent: 'Присущий риск',
    current: 'Уровень риска',
    residual: 'Остаточный риск',
    inherentNote: 'без контролей',
    currentNote: 'внедрённые контроли',
    residualNote: '+ запланированные',
    threatRating: (n: number | string) => `уровень угрозы ${n}`,
    tabImplemented: (n: number) => `Внедренные контроли (${n})`,
    tabPlanned: (n: number) => `Запланированные контроли (${n})`,
    noControls: 'Контроли не привязаны.',
    colId: 'ID',
    colName: 'Название',
    colReduction: 'Снижение',
    detach: 'Убрать',
    attach: 'Привязать',
    pickControl: '— выберите контроль из каталога —',
  },

  matrix: {
    title: 'Матрица рисков',
    subtitle: (total: number) =>
      `Всего рисков: ${total}. Строки — критичность актива, столбцы — уровень угрозы после внедрённых контролей. Нажмите на ячейку, чтобы увидеть её риски.`,
    yAxis: 'Критичность актива',
    xAxis: 'Уровень угрозы',
    legend: 'Легенда',
    legendAsset: 'Значимость актива',
    legendThreat: 'Уровень угрозы',
    legendRisk: 'Уровень риска',
    cellTitle: (a: number, t: number, label: string, count: number | null) =>
      `Критичность ${a} × угроза ${t} → ${label}${count ? ` · рисков: ${count}` : ' · нет рисков'}`,
  },

  dictionaries: {
    title: 'Техническая страница',
    subtitle:
      'Эти значения формируют все выпадающие списки в системе. Уровни 1–5 у значимости актива и уровня угрозы участвуют в расчёте риска — изменение повлияет на вычисленные уровни.',
    readOnly: 'У вас нет прав на изменение справочников — страница доступна только для чтения.',
    colIndex: '#',
    colValue: 'Значение',
    colLevel: 'Уровень',
    addValue: '+ Добавить значение',
    saved: (title: string) => `«${title}» сохранено`,
  },

  users: {
    title: 'Пользователи',
    accounts: 'Учётные записи',
    colLogin: 'Логин',
    colFullName: 'ФИО',
    colEmail: 'Email',
    colRole: 'Роль',
    colStatus: 'Статус',
    active: 'Активен',
    inactive: 'Отключён',
    permissions: 'Права',
    deactivate: 'Отключить',
    activate: 'Включить',
    newUser: 'Новый пользователь',
    fieldLogin: 'Логин',
    fieldFullName: 'ФИО',
    fieldEmail: 'Email',
    fieldPassword: 'Пароль',
    create: 'Создать',
    created: 'Пользователь создан (по умолчанию: только чтение)',
    permissionsFor: (name: string, login: string) => `Права доступа — ${name} (${login})`,
    adminNote:
      'Администратор всегда имеет полный доступ ко всем модулям; сетка прав не редактируется.',
    colModule: 'Модуль',
    permissionsSaved: 'Права сохранены',
  },

  infoSystems: {
    title: 'Перечень инфосистем Банка',
    subtitle:
      'Инвентарь информационных систем. К записи можно привязать ключевой актив.',
    add: 'Добавить систему',
    newTitle: 'Новая информационная система',
    editTitle: (code: string) => `Система ${code}`,
    search: 'Поиск по названию, описанию, владельцу…',
    colId: 'ID',
    colName: 'Название ресурса',
    colDescription: 'Описание ресурса',
    colFormat: 'Формат',
    colConfidentiality: 'Конфиденциальность',
    colIntegrity: 'Целостность',
    colAvailability: 'Доступность',
    colOwner: 'Владелец',
    fieldName: 'Название ресурса',
    fieldDescription: 'Описание ресурса',
    fieldHosting: 'Размещение',
    fieldUsage: 'Использование',
    fieldFormat: 'Формат',
    fieldConfidentiality: 'Уровень конфиденциальности',
    fieldIntegrity: 'Целостность',
    fieldAvailability: 'Доступность',
    fieldUpdateFrequency: 'Частота обновления данных',
    fieldUsers: 'Пользователи',
    fieldOwner: 'Владелец',
    deleteConfirm: (code: string, name: string) => `Удалить систему ${code} «${name}»?`,
  },

  modules: {
    ASSETS: 'Реестр ключевых ИА',
    THREATS: 'Реестр угроз',
    RISKS: 'Реестр рисков',
    CONTROLS: 'Риск-контроль',
    RISK_CONTROLS: 'Связи риск–контроль',
    DICTIONARIES: 'Техническая страница',
    INFO_SYSTEMS: 'Перечень инфосистем',
  },

  /**
   * Display names for values the SERVER stores in Russian.
   *
   * The database keeps the Russian label because the Excel export must match
   * the source workbook regardless of who is looking at the screen. The UI maps
   * it for display only — nothing is rewritten in storage.
   */
  riskLevel: {
    Незначительный: 'Незначительный',
    Низкий: 'Низкий',
    Средний: 'Средний',
    Высокий: 'Высокий',
    Критический: 'Критический',
    'Очень высокий': 'Очень высокий',
  } as Record<string, string>,

  criticality: {
    'Очень низкая': 'Очень низкая',
    Низкая: 'Низкая',
    Средняя: 'Средняя',
    Высокая: 'Высокая',
    Критичная: 'Критичная',
  } as Record<string, string>,

  treatmentMethod: {
    Снижение: 'Снижение',
    Перемещение: 'Перемещение',
    Избегание: 'Избегание',
    Принятие: 'Принятие',
  } as Record<string, string>,

  measureStatus: {
    'Укладывается в срок': 'Укладывается в срок',
    Задержка: 'Задержка',
    Проблема: 'Проблема',
    Выполнено: 'Выполнено',
  } as Record<string, string>,

  /** Threat levels. Same five words as risk levels except the top one. */
  threatLevel: {
    Незначительный: 'Незначительный',
    Низкий: 'Низкий',
    Средний: 'Средний',
    Высокий: 'Высокий',
    'Очень высокий': 'Очень высокий',
  },

  dictTitle: {
    ASSET_CRITICALITY: 'Значимость актива',
    THREAT_LEVEL: 'Уровень угрозы',
    TREATMENT_METHOD: 'Метод управления риском',
    MEASURE_STATUS: 'Статус мероприятий',
  },

  common: {
    select: '— выберите —',
    none: '—',
    noAccess: 'нет доступа',
  },

  /**
   * Strings for the "how was this calculated?" hints.
   *
   * Written for a risk officer, not a programmer. The lead sentence says what
   * is happening in words; the numbers arrive with their labels attached
   * ("Критичная · 5", never a bare 5); and where a rule decides something, the
   * card SHOWS the rule as a picture - the 5x5 grid, the band ladder - rather
   * than transcribing its condition. The literal workbook formula is still
   * there for anyone auditing against Excel, folded away at the bottom.
   */
  formula: {
    ariaLabel: (what: string) => `Как рассчитано: ${what}`,
    excelToggle: 'Формула из Excel',

    srcTech: 'Техническая страница',
    srcThreats: 'Реестр угроз',
    srcAssets: 'Реестр ключевых ИА',
    srcRisks: 'Реестр рисков',
    srcMatrix: 'Матрица рисков',
    column: (col: string) => `столбец ${col}`,

    // --- asset rating
    assetRatingTitle: 'Рейтинг актива',
    assetRatingLead:
      'Каждой словесной оценке значимости соответствует число от 1 до 5. Оно берётся из справочника на «Технической странице».',
    assetRatingInput: 'Значимость актива',
    assetRatingExcel: '=ВПР(E2;\'Техническая страница\'!$A$2:$B$6;2;ЛОЖЬ)',

    // --- DREAD
    dreadSumTitle: 'Сумма DREAD',
    dreadSumLead:
      'Пять показателей угрозы, каждый от 0 до 5 баллов. Складываются — получается от 0 до 25.',
    dreadRatingTitle: 'Уровень угрозы',
    dreadRatingLead: 'Сумма баллов попадает в одну из пяти групп. Группа и есть уровень угрозы.',
    dreadSumInput: 'Сумма баллов',
    dreadSumExcel: '=СУММ(L2:P2)',
    dreadRatingExcel: '=ЕСЛИ(Q2<6;1;ЕСЛИ(Q2<11;2;ЕСЛИ(Q2<16;3;ЕСЛИ(Q2<21;4;5))))',

    // --- reduction chain
    chainTitleImplemented: 'Счёт после внедрённых контролей',
    chainTitlePlanned: 'Счёт после запланированных контролей',
    chainLeadImplemented:
      'Каждый внедрённый контроль снижает счёт угрозы на свой процент. Контроли применяются один за другим.',
    chainLeadPlanned:
      'Запланированные контроли снижают счёт дальше — начиная с того, что уже дали внедрённые.',
    chainBase: 'Счёт угрозы',
    chainBaseCurrent: 'Счёт после внедрённых',
    chainNone: 'Контроли не привязаны — счёт остаётся прежним.',
    chainNote:
      'Проценты перемножаются, а не складываются: два контроля по 50 % дают 75 %, а не 100 %.',
    chainExcelImplemented: '=AH2-AH2*AJ2   (и так далее по цепочке)',
    chainExcelPlanned: '=AW2-AW2*AX2   (и так далее по цепочке)',

    // --- classify(a, t)
    riskLevelLead:
      'Уровень риска — это пересечение двух оценок: насколько важен актив и насколько силён уровень угрозы. Смотрим клетку на их пересечении.',
    riskLevelAsset: 'Значимость актива',
    riskLevelThreat: 'Уровень угрозы',
    matrixAxisAsset: 'значимость актива',
    matrixAxisThreat: 'уровень угрозы',
    matrixYouAreHere: 'Ваша клетка',

    inherentTitle: 'Риск без контролей',
    currentTitle: 'Текущий уровень риска',
    residualTitle: 'Остаточный уровень риска',
    riskLevelExcel:
      '=ЕСЛИ(AF2*BV2>=20;"Критический";\n ЕСЛИ(ИЛИ(И(AF2=1;BV2<3);И(BV2=1;AF2<4));"Незначительный";\n ЕСЛИ(И(BV2>2;AF2*BV2>=10);"Высокий";\n ЕСЛИ(ИЛИ(И(BV2<4;BV2*AF2>3;BV2*AF2<6);И(BV2=3;AF2<3));"Низкий";\n "Средний"))))',

    /**
     * Plain-language reason, phrased as a sentence a person would say out loud.
     * Each takes the two words, not the two numbers.
     */
    whyCritical: (asset: string, threat: string) =>
      `Актив «${asset}» и угроза «${threat}» вместе дают верхнюю клетку — риск критический.`,
    whyNegligible: (asset: string, threat: string) =>
      `Угроза «${threat}» слишком слаба, чтобы всерьёз навредить активу «${asset}».`,
    whyHigh: (asset: string, threat: string) =>
      `Заметная угроза «${threat}» на важном активе «${asset}» — риск высокий.`,
    whyLow: (asset: string, threat: string) =>
      `Сочетание «${asset}» и «${threat}» остаётся в нижней части матрицы.`,
    whyMedium: (asset: string, threat: string) =>
      `Актив «${asset}», но уровень угрозы всего «${threat}» — до высокого не дотягивает, остаётся средний.`,

    // --- gauge
    gaugeTitle: 'Положение стрелки',
    gaugeNote: (count: number) =>
      count === 1
        ? 'По этому активу зарегистрирован один риск.'
        : `Показан худший из ${count} рисков по этому активу.`,
    gaugeNoRisks: 'По активу нет зарегистрированных рисков — стрелка не выводится.',
    gaugeWhySame:
      'Значимость актива задаёт только потолок: пока уровень угрозы низкий, даже критичный актив даёт средний риск.',

    // --- dashboard counters
    percentTitle: 'Доля внедрённых контролей',
    percentLead:
      'Сколько привязок «риск — контроль» уже внедрено, из всех привязок вместе с запланированными.',
    percentImplemented: 'Внедрено',
    percentPlanned: 'Запланировано',
    percentTotal: 'Всего привязок',
    percentExcel: '=Внедрённые/(Внедрённые+Запланированные)*100',
    overdueTitle: 'Просроченные мероприятия',
    overdueLead:
      'Риски, у которых дата внедрения уже прошла, а статус мероприятий ещё не «Выполнено».',
    overdueToday: 'Сегодня',
    overdueNote: 'Учитываются только риски с указанным сроком внедрения.',
    overdueExcel: '=СЧЁТЕСЛИМН(O:O;"<"&СЕГОДНЯ();N:N;"<>Выполнено")',

    // --- matrix cell
    matrixCellTitle: 'Клетка матрицы',
    matrixCount: 'Рисков в этой клетке',
    matrixEmpty: 'нет',
    matrixCellExcel: '=СЧЁТЕСЛИМН(\'Реестр рисков\'!$AF:$AF;$B2;\'Реестр рисков\'!$BV:$BV;C$7)',
  },
};

/**
 * Every other language must provide exactly this shape.
 *
 * Deliberately NOT `typeof (ru as const)`: `as const` would infer each value as
 * its own literal type, so `Dictionary` would demand the exact Russian strings
 * back and every Uzbek translation would be a type error. Without it the values
 * widen to `string`, which is what we want — the contract is the set of keys and
 * the function signatures, not the words.
 */
export type Dictionary = typeof ru;
