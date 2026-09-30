# A API, sem entrada da internet.
#
# `INGRESS_TRAFFIC_INTERNAL_ONLY` (C2) recusa qualquer requisicao que nao venha
# de dentro. Isso sozinho NAO basta: entrada interna ainda aceita qualquer
# carga do projeto, e por isso o invoker e so a conta de servico do site (C3).
# Duas trancas, cada uma cobrindo o que a outra deixa passar.

resource "google_cloud_run_v2_service" "api" {
  name     = "attus-api-${var.ambiente}"
  location = var.regiao

  ingress             = "INGRESS_TRAFFIC_INTERNAL_ONLY"
  deletion_protection = false

  template {
    service_account = google_service_account.api.email

    # Minimo zero: validacao nao paga instancia parada. O custo e a primeira
    # requisicao depois de um periodo ocioso esperar a subida da JVM.
    scaling {
      min_instance_count = 0
      max_instance_count = var.instancias_maximas
    }

    # Saida direta pela VPC, sem conector: e o caminho do Cloud Run v2, e e o
    # que permite alcancar o IP privado do banco.
    vpc_access {
      network_interfaces {
        network    = google_compute_network.privada.id
        subnetwork = google_compute_subnetwork.privada.id
      }

      egress = "PRIVATE_RANGES_ONLY"
    }

    containers {
      image = var.imagem_da_api

      resources {
        limits = {
          cpu    = var.cpu
          memory = var.memoria
        }
      }

      env {
        name  = "SPRING_DATASOURCE_URL"
        value = "jdbc:postgresql://${google_sql_database_instance.principal.private_ip_address}:5432/${google_sql_database.aplicacao.name}"
      }

      env {
        name  = "SPRING_DATASOURCE_USERNAME"
        value = google_sql_user.aplicacao.name
      }

      # Os segredos entram por referencia, nunca por valor: valor aqui viraria
      # texto em claro no estado e na descricao do servico.
      env {
        name = "SPRING_DATASOURCE_PASSWORD"

        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.senha_do_banco.secret_id
            version = "latest"
          }
        }
      }

      env {
        name = "JWT_SECRET"

        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.jwt.secret_id
            version = "latest"
          }
        }
      }

      env {
        name  = "ATTUS_IMAGENS_BUCKET"
        value = google_storage_bucket.imagens.name
      }

      # Vazio: em producao o navegador fala com a propria origem do site, e
      # requisicao de outra origem nao existe. A spec 40 garante que vazio
      # recusa todas, e nunca vira `*`.
      env {
        name  = "ATTUS_CORS_ORIGENS"
        value = ""
      }

      # A sonda de prontidao da spec 40: ela depende do banco, e e o que diz
      # "posso atender". A de saude nao depende, e e outra rota de proposito.
      startup_probe {
        http_get {
          path = "/actuator/health/readiness"
        }

        initial_delay_seconds = 10
        period_seconds        = 5
        failure_threshold     = 12
      }

      liveness_probe {
        http_get {
          path = "/actuator/health/liveness"
        }

        period_seconds = 30
      }
    }
  }

  depends_on = [
    google_secret_manager_secret_iam_member.api_nos_segredos,
    google_secret_manager_secret_version.senha_do_banco,
    google_project_service.necessarios,
  ]
}

# Quem pode invocar: a conta de servico do site, e mais ninguem.
#
# Nao ha `allUsers` aqui, e e a diferenca entre "a API esta fechada" e "a API
# esta atras de uma porta destrancada".
resource "google_cloud_run_v2_service_iam_member" "so_o_site" {
  name     = google_cloud_run_v2_service.api.name
  location = google_cloud_run_v2_service.api.location
  role     = "roles/run.invoker"
  member   = "serviceAccount:${var.conta_do_site}"
}
