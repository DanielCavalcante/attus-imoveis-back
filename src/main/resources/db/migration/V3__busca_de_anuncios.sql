-- Busca de anuncios por titulo, cidade e estado.
--
-- A normalizacao (minuscula, sem acento) fica numa coluna propria em vez de ser
-- calculada na consulta. Duas razoes:
--
--   1. unaccent() e STABLE, e Postgres recusa funcao nao imutavel em expressao
--      de indice. Contornar exige um involucro IMMUTABLE, que e pegadinha
--      conhecida de quem mantem o banco depois.
--   2. Com coluna, a consulta continua em JPQL, e a ordenacao do Pageable segue
--      funcionando por nome de propriedade. Em consulta nativa o Spring monta o
--      ORDER BY com o nome cru e "createdAt" nao existe no SQL.
--
-- A coluna e mantida pela entidade em @PrePersist/@PreUpdate; aqui ela so nasce
-- e recebe a carga do que ja existe.

CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

ALTER TABLE announcements ADD COLUMN search_text TEXT;

UPDATE announcements
SET search_text = lower(unaccent(coalesce(title, '') || ' ' || coalesce(city, '') || ' ' || coalesce(state, '')));

-- GIN com trigrama porque a busca e ILIKE '%termo%': curinga a esquerda,
-- e indice B-tree nao serve para isso.
CREATE INDEX idx_announcements_busca
    ON announcements USING gin (search_text gin_trgm_ops);
