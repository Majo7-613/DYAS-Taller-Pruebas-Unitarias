# Capturas de los reportes

Coloque aquí las dos capturas que pide la rúbrica.

| Reporte | Archivo a abrir en el navegador | Guardar como |
|---|---|---|
| Cobertura JaCoCo | `registraduria/target/site/jacoco/index.html` | `jacoco.png` |
| Mutación PIT | `registraduria/target/pit-reports/index.html` | `pit.png` |

Para generarlos:

```bash
cd registraduria
mvn clean verify                                              # JaCoCo
mvn test-compile org.pitest:pitest-maven:mutationCoverage      # PIT
```

En el reporte de JaCoCo conviene capturar la vista del paquete `edu.unisabana.tyvs.domain`, donde se ven los porcentajes por clase. En el de PIT, la página inicial con el *mutation score* y, si cabe, el detalle de `Registry` con las líneas en verde.

**Para incrustarlas en el Wiki de GitHub**: abra la página [06 — Resultados](../06-Resultados.md) en el editor del Wiki y arrastre los PNG dentro. GitHub los sube y genera el enlace automáticamente.
