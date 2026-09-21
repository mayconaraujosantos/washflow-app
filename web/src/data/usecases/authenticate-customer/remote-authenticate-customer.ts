import { HttpClient, HttpStatusCode } from '@/data/protocols/http/http-client'
import { AuthenticateCustomer } from '@/domain/usecases/authenticate-customer'

type ErrorBody = { error?: string }

export class RemoteAuthenticateCustomer implements AuthenticateCustomer {
  constructor(
    private readonly url: string,
    private readonly httpClient: HttpClient<AuthenticateCustomer.Model>,
  ) {}

  async auth(
    params: AuthenticateCustomer.Params,
  ): Promise<AuthenticateCustomer.Model> {
    const httpResponse = await this.httpClient.request({
      url: this.url,
      method: 'post',
      body: params,
    })

    switch (httpResponse.statusCode) {
      case HttpStatusCode.ok:
        return httpResponse.body as AuthenticateCustomer.Model
      case HttpStatusCode.badRequest:
        throw new Error(
          (httpResponse.body as ErrorBody)?.error ??
            'Campo obrigatório ausente',
        )
      case HttpStatusCode.conflict:
        throw new Error(
          (httpResponse.body as ErrorBody)?.error ??
            'Este telefone já está cadastrado com outro perfil',
        )
      default:
        throw new Error('Erro inesperado ao entrar')
    }
  }
}
