from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
import torch

# 轻量本地向量服务：为 rag-service 提供 embedding API。
app = FastAPI()
_models = {}

# 预检测可用设备，优先使用 CUDA，否则使用 CPU
DEVICE = "cuda" if torch.cuda.is_available() else "cpu"
print(f"Using device: {DEVICE}")

class EmbeddingRequest(BaseModel):
    # 本地 embedding 模型名称，如 BAAI/bge-base-zh-v1.5
    model: str
    # 待向量化文本
    input: str

class EmbeddingResponse(BaseModel):
    # 向量结果
    embedding: list[float]

def _get_model(name: str) -> SentenceTransformer:
    # 简单缓存模型，避免重复加载导致请求变慢。
    if name not in _models:
        print(f"Loading model '{name}' on device '{DEVICE}'...")
        # 【关键修改】显式传入 device 参数，防止模型被加载到 meta 设备
        _models[name] = SentenceTransformer(name, device=DEVICE)
        print(f"Model '{name}' loaded successfully.")
    return _models[name]

@app.post("/embeddings", response_model=EmbeddingResponse)
def embeddings(req: EmbeddingRequest):
    model = _get_model(req.model)
    # encode 方法默认会使用模型初始化时指定的设备
    vector = model.encode(req.input, normalize_embeddings=True)
    return EmbeddingResponse(embedding=vector.tolist())