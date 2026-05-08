"""
processing/translation_validator.py

Постпроцессор: валидирует результат перевода после каждого чанка.
Ловит незапереведённые слова, потерянные строки, пустые строки.
"""

import re
import logging
from dataclasses import dataclass, field
from typing import List, Dict, Optional, Set

logger = logging.getLogger(__name__)


class AlignmentError(Exception):
    """Количество строк на входе и выходе не совпадает."""


class ValidationError(Exception):
    """Перевод содержит исправимые проблемы — нужен retry."""

    def __init__(self, message: str, issues: List[str]):
        super().__init__(message)
        self.issues = issues


@dataclass
class ValidationResult:
    is_valid: bool
    issues: List[str] = field(default_factory=list)
    flagged_lines: Dict[int, str] = field(default_factory=dict)  # index → issue


# Слова, которые разрешено оставлять на английском (бренды, имена и т.д.)
DEFAULT_ALLOWED_ENGLISH: Set[str] = {
    # Технические маркеры субтитров
    "WEBVTT",
    # Распространённые бренды, которые не переводят
    "OK", "ok",
}

# Минимальная длина английского слова, чтобы считать его "незапереведённым"
MIN_ENGLISH_WORD_LEN = 4

# Паттерн: последовательность латинских букв (слово)
_ENGLISH_WORD_RE = re.compile(r"\b([a-zA-Z]{%d,})\b" % MIN_ENGLISH_WORD_LEN)


class TranslationValidator:
    """
    Запускается после translate_chunk().

    Использование:
        validator = TranslationValidator(glossary={"Jules": "Джулс"})
        result = validator.validate(original_lines, translated_lines)
        if not result.is_valid:
            # retry или логирование
    """

    def __init__(
            self,
            glossary: Optional[Dict[str, str]] = None,
            extra_allowed: Optional[Set[str]] = None,
    ):
        # Собираем множество разрешённых английских слов из глоссария
        self._allowed: Set[str] = set(DEFAULT_ALLOWED_ENGLISH)
        if glossary:
            for src, tgt in glossary.items():
                # Если оригинальное слово в глоссарии — разрешаем его в выводе
                for word in src.split():
                    self._allowed.add(word)
                # Если перевод содержит латиницу (бренд) — тоже разрешаем
                for word in tgt.split():
                    if re.match(r"[a-zA-Z]+", word):
                        self._allowed.add(word)
        if extra_allowed:
            self._allowed |= extra_allowed

    def validate(
            self,
            original_lines: List[str],
            translated_lines: List[str],
    ) -> ValidationResult:
        issues: List[str] = []
        flagged: Dict[int, str] = {}

        # ── 1. Количество строк ──────────────────────────────────────────────
        if len(original_lines) != len(translated_lines):
            raise AlignmentError(
                f"Line count mismatch: original={len(original_lines)}, "
                f"translated={len(translated_lines)}"
            )

        for i, (orig, trans) in enumerate(zip(original_lines, translated_lines)):

            # ── 2. Пустые строки ────────────────────────────────────────────
            if not trans.strip():
                issue = f"Line {i}: empty translation (original: {orig!r})"
                issues.append(issue)
                flagged[i] = "empty"
                logger.warning(issue)
                continue

            # ── 3. Незапереведённые английские слова ────────────────────────
            untranslated = self._find_untranslated(trans)
            if untranslated:
                issue = (
                    f"Line {i}: untranslated English word(s) {untranslated} "
                    f"in: {trans!r}"
                )
                issues.append(issue)
                flagged[i] = f"untranslated: {untranslated}"
                logger.warning(issue)

            # ── 4. Строка полностью совпадает с оригиналом ──────────────────
            # (модель просто скопировала без перевода)
            if trans.strip() == orig.strip() and self._is_translatable(orig):
                issue = f"Line {i}: translation identical to original: {orig!r}"
                issues.append(issue)
                flagged[i] = "not translated"
                logger.warning(issue)

        is_valid = len(issues) == 0
        return ValidationResult(is_valid=is_valid, issues=issues, flagged_lines=flagged)

    def _find_untranslated(self, text: str) -> List[str]:
        """Возвращает английские слова в переводе, которые не входят в allowed."""
        matches = _ENGLISH_WORD_RE.findall(text)
        return [w for w in matches if w not in self._allowed]

    @staticmethod
    def _is_translatable(text: str) -> bool:
        """Проверяет, что строка содержит хоть что-то для перевода (не просто знаки)."""
        return bool(re.search(r"[a-zA-Zа-яА-ЯёЁ]", text))

    def build_fix_prompt_issues(self, result: ValidationResult) -> str:
        """Формирует описание проблем для retry-промпта."""
        lines = []
        for i, issue_type in result.flagged_lines.items():
            lines.append(f"  - Line index {i}: {issue_type}")
        return "\n".join(lines)