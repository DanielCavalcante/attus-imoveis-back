# A rede privada onde o banco vive.
#
# O Cloud SQL sem IP publico (spec 43, C4) so e alcancavel de dentro de uma
# VPC, e quem alcanca e a API -- por saida direta de VPC, sem conector, que e
# o caminho do Cloud Run v2.

resource "google_compute_network" "privada" {
  name                    = "attus-${var.ambiente}"
  auto_create_subnetworks = false

  depends_on = [google_project_service.necessarios]
}

resource "google_compute_subnetwork" "privada" {
  name          = "attus-${var.ambiente}"
  region        = var.regiao
  network       = google_compute_network.privada.id
  ip_cidr_range = "10.10.0.0/24"

  # Exigido pela saida direta de VPC do Cloud Run.
  private_ip_google_access = true
}

# A faixa que o Google reserva para os servicos gerenciados dele -- e onde o
# IP privado do banco vai nascer.
resource "google_compute_global_address" "faixa_do_banco" {
  name          = "attus-${var.ambiente}-sql"
  purpose       = "VPC_PEERING"
  address_type  = "INTERNAL"
  prefix_length = 16
  network       = google_compute_network.privada.id
}

resource "google_service_networking_connection" "banco" {
  network                 = google_compute_network.privada.id
  service                 = "servicenetworking.googleapis.com"
  reserved_peering_ranges = [google_compute_global_address.faixa_do_banco.name]
}

# ---------------------------------------------------------------------------
# Como o site alcanca uma API que nao tem entrada da internet.
#
# `run.app` e um dominio publico: resolvido normalmente, ele aponta para um IP
# publico, e a saida do site iria para a internet em vez de atravessar esta
# VPC. Chegando de fora, `INGRESS_TRAFFIC_INTERNAL_ONLY` recusa -- e o Cloud
# Run recusa com **404**, nao com 403, o que faz o sintoma parecer rota errada
# em vez de bloqueio de rede. Foi exatamente o que aconteceu no primeiro
# deploy: o site subiu, a API subiu, e todo `/api/v1/...` voltava 404.
#
# A saida e resolver `run.app` DENTRO da VPC para a faixa de acesso privado do
# Google. Com `private_ip_google_access` ligado na sub-rede, a requisicao vai
# ao Cloud Run sem nunca sair para a internet -- e chega como interna.
#
# A alternativa seria um Cloud NAT, que resolveria o mesmo e custa por hora de
# gateway parado. Para uma validacao, nao se justifica.

resource "google_dns_managed_zone" "run_app" {
  name        = "attus-${var.ambiente}-run-app"
  dns_name    = "run.app."
  description = "Resolve run.app para o acesso privado do Google, dentro da VPC"

  visibility = "private"

  private_visibility_config {
    networks {
      network_url = google_compute_network.privada.id
    }
  }

  depends_on = [google_project_service.necessarios]
}

# `private.googleapis.com`, e nao `restricted`: a faixa restrita exige VPC
# Service Controls, que nao existe aqui, e sem eles ela simplesmente nao
# responde.
resource "google_dns_record_set" "run_app" {
  for_each = toset(["run.app.", "*.run.app."])

  managed_zone = google_dns_managed_zone.run_app.name
  name         = each.value
  type         = "A"
  ttl          = 300

  rrdatas = ["199.36.153.8", "199.36.153.9", "199.36.153.10", "199.36.153.11"]
}
