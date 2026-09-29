package com.example.demo.integracoes;

import java.util.Optional;

/**
 * Descobre o bairro a partir do CEP que o anunciante ja digita.
 *
 * Existe como interface para que o teste nao dependa de rede e para que trocar
 * o servico depois nao espalhe pelo dominio.
 *
 * Contrato: nunca lanca. CEP que nao resolve, servico fora do ar ou tempo
 * limite estourado devolvem Optional.empty(). Bairro e recorte de busca, nao
 * dado essencial do imovel: derrubar a publicacao de um anuncio porque um
 * servico de terceiro caiu troca um problema pequeno por um grande.
 */
public interface ConsultaDeCep {

    Optional<String> bairroDe(String cep);
}
