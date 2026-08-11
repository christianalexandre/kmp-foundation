#!/usr/bin/env bash
# Valida a mensagem de commit: sem trailer Co-Authored-By e no formato
# Conventional Commits (regra do docs/guidelines/01_style_guide.md).
set -euo pipefail

msg_file="$1"
fail=0

if grep -qiE '^[[:space:]]*Co-Authored-By:' "$msg_file"; then
  echo "oh, Co-Authored-By? make ABSOLUTELY sure you reviewed these changes, then remove the trailer(s) below before committing:" >&2
  grep -niE '^[[:space:]]*Co-Authored-By:' "$msg_file" | sed 's/^/  /' >&2
  fail=1
fi

# Primeira linha não-vazia e não-comentário.
subject="$(grep -vE '^[[:space:]]*#' "$msg_file" | grep -vE '^[[:space:]]*$' | head -1 || true)"

# Commits gerados pelo git (merge/revert/fixup) passam direto.
if [[ ! "$subject" =~ ^(Merge|Revert|fixup!|squash!) ]]; then
  types='feat|fix|chore|docs|refactor|test|perf|build|ci|style'
  if [[ ! "$subject" =~ ^($types)(\([a-z0-9._/-]+\))?!?:\ .+ ]]; then
    echo "mensagem fora do Conventional Commits:" >&2
    echo "  $subject" >&2
    echo "esperado: <tipo>(<escopo opcional>): <descrição no imperativo>" >&2
    echo "tipos: ${types//|/, }" >&2
    fail=1
  elif [[ ${#subject} -gt 72 ]]; then
    echo "assunto com ${#subject} caracteres (máximo 72):" >&2
    echo "  $subject" >&2
    fail=1
  fi
fi

exit "$fail"
