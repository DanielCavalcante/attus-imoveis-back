-- Catalogo de bairros por cidade.
--
-- Isto reverte, para UMA cidade, a decisao central da spec 19 ("nenhuma tabela
-- de bairros"). O argumento de la -- manter 5.570 municipios a mao -- continua
-- de pe para o Brasil inteiro, e por isso o catalogo e por cidade e OPCIONAL:
-- cidade sem catalogo mantem o distinct sobre os anuncios.
--
-- FONTE DA CARGA, e a ressalva que vem com ela:
--
--   Lista de bairros de Joao Pessoa, Wikipedia (pt), 64 bairros, consultada em
--   2026-09-29: https://pt.wikipedia.org/wiki/Lista_de_bairros_de_Joao_Pessoa
--
--   Nao foi possivel confirmar contra fonte municipal. A pagina de Regioes de
--   Participacao Popular da prefeitura mistura bairros com comunidades e
--   loteamentos (Mangabeira I a VII, Quadramares, Rangel), entao nao serve de
--   lista de bairros; e o Atlas Municipal em PDF excede o limite de download.
--   Uma busca indicou 65 bairros, divergindo em um da lista abaixo.
--
--   Nenhum nome foi inventado para fechar a diferenca: e melhor faltar um
--   bairro conferivel do que existir um que nao existe. Corrigir e editar esta
--   migracao -- ou melhor, acrescentar uma V8 com o que faltar, ja que migracao
--   aplicada nao se altera.

CREATE TABLE neighborhoods (
    id BIGSERIAL PRIMARY KEY,
    city_search VARCHAR(40) NOT NULL,
    name VARCHAR(60) NOT NULL,
    name_search VARCHAR(60) NOT NULL
);

-- A unicidade e sobre a forma normalizada: acento e caixa nao podem deixar
-- "Manaira" e "Manaira" entrarem como dois bairros diferentes.
ALTER TABLE neighborhoods
    ADD CONSTRAINT uk_neighborhood_city_name UNIQUE (city_search, name_search);

CREATE INDEX idx_neighborhoods_cidade ON neighborhoods (city_search);

INSERT INTO neighborhoods (city_search, name, name_search)
SELECT
    lower(unaccent('João Pessoa')),
    nome,
    lower(unaccent(nome))
FROM (VALUES
    ('Aeroclube'), ('Água Fria'), ('Altiplano'), ('Alto do Céu'),
    ('Alto do Mateus'), ('Anatólia'), ('Bancários'), ('Barra de Gramame'),
    ('Bessa'), ('Brisamar'), ('Cabo Branco'), ('Castelo Branco'),
    ('Centro'), ('Cidade dos Colibris'), ('Costa do Sol'), ('Costa e Silva'),
    ('Cristo Redentor'), ('Cruz das Armas'), ('Cuiá'), ('Distrito Industrial'),
    ('Ernâni Sátiro'), ('Ernesto Geisel'), ('Estados'), ('Expedicionários'),
    ('Funcionários'), ('Gramame'), ('Grotão'), ('Ilha do Bispo'),
    ('Indústrias'), ('Ipês'), ('Jaguaribe'), ('Jardim Cidade Universitária'),
    ('Jardim Oceania'), ('Jardim São Paulo'), ('Jardim Veneza'),
    ('João Agripino'), ('João Paulo II'), ('José Américo'), ('Manaíra'),
    ('Mandacaru'), ('Mangabeira'), ('Miramar'), ('Muçumagro'), ('Mumbaba'),
    ('Mussuré'), ('Oitizeiro'), ('Padre Zé'), ('Paratibe'), ('Pedro Gondim'),
    ('Penha'), ('Planalto da Boa Esperança'), ('Ponta do Seixas'),
    ('Portal do Sol'), ('Róger'), ('São José'), ('Tambaú'), ('Tambauzinho'),
    ('Tambiá'), ('Torre'), ('Treze de Maio'), ('Trincheiras'),
    ('Valentina de Figueiredo'), ('Varadouro'), ('Varjão')
) AS bairros(nome);
