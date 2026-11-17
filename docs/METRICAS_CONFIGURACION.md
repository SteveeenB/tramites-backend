# Métricas de gestión de configuración

| | |
|---|---|
| **Tarea** | TP-172 · AUD-06 (auditoría IEEE 828:2022, hallazgo 6) |
| **Medición** | 16/11/2026, sobre los dos repositorios |
| **Frecuencia** | En cada reunión ordinaria del CCB (semanal). El resultado queda en el acta |

## 1. Métricas y valores actuales

| ID | Métrica | Cómo se calcula | Meta | Backend | Frontend |
|---|---|---|---|---|---|
| M1 | Cambios en `main`/`develop` que entraron por PR | 100 % − commits directos (`--first-parent --no-merges`) desde el 03/11/2026 | 100 % | **100 %** (0 directos) | **100 %** (0 directos) |
| M2 | PR fusionados desde el 03/11/2026 | `gh pr list --state merged` | — | 14 (13 a `develop`, 1 a otra rama) | 3 |
| M3 | PR fusionados con aprobación de alguien distinto del autor | Reviews en estado `APPROVED` de otro usuario | 100 % | **0 / 14** | **0 / 3** |
| M4 | Ejecuciones de CI exitosas (últimas 50) | `gh run list --limit 50` | ≥ 95 % | 50 / 50 | 40 / 40 (todas las disponibles) |
| M5 | Tiempo mediano de un PR, de abierto a fusionado | `mergedAt − createdAt` | ≤ 48 h | 0,5 h | 1,1 h |
| M6 | PR abiertos con más de 30 días | `gh pr list --state open` | 0 | 2 (#26, #32) | 2 (#28, #34) |
| M7 | Identidades de Git por persona | `git shortlog -sne --all` con `.mailmap` | 1 | 14 → **6** | 13 → **7** (6 personas + asistente de IA, `CMR.md` §4) |
| M8 | Días desde la última línea base (tag) | Hoy − fecha del último tag | ≤ 1 sprint | 169 (`pre-roles-v2-refactor`, 31/05/2026) | 169 |
| M9 | Migraciones de esquema con respaldo previo | Migraciones aplicadas en producción con `mysqldump` registrado | 100 % | Sin medición todavía: V1–V3 aún no se aplican en producción | — |

## 2. Lectura

- **M1 se cumple:** desde la política del 03/11/2026 no hay commits directos en las líneas principales.
- **M3 es la brecha principal.** Los PR se revisan y los fusiona otra persona, pero nadie deja la aprobación formal
  (*Approve*). En `main` GitHub la exige, así que el PR `develop` → `main` no pasará sin ella. En `develop` no hay
  regla técnica (`CONTROL_DE_CAMBIOS.md` §2). Acción en `CMR.md` §5, observación 1.
- **M6:** son los PR de `AlexCo10` (Bryan Niño) contra `main`, ya superados por `develop`. Se cierran después de
  confirmarlo con el autor.
- **M8** se corrige con el tag `v2.0.0` (TP-155).

## 3. Cómo recalcularlas

Desde la carpeta que contiene los dos repositorios, en Git Bash:

```bash
DESDE=2026-11-03
for r in backend frontend; do
  echo "== $r"
  # M1: commits directos en las líneas principales (debe dar 0)
  git -C $r log origin/main    --first-parent --no-merges --since=$DESDE --oneline | wc -l
  git -C $r log origin/develop --first-parent --no-merges --since=$DESDE --oneline | wc -l
  # M2, M3, M5: PR fusionados, aprobados por otro y tiempo de cada uno (horas)
  gh pr list -R SteveeenB/tramites-$r --state merged --limit 300 --json number,author,reviews,createdAt,mergedAt \
    --jq ".[] | select(.mergedAt >= \"$DESDE\") | [.number, ([.reviews[] | select(.state==\"APPROVED\") | .author.login] - [.author.login] | length > 0), (((.mergedAt|fromdate) - (.createdAt|fromdate))/3600 | floor)] | @tsv"
  # M4: resultado de las últimas 50 ejecuciones de CI
  gh run list -R SteveeenB/tramites-$r --limit 50 --json conclusion --jq 'group_by(.conclusion) | map("\(.[0].conclusion)=\(length)") | join(" ")'
  # M6: PR abiertos y su fecha
  gh pr list -R SteveeenB/tramites-$r --state open --json number,createdAt --jq '.[] | "\(.number) \(.createdAt[:10])"'
  # M7: identidades (con .mailmap)
  git -C $r shortlog -sne --all | wc -l
done
```
