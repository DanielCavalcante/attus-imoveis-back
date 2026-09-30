-- Estado do anuncio: no ar ou fora do ar.
--
-- Inativar nao e excluir, e o produto precisa dos dois. Inativar tira do ar e
-- devolve a vaga da quota, preservando o anuncio para voltar depois -- e o caso
-- de quem vendeu e pode voltar a vender. Excluir e definitivo.
--
-- O padrao e ativo e a carga deixa ativo o que ja esta publicado: nenhum
-- anuncio existente pode sair do ar por causa desta migracao.

ALTER TABLE announcements ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE announcements SET active = TRUE WHERE active IS NULL;
