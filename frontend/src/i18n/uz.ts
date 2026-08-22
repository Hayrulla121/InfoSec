import type { Dictionary } from './ru';

/**
 * Uzbek UI strings (Latin script, matching the wording already used in the
 * workbook's own Uzbek sheets).
 *
 * Typed as `Dictionary`, so omitting a key — or misspelling one — fails the
 * build rather than silently rendering `undefined` on a screen nobody opened
 * during review.
 */
export const uz: Dictionary = {
  lang: {
    name: "O'zbekcha",
    switchTo: 'Til / Язык',
  },

  brand: {
    title: 'Xavflarni baholash',
    subtitle: 'AT va AX',
    fullTitle: 'AT VA AX RISKLARINI AVTOMATLASHTIRILGAN BOSHQARISH TIZIMI',
  },

  login: {
    prompt: 'Davom etish uchun tizimga kiring',
    username: 'Login',
    password: 'Parol',
    submit: 'Kirish',
    submitting: 'Kirilmoqda…',
    failed: 'Tizimga kirib bo‘lmadi',
    footer: 'Axborot xavfsizligi · ichki tizim',
    // HUD text on the radar backdrop. Localised rather than left as
    // English chrome: a Russian screen with an English word on it is
    // exactly the mixed-language problem the switch exists to remove.
    radarStatus: 'SKANERLASH',
  },

  nav: {
    modules: 'Modullar',
    administration: 'Boshqaruv',
    home: 'Bosh sahifa',
    threatModel: "Ma'lumot — Tahdidlar modeli",
    assets: 'Asosiy axborot aktivlari',
    threats: 'Tahdidlar reyestri',
    riskMatrix: 'Xavflar matritsasi',
    risks: 'Xavflar reyestri',
    controls: 'Xavf nazorati',
    dictionaries: 'Texnik sahifa',
    users: 'Foydalanuvchilar',
    infoSystems: 'Axborot tizimlari',
  },

  role: {
    admin: 'Administrator',
    user: 'Foydalanuvchi',
  },

  action: {
    add: "Qo‘shish",
    edit: "O‘zgartirish",
    delete: "O‘chirish",
    save: 'Saqlash',
    saving: 'Saqlanmoqda…',
    cancel: 'Bekor qilish',
    close: 'Yopish',
    confirm: 'Tasdiqlash',
    logout: 'Chiqish',
    loading: 'Yuklanmoqda…',
    back: 'Orqaga',
    forward: 'Oldinga',
    actions: 'Amallar',
    reset: 'Tozalash',
  },

  sidebar: {
    exportExcel: 'Excelga eksport',
    exporting: 'Yuklanmoqda…',
    loadDemo: "Demo ma'lumotlarni yuklash",
    loadingDemo: 'Yuklanmoqda…',
  },

  toast: {
    exported: 'Excel fayli yuklab olindi',
    exportFailed: 'Faylni yuklab bo‘lmadi',
    exportedWithWarnings: (details: string) => `Eslatmalar bilan yuklandi: ${details}`,
    demoLoaded: (r: number, a: number, t: number) =>
      `Demo ma'lumotlar yuklandi: xavflar ${r}, aktivlar ${a}, tahdidlar ${t}`,
    demoFailed: "Demo ma'lumotlarni yuklab bo‘lmadi",
  },

  table: {
    empty: 'Yozuvlar yo‘q',
    emptyHint: "Yozuvlar qo‘shilgandan so‘ng shu yerda ko‘rinadi",
    total: (n: number) => `Jami yozuvlar: ${n}`,
    page: (current: number, total: number, all: number) =>
      `${current} / ${total}-sahifa · jami ${all}`,
  },

  dashboard: {
    title: "O'ZBEKISTON RESPUBLIKASI MARKAZIY BANKI AXBOROT AKTIVLARI HOLATI",
    risks: 'Xavflar',
    assets: 'Asosiy aktivlar',
    threats: 'Tahdidlar',
    controls: 'Nazoratlar',
    implementedPercent: 'Nazoratlar joriy etilgan',
    implementedHint: (impl: number, total: number) => `${total} tadan ${impl} tasi`,
    overdue: 'Muddati o‘tgan tadbirlar',
    overdueHint: 'muddati o‘tgan, holati ≠ Bajarilgan',
    distribution: 'Xavflarning darajalar bo‘yicha taqsimoti',
    currentLevel: 'Joriy daraja (joriy etilganlardan keyin)',
    residualLevel: 'Qoldiq daraja (+ rejalashtirilganlar)',
    keyAssets: 'Asosiy axborot aktivlari',
    gaugeHint: "Strelka aktiv bo‘yicha eng yuqori joriy xavf darajasini ko‘rsatadi.",
    noAssets: 'Aktivlar hali ro‘yxatdan o‘tkazilmagan.',
    noRisksForAsset: 'xavflar ro‘yxatdan o‘tkazilmagan',
    assetRisks: (count: number, residual: string | null) =>
      `xavflar: ${count}${residual ? ` · qoldiq: ${residual}` : ''}`,
    noRisks: 'xavflar yo‘q',
  },

  filter: {
    all: 'Barchasi',
    matched: (n: number) => `Topildi: ${n}`,
    reset: (n: number) => `Filtrlarni tozalash (${n})`,
    title: 'Ustunlar boʻyicha filtr',
  },

  charts: {
    reductionTitle: 'Nazoratlar hisobiga xavfning kamayishi',
    reductionNote:
      'Nazoratlar qo‘llanilishi bilan xavflar taqsimoti qanday siljiydi: nazoratsiz → joriy etilganlari bilan → rejalashtirilganlari hisobga olingan holda.',
    stageInherent: 'Nazoratsiz',
    stageCurrent: 'Joriy etilgan',
    stageResidual: '+ Rejalashtirilgan',

    timelineTitle: 'Tadbirlar rejasining bajarilishi',
    timelineNote:
      'Joriy etish muddati bo‘yicha o‘sib boruvchi jami. Chiziqlar orasidagi farq — to‘plangan orqada qolish.',
    timelineDue: 'Muddati keldi',
    timelineDone: 'Bajarilgan',

    treatmentTitle: 'Xavfni boshqarish usullari',
    treatmentCenter: 'xavf',
    treatmentNote: 'Xavflar reyestridagi «Xavfni boshqarish usuli» maydoni bo‘yicha.',

    statusTitle: 'Tadbirlar holati',
    statusNote: 'Xavflar reyestridagi «Tadbirlar holati» maydoni qiymati bo‘yicha.',

    risksAxis: 'Xavflar soni',
    measuresAxis: 'Tadbirlar soni',
    noData: 'Grafik qurish uchun ma’lumot yetarli emas.',
    noDeadlines: 'Hech bir xavf uchun joriy etish muddati ko‘rsatilmagan.',
    months: ['yan', 'fev', 'mar', 'apr', 'may', 'iyn', 'iyl', 'avg', 'sen', 'okt', 'noy', 'dek'],
  },

  assets: {
    title: 'Asosiy axborot aktivlari reyestri',
    subtitle:
      "Asosiy axborot aktivlari. 1–5 reyting muhimlik ma'lumotnomasidan olinadi.",
    add: "Aktiv qo‘shish",
    newTitle: 'Yangi asosiy aktiv',
    editTitle: (code: string) => `${code} aktivi`,
    search: 'Nomi, kodi, toifasi bo‘yicha qidirish…',
    colId: 'ID',
    colName: 'Aktiv nomi',
    colScope: 'Ko‘lami',
    colCategory: 'Axborot toifasi',
    colCriticality: 'Muhimligi',
    colRating: 'Reyting',
    colSecurityClass: 'Himoya sinfi',
    fieldName: 'Aktiv nomi',
    fieldScope: 'Tizim ko‘lami',
    fieldCategory: 'Qayta ishlanadigan axborot toifasi',
    fieldCriticality: 'Aktivning muhimlik darajasi',
    fieldSecurityClass: 'Himoyalanganlik sinfi',
    criticalityHint:
      "1–5 reyting ma'lumotnoma asosida avtomatik hisoblanadi",
    deleteConfirm: (code: string, name: string) =>
      `${code} «${name}» aktivi o‘chirilsinmi?`,
  },

  threats: {
    title: 'Tahdidlar reyestri',
    subtitle:
      'DREAD modeli bo‘yicha baholash — yig‘indi va tahdid darajasi avtomatik hisoblanadi.',
    add: "Tahdid qo‘shish",
    newTitle: 'Yangi tahdid',
    editTitle: (code: string) => `${code} tahdidi`,
    search: 'Tavsifi, kodi, darajasi bo‘yicha qidirish…',
    colCode: '#',
    colDescription: 'Tahdidlar',
    colSum: 'Yig‘indi',
    colLevel: 'Tahdid darajasi',
    fieldDescription: 'Tahdid tavsifi',
    dreadLegend: 'DREAD modeli bo‘yicha baholash (0–5)',
    dreadDiscoverability: 'Aniqlash',
    dreadRepeatability: 'Takrorlash',
    dreadExploitability: 'Ekspluatatsiya',
    dreadAffectedUsers: 'Ko‘lami',
    dreadDamage: 'Zarar',
    previewSum: 'Yig‘indi',
    previewLevel: 'Daraja',
    previewHint: 'Saqlashda server tomonidan hisoblanadi',
    deleteConfirm: (code: string) => `${code} tahdidi o‘chirilsinmi?`,
  },

  controls: {
    title: 'Xavf nazorati',
    subtitle:
      'Xavfni kamaytirish choralari katalogi. Foizlar nazoratlar zanjiri bo‘ylab ko‘paytiriladi.',
    add: "Nazorat qo‘shish",
    newTitle: 'Yangi nazorat',
    editTitle: (code: string) => `${code} nazorati`,
    search: 'Nomi, tavsifi, usuli bo‘yicha qidirish…',
    colId: 'ID',
    colName: 'Nazorat nomi',
    colDescription: 'Tavsifi',
    colMethod: 'Boshqarish usuli',
    colReduction: 'Kamaytirish %',
    colImplemented: 'Joriy etilgan?',
    fieldName: 'Nazorat nomi',
    fieldDescription: 'Nazorat tavsifi',
    fieldMethod: 'Xavfni boshqarish usuli',
    fieldReduction: 'Xavfni kamaytirish foizi',
    reductionHint:
      'Kamaytirishlar ko‘paytiriladi: 50% dan ikkita nazorat 100% emas, 75% beradi',
    implemented: 'Joriy etilgan',
    yes: 'Ha',
    no: 'Yo‘q',
    deleteConfirm: (code: string) => `${code} nazorati o‘chirilsinmi?`,
  },

  risks: {
    title: 'Xavflar reyestri',
    subtitle:
      "«Aktiv + tahdid» juftligi. Nazoratlar o‘zgarganda darajalar qayta hisoblanadi.",
    add: "Xavf qo‘shish",
    newTitle: 'Yangi xavf',
    editTitle: (code: string) => `${code} xavfi`,
    search: 'Nomi, aktivi, tahdidi, egasi bo‘yicha qidirish…',
    colId: 'ID',
    colAsset: 'Aktiv',
    colThreat: 'Tahdid',
    colName: 'Xavf nomi',
    colControls: 'Kamaytiruvchi nazoratlar',
    colCurrent: 'Xavf darajasi',
    colResidual: 'Qoldiq xavf',
    colStatus: 'Holati',
    fieldAsset: 'Bog‘liq aktiv',
    fieldThreat: 'Bog‘liq tahdid',
    fieldName: 'Xavf nomi',
    fieldIndicators: 'Xavf indikatorlari',
    fieldOwner: 'Xavf egasi',
    fieldMethod: 'Xavfni boshqarish usuli',
    fieldStatus: 'Tadbirlar holati',
    fieldDeadline: 'Tadbirlarni joriy etishning yakuniy sanasi',
    fieldComment: 'Izoh',
    pairLocked:
      "«Aktiv + tahdid» juftligi noyob va yaratilgandan keyin o‘zgarmaydi",
    controlsButton: 'Nazoratlar',
    deleteConfirm: (code: string) =>
      `${code} xavfi o‘chirilsinmi? Nazorat bog‘lanishlari ham o‘chiriladi.`,
    matrixFilter: (asset: number | string, threat: number | string) =>
      `Matritsadan filtr: aktiv muhimligi ${asset}, tahdid darajasi ${threat}.`,
    emptyCell: 'Matritsaning bu katagida xavflar yo‘q',
    assetRating: (n: number) => `reyting ${n}`,
    threatScore: (n: number) => `ball ${n}`,
  },

  drawer: {
    asset: 'Aktiv',
    threat: 'Tahdid',
    owner: 'Egasi',
    score: (n: number | string) => `ball ${n}`,
    inherent: 'Xos xavf',
    current: 'Xavf darajasi',
    residual: 'Qoldiq xavf',
    inherentNote: 'nazoratlarsiz',
    currentNote: 'joriy etilgan nazoratlar',
    residualNote: '+ rejalashtirilganlar',
    threatRating: (n: number | string) => `tahdid darajasi ${n}`,
    tabImplemented: (n: number) => `Joriy etilgan nazoratlar (${n})`,
    tabPlanned: (n: number) => `Rejalashtirilgan nazoratlar (${n})`,
    noControls: 'Nazoratlar bog‘lanmagan.',
    colId: 'ID',
    colName: 'Nomi',
    colReduction: 'Kamaytirish',
    detach: 'Olib tashlash',
    attach: 'Bog‘lash',
    pickControl: '— katalogdan nazoratni tanlang —',
  },

  matrix: {
    title: 'Xavflar matritsasi',
    subtitle: (total: number) =>
      `Jami xavflar: ${total}. Qatorlar — aktiv muhimligi, ustunlar — joriy etilgan nazoratlardan keyingi tahdid darajasi. Katakni bosing va uning xavflarini ko‘ring.`,
    yAxis: 'Aktivning muhimligi',
    xAxis: 'Tahdid darajasi',
    legend: 'Izoh',
    legendAsset: 'Aktivning muhimligi',
    legendThreat: 'Tahdid darajasi',
    legendRisk: 'Xavf darajasi',
    cellTitle: (a: number, t: number, label: string, count: number | null) =>
      `Muhimlik ${a} × tahdid ${t} → ${label}${
        count ? ` · xavflar: ${count}` : ' · xavflar yo‘q'
      }`,
  },

  dictionaries: {
    title: 'Texnik sahifa',
    subtitle:
      "Bu qiymatlar tizimdagi barcha ochiluvchi ro‘yxatlarni to‘ldiradi. Aktiv muhimligi va tahdid darajasidagi 1–5 darajalar xavf hisobida ishtirok etadi — o‘zgarish hisoblangan darajalarga ta'sir qiladi.",
    readOnly:
      "Ma'lumotnomalarni o‘zgartirishga huquqingiz yo‘q — sahifa faqat o‘qish uchun.",
    colIndex: '#',
    colValue: 'Qiymat',
    colLevel: 'Daraja',
    addValue: "+ Qiymat qo‘shish",
    saved: (title: string) => `«${title}» saqlandi`,
  },

  users: {
    title: 'Foydalanuvchilar',
    accounts: 'Hisob yozuvlari',
    colLogin: 'Login',
    colFullName: 'F.I.Sh.',
    colEmail: 'Email',
    colRole: 'Rol',
    colStatus: 'Holati',
    active: 'Faol',
    inactive: 'O‘chirilgan',
    permissions: 'Huquqlar',
    deactivate: 'O‘chirish',
    activate: 'Yoqish',
    newUser: 'Yangi foydalanuvchi',
    fieldLogin: 'Login',
    fieldFullName: 'F.I.Sh.',
    fieldEmail: 'Email',
    fieldPassword: 'Parol',
    create: 'Yaratish',
    created: 'Foydalanuvchi yaratildi (sukut bo‘yicha: faqat o‘qish)',
    permissionsFor: (name: string, login: string) => `Kirish huquqlari — ${name} (${login})`,
    adminNote:
      "Administrator har doim barcha modullarga to‘liq kirish huquqiga ega; huquqlar jadvali tahrirlanmaydi.",
    colModule: 'Modul',
    permissionsSaved: 'Huquqlar saqlandi',
  },

  infoSystems: {
    title: 'Bank axborot tizimlari ro‘yxati',
    subtitle:
      "Axborot tizimlari inventari. Yozuvga asosiy aktivni bog‘lash mumkin.",
    add: "Tizim qo‘shish",
    newTitle: 'Yangi axborot tizimi',
    editTitle: (code: string) => `${code} tizimi`,
    search: 'Nomi, tavsifi, egasi bo‘yicha qidirish…',
    colId: 'ID',
    colName: 'Resurs nomi',
    colDescription: 'Resurs tavsifi',
    colFormat: 'Format',
    colConfidentiality: 'Maxfiylik',
    colIntegrity: 'Yaxlitlik',
    colAvailability: 'Foydalanuvchanlik',
    colOwner: 'Egasi',
    fieldName: 'Resurs nomi',
    fieldDescription: 'Resurs tavsifi',
    fieldHosting: 'Joylashuvi',
    fieldUsage: 'Foydalanish',
    fieldFormat: 'Format',
    fieldConfidentiality: 'Maxfiylik darajasi',
    fieldIntegrity: 'Yaxlitlik',
    fieldAvailability: 'Foydalanuvchanlik',
    fieldUpdateFrequency: "Ma'lumotlarni yangilash chastotasi",
    fieldUsers: 'Foydalanuvchilar',
    fieldOwner: 'Egasi',
    deleteConfirm: (code: string, name: string) =>
      `${code} «${name}» tizimi o‘chirilsinmi?`,
  },

  modules: {
    ASSETS: 'Asosiy axborot aktivlari',
    THREATS: 'Tahdidlar reyestri',
    RISKS: 'Xavflar reyestri',
    CONTROLS: 'Xavf nazorati',
    RISK_CONTROLS: 'Xavf-nazorat bog‘lanishlari',
    DICTIONARIES: 'Texnik sahifa',
    INFO_SYSTEMS: 'Axborot tizimlari ro‘yxati',
  },

  // Server-stored Russian labels, mapped for display only.
  riskLevel: {
    Незначительный: 'Ahamiyatsiz',
    Низкий: 'Past',
    Средний: 'O‘rta',
    Высокий: 'Yuqori',
    Критический: 'Kritik',
    'Очень высокий': 'Juda yuqori',
  },

  criticality: {
    'Очень низкая': 'Juda past',
    Низкая: 'Past',
    Средняя: 'O‘rta',
    Высокая: 'Yuqori',
    Критичная: 'Kritik',
  },

  treatmentMethod: {
    Снижение: 'Kamaytirish',
    Перемещение: 'O‘tkazish',
    Избегание: 'Qochish',
    Принятие: 'Qabul qilish',
  },

  measureStatus: {
    'Укладывается в срок': 'Muddatida',
    Задержка: 'Kechikish',
    Проблема: 'Muammo',
    Выполнено: 'Bajarilgan',
  },

  threatLevel: {
    Незначительный: 'Ahamiyatsiz',
    Низкий: 'Past',
    Средний: "O‘rta",
    Высокий: 'Yuqori',
    'Очень высокий': 'Juda yuqori',
  },

  dictTitle: {
    ASSET_CRITICALITY: 'Aktivning muhimligi',
    THREAT_LEVEL: 'Tahdid darajasi',
    TREATMENT_METHOD: 'Xavfni boshqarish usuli',
    MEASURE_STATUS: 'Tadbirlar holati',
  },

  common: {
    select: '— tanlang —',
    none: '—',
    noAccess: 'kirish yo‘q',
  },

  formula: {
    ariaLabel: (what: string) => `Qanday hisoblangan: ${what}`,
    excelToggle: 'Excel formulasi',

    srcTech: 'Texnik sahifa',
    srcThreats: 'Tahdidlar reyestri',
    srcAssets: 'Kalit AAlar reyestri',
    srcRisks: 'Xavflar reyestri',
    srcMatrix: 'Xavflar matritsasi',
    column: (col: string) => `${col} ustuni`,

    assetRatingTitle: 'Aktiv reytingi',
    assetRatingLead:
      'Har bir muhimlik darajasiga 1 dan 5 gacha son mos keladi. U «Texnik sahifa» ma\'lumotnomasidan olinadi.',
    assetRatingInput: 'Aktivning muhimligi',
    assetRatingExcel: '=VLOOKUP(E2;\'Техническая страница\'!$A$2:$B$6;2;FALSE)',

    dreadSumTitle: 'DREAD yig‘indisi',
    dreadSumLead:
      'Tahdidning beshta ko‘rsatkichi, har biri 0 dan 5 ballgacha. Qo‘shiladi — 0 dan 25 gacha chiqadi.',
    dreadRatingTitle: 'Tahdid darajasi',
    dreadRatingLead:
      'Ballar yig‘indisi beshta guruhdan biriga tushadi. Shu guruh tahdid darajasi bo‘ladi.',
    dreadSumInput: 'Ballar yig‘indisi',
    dreadSumExcel: '=SUM(L2:P2)',
    dreadRatingExcel: '=IF(Q2<6;1;IF(Q2<11;2;IF(Q2<16;3;IF(Q2<21;4;5))))',

    chainTitleImplemented: 'Joriy etilgan nazoratlardan keyingi ball',
    chainTitlePlanned: 'Rejalashtirilgan nazoratlardan keyingi ball',
    chainLeadImplemented:
      'Har bir joriy etilgan nazorat tahdid balini o‘z foiziga kamaytiradi. Nazoratlar ketma-ket qo‘llanadi.',
    chainLeadPlanned:
      'Rejalashtirilgan nazoratlar balni yanada kamaytiradi — joriy etilganlar qoldirgan joydan boshlab.',
    chainBase: 'Tahdid bali',
    chainBaseCurrent: 'Joriy etilganlardan keyingi ball',
    chainNone: 'Nazoratlar biriktirilmagan — ball o‘zgarmaydi.',
    chainNote:
      'Foizlar qo‘shilmaydi, ko‘paytiriladi: 50 % li ikkita nazorat 100 % emas, 75 % beradi.',
    chainExcelImplemented: '=AH2-AH2*AJ2   (zanjir bo‘ylab shu tarzda)',
    chainExcelPlanned: '=AW2-AW2*AX2   (zanjir bo‘ylab shu tarzda)',

    riskLevelLead:
      'Xavf darajasi — ikki bahoning kesishmasi: aktiv qanchalik muhim va tahdid qanchalik kuchli. Ularning kesishgan katagiga qaraymiz.',
    riskLevelAsset: 'Aktivning muhimligi',
    riskLevelThreat: 'Tahdid darajasi',
    matrixAxisAsset: 'aktivning muhimligi',
    matrixAxisThreat: 'tahdid darajasi',
    matrixYouAreHere: 'Sizning katagingiz',

    inherentTitle: 'Nazoratlarsiz xavf',
    currentTitle: 'Joriy xavf darajasi',
    residualTitle: 'Qoldiq xavf darajasi',
    riskLevelExcel:
      '=IF(AF2*BV2>=20;"Критический";\n IF(OR(AND(AF2=1;BV2<3);AND(BV2=1;AF2<4));"Незначительный";\n IF(AND(BV2>2;AF2*BV2>=10);"Высокий";\n IF(OR(AND(BV2<4;BV2*AF2>3;BV2*AF2<6);AND(BV2=3;AF2<3));"Низкий";\n "Средний"))))',

    whyCritical: (asset: string, threat: string) =>
      `«${asset}» aktivi va «${threat}» tahdidi birgalikda eng yuqori katakni beradi — xavf kritik.`,
    whyNegligible: (asset: string, threat: string) =>
      `«${threat}» tahdidi «${asset}» aktiviga jiddiy zarar yetkazish uchun juda kuchsiz.`,
    whyHigh: (asset: string, threat: string) =>
      `Muhim «${asset}» aktivida sezilarli «${threat}» tahdidi — xavf yuqori.`,
    whyLow: (asset: string, threat: string) =>
      `«${asset}» va «${threat}» birikmasi matritsaning quyi qismida qoladi.`,
    whyMedium: (asset: string, threat: string) =>
      `Aktiv «${asset}», lekin tahdid darajasi bor-yo‘g‘i «${threat}» — yuqoriga yetmaydi, o‘rta bo‘lib qoladi.`,

    gaugeTitle: 'Strelka holati',
    gaugeNote: (count: number) =>
      count === 1
        ? 'Bu aktiv bo‘yicha bitta xavf ro‘yxatga olingan.'
        : `Bu aktiv bo‘yicha ${count} ta xavfning eng yomoni ko‘rsatilgan.`,
    gaugeNoRisks: 'Aktiv bo‘yicha xavflar ro‘yxatga olinmagan — strelka ko‘rsatilmaydi.',
    gaugeWhySame:
      'Aktivning muhimligi faqat yuqori chegarani belgilaydi: tahdid darajasi past ekan, kritik aktiv ham o‘rta xavf beradi.',

    percentTitle: 'Joriy etilgan nazoratlar ulushi',
    percentLead:
      'Rejalashtirilganlari bilan birga olganda, «xavf — nazorat» bog‘lanishlarining qanchasi joriy etilgan.',
    percentImplemented: 'Joriy etilgan',
    percentPlanned: 'Rejalashtirilgan',
    percentTotal: 'Jami bog‘lanishlar',
    percentExcel: '=Joriy/(Joriy+Rejalashtirilgan)*100',
    overdueTitle: 'Muddati o‘tgan tadbirlar',
    overdueLead:
      'Joriy etish sanasi o‘tib ketgan, ammo tadbirlar holati hali «Bajarilgan» bo‘lmagan xavflar.',
    overdueToday: 'Bugun',
    overdueNote: 'Faqat joriy etish muddati ko‘rsatilgan xavflar hisobga olinadi.',
    overdueExcel: '=COUNTIFS(O:O;"<"&TODAY();N:N;"<>Выполнено")',

    matrixCellTitle: 'Matritsa katagi',
    matrixCount: 'Shu katakdagi xavflar',
    matrixEmpty: 'yo‘q',
    matrixCellExcel: '=COUNTIFS(\'Реестр рисков\'!$AF:$AF;$B2;\'Реестр рисков\'!$BV:$BV;C$7)',
  },
};
