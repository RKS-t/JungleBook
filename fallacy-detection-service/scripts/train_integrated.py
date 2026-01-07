#!/usr/bin/env python3
"""
통합 학습 스크립트
- 소스: MidhunKanadan/logical-fallacy-classification (HF)
- 소스: MAFALDA (공개 시 사용, 현재 비공개면 건너뜀)
- 라벨 매핑 → 우리 스키마
- 하이브리드 샘플링:
  - 적은 라벨: 오버샘플링(min_count)
  - 많은 라벨: 언더샘플링(max_count)
  - no_fallacy 부족 시: 합성 문장으로 보강
- (선택) 한국어 번역 후 저장
"""

import os
import sys
import json
import random
from collections import Counter, defaultdict
from typing import Dict, List, Tuple

sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from datasets import load_dataset, Dataset
from app.models.translator import Translator
from app.services.training_service import TrainingService
from config.settings import settings

# 우리 스키마
TARGET_LABELS = [
    "ad_hominem",
    "straw_man",
    "false_dilemma",
    "appeal_to_emotion",
    "circular_reasoning",
    "hasty_generalization",
    "false_cause",
    "bandwagon",
    "appeal_to_authority",
    "red_herring",
    "no_fallacy",
]

# 소스 라벨 → 우리 라벨 매핑
SOURCE_TO_TARGET = {
    # logical-fallacy-classification (예상 라벨)
    "ad hominem": "ad_hominem",
    "ad_hominem": "ad_hominem",
    "hasty generalization": "hasty_generalization",
    "hasty_generalization": "hasty_generalization",
    "appeal to emotion": "appeal_to_emotion",
    "appeal_to_emotion": "appeal_to_emotion",
    "appeal to authority": "appeal_to_authority",
    "appeal_to_authority": "appeal_to_authority",
    "appeal to popularity": "bandwagon",
    "appeal_to_popularity": "bandwagon",
    "bandwagon": "bandwagon",
    "false cause": "false_cause",
    "false_cause": "false_cause",
    "false dilemma": "false_dilemma",
    "false_dilemma": "false_dilemma",
    "straw man": "straw_man",
    "straw_man": "straw_man",
    "red herring": "red_herring",
    "red_herring": "red_herring",
    "circular reasoning": "circular_reasoning",
    "circular_reasoning": "circular_reasoning",
    "no fallacy": "no_fallacy",
    "no_fallacy": "no_fallacy",
    # MAFALDA 예상 라벨 → 스키마 매핑 (추정치)
    "faulty generalization": "hasty_generalization",
    "ad populum": "bandwagon",
    "false causality": "false_cause",
    "fallacy of logic": "red_herring",          # 포괄 라벨 → red_herring로 귀속
    "fallacy of relevance": "red_herring",
    "fallacy of extension": "straw_man",
    "fallacy of credibility": "appeal_to_authority",
    "equivocation": "red_herring",
    "intentional": "red_herring",
}

SYNTHETIC_NO_FALLACY = [
    "이 주장은 근거와 논리를 갖추고 있으며, 명확한 인과관계를 제시한다.",
    "서로 다른 관점을 균형 있게 제시하며, 감정적 호소 없이 논리를 전개한다.",
    "전제와 결론이 일관되고 순환 논증이나 허위 인과를 사용하지 않는다.",
    "자료와 통계를 인용하여 주장의 신뢰성을 뒷받침하며, 일반화 오류를 피한다.",
    "반례와 한계를 인정하며, 과도한 단정이나 인신공격을 하지 않는다.",
]


def load_hf_dataset(name: str, split: str = "train") -> List[Dict]:
    try:
        ds = load_dataset(name, split=split)
        return [dict(item) for item in ds]
    except Exception as e:
        print(f"⚠️  {name} 로드 실패: {e}")
        return []


def map_record(text: str, label: str) -> Tuple[str, str]:
    mapped = SOURCE_TO_TARGET.get(label.strip().lower(), None)
    return text, mapped


def build_dataset() -> List[Dict]:
    records = []

    # 1) logical-fallacy-classification (train/dev/test 모두 사용)
    for split in ["train", "validation", "test"]:
        data = load_hf_dataset("MidhunKanadan/logical-fallacy-classification", split=split)
        for item in data:
            text = item.get("text", "") or item.get("claim", "")
            label = item.get("label", "") or item.get("fallacy_type", "")
            text, mapped = map_record(text, label)
            if text and mapped:
                records.append({"text": text, "label": mapped})

    # 2) MAFALDA (공개 시)
    mafalda = load_hf_dataset("ChadiHelwe/MAFALDA", split="train")
    for item in mafalda:
        text = item.get("source_article", "") or item.get("text", "")
        label = ""
        lf = item.get("logical_fallacies", "")
        if isinstance(lf, list) and lf:
            label = lf[0]
        elif isinstance(lf, str):
            label = lf
        text, mapped = map_record(text, label)
        if text and mapped:
            records.append({"text": text, "label": mapped})

    return records


def hybrid_sample(records: List[Dict], min_count: int = 300, max_count: int = 800) -> List[Dict]:
    by_label = defaultdict(list)
    for r in records:
        by_label[r["label"]].append(r)

    augmented = []
    for label in TARGET_LABELS:
        items = by_label.get(label, [])
        cnt = len(items)

        # no_fallacy가 부족하면 합성 데이터로 보강
        if label == "no_fallacy" and cnt < min_count:
            needed = min_count - cnt
            for i in range(needed):
                augmented.append({"text": random.choice(SYNTHETIC_NO_FALLACY), "label": "no_fallacy"})
            cnt += needed

        if cnt == 0:
            continue
        if cnt < min_count:
            # 오버샘플링
            k = min_count - cnt
            sampled = random.choices(items, k=k)
            augmented.extend(items + sampled)
        elif cnt > max_count:
            # 언더샘플링
            sampled = random.sample(items, k=max_count)
            augmented.extend(sampled)
        else:
            augmented.extend(items)

    random.shuffle(augmented)
    return augmented


def translate_if_needed(records: List[Dict]) -> List[Dict]:
    if not settings.TRANSLATION_ENABLED:
        return records
    translator = Translator()
    if not translator.enabled:
        print("⚠️  번역 비활성화 또는 API 키 없음. 번역 없이 진행합니다.")
        return records

    translated = []
    for i, r in enumerate(records, 1):
        if i % 200 == 0:
            print(f"  번역 진행: {i}/{len(records)}")
        ko = translator.translate_to_korean(r["text"], "en")
        if ko:
            translated.append({"text": ko, "label": r["label"]})
    print(f"✅ 번역 완료: {len(translated)} / {len(records)}")
    return translated if translated else records


def save_dataset(records: List[Dict], path: str):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(records, f, ensure_ascii=False, indent=2)
    print(f"💾 저장: {path} (샘플 {len(records)}개)")


def main():
    print("=" * 70)
    print("통합 데이터 로드 및 매핑")
    print("=" * 70)

    records = build_dataset()
    if not records:
        print("⚠️  사용 가능한 공개 데이터가 없습니다. 학습을 중단합니다.")
        return
    print(f"원본 로드/매핑 후 샘플: {len(records)}개")

    # 라벨 통계
    cnt = Counter([r["label"] for r in records])
    print("라벨 분포(원본):")
    for k, v in cnt.most_common():
        print(f"  - {k}: {v}")

    # 하이브리드 샘플링
    sampled = hybrid_sample(records, min_count=300, max_count=800)
    cnt2 = Counter([r["label"] for r in sampled])
    print("\n라벨 분포(샘플링 후):")
    for k, v in cnt2.most_common():
        print(f"  - {k}: {v}")

    # 번역 (옵션)
    final_records = translate_if_needed(sampled)

    # 저장
    out_path = "./data/combined_training/combined_training_data.json"
    save_dataset(final_records, out_path)

    # 학습 실행
    print("\n모델 학습 시작...")
    trainer = TrainingService()
    trainer.train_model(final_records, output_dir="./models/korean_trained_model")
    print("✅ 모델 학습 완료")


if __name__ == "__main__":
    main()

