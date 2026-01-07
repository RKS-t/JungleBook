"""
초기 학습 데이터 준비 스크립트
영어 데이터를 한국어로 번역하여 한국어 모델 학습용 데이터 생성
"""
import sys
import os
import json
from datasets import load_dataset

# 프로젝트 루트를 경로에 추가
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from app.models.translator import Translator
from config.settings import settings

def prepare_korean_training_data():
    """영어 데이터셋을 한국어로 번역하여 학습 데이터 준비"""
    translator = Translator()
    
    if not translator.enabled:
        print("❌ OpenAI API key가 없습니다. 번역을 수행할 수 없습니다.")
        return
    
    print("📥 HuggingFace 데이터셋 로드 중...")
    dataset = load_dataset("tasksource/logical-fallacy", split="train")
    
    # 샘플링 (전체 데이터가 많으므로 일부만 사용)
    sample_size = min(2000, len(dataset))
    dataset = dataset.select(range(sample_size))
    
    print(f"✅ {sample_size}개 샘플 선택")
    print("🔄 영어 → 한국어 번역 시작...")
    
    translated_data = []
    for i, item in enumerate(dataset):
        if i % 100 == 0:
            print(f"   진행 중: {i}/{sample_size} ({i*100//sample_size}%)")
        
        # 원본 데이터셋 구조에 맞게 수정
        # tasksource/logical-fallacy 데이터셋은 logical_fallacies 키 사용
        if "logical_fallacies" in item:
            # 논리 오류 타입 추출
            fallacies = item["logical_fallacies"]
            if isinstance(fallacies, list) and len(fallacies) > 0:
                label = fallacies[0]  # 첫 번째 논리 오류 타입 사용
            elif isinstance(fallacies, str) and fallacies.strip():
                label = fallacies.strip()
            else:
                label = "no_fallacy"
            
            # 텍스트 추출 (source_article 사용)
            english_text = item.get("source_article", "")
            if not english_text:
                english_text = item.get("text", item.get("argument", ""))
        else:
            # 기존 구조 지원
            english_text = item.get("text", item.get("argument", ""))
            label = item.get("label", item.get("logical_fallacy", "no_fallacy"))
        
        # 텍스트가 비어있으면 건너뛰기
        if not english_text or not english_text.strip():
            continue
        
        # 한국어로 번역 (실제 번역 결과만 사용)
        korean_text = translator.translate_to_korean(english_text, "en")
        
        # 번역 결과가 실제 번역인지 확인 (프롬프트 응답이 아닌지)
        if korean_text and len(korean_text) > 20 and "Please provide" not in korean_text and "번역" not in korean_text[:50]:
            translated_data.append({
                "text": korean_text,
                "label": label
            })
        else:
            if i < 5:  # 처음 몇 개만 경고 출력
                print(f"   ⚠️  번역 실패 또는 프롬프트 응답: 샘플 {i} 건너뜀")
    
    print(f"✅ 번역 완료: {len(translated_data)}개 샘플")
    
    # 결과 저장
    output_dir = "./data/korean_training"
    os.makedirs(output_dir, exist_ok=True)
    
    output_file = os.path.join(output_dir, "korean_training_data.json")
    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(translated_data, f, ensure_ascii=False, indent=2)
    
    print(f"💾 번역된 데이터 저장 완료: {output_file}")
    print(f"📊 라벨 분포:")
    
    # 라벨 분포 확인
    label_counts = {}
    for item in translated_data:
        label = item["label"]
        label_counts[label] = label_counts.get(label, 0) + 1
    
    for label, count in sorted(label_counts.items(), key=lambda x: x[1], reverse=True):
        print(f"   - {label}: {count}개")
    
    print("\n✅ 초기 학습 데이터 준비 완료!")
    print(f"📝 다음 단계: 이 데이터로 한국어 모델 학습을 진행하세요.")

if __name__ == "__main__":
    prepare_korean_training_data()

