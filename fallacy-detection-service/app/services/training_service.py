import logging
from typing import List, Dict
from transformers import AutoTokenizer, AutoModelForSequenceClassification, Trainer, TrainingArguments
from datasets import Dataset
import torch
import json
import os

logger = logging.getLogger(__name__)

class TrainingService:
    def __init__(self, model_name: str = "monologg/koelectra-base-v3-discriminator"):
        self.model_name = model_name
        self.tokenizer = None
        self.model = None
    
    def prepare_training_data(self, training_data: List[Dict]) -> Dataset:
        """재학습 데이터 준비"""
        if not training_data:
            raise ValueError("Training data is empty")
        
        # 라벨 매핑 생성
        unique_labels = list(set([item["label"] for item in training_data]))
        label_to_id = {label: idx for idx, label in enumerate(unique_labels)}
        
        # 데이터 변환
        processed_data = []
        for item in training_data:
            processed_data.append({
                "text": item["text"],
                "labels": label_to_id[item["label"]]
            })
        
        dataset = Dataset.from_list(processed_data)
        
        return dataset, label_to_id
    
    def train_model(self, training_data: List[Dict], output_dir: str = "./models/fallacy_detector"):
        """모델 재학습"""
        try:
            logger.info(f"Starting retraining with {len(training_data)} samples")
            
            # 데이터 준비
            dataset, label_to_id = self.prepare_training_data(training_data)
            
            # 토크나이저 초기화
            tokenizer = AutoTokenizer.from_pretrained(self.model_name)
            
            # 토크나이징 (한국어 모델, max_length 512로 증가)
            tokenized_dataset = dataset.map(
                lambda examples: tokenizer(
                    examples["text"],
                    truncation=True,
                    padding="max_length",
                    max_length=512  # 한국어 모델은 512 토큰 지원
                ),
                batched=True
            )
            
            # 모델 초기화
            model = AutoModelForSequenceClassification.from_pretrained(
                self.model_name,
                num_labels=len(label_to_id)
            )
            
            # 학습 인수 (작은 데이터셋의 경우 더 적은 epoch로)
            num_samples = len(training_data)
            num_epochs = 3 if num_samples >= 100 else 1  # 작은 데이터셋은 1 epoch
            
            training_args = TrainingArguments(
                output_dir=output_dir,
                num_train_epochs=num_epochs,
                per_device_train_batch_size=min(16, num_samples),  # 데이터보다 큰 batch size 방지
                learning_rate=3e-5,
                weight_decay=0.01,
                logging_steps=max(1, num_samples // 10),  # 최소 1 step
                save_steps=1000,  # 작은 데이터셋에서는 저장 스킵
                eval_strategy="no",
                report_to="none",
                save_total_limit=1  # 모델 저장 공간 절약
            )
            
            # 트레이너 설정
            trainer = Trainer(
                model=model,
                args=training_args,
                train_dataset=tokenized_dataset,
                tokenizer=tokenizer
            )
            
            # 학습 실행
            trainer.train()
            trainer.save_model()
            
            # 라벨 매핑 저장
            id_to_label = {idx: label for label, idx in label_to_id.items()}
            os.makedirs(output_dir, exist_ok=True)
            with open(f"{output_dir}/label_mapping.json", "w", encoding="utf-8") as f:
                json.dump({
                    "label_to_id": label_to_id,
                    "id_to_label": id_to_label
                }, f, indent=2, ensure_ascii=False)
            
            logger.info(f"Model training completed: {output_dir}")
            return output_dir
            
        except Exception as e:
            logger.error(f"Training failed: {e}", exc_info=True)
            raise

