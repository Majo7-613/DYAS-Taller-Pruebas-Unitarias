package edu.unisabana.tyvs.domain.service;

import edu.unisabana.tyvs.domain.model.Person;
import edu.unisabana.tyvs.domain.model.RegisterResult;

import java.util.HashSet;
import java.util.Set;

/**
 * Caso de uso del dominio: inscribir a una persona como votante.
 *
 * Reglas de negocio, en su orden de evaluacion. La PRIMERA que falla determina
 * el resultado, de modo que una persona muerta de 15 anios devuelve DEAD y no
 * UNDERAGE. Ese orden es una decision de diseno y esta fijado con pruebas
 * (ver shouldPrioritizeDeadOverUnderage y shouldPrioritizeInvalidIdOverDead).
 *
 *   R1  persona nula                  -> INVALID
 *   R2  id < MIN_VALID_ID             -> INVALID
 *   R3  no esta viva                  -> DEAD
 *   R4  edad fuera de [MIN_AGE,MAX_AGE] -> INVALID_AGE
 *   R5  edad < MIN_VOTING_AGE         -> UNDERAGE
 *   R6  id ya inscrito                -> DUPLICATED
 *   R7  cumple todas las anteriores   -> VALID
 *
 * El dominio no conoce bases de datos, HTTP ni frameworks: las dependencias
 * apuntan hacia adentro (Arquitectura Limpia).
 */
public class Registry {

    /** Documento positivo mas pequenio que se considera valido. */
    public static final int MIN_VALID_ID = 1;

    /** Edad minima biologicamente posible. */
    public static final int MIN_AGE = 0;

    /** Edad maxima biologicamente posible. */
    public static final int MAX_AGE = 120;

    /** Edad a partir de la cual se puede votar. */
    public static final int MIN_VOTING_AGE = 18;

    /**
     * Documentos ya inscritos.
     *
     * Es un campo DE INSTANCIA, nunca estatico: si fuera estatico, el estado se
     * filtraria entre pruebas y el resultado dependeria del orden de ejecucion.
     * El @BeforeEach de RegistryTest se apoya en esta decision.
     */
    private final Set<Integer> registeredIds = new HashSet<>();

    /**
     * Evalua las reglas R1..R7 sobre la persona y, si todas se cumplen, la
     * inscribe.
     *
     * @param p persona a inscribir; se admite null (regla defensiva R1)
     * @return el resultado de la primera regla incumplida, o VALID
     */
    public RegisterResult registerVoter(Person p) {
        if (p == null) {                                            // R1
            return RegisterResult.INVALID;
        }
        if (p.getId() < MIN_VALID_ID) {                             // R2
            return RegisterResult.INVALID;
        }
        if (!p.isAlive()) {                                         // R3
            return RegisterResult.DEAD;
        }
        if (p.getAge() < MIN_AGE || p.getAge() > MAX_AGE) {         // R4
            return RegisterResult.INVALID_AGE;
        }
        if (p.getAge() < MIN_VOTING_AGE) {                          // R5
            return RegisterResult.UNDERAGE;
        }
        if (registeredIds.contains(p.getId())) {                    // R6
            return RegisterResult.DUPLICATED;
        }
        // Solo un registro exitoso consume el numero de documento.
        registeredIds.add(p.getId());
        return RegisterResult.VALID;                                // R7
    }
}
