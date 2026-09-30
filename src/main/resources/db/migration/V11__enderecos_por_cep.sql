-- O que ja foi resolvido, para nao perguntar duas vezes.
--
-- CEP nao muda de bairro: o que um provedor respondeu uma vez vale para
-- sempre. Isso transforma cada consulta bem-sucedida numa que nunca mais
-- precisa sair daqui -- e, mais importante, numa que continua respondendo
-- quando TODOS os provedores estao fora. Foi o ViaCEP cair que fez este
-- problema aparecer.
--
-- Tabela e nao memoria: memoria morre com o conteiner, e com ela morre
-- justamente a unica resposta que sobrava no cenario que motivou isto.

CREATE TABLE enderecos (
    -- So digitos, sem hifen: e a forma normalizada com que se procura.
    cep VARCHAR(8) PRIMARY KEY,
    logradouro VARCHAR(120),
    -- Bairro pode ser nulo de verdade: cidade com CEP unico nao tem.
    bairro VARCHAR(60),
    cidade VARCHAR(60) NOT NULL,
    uf VARCHAR(2) NOT NULL,
    -- Qual provedor respondeu. Nao e enfeite: quando um deles passar a errar,
    -- e por aqui que se descobre o que veio de onde.
    provedor VARCHAR(40) NOT NULL,
    resolvido_em TIMESTAMP NOT NULL
);
