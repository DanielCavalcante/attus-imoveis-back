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

resource "google_sql_user" "aplicacao" {
  name     = "attus"
  instance = google_sql_database_instance.principal.name

  # A senha vem do Secret Manager, gravada fora do Terraform: valor que passa
  # pelo estado fica em claro no bucket do estado.
  password_wo         = data.google_secret_manager_secret_version.senha_do_banco.secret_data
  password_wo_version = data.google_secret_manager_secret_version.senha_do_banco.version
}

data "google_secret_manager_secret_version" "senha_do_banco" {
  secret = google_secret_manager_secret.senha_do_banco.id
}
