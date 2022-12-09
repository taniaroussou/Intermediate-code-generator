import java.util.*;
import static utilities.Constants.*;

public class SymbolTable {

    LinkedHashMap<String, ClassType> table;

    SymbolTable() {
        table = new LinkedHashMap<String, ClassType>();
    }

    /* DEBUG ONLY*/
    public void printSymbolTable() {
        for (String classKey : table.keySet()) {
            ClassType varClass = table.get(classKey);
            if (varClass.superClass.isEmpty())
                System.out.println("class " + varClass.name + "{");
            else 
                System.out.println("class " + varClass.name + " extends " + varClass.superClass + "{");
            
            for (String varKey : varClass.variables.keySet()) {
                Variable variable = varClass.variables.get(varKey);
                System.out.println(variable.type + " " + variable.name);
            } 
            for (String methodKey : varClass.methods.keySet()) {
                MethodType varMethod = varClass.methods.get(methodKey);
                System.out.print(varMethod.returnType.toString() + " " + varMethod.name + "(");
                // parameters list
                for (String parKey : varMethod.parameters.keySet()) {
                    Variable variable = varMethod.parameters.get(parKey);
                    System.out.print(variable.type + " " + variable.name + ", ");
                }
                System.out.println(") {");
                for (String localKey : varMethod.locals.keySet()) {
                    Variable variable = varMethod.locals.get(localKey);
                    System.out.print(variable.type + " " + variable.name + ", ");
                }  
                System.out.println("");
                for (int i = 0; i < varMethod.callArgs.size(); i++) {
                    System.out.print(varMethod.callArgs.get(i) + ",callarg");
                }
                System.out.println("\n}");
            }                   
            System.out.println("}\n");
        }
    }

    public Boolean classExists(String identifier) {
        return table.containsKey(identifier);
    }

    public Boolean methodExists(String identifier, String className) {
        ClassType varClass = table.get(className);
        return varClass.methods.containsKey(identifier);
    } 

    private Boolean methodIsInherited(String methodName, String className) {
        while (classInheritsClass(className)) {
            ClassType derivedClass = table.get(className);
            if (methodExists(methodName, derivedClass.superClass)) 
                return true;           
            else 
                className = derivedClass.superClass;                      
        }
        return false;
    }

    private MethodType getInheritedMethod(String methodName, String className) {
        while (classInheritsClass(className)) {
            ClassType derivedClass = table.get(className);

            if (methodExists(methodName, derivedClass.superClass))         {
                return table.get(derivedClass.superClass).methods.get(methodName);
            }  
            else {
                className = derivedClass.superClass;                      
            }
        }
        return null;
    }
    
    public Boolean classInheritsClass(String className) {
        return !table.get(className).superClass.isEmpty();
    }

    public Boolean variableExistsRecursive(String name, String className) {
        ClassType classType = table.get(className);
        Boolean exists = classType.variables.containsKey(name);
       
        if (!exists && classInheritsClass(className))
            exists = variableExistsRecursive(name, classType.superClass);

        return exists;
    }

    public Variable getInheritedField(String name, String className) {
        ClassType classType = table.get(className);
        Boolean exists = classType.variables.containsKey(name);
        Variable var = null;
       
        if (exists) {
            var = classType.variables.get(name);
        }
        else if (classInheritsClass(className))
            var = getInheritedField(name, classType.superClass);

        return var;
    }

    public Boolean variableExists(String name, String className, String scope, String methodName) {          
        ClassType varClass = table.get(className);
        switch (scope) {
            case CLASS:
                return varClass.variables.containsKey(name);
            case METHOD_BODY:
                return (varClass.methods.get(methodName)).locals.containsKey(name) || (varClass.methods.get(methodName)).parameters.containsKey(name);                
            case FORMAL_PARAMETER:
                return (varClass.methods.get(methodName)).parameters.containsKey(name);   
        }
        return true;
    }

    public String insertVariable(String name, String type, String className, String scope) {   
        String methodName = "";
        if (scope.equals(METHOD_BODY) || scope.equals(FORMAL_PARAMETER)) {
            methodName = className.substring(className.indexOf(',') + 1);
            className = className.substring(0, className.indexOf(','));
        }
        
        if (!classExists(className))
            return UNDECLARED;
            
        if (variableExists(name, className, scope, methodName))
            return DOUBLE_DECL;
  
        ClassType varClass = table.get(className);
        Variable var = new Variable(name, type);

        switch (scope) {
            case CLASS:
                varClass.variables.put(name, var);
                break;
            case METHOD_BODY:
                (varClass.methods.get(methodName)).locals.put(name, var);
                break;
            case FORMAL_PARAMETER:
                (varClass.methods.get(methodName)).parameters.put(name, var);
                break;
        }
        return "";
    }

   

    public String insertClass(String name, String superClassName) {
        if (classExists(name)) 
            return DOUBLE_DECL;
        
        //  When we have "class B extends A", A must be defined before B
        if (!superClassName.isEmpty()) {
            if (!classExists(superClassName)) 
                return UNDECLARED;                           
        }
        // insert 
        ClassType newClass = new ClassType(name, superClassName);
        table.put(name, newClass);
        return "";
    }

    public String insertMethod(String methodName, String type, String className) {
        if (methodExists(methodName, className))
            return DOUBLE_DECL;

        // insert 
        MethodType newMethod = new MethodType(methodName, type);
        ClassType varClass = table.get(className);
        varClass.methods.put(methodName, newMethod); 
        return "";
    }    

    public void insertCallArg(String methodName, String className, String type) {
        ClassType classType = table.get(className);
        MethodType method;
        if (methodExists(methodName, className)) {
            method = classType.methods.get(methodName);
            method.callArgs.add(type);
        }
        else if (( getInheritedMethod(methodName, className)) != null) {
            method = getInheritedMethod(methodName, className);
            method.callArgs.add(type);
        }
    }

    public ArrayList<String> getCallArgs(String methodName, String className) {
        ClassType classType = table.get(className);
        MethodType method;
        if (methodExists(methodName, className)) { 
            method = classType.methods.get(methodName);         
            return method.callArgs;
        }
        else if ((method = getInheritedMethod(methodName, className)) != null) {
       
            return method.callArgs;
        }
        return new ArrayList<String>();
    }

    public void clearCallArgs(String methodName, String className) {
        ClassType classType = table.get(className);
        MethodType method;
        if (methodExists(methodName, className)) { 
            method = classType.methods.get(methodName);
            method.callArgs.clear();
        }
        else if ((method = getInheritedMethod(methodName, className)) != null) {
       
            method.callArgs.clear();
        }
    }

    public int getClassSize(String className) {
        int size = 0;
        ClassType classType = table.get(className);
        while (true) {
            for(Variable var : classType.variables.values()) {
                switch(var.type) {
                    case INT:
                        size += 4;
                        break;
                    case BOOLEAN:
                        size += 1;
                        break;
                    default:
                        size += 8;
                        break;
                }
            }
            if (!classInheritsClass(className))
                break;

            className = classType.superClass;
            classType = table.get(className);
        }
        return size + 8;
    }

    public void calculateOffsets() {
        int classCount = 0;
        for (String classKey : table.keySet()) {
            if (++classCount == 1)
                continue;
            ClassType classType = table.get(classKey);
            System.out.println("-----------Class " + classKey + "-----------");
            System.out.println("--Variables---");
            if (classType.variables.keySet().size() > 0) {

                int offset = 0;
                Variable prevVar = (Variable)classType.variables.values().toArray()[0];
             
                if (classInheritsClass(classKey)) {
                    ClassType parentClass = table.get(classType.superClass);
                    prevVar = (Variable)parentClass.variables.values().toArray()[parentClass.variables.keySet().size() - 1];
                }
                for (String varKey : classType.variables.keySet()) {
                    Variable variable = classType.variables.get(varKey);

                    if (prevVar != variable) {
                        if (prevVar.type.equals("int"))
                            offset = 4;
                        else if (prevVar.type.equals("boolean"))
                            offset = 1;
                        else 
                            offset = 8;
                    }                   

                    variable.offset = prevVar.offset + offset ;
                    
                    prevVar = variable;
                    System.out.println(classKey + '.' + varKey + " : " + variable.offset);
                }
            }
            System.out.println("---Methods---");
            int offset = 0;
            if (classType.methods.keySet().size() > 0) {
                MethodType prevMethod = (MethodType)classType.methods.values().toArray()[0];
    
                if (classInheritsClass(classKey)) {
                    ClassType parentClass = table.get(classType.superClass);
                    prevMethod = (MethodType)parentClass.methods.values().toArray()[parentClass.methods.keySet().size() - 1];
                }
    
                for (String methodKey : classType.methods.keySet()) {
                    MethodType method = classType.methods.get(methodKey);
    
                    if (prevMethod != method && !prevMethod.returnType.equals(VOID)) {
                        offset = 8;
                    }
    
                    method.offset = prevMethod.offset + offset;
                    prevMethod = method;
                    
                    if (!methodIsInherited(methodKey, classKey))
                        System.out.println(classKey + '.' + methodKey + " : " + method.offset);
                }
            }
            
            System.out.println("");
        }
    }
}
