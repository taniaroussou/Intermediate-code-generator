import visitor.*;
import java.util.ArrayList;
import syntaxtree.*;
import static utilities.Constants.*;

class TypeCheckingVisitor extends GJDepthFirst<String, String> {
    SymbolTable symbolTable;

    public TypeCheckingVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }
    
    private Boolean isPrimitiveType(String type) {
        return (type.equals(INT) || type.equals(BOOLEAN) || type.equals(INT_ARRAY) || type.equals(BOOLEAN_ARRAY));
    }

    private String checkVarType(String type) {
        if (!isPrimitiveType(type)) {
            if (!symbolTable.classExists(type))
                return UNDECLARED;
        }
        return "";
    }

    private Boolean isSubType(String lType, String rType) {
        Boolean ret = false;
        ClassType derivedClass = symbolTable.table.get(rType);
        if (symbolTable.classInheritsClass(derivedClass.name)) {
            if (!derivedClass.superClass.equals(lType))
                ret = isSubType(lType, derivedClass.superClass);
            else 
                ret = true;
        }
        return ret;
    }

    private Boolean isMainMethod(String methodName, String className) {
        if (!methodName.equals("main"))
            return false;
        ClassType classType = symbolTable.table.get(className);
        MethodType method = classType.methods.get(methodName);
        if (method.parameters.size() != 1)
            return false;

        Variable arg = (Variable)method.parameters.values().toArray()[0];
        return arg.type.equals(STRING_ARRAY);
    }

    private Boolean isVirtual(ClassType derivedClass, ClassType parentClass, String methodName) {
        MethodType derivedMethod = derivedClass.methods.get(methodName);
        MethodType parentMethod = parentClass.methods.get(methodName);
        
        if (!derivedMethod.returnType.equals(parentMethod.returnType))
            return false;
        
        if (derivedMethod.parameters.size() != parentMethod.parameters.size())
            return false;
        else {
            // compare the types of all parameter pairs          
            ArrayList<Variable> derivedParams = new ArrayList<Variable>(derivedMethod.parameters.values());
            ArrayList<Variable> parentParams = new ArrayList<Variable>(parentMethod.parameters.values());
            int argsCount = derivedParams.size();
            for (int i = 0; i < argsCount; i++) {
                // type mismatch
                if (!derivedParams.get(i).type.equals(parentParams.get(i).type))
                    return false;
            }
        }
        return true;        
    }

    // check for virtual function signature if class extends another class
    private Boolean checkMethod(String methodName, String className) {
        ClassType initialClass = symbolTable.table.get(className);
        while (symbolTable.classInheritsClass(className)) {
            ClassType derivedClass = symbolTable.table.get(className);
            ClassType parentClass = symbolTable.table.get(derivedClass.superClass);
            Boolean parentHasMethod = symbolTable.methodExists(methodName, derivedClass.superClass);

            if (parentHasMethod && !isVirtual(initialClass, parentClass, methodName)) 
                return false;           
            else if (!parentHasMethod)  
                className = derivedClass.superClass;
            else 
                return true;                      
        }
        return true;
    }

    private String checkInheritedLocals(String identifier, String className) {
        while (symbolTable.classInheritsClass(className)) {
            ClassType derivedClass = symbolTable.table.get(className);
            ClassType parentClass = symbolTable.table.get(derivedClass.superClass);
            Boolean parentHasLocalVar = symbolTable.variableExists(identifier, derivedClass.superClass, CLASS, "");

            if (parentHasLocalVar) 
                return parentClass.variables.get(identifier).type.toString();      
            else 
                className = derivedClass.superClass;                              
        }
        return UNDECLARED;
    }

    private Boolean validateArgTypes(MethodType method, ArrayList<String> callArgs) {
        ArrayList<Variable> methodParameters = new ArrayList<Variable>(method.parameters.values());
        
        int argsCount = callArgs.size();
        if (argsCount != methodParameters.size())
            return false;

        for (int i = 0; i < argsCount; i++) {
            String lType = methodParameters.get(i).type.toString();
            String rType = callArgs.get(i);
             if (!isPrimitiveType(lType) && !isPrimitiveType(rType) && !lType.equals(rType)) {
                if (!isSubType(lType, rType)) 
                    return false;
                else
                    return true;
            }
            else if (!lType.equals(rType))
                return false;
        }
        return true;
    }


    private String methodGetRetTypeIfExistsRecursive(String methodId, String objectType, ArrayList<String> callArgs) {
        ClassType classType = symbolTable.table.get(objectType);
        String retValue = "error";

        if (symbolTable.methodExists(methodId, classType.name)) {
            MethodType method = classType.methods.get(methodId);
            retValue = validateArgTypes(method, callArgs) ? method.returnType.toString() : WRONG_ARGS;
        }
        
        else if (symbolTable.classInheritsClass(classType.name))
            retValue = methodGetRetTypeIfExistsRecursive(methodId, classType.superClass, callArgs);
        else 
            retValue = METHOD_NOTFOUND;

        return retValue;
    }
   

    public String visit(MainClass n, String argu) throws Exception {
        String identifier = n.f1.accept(this, argu);

         //  ( VarDeclaration() )*
        if (n.f14.present())
            n.f14.accept(this, METHOD_BODY + " " + identifier + ",main");        

        //  ( Statement() )*
        if (n.f15.present())
            n.f15.accept(this,  identifier + ",main");         

        return null;
    }

        /**-
     * f0 -> "class"
     * f1 -> Identifier()
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     */
    public String visit(ClassDeclaration n, String argu) throws Exception {		
        String identifier = n.f1.accept(this, null);

        if (n.f3.present())
		    n.f3.accept(this, CLASS + " " + identifier);

        if (n.f4.present())
            n.f4.accept(this, identifier);
        
        return null;
 	}

    /**
     * f0 -> "class"
    * f1 -> Identifier()
    * f3 -> Identifier()
    * f5 -> ( VarDeclaration() )*
    * f6 -> ( MethodDeclaration() )*
    */
	public String visit(ClassExtendsDeclaration n, String argu) throws Exception {
		String identifier = n.f1.accept(this, null);

        if (n.f5.present())
		    n.f5.accept(this, CLASS + " " + identifier);
		
        if (n.f6.present())
            n.f6.accept(this, identifier);

		return null;
	}
    /*
    * f2 -> Identifier()
    * f4 -> ( FormalParameterList() )?
    * f5 -> ")"
    * f6 -> "{"
    * f7 -> ( VarDeclaration() )*
    * f8 -> ( Statement() )*
    * f9 -> "return"
    * f10 -> Expression()
    */
    public String visit(MethodDeclaration n, String className) throws Exception {
        String methodName = n.f2.accept(this, null);

        if (!checkMethod(methodName, className)) 
            throw new MyException(NON_VIRTUAL, methodName, "");
        
        if (n.f4.present())
            n.f4.accept(this, null);

        if (n.f7.present())
            n.f7.accept(this, null);
            
        if (n.f8.present())
            n.f8.accept(this, className + "," + methodName);

        String returnType = n.f10.accept(this, className + "," + methodName);
        MethodType method = symbolTable.table.get(className).methods.get(methodName);

        if (!returnType.equals(method.returnType))
            throw new MyException(RETURN_TYPE, methodName, "");

        return null;
    }
    
    /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
    public String visit(FormalParameter n, String argu) throws Exception {
        String type = n.f0.accept(this, null);
        if (checkVarType(type).equals(UNDECLARED))
            throw new MyException(UNDECLARED, type, CLASS);
            
        return null;
    }

    /* VarDeclaration 
    * f0 -> Type()
    * f1 -> Identifier()
    */
    public String visit(VarDeclaration n, String argu) throws Exception {
		String type = n.f0.accept(this, null);
        String id = n.f1.accept(this, null);
        if (checkVarType(type).equals(UNDECLARED))
            throw new MyException(UNDECLARED, type, CLASS);

        return type;
    }

    /**
     * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
    public String visit(AssignmentStatement n, String scope) throws Exception {
        // scope = className + "," + methodName;
        String lType = n.f0.accept(this, scope);
        String rType = n.f2.accept(this, scope);

        if (lType.equals(STRING_ARRAY) || rType.equals(STRING_ARRAY))
            throw new MyException(USE_ARGS, "", "");

        if (lType.equals(UNDECLARED))
            throw new MyException(UNDECLARED, VARIABLE, "");

        if (rType.equals("this")) {
            rType = scope.substring(0, scope.indexOf(','));
        }

        if (!isPrimitiveType(lType) && !isPrimitiveType(rType)) {
            if (symbolTable.classExists(rType) && !lType.equals(rType)) {
                if (!isSubType(lType, rType)) 
                    throw new MyException(SUBTYPE, rType + ',' + lType, "");
                return null;
            }
            else if (!symbolTable.classExists(rType))
                throw new MyException(UNDECLARED, "identifier", "");    
        }

        if (!lType.equals(rType)) 
            throw new MyException(STATEMENT, scope.substring(scope.indexOf(',') + 1), "assignment");

        return null;
    }

    /**
     * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
    public String visit(ArrayAssignmentStatement n, String scope) throws Exception {

        String idType = n.f0.accept(this, scope);
        String indexType = n.f2.accept(this, scope);
        String assignmentType = n.f5.accept(this, scope);

        if (idType.equals(UNDECLARED) || assignmentType.equals(UNDECLARED))
            throw new MyException(UNDECLARED, VARIABLE, "");

        if (!indexType.equals(INT))
            throw new MyException(ASSIGNMENT, scope.substring(scope.indexOf(',') + 1), "index");

        if (idType.equals(INT_ARRAY) || idType.equals(BOOLEAN_ARRAY)) {
            idType = idType.replace("[]", "");
            if (!idType.equals(assignmentType))
                throw new MyException(ASSIGNMENT, scope.substring(scope.indexOf(',') + 1), "expression");
        }    

        return null;
    }

    /**
     * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
    public String visit(IfStatement n, String scope) throws Exception {

        String type = n.f2.accept(this, scope);
        if (!type.equals(BOOLEAN))
            throw new MyException(EXPRESSION, "", "if");

        n.f4.accept(this, scope);
        n.f6.accept(this, scope);
        return null;
    }

    /**
     * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
    public String visit(WhileStatement n, String scope) throws Exception {
        String type = n.f2.accept(this, scope);
        if (!type.equals(BOOLEAN))
            throw new MyException(EXPRESSION, "", "while");

        n.f4.accept(this, scope);
        return null;
    }

    /**
     * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
    public String visit(PrintStatement n, String scope) throws Exception {
        String type = n.f2.accept(this, scope);
        if (type == null || !type.equals(INT))
            throw new MyException(PRINTSTATEMENT, "", "");

        return INT;
    }

    public String visit(AndExpression n, String argu) throws Exception {
        String lTermType = n.f0.accept(this, argu);
        String rTermType = n.f2.accept(this, argu);
        if (!lTermType.equals(BOOLEAN) || !rTermType.equals(BOOLEAN))
            throw new MyException(EXPRESSION, BOOLEAN, "and");

        return BOOLEAN;
    }

    public String visit(CompareExpression n, String argu) throws Exception {
        String lTermType = n.f0.accept(this, argu);
        String rTermType = n.f2.accept(this, argu);
        if (!lTermType.equals(INT) || !rTermType.equals(INT))
            throw new MyException(EXPRESSION, INT, "compare(<)");

        return BOOLEAN;
    }

    public String visit(PlusExpression n, String argu) throws Exception {
        String lTermType = n.f0.accept(this, argu);
        String rTermType = n.f2.accept(this, argu);
        if (!lTermType.equals(INT) || !rTermType.equals(INT))
            throw new MyException(EXPRESSION, INT, "plus(+)");

        return INT;
    }

    public String visit(MinusExpression n, String argu) throws Exception {
        String lTermType = n.f0.accept(this, argu);
        String rTermType = n.f2.accept(this, argu);
        if (!lTermType.equals(INT) || !rTermType.equals(INT))
            throw new MyException(EXPRESSION, INT, "minus(-)");

        return INT;
    }

    public String visit(TimesExpression n, String argu) throws Exception {
        String lTermType = n.f0.accept(this, argu);
        String rTermType = n.f2.accept(this, argu);
        if (!lTermType.equals(INT) || !rTermType.equals(INT))
            throw new MyException(EXPRESSION, INT, "times(*)");

        return INT;
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    public String visit(ArrayLookup n, String argu) throws Exception {
        String lType = n.f0.accept(this, argu);
        if (!lType.equals(INT_ARRAY) && !lType.equals(BOOLEAN_ARRAY))
            throw new MyException(ARRAY_ACCESS, "", "");

        String rType = n.f2.accept(this, argu);
        if (!rType.equals(INT))
            throw new MyException(INVALID_INDEX, "", "");

        return lType.equals(INT_ARRAY) ? INT : BOOLEAN;
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
    public String visit(ArrayLength n, String argu) throws Exception {        
        String lType = n.f0.accept(this, argu);
        if (!lType.equals(INT_ARRAY) && !lType.equals(BOOLEAN_ARRAY))
            throw new MyException(ARRAY_LENGTH, "", "");

        return INT;
    }
    
    /**
     * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
    public String visit(MessageSend n, String scope) throws Exception {
       // scope = className + "," + methodName
        String lType = n.f0.accept(this, scope);
        if (lType.equals(INT) || lType.equals(BOOLEAN) || lType.equals(INT_ARRAY) || lType.equals(BOOLEAN_ARRAY)) 
            throw new MyException(MEMBER_CALL, "", "");

        if (lType.equals(UNDECLARED))
            throw new MyException(MEMBER_CALL_UNDECL, "", "");

        String identifier = n.f2.f0.toString();
        String className = scope.substring(0, scope.indexOf(','));
        String methodName = scope.substring(scope.indexOf(',') + 1);
        if (lType.equals("this")) {
            if (isMainMethod(methodName, className))
                throw new MyException(THIS_MAIN, "", "");            
            lType = className;
        }
        n.f4.accept(this, scope + "-" + identifier + "+" + lType);

        ArrayList<String> tempCallArgs = symbolTable.getCallArgs(identifier, lType);
        String ret = methodGetRetTypeIfExistsRecursive(identifier, lType, tempCallArgs);
        switch (ret) {
            case WRONG_ARGS:
                throw new MyException(WRONG_ARGS, identifier, "");
            case METHOD_NOTFOUND:
                throw new MyException(METHOD_NOTFOUND, identifier, "");
        }
        symbolTable.clearCallArgs(identifier, lType);
        return ret;
    }

    /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
    public String visit(ExpressionList n, String scope) throws Exception {
        String type = n.f0.accept(this, scope.substring(0, scope.indexOf('-')));     // f1 -> Expression()
        // undeclared identifier
        if (type.equals(UNDECLARED)) 
            throw new MyException(UNDECLARED, "call argument", "");
        if (type.equals("this")) 
            type = scope.substring(0, scope.indexOf(','));

        // insert temporary call argument in method
        String memberCallName = scope.substring(scope.indexOf('-') + 1, scope.indexOf('+'));
        String callingObjectClassName = scope.substring(scope.indexOf('+') + 1);
        symbolTable.insertCallArg(memberCallName, callingObjectClassName, type);

        n.f1.accept(this, scope);
        return null;
    }

    public String visit(ExpressionTerm n, String scope) throws Exception {

        String type = n.f1.accept(this, scope.substring(0, scope.indexOf('-')));     // f1 -> Expression()

        // undeclared identifier
        if (type.equals(UNDECLARED)) 
            throw new MyException(UNDECLARED, "call argument", "");

        if (type.equals("this")) 
            type = scope.substring(0, scope.indexOf(','));
        
        // insert temporary call argument in method    
        String memberCallName = scope.substring(scope.indexOf('-') + 1, scope.indexOf('+'));
        String callingObjectClassName = scope.substring(scope.indexOf('+') + 1);
        symbolTable.insertCallArg(memberCallName, callingObjectClassName, type);

        return null;
    }

    
    /**
        * f0 -> ( ExpressionTerm() )*
        */
    public String visit(ExpressionTail n, String scope) throws Exception {
        return n.f0.accept(this, scope);
    }

    public String visit(NotExpression n, String argu) throws Exception { 
        // primary expression
        String type = n.f1.accept(this, argu);
        if (!type.equals(BOOLEAN))
            throw new MyException(EXPRESSION, BOOLEAN, "not");
        
        return type;
    }

    public String visit(BooleanArrayAllocationExpression n, String scope) throws Exception {
        String type = n.f3.accept(this, scope);
        if (!type.equals(INT))
            throw new MyException(ARRAY_ALLOCATION, "", BOOLEAN);

        return BOOLEAN_ARRAY;
    }

    public String visit(IntegerArrayAllocationExpression n, String scope) throws Exception {
        String type = n.f3.accept(this, scope);
        if (!type.equals(INT))
            throw new MyException(ARRAY_ALLOCATION, "", INT);            

        return INT_ARRAY;
    }

    /**
     * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
    public String visit(AllocationExpression n, String argu) throws Exception {

        String identifier = n.f1.f0.toString();
        if (!symbolTable.classExists(identifier))
            throw new MyException(ALLOCATION, identifier, "");

        return identifier;
    }

    public String visit(ThisExpression n, String argu) throws Exception { return n.f0.tokenImage; }

    public String visit(BracketExpression n, String argu) throws Exception { return n.f1.accept(this, argu); }
    
    public String visit(BooleanType n, String argu) throws Exception { return BOOLEAN; }

	public String visit(IntegerType n, String argu) throws Exception { return INT; }

	public String visit(BooleanArrayType n, String argu) throws Exception { return BOOLEAN_ARRAY; }

	public String visit(IntegerArrayType n, String argu) throws Exception { return INT_ARRAY; }

    public String visit(IntegerLiteral n, String argu) throws Exception { return INT; }
    
    public String visit(TrueLiteral n, String argu) throws Exception { return BOOLEAN; }
    
    public String visit(FalseLiteral n, String argu) throws Exception { return BOOLEAN; }

    public String visit(NodeToken n, String argu) throws Exception { return n.tokenImage; }

    // scope = className + "," + methodName;
    public String visit(Identifier n, String scope) throws Exception { 
        if (n == null) 
            return "";
        
        if (scope != null) {
            String name = n.f0.toString(); 
            String className = scope.substring(0, scope.indexOf(','));
            String methodName = scope.substring(scope.indexOf(',') + 1);
            ClassType classVar = symbolTable.table.get(className);
            MethodType method = classVar.methods.get(methodName);

            if (method.locals.containsKey(name))
                return method.locals.get(name).type;
            else if (method.parameters.containsKey(name))
                return method.parameters.get(name).type;
            else if (classVar.variables.containsKey(name))
                return classVar.variables.get(name).type;
            else if (symbolTable.classInheritsClass(className))
                return checkInheritedLocals(name, className);   // if found returns variable's type, else empty string    
            else
                return UNDECLARED;    
        }
        return n.f0.toString();
    }
}
