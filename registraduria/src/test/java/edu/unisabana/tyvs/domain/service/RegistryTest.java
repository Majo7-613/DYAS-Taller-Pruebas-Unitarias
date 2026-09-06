package edu.unisabana.tyvs.domain.service;

import edu.unisabana.tyvs.domain.model.Gender;
import edu.unisabana.tyvs.domain.model.Person;
import edu.unisabana.tyvs.domain.model.RegisterResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas por EJEMPLO del dominio: cada prueba fija una entrada concreta y su
 * resultado esperado.
 *
 * Convenciones de la suite:
 *  - Estructura AAA: cada prueba separa explicitamente Arrange, Act y Assert.
 *  - Trazabilidad BDD: el @DisplayName redacta el escenario en Given-When-Then,
 *    de modo que el reporte de pruebas se lee como la especificacion de negocio.
 *  - Nomenclatura should...When...().
 *
 * Complemento: RegistryPropertiesTest expresa las mismas reglas como
 * PROPIEDADES sobre rangos completos de entradas, en vez de ejemplos sueltos.
 */
public class RegistryTest {

    private Registry registry;

    /**
     * Un Registry NUEVO antes de cada prueba.
     *
     * Importante: cuando implementemos DUPLICATED, el Registry guardara estado
     * (los ids ya registrados). Si compartieramos la misma instancia entre
     * pruebas, una prueba podria "ensuciar" a la siguiente y los resultados
     * dependerian del orden de ejecucion. Cada prueba debe ser independiente.
     */
    @BeforeEach
    void setUp() {
        registry = new Registry();
    }

    @Test
    @DisplayName("Dado una persona viva de 30 anios con id valido, "
            + "cuando la registro, entonces el resultado es VALID")
    void shouldRegisterValidPerson() {
        // Arrange: preparar los datos
        Person person = new Person("Ana", 1, 30, Gender.FEMALE, true);

        // Act: ejecutar la accion que queremos probar
        RegisterResult result = registry.registerVoter(person);

        // Assert: verificar el resultado esperado
        assertEquals(RegisterResult.VALID, result);
    }

    @Test
    @DisplayName("Dado una persona no viva, "
            + "cuando la registro, entonces el resultado es DEAD")
    void shouldRejectDeadPerson() {
        // Arrange: preparar los datos
        Person dead = new Person("Carlos", 2, 40, Gender.MALE, false);

        // Act: ejecutar la accion que queremos probar
        RegisterResult result = registry.registerVoter(dead);

        // Assert: verificar el resultado esperado
        assertEquals(RegisterResult.DEAD, result);
    }

    @Test
    @DisplayName("Dado una persona nula, "
            + "cuando la registro, entonces el resultado es INVALID")
    void shouldReturnInvalidWhenPersonIsNull() {
        // Arrange: preparar los datos
        Person person = null;

        // Act: ejecutar la accion que queremos probar
        RegisterResult result = registry.registerVoter(person);

        // Assert: verificar el resultado esperado
        assertEquals(RegisterResult.INVALID, result);
    }

    // ------------------------------------------------------------------
    // R2: el numero de documento debe ser positivo (id > 0)
    // ------------------------------------------------------------------

    @ParameterizedTest(name = "id = {0}")
    @ValueSource(ints = {0, -1, -5})
    @DisplayName("Dado una persona viva con documento no positivo, "
            + "cuando la registro, entonces el resultado es INVALID")
    void shouldRejectWhenIdIsZeroOrNegative(int invalidId) {
        // Arrange: preparar los datos
        Person person = new Person("Beatriz", invalidId, 25, Gender.FEMALE, true);

        // Act: ejecutar la accion que queremos probar
        RegisterResult result = registry.registerVoter(person);

        // Assert: verificar el resultado esperado
        assertEquals(RegisterResult.INVALID, result);
    }

    @Test
    @DisplayName("Dado una persona viva con el documento positivo mas pequenio (id = 1), "
            + "cuando la registro, entonces el resultado es VALID")
    void shouldAcceptMinimumValidId() {
        // Arrange: valor limite inferior de la clase de ids validos
        Person person = new Person("Diego", Registry.MIN_VALID_ID, 25, Gender.MALE, true);

        // Act: ejecutar la accion que queremos probar
        RegisterResult result = registry.registerVoter(person);

        // Assert: verificar el resultado esperado
        assertEquals(RegisterResult.VALID, result);
    }

    // ------------------------------------------------------------------
    // R5: la persona debe ser mayor de edad (edad >= 18)
    // ------------------------------------------------------------------

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

    @Test
    @DisplayName("Dado una persona viva de 18 anios con id valido, "
            + "cuando la registro, entonces el resultado es VALID")
    void shouldAcceptAdultAt18() {
        // Arrange: valor limite inferior de la clase "mayor de edad"
        Person person = new Person("Felipe", 4, Registry.MIN_VOTING_AGE, Gender.MALE, true);

        // Act: ejecutar la accion que queremos probar
        RegisterResult result = registry.registerVoter(person);

        // Assert: verificar el resultado esperado
        assertEquals(RegisterResult.VALID, result);
    }

    // ------------------------------------------------------------------
    // R4: la edad debe ser biologicamente posible (0 <= edad <= 120)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Dado una persona viva con edad negativa (-1), "
            + "cuando la registro, entonces el resultado es INVALID_AGE")
    void shouldRejectNegativeAge() {
        // Arrange: valor limite inferior de la clase de edades imposibles
        Person person = new Person("Gloria", 5, Registry.MIN_AGE - 1, Gender.FEMALE, true);

        // Act: ejecutar la accion que queremos probar
        RegisterResult result = registry.registerVoter(person);

        // Assert: la edad imposible manda sobre la minoria de edad
        assertEquals(RegisterResult.INVALID_AGE, result);
    }

    @Test
    @DisplayName("Dado una persona viva de 0 anios, "
            + "cuando la registro, entonces el resultado es UNDERAGE y no INVALID_AGE")
    void shouldRejectAgeZeroAsUnderage() {
        // Arrange: 0 es la frontera exacta entre INVALID_AGE y UNDERAGE
        Person person = new Person("Hugo", 6, Registry.MIN_AGE, Gender.MALE, true);

        // Act: ejecutar la accion que queremos probar
        RegisterResult result = registry.registerVoter(person);

        // Assert: 0 es biologicamente posible, asi que la regla que aplica es R5
        assertEquals(RegisterResult.UNDERAGE, result);
    }

    @Test
    @DisplayName("Dado una persona viva de 120 anios con id valido, "
            + "cuando la registro, entonces el resultado es VALID")
    void shouldAcceptMaxAge120() {
        // Arrange: valor limite superior de la clase de edades validas
        Person person = new Person("Irene", 7, Registry.MAX_AGE, Gender.FEMALE, true);

        // Act: ejecutar la accion que queremos probar
        RegisterResult result = registry.registerVoter(person);

        // Assert: verificar el resultado esperado
        assertEquals(RegisterResult.VALID, result);
    }

    @Test
    @DisplayName("Dado una persona viva de 121 anios, "
            + "cuando la registro, entonces el resultado es INVALID_AGE")
    void shouldRejectInvalidAgeOver120() {
        // Arrange: primer valor fuera del rango biologicamente posible
        Person person = new Person("Jorge", 8, Registry.MAX_AGE + 1, Gender.MALE, true);

        // Act: ejecutar la accion que queremos probar
        RegisterResult result = registry.registerVoter(person);

        // Assert: verificar el resultado esperado
        assertEquals(RegisterResult.INVALID_AGE, result);
    }

    // ------------------------------------------------------------------
    // R6: solo se permite una inscripcion por numero de documento
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Dado un documento ya inscrito, "
            + "cuando registro otra persona con el mismo documento, "
            + "entonces el resultado es DUPLICATED")
    void shouldReturnDuplicatedWhenSameIdRegisteredTwice() {
        // Arrange: dos personas distintas que comparten el numero de documento
        Person primera = new Person("Karla", 777, 30, Gender.FEMALE, true);
        Person segunda = new Person("Kevin", 777, 25, Gender.MALE, true);
        registry.registerVoter(primera);

        // Act: ejecutar la accion que queremos probar
        RegisterResult result = registry.registerVoter(segunda);

        // Assert: verificar el resultado esperado
        assertEquals(RegisterResult.DUPLICATED, result);
    }

    @Test
    @DisplayName("Dado dos personas con documentos distintos, "
            + "cuando las registro, entonces ambas quedan registradas")
    void shouldAllowDifferentIds() {
        // Arrange: la memoria del registro no debe rechazar documentos distintos
        Person primera = new Person("Laura", 10, 30, Gender.FEMALE, true);
        Person segunda = new Person("Mario", 11, 30, Gender.MALE, true);

        // Act: ejecutar la accion que queremos probar
        RegisterResult primerResultado = registry.registerVoter(primera);
        RegisterResult segundoResultado = registry.registerVoter(segunda);

        // Assert: verificar el resultado esperado
        assertEquals(RegisterResult.VALID, primerResultado);
        assertEquals(RegisterResult.VALID, segundoResultado);
    }

    @Test
    @DisplayName("Dado un intento de registro rechazado, "
            + "cuando vuelvo a usar ese documento con una persona valida, "
            + "entonces el resultado es VALID y no DUPLICATED")
    void shouldNotRegisterIdWhenPersonIsRejected() {
        // Arrange: un menor de edad no debe ocupar el numero de documento
        Person menorRechazado = new Person("Nora", 9, 15, Gender.FEMALE, true);
        Person adultaConElMismoId = new Person("Nora", 9, 20, Gender.FEMALE, true);
        assertEquals(RegisterResult.UNDERAGE, registry.registerVoter(menorRechazado));

        // Act: ejecutar la accion que queremos probar
        RegisterResult result = registry.registerVoter(adultaConElMismoId);

        // Assert: solo los registros exitosos consumen el documento
        assertEquals(RegisterResult.VALID, result);
    }

    // ------------------------------------------------------------------
    // Orden de evaluacion R1 -> R7: la PRIMERA regla que falla determina el
    // resultado. Es una decision de diseno, y por eso se fija con pruebas:
    // si alguien reordena las guardas, estas pruebas se caen.
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Dado una persona no viva y menor de edad, "
            + "cuando la registro, entonces el resultado es DEAD y no UNDERAGE")
    void shouldPrioritizeDeadOverUnderage() {
        // Arrange: la persona incumple R3 y R5 al mismo tiempo
        Person person = new Person("Olga", 12, 15, Gender.FEMALE, false);

        // Act: ejecutar la accion que queremos probar
        RegisterResult result = registry.registerVoter(person);

        // Assert: R3 se evalua antes que R5
        assertEquals(RegisterResult.DEAD, result);
    }

    @Test
    @DisplayName("Dado una persona no viva y con documento invalido, "
            + "cuando la registro, entonces el resultado es INVALID y no DEAD")
    void shouldPrioritizeInvalidIdOverDead() {
        // Arrange: la persona incumple R2 y R3 al mismo tiempo
        Person person = new Person("Pedro", 0, 40, Gender.MALE, false);

        // Act: ejecutar la accion que queremos probar
        RegisterResult result = registry.registerVoter(person);

        // Assert: R2 se evalua antes que R3
        assertEquals(RegisterResult.INVALID, result);
    }
}
