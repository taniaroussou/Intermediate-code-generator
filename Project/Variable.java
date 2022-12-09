public class Variable extends SymbolType {
    String type;    

    Variable(String name, String type) {
        super(name);
        this.type = type;
    }
}
