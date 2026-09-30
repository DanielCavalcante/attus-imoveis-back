package com.example.demo.dtos;

/**
 * O endereco que um CEP resolve.
 *
 * Ate a spec 37 so o bairro era aproveitado: o `record Resposta(String bairro)`
 * do ViaCEP descartava logradouro, cidade e UF que o servico ja mandava na
 * mesma resposta. A tela precisa dos quatro para preencher o formulario.
 */
public record EnderecoDTO(
    String cep,
    String logradouro,
    String bairro,
    String cidade,
    String uf
) {}
