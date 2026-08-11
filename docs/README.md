# Documentação viva — como usar

Três tipos de documento, três momentos da vida de uma decisão:

| Tipo | Quando | Natureza |
| --- | --- | --- |
| **RFC** (`rfc/`) | Uma ideia ainda em discussão | Proposta. Aberta a comentários; pode ser rejeitada. |
| **ADR** (`adr/`) | A decisão foi tomada | Registro **imutável**. Não se edita um ADR aceito — escreve-se outro que o supersede (status `Deprecated` no antigo). |
| **SDD** (`sdd/`) | Sempre | Estado **atual** do sistema. Atualizado toda vez que um ADR muda o desenho. É o único documento que descreve "como é hoje". |

Fluxo: RFC (discussão) → ADR (decisão) → SDD (estado refletido).

Regras:

- Todo RFC/ADR nasce do `_template.md` da sua pasta, com o frontmatter completo.
- Numeração sequencial no id e no nome do arquivo: `0001-titulo-curto.md`.
- O SDD nunca contradiz um ADR aceito; se contradisser, um dos dois está errado — corrija no mesmo PR.
- `guidelines/` guarda regras de trabalho (estilo, testes, nomenclatura); `tech-debt.md` guarda dívidas conscientes com contexto.
