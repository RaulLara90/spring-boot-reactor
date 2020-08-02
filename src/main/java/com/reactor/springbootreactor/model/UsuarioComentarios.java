package com.reactor.springbootreactor.model;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class UsuarioComentarios {

    private Usuario usuario;
    private Comentarios comentarios;

    @Override
    public String toString() {
        return "UsuarioComentarios{" +
                "usuario=" + usuario +
                ", comentarios=" + comentarios +
                '}';
    }
}
