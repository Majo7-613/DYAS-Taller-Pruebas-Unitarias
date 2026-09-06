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
        Person person = new Person("Diego", 1, 25, Gender.MALE, true);

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
        Person person = new Person("Elena", 3, 17, Gender.FEMALE, true);

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
        Person person = new Person("Felipe", 4, 18, Gender.MALE, true);

        // Act: ejecutar la accion que queremos probar
        RegisterResult result = registry.registerVoter(person);

        // Assert: verificar el resultado esperado
        assertEquals(RegisterResult.VALID, result);
    }
}
