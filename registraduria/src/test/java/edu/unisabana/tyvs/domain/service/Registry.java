package edu.unisabana.tyvs.domain.service;

import edu.unisabana.tyvs.domain.model.*;
import edu.unisabana.tyvs.domain.model.Person;
import edu.unisabana.tyvs.domain.model.RegisterResult;

public class Registry {

    public RegisterResult registerVoter(Person p) {
        // 1. Validar si la persona es nula
        if (p == null) {
            return RegisterResult.INVALID;
        }

        // 2. Validar si la persona no está viva
        if (!p.isAlive()) { // O p.getIsAlive() según los métodos de tu clase Person
            return RegisterResult.DEAD;
        }

        // 3. Si pasa las validaciones, el registro es válido
        return RegisterResult.VALID;
    }
}