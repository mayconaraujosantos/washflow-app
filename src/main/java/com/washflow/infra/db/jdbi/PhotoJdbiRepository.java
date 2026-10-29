package com.washflow.infra.db.jdbi;

import com.washflow.data.protocols.db.DeleteServiceOrderPhotoRepository;
import com.washflow.data.protocols.db.LoadServiceOrderPhotoRepository;
import com.washflow.data.protocols.db.SaveServiceOrderPhotoRepository;
import com.washflow.domain.entities.ServiceOrderPhoto;
import com.washflow.domain.entities.ServiceOrderPhoto.PhotoType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.StatementContext;

public class PhotoJdbiRepository
    implements SaveServiceOrderPhotoRepository,
        LoadServiceOrderPhotoRepository,
        DeleteServiceOrderPhotoRepository {

  private static final String INSERT_SQL =
      """
      INSERT INTO service_order_photo
        (id, atendimento_id, tipo, imagem_data, descricao, criado_em, criado_por)
      VALUES
        (:id, :serviceOrderId, :type, :imageData, :description, :createdAt, :createdBy)
      """;

  private static final String SELECT_BY_ORDER_SQL =
      """
      SELECT id, atendimento_id, tipo, imagem_data, descricao, criado_em, criado_por
      FROM service_order_photo
      WHERE atendimento_id = :serviceOrderId
      ORDER BY criado_em ASC
      """;

  private static final String DELETE_BY_ID_SQL = "DELETE FROM service_order_photo WHERE id = :id";

  private final Jdbi jdbi;

  public PhotoJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  private ServiceOrderPhoto mapRow(ResultSet rs, StatementContext ctx) throws SQLException {
    UUID id = UUID.fromString(rs.getString("id"));
    UUID serviceOrderId = UUID.fromString(rs.getString("atendimento_id"));
    PhotoType type = PhotoType.valueOf(rs.getString("tipo"));
    String imageData = rs.getString("imagem_data");
    String description = rs.getString("descricao");
    Instant createdAt = rs.getTimestamp("criado_em").toInstant();
    UUID createdBy = rs.getObject("criado_por", UUID.class);
    return new ServiceOrderPhoto(
        id, serviceOrderId, type, imageData, description, createdAt, createdBy);
  }

  @Override
  public ServiceOrderPhoto save(ServiceOrderPhoto photo) {
    jdbi.useHandle(
        handle -> {
          handle
              .createUpdate(INSERT_SQL)
              .bind("id", photo.id())
              .bind("serviceOrderId", photo.serviceOrderId())
              .bind("type", photo.type().name())
              .bind("imageData", photo.imageData())
              .bind("description", photo.description())
              .bind("createdAt", photo.createdAt())
              .bind("createdBy", photo.createdBy())
              .execute();
        });
    return photo;
  }

  @Override
  public List<ServiceOrderPhoto> loadByServiceOrderId(UUID serviceOrderId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(SELECT_BY_ORDER_SQL)
                .bind("serviceOrderId", serviceOrderId)
                .map(this::mapRow)
                .list());
  }

  @Override
  public void deleteById(UUID photoId) {
    jdbi.useHandle(handle -> handle.createUpdate(DELETE_BY_ID_SQL).bind("id", photoId).execute());
  }
}
