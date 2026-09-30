-- Bairro do anuncio.
--
-- Nao existe tabela de bairros, e e isto que faz a coisa escalar: a lista de
-- bairros de uma cidade e um distinct sobre os anuncios daquela cidade. O
-- sistema sabe os bairros de Joao Pessoa porque e la que ha anuncio, e sabe os
-- de Belo Horizonte no dia em que alguem publicar la -- sem ninguem cadastrar
-- nada. Tabela de cadastro exigiria manter 5.570 municipios a mao.
--
-- As colunas nascem nulas de proposito: o bairro dos anuncios ja publicados e
-- resolvido pela rotina de carga, fora daqui, porque Flyway nao faz chamada
-- HTTP. Anuncio sem bairro simplesmente nao responde a filtro de bairro.

ALTER TABLE announcements ADD COLUMN neighborhood VARCHAR(60);
ALTER TABLE announcements ADD COLUMN neighborhood_search VARCHAR(60);
