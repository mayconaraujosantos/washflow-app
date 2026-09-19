package com.washflow.domain.entities;

import java.math.BigDecimal;

/** {@code servicos_preco} from CLAUDE.md - the wash type catalog. */
public record ServicoPreco(int id, String nome, BigDecimal preco, BigDecimal comissaoLavador) {}
