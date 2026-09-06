# Taller de Pruebas Unitarias — Registraduría

**Integrantes:** ver [`integrantes.txt`](integrantes.txt)

---

## Resultado del proyecto

| Métrica | Resultado | Exigido |
|---|---|---|
| Pruebas | 30 en verde | — |
| Cobertura de línea / rama (JaCoCo) | **100 % / 100 %** | ≥ 80 % |
| *Mutation score* (PIT) | **100 %** (24/24) | ≥ 60 % |
| Propiedades jqwik | 9 | 3 + referencia |
| Iteraciones TDD documentadas | 5 | ≥ 3 |
| Defectos registrados | 8, ninguno abierto | ≥ 1 |

Los umbrales de cobertura (80 %) y mutación (60 %) están configurados en el `pom.xml` y **rompen el build** si se incumplen — se verifican en cada `mvn clean verify` y en cada push mediante [integración continua](.github/workflows/ci.yml).

La documentación completa — historia TDD, clases de equivalencia, escenarios BDD, análisis de mutación, gestión de defectos y reflexión final — está en la **[Wiki del repositorio](../../wiki)**.

---

## Estructura del repositorio

```
TYVS-Taller_Pruebas_Unitarias/
├── README.md                    Este archivo
├── integrantes.txt              Nombres del equipo
├── defectos.md                  Ejemplo de registro de defectos (guía del profesor)
├── PUBLICAR-WIKI.md             Instrucciones para publicar wiki/ en el Wiki de GitHub
│
├── .github/workflows/ci.yml     Integración continua: pruebas + cobertura + mutación en cada push
│
├── wiki/                        Contenido de la Wiki, listo para publicar (ver PUBLICAR-WIKI.md)
│   ├── Home.md                  Página de inicio: dominio, reglas de negocio, cómo ejecutar
│   ├── 01-Historia-TDD.md       Las 5 iteraciones Red → Green → Refactor
│   ├── 02-Patron-AAA.md         Cómo se estructuran las pruebas
│   ├── 03-Clases-de-Equivalencia.md   Matriz de particiones y valores límite
│   ├── 04-BDD-Escenarios.md     Escenarios Given–When–Then de cada regla
│   ├── 05-Pruebas-de-Propiedades.md   Las 9 propiedades jqwik y el shrinking
│   ├── 06-Resultados.md         JaCoCo, PIT y el análisis del mutante sobreviviente
│   ├── 07-Gestion-de-Defectos.md      Los 8 defectos y su evidencia
│   ├── 08-Reflexion-Final.md    Qué no se cubrió, qué aprendimos
│   ├── evidencia/               Salidas de consola reales de cada rojo/verde y del shrinking
│   └── img/                     Capturas de los reportes JaCoCo y PIT
│
└── registraduria/                     ← El proyecto Maven de la entrega
    ├── pom.xml                        JUnit 5, jqwik, JaCoCo, PIT
    ├── defectos.md                    Registro de defectos del equipo (la entrega real)
    └── src/
        ├── main/java/edu/unisabana/tyvs/domain/
        │   ├── model/
        │   │   ├── Person.java        Datos de una persona
        │   │   ├── Gender.java        MALE, FEMALE, UNIDENTIFIED
        │   │   └── RegisterResult.java   Los seis resultados posibles
        │   └── service/
        │       └── Registry.java      Las 7 reglas de negocio (R1–R7)
        └── test/java/edu/unisabana/tyvs/domain/
            ├── model/
            │   └── PersonTest.java              Verifica el contrato de Person
            └── service/
                ├── RegistryTest.java             18 pruebas por ejemplo (AAA + BDD)
                └── RegistryPropertiesTest.java   9 propiedades jqwik
```

---

## Las reglas de negocio

`registerVoter(Person)` evalúa estas reglas **en orden**, y la primera que falla determina el resultado:

| # | Regla | Resultado |
|---|-------|-----------|
| R1 | La persona no puede ser nula | `INVALID` |
| R2 | El documento debe ser positivo | `INVALID` |
| R3 | La persona debe estar viva | `DEAD` |
| R4 | La edad debe ser biológicamente posible (0–120) | `INVALID_AGE` |
| R5 | La persona debe ser mayor de edad (≥18) | `UNDERAGE` |
| R6 | Solo una inscripción por documento | `DUPLICATED` |
| R7 | Si cumple todas las anteriores | `VALID` |

Detalle completo de por qué el orden importa en [Wiki: Home](../../wiki/Home#el-orden-de-evaluación-es-parte-de-la-especificación).

---

## Cómo ejecutar el proyecto

Todos los comandos desde la carpeta `registraduria/`, donde está el `pom.xml`. Requiere **JDK 17** y Maven 3.9+.

```bash
cd registraduria

# Pruebas (30 en total: 18 por ejemplo + 9 propiedades + 3 de Person)
mvn clean test

# Pruebas + cobertura + puerta del 80%  ->  target/site/jacoco/index.html
mvn clean verify

# Pruebas de mutación + umbral del 60%  ->  target/pit-reports/index.html
mvn test-compile org.pitest:pitest-maven:mutationCoverage
```

**Verificación desde cero:** clonar el repositorio en una carpeta nueva y correr `mvn clean test` — no requiere ningún paso manual adicional.

---
