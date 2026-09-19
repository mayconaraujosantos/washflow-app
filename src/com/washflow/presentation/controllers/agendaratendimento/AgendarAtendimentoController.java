package com.washflow.presentation.controllers.agendaratendimento;

import com.washflow.domain.errors.ServicoPrecoNaoEncontradoError;
import com.washflow.domain.errors.VeiculoNaoEncontradoError;
import com.washflow.domain.usecases.AgendarAtendimento;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import com.washflow.validation.protocols.Validation;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

public class AgendarAtendimentoController implements Controller {

  private final AgendarAtendimento agendarAtendimento;
  private final Validation validation;

  public AgendarAtendimentoController(
      AgendarAtendimento agendarAtendimento, Validation validation) {
    this.agendarAtendimento = agendarAtendimento;
    this.validation = validation;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      Exception validationError = validation.validate(request.body());
      if (validationError != null) {
        return HttpHelper.badRequest(validationError);
      }

      UUID veiculoId = UUID.fromString((String) request.body().get("veiculoId"));
      int servicoPrecoId = ((Number) request.body().get("servicoPrecoId")).intValue();
      Instant dataAgendamento = Instant.parse((String) request.body().get("dataAgendamento"));

      var atendimento =
          agendarAtendimento.agendar(
              new AgendarAtendimento.Params(veiculoId, servicoPrecoId, dataAgendamento));

      return HttpHelper.ok(atendimento);
    } catch (DateTimeParseException | IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (VeiculoNaoEncontradoError | ServicoPrecoNaoEncontradoError e) {
      return HttpHelper.notFound(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
