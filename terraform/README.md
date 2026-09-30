# Terraform da API

A infraestrutura da API: Cloud Run **sem entrada da internet**, Cloud SQL sem
IP público, bucket privado e segredos no Secret Manager.

## A ordem entre os dois repositórios

Cada repositório tem a sua terraformagem, mas eles são o mesmo domínio e a
dependência tem direção: **o site depende da API, não o contrário.** Este
estado publica onde a API está; o do site consome por
`terraform_remote_state`.

Isso tem uma consequência no primeiro `apply`, e ela é circular à primeira
vista: a API precisa saber a conta de serviço do site para autorizá-la, e o
site precisa saber o endereço da API. A saída é aplicar em três passos, uma
vez só:

1. **No repositório do site**, aplique só a identidade:
   `terraform apply -target=google_service_account.site`
   Ela não depende de nada daqui.
2. **Aqui**, aplique tudo, passando a conta criada acima em `conta_do_site`.
3. **No repositório do site**, aplique o resto. Ele já encontra as saídas
   daqui.

Da segunda vez em diante, cada lado aplica sozinho.

## O que precisa existir antes

O Terraform não cria o que ele usa para guardar o próprio estado, nem grava
segredo — valor que passa pelo Terraform fica em claro no estado.

```bash
# 1. O bucket do estado, com versionamento: estado perdido é infraestrutura
#    órfã, e versionamento é o que permite voltar.
gcloud storage buckets create gs://SEU_PROJETO-tfstate \
  --location=southamerica-east1 --uniform-bucket-level-access
gcloud storage buckets update gs://SEU_PROJETO-tfstate --versioning

# 2. A chave de assinatura. Precisa de 32 bytes no minimo -- a spec 40 recusa
#    menos, e recusa na subida. Roda DEPOIS do primeiro apply, que e quem cria
#    o segredo vazio.
openssl rand -base64 48 | tr -d '
' | gcloud secrets versions add attus-prod-jwt --data-file=-
```

**A senha do banco nao esta ai, e a diferenca e proposital.** O Terraform
*precisa* conhece-la -- e ele quem cria o usuario do banco --, entao ela e
sorteada por ele e gravada no Secret Manager no mesmo `apply`. A chave JWT ele
nunca precisa conhecer: quem a le e a aplicacao, em tempo de execucao.

Tratar "segredo" como uma coisa so foi o que produziu a primeira versao desta
configuracao, que lia a senha de uma versao criada *depois* do `apply` que
precisava dela -- ordem impossivel, que so o `plan` contra o GCP revelou.

O servico nao sobe sem a chave JWT: e o comportamento que a spec 40 exige.

## Aplicar

```bash
terraform init \
  -backend-config="bucket=SEU_PROJETO-tfstate" \
  -backend-config="prefix=api"

terraform plan \
  -var="projeto=SEU_PROJETO" \
  -var="conta_do_site=attus-site-prod@SEU_PROJETO.iam.gserviceaccount.com" \
  -var="imagem_da_api=southamerica-east1-docker.pkg.dev/SEU_PROJETO/attus-prod/api@sha256:..."
```

**A imagem vai por digest, nunca por tag.** Tag é um ponteiro que alguém move,
e um `apply` que não mudou nada no código poderia subir outro binário. A
variável recusa qualquer valor que não termine em `@sha256:`.

Para publicar a imagem:

```bash
gcloud auth configure-docker southamerica-east1-docker.pkg.dev
docker build -t southamerica-east1-docker.pkg.dev/SEU_PROJETO/attus-prod/api:$(git rev-parse --short HEAD) .
docker push southamerica-east1-docker.pkg.dev/SEU_PROJETO/attus-prod/api:$(git rev-parse --short HEAD)

# O digest sai daqui, e é ele que vai para o Terraform.
gcloud artifacts docker images describe \
  southamerica-east1-docker.pkg.dev/SEU_PROJETO/attus-prod/api:$(git rev-parse --short HEAD) \
  --format='value(image_summary.digest)'
```

## Porte

`db-f1-micro`, 1 vCPU e 1 GiB, mínimo de zero instâncias. É validação — subir
grande esconderia justamente o que a validação deveria mostrar.

1 GiB e não 512 MiB porque uma JVM com Spring Boot não sobe com folga em meio
giga entre heap e metaspace. Esse é o número que a validação existe para
confirmar ou desmentir: se a instância morrer por memória, o valor sobe; se
sobrar, desce.

## O que este estado NÃO faz

- **Não aplica sozinho.** Escrever e planejar é uma coisa; subir é decisão de
  quem opera.
- **Não constrói imagem.** Ele consome imagem que já existe no registro.
- **Não grava segredo.** Só declara o contêiner.
- **Não compra domínio.** Isso é do site, e domínio é compra, não código.
