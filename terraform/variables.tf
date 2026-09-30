variable "projeto" {
  description = "ID do projeto no GCP."
  type        = string
}

variable "regiao" {
  description = "Regiao dos servicos. Uma so: e validacao, nao redundancia."
  type        = string
  default     = "southamerica-east1"
}

variable "ambiente" {
  description = "Sufixo que separa um ambiente do outro nos nomes dos recursos."
  type        = string
  default     = "prod"
}

variable "imagem_da_api" {
  description = <<-TEXTO
    A imagem da API, POR DIGEST.

    Por digest e nao por `latest`: tag e um ponteiro que alguem move, e um
    `terraform apply` que nao mudou nada no codigo poderia subir outro binario.
    Formato: REGIAO-docker.pkg.dev/PROJETO/REPO/api@sha256:...
  TEXTO
  type        = string

  validation {
    condition     = can(regex("@sha256:[0-9a-f]{64}$", var.imagem_da_api))
    error_message = "A imagem precisa vir por digest (@sha256:...), nunca por tag."
  }
}

variable "cpu" {
  description = "O menor porte que sobe a aplicacao. Sobe depois de medir, nao antes."
  type        = string
  default     = "1"
}

variable "memoria" {
  description = <<-TEXTO
    Memoria do conteiner.

    512Mi nao sobe uma JVM com Spring Boot com folga para o heap e o
    metaspace; 1Gi e o piso pratico. Este e o numero que a validacao existe
    para confirmar ou desmentir.
  TEXTO
  type        = string
  default     = "1Gi"
}

variable "instancias_maximas" {
  description = "Teto de escala. Minimo e sempre zero: validacao nao paga instancia parada."
  type        = number
  default     = 3
}

variable "conta_do_site" {
  description = <<-TEXTO
    E-mail da conta de servico do site, a unica autorizada a invocar a API.

    Vem de fora porque a direcao da dependencia e site -> API: este estado
    publica onde a API esta, e o outro consome. Fosse o contrario, nenhum dos
    dois teria ordem de aplicacao possivel.
  TEXTO
  type        = string
}
