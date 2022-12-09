import static utilities.Constants.*;

public class MyException extends Exception{
    String message;

    MyException(String type, String name, String varType) {
        switch(type) {
            case DOUBLE_DECL:
                this.message = "Error: " + varType + " " + name + " has already been declared"; 
                break;
            case UNDECLARED:
                this.message = "Error: " + varType  + " " + name + " has not been declared";
                break; 
            case NON_VIRTUAL:
                this.message = "Error: " + name + " method must be virtual";
                break;
            case PRINTSTATEMENT:
                this.message = "Error: System.out.println() accepts only int arguments"; 
                break;
            case EXPRESSION:
                if (varType.equals("not"))
                    this.message = "Error: " + varType + " expression requires boolean after !";
                else if (varType.equals("if") || varType.equals("while"))
                    this.message = "Error: " + varType + " expression must be evaluated to boolean";
                else
                    this.message = "Error: " + varType + " expression requires " + name + " operands";
                break;
            case STATEMENT:
                this.message = "Error in method " + name + " incompatible types in " + varType + " statement";
                break;
            case ASSIGNMENT:
                if (varType.equals("index"))
                    this.message = "Error in method " + name + " , expression in square brackets must be of type int";
                else if (varType.equals("expression"))
                    this.message = "Error in method " + name + " , Bad type assignment";
                break;
            case ARRAY_ACCESS:
                this.message = "Error: array access on invalid type";
                break;
            case INVALID_INDEX:
                this.message = "Error: invalid type of array index";
                break;
            case ARRAY_LENGTH:
                this.message = "Error: Cannot access property length of non array type";
                break;
            case MEMBER_CALL:
                this.message = "Error: Member call only valid on class instances";
                break;
            case MEMBER_CALL_UNDECL:
                this.message = "Error: Member call on underfined variable";
                break;
            case SUBTYPE:
                this.message = "Error in assignment: type " + name.substring(0, name.indexOf(',')) + " is not a subtype of " + name.substring(name.indexOf(',') + 1);
                break;
            case RETURN_TYPE:
                this.message = "Error in method " + name + ", wrong return type";
                break;
            case USE_ARGS:
                this.message = "Error in public static main method: Cannot use String[] args parameter";
                break;
            case WRONG_ARGS:
                this.message = "Error: wrong arguments in method call " + name;
                break;
            case METHOD_NOTFOUND:
                this.message = "Error: method " + name + " does not exist";
                break;
            case THIS_MAIN:
                this.message = "Error: Cannot use \"this\" keyword in public static void main method";
                break;
            case ARRAY_ALLOCATION:
                this.message = "Error: " + varType + " array allocation requires index of type int";
                break;
            case ALLOCATION:
                this.message = "Error in allocation expression: Unknown class type";
                break;
            default:
                this.message = "Error";
        }
    }
}
