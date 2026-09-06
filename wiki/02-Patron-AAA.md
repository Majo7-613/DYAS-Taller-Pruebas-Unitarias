# 02 — Patrón AAA (Arrange – Act – Assert)

Las 18 pruebas por ejemplo de la suite siguen el mismo esqueleto de tres pasos. La uniformidad es deliberada: cuando todas las pruebas tienen la misma forma, leer una prueba nueva cuesta cero.

---

## El ejemplo canónico

```java
@Test
@DisplayName("Dado una persona viva de 17 anios con id valido, "
        + "cuando la registro, entonces el resultado es UNDERAGE")
void shouldRejectUnderageAt17() {
    // Arrange: valor limite superior de la clase "menor de edad"
    Person person = new Person("Elena", 3, Registry.MIN_VOTING_AGE - 1, Gender.FEMALE, true);

    // Act: ejecutar la accion que queremos probar
    RegisterResult result = registry.registerVoter(person);

    // Assert: verificar el resultado esperado
    assertEquals(RegisterResult.UNDERAGE, result);
}
```

| Fase | Qué hace aquí |
|---|---|
| **Arrange** | Construye la persona en el estado exacto que el escenario describe. La instancia de `Registry` no aparece: la prepara el `@BeforeEach`. |
| **Act** | Una sola línea, una sola llamada. Si el *Act* necesitara varias líneas, sería señal de que la prueba está probando más de una cosa. |
| **Assert** | Una afirmación sobre el resultado de esa llamada. |

---

## Pautas que adoptamos

### 1. El *Act* es siempre una sola línea

Si hacen falta varias llamadas para llegar al comportamiento bajo prueba, las llamadas previas son **preparación**, no acción, y por lo tanto van en el *Arrange*. Se ve claro en la prueba de duplicados:

```java
void shouldReturnDuplicatedWhenSameIdRegisteredTwice() {
    // Arrange: dos personas distintas que comparten el numero de documento
    Person primera = new Person("Karla", 777, 30, Gender.FEMALE, true);
    Person segunda = new Person("Kevin", 777, 25, Gender.MALE, true);
    registry.registerVoter(primera);          // <-- preparación del estado, no la acción

    // Act
    RegisterResult result = registry.registerVoter(segunda);

    // Assert
    assertEquals(RegisterResult.DUPLICATED, result);
}
```

El primer `registerVoter` **no** es lo que estamos probando; es lo que crea la condición "documento ya inscrito". Ubicarlo en el *Arrange* deja explícito qué se está midiendo.

### 2. El *Arrange* comenta la intención, no la mecánica

`// Arrange: valor limite superior de la clase "menor de edad"` aporta algo. `// Arrange: crear una persona` no aporta nada: eso ya lo dice la línea siguiente. El comentario justifica **por qué ese dato y no otro**, que es lo único que el código no puede decir por sí solo.

### 3. Un `@BeforeEach` para lo que toda prueba necesita

```java
private Registry registry;

@BeforeEach
void setUp() {
    registry = new Registry();   // instancia limpia para cada prueba
}
```

Empezó como una forma de no repetir `new Registry()`. Cuando `Registry` pasó a tener estado (la regla de duplicados), se volvió **imprescindible**: garantiza que ninguna prueba herede los documentos inscritos por otra. Ver la iteración 6 en [Historia TDD](01-Historia-TDD#iteración-6--los-duplicados-r6).

> Un defecto real de este taller vino justo de romper esta regla: una prueba declaraba su propia variable local `Registry registry = new Registry();` que sombreaba el campo, saltándose el `@BeforeEach` sin que nada avisara. Está registrado como defecto 07.

### 4. Nomenclatura `should…When…()`

El nombre del método dice qué debe pasar y bajo qué condición: `shouldRejectWhenIdIsZeroOrNegative`, `shouldNotRegisterIdWhenPersonIsRejected`. Un fallo en la consola se entiende **sin abrir el archivo**.

### 5. AAA y BDD en el mismo artefacto

Cada prueba lleva un `@DisplayName` redactado en Given–When–Then:

```java
@DisplayName("Dado un documento ya inscrito, "
        + "cuando registro otra persona con el mismo documento, "
        + "entonces el resultado es DUPLICATED")
```

Así, la misma prueba sirve a dos audiencias: la estructura AAA organiza el código para quien lo mantiene, y el `@DisplayName` produce un reporte legible para quien define el negocio. No hay dos artefactos que mantener sincronizados — el reporte de pruebas **es** la especificación. Ver [Escenarios BDD](04-BDD-Escenarios).

### 6. Varias aserciones solo cuando describen un mismo hecho

La regla general es una aserción por prueba. `shouldAllowDifferentIds` tiene dos, y con razón:

```java
assertEquals(RegisterResult.VALID, primerResultado);
assertEquals(RegisterResult.VALID, segundoResultado);
```

El hecho que se afirma es único —"documentos distintos no se estorban entre sí"— y necesita las dos mitades para quedar dicho. Lo que evitamos es la prueba que verifica tres comportamientos sin relación entre sí: cuando falla, no se sabe cuál se rompió.

---

## Por qué insistimos en esto

| Sin AAA | Con AAA |
|---|---|
| Hay que leer la prueba entera para saber qué se está probando | El *Act* lo dice en una línea |
| La preparación se mezcla con la verificación | Las tres fases están separadas visualmente |
| Al fallar, hay que reconstruir el escenario mentalmente | El *Arrange* es el escenario, escrito |
| Cada quien escribe con un estilo distinto | Una prueba nueva se lee igual que las 17 anteriores |

El costo son tres comentarios por método. La ganancia es que la suite se puede leer como documentación, que es exactamente lo que se le pide cuando alguien llega al proyecto seis meses después.
