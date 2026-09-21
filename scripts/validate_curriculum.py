#!/usr/bin/env python3
"""Validate the bundled SPREVA A1-C1 curriculum using only the Python stdlib."""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONTENT = ROOT / "app" / "src" / "main" / "assets" / "content"
COURSE_FILE = CONTENT / "courses" / "de-core" / "course.json"
LESSONS_DIR = CONTENT / "courses" / "de-core" / "lessons"
MANIFEST_FILE = CONTENT / "manifest.json"
EXPECTED_LEVELS = ["A1", "A2", "B1", "B2", "C1"]
EXPECTED_UNIT_COUNTS = {"A1": 14, "A2": 14, "B1": 14, "B2": 15, "C1": 16}
REQUIRED_LANGS = ("de", "ar", "en")


def load_json(path: Path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        raise ValueError(f"{path.relative_to(ROOT)}: invalid JSON: {exc}") from exc


def require_localized(value, where: str, errors: list[str]):
    if not isinstance(value, dict):
        errors.append(f"{where}: localized value must be an object")
        return
    for lang in REQUIRED_LANGS:
        text = value.get(lang)
        if not isinstance(text, str) or not text.strip():
            errors.append(f"{where}: missing/non-empty '{lang}' text")


def validate():
    errors: list[str] = []
    course = load_json(COURSE_FILE)
    manifest = load_json(MANIFEST_FILE)

    levels = course.get("levels", [])
    cefr = [level.get("cefr") for level in levels]
    if cefr != EXPECTED_LEVELS:
        errors.append(f"course levels must be exactly {EXPECTED_LEVELS}, got {cefr}")

    packages = {p.get("level") for p in manifest.get("packages", [])}
    if set(EXPECTED_LEVELS) - packages:
        errors.append(f"manifest missing packages for: {sorted(set(EXPECTED_LEVELS) - packages)}")

    seen_levels, seen_units, seen_lessons = set(), set(), set()
    refs: dict[str, dict] = {}

    for level in levels:
        level_id = level.get("id")
        expected_units = EXPECTED_UNIT_COUNTS.get(level.get("cefr"))
        if expected_units is not None and len(level.get("units", [])) != expected_units:
            errors.append(
                f"level {level.get('cefr')} must contain {expected_units} units, "
                f"got {len(level.get('units', []))}"
            )
        if not level_id or level_id in seen_levels:
            errors.append(f"invalid/duplicate level id: {level_id!r}")
        seen_levels.add(level_id)
        require_localized(level.get("title"), f"level {level_id} title", errors)

        for unit in level.get("units", []):
            unit_id = unit.get("id")
            if not unit_id or unit_id in seen_units:
                errors.append(f"invalid/duplicate unit id: {unit_id!r}")
            seen_units.add(unit_id)
            require_localized(unit.get("title"), f"unit {unit_id} title", errors)

            lessons = unit.get("lessons", [])
            if len(lessons) != 4:
                errors.append(f"unit {unit_id} must contain exactly 4 lessons (foundation, real-life, mastery, casebook), got {len(lessons)}")
            expected_suffixes = ["_l01", "_l02", "_l03", "_l04"]
            actual_ids = [ref.get("id", "") for ref in lessons]
            for suffix in expected_suffixes:
                if not any(lesson_id.endswith(suffix) for lesson_id in actual_ids):
                    errors.append(f"unit {unit_id} missing required lesson layer {suffix}")
            for ref in lessons:
                lesson_id = ref.get("id")
                if not lesson_id or lesson_id in seen_lessons:
                    errors.append(f"invalid/duplicate lesson id: {lesson_id!r}")
                    continue
                seen_lessons.add(lesson_id)
                require_localized(ref.get("title"), f"lesson ref {lesson_id} title", errors)
                if not isinstance(ref.get("activityCount"), int) or ref["activityCount"] <= 0:
                    errors.append(f"lesson ref {lesson_id}: invalid activityCount")
                refs[lesson_id] = {"unitId": unit_id, "ref": ref}

    disk_files = {p.stem: p for p in LESSONS_DIR.glob("*.json")}
    missing = sorted(set(refs) - set(disk_files))
    extra = sorted(set(disk_files) - set(refs))
    if missing:
        errors.append(f"referenced lesson files missing: {missing}")
    if extra:
        errors.append(f"unreferenced lesson files present: {extra}")

    total_activities = 0
    seen_activity_ids: set[str] = set()
    seen_vocab_ids: set[str] = set()

    for lesson_id, meta in refs.items():
        path = disk_files.get(lesson_id)
        if not path:
            continue
        lesson = load_json(path)
        if lesson.get("id") != lesson_id:
            errors.append(f"{lesson_id}: file id {lesson.get('id')!r} does not match filename/index")
        if lesson.get("unitId") != meta["unitId"]:
            errors.append(f"{lesson_id}: unitId {lesson.get('unitId')!r} != {meta['unitId']!r}")
        require_localized(lesson.get("title"), f"lesson {lesson_id} title", errors)
        if lesson.get("title") != meta["ref"].get("title"):
            errors.append(f"{lesson_id}: indexed title differs from lesson title")

        can_do = lesson.get("canDo", [])
        if not can_do or any(not isinstance(x, str) or not x.strip() for x in can_do):
            errors.append(f"{lesson_id}: missing/invalid Can-Do outcomes")

        activities = lesson.get("activities", [])
        if len(activities) != meta["ref"].get("activityCount"):
            errors.append(
                f"{lesson_id}: activityCount={meta['ref'].get('activityCount')} but file has {len(activities)}"
            )
        if not activities:
            errors.append(f"{lesson_id}: has no activities")
            continue
        total_activities += len(activities)

        for activity in activities:
            aid = activity.get("id")
            if not aid or aid in seen_activity_ids:
                errors.append(f"{lesson_id}: invalid/duplicate activity id {aid!r}")
            else:
                seen_activity_ids.add(aid)

            kind = activity.get("type")
            if kind == "text_intro":
                require_localized(activity.get("title"), f"{aid} title", errors)
                require_localized(activity.get("body"), f"{aid} body", errors)
            elif kind == "vocab_intro":
                words = activity.get("words", [])
                if not words:
                    errors.append(f"{aid}: empty vocabulary list")
                for word in words:
                    wid = word.get("id")
                    if not wid or wid in seen_vocab_ids:
                        errors.append(f"{aid}: invalid/duplicate vocabulary id {wid!r}")
                    else:
                        seen_vocab_ids.add(wid)
                    if not isinstance(word.get("german"), str) or not word["german"].strip():
                        errors.append(f"{aid}: vocabulary item {wid!r} missing German form")
                    require_localized(word.get("translation"), f"{aid}/{wid} translation", errors)
            elif kind == "multiple_choice":
                options = activity.get("options", [])
                ids = [o.get("id") for o in options]
                if len(options) < 2:
                    errors.append(f"{aid}: multiple_choice needs at least 2 options")
                if activity.get("correctOptionId") not in ids:
                    errors.append(f"{aid}: correctOptionId not found in options")
                require_localized(activity.get("prompt"), f"{aid} prompt", errors)
                require_localized(activity.get("question"), f"{aid} question", errors)
                for option in options:
                    require_localized(option.get("text"), f"{aid}/{option.get('id')} text", errors)
            elif kind == "cloze":
                template = activity.get("sentenceTemplate")
                require_localized(template, f"{aid} sentenceTemplate", errors)
                placeholder = activity.get("answerPlaceholder", "{answer}")
                if isinstance(template, dict) and placeholder not in template.get("de", ""):
                    errors.append(f"{aid}: German cloze template lacks placeholder {placeholder!r}")
                answers = activity.get("acceptedAnswers", [])
                if not answers or any(not isinstance(x, str) or not x.strip() for x in answers):
                    errors.append(f"{aid}: cloze needs non-empty acceptedAnswers")
            elif kind == "lesson_summary":
                require_localized(activity.get("title"), f"{aid} title", errors)
                if activity.get("canDo") != can_do:
                    errors.append(f"{aid}: lesson_summary Can-Do list differs from lesson")
            elif kind in {"listening_choice", "speaking_repeat"}:
                # Existing renderer-supported audio activities remain valid; media integrity is checked separately.
                if not activity.get("audio"):
                    errors.append(f"{aid}: {kind} missing audio")
            else:
                errors.append(f"{aid}: unsupported activity type {kind!r}")

        if activities[-1].get("type") != "lesson_summary":
            errors.append(f"{lesson_id}: final activity must be lesson_summary")

        type_counts = {}
        for activity in activities:
            kind = activity.get("type")
            type_counts[kind] = type_counts.get(kind, 0) + 1

        if lesson_id.endswith("_l03"):
            if len(activities) < 14:
                errors.append(f"{lesson_id}: mastery layer must have at least 14 activities")
            if type_counts.get("cloze", 0) < 6 or type_counts.get("multiple_choice", 0) < 2:
                errors.append(f"{lesson_id}: mastery layer needs >=6 cloze and >=2 multiple-choice drills")
        if lesson_id.endswith("_l04"):
            if len(activities) != 16:
                errors.append(f"{lesson_id}: casebook layer must have exactly 16 activities")
            if type_counts.get("cloze", 0) != 6 or type_counts.get("multiple_choice", 0) != 4:
                errors.append(f"{lesson_id}: casebook layer must have exactly 6 cloze and 4 multiple-choice drills")

    if errors:
        print(f"Curriculum validation FAILED with {len(errors)} error(s):", file=sys.stderr)
        for error in errors:
            print(f" - {error}", file=sys.stderr)
        return 1

    print(
        "Curriculum validation OK: "
        f"{len(levels)} levels, {len(seen_units)} units, "
        f"{len(seen_lessons)} lessons, {total_activities} activities, "
        f"{len(seen_vocab_ids)} vocabulary items."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(validate())
