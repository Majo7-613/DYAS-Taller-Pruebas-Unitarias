# Taller de Pruebas Unitarias — Registraduría

Documentación de entrega del taller de **Desarrollo Dirigido por Pruebas (TDD)** sobre una **Arquitectura Limpia**.

**Asignatura:** Testing y Validación de Software
**Programa:** Maestría en Ingeniería de Software — Universidad de La Sabana
**Repositorio:** [DYAS-Taller-Pruebas-Unitarias](https://github.com/Majo7-613/DYAS-Taller-Pruebas-Unitarias)
**Equipo:** ver [`integrantes.txt`](https://github.com/Majo7-613/DYAS-Taller-Pruebas-Unitarias/blob/main/integrantes.txt)

---

## Resultados en una línea

| Métrica | Resultado | Exigido |
|---|---|---|
| Pruebas ejecutándose | **30** en verde | — |
| Cobertura de línea (JaCoCo) | **100 %** | ≥ 80 % |
| Cobertura de rama (JaCoCo) | **100 %** | ≥ 80 % |
| *Mutation score* (PIT) | **100 %** (24/24) | ≥ 60 % |
| Propiedades jqwik | **9** (1000 casos generados cada una) | 3 + las de referencia |
| Iteraciones TDD documentadas | **5** | ≥ 3 |
| Defectos registrados | **8**, ninguno abierto | ≥ 1 |

El `pom.xml` verifica los dos umbrales automáticamente: si la cobertura baja del 80 % o el *mutation score* del 60 %, **el build falla**. No son cifras declaradas en un documento, son condiciones que el proyecto se impone a sí mismo en cada ejecución.

---

## Índice

| Página | Contenido |
|---|---|
| [01 — Historia TDD](01-Historia-TDD) | Las 5 iteraciones Red → Green → Refactor, con la salida real de consola |
| [02 — Patrón AAA](02-Patron-AAA) | Cómo se estructuran las pruebas y por qué |
| [03 — Clases de equivalencia](03-Clases-de-Equivalencia) | Matriz de particiones, valores límite y la prueba que cubre cada uno |
| [04 — Escenarios BDD](04-BDD-Escenarios) | Given–When–Then de cada regla, trazado al código |
| [05 — Pruebas de propiedades](05-Pruebas-de-Propiedades) | jqwik, las 9 propiedades y el contraejemplo reducido |
| [06 — Resultados](06-Resultados) | JaCoCo, PIT y el análisis del mutante sobreviviente |
| [07 — Gestión de defectos](07-Gestion-de-Defectos) | Los 8 defectos y su evidencia |
| [08 — Reflexión final](08-Reflexion-Final) | Qué no se cubrió, qué aprendimos, cómo mejoraríamos el diseño |

---

## El dominio

Una registraduría inscribe personas como votantes para las próximas elecciones. El caso de uso es uno solo, `registerVoter(Person)`, y toda la complejidad está en decidir **si una persona puede quedar inscrita**.

### Reglas de negocio

| # | Regla | Resultado |
|---|-------|-----------|
| R1 | La persona no puede ser nula | `INVALID` |
| R2 | El número de documento debe ser positivo (`id > 0`) | `INVALID` |
| R3 | La persona debe estar viva | `DEAD` |
| R4 | La edad debe ser biológicamente posible (`0 ≤ edad ≤ 120`) | `INVALID_AGE` |
| R5 | La persona debe ser mayor de edad (`edad ≥ 18`) | `UNDERAGE` |
| R6 | Solo se permite una inscripción por documento | `DUPLICATED` |
| R7 | Si cumple todas las anteriores, queda registrada | `VALID` |

### El orden de evaluación es parte de la especificación

Las reglas se evalúan **R1 → R7, y la primera que falla determina el resultado**. Una persona muerta de 15 años devuelve `DEAD`, no `UNDERAGE`.

Esto no es un detalle de implementación: es una **decisión de diseño observable desde afuera**, porque cambia la respuesta del sistema. Por eso no vive solo en un comentario, sino en dos pruebas que se rompen si alguien reordena las guardas:

- `shouldPrioritizeDeadOverUnderage` — persona muerta *y* menor → `DEAD`
- `shouldPrioritizeInvalidIdOverDead` — documento inválido *y* muerta → `INVALID`

Y en una propiedad, `todoIdNoPositivoEsInvalido`, que afirma sobre todo el rango que R2 domina a R3.

Durante la iteración 5 este orden dejó de ser teórico: implementar R4 *después* de R5 hacía que una edad de `-1` se clasificara como `UNDERAGE`. Plausible, y equivocado: `-1` no es un menor de edad, es un dato imposible. Ver [Historia TDD](01-Historia-TDD#iteración-5--el-rango-biológico-de-la-edad).

---

## Alcance y arquitectura

```
registraduria/
└─ src/
   ├─ main/java/edu/unisabana/tyvs/domain/
   │   ├─ model/       Person, Gender, RegisterResult
   │   └─ service/     Registry           <- las reglas de negocio
   └─ test/java/edu/unisabana/tyvs/domain/
       ├─ model/       PersonTest
       └─ service/     RegistryTest, RegistryPropertiesTest
```

Todo el código es **dominio puro**: no hay base de datos, ni HTTP, ni framework. Las dependencias apuntan hacia adentro, así que las pruebas se ejecutan en milisegundos y sin ningún montaje previo — no hay contenedores que levantar ni dobles de prueba que configurar.

**Código fuente:**
[`Registry.java`](https://github.com/Majo7-613/DYAS-Taller-Pruebas-Unitarias/blob/main/registraduria/src/main/java/edu/unisabana/tyvs/domain/service/Registry.java) ·
[`Person.java`](https://github.com/Majo7-613/DYAS-Taller-Pruebas-Unitarias/blob/main/registraduria/src/main/java/edu/unisabana/tyvs/domain/model/Person.java) ·
[`RegisterResult.java`](https://github.com/Majo7-613/DYAS-Taller-Pruebas-Unitarias/blob/main/registraduria/src/main/java/edu/unisabana/tyvs/domain/model/RegisterResult.java)

**Pruebas:**
[`RegistryTest.java`](https://github.com/Majo7-613/DYAS-Taller-Pruebas-Unitarias/blob/main/registraduria/src/test/java/edu/unisabana/tyvs/domain/service/RegistryTest.java) ·
[`RegistryPropertiesTest.java`](https://github.com/Majo7-613/DYAS-Taller-Pruebas-Unitarias/blob/main/registraduria/src/test/java/edu/unisabana/tyvs/domain/service/RegistryPropertiesTest.java) ·
[`PersonTest.java`](https://github.com/Majo7-613/DYAS-Taller-Pruebas-Unitarias/blob/main/registraduria/src/test/java/edu/unisabana/tyvs/domain/model/PersonTest.java)

---

## Cómo ejecutarlo

Todos los comandos desde la carpeta `registraduria/`, que es donde está el `pom.xml`.

```bash
# Pruebas
mvn clean test

# Pruebas + cobertura + puerta del 80%  ->  target/site/jacoco/index.html
mvn clean verify

# Pruebas de mutación + umbral del 60%  ->  target/pit-reports/index.html
mvn test-compile org.pitest:pitest-maven:mutationCoverage
```

Requiere **JDK 17** y Maven 3.9+.

El repositorio también tiene [integración continua](https://github.com/Majo7-613/DYAS-Taller-Pruebas-Unitarias/blob/main/.github/workflows/ci.yml): cada `push` y cada *pull request* ejecuta los tres comandos y publica los reportes como artefactos.
