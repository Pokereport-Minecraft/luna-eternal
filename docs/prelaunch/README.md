# Evidencia sanitizada

- final-build-manifest.json: hash del build final, base live y entradas modificadas; ninguna entrada eliminada.
- build-and-test-receipt.json: 187 JUnit y resultado final de las suites staging.
- server-inventory-and-tests.json: 69 mods live, dimensiones, propiedades públicas, OP por nivel y líneas de prueba del último arranque candidato.
- recovery-receipt.json: restauración del backup corregido y ensayo SQL, con límite de coordinación temporal.
- runtime-commands-with-tebex.txt: inventario inicial 2247 usos.
- runtime-commands-isolated.txt: inventario final sin conector Tebex, 2236 usos.
- prelaunch-jar-comparison.json y prelaunch-changed-entries.json: comparación inicial preservada, anterior al hardening final. Usar final-build-manifest para candidato actual.

No incluyen passwords, secretos Tebex, dump SQL, ops personales ni datos individuales de compras. Evidencia agregada no sustituye los pendientes de QA descritos en el reporte principal.
