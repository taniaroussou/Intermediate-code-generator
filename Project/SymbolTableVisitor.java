import syntaxtree.*;
import visitor.*;
import static utilities.Constants.*;

class SymbolTableVisitor extends GJDepthFirst<String, String> {

    SymbolTable symbolTable;

    public SymbolTableVisitor() {
        symbolTable = new SymbolTable();
    }
    
    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
    */
    public String visit(MainClass n, String argu) throws Exception {
        String identifier = n.f1.accept(this, argu);
        symbolTable.insertClass(identifier, "");
        if (symbolTable.insertMethod("main", VOID, identifier) == null)
            throw new MyException(DOUBLE_DECL, "main", METHOD);

        String argsId = n.f11.accept(this, null);
        symbolTable.insertVariable(argsId, STRING_ARRAY, identifier + ",main", FORMAL_PARAMETER);
        if (n.f14.present())
            n.f14.accept(this, METHOD_BODY + " " + identifier + ",main");

        // n.f15.accept(this, argu);
        return null;
    }
    
    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     */
    public String visit(ClassDeclaration n, String argu) throws Exception {		
        String identifier = n.f1.accept(this, null);
		if (symbolTable.insertClass(identifier, "").equals(DOUBLE_DECL))
			throw new MyException(DOUBLE_DECL, identifier, n.f0.accept(this, argu).toString());	

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
		String parentIdentifier = n.f3.accept(this, null);

        String ret = symbolTable.insertClass(identifier, parentIdentifier);
		if (ret.equals(DOUBLE_DECL))
			throw new MyException(DOUBLE_DECL, identifier, n.f0.accept(this, argu).toString());
        else if (ret.equals(UNDECLARED))
            throw new MyException(UNDECLARED, parentIdentifier, CLASS);

        if (n.f5.present())
		    n.f5.accept(this, CLASS + " " + identifier);

		if (n.f6.present())
            n.f6.accept(this, identifier);

		return null;
	}

        /**
    * f0 -> "public"
    * f1 -> Type()
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( FormalParameterList() )?
    * f5 -> ")"
    * f6 -> "{"
    * f7 -> ( VarDeclaration() )*
    * f8 -> ( Statement() )*
    * f9 -> "return"
    * f10 -> Expression()
    * f11 -> ";"
    * f12 -> "}"
    */
    public String visit(MethodDeclaration n, String className) throws Exception {
        String type = n.f1.accept(this, null);
        String methodName = n.f2.accept(this, null);

        String ret = symbolTable.insertMethod(methodName, type, className);

        if (ret.equals(DOUBLE_DECL))
            throw new MyException(DOUBLE_DECL, methodName, METHOD);
        else if (ret.equals(NON_VIRTUAL))
            throw new MyException(ret, methodName, "");

        if (n.f4.present())
            n.f4.accept(this, FORMAL_PARAMETER + " " + className + "," + methodName);

        if (n.f7.present())
            n.f7.accept(this, METHOD_BODY + " " + className + "," + methodName);
            
        return null;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
    public String visit(FormalParameter n, String typeId) throws Exception {
        String type = n.f0.accept(this, null);
        String identifier = n.f1.accept(this, null);
        String scope = typeId.substring(0, typeId.indexOf(' '));
        String className = typeId.substring(typeId.indexOf(' ') + 1);

  
  
        String ret = symbolTable.insertVariable(identifier, type, className, scope);
		switch (ret) {
            case UNDECLARED:
                throw new MyException(UNDECLARED, className.substring(0, className.indexOf(',')), CLASS);
            case DOUBLE_DECL:
                throw new MyException(ret, type + " " + identifier + " in parameters list", VARIABLE);
        }
            
        return null;
    }

    /* VarDeclaration 
    * f0 -> Type()
    * f1 -> Identifier()
    */
    public String visit(VarDeclaration n, String typeId) throws Exception {
		String type = n.f0.accept(this, null);
        String identifier = n.f1.accept(this, null);
        String scope = typeId.substring(0, typeId.indexOf(' '));
        String className = typeId.substring(typeId.indexOf(' ') + 1);
        String ret = symbolTable.insertVariable(identifier, type, className, scope);

        if (!scope.equals(CLASS))
            scope = scope.equals(METHOD_BODY) ? "method body" : "parameters list";

		switch (ret) {
            case UNDECLARED:
                throw new MyException(UNDECLARED, className.substring(0, className.indexOf(',')), CLASS);
            case DOUBLE_DECL:                
                throw new MyException(ret, type + " " + identifier + " in " + scope, VARIABLE);
        }
        return null;
    }

	public String visit(BooleanType n, String argu) throws Exception { return BOOLEAN; }

	public String visit(IntegerType n, String argu) throws Exception { return INT; }

	public String visit(BooleanArrayType n, String argu) throws Exception { return BOOLEAN_ARRAY; }

	public String visit(IntegerArrayType n, String argu) throws Exception { return INT_ARRAY; }

	public String visit(NodeToken n, String argu) throws Exception { return n.tokenImage; }

    public String visit(Identifier n, String argu) throws Exception { return n.f0.toString(); }
}