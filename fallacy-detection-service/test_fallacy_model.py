#!/usr/bin/env python3
"""
학습된 모델 테스트 스크립트
"""
import sys
import os
import json
import requests
from datetime import datetime

# 테스트 데이터 생성
topic_title = "가난과 사회복지"
topic_description = "가난한 사람들을 위한 사회복지 정책에 대한 토론"

test_cases = [
    # 오류 있는 논증 5개
    {
        "category": "논리 오류",
        "text": "가난한 사람들은 모두 게으르다. 내 친구가 가난했는데 열심히 일해서 부자가 되었으니, 모든 가난한 사람들도 그렇게 할 수 있다.",
        "expected": "논리 오류 탐지 (hasty_generalization 또는 ad_hominem)"
    },
    {
        "category": "논리 오류",
        "text": "만약 우리가 가난한 사람들에게 복지를 제공하면, 그들은 일하지 않고 계속 복지에 의존하게 될 것이다. 결국 우리 경제가 파탄날 것이다.",
        "expected": "논리 오류 탐지 (slippery_slope 또는 false_cause)"
    },
    {
        "category": "논리 오류",
        "text": "가난한 사람들을 돕자는 것은 사회주의다. 사회주의는 나쁜 것이므로 가난한 사람들을 돕는 것도 나쁘다.",
        "expected": "논리 오류 탐지 (straw_man 또는 false_dilemma)"
    },
    {
        "category": "논리 오류",
        "text": "가난한 아이들을 생각해보세요! 그들의 눈물을 보면 복지를 제공해야 한다는 것이 당연합니다.",
        "expected": "논리 오류 탐지 (appeal_to_emotion)"
    },
    {
        "category": "논리 오류",
        "text": "가난은 가난한 사람들이 나쁜 선택을 했기 때문에 발생한다. 따라서 가난한 사람들은 도움을 받을 자격이 없다.",
        "expected": "논리 오류 탐지 (false_cause 또는 ad_hominem)"
    },
    # 정상적인 논증 3개
    {
        "category": "정상 논증",
        "text": "가난은 복합적인 문제로 교육 기회 부족, 일자리 부족, 구조적 불평등 등 여러 요인이 복합적으로 작용합니다. 이러한 근본 원인들을 해결하기 위한 포괄적인 사회 프로그램과 경제 정책이 필요합니다.",
        "expected": "논리 오류 없음 (no_fallacy 또는 낮은 신뢰도)"
    },
    {
        "category": "정상 논증",
        "text": "연구 결과에 따르면, 어린 시절의 빈곤은 성인기의 건강, 교육, 소득에 장기적인 부정적 영향을 미칩니다. 따라서 조기 개입 프로그램을 통해 이러한 악순환을 끊는 것이 중요합니다.",
        "expected": "논리 오류 없음 (no_fallacy 또는 낮은 신뢰도)"
    },
    {
        "category": "정상 논증",
        "text": "사회복지 정책은 단기적으로는 재정 부담이 될 수 있지만, 장기적으로는 빈곤 감소를 통해 사회 안정과 경제 성장에 기여할 수 있습니다. 이는 여러 국가의 사례에서 입증되었습니다.",
        "expected": "논리 오류 없음 (no_fallacy 또는 낮은 신뢰도)"
    },
    # 주제와 어긋난 논증 2개
    {
        "category": "주제 어긋남",
        "text": "치킨은 프라이드 치킨이 가장 맛있다. 양념치킨은 너무 달고, 간장치킨은 짜다. 따라서 모든 사람은 프라이드 치킨을 먹어야 한다.",
        "expected": "주제 연관성 낮음 (topic_relevance < 0.3)"
    },
    {
        "category": "주제 어긋남",
        "text": "오늘 날씨가 정말 좋다. 이런 날씨에는 공원에 가서 산책하는 것이 최고다. 운동을 하면 건강에도 좋고 기분도 좋아진다.",
        "expected": "주제 연관성 낮음 (topic_relevance < 0.3)"
    }
]

def test_model():
    base_url = "http://localhost:8000/api/v1"
    
    print("=" * 70)
    print("학습된 모델 테스트")
    print("=" * 70)
    print(f"\n토픽: {topic_title}")
    print(f"설명: {topic_description}")
    print()
    
    results = []
    
    for i, test_case in enumerate(test_cases, 1):
        print(f"[테스트 {i}] {test_case['category']}: {test_case['text'][:50]}...")
        
        payload = {
            "text": test_case["text"],
            "language": "ko",
            "topic_title": topic_title,
            "topic_description": topic_description
        }
        
        try:
            response = requests.post(
                f"{base_url}/detect",
                json=payload,
                timeout=30
            )
            
            if response.status_code == 200:
                data = response.json()
                has_fallacy = data.get("has_fallacy", False)
                fallacy_type = data.get("fallacy_type", "N/A")
                confidence = data.get("confidence", 0.0)
                topic_relevance = data.get("topic_relevance", 0.0)
                
                result = {
                    "category": test_case["category"],
                    "text": test_case["text"][:50] + "...",
                    "has_fallacy": has_fallacy,
                    "fallacy_type": fallacy_type,
                    "confidence": confidence,
                    "topic_relevance": topic_relevance,
                    "expected": test_case["expected"]
                }
                results.append(result)
                
                print(f"  ✅ 논리 오류 탐지: {has_fallacy}")
                print(f"     타입: {fallacy_type}")
                print(f"     신뢰도: {confidence:.3f}")
                print(f"     주제 연관성: {topic_relevance:.3f}")
                print()
            else:
                print(f"  ❌ API 오류: {response.status_code}")
                print(f"     응답: {response.text}")
                print()
        except Exception as e:
            print(f"  ❌ 오류 발생: {e}")
            print()
    
    # 결과 요약
    print("=" * 70)
    print("테스트 결과 요약")
    print("=" * 70)
    print()
    
    fallacy_cases = [r for r in results if r["category"] == "논리 오류"]
    normal_cases = [r for r in results if r["category"] == "정상 논증"]
    off_topic_cases = [r for r in results if r["category"] == "주제 어긋남"]
    
    print("1. 논리 오류 논증 (5개):")
    for r in fallacy_cases:
        status = "✅" if r["has_fallacy"] else "❌"
        print(f"   {status} {r['fallacy_type']} (신뢰도: {r['confidence']:.3f}, 연관성: {r['topic_relevance']:.3f})")
    
    print("\n2. 정상 논증 (3개):")
    for r in normal_cases:
        status = "✅" if not r["has_fallacy"] or r["confidence"] < 0.3 else "❌"
        print(f"   {status} {r['fallacy_type']} (신뢰도: {r['confidence']:.3f}, 연관성: {r['topic_relevance']:.3f})")
    
    print("\n3. 주제 어긋난 논증 (2개):")
    for r in off_topic_cases:
        status = "✅" if r["topic_relevance"] < 0.3 else "❌"
        print(f"   {status} 연관성: {r['topic_relevance']:.3f} (논리 오류: {r['has_fallacy']}, 타입: {r['fallacy_type']})")
    
    print()
    print("=" * 70)

if __name__ == "__main__":
    test_model()

