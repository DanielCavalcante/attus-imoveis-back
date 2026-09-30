#!/usr/bin/env bash
#
# Sobe o Encontrei no GCP, na ordem em que as dependências permitem.
#
# São seis passos porque três coisas só existem depois de outra: a API precisa
# saber a conta do site para autorizá-la; as imagens precisam do registro, que
# o Terraform cria; e o serviço da API precisa do digest da imagem, que só
# existe depois do push.
#
# Rode de dentro de attus-imoveis-back/terraform. É idempotente: rodar de novo
# depois de uma falha retoma de onde parou.

set -euo pipefail

PROJETO="mei-tranquilo-501616"
NUMERO="279337898922"
REGIAO="southamerica-east1"
AMBIENTE="prod"
ESTADO="mei-tranquilo-501616-attus-tfstate"
REGISTRO="${REGIAO}-docker.pkg.dev/${PROJETO}/attus-prod"
BACK="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONT="$(cd "$BACK/../attus-imoveis-front" && pwd)"

# A URL do site precisa ser conhecida ANTES do build: NEXT_PUBLIC_* e embutido
# no bundle, e e ela que vai no canonical, no Open Graph e no sitemap.
#
# Prever nao funciona. O formato `<servico>-<numero do projeto>.<regiao>` saiu
# de linha: o Cloud Run passou a emitir `<servico>-<hash>-<sigla>.a.run.app`,
# e o endereco real do primeiro deploy nao foi o previsto -- o `robots.txt`
# em producao apontou o sitemap para um host que nao existe.
#
# Entao perguntamos ao servico. Na primeira vez ele ainda nao existe, e o
# fallback e a previsao antiga: a imagem sai com um endereco provavelmente
# errado, o passo 6 cria o servico, e a checagem do fim manda repetir os
# passos 4 e 6 -- que ai ja acham a URL de verdade.
URL_DO_SITE="$(gcloud run services describe "attus-site-${AMBIENTE}"   --region="${REGIAO}" --project="${PROJETO}"   --format='value(status.url)' 2>/dev/null || true)"

if [ -z "${URL_DO_SITE}" ]; then
  URL_DO_SITE="https://attus-site-${AMBIENTE}-${NUMERO}.${REGIAO}.run.app"
  echo "servico ainda nao existe; usando endereco previsto ${URL_DO_SITE}"
fi

CONTA_DO_SITE="attus-site-${AMBIENTE}@${PROJETO}.iam.gserviceaccount.com"
FALSO="sha256:0000000000000000000000000000000000000000000000000000000000000000"

passo() { printf "\n\033[1m==> %s\033[0m\n" "$1"; }

digest_de() {
  gcloud artifacts docker images describe "$1" --format='value(image_summary.digest)'
}

# ---------------------------------------------------------------------------

passo "1/6  identidade do site (não depende da API)"
cd "$FRONT/terraform"
terraform init -input=false -reconfigure \
  -backend-config="bucket=${ESTADO}" -backend-config="prefix=site" >/dev/null
terraform apply -input=false -auto-approve -target=google_service_account.site \
  -var="projeto=${PROJETO}" \
  -var="bucket_do_estado=${ESTADO}" \
  -var="imagem_do_site=${REGISTRO}/site@${FALSO}"

passo "2/6  infraestrutura da API, menos o serviço"
# O serviço fica de fora porque ainda não há imagem: o registro que vai
# guardá-la é criado AQUI.
cd "$BACK/terraform"
terraform init -input=false -reconfigure \
  -backend-config="bucket=${ESTADO}" -backend-config="prefix=api" >/dev/null
terraform apply -input=false -auto-approve \
  -target=google_artifact_registry_repository.imagens \
  -target=google_sql_database_instance.principal \
  -target=google_sql_database.aplicacao \
  -target=google_sql_user.aplicacao \
  -target=google_storage_bucket.imagens \
  -target=google_storage_bucket_iam_member.api \
  -target=google_secret_manager_secret_version.senha_do_banco \
  -target=google_secret_manager_secret_iam_member.api_nos_segredos \
  -var="projeto=${PROJETO}" \
  -var="conta_do_site=${CONTA_DO_SITE}" \
  -var="imagem_da_api=${REGISTRO}/api@${FALSO}"

passo "3/6  a chave de assinatura"
# O Terraform nunca a vê: quem a lê é a aplicação, em execução. Só grava se
# ainda não houver versão -- rodar de novo não invalida as sessões abertas.
if ! gcloud secrets versions list attus-prod-jwt --project="${PROJETO}" \
      --filter="state=ENABLED" --format="value(name)" 2>/dev/null | grep -q .; then
  openssl rand -base64 48 | tr -d '[:space:]' | \
    gcloud secrets versions add attus-prod-jwt --project="${PROJETO}" --data-file=-
  echo "chave gravada"
else
  echo "já havia chave; mantida"
fi

passo "4/6  imagens para o Artifact Registry"
gcloud auth configure-docker "${REGIAO}-docker.pkg.dev" --quiet

VERSAO="$(git -C "$BACK" rev-parse --short HEAD)"

docker build -t "${REGISTRO}/api:${VERSAO}" "$BACK"
docker push "${REGISTRO}/api:${VERSAO}"

# A imagem do site carrega o endereço com que foi construída.
docker build --build-arg "NEXT_PUBLIC_SITE_URL=${URL_DO_SITE}" \
  -t "${REGISTRO}/site:${VERSAO}" "$FRONT"
docker push "${REGISTRO}/site:${VERSAO}"

API_DIGEST="$(digest_de "${REGISTRO}/api:${VERSAO}")"
SITE_DIGEST="$(digest_de "${REGISTRO}/site:${VERSAO}")"

echo "api:  ${API_DIGEST}"
echo "site: ${SITE_DIGEST}"

passo "5/6  a API inteira, com a imagem de verdade"
cd "$BACK/terraform"
terraform apply -input=false -auto-approve \
  -var="projeto=${PROJETO}" \
  -var="conta_do_site=${CONTA_DO_SITE}" \
  -var="imagem_da_api=${REGISTRO}/api@${API_DIGEST}"

passo "6/6  o site inteiro"
cd "$FRONT/terraform"
terraform apply -input=false -auto-approve \
  -var="projeto=${PROJETO}" \
  -var="bucket_do_estado=${ESTADO}" \
  -var="imagem_do_site=${REGISTRO}/site@${SITE_DIGEST}"

passo "no ar"
terraform output -raw endereco_do_site; echo
echo "previsto no build: ${URL_DO_SITE}"
echo
echo "Se os dois endereços acima diferirem, a imagem do site foi construída com"
echo "um endereço em que ele não responde: refaça o passo 4 com o valor real e"
echo "repita o 6."
