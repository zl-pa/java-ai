CREATE TABLE IF NOT EXISTS document (
    id BINARY(16) NOT NULL,
    title VARCHAR(255) NOT NULL,
    parser_type VARCHAR(64) NOT NULL,
    content LONGTEXT NOT NULL,
    metadata_json JSON NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS document_chunk (
    id BINARY(16) NOT NULL,
    document_id BINARY(16) NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    qdrant_point_id VARCHAR(64) NOT NULL,
    metadata_json JSON NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_document_chunk_document_id (document_id),
    CONSTRAINT fk_document_chunk_document
        FOREIGN KEY (document_id) REFERENCES document(id)
        ON DELETE CASCADE
);
