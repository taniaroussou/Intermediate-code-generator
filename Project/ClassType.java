import java.util.LinkedHashMap;

public class ClassType extends SymbolType {
    String superClass;
    LinkedHashMap<String, Variable> variables;
    LinkedHashMap<String, MethodType> methods;

    ClassType(String name, String superClass) {
        super(name);
        this.superClass = superClass;
        variables = new LinkedHashMap<String, Variable>();
        methods = new LinkedHashMap<String, MethodType>();
    }

}
