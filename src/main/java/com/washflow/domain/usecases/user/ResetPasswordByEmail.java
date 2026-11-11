package com.washflow.domain.usecases.user;

import com.washflow.domain.errors.auth.InvalidOtpError;
import com.washflow.domain.errors.user.EmailDeliveryFailedError;
import com.washflow.domain.errors.user.EmailResetUnavailableError;

/**
 * "Esqueci a senha" without the counter: a 6-digit code goes to the account's e-mail and, with it,
 * the person sets a new password.
 *
 * <p>Customers ask by phone (inside the lava-jato they picked) and the code goes to the recovery
 * e-mail saved in "Meus dados"; managers ask by their login e-mail. The request answers the same
 * whether the account exists, has an e-mail or asked too recently, so it reveals nothing.
 */
public interface ResetPasswordByEmail {

  boolean available();

  /**
   * @return seconds the code stays valid
   */
  int requestForCustomer(String phone) throws EmailResetUnavailableError, EmailDeliveryFailedError;

  /**
   * @return seconds the code stays valid
   */
  int requestForManager(String email) throws EmailResetUnavailableError, EmailDeliveryFailedError;

  void confirmForCustomer(String phone, String code, String newPassword) throws InvalidOtpError;

  void confirmForManager(String email, String code, String newPassword) throws InvalidOtpError;
}
