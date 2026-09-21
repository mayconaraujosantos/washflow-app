import { HttpClient, HttpStatusCode } from '@/data/protocols/http/http-client'
import { AuthenticateWasher } from '@/domain/usecases/authenticate-washer'

type ErrorBody = { error?: string }

export class RemoteAuthenticateWasher implements AuthenticateWasher {
  constructor(
    private readonly url: string,
    private readonly httpClient: HttpClient<AuthenticateWasher.Model>,
  ) {}

  async auth(
    params: AuthenticateWasher.Params,
  ): Promise<AuthenticateWasher.Model> {
    const httpResponse = await this.httpClient.request({
      url: this.url,
      method: 'post',
      body: params,
    })

    switch (httpResponse.statusCode) {
      case HttpStatusCode.ok:
        return httpResponse.body as AuthenticateWasher.Model
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
