/**
 * Static content of the "Ma'lumot - Tahdidlar modeli" sheet.
 *
 * The Uzbek text is transcribed verbatim from the workbook (cells B2:I18) and
 * is the ORIGINAL — the Russian is a translation of it, not the other way
 * round. Kept as typed data rather than JSX so the page renders either language
 * from the same layout.
 */

export interface DreadCriterion {
  code: string;
  nameUz: string;
  nameRu: string;
  descriptionUz: string;
  descriptionRu: string;
  score0Uz: string;
  score0Ru: string;
  score5Uz: string;
  score5Ru: string;
  recommendationUz: string;
  recommendationRu: string;
}

export const DREAD_TITLE = {
  uz: 'DREAD tahdidlarni baholash modeli',
  ru: 'Модель оценки угроз DREAD',
};

export const DREAD_HEADERS = {
  uz: { criterion: 'Mezon', name: 'Nomi', note: 'Izoh', s0: '0 ball', s5: '5 ball', tips: 'Tavsiyalar' },
  ru: {
    criterion: 'Критерий',
    name: 'Название',
    note: 'Пояснение',
    s0: '0 баллов',
    s5: '5 баллов',
    tips: 'Рекомендации',
  },
};

export const DREAD_CRITERIA: DreadCriterion[] = [
  {
    code: 'Discoverability',
    nameUz: 'Aniqlash imkoniyati',
    nameRu: 'Возможность обнаружения',
    descriptionUz:
      'Hujum qiluvchi tomonidan himoyadagi zaiflikni aniqlashning osonlik darajasi. Qasddan bo‘lmagan harakatlarda esa, subyekt tahdidni yuzaga keltiruvchi harakatni bilgan holda yoki tasodifan amalga oshirish ehtimolini ifodalaydi.',
    descriptionRu:
      'Насколько легко атакующему обнаружить уязвимость в защите. При непреднамеренных действиях отражает вероятность того, что субъект совершит действие, приводящее к угрозе, осознанно или случайно.',
    score0Uz: 'Murakkab',
    score0Ru: 'Сложно',
    score5Uz: 'Oson',
    score5Ru: 'Легко',
    recommendationUz:
      'Agar tahdid tabiiy omillar ta’sirida yuzaga kelsa, mazkur mezon bo‘yicha 0 ball qo‘yiladi.',
    recommendationRu:
      'Если угроза вызвана природными факторами, по этому критерию ставится 0 баллов.',
  },
  {
    code: 'Repeatability',
    nameUz: 'Takrorlash imkoniyati',
    nameRu: 'Возможность повторения',
    descriptionUz:
      'Hujum qiluvchi yoki subyektning tahdidni amalga oshirishga olib keladigan harakatni takroran bajarish imkoniyatining osonlik darajasi.',
    descriptionRu:
      'Насколько легко атакующему или субъекту повторно выполнить действие, приводящее к реализации угрозы.',
    score0Uz: 'Murakkab',
    score0Ru: 'Сложно',
    score5Uz: 'Oson',
    score5Ru: 'Легко',
    recommendationUz:
      'Agar tahdid tabiiy omillar ta’sirida yuzaga kelsa, mazkur mezon bo‘yicha ball hodisaning yuzaga kelish ehtimoli yoki sodir bo‘lish chastotasidan kelib chiqib belgilanadi.',
    recommendationRu:
      'Если угроза вызвана природными факторами, балл определяется исходя из вероятности возникновения события или частоты его наступления.',
  },
  {
    code: 'Exploitability',
    nameUz: 'Ekspluatatsiya qilish (zaiflikdan foydalanish)',
    nameRu: 'Эксплуатирование (использование уязвимости)',
    descriptionUz:
      'Mazkur tahdid bilan bog‘liq himoya jarayonlari va tizimlaridagi zaifliklardan foydalanish imkoniyatining osonlik darajasi.',
    descriptionRu:
      'Насколько легко воспользоваться уязвимостями в процессах и системах защиты, связанных с данной угрозой.',
    score0Uz: 'Murakkab',
    score0Ru: 'Сложно',
    score5Uz: 'Oson',
    score5Ru: 'Легко',
    recommendationUz:
      'Agar tahdid tabiiy omillar bilan bog‘liq bo‘lsa, ushbu mezon bo‘yicha 5 ball qo‘yiladi.',
    recommendationRu:
      'Если угроза связана с природными факторами, по этому критерию ставится 5 баллов.',
  },
  {
    code: 'Affected users',
    nameUz: 'Ta’sir ko‘radigan foydalanuvchilar',
    nameRu: 'Затрагиваемые пользователи',
    descriptionUz:
      'Mazkur tahdid amalga oshirilganda bevosita yoki bilvosita ta’sirga uchraydigan xodimlar, mijozlar va fuqarolar soni.',
    descriptionRu:
      'Число сотрудников, клиентов и граждан, которые прямо или косвенно пострадают при реализации данной угрозы.',
    score0Uz: 'Kamroq',
    score0Ru: 'Меньше',
    score5Uz: 'Ko‘proq',
    score5Ru: 'Больше',
    recommendationUz: '—',
    recommendationRu: '—',
  },
  {
    code: 'Damage',
    nameUz: 'Zarar darajasi',
    nameRu: 'Уровень ущерба',
    descriptionUz:
      'Mazkur tahdid amalga oshirilishi natijasida yuzaga kelishi kutilayotgan moddiy, moliyaviy yoki boshqa nomoddiy zarar.',
    descriptionRu:
      'Материальный, финансовый или иной нематериальный ущерб, ожидаемый в результате реализации данной угрозы.',
    score0Uz: 'Kamroq',
    score0Ru: 'Меньше',
    score5Uz: 'Ko‘proq',
    score5Ru: 'Больше',
    recommendationUz: '—',
    recommendationRu: '—',
  },
];

export const DREAD_SCORING_NOTE = {
  uz: 'Har bir ko‘rsatkich 0 dan 5 ballgacha bo‘lgan qiymatga ega bo‘lishi mumkin. Tahdidning yakuniy darajasini aniqlash uchun barcha ko‘rsatkichlar bo‘yicha ballar yig‘indisi hisoblanadi va tahdid darajasi quyidagi jadvalga muvofiq belgilanadi.',
  ru: 'Каждый показатель может принимать значение от 0 до 5 баллов. Для определения итогового уровня угрозы вычисляется сумма баллов по всем показателям, и уровень угрозы определяется согласно таблице ниже.',
};

export const THREAT_LEVEL_HEADERS = {
  uz: { sum: 'Ko‘rsatkichlar yig‘indisi', level: 'Tahdid darajasi', rating: 'Reyting' },
  ru: { sum: 'Сумма показателей', level: 'Уровень угрозы', rating: 'Рейтинг' },
};

export interface ThreatLevelRow {
  range: string;
  levelUz: string;
  levelRu: string;
  rating: number;
}

/** Sum 0-25 -> level 1-5. Same thresholds the backend applies. */
export const THREAT_LEVEL_TABLE: ThreatLevelRow[] = [
  { range: '0~5', levelUz: 'Ahamiyatsiz', levelRu: 'Незначительный', rating: 1 },
  { range: '6~10', levelUz: 'Past', levelRu: 'Низкий', rating: 2 },
  { range: '11~15', levelUz: 'O‘rta', levelRu: 'Средний', rating: 3 },
  { range: '16~20', levelUz: 'Yuqori', levelRu: 'Высокий', rating: 4 },
  { range: '21~25', levelUz: 'Juda yuqori', levelRu: 'Очень высокий', rating: 5 },
];

export const PRINCIPLES_TITLE = {
  uz: 'Xavflarni sifat jihatidan baholash tamoyillari',
  ru: 'Принципы качественной оценки рисков',
};

/** Qualitative principles, from the legend on the Матрица рисков sheet (I2). */
export const ASSESSMENT_PRINCIPLES = {
  uz: [
    'Agar aktivning muhimlik darajasi yuqori yoki undan yuqori bo‘lsa va tahdid darajasi ham yuqori bo‘lsa, yoxud aksincha, tahdid darajasi yuqori yoki undan yuqori bo‘lsa hamda aktivning muhimlik darajasi yuqori bo‘lsa, xavf har doim kritik deb baholanadi;',
    'Muhimlik darajasi yuqori bo‘lgan aktivga nisbatan xavf ahamiyatsiz darajada bo‘lishi mumkin emas;',
    'Juda yuqori darajadagi tahdid natijasida yuzaga keladigan xavf o‘rta darajadan past bo‘lishi mumkin emas;',
    'Ahamiyatsiz darajadagi tahdid past darajadan yuqori bo‘lgan xavfni keltirib chiqarmaydi;',
    'Tahdidning o‘rta darajasi va aktivning o‘rta muhimlik darajasi o‘rta darajadagi xavfni yuzaga keltiradi.',
  ],
  ru: [
    'Если значимость актива высокая или выше, а уровень угрозы также высокий, либо наоборот — уровень угрозы высокий или выше, а значимость актива высокая, риск всегда оценивается как критический;',
    'Риск в отношении актива с высокой значимостью не может быть незначительным;',
    'Риск, возникающий вследствие угрозы очень высокого уровня, не может быть ниже среднего;',
    'Незначительная угроза не порождает риск выше низкого уровня;',
    'Средний уровень угрозы и средняя значимость актива дают средний уровень риска.',
  ],
};
