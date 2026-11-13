package com.washflow.application.modules;

import com.washflow.application.AppConfig;
import com.washflow.data.usecases.serviceorder.DbCancelServiceOrder;
import com.washflow.data.usecases.serviceorder.DbCheckInServiceOrder;
import com.washflow.data.usecases.serviceorder.DbCompleteServiceOrder;
import com.washflow.data.usecases.serviceorder.DbDeleteServiceOrderPhoto;
import com.washflow.data.usecases.serviceorder.DbFinalizeServiceOrder;
import com.washflow.data.usecases.serviceorder.DbGetServiceOrderById;
import com.washflow.data.usecases.serviceorder.DbListServiceOrderChecklists;
import com.washflow.data.usecases.serviceorder.DbListServiceOrderPhotos;
import com.washflow.data.usecases.serviceorder.DbPauseServiceOrder;
import com.washflow.data.usecases.serviceorder.DbRefundServiceOrder;
import com.washflow.data.usecases.serviceorder.DbResumeServiceOrder;
import com.washflow.data.usecases.serviceorder.DbSaveServiceOrderChecklist;
import com.washflow.data.usecases.serviceorder.DbSaveServiceOrderPhoto;
import com.washflow.data.usecases.serviceorder.DbStartServiceOrder;
import com.washflow.domain.usecases.serviceorder.CancelServiceOrder;
import com.washflow.domain.usecases.serviceorder.CheckInServiceOrder;
import com.washflow.domain.usecases.serviceorder.CompleteServiceOrder;
import com.washflow.domain.usecases.serviceorder.DeleteServiceOrderPhoto;
import com.washflow.domain.usecases.serviceorder.FinalizeServiceOrder;
import com.washflow.domain.usecases.serviceorder.GetServiceOrderById;
import com.washflow.domain.usecases.serviceorder.ListServiceOrderChecklists;
import com.washflow.domain.usecases.serviceorder.ListServiceOrderPhotos;
import com.washflow.domain.usecases.serviceorder.PauseServiceOrder;
import com.washflow.domain.usecases.serviceorder.RefundServiceOrder;
import com.washflow.domain.usecases.serviceorder.ResumeServiceOrder;
import com.washflow.domain.usecases.serviceorder.SaveServiceOrderChecklist;
import com.washflow.domain.usecases.serviceorder.SaveServiceOrderPhoto;
import com.washflow.domain.usecases.serviceorder.StartServiceOrder;
import com.washflow.infra.db.jdbi.pricing.ServicePriceJdbiRepository;
import com.washflow.infra.db.jdbi.serviceorder.ChecklistJdbiRepository;
import com.washflow.infra.db.jdbi.serviceorder.PhotoJdbiRepository;
import com.washflow.infra.db.jdbi.serviceorder.ServiceOrderJdbiRepository;
import com.washflow.presentation.controllers.serviceorder.CancelServiceOrderController;
import com.washflow.presentation.controllers.serviceorder.CheckInServiceOrderController;
import com.washflow.presentation.controllers.serviceorder.CompleteServiceOrderController;
import com.washflow.presentation.controllers.serviceorder.DeleteServiceOrderPhotoController;
import com.washflow.presentation.controllers.serviceorder.FinalizeServiceOrderController;
import com.washflow.presentation.controllers.serviceorder.GetServiceOrderByIdController;
import com.washflow.presentation.controllers.serviceorder.ListServiceOrderChecklistsController;
import com.washflow.presentation.controllers.serviceorder.ListServiceOrderPhotosController;
import com.washflow.presentation.controllers.serviceorder.PauseServiceOrderController;
import com.washflow.presentation.controllers.serviceorder.RefundServiceOrderController;
import com.washflow.presentation.controllers.serviceorder.ResumeServiceOrderController;
import com.washflow.presentation.controllers.serviceorder.SaveServiceOrderChecklistController;
import com.washflow.presentation.controllers.serviceorder.SaveServiceOrderPhotoController;
import com.washflow.presentation.controllers.serviceorder.StartServiceOrderController;
import com.washflow.presentation.protocols.Controller;
import com.washflow.validation.protocols.Validation;
import com.washflow.validation.validators.RequiredFieldValidation;
import com.washflow.validation.validators.ValidationComposite;
import java.util.List;
import org.jdbi.v3.core.Jdbi;

/** Composition root of the service order lifecycle: builds its use cases and controllers. */
public final class ServiceOrderModule {

  private ServiceOrderModule() {}

  public static CancelServiceOrder cancelServiceOrder(Jdbi jdbi) {
    ServiceOrderJdbiRepository repository = new ServiceOrderJdbiRepository(jdbi);
    return new DbCancelServiceOrder(repository);
  }

  /**
   * Swapping JDBI for Hibernate later means changing only the {@code new
   * ServiceOrderJdbiRepository(...)} call here - {@code DbCheckInServiceOrder} depends on the
   * {@code data.protocols.db} interfaces, not on this concrete class.
   */
  public static CheckInServiceOrder checkInServiceOrder(Jdbi jdbi) {
    ServiceOrderJdbiRepository serviceOrderRepository = new ServiceOrderJdbiRepository(jdbi);
    return new DbCheckInServiceOrder(serviceOrderRepository, serviceOrderRepository);
  }

  /**
   * Swapping JDBI for Hibernate later means changing only the {@code new
   * ServiceOrderJdbiRepository(...)} call here - {@code DbCompleteServiceOrder} depends on the
   * {@code data.protocols.db} interfaces, not on this concrete class.
   */
  public static CompleteServiceOrder completeServiceOrder(Jdbi jdbi) {
    ServiceOrderJdbiRepository serviceOrderRepository = new ServiceOrderJdbiRepository(jdbi);
    return new DbCompleteServiceOrder(serviceOrderRepository, serviceOrderRepository);
  }

  public static DeleteServiceOrderPhoto deleteServiceOrderPhoto(Jdbi jdbi) {
    PhotoJdbiRepository repository = new PhotoJdbiRepository(jdbi);
    return new DbDeleteServiceOrderPhoto(repository);
  }

  /**
   * Swapping JDBI for Hibernate later means changing only the {@code new *JdbiRepository(...)}
   * calls here - {@code DbFinalizeServiceOrder} depends on the {@code data.protocols.db}
   * interfaces, not on these concrete classes.
   */
  public static FinalizeServiceOrder finalizeServiceOrder(Jdbi jdbi) {
    ServiceOrderJdbiRepository serviceOrderRepository = new ServiceOrderJdbiRepository(jdbi);
    return new DbFinalizeServiceOrder(
        serviceOrderRepository, new ServicePriceJdbiRepository(jdbi), serviceOrderRepository);
  }

  /**
   * Swapping JDBI for Hibernate later means changing only the {@code new
   * ServiceOrderJdbiRepository(...)} call here - {@code DbGetServiceOrderById} depends on the
   * {@code data.protocols.db} interfaces, not on this concrete class.
   */
  public static GetServiceOrderById getServiceOrderById(Jdbi jdbi) {
    return new DbGetServiceOrderById(new ServiceOrderJdbiRepository(jdbi));
  }

  /**
   * Swapping JDBI for Hibernate later means changing only the {@code new
   * ChecklistJdbiRepository(...)} call here - {@code DbListServiceOrderChecklists} depends on the
   * {@code data.protocols.db} interfaces, not on this concrete class.
   */
  public static ListServiceOrderChecklists listServiceOrderChecklists(Jdbi jdbi) {
    ChecklistJdbiRepository repository = new ChecklistJdbiRepository(jdbi);
    return new DbListServiceOrderChecklists(repository);
  }

  public static ListServiceOrderPhotos listServiceOrderPhotos(Jdbi jdbi) {
    PhotoJdbiRepository repository = new PhotoJdbiRepository(jdbi);
    return new DbListServiceOrderPhotos(repository);
  }

  public static PauseServiceOrder pauseServiceOrder(Jdbi jdbi) {
    ServiceOrderJdbiRepository repository = new ServiceOrderJdbiRepository(jdbi);
    return new DbPauseServiceOrder(repository);
  }

  public static RefundServiceOrder refundServiceOrder(Jdbi jdbi) {
    ServiceOrderJdbiRepository repository = new ServiceOrderJdbiRepository(jdbi);
    return new DbRefundServiceOrder(repository);
  }

  public static ResumeServiceOrder resumeServiceOrder(Jdbi jdbi) {
    ServiceOrderJdbiRepository repository = new ServiceOrderJdbiRepository(jdbi);
    return new DbResumeServiceOrder(repository);
  }

  /**
   * Swapping JDBI for Hibernate later means changing only the {@code new
   * ChecklistJdbiRepository(...)} call here - {@code DbSaveServiceOrderChecklist} depends on the
   * {@code data.protocols.db} interfaces, not on this concrete class.
   */
  public static SaveServiceOrderChecklist saveServiceOrderChecklist(Jdbi jdbi) {
    ChecklistJdbiRepository repository = new ChecklistJdbiRepository(jdbi);
    return new DbSaveServiceOrderChecklist(repository);
  }

  public static SaveServiceOrderPhoto saveServiceOrderPhoto(Jdbi jdbi) {
    PhotoJdbiRepository repository = new PhotoJdbiRepository(jdbi);
    return new DbSaveServiceOrderPhoto(repository);
  }

  /**
   * Swapping JDBI for Hibernate later means changing only the {@code new
   * ServiceOrderJdbiRepository(...)} call here - {@code DbStartServiceOrder} depends on the {@code
   * data.protocols.db} interfaces, not on this concrete class.
   */
  public static StartServiceOrder startServiceOrder(Jdbi jdbi) {
    ServiceOrderJdbiRepository serviceOrderRepository = new ServiceOrderJdbiRepository(jdbi);
    return new DbStartServiceOrder(serviceOrderRepository, serviceOrderRepository);
  }

  public static Controller cancelServiceOrderController(Jdbi jdbi) {
    return new CancelServiceOrderController(cancelServiceOrder(jdbi));
  }

  public static Controller checkInServiceOrderController(Jdbi jdbi) {
    return new CheckInServiceOrderController(checkInServiceOrder(jdbi));
  }

  /** The customer also gets a WhatsApp notice when the server has WhatsApp (WhatsappModule). */
  public static Controller completeServiceOrderController(Jdbi jdbi, AppConfig appConfig) {
    return new CompleteServiceOrderController(
        WhatsappModule.notifying(completeServiceOrder(jdbi), jdbi, appConfig));
  }

  public static Controller deleteServiceOrderPhotoController(Jdbi jdbi) {
    return new DeleteServiceOrderPhotoController(deleteServiceOrderPhoto(jdbi));
  }

  public static Controller finalizeServiceOrderController(Jdbi jdbi) {
    return new FinalizeServiceOrderController(finalizeServiceOrder(jdbi));
  }

  public static Controller listServiceOrderChecklistsController(Jdbi jdbi) {
    return new ListServiceOrderChecklistsController(listServiceOrderChecklists(jdbi));
  }

  public static Controller listServiceOrderPhotosController(Jdbi jdbi) {
    return new ListServiceOrderPhotosController(listServiceOrderPhotos(jdbi));
  }

  public static Controller getServiceOrderByIdController(Jdbi jdbi) {
    return new GetServiceOrderByIdController(getServiceOrderById(jdbi));
  }

  public static Controller pauseServiceOrderController(Jdbi jdbi) {
    return new PauseServiceOrderController(pauseServiceOrder(jdbi));
  }

  public static Controller refundServiceOrderController(Jdbi jdbi) {
    return new RefundServiceOrderController(refundServiceOrder(jdbi));
  }

  public static Controller resumeServiceOrderController(Jdbi jdbi) {
    return new ResumeServiceOrderController(resumeServiceOrder(jdbi));
  }

  public static Controller saveServiceOrderChecklistController(Jdbi jdbi) {
    return new SaveServiceOrderChecklistController(saveServiceOrderChecklist(jdbi));
  }

  public static Controller saveServiceOrderPhotoController(Jdbi jdbi) {
    return new SaveServiceOrderPhotoController(saveServiceOrderPhoto(jdbi));
  }

  public static Controller startServiceOrderController(Jdbi jdbi) {
    Validation validation =
        new ValidationComposite(List.of(new RequiredFieldValidation("washerId")));

    return new StartServiceOrderController(startServiceOrder(jdbi), validation);
  }
}
