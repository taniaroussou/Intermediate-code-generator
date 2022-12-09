import java.util.ArrayList;
import java.util.LinkedHashMap;

public class MethodType extends SymbolType { 
    String returnType;
    LinkedHashMap<String, Variable> locals;
    LinkedHashMap<String, Variable> parameters;
    ArrayList<String> callArgs;

    MethodType(String name, String returnType) {
        super(name);
        this.returnType = returnType;
        locals = new LinkedHashMap<String, Variable>();
        parameters = new LinkedHashMap<String, Variable>();
        callArgs = new ArrayList<String>();
    }
}
