-- As imagens do anuncio, uma linha por foto.
--
-- Ate aqui o anuncio tinha UMA imagem, na coluna `announcements.image`. Essa
-- coluna NAO sai: ela passa a guardar a capa, espelhada. E o que faz duas
-- coisas serem verdade ao mesmo tempo -- a listagem nao muda de forma, e o
-- anuncio gravado antes desta migracao continua tendo imagem sem precisar de
-- migracao de dados.
--
-- O espelho custa duas fontes para o mesmo fato, e quem paga esse custo e a
-- gravacao passar por um lugar so, no servico.

CREATE TABLE announcement_images (
    id BIGSERIAL PRIMARY KEY,
    announcement_id BIGINT NOT NULL REFERENCES announcements (id) ON DELETE CASCADE,
    -- O nome do objeto no bucket, nao o caminho: quem monta o caminho e o
    -- servico, e guardar o caminho pronto prenderia o banco ao formato da rota.
    object_name VARCHAR(120) NOT NULL,
    content_type VARCHAR(40) NOT NULL,
    -- Ordem em coluna, nao implicita pelo id: reordenar e caso normal, e ordem
    -- que depende da chave primaria nao se reordena sem reescrever linha.
    posicao INT NOT NULL,
    capa BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_announcement_images_anuncio ON announcement_images (announcement_id);

-- Uma capa por anuncio, garantida pelo banco: card, busca e compartilhamento
-- precisam de UMA imagem, e regra so na aplicacao perde para concorrencia.
CREATE UNIQUE INDEX uk_announcement_image_capa
    ON announcement_images (announcement_id)
    WHERE capa;

-- A posicao tambem nao se repete dentro do mesmo anuncio.
ALTER TABLE announcement_images
    ADD CONSTRAINT uk_announcement_image_posicao UNIQUE (announcement_id, posicao);
