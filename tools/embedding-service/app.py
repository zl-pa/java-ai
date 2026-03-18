from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

# 轻量本地向量服务：为 rag-service 提供 embedding API。
app = FastAPI()
_models = {}


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
        _models[name] = SentenceTransformer(name)
    return _models[name]


@app.post("/embeddings", response_model=EmbeddingResponse)
def embeddings(req: EmbeddingRequest):
    model = _get_model(req.model)
    vector = model.encode(req.input, normalize_embeddings=True)
    return EmbeddingResponse(embedding=vector.tolist())
