package com.example.demo.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.demo.services.JwtService;
import com.example.demo.services.UserDetailsServiceImpl;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final org.slf4j.Logger log =
        org.slf4j.LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;

    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthenticationFilter(
        JwtService jwtService,
        UserDetailsServiceImpl userDetailsService
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader =
            request.getHeader("Authorization");

        if (authHeader == null ||
            !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);

            return;
        }

        String token = authHeader.substring(7);

        /*
         * Nenhuma falha de token escapa deste bloco.
         *
         * Antes, token malformado, expirado, de assinatura invalida ou de conta
         * removida deixava a excecao subir: o Tomcat despachava para /error, o
         * cliente recebia 403 de corpo vazio e o servidor registrava rastro de
         * pilha em nivel ERROR a cada tentativa. Pior, ROTA PUBLICA parava de
         * responder quando havia cabecalho ruim -- qualquer visitante com token
         * velho guardado perdia a listagem.
         *
         * Token invalido agora significa apenas "esta requisicao nao tem
         * sessao". Quem decide o que fazer com isso e o SecurityConfig.
         */
        try {
            autenticar(request, token);
        } catch (JwtException | UsernameNotFoundException falha) {
            // Nivel debug de proposito: token invalido e situacao normal e de
            // origem externa. Em error, qualquer pessoa enche o disco de quem opera.
            log.debug("Token recusado: {}", falha.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private void autenticar(HttpServletRequest request, String token) {
        String email = jwtService.extractUsername(token);

        if (email == null
            || SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        // Carregado do banco a cada requisicao: as authorities vem do
        // estado atual da conta, entao remover o papel tem efeito imediato.
        // A claim "role" segue no token, mas so como dica para a interface.
        UserDetails userDetails =
            userDetailsService
                .loadUserByUsername(email);

        if (!jwtService.isTokenValid(token, userDetails)) {
            return;
        }

        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
            );

        authToken.setDetails(
            new WebAuthenticationDetailsSource()
                .buildDetails(request)
        );

        SecurityContextHolder
            .getContext()
            .setAuthentication(authToken);
    }
}