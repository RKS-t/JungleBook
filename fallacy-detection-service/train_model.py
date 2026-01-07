#!/usr/bin/env python3
"""
번역된 한국어 학습 데이터로 모델 학습 스크립트
"""
import sys
import os
import json
import time
from datetime import datetime

# 프로젝트 루트를 경로에 추가
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from app.services.training_service import TrainingService

def main():
    print("=" * 60)
    print("논리 오류 탐지 모델 학습 시작")
    print("=" * 60)
    print()
    
    # 학습 데이터 로드
    data_path = "./data/korean_training/korean_training_data.json"
    if not os.path.exists(data_path):
        print(f"❌ 학습 데이터 파일을 찾을 수 없습니다: {data_path}")
        return
    
    print(f"📂 학습 데이터 로드 중: {data_path}")
    with open(data_path, "r", encoding="utf-8") as f:
        training_data = json.load(f)
    
    print(f"✅ {len(training_data)}개 샘플 로드 완료")
    
    # 라벨 분포 확인
    from collections import Counter
    labels = Counter([item["label"] for item in training_data])
    print(f"\n📊 라벨 분포:")
    for label, count in labels.most_common():
        print(f"   - {label}: {count}개")
    
    # 학습 서비스 초기화
    print(f"\n🤖 학습 서비스 초기화 중...")
    training_service = TrainingService()
    
    # 출력 디렉토리 설정
    output_dir = "./models/korean_trained_model"
    os.makedirs(output_dir, exist_ok=True)
    
    # 학습 시작
    print(f"\n🚀 학습 시작 시간: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"📁 모델 저장 경로: {output_dir}")
    print()
    
    start_time = time.time()
    
    try:
        model_path = training_service.train_model(training_data, output_dir)
        
        elapsed_time = time.time() - start_time
        minutes = int(elapsed_time // 60)
        seconds = int(elapsed_time % 60)
        
        print()
        print("=" * 60)
        print("✅ 학습 완료!")
        print("=" * 60)
        print(f"⏱️  소요 시간: {minutes}분 {seconds}초")
        print(f"📁 모델 경로: {model_path}")
        print(f"📊 학습 데이터: {len(training_data)}개 샘플")
        print()
        print("다음 단계:")
        print(f"1. .env 파일에서 FALLACY_MODEL_PATH를 '{model_path}'로 설정")
        print("2. Python 서비스를 재시작하여 새 모델 로드")
        print("=" * 60)
        
    except Exception as e:
        elapsed_time = time.time() - start_time
        print()
        print("=" * 60)
        print("❌ 학습 실패")
        print("=" * 60)
        print(f"오류: {e}")
        print(f"소요 시간: {int(elapsed_time // 60)}분 {int(elapsed_time % 60)}초")
        import traceback
        traceback.print_exc()
        print("=" * 60)

if __name__ == "__main__":
    main()

