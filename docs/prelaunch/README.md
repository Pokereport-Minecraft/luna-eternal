# Evidencia sanitizada

Fase final: final-build.json identifica el nuevo build (188 casos, 186 ejecutados, 2 omitidos); final-staging-evidence.json identifica su arranque, 753 autotests/21 protecciones, métricas y hash principal intacto. final-tebex-drill.json registra únicamente fixtures sintéticos y replay tras reinicio; tebex-readonly-api.json registra GET de API sin reconocer/ejecutar pedidos. RELEASE_CANDIDATE.md describe límites. Los manifests/recibos anteriores enumerados abajo son históricos del candidato previo; no usarlos como hash del build actual. La copia QA del cliente recibió el nuevo JAR después del ensayo de arranque; no se volvió a certificar juego.

- final-build-manifest.json: hash del build anterior, base live y entradas modificadas; ninguna entrada eliminada.
- build-and-test-receipt.json: 187 casos registrados del build anterior y sus suites staging; el contador incluye casos omitidos.
- server-inventory-and-tests.json: 69 mods live, dimensiones, propiedades públicas, OP por nivel y líneas de prueba del último arranque candidato.
- recovery-receipt.json: restauración del backup corregido y ensayo SQL, con límite de coordinación temporal.
- runtime-commands-with-tebex.txt: inventario inicial 2247 usos.
- runtime-commands-isolated.txt: inventario final sin conector Tebex, 2236 usos.
- prelaunch-jar-comparison.json y prelaunch-changed-entries.json: comparación inicial preservada, anterior al hardening final. Usar final-build.json para el candidato actual.

No incluyen passwords, secretos Tebex, dump SQL, ops personales ni datos individuales de compras. Evidencia agregada no sustituye los pendientes de QA descritos en el reporte principal.
