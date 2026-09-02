# Manual de Engenharia de Software e Arquitetura — Chatbot MMEEBB UNIPAM

## 1. Princípios SOLID e Clean Architecture

A Clean Architecture de Robert C. Martin visa a independência de frameworks, testabilidade e separação de preocupações em camadas concêntricas (Entidades, Casos de Uso, Gateways/Controllers, Frameworks/Drivers).

### Princípios SOLID:
1. **Single Responsibility Principle (SRP)**: Uma classe deve ter um, e apenas um, motivo para mudar.
2. **Open/Closed Principle (OCP)**: Entidades de software devem ser abertas para extensão, mas fechadas para modificação.
3. **Liskov Substitution Principle (LSP)**: Objetos de uma superclasse devem ser substituíveis por objetos de subclasses sem quebrar o comportamento do sistema.
4. **Interface Segregation Principle (ISP)**: Clientes não devem ser forçados a depender de interfaces que não utilizam. Múltiplas interfaces específicas são preferíveis a uma interface genérica inchada.
5. **Dependency Inversion Principle (DIP)**: Módulos de alto nível não devem depender de módulos de baixo nível. Ambos devem depender de abstrações.

---

## 2. Padrões de Resiliência em Microsserviços e Sistemas Distribuídos

### Circuit Breaker Pattern (Resilience4j):
Protege o sistema contra falhas em cascata monitorando chamadas para serviços externos.
- **Closed (Fechado)**: Fluxo normal. Todas as requisições são enviadas ao serviço dependente.
- **Open (Aberto)**: Taxa de erros superou o limite configurado (ex: 50%). Novas requisições falham imediatamente (CallNotPermittedException) sem chamar o serviço remoto, acionando métodos de fallback com baixa latência.
- **Half-Open (Semi-aberto)**: Após um período de espera (waitDurationInOpenState), permite que um número restrito de requisições de teste passe para avaliar se o serviço remoto se recuperou.
