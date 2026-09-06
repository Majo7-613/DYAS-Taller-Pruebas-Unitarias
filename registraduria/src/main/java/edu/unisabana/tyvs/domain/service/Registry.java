package edu.unisabana.tyvs.domain.service;

import edu.unisabana.tyvs.domain.model.Person;
import edu.unisabana.tyvs.domain.model.RegisterResult;

import java.util.HashSet;
import java.util.Set;

/**
 * PUNTO DE PARTIDA DEL TALLER - no es la solucion final.
 *
 * Esta clase es el estado del codigo al terminar la ITERACION 2 del README
 * (regla "persona muerta"). Las reglas que faltan son las que usted debe
 * construir con TDD (Red -> Green -> Refactor):
 *
 *   - id <= 0                -> INVALID
 *   - edad < 0 o edad > 120  -> INVALID_AGE
 *   - 0 <= edad < 18         -> UNDERAGE
 *   - id ya registrado antes -> DUPLICATED
 *
 * Escriba PRIMERO la prueba que falla, luego la implementacion minima.
 */
public class Registry {

    /** Documentos ya inscritos. Es de instancia, nunca estatico: ver README. */
    private final Set<Integer> registeredIds = new HashSet<>();

    public RegisterResult registerVoter(Person p) {
        if (p == null) {
            return RegisterResult.INVALID; // regla defensiva
        }
        if (p.getId() <= 0) {
            return RegisterResult.INVALID;
        }
        if (!p.isAlive()) {
            return RegisterResult.DEAD;
        }
        if (p.getAge() < 0 || p.getAge() > 120) {
            return RegisterResult.INVALID_AGE;
        }
        if (p.getAge() < 18) {
            return RegisterResult.UNDERAGE;
        }
        if (registeredIds.contains(p.getId())) {
            return RegisterResult.DUPLICATED;
        }
        // Solo un registro exitoso consume el numero de documento.
        registeredIds.add(p.getId());
        return RegisterResult.VALID;
    }
}
