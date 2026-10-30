### 1.4 Regras de Negócio Estritas para o Agendamento (Foco MVP)

* **Horário de Funcionamento:** Segunda a Sábado, das 08:00 às 18:00.
* **Intervalo Mínimo:** Os agendamentos devem ser feitos com no mínimo 30 minutos de antecedência do horário atual.
* **Capacidade Máxima por Slot:** Padrão de 3 veículos por hora (configurável).
* **Resiliência de Notificação:** Falhas no envio de notificações de confirmação não devem estornar a transação de agendamento no banco de dados.
