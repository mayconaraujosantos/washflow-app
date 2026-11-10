import { expect, test, type Page } from '@playwright/test'
import { cardWith, expectMessage, Phones } from './support/phones'

/**
 * Roteiro: instalar o Washflow em dois lava-jatos e dar acesso a gerentes,
 * lavadores e clientes - feito como uma pessoa faria, tela a tela.
 *
 *  1. O administrador configura a plataforma e cria o Lava Raio e o Lava Sol.
 *  2. Cada gerente entra com e-mail e senha e cadastra um lavador (crachá).
 *  3. Cliente do Lava Raio abre o link do balcão, cria a conta, cadastra o
 *     carro e agenda. O gerente recebe, o lavador (PIN) lava, o cliente
 *     acompanha e o gerente recebe o pagamento.
 *  4. No Lava Sol, cliente sem app: entrada pela placa, ticket sem login,
 *     lavador entra lendo o QR do crachá.
 *  5. Isolamento: a conta de um lava-jato não entra no outro, e um pátio não
 *     vê os carros do outro.
 */

const PASSWORD = 'senha-forte-123'

type Shop = {
  name: string
  slug?: string
  manager: { name: string; email: string }
  washer: {
    name: string
    phone: string
    id?: string
    pin?: string
    badgeToken?: string
  }
}

const raio: Shop = {
  name: 'Lava Raio',
  manager: { name: 'Rita Raio', email: 'rita@lavaraio.test' },
  washer: { name: 'Lucas Lavador', phone: '92981110001' },
}
const sol: Shop = {
  name: 'Lava Sol',
  manager: { name: 'Sergio Sol', email: 'sergio@lavasol.test' },
  washer: { name: 'Bruno Lavador', phone: '92981110002' },
}

const customer = {
  name: 'Ana Cliente',
  phone: '92991234567',
  plate: 'RAI1A23',
  model: 'Onix',
  color: 'Prata',
}
const walkIn = {
  name: 'Carlos Balcão',
  phone: '92997654321',
  plate: 'SOL2B34',
  model: 'HB20',
  color: 'Branco',
}

test.describe.configure({ mode: 'serial' })

let phones: Phones
let admin: Page
const managerOf = new Map<Shop, Page>()
let customerPhone: Page
let ticketToken = ''

test.beforeAll(async ({ browser }) => {
  phones = new Phones(browser)
})

test.afterAll(async () => {
  await phones.closeAll()
})

test('1. administrador configura a plataforma e cria os dois lava-jatos', async () => {
  admin = await phones.of('administrador')

  await test.step('instalação nova abre o assistente /configurar', async () => {
    await admin.goto('/')
    await expect(admin).toHaveURL(/\/configurar$/)
    await admin.getByLabel('Seu nome').fill('Maycon Admin')
    await admin.getByLabel('E-mail de acesso').fill('admin@washflow.test')
    await admin.getByLabel('Senha (mínimo 8 caracteres)').fill(PASSWORD)
    await admin.getByRole('button', { name: 'Criar administrador' }).click()
    await expect(admin).toHaveURL(/\/admin$/)
  })

  for (const shop of [raio, sol]) {
    await test.step(`cria ${shop.name} com o gerente ${shop.manager.name}`, async () => {
      await admin.getByRole('button', { name: 'Novo lava-jato' }).click()
      const form = admin.getByRole('form', { name: 'Novo lava-jato' })
      await form.getByLabel('Nome do lava-jato').fill(shop.name)
      await form.getByLabel('Nome do gerente').fill(shop.manager.name)
      await form
        .getByLabel('E-mail de acesso do gerente')
        .fill(shop.manager.email)
      await form
        .getByLabel('Senha inicial (mínimo 8 caracteres)')
        .fill(PASSWORD)
      await form.getByRole('button', { name: 'Criar lava-jato' }).click()
      await expectMessage(admin, `${shop.name} criado.`)

      const card = cardWith(admin, shop.name)
      await expect(card).toContainText(shop.manager.email)
      const link = (await card.locator('code').textContent()) ?? ''
      shop.slug = new URL(link).searchParams.get('lavajato') ?? undefined
      expect(shop.slug, `link dos clientes de ${shop.name}`).toBeTruthy()
    })
  }
})

test('2. cada gerente entra e cadastra o lavador com crachá', async () => {
  for (const shop of [raio, sol]) {
    const manager = await phones.of(`gerente-${shop.slug}`)
    managerOf.set(shop, manager)

    await test.step(`${shop.manager.name} entra com e-mail e senha`, async () => {
      await manager.goto('/login/gerente')
      await manager.getByLabel('E-mail ou telefone').fill(shop.manager.email)
      await manager.getByLabel('Senha', { exact: true }).fill(PASSWORD)
      await manager.getByRole('button', { name: 'Entrar', exact: true }).click()
      await expect(
        manager.getByText(`Bem-vindo, ${shop.manager.name}!`),
      ).toBeVisible()
      await manager.getByText('Abrir painel do gerente').click()
      await expect(manager).toHaveURL(/:\d+\/gerente$/)
    })

    await test.step(`cadastra ${shop.washer.name} e recebe o crachá`, async () => {
      await manager.getByRole('button', { name: 'Abrir menu' }).click()
      await manager.getByRole('link', { name: 'Equipe e acessos' }).click()
      await manager.getByLabel('Nome', { exact: true }).fill(shop.washer.name)
      await manager
        .getByLabel('Telefone', { exact: true })
        .fill(shop.washer.phone)

      const badgeIssued = manager.waitForResponse(
        (response) =>
          /\/api\/users\/[^/]+\/badge$/.test(response.url()) &&
          response.request().method() === 'POST',
      )
      await manager.getByRole('button', { name: 'Cadastrar lavador' }).click()
      const badgeResponse = await badgeIssued
      shop.washer.id = /\/api\/users\/([^/]+)\/badge$/.exec(
        badgeResponse.url(),
      )![1]
      const badge = (await badgeResponse.json()) as {
        pin: string
        badgeToken: string
      }
      shop.washer.pin = badge.pin
      shop.washer.badgeToken = badge.badgeToken

      const sheet = manager.getByRole('dialog', { name: 'Crachá do lavador' })
      await expect(sheet).toContainText(shop.washer.name)
      await expect(sheet.locator('.badge-pin')).toHaveText(badge.pin)
      await expect(sheet.locator('svg').first()).toBeVisible() // QR do crachá
      await sheet.getByRole('button', { name: 'Fechar' }).click()
      await expect(cardWith(manager, shop.washer.phone)).toContainText(
        shop.washer.name,
      )
      await manager.getByRole('link', { name: 'Voltar' }).click()
      await expect(manager).toHaveURL(/:\d+\/gerente$/)
    })
  }
})

test('3. Lava Raio: cliente agenda pelo app e acompanha até a retirada', async () => {
  const manager = managerOf.get(raio)!
  customerPhone = await phones.of('cliente-ana')

  await test.step('abre o link do balcão e já vê o lava-jato certo', async () => {
    await customerPhone.goto(`/?lavajato=${raio.slug}`)
    await expect(customerPhone).not.toHaveURL(/lavajato=/) // tirado da barra
    await expect(customerPhone.getByText('Lava-jato:')).toContainText(
      new RegExp(`${raio.name}|${raio.slug}`),
    )
  })

  await test.step('cria a conta com nome, telefone e senha', async () => {
    await customerPhone.getByRole('button', { name: 'Criar conta' }).click()
    await customerPhone.getByLabel('Nome', { exact: true }).fill(customer.name)
    await customerPhone.getByLabel('Telefone').fill(customer.phone)
    await customerPhone.getByLabel('Senha (mínimo 8 caracteres)').fill(PASSWORD)
    await customerPhone.getByRole('button', { name: 'Criar uma conta' }).click()
    await expect(
      customerPhone.getByRole('heading', { name: 'Bem-vindo ao Washflow!' }),
    ).toBeVisible()
    await expect(customerPhone.getByText('segunda a sábado')).toHaveCount(0)
    await customerPhone
      .getByRole('button', { name: 'Perfeito, entendi, vou começar' })
      .click()
    await expect(customerPhone).toHaveURL(/\/cliente$/)
    await expect(customerPhone.getByText('Lava-jato:')).toContainText(raio.name)
  })

  await test.step('cadastra o carro', async () => {
    await customerPhone.getByRole('link', { name: 'Veículos' }).click()
    await customerPhone.getByLabel('Placa').fill(customer.plate)
    await customerPhone.getByLabel('Modelo (opcional)').fill(customer.model)
    await customerPhone.getByLabel('Cor (opcional)').fill(customer.color)
    await customerPhone
      .getByRole('button', { name: 'Adicionar veículo' })
      .click()
    await expect(cardWith(customerPhone, customer.plate)).toBeVisible()
  })

  await test.step('agenda uma Lavagem Completa no primeiro horário livre', async () => {
    await customerPhone.getByRole('link', { name: 'Início' }).click()
    await customerPhone
      .getByRole('link', { name: 'Agendar', exact: true })
      .click()
    await expect(customerPhone.getByText('Agendando no')).toContainText(
      raio.name,
    )
    await customerPhone
      .getByRole('radio', { name: new RegExp(customer.plate) })
      .click()
    // Um serviço (radio) ou vários (checkbox), conforme a versão da tela.
    await customerPhone
      .locator('[role="checkbox"], [role="radio"]')
      .filter({ hasText: 'Lavagem Completa' })
      .click()
    const firstFreeSlot = customerPhone
      .getByRole('radiogroup', { name: 'Horário' })
      .locator('button:enabled')
      .first()
    await firstFreeSlot.click()
    await customerPhone
      .getByRole('button', { name: 'Agendar', exact: true })
      .click()
    await expect(customerPhone.locator('.notice-success')).toBeVisible()
    await customerPhone.getByRole('link', { name: 'Voltar' }).click()
    await customerPhone.getByRole('link', { name: 'Agendamentos' }).click()
    await expect(cardWith(customerPhone, customer.plate)).toContainText(
      'Agendado',
    )
  })

  await test.step('gerente vê o agendamento e recebe o carro no pátio', async () => {
    await manager.getByRole('link', { name: 'Pátio' }).click()
    const order = cardWith(manager, customer.plate)
    await expect(order).toBeVisible()
    await order.getByRole('button', { name: 'Receber no pátio' }).click()
    await expectMessage(manager, `${customer.plate} recebido no pátio.`)
  })

  const washer = await phones.of(`lavador-${raio.slug}`)
  await test.step('lavador entra com telefone + PIN do crachá', async () => {
    await washer.goto('/login/lavador')
    await washer.getByLabel('Telefone').fill(raio.washer.phone)
    await washer.getByLabel('PIN').fill(raio.washer.pin!)
    await washer.getByRole('button', { name: 'Entrar com PIN' }).click()
    await expect(washer).toHaveURL(/:\d+\/lavador$/)
  })

  await test.step('lavador pega o carro da fila, lava e conclui', async () => {
    const queued = cardWith(washer, customer.plate)
    await queued.getByRole('button', { name: 'Iniciar lavagem' }).click()
    await expectMessage(washer, `Lavagem de ${customer.plate} iniciada.`)
    await washer.getByRole('link', { name: 'Minhas lavagens' }).click()
    await cardWith(washer, customer.plate)
      .getByRole('button', { name: 'Concluir lavagem' })
      .click()
    await expectMessage(washer, 'Lavagem concluída.')
  })

  await test.step('cliente vê no painel que o carro está pronto', async () => {
    await customerPhone.getByRole('link', { name: 'Início' }).click()
    await expect(
      customerPhone.getByText('Seu veículo está pronto para retirada.'),
    ).toBeVisible()
  })

  await test.step('gerente recebe no Pix e finaliza', async () => {
    await manager.getByRole('link', { name: 'Pátio' }).click()
    await cardWith(manager, customer.plate)
      .getByRole('button', { name: 'Receber e finalizar' })
      .click()
    const sheet = manager.getByRole('dialog', { name: 'Receber pagamento' })
    await expect(sheet).toContainText('R$')
    await sheet.getByRole('button', { name: 'Pix' }).click()
    await sheet.getByRole('button', { name: 'Confirmar e finalizar' }).click()
    await expectMessage(manager, `${customer.plate} finalizado.`)
  })

  await test.step('cliente vê o atendimento finalizado', async () => {
    await customerPhone.getByRole('link', { name: 'Agendamentos' }).click()
    await expect(cardWith(customerPhone, customer.plate)).toContainText(
      'Finalizado',
    )
  })
})

test('4. Lava Sol: cliente sem app acompanha pelo ticket, lavador entra pelo QR', async () => {
  const manager = managerOf.get(sol)!

  await test.step('gerente registra a entrada pela placa', async () => {
    await manager.getByRole('link', { name: 'Início' }).click()
    await manager.getByRole('link', { name: 'Entrada' }).click()
    await manager
      .getByRole('button', { name: 'Adicionar novo cliente' })
      .click()
    const sheet = manager.getByRole('dialog', { name: 'Novo cliente' })
    await sheet.getByLabel('Nome').fill(walkIn.name)
    await sheet.getByLabel('Telefone (WhatsApp)').fill(walkIn.phone)
    await sheet.getByLabel('Placa').fill(walkIn.plate)
    await sheet.getByLabel('Descrição do carro').fill(walkIn.model)
    await sheet.getByLabel('Cor').fill(walkIn.color)
    await sheet.getByRole('button', { name: 'Salvar cliente' }).click()

    await manager
      .getByRole('button', { name: 'Adicionar produto ou serviço' })
      .click()
    await manager
      .getByRole('dialog', { name: 'Selecione o produto ou serviço' })
      .getByRole('button', { name: /Lavagem Simples/ })
      .click()

    const ticketIssued = manager.waitForResponse(
      (response) =>
        /\/api\/service-orders\/[^/]+\/ticket$/.test(response.url()) &&
        response.request().method() === 'POST',
    )
    await manager.getByRole('button', { name: 'Registrar entrada' }).click()
    ticketToken = ((await (await ticketIssued).json()) as { token: string })
      .token
    const ticket = manager.getByRole('dialog', { name: 'Ticket do cliente' })
    await expect(ticket).toContainText(walkIn.plate)
    await expect(
      ticket.getByRole('link', { name: 'Enviar no WhatsApp' }),
    ).toBeVisible()
    await ticket.getByRole('button', { name: 'Fechar' }).click()
  })

  const visitor = await phones.of('cliente-balcao')
  await test.step('cliente abre o ticket sem login', async () => {
    await visitor.goto(`/ticket/${ticketToken}`)
    await expect(visitor.getByText(sol.name)).toBeVisible()
    await expect(visitor.getByText(walkIn.plate)).toBeVisible()
    await expect(
      visitor.getByRole('heading', { name: 'No pátio, aguardando um lavador' }),
    ).toBeVisible()
    await expect(visitor.getByText(walkIn.phone)).toHaveCount(0) // sem dados do cliente
  })

  const washer = await phones.of(`lavador-${sol.slug}`)
  await test.step('lavador entra lendo o QR do crachá (link do QR)', async () => {
    await washer.goto(
      `/login/lavador?cracha=${encodeURIComponent(sol.washer.badgeToken!)}`,
    )
    await expect(washer).toHaveURL(/:\d+\/lavador$/)
  })

  await test.step('lavador lava e conclui; o ticket mostra "pronto"', async () => {
    await cardWith(washer, walkIn.plate)
      .getByRole('button', { name: 'Iniciar lavagem' })
      .click()
    await expect(
      visitor.getByRole('heading', { name: 'Seu veículo está sendo lavado' }),
    ).toBeVisible()
    await washer.getByRole('link', { name: 'Minhas lavagens' }).click()
    await cardWith(washer, walkIn.plate)
      .getByRole('button', { name: 'Concluir lavagem' })
      .click()
    await expect(
      visitor.getByRole('heading', { name: 'Pronto para retirada!' }),
    ).toBeVisible()
  })

  await test.step('gerente recebe em dinheiro; o ticket encerra', async () => {
    await manager.getByRole('link', { name: 'Pátio' }).click()
    await cardWith(manager, walkIn.plate)
      .getByRole('button', { name: 'Receber e finalizar' })
      .click()
    const sheet = manager.getByRole('dialog', { name: 'Receber pagamento' })
    await sheet.getByRole('button', { name: 'Dinheiro' }).click()
    await sheet.getByRole('button', { name: 'Confirmar e finalizar' }).click()
    await expectMessage(manager, `${walkIn.plate} finalizado.`)
    await expect(
      visitor.getByRole('heading', {
        name: 'Atendimento finalizado. Obrigado!',
      }),
    ).toBeVisible()
  })
})

test('5. cada lava-jato é isolado do outro', async () => {
  await test.step('a busca de veículos do Lava Raio não acha os carros do Lava Sol', async () => {
    const manager = managerOf.get(raio)!
    await manager.getByRole('link', { name: 'Início' }).click()
    await manager.getByRole('link', { name: 'Entrada' }).click()
    const search = manager.getByLabel('Buscar cliente')
    await search.fill(customer.plate)
    await expect(
      manager.getByRole('button', { name: new RegExp(`^${customer.plate}`) }),
    ).toBeVisible()
    await search.fill(walkIn.plate)
    await manager.waitForResponse((response) =>
      response.url().includes(`/api/operations/vehicles?query=${walkIn.plate}`),
    )
    await expect(
      manager.getByRole('button', { name: new RegExp(`^${walkIn.plate}`) }),
    ).toHaveCount(0)
    // Placa desconhecida aqui: o app só oferece cadastrá-la.
    await expect(
      manager.getByRole('button', { name: `Cadastrar placa ${walkIn.plate}` }),
    ).toBeVisible()
  })

  const other = await phones.of('cliente-ana-no-lava-sol')
  await test.step('a conta da Ana no Lava Raio não entra no Lava Sol', async () => {
    await other.goto(`/?lavajato=${sol.slug}`)
    await expect(other.getByText('Lava-jato:')).toContainText(
      new RegExp(`${sol.name}|${sol.slug}`),
    )
    await other.getByLabel('Telefone').fill(customer.phone)
    await other.getByLabel('Senha', { exact: true }).fill(PASSWORD)
    await other.getByRole('button', { name: 'Entrar', exact: true }).click()
    await expect(other.locator('.notice-error')).toBeVisible()
    await expect(other).not.toHaveURL(/\/cliente/)
  })

  await test.step('com o mesmo telefone, ela cria uma conta separada no Lava Sol', async () => {
    await other.getByRole('button', { name: 'Criar conta' }).click()
    await other.getByLabel('Nome', { exact: true }).fill(customer.name)
    await other.getByLabel('Telefone').fill(customer.phone)
    await other.getByLabel('Senha (mínimo 8 caracteres)').fill(PASSWORD)
    await other.getByRole('button', { name: 'Criar uma conta' }).click()
    await other
      .getByRole('button', { name: 'Perfeito, entendi, vou começar' })
      .click()
    await expect(other.getByText('Lava-jato:')).toContainText(sol.name)
    await other.getByRole('link', { name: 'Veículos' }).click()
    await expect(other.getByText(customer.plate)).toHaveCount(0) // carros são por lava-jato
  })

  await test.step('"Trocar" no painel sai da conta e volta para a escolha do lava-jato', async () => {
    await other.getByRole('link', { name: 'Início' }).click()
    await other.getByRole('button', { name: 'Trocar' }).click()
    await expect(
      other.getByRole('heading', { name: 'Qual é o seu lava-jato?' }),
    ).toBeVisible()
  })

  await test.step('o mesmo lavador trabalha nos dois: cada PIN abre o seu lava-jato', async () => {
    const manager = managerOf.get(sol)!
    await manager.goto('/gerente')
    await manager.getByRole('button', { name: 'Abrir menu' }).click()
    await manager.getByRole('link', { name: 'Equipe e acessos' }).click()
    await manager.getByLabel('Nome', { exact: true }).fill(raio.washer.name)
    await manager
      .getByLabel('Telefone', { exact: true })
      .fill(raio.washer.phone)
    const badgeIssued = manager.waitForResponse(
      (response) =>
        /\/api\/users\/[^/]+\/badge$/.test(response.url()) &&
        response.request().method() === 'POST',
    )
    await manager.getByRole('button', { name: 'Cadastrar lavador' }).click()
    const solBadge = await badgeIssued
    const lucasAtSol = /\/api\/users\/([^/]+)\/badge$/.exec(solBadge.url())![1]
    const solPin = ((await solBadge.json()) as { pin: string }).pin
    expect(lucasAtSol).not.toBe(raio.washer.id) // outra conta, no outro lava-jato
    await manager
      .getByRole('dialog', { name: 'Crachá do lavador' })
      .getByRole('button', { name: 'Fechar' })
      .click()

    for (const [pin, shop, accountId] of [
      [solPin, sol, lucasAtSol],
      [raio.washer.pin!, raio, raio.washer.id!],
    ] as const) {
      const washer = await phones.of(`lucas-pin-${shop.slug}`)
      await washer.goto('/login/lavador')
      await washer.getByLabel('Telefone').fill(raio.washer.phone)
      await washer.getByLabel('PIN').fill(pin)
      await washer.getByRole('button', { name: 'Entrar com PIN' }).click()
      await expect(washer).toHaveURL(/:\d+\/lavador$/)
      const session = await washer.evaluate(() =>
        JSON.stringify(Object.values(localStorage)),
      )
      expect(session, `PIN do ${shop.name}`).toContain(accountId)
    }
  })

  await test.step('lavador do Lava Raio não entra pelo crachá no Lava Sol com PIN errado', async () => {
    const washer = await phones.of('lavador-pin-errado')
    await washer.goto('/login/lavador')
    await washer.getByLabel('Telefone').fill(sol.washer.phone)
    await washer.getByLabel('PIN').fill(raio.washer.pin!)
    await washer.getByRole('button', { name: 'Entrar com PIN' }).click()
    await expect(washer.locator('.notice-error')).toBeVisible()
  })
})

test('6. quem esqueceu a senha volta a entrar', async () => {
  const NEW_PASSWORD = 'senha-nova-456'

  await test.step('gerente do Lava Raio cria uma senha nova para a Ana', async () => {
    const manager = managerOf.get(raio)!
    await manager.goto('/gerente')
    await manager.getByRole('button', { name: 'Abrir menu' }).click()
    await manager.getByRole('link', { name: 'Equipe e acessos' }).click()
    await manager
      .getByRole('button', { name: `Redefinir senha de ${customer.name}` })
      .click()
    await manager
      .getByLabel(`Nova senha de ${customer.name}`)
      .fill(NEW_PASSWORD)
    await manager.getByRole('button', { name: 'Salvar nova senha' }).click()
    await expectMessage(manager, `Senha de ${customer.name} redefinida.`)
    await expect(
      manager.getByRole('button', {
        name: `Redefinir senha de ${raio.manager.name}`,
      }),
    ).toHaveCount(0) // gerente não redefine senha de gerente
  })

  await test.step('Ana entra com a senha nova; a antiga não vale mais', async () => {
    const ana = await phones.of('cliente-ana-senha-nova')
    await ana.goto(`/?lavajato=${raio.slug}`)
    await ana.getByLabel('Telefone').fill(customer.phone)
    await ana.getByLabel('Senha', { exact: true }).fill(PASSWORD)
    await ana.getByRole('button', { name: 'Entrar', exact: true }).click()
    await expect(ana.locator('.notice-error')).toBeVisible()
    await ana.getByLabel('Senha', { exact: true }).fill(NEW_PASSWORD)
    await ana.getByRole('button', { name: 'Entrar', exact: true }).click()
    await expect(ana).toHaveURL(/\/cliente$/)
  })

  await test.step('administrador cria uma senha nova para o gerente do Lava Sol', async () => {
    await admin.goto('/admin')
    await cardWith(admin, sol.name)
      .getByRole('button', { name: 'Redefinir senha do gerente' })
      .click()
    await admin
      .getByLabel(`Nova senha de ${sol.manager.name}`)
      .fill(NEW_PASSWORD)
    await admin.getByRole('button', { name: 'Salvar nova senha' }).click()
    await expectMessage(
      admin,
      `Senha de ${sol.manager.name} (${sol.name}) redefinida.`,
    )
  })

  await test.step('gerente do Lava Sol entra com a senha nova', async () => {
    const manager = await phones.of('gerente-lava-sol-senha-nova')
    await manager.goto('/login/gerente')
    await manager.getByLabel('E-mail ou telefone').fill(sol.manager.email)
    await manager.getByLabel('Senha', { exact: true }).fill(NEW_PASSWORD)
    await manager.getByRole('button', { name: 'Entrar', exact: true }).click()
    await expect(
      manager.getByText(`Bem-vindo, ${sol.manager.name}!`),
    ).toBeVisible()
  })
})
