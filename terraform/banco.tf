# O banco, sem IP publico.
#
# `ipv4_enabled = false` e o criterio C4 inteiro: sem isso a instancia nasce
# alcancavel da internet, e a unica coisa entre ela e o mundo seria a senha.

resource "google_sql_database_instance" "principal" {
  name             = "attus-${var.ambiente}"
  database_version = "POSTGRES_16"
  region           = var.regiao

  # Validacao: a instancia pode ser destruida sem cerimonia. Em producao de
  # verdade isto vira `true`, e essa mudanca merece ser consciente.
  deletion_protection = false

  settings {
    # O menor porte que roda Postgres 16. Sobe depois de medir.
    tier = "db-f1-micro"

    ip_configuration {
      ipv4_enabled    = false
      private_network = google_compute_network.privada.id
    }

    backup_configuration {
      enabled = true
    }
  }

  depends_on = [google_service_networking_connection.banco]
}

resource "google_sql_database" "aplicacao" {
  name     = "imoveis"
  instance = google_sql_database_instance.principal.name
}

# A senha do banco e SORTEADA aqui, e nao trazida de fora.
#
# A distincao que falta quando se trata "segredo" como uma coisa so: o
# Terraform PRECISA conhecer esta senha, porque e ele quem cria o usuario do
# banco. A chave JWT ele nunca precisa conhecer -- quem a le e a aplicacao, em
# tempo de execucao, direto do Secret Manager.
#
# A primeira versao desta configuracao lia a senha por `data source`, de uma
# versao de segredo que o README mandava criar DEPOIS do primeiro apply. O
# `plan` mostrou a ordem impossivel: o apply precisaria do valor para criar o
# usuario, e o valor so existiria depois do apply.
resource "random_password" "banco" {
  length  = 32
  special = false
}

resource "google_sql_user" "aplicacao" {
  name     = "attus"
  instance = google_sql_database_instance.principal.name
  password = random_password.banco.result
}

# A mesma senha vai para o Secret Manager, que e de onde o Cloud Run a le.
resource "google_secret_manager_secret_version" "senha_do_banco" {
  secret      = google_secret_manager_secret.senha_do_banco.id
  secret_data = random_password.banco.result
}
