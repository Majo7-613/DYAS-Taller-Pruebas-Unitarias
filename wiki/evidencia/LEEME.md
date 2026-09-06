# Evidencia de ejecución

Salidas de consola capturadas **en el momento** en que se produjeron, durante el ciclo TDD. No son reconstrucciones posteriores: varias de ellas (los rojos, y sobre todo el *shrinking* de jqwik) no se pueden volver a producir una vez implementada la regla, porque el código ya no falla.

| Archivo | Qué contiene |
|---|---|
| `iter3-RED.txt` | 🔴 Iteración 3 — 3 fallos: `expected: <INVALID> but was: <VALID>` |
| `iter3-GREEN.txt` | 🟢 Iteración 3 — 10 pruebas en verde |
| `iter4-RED.txt` | 🔴 Iteración 4 — falla el ejemplo y la propiedad de mayoría de edad |
| `iter4-GREEN.txt` | 🟢 Iteración 4 — 13 pruebas en verde |
| `iter5-RED.txt` | 🔴 Iteración 5 — incluye `expected: <INVALID_AGE> but was: <UNDERAGE>`, el fallo que reveló el orden incorrecto de las reglas |
| `iter5-GREEN.txt` | 🟢 Iteración 5 — 18 pruebas en verde |
| `iter6-RED.txt` | 🔴 Iteración 6 — falla la regla de duplicados |
| `iter6-GREEN.txt` | 🟢 Iteración 6 — 22 pruebas en verde |
| `iter7-REFACTOR.txt` | 🔵 Iteración 7 — 27 pruebas en verde tras extraer las constantes |
| **`shrinking-todoMenorDeEdadEsRechazado.txt`** | El contraejemplo **reducido** de jqwik: muestra original `("a", 29442, 10, FEMALE)` → muestra reducida `("", 1, 0, MALE)` en 4 pasos |
| **`pit-mutantes-sobrevivientes.txt`** | Reporte de PIT **antes** de `PersonTest`: 22/24 = 91,67 %, con los dos mutantes vivos de `Person.getName()` y `Person.getGender()` |
| `pit-final-resumen.txt` | Reporte de PIT **después** de `PersonTest`: 24/24 = 100 %, sin supervivientes |
| **`defecto05-inyectado.txt`** | Ejecución con el defecto 05 inyectado a propósito: de 30 pruebas falla **solo** `shouldNotRegisterIdWhenPersonIsRejected` |
| `pit-antes-de-PersonTest.txt` | Salida completa de la ejecución de PIT previa a `PersonTest` |

Los tres archivos en negrita son los que sustentan las afirmaciones centrales de la documentación:

- El **shrinking** justifica el uso de pruebas basadas en propiedades — ver [05 — Pruebas de propiedades](../05-Pruebas-de-Propiedades.md).
- Los **mutantes sobrevivientes** demuestran que 100 % de cobertura no implica comportamiento verificado — ver [06 — Resultados](../06-Resultados.md).
- El **defecto inyectado** demuestra que una sola prueba de treinta cubría ese caso límite — ver [07 — Gestión de defectos](../07-Gestion-de-Defectos.md).

> Nota: estos archivos viven en el **repositorio**, no en el Wiki. Las páginas del Wiki los enlazan por URL de GitHub.
