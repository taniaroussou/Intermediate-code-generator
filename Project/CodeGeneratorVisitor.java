import visitor.GJDepthFirst;
import syntaxtree.*;
import static utilities.Constants.*;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class CodeGeneratorVisitor extends GJDepthFirst<String, String> {
    SymbolTable symbolTable;
    LinkedHashMap<String, HashMap<String, MethodType>> vtables;
    StringBuilder buffer;
    int register;
    int arrayLabelCounter;
    int andClauseCounter;
    int loopCounter;
    int ifCounter;
    String currentType;
    String currentCallClassName;
    ArrayList<String> currentCallArgs;
    Boolean load;

    public CodeGeneratorVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.vtables = new LinkedHashMap<>();
        this.buffer = new StringBuilder();
        this.register = 0;
        this.arrayLabelCounter = 0;
        this.andClauseCounter = 0;
        this.loopCounter = 0;
        this.ifCounter = 0;
        this.currentType = "";
        this.currentCallClassName = "";
        this.load = false;
        this.currentCallArgs = new ArrayList<>();
        createVtables();
        addHelperMethods();
      
        // printVtables();
    }

    private void addHelperMethods() {
        buffer.append("declare i8* @calloc(i32, i32)\n");
        buffer.append("declare i32 @printf(i8*, ...)\n");
        buffer.append("declare void @exit(i32)\n\n");
        buffer.append("@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n");
        buffer.append("@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n");
        buffer.append("define void @print_int(i32 %i) {\n\t");
        buffer.append("%_str = bitcast [4 x i8]* @_cint to i8*\n\t");
        buffer.append("call i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n\t");
        buffer.append("ret void\n}\n\n");
        buffer.append("define void @throw_oob() {\n\t");
        buffer.append("%_str = bitcast [15 x i8]* @_cOOB to i8*\n\t");
        buffer.append("call i32 (i8*, ...) @printf(i8* %_str)\n\t");
        buffer.append("call void @exit(i32 1)\n\t");
        buffer.append("ret void\n}\n\n");
    }

    // DEBUG ONLY
    public void printVtables() {
        for(String classKey : vtables.keySet()) {
            System.out.println("class: " + classKey);
            for(String method : vtables.get(classKey).keySet()) {
                System.out.println("method: " + method);
                System.out.println("offset: " + vtables.get(classKey).get(method).offset);
                System.out.print("parameters: ");
                for(String param : vtables.get(classKey).get(method).parameters.keySet()) {
                    Variable variable = vtables.get(classKey).get(method).parameters.get(param);
                    System.out.print(variable.type + ", ");
                }
                System.out.println("\nreturn type: " + vtables.get(classKey).get(method).returnType + "\n");
            }
        }
    }

    private void createVtables() {
        for(String className : symbolTable.table.keySet()) {
            ClassType classType = symbolTable.table.get(className);
            vtables.put(className, new HashMap<String, MethodType>());
            buffer.append("@." + className + "_vtable = global [");
            // main class
            if (classType.methods.size() == 1 && ((MethodType)classType.methods.values().toArray()[0]).returnType.equals(VOID)) {
                buffer.append("0 x i8*] []\n");
                continue;
            }
            ArrayList<String> methodSignaturesIR = getMethodsSignatures(className);
            buffer.append(methodSignaturesIR.size());
            buffer.append(" x i8*] [");
            methodSignaturesIR.forEach(s -> buffer.append(s));
            buffer.append("]\n");
        }
        buffer.append("\n\n");
    }

    private String getType(String type) {
        switch (type) {
            case INT:
                return "i32";
            case INT_ARRAY:
                return "i32*";
            case BOOLEAN:
                return "i1";
            default:
                return "i8*";
        }
    }

    private String getMethodParametersTypes(MethodType method) {
        ArrayList<Variable> params = new ArrayList<Variable>(method.parameters.values());
        String resultStr = params.size() > 0 ? "i8*," : "i8*";
        for (Variable parameter : params) {
            resultStr += getType(parameter.type);
            if (parameter != params.get(params.size() - 1))
                resultStr += ',';
        }
        return resultStr;
    }

    private String getMethodSignature(String className, MethodType method) {
        String parametersTypes = getMethodParametersTypes(method);
        String signature = String.format("i8* bitcast (%1$s (%2$s)* @%3$s.%4$s to i8*)", getType(method.returnType), parametersTypes, className, method.name);

        return signature;
    }

    private ArrayList<String> getMethodsSignatures(String className) {
        ClassType derivedClass = symbolTable.table.get(className);
        String derivedClassName = derivedClass.name;
        // get derived class methods
        ArrayList<MethodType> methodTypes = new ArrayList<MethodType>(derivedClass.methods.values());
        ArrayList<String> methodNames = new ArrayList<String>();
        ArrayList<String> methodSignaturesIR = new ArrayList<String>();

        // get the names of each method of derived class and get its signature
        methodTypes.forEach((m) -> {    methodNames.add(m.name); 
                                        methodSignaturesIR.add(getMethodSignature(derivedClassName, m)); 
                                        vtables.get(derivedClassName).put(m.name, m);
                                    });        
        
        while (symbolTable.classInheritsClass(derivedClass.name)) {
            ClassType baseClass = symbolTable.table.get(derivedClass.superClass);

            // get base class methods
            ArrayList<MethodType> baseClassmethodTypes = new ArrayList<MethodType>(baseClass.methods.values());
            String baseClassName = baseClass.name;

            // check if derived class inherits method from parent
            baseClassmethodTypes.forEach((m) -> { if (!methodNames.contains(m.name)) {
                                                    methodNames.add(m.name); 
                                                    methodSignaturesIR.add(getMethodSignature(baseClassName, m));
                                                    vtables.get(derivedClassName).put(m.name, m);
                                                } });           
            derivedClass = baseClass;                                  
        }

        for (int i = 0; i < methodSignaturesIR.size(); i++) {
            // not the last element
            if (methodSignaturesIR.get(i) != methodSignaturesIR.get(methodSignaturesIR.size() - 1))
                methodSignaturesIR.set(i, methodSignaturesIR.get(i) + ", ") ;
        }        
        return methodSignaturesIR;
    }

    private void allocateFormalParams(MethodType method) {
        for(Variable param : method.parameters.values()) {
            String type = getType(param.type);
            buffer.append("\t%" + param.name + " = alloca " + type + "\n");
            buffer.append("\tstore " + type + " %." + param.name + ", " + type + "* %" + param.name + "\n");
        }
    }

    public void writeBufferToFile(String fileName) {
        try {
            // create file and write buffer
            File file = new File(fileName);
            String llfileName = file.getName().substring(0, file.getName().lastIndexOf('.')) + ".ll";
            // FileWriter fw = new FileWriter(new File("ll_files", llfileName));
            FileWriter fw = new FileWriter(new File(llfileName));

            fw.write(buffer.toString());
            fw.close();
        }
        catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }
    
    public String visit(MainClass n, String argu) throws Exception { 
        String id = n.f1.accept(this, null);
        currentCallClassName = id;

        buffer.append("define i32 @main() {\n");       

        n.f14.accept(this, METHOD_BODY + " " + id + ",main");
        n.f15.accept(this, id + ",main");

        buffer.append("\n\tret i32 0\n}\n\n");
        return null; 
    }

    public String visit(ClassDeclaration n, String argu) throws Exception { 
        String identifier = n.f1.accept(this, null);
        currentCallClassName = identifier;

        if (n.f4.present())
            n.f4.accept(this, identifier);       

        return null; 
    }

	public String visit(ClassExtendsDeclaration n, String argu) throws Exception { 
        String identifier = n.f1.accept(this, null);
        currentCallClassName = identifier;

        if (n.f6.present())
            n.f6.accept(this, identifier);

		return null;
    }

    public String visit(MethodDeclaration n, String className) throws Exception { 
        String methodName = n.f2.accept(this, null);
        MethodType method = symbolTable.table.get(className).methods.get(methodName);
        String returnType = getType(method.returnType);
        String scope = className + "," + methodName;

        buffer.append("define " + returnType + " @" + className + "." + methodName + "(i8* %this");
        // formal parameters list
        for(Variable var : method.parameters.values()) {
            buffer.append(", " + getType(var.type) + " %." + var.name);
        }
        buffer.append(") {\n");
        allocateFormalParams(method);
        
        register = 0;
        // ( VarDeclaration() )*
        n.f7.accept(this, METHOD_BODY);

        // ( Statement() )*
        n.f8.accept(this,  scope);

        // return expression
        String retValue = n.f10.accept(this, scope);
        buffer.append("\tret " + returnType + " " + retValue);

        buffer.append("\n}\n\n");
        return null; 
    }

    public String visit(VarDeclaration n, String scope) throws Exception { 
        if (!scope.equals(CLASS)) {
            String type = getType(n.f0.accept(this, null));
            String identifier = n.f1.accept(this, null);
            buffer.append("\t%" + identifier + " = alloca " + type + "\n");
        }
        return null; 
    }

    public String visit(AssignmentStatement n, String scope) throws Exception { 
   
        load = true;
        String rValue = n.f2.accept(this, scope);
        load = false;
        String lValue = n.f0.accept(this, scope);
        load = true;
        buffer.append("\tstore " + getType(currentType) + " " + rValue + ", " + getType(currentType) + "* " + lValue + "\n");

        return null; 
    }

    public String visit(ArrayAssignmentStatement n, String scope) throws Exception { 
        String label1 = "array_assign" + arrayLabelCounter++;
        String label2 = "array_assign" + arrayLabelCounter++;
        String label3 = "array_assign" + arrayLabelCounter++;
        
        String _register = n.f0.accept(this, scope);
        
        buffer.append("\t%_" + register + " = load i32, i32* " + _register + "\n");
        int _reg = register;
        register++;

        String expr1 = n.f2.accept(this, scope);

        buffer.append("\t%_" + register + " = icmp ult i32 " + expr1 + ", %_" + _reg + "\n");
        buffer.append("\tbr i1 %_" + register++ + ", label %" + label1 + ", label %" + label2 + "\n\n");
        buffer.append(label1 + ":\n");

        String expr2 = n.f5.accept(this, scope);

        buffer.append("\t%_" + register + " = add i32 " + expr1 + ", 1\n");
        _reg = register;
        register++;
   
        buffer.append("\t%_" + register + " = getelementptr i32, i32* " + _register + ", i32 %_" + _reg + "\n");
        buffer.append("\tstore i32 " + expr2 + ", i32* %_" + register++ + "\n");
        buffer.append("\tbr label %" + label3 + "\n\n");
        buffer.append(label2 + ":\n");
        buffer.append("\tcall void @throw_oob()\n");
        buffer.append("\tbr label %" + label3 + "\n\n");
        buffer.append(label3 + ":\n");
        
        return null; 
    }

    public String visit(IfStatement n, String scope) throws Exception { 
        String label1 = "if" + ifCounter++;
        String label2 = "if" + ifCounter++;
        String label3 = "if" + ifCounter++;
        
        String expr = n.f2.accept(this, scope);

        buffer.append("\tbr i1 " + expr + ", label %" + label1 + ", label %" + label2 + "\n\n");
        buffer.append(label1 + ":\n");

        n.f4.accept(this, scope);

        buffer.append("\tbr label %" + label3 + "\n\n");
        buffer.append(label2 + ":\n");

        n.f6.accept(this, scope);

        buffer.append("\tbr label %" + label3 + "\n\n");
        buffer.append(label3 + ":\n");
        
        return null; 
    }

    public String visit(WhileStatement n, String scope) throws Exception { 
        String label1 = "loop" + loopCounter++;
        String label2 = "loop" + loopCounter++;
        String label3 = "loop" + loopCounter++;
        
        buffer.append("\tbr label %" + label1 + "\n\n");
        buffer.append(label1 + ":\n");

        String expr = n.f2.accept(this, scope);
    
        buffer.append("\tbr i1 " + expr + ", label %" + label2 + ", label %" + label3 + "\n\n");
        buffer.append(label2 + ":\n");

        n.f4.accept(this, scope);

        buffer.append("\tbr label %" + label1 + "\n\n");
        buffer.append(label3 + ":\n");
        
        return null; 
    }

    public String visit(PrintStatement n, String scope) throws Exception { 
        String ret = n.f2.accept(this, scope);
        buffer.append("\tcall void (i32) @print_int(i32 " + ret + ")\n");
    
        return null; 
    }

    public String visit(AndExpression n, String scope) throws Exception {
        String label1 = "andClause" + andClauseCounter++;
        String label2 = "andClause" + andClauseCounter++;
        String label3 = "andClause" + andClauseCounter++;
        String label4 = "andClause" + andClauseCounter++;

        String _register1 = n.f0.accept(this, scope);

        buffer.append("\tbr i1 " + _register1 + ", label %" + label1 + ", label %" + label2 + "\n\n");
        buffer.append(label2 + ":\n");
        buffer.append("\tbr label %" + label3 + "\n\n");
        buffer.append(label1 + ":\n");

        String _register2 = n.f2.accept(this, scope);

        buffer.append("\tbr label %" + label3 + "\n\n");
        buffer.append(label3 + ":\n");
        buffer.append("\tbr label %" + label4 + "\n\n");
        buffer.append(label4 + ":\n");
        buffer.append("\t%_" + register++ + " = phi i1 [ 0, %" + label2 + "], [" + _register2 + ", %" + label3 + "]\n");

        return "%_" + (register - 1);
    }

    public String visit(CompareExpression n, String scope) throws Exception { 
        String _register1 = n.f0.accept(this, scope);
        String _register2 = n.f2.accept(this, scope);

        buffer.append("\t%_" + register++ + " = icmp slt i32 " + _register1 + ", " + _register2 + "\n");

        return "%_" + (register -1); 
    }

    public String visit(PlusExpression n, String scope) throws Exception { 
        String _register1 = n.f0.accept(this, scope);
        String _register2 = n.f2.accept(this, scope);

        buffer.append("\t%_" + register++ + " = add i32 " + _register1 + ", " + _register2 + "\n");

        return "%_" + (register -1); 
    }

    public String visit(MinusExpression n, String scope) throws Exception { 
        String _register1 = n.f0.accept(this, scope);
        String _register2 = n.f2.accept(this, scope);

        buffer.append("\t%_" + register++ + " = sub i32 " + _register1 + ", " + _register2 + "\n");

        return "%_" + (register -1);
    }

    public String visit(TimesExpression n, String scope) throws Exception { 
        String _register1 = n.f0.accept(this, scope);
        String _register2 = n.f2.accept(this, scope);

        buffer.append("\t%_" + register++ + " = mul i32 " + _register1 + ", " + _register2 + "\n");

        return "%_" + (register -1); 
    }

    public String visit(MessageSend n, String scope) throws Exception {
        String _register = n.f0.accept(this, scope);
        int tmp_reg;

        buffer.append("\t%_" + register++ + " = bitcast i8* " + _register + " to i8***\n");
        buffer.append("\t%_" + register + " = load i8**, i8*** %_" + (register - 1) + "\n");
        register++;

        HashMap<String, MethodType> methodMap = vtables.get(currentType);
        String methodId = n.f2.accept(this, null);
        MethodType method = methodMap.get(methodId);
        int offset = method.offset / 8;
        String methodParams = getMethodParametersTypes(method);

        buffer.append("\t%_" + register + " = getelementptr i8*, i8** %_" + (register - 1) + ", i32 " + offset + "\n");
        register++;
        buffer.append("\t%_" + register + " = load i8*, i8** %_" + (register - 1) + "\n");
        register++;
        buffer.append("\t%_" + register + " = bitcast i8* %_" + (register - 1) + " to " + getType(method.returnType) + " (" + methodParams + ")*\n");
        tmp_reg = register;
        register++;

        n.f4.accept(this, scope);

        currentCallArgs.add(0, _register);

        String[] parametersTypes = methodParams.split(",");
        String args = "";

        if (currentCallArgs.size() == parametersTypes.length) { 
            for (int i = 0; i < parametersTypes.length; i++) {
                args = args + parametersTypes[i] + " " + currentCallArgs.get(i) + ","; 
            }
            if (!args.isEmpty())
                args = args.substring(0, args.length() - 1);
        }

        buffer.append("\t%_" + register + " = call " + getType(method.returnType) + " %_" + tmp_reg + "(" + args + ")\n");
        tmp_reg = register;
        register++;
        currentType = method.returnType;
        currentCallArgs.clear();
        
        return "%_" + tmp_reg; 
    }

    public String visit(ExpressionList n, String scope) throws Exception { 
        currentCallArgs.add(n.f0.accept(this, scope));
        n.f1.accept(this, scope);

        return null; 
    }

    public String visit(ExpressionTerm n, String scope) throws Exception { 
        currentCallArgs.add(n.f1.accept(this, scope));
        return null; 
    }
 
    public String visit(ExpressionTail n, String scope) throws Exception { 
        return n.f0.accept(this, scope);
    }

    public String visit(Expression n, String scope) throws Exception {
        return n.f0.accept(this, scope);
    }

    public String visit(ArrayLookup n, String scope) throws Exception { 
        String label1 = "array_lookup" + arrayLabelCounter++;
        String label2 = "array_lookup" + arrayLabelCounter++;
        String label3 = "array_lookup" + arrayLabelCounter++;

        String _register = n.f0.accept(this, scope);
        
        int tmpReg;

        buffer.append("\t%_" + register + " = load i32, i32* " + _register + "\n");
        tmpReg = register;

        register++;
        String expr = n.f2.accept(this, scope);
  
        buffer.append("\t%_" + register + " = icmp ult i32 " + expr + ", %_" + (tmpReg) + "\n");

        buffer.append("\tbr i1 %_" + register++ + ", label %" + label1 + ", label %" + label2 + "\n\n");
        buffer.append(label1 + ":\n");
        buffer.append("\t%_" + register + " = add i32 " + expr + ", 1\n");
        buffer.append("\t%_" + (register + 1) + " = getelementptr i32, i32* " + _register + ", i32 %_" + register + "\n");
        tmpReg = ++register;
        buffer.append("\t%_" + ++register + " = load i32, i32* %_" + tmpReg + "\n");
        buffer.append("\tbr label %" + label3 + "\n\n");
        buffer.append(label2 + ":\n");
        buffer.append("\tcall void @throw_oob()\n");
        buffer.append("\tbr label %" + label3 + "\n\n");
        buffer.append(label3 + ":\n");
        
        return "%_" + register++; 
    }

    public String visit(ArrayLength n, String scope) throws Exception {
        String _register = n.f0.accept(this, scope);

        buffer.append("\t%_" + register++ + " = load i32, i32* " + _register + "\n");
        
        return "%_" + (register - 1); 
    }     

    public String visit(NotExpression n, String scope) throws Exception {  
        String _register = n.f1.accept(this, scope);
        buffer.append("\t%_" + register++ + " = xor i1 1," + _register + "\n");

        return "%_" + (register -1); 
    }

    public String visit(BooleanArrayAllocationExpression n, String scope) throws Exception {
        String expr = n.f3.accept(this, scope);

        return arrayAllocation(expr, BOOLEAN);
    }

    public String visit(IntegerArrayAllocationExpression n, String scope) throws Exception {
        String expr = n.f3.accept(this, scope);

        return arrayAllocation(expr, INT);
    }

    String arrayAllocation(String expr, String type) {
      
        String label1 = type + "_arr_alloc" + arrayLabelCounter++;
        String label2 = type + "_arr_alloc" + arrayLabelCounter++;

        buffer.append("\t%_" + register + " = icmp slt i32 " + expr + ", 0\n");
        buffer.append("\tbr i1 %_" + register++ + ", label %" + label1 + ", label %" + label2 + "\n");
        buffer.append(label1 + ":\n");
        buffer.append("\tcall void @throw_oob()\n");
        buffer.append("\tbr label %" + label2 + "\n");
        buffer.append(label2 + ":\n");
        buffer.append("\t%_" + register + " = add i32 " + expr + ", 1\n");
        register++;
        buffer.append("\t%_" + register + " = call i8* @calloc(i32 4, i32 %_" + (register - 1) + ")\n");
        register++;
        buffer.append("\t%_" + register + " = bitcast i8* %_" + (register - 1) + " to i32*\n");
        register++;
        buffer.append("\tstore i32 " + expr + ", i32* %_" + (register - 1) + "\n");

        return "%_" + (register - 1);
    }

    public String visit(AllocationExpression n, String argu) throws Exception { 
        String className = n.f1.accept(this, null);
        int offset = symbolTable.getClassSize(className);
        int methodCount = vtables.get(className).size();

        if (methodCount > 0) {
            buffer.append("\t%_" + register++ + " = call i8* @calloc(i32 1, i32 " + offset + ")\n");
            buffer.append("\t%_" + register + " = bitcast i8* %_" + (register - 1) + " to i8***\n");
            buffer.append("\t%_" + ++register + " = getelementptr [" + methodCount + " x i8*], [" + methodCount + " x i8*]* @." + className + "_vtable, i32 0, i32 0\n");
            buffer.append("\tstore i8** %_" + register + ", i8*** %_" + (register - 1) + "\n");
            register++;
            currentType = className;

        }
        
        return "%_" + (register - 3); 
    }


    public String visit(IntegerLiteral n, String argu) throws Exception { 
        currentType = INT;
        return n.f0.accept(this, argu); 
    }
    
    public String visit(TrueLiteral n, String argu) { 
        currentType = BOOLEAN;
        return TRUE; 
    }
    
    public String visit(FalseLiteral n, String argu) { 
        currentType = BOOLEAN;
        return FALSE; 
    }

    public String visit(ThisExpression n, String scope) {
        currentType = currentCallClassName;
        return "%" + THIS; 
    }

    public String visit(BracketExpression n, String argu) throws Exception { return n.f1.accept(this, argu); }
    
    public String visit(BooleanType n, String argu) { return BOOLEAN; }

	public String visit(IntegerType n, String argu) { return INT; }

	public String visit(BooleanArrayType n, String argu) { return BOOLEAN_ARRAY; }

	public String visit(IntegerArrayType n, String argu) { return INT_ARRAY; }

    public String visit(Identifier n, String scope) { 
        String identifier = n.f0.toString();

        if (scope == null)
            return identifier;

        String className = scope.substring(0, scope.indexOf(','));
        String methodName = scope.substring(scope.indexOf(',') + 1);
        Variable var = null;

        Boolean isClassField = symbolTable.variableExistsRecursive(identifier, className);
        Boolean isMethodLocal = symbolTable.table.get(className).methods.get(methodName).locals.containsKey(identifier);
        Boolean isMethodFormalParam = symbolTable.variableExists(identifier, className, FORMAL_PARAMETER, methodName); 
           
        if (isMethodLocal) 
            var = symbolTable.table.get(className).methods.get(methodName).locals.get(identifier);        
        else if (isMethodFormalParam) 
            var = symbolTable.table.get(className).methods.get(methodName).parameters.get(identifier);
        else if (isClassField) {
            if (symbolTable.table.get(className).variables.containsKey(identifier))
                var = symbolTable.table.get(className).variables.get(identifier);
            else 
                var = symbolTable.getInheritedField(identifier, className);
        }

        // if identifier is class field get variable's offset            
        if (isClassField) {                
            int offset = var.offset + 8;

            buffer.append("\t%_" + register++ + " = getelementptr i8, i8* %this, i32 " + offset);
            buffer.append("\n\t%_" + register-- + " = bitcast i8* %_");
            buffer.append(register + " to " + getType(var.type) + "*\n");

            identifier = "%_" + ++register;
            register++;
        }
        else  {
            identifier = "%" + identifier;
        }

        if (load) {      
            buffer.append("\t%_" + register + " = load " + getType(var.type) + ", " + getType(var.type) + "* " + identifier + "\n"); 
            identifier = "%_" + register++;
        }
        currentType = var.type;

        return identifier;
     }

    public String visit(NodeToken n, String argu) {
        return n.toString();
    }
}