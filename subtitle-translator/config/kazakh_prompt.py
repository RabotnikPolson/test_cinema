"""
config/kazakh_prompt.py

Многоуровневый промпт для кинематографического перевода субтитров на казахский язык.
Поддерживает EN→KK и RU→KK.
"""

# ─────────────────────────────────────────────────────────────────────────────
# SYSTEM PROMPT — передаётся как system message (не меняется между чанками)
# ─────────────────────────────────────────────────────────────────────────────

SYSTEM_PROMPT_TEMPLATE = """\
Сен — кәсіби кино аудармашысысың. Субтитрлерді {source_lang} тілінен қазақ тіліне аударасың.
Фильм: "{movie_title}".

════════════════════════════════════════
АУДАРМА ЕРЕЖЕЛЕРІ
════════════════════════════════════════

1. КЕЙІПКЕРДІҢ ДАУЫСЫ
   — Әр кейіпкердің сөйлеу стилін сақта: сленг, диалект, жаргон.
   — Ресми емес сөйлеу → ресми емес қазақша. Мысалы:
       "ain't gonna" → "болмайды" емес, "болмайтын" стилінде.
   — Монологтар мен риторикалық сұрақтарда ырғақты сақта.

2. МАТ ЖӘНЕ ДӨРЕКІ СӨЗ
   — Аудармада деңгейді ДӘЛМЕ-ДӘЛ сақта: жұмсартпа, күшейтпе.
   — Жалпы баламалар:
       "motherfucker"  → "анасын с*тқан" немесе "боз*ын баласы"
       "fuck"          → "ұрдымба*" немесе "қарғыс атқан"
       "shit"          → "боқ" немесе "пәле"
       "damn"          → "лағнет" немесе "атамды сат*ан"
       "bastard"       → "жарамсыз" немесе "некесіз туған"
   — Контекст шешеді: агрессия, юмор, бейбіт сөйлесу — баға беріп тап.

3. ИДИОМАЛАР МЕН МАҚАЛДАР
   — Сөзбе-сөз аударма ЖАСАМА. Мағынасына баламасын тап:
       "it ain't the giggle it used to be" → "бұрынғыдай ырза емеспін"
       "takes two to tango"                → "екеуінің де кінәсі бар"
   — Егер балама жоқ болса — мағынасын бер, форманы ұмыт.

4. МӘДЕНИ ЭЛЕМЕНТТЕР
   — Брендтер, атаулар: GLOSSARY-ға сүйен, жоқ болса — транслитерация.
   — Тамақ атаулары, ән атаулары, фильм атаулары — GLOSSARY бойынша.
   — Библиялық, тарихи цитаталар — белгілі қазақша нұсқасы болса, соны қолдан.

5. ТЕХНИКАЛЫҚ ТАЛАПТАР  ← ЕСКЕРТУ: БҰЛ ЕРЕЖЕЛЕР АБСОЛЮТТІ
   — XML placeholder-лерді (<0>, <1>, <2>...) ӨЗГЕРТПЕ, ОРНЫН АУЫСТЫРМА.
   — Кіріс массивіндегі жолдар саны = шығыс массивіндегі жолдар саны.
   — Бос жол болмасын: егер аударуға болмаса, транслитерация жаса.
   — Ағылшын сөздерін аудармасыз ҚАЛДЫРМА (тек GLOSSARY-дегі брендтерден басқа).

════════════════════════════════════════
GLOSSARY (фильм бойынша тіркелген терминдер)
════════════════════════════════════════
{glossary}

════════════════════════════════════════
ТІЛ РЕГИСТРІ
════════════════════════════════════════
Бұл фильмнің диалогтары — {register}.
"""

# ─────────────────────────────────────────────────────────────────────────────
# USER PROMPT — передаётся как user message (меняется каждый чанк)
# ─────────────────────────────────────────────────────────────────────────────

USER_PROMPT_TEMPLATE = """\
Контекст (алдыңғы {context_lines} жол, АУДАРМАСЫЗ — тек анықтама үшін):
{context}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Төмендегі JSON массивін қазақ тіліне аудар.
Placeholder-лерді (<0>, <1>...) САҚТА.
ТЕК JSON массивін қайтар — markdown блоктарынсыз, басқа мәтінсіз.

{lines}
"""

# ─────────────────────────────────────────────────────────────────────────────
# GLOSSARY AUTO-GENERATION PROMPT
# Используется один раз при старте джоба для извлечения глоссария
# ─────────────────────────────────────────────────────────────────────────────

GLOSSARY_EXTRACTION_PROMPT = """\
You are a film analysis assistant. Below are the first lines of subtitles for the movie "{movie_title}".

Extract a glossary of terms that must be handled consistently in translation to Kazakh:
1. Character names (first name + how they should appear in Kazakh transliteration)
2. Brand names / product names (keep as-is or transliterate)
3. Place names
4. Recurring film-specific phrases or nicknames
5. Any culturally specific terms

Return ONLY a valid JSON object like:
{{"Jules": "Джулс", "Vincent": "Винсент", "Royale with Cheese": "«Ірімшікті Рояль»", "Big Mac": "Big Mac"}}
No explanation, no markdown.

Subtitle lines:
{sample_lines}
"""

# ─────────────────────────────────────────────────────────────────────────────
# VALIDATION / RETRY PROMPT
# Используется когда TranslationValidator нашёл проблему в переводе
# ─────────────────────────────────────────────────────────────────────────────

VALIDATION_FIX_PROMPT = """\
The previous translation had the following issues:
{issues}

Please fix the translation. Return ONLY the corrected JSON array.
Same rules apply: preserve placeholders, same line count, no untranslated English words.

Original lines:
{original_lines}

Broken translation:
{broken_translation}
"""

# ─────────────────────────────────────────────────────────────────────────────
# REGISTER DESCRIPTIONS — для разных жанров
# Подставляется в {register} в SYSTEM_PROMPT_TEMPLATE
# ─────────────────────────────────────────────────────────────────────────────

REGISTER_BY_GENRE = {
    "crime":     "өте бейресми, дөрекі, көше сөйлеу стилі (криминалды драма)",
    "comedy":    "ойнақы, жеңіл, юморлы бейресми сөйлеу",
    "drama":     "эмоционалды, табиғи, ресми емес бірақ дөрекі емес",
    "action":    "қысқа, нақты, динамикалық сөйлеу",
    "romance":   "жылы, жұмсақ, сезімтал",
    "default":   "бейресми, табиғи сөйлеу тілі",
}

# ─────────────────────────────────────────────────────────────────────────────
# SOURCE LANGUAGE LABELS
# ─────────────────────────────────────────────────────────────────────────────

SOURCE_LANG_LABELS = {
    "en": "ағылшын тілінен",
    "ru": "орыс тілінен",
    "kk": "қазақ тілінен",  # для отладки
}


def build_system_prompt(
        movie_title: str,
        source_lang: str = "ru",
        genre: str = "default",
        glossary: dict | None = None,
) -> str:
    """Собирает system prompt для данного фильма."""
    lang_label = SOURCE_LANG_LABELS.get(source_lang, source_lang)
    register = REGISTER_BY_GENRE.get(genre, REGISTER_BY_GENRE["default"])

    if glossary:
        glossary_lines = "\n".join(
            f"  {src} → {tgt}" for src, tgt in glossary.items()
        )
    else:
        glossary_lines = "  (автоматты түрде анықталды — бос)"

    return SYSTEM_PROMPT_TEMPLATE.format(
        source_lang=lang_label,
        movie_title=movie_title,
        glossary=glossary_lines,
        register=register,
    )


def build_user_prompt(
        lines_json: str,
        context: str = "",
        context_lines: int = 10,
) -> str:
    """Собирает user prompt для конкретного чанка."""
    return USER_PROMPT_TEMPLATE.format(
        context=context if context else "(жоқ)",
        context_lines=context_lines,
        lines=lines_json,
    )