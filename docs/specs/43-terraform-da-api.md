# 43-terraform-da-api

## Intenção
A infraestrutura da API existe como código: Cloud Run sem entrada da internet,
banco privado, bucket privado e segredos fora do repositório.

## Depende de
40-api-configuravel-por-ambiente

## Critérios

- [ ] **C1** `infraestrutura` — `terraform plan` roda a partir do repositório limpo, sem nenhum recurso criado à mão antes.
- [ ] **C2** `seguranca` — o serviço da API é declarado com entrada interna: requisição vinda da internet não o alcança.
- [ ] **C3** `seguranca` — só a conta de serviço do site tem permissão de invocar a API.
- [ ] **C4** `seguranca` — o banco é declarado sem IP público.
- [ ] **C5** `seguranca` — o bucket das imagens é declarado sem acesso público.
- [ ] **C6** `seguranca` — a conta de serviço da API tem acesso ao bucket e ao banco, e a nada além disso.
- [ ] **C7** `seguranca` — nenhum segredo aparece no código: chave de assinatura e senha do banco vêm do Secret Manager.
- [ ] **C8** `infraestrutura` — o estado do Terraform fica em bucket remoto, com versionamento.
- [ ] **C9** `contrato-api` — o estado publica como saída o endereço interno da API e o e-mail da conta de serviço que pode invocá-la.
- [ ] **C10** `performance` — o serviço é declarado com o menor porte de CPU e memória que sobe a aplicação, e com mínimo de instâncias zero.
- [ ] **C11** `observabilidade` — o serviço usa a rota de prontidão da spec 40 como sonda.
- [ ] **C12** `infraestrutura` — a imagem vem do Artifact Registry declarado aqui, por digest e não por `latest`.
- [ ] **C13** `infraestrutura` — aplicar duas vezes seguidas não produz mudança na segunda.

## Contrato técnico

**Cloud Run**, que é o equivalente do Fargate aqui: contêiner sem servidor para
administrar, com escala a zero. Porte inicial pequeno de propósito -- é
validação, e subir grande esconde justamente o que a validação deveria
mostrar. C10 fixa o menor porte que sobe a aplicação, e não um número bonito:
JVM tem piso de memória, e o valor sai de medir, não de chutar.

**A API não tem entrada da internet** (C2), e isso sozinho não basta: entrada
interna ainda aceita qualquer carga de dentro do projeto. Por isso C3 -- só a
conta de serviço do site invoca. São duas trancas porque cada uma cobre o que
a outra deixa passar.

**C9 é o que faz os dois repositórios conversarem.** Cada um tem a sua
terraformagem, como você pediu, mas eles são o mesmo domínio: o site precisa
saber o endereço da API e a API precisa saber quem pode chamá-la. Isso viaja
por saída de estado remoto, lido pelo outro lado com `terraform_remote_state`
-- não por alguém copiar um valor de um terminal para um arquivo.

**C13 é o critério que separa infraestrutura como código de script de
criação.** Recurso que muda a cada aplicação não descreve o mundo, descreve
uma intenção mal escrita.

## Fora de escopo

- **Aplicar.** Esta spec escreve e planeja; subir é decisão sua, e é o
  combinado: terraformar, sinalizar, e só então subir.
- **Domínio e certificado**, que entram com o serviço público — spec 44.
- **Pipeline de build das imagens.** O Terraform consome imagem que já existe
  no registro; quem a constrói é outra decisão.
- **Redundância entre regiões, réplica de leitura, backup com retenção
  definida.** É validação de porte pequeno; dimensionar vem depois de medir.
- **Migrar os dados de desenvolvimento.** O banco de produção nasce vazio, e a
  spec 40 exige que as migrações apliquem da V1 à última numa base limpa.

## Plano

1. Estado remoto e projeto — `terraform/backend.tf`, `terraform/versions.tf`,
   `terraform/variables.tf`.
2. Registro de imagens e conta de serviço — `terraform/registro.tf`,
   `terraform/identidade.tf`.
3. Banco privado e segredos — `terraform/banco.tf`, `terraform/segredos.tf`.
4. Bucket das imagens — `terraform/bucket.tf`.
5. O serviço, com entrada interna e sonda — `terraform/servico.tf`.
6. As saídas que o outro repositório lê — `terraform/outputs.tf`.
7. Como rodar, e o que precisa existir antes — `terraform/README.md`.

## Perguntas em aberto

Nenhuma. A que existia -- o que "o usuário tem acesso apenas ao cadastro do
usuário" restringe -- foi respondida: é regra do produto, não infraestrutura.
Virou a spec 45, e não muda nada aqui.
