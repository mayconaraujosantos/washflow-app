package com.washflow.infra.db.jdbi.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.washflow.data.protocols.db.whatsapp.ConversationRepository;
import com.washflow.domain.entities.whatsapp.Conversation;
import com.washflow.domain.entities.whatsapp.ConversationDraft;
import com.washflow.domain.entities.whatsapp.ConversationStep;
import java.time.Instant;
import java.util.Optional;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConversationJdbiRepository implements ConversationRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConversationJdbiRepository.class);
  // Unknown fields are ignored so a draft saved by an older version still loads.
  private static final ObjectMapper JSON =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private static final String ESTABLISHMENT_SQL =
      "SELECT nome FROM estabelecimentos WHERE id = :tenant AND ativo";

  private static final String LOAD_SQL =
      """
      SELECT etapa, dados, atualizado_em FROM conversas_whatsapp
      WHERE estabelecimento_id = :tenant AND telefone = :number
      """;

  private static final String SAVE_SQL =
      """
      INSERT INTO conversas_whatsapp (estabelecimento_id, telefone, etapa, dados, atualizado_em)
      VALUES (:tenant, :number, :step, :data, :at)
      ON CONFLICT (estabelecimento_id, telefone)
      DO UPDATE SET etapa = EXCLUDED.etapa, dados = EXCLUDED.dados,
        atualizado_em = EXCLUDED.atualizado_em
      """;

  private static final String MARK_RECEIVED_SQL =
      """
      INSERT INTO whatsapp_mensagens_recebidas (estabelecimento_id, mensagem_id, recebida_em)
      VALUES (:tenant, :messageId, :at)
      ON CONFLICT DO NOTHING
      """;

  // Every lava-jato at once: housekeeping runs outside a request.
  private static final String FORGET_RECEIVED_SQL =
      "DELETE FROM whatsapp_mensagens_recebidas WHERE recebida_em < :before";

  private final Jdbi jdbi;

  public ConversationJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public Optional<String> activeEstablishmentName() {
    return jdbi.withHandle(
        handle -> handle.createQuery(ESTABLISHMENT_SQL).mapTo(String.class).findOne());
  }

  @Override
  public Optional<Conversation> load(String whatsappNumber) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(LOAD_SQL)
                .bind("number", whatsappNumber)
                .map(
                    (rs, ctx) ->
                        read(
                            rs.getString("etapa"),
                            rs.getString("dados"),
                            rs.getTimestamp("atualizado_em").toInstant()))
                .findOne()
                .flatMap(Optional::ofNullable));
  }

  @Override
  public void save(String whatsappNumber, Conversation conversation) {
    String data;
    try {
      data = JSON.writeValueAsString(conversation.draft());
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Conversation draft is not serializable", e);
    }
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(SAVE_SQL)
                .bind("number", whatsappNumber)
                .bind("step", conversation.step().name())
                .bind("data", data)
                .bind("at", conversation.updatedAt())
                .execute());
  }

  @Override
  public boolean markReceived(String messageId, Instant at) {
    return jdbi.withHandle(
            handle ->
                handle
                    .createUpdate(MARK_RECEIVED_SQL)
                    .bind("messageId", messageId)
                    .bind("at", at)
                    .execute())
        > 0;
  }

  @Override
  public int forgetReceivedBefore(Instant before) {
    return jdbi.withHandle(
        handle -> handle.createUpdate(FORGET_RECEIVED_SQL).bind("before", before).execute());
  }

  /** A row this version can't read starts over from the menu instead of failing the chat. */
  private static Conversation read(String step, String data, Instant updatedAt) {
    try {
      return new Conversation(
          ConversationStep.valueOf(step), JSON.readValue(data, ConversationDraft.class), updatedAt);
    } catch (JsonProcessingException | IllegalArgumentException e) {
      LOGGER.warn("event=whatsapp_conversation_unreadable reason={}", e.getMessage());
      return null;
    }
  }
}
