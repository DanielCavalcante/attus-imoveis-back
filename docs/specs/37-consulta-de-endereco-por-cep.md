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
- [ ] **C4** `persistencia` — CEP já consultado antes é respondido sem nenhuma chamada a provedor.
- [ ] **C5** `persistencia` — CEP consultado pela primeira vez fica guardado para as próximas.
- [ ] **C6** `integracao` — com o provedor primário fora do ar, a resposta vem do segundo.
- [ ] **C7** `integracao` — com os dois fora e o CEP nunca consultado, a rota responde 503 e não 500.
- [ ] **C8** `persistencia` — com todos os provedores fora, CEP que já está guardado continua respondendo.
- [ ] **C9** `seguranca` — a rota exige autenticação.
- [ ] **C10** `performance` — a consulta a provedor tem tempo limite, e estourá-lo cai para o próximo em vez de esperar.
- [ ] **C11** `dominio` — criar anúncio sem informar bairro continua derivando o bairro da mesma consulta.
- [ ] **C12** `dominio` — com todos os provedores fora e o CEP desconhecido, o anúncio publica sem bairro, como antes.
- [ ] **C13** `dominio` — bairro informado no anúncio continua prevalecendo sobre o derivado.
- [ ] **C14** `performance` — a resposta da rota é marcada como cacheável pelo navegador.

## Contrato técnico

**A ordem é: guardado, depois provedor.** Só se consulta terceiro o que ainda
não se sabe. CEP não muda de bairro, então o que foi resolvido uma vez vale
para sempre -- e isso transforma cada consulta bem-sucedida em uma que nunca
mais precisa sair daqui.

O guardado vive em **tabela**, não em memória: memória morre com o contêiner e
com ela morre a única resposta que sobrava quando todos os provedores estão
fora. É exatamente o cenário de hoje, e é o que C8 mede -- foi o ViaCEP cair
que fez este problema aparecer.

`ConsultaDeCep` hoje devolve `Optional<String>` com o bairro e nada mais — a
`record Resposta(String bairro)` descarta logradouro, cidade e UF que o serviço
já manda. A interface passa a devolver um endereço; o bairro vira um campo
dele, não o retorno inteiro.

Dois provedores em cadeia, e a ordem importa menos que o fato de haver dois:
hoje o ViaCEP é ponto único de falha, e ele **está fora enquanto esta spec é
escrita** — foi assim que o problema apareceu. A BrasilAPI foi verificada no
mesmo momento e respondeu, ela própria caindo para outro provedor
(`"service":"open-cep"` na resposta).

O contrato de nunca lançar se mantém para o caminho do anúncio (C12): serviço de
terceiro fora não derruba publicação, e essa foi a decisão da spec 19. A rota
nova é o caso oposto — ali quem chama **está perguntando pelo endereço**, então
a falha precisa aparecer, e é isso que separa C7 de C12.

Autenticada (C9) porque rota aberta que chama serviço externo é proxy de graça
para quem quiser, na nossa cota e com o nosso IP.

## Fora de escopo

- **Pré-carregar a base de CEPs do Brasil.** O guardado cresce pelo uso, com
  os CEPs que alguém realmente consultou; importar milhões de linhas é catálogo
  para manter, e ninguém pediu.
- **Expirar o que está guardado.** CEP não muda de bairro. Se mudar, é caso
  raro e merece uma decisão explícita, não um tempo de vida chutado.
- **Preencher a tela** — é a spec 38, no front.
- **Corrigir o bairro dos anúncios que já estão sem ele.** É migração de dados.
- **Validar se o CEP corresponde à cidade digitada.** Divergência entre os dois
  é caso real e merece decisão própria.

## Plano

1. A interface passa a devolver endereço, não só bairro —
   `integracoes/ConsultaDeCep.java`, `dtos/EnderecoDTO.java`.
2. A tabela do que já foi resolvido, e a consulta que olha nela primeiro —
   `model/Endereco.java`, `repositories/EnderecoRepository.java`,
   `db/migration/V11__enderecos_por_cep.sql`, `services/EnderecoService.java`.
3. Provedores em cadeia, com tempo limite —
   `integracoes/ViaCepConsulta.java`, `integracoes/BrasilApiConsulta.java`,
   `integracoes/ConsultaEmCadeia.java`.
4. A rota — `controllers/EnderecoController.java`, `config/SecurityConfig.java`.
5. O create passa a ler o bairro do endereço —
   `services/AnnouncementService.java`.
6. Testes por critério, com os provedores dublados —
   `src/test/java/com/example/demo/ConsultaDeEnderecoSpecTest.java`.

## Perguntas em aberto

Nenhuma.
