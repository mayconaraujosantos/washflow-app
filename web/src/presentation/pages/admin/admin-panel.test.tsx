import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { Platform } from '@/domain/usecases/platform'
import { AdminPanel } from './admin-panel'

const centro = {
  id: 'e1',
  slug: 'lava-centro',
  name: 'Lava Centro',
  active: true,
  createdAt: '2026-11-04T12:00:00Z',
  managerName: 'Ana',
  managerEmail: 'ana@centro.com',
}

const platform = () => ({
  search: vi.fn(),
  signIn: vi.fn(),
  list: vi.fn().mockResolvedValue([centro]),
  create: vi.fn().mockResolvedValue({
    ...centro,
    id: 'e2',
    slug: 'lava-norte',
    name: 'Lava Norte',
    managerEmail: 'bia@norte.com',
  }),
  setActive: vi.fn().mockResolvedValue({ ...centro, active: false }),
  resetManagerPassword: vi.fn().mockResolvedValue(centro),
})

const renderPanel = (admin: Platform) =>
  render(
    <MemoryRouter initialEntries={['/admin']}>
      <Routes>
        <Route path="/admin" element={<AdminPanel platform={admin} />} />
        <Route path="/login/admin" element={<p>login admin</p>} />
      </Routes>
    </MemoryRouter>,
  )

describe('AdminPanel', () => {
  beforeEach(() => {
    localStorage.clear()
    localStorage.setItem(
      'washflow:admin-session',
      JSON.stringify({ token: 't', user: { id: 'a1', profile: 'ADMIN' } }),
    )
  })

  it('sends visitors without an admin session to the admin login', async () => {
    localStorage.clear()
    renderPanel(platform())

    expect(await screen.findByText('login admin')).toBeInTheDocument()
  })

  it('lists lava-jatos with the customers link and suspends one', async () => {
    const admin = platform()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderPanel(admin)

    expect(await screen.findByText('Lava Centro')).toBeInTheDocument()
    expect(
      screen.getByText(`${window.location.origin}/?lavajato=lava-centro`),
    ).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Suspender' }))

    expect(admin.setActive).toHaveBeenCalledWith('e1', false)
    expect(await screen.findByText('Suspenso')).toBeInTheDocument()
  })

  it('creates a lava-jato with its manager', async () => {
    const admin = platform()
    renderPanel(admin)
    await screen.findByText('Lava Centro')

    await userEvent.click(
      screen.getByRole('button', { name: 'Novo lava-jato' }),
    )
    await userEvent.type(
      screen.getByLabelText('Nome do lava-jato'),
      'Lava Norte',
    )
    await userEvent.type(screen.getByLabelText('Nome do gerente'), 'Bia')
    await userEvent.type(
      screen.getByLabelText('E-mail de acesso do gerente'),
      'bia@norte.com',
    )
    await userEvent.type(
      screen.getByLabelText('Senha inicial (mínimo 8 caracteres)'),
      'senha-forte',
    )
    await userEvent.click(
      screen.getByRole('button', { name: 'Criar lava-jato' }),
    )

    expect(admin.create).toHaveBeenCalledWith(
      expect.objectContaining({
        name: 'Lava Norte',
        managerName: 'Bia',
        managerEmail: 'bia@norte.com',
        managerPassword: 'senha-forte',
      }),
    )
    expect(await screen.findByText(/Lava Norte criado/)).toBeInTheDocument()
  })

  it("resets the manager's lost password", async () => {
    const admin = platform()
    renderPanel(admin)
    await screen.findByText('Lava Centro')

    await userEvent.click(
      screen.getByRole('button', { name: 'Redefinir senha do gerente' }),
    )
    const save = screen.getByRole('button', { name: 'Salvar nova senha' })
    await userEvent.type(screen.getByLabelText('Nova senha de Ana'), 'curta')
    expect(save).toBeDisabled()
    await userEvent.type(
      screen.getByLabelText('Nova senha de Ana'),
      '-mas-agora-longa',
    )
    await userEvent.click(save)

    expect(admin.resetManagerPassword).toHaveBeenCalledWith(
      'e1',
      'curta-mas-agora-longa',
    )
    expect(
      await screen.findByText(/Senha de Ana \(Lava Centro\) redefinida/),
    ).toBeInTheDocument()
    expect(screen.queryByLabelText('Nova senha de Ana')).not.toBeInTheDocument()
  })
})
