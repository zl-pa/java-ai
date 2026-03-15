from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

app = FastAPI()
_models = {}


class EmbeddingRequest(BaseModel):
    model: str
    input: str


class EmbeddingResponse(BaseModel):
    embedding: list[float]


def _get_model(name: str) -> SentenceTransformer:
    if name not in _models:
        _models[name] = SentenceTransformer(name)
    return _models[name]


@app.post("/embeddings", response_model=EmbeddingResponse)
def embeddings(req: EmbeddingRequest):
    model = _get_model(req.model)
    vector = model.encode(req.input, normalize_embeddings=True)
    return EmbeddingResponse(embedding=vector.tolist())
