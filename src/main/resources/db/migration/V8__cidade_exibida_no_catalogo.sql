-- A cidade do catalogo passa a guardar tambem a forma exibida.
--
-- A V7 guardou so `city_search` ("joao pessoa"), que basta para PROCURAR o
-- catalogo mas nao para OFERECE-LO: a lista de cidades da tela precisa do nome
-- como se escreve, com acento e caixa. Sem esta coluna a unica saida seria
-- reconstruir "João Pessoa" a partir de "joao pessoa", que nao tem volta.
--
-- Sem esta coluna, /announcements/cidades so conseguia listar cidades que ja
-- tem anuncio -- e o produto comeca em Joao Pessoa, que nao tinha nenhum. Quem
-- abria o filtro nao encontrava a propria cidade do produto.

ALTER TABLE neighborhoods ADD COLUMN city VARCHAR(40);

UPDATE neighborhoods
   SET city = 'João Pessoa'
 WHERE city_search = lower(unaccent('João Pessoa'));

-- Qualquer linha que tenha entrado por outro caminho fica com a forma
-- normalizada mesmo: e feia, mas e verdadeira, e a coluna pode ser NOT NULL.
UPDATE neighborhoods SET city = city_search WHERE city IS NULL;

ALTER TABLE neighborhoods ALTER COLUMN city SET NOT NULL;
