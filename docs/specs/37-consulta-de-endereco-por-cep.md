# 37-consulta-de-endereco-por-cep

## Intenção
O CEP passa a devolver o endereço inteiro, por uma rota própria, e a consulta
para de depender de um único serviço de terceiro.

## Depende de
nenhuma

## Critérios

- [ ] **C1** `contrato-api` — a rota devolve logradouro, bairro, cidade e UF para um CEP que existe.
- [ ] **C2** `contrato-api` — CEP que não existe recebe 404.
- [ ] **C3** `contrato-api` — CEP fora do formato de 8 dígitos recebe 400.
- [ ] **C4** `integracao` — com o provedor primário fora do ar, a resposta vem do segundo.
- [ ] **C5** `integracao` — com os dois fora, a rota responde 503 e não 500.
- [ ] **C6** `seguranca` — a rota exige autenticação.
- [ ] **C7** `performance` — a consulta tem tempo limite, e estourá-lo cai para o próximo provedor em vez de esperar.
- [ ] **C8** `dominio` — criar anúncio sem informar bairro continua derivando o bairro da mesma consulta.
- [ ] **C9** `dominio` — com todos os provedores fora, o anúncio publica sem bairro, como antes.
- [ ] **C10** `dominio` — bairro informado no anúncio continua prevalecendo sobre o derivado.
- [ ] **C11** `performance` — a resposta da rota é marcada como cacheável.

## Contrato técnico

`ConsultaDeCep` hoje devolve `Optional<String>` com o bairro e nada mais — a
`record Resposta(String bairro)` descarta logradouro, cidade e UF que o serviço
já manda. A interface passa a devolver um endereço; o bairro vira um campo
dele, não o retorno inteiro.

Dois provedores em cadeia, e a ordem importa menos que o fato de haver dois:
hoje o ViaCEP é ponto único de falha, e ele **está fora enquanto esta spec é
escrita** — foi assim que o problema apareceu. A BrasilAPI foi verificada no
mesmo momento e respondeu, ela própria caindo para outro provedor
(`"service":"open-cep"` na resposta).

O contrato de nunca lançar se mantém para o caminho do anúncio (C9): serviço de
terceiro fora não derruba publicação, e essa foi a decisão da spec 19. A rota
nova é o caso oposto — ali quem chama **está perguntando pelo endereço**, então
a falha precisa aparecer, e é isso que separa C5 de C9.

Autenticada (C6) porque rota aberta que chama serviço externo é proxy de graça
para quem quiser, na nossa cota e com o nosso IP.

## Fora de escopo

- **Guardar os CEPs consultados em banco.** Cache em memória com tempo de vida
  resolve o custo de hoje; tabela de CEP é catálogo para manter.
- **Preencher a tela** — é a spec 38, no front.
- **Corrigir o bairro dos anúncios que já estão sem ele.** É migração de dados.
- **Validar se o CEP corresponde à cidade digitada.** Divergência entre os dois
  é caso real e merece decisão própria.

## Plano

1. A interface passa a devolver endereço, não só bairro —
   `integracoes/ConsultaDeCep.java`, `dtos/EnderecoDTO.java`.
2. Provedores em cadeia, com tempo limite —
   `integracoes/ViaCepConsulta.java`, `integracoes/BrasilApiConsulta.java`,
   `integracoes/ConsultaEmCadeia.java`.
3. A rota — `controllers/EnderecoController.java`, `config/SecurityConfig.java`.
4. O create passa a ler o bairro do endereço —
   `services/AnnouncementService.java`.
5. Testes por critério, com os provedores dublados —
   `src/test/java/com/example/demo/ConsultaDeEnderecoSpecTest.java`.

## Perguntas em aberto

Nenhuma.
