package com.washflow.domain.usecases.user;

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
  int requestForCustomer(String phone);

  /**
   * @return seconds the code stays valid
   */
  int requestForManager(String email);

  void confirmForCustomer(String phone, String code, String newPassword);

  void confirmForManager(String email, String code, String newPassword);
}
