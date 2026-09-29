-- Filtro por cidade.
--
-- A coluna search_text da V3 nao serve aqui: ela concatena titulo, cidade e
-- estado, e filtrar cidade contra ela faria um anuncio chamado "Casa em
-- Recife" localizado na Paraiba aparecer como se fosse de Recife.
--
-- Mesma disciplina da V3: a normalizacao mora numa coluna, mantida pela
-- entidade em @PrePersist/@PreUpdate, e aqui ela so nasce e recebe a carga do
-- que ja existe. Assim a consulta continua em JPQL e o ORDER BY do Pageable
-- segue funcionando por nome de propriedade.

ALTER TABLE announcements ADD COLUMN city_search VARCHAR(40);

UPDATE announcements
SET city_search = lower(unaccent(coalesce(city, '')));

-- Sem indice: a comparacao e por igualdade e o acervo nao tem volume que
-- justifique. Indice sem medicao e adivinhacao.
