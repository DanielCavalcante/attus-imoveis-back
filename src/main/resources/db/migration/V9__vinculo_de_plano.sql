-- O vinculo entre empresa e plano.
--
-- Ate aqui os planos existiam e nao valiam para ninguem: a tabela `plans` era
-- catalogo sem assinante, e `temTeto` respondia "so PF" -- entao a empresa
-- anunciava sem limite nenhum, qualquer que fosse o plano que ela "tivesse".
--
-- Tabela propria, e nao uma coluna `plan_id` em `companies`, porque o vinculo
-- guarda QUEM liberou e QUANDO. Isso e historico, e historico nao cabe em
-- coluna que a proxima troca sobrescreve. `vigente` marca qual das linhas da
-- empresa vale agora; as outras ficam como trilha.

CREATE TABLE subscriptions (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    plan_id BIGINT NOT NULL REFERENCES plans (id),
    vigente BOOLEAN NOT NULL DEFAULT TRUE,
    -- E-mail e nao id, como em companies.reviewed_by: a trilha tem de
    -- continuar legivel se a conta interna que liberou sumir.
    liberado_por VARCHAR(80) NOT NULL,
    liberado_em TIMESTAMP NOT NULL
);

-- No maximo um plano vigente por empresa, e o banco e quem garante: duas
-- liberacoes simultaneas nao podem deixar a empresa com dois tetos, e regra
-- so na aplicacao perde para concorrencia.
CREATE UNIQUE INDEX uk_subscription_vigente
    ON subscriptions (company_id)
    WHERE vigente;

CREATE INDEX idx_subscriptions_company ON subscriptions (company_id);
