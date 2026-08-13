# Regras do build Release. Hoje vazio de propósito: o Compose e as bibliotecas
# AndroidX embarcam suas próprias regras de consumidor, e o app não usa reflexão.
#
# Existe porque o buildType `release` (adicionado para medir a bancada do RFC 0001
# em Release — em Debug a ART desliga otimizações e o número sai enganoso) declara
# este arquivo. Se o R8 quebrar algo, a regra entra aqui, com o porquê.
