package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.NameDef;
import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    private int scopeLevel;
    private Map<Integer, Scope> scopes;

    public SymbolTable() {
        this.scopeLevel = 0;
        this.scopes = new HashMap<>();
    }

    public void enterScope() {
        this.scopeLevel++;
        Scope scope = new Scope();
        this.scopes.put(this.scopeLevel, scope);
    }

    public void leaveScope() {
        this.scopeLevel--;
    }

    public void insert(NameDef nameDef) throws TypeCheckException {
        Scope scope = this.scopes.get(this.scopeLevel);
        NameDef existingNameDef = scope.lookup(nameDef.getName());
       /* if (existingNameDef != null) {
            throw new TypeCheckException(nameDef.getName()+nameDef.getType());
        }*/
        scope.insert(nameDef);
    }

    public NameDef lookup(String name) {
        for (int i = this.scopeLevel; i >= 1; i--) {
            Scope scope = this.scopes.get(i);
            NameDef nameDef = scope.lookup(name);
            if (nameDef != null) {
                return nameDef;
            }
        }
        return null;
    }

    private class Scope {

        private Scope parentScope;
        private Map<String, NameDef> nameDefs;

        public Scope() {
            this.parentScope = null;
            this.nameDefs = new HashMap<>();
        }

        public void setParentScope(Scope parentScope) {
            this.parentScope = parentScope;
        }

        public Scope getParentScope() {
            return this.parentScope;
        }

        public void insert(NameDef nameDef) {
            this.nameDefs.put(nameDef.getName(), nameDef);
        }

        public NameDef lookup(String name) {
            return this.nameDefs.get(name);
        }
    }
}
