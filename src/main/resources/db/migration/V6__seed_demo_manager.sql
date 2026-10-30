-- Bootstraps the first GERENTE. AuthenticateUser never self-registers a new
-- manager (see ManagerSelfRegistrationNotAllowedError) - every other manager
-- has to come from this one promoting them through
-- PATCH /api/users/{id}/profile, so without this seed there'd be no manager
-- able to promote anyone at all.

INSERT INTO usuarios (id, nome, telefone, perfil)
VALUES ('44444444-4444-4444-4444-444444444444', 'Gerente Demo', '11999993333', 'GERENTE');
