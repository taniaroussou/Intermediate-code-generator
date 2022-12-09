import syntaxtree.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: java Main <inputFile>");
            System.exit(1);
        }

        FileInputStream fis = null;
        for (int i = 0; i < args.length; i++) {
            try {
                fis = new FileInputStream(args[i]);
                MiniJavaParser parser = new MiniJavaParser(fis);
    
                Goal root = parser.Goal();
    
                System.err.println("File name: \"" + args[i] + "\". Program parsed successfully.");
         
                SymbolTableVisitor symbolTableVisitor = new SymbolTableVisitor();
                root.accept(symbolTableVisitor, null);

                TypeCheckingVisitor typeCheckingVisitor = new TypeCheckingVisitor(symbolTableVisitor.symbolTable);
                root.accept(typeCheckingVisitor, null);

                symbolTableVisitor.symbolTable.calculateOffsets();

                CodeGeneratorVisitor codeGenVisitor = new CodeGeneratorVisitor(symbolTableVisitor.symbolTable);
                root.accept(codeGenVisitor, null);

                codeGenVisitor.writeBufferToFile(args[i]);
            }
            catch (ParseException ex) {
                System.out.println(ex.getMessage());
            }
            catch (FileNotFoundException ex) {
                System.err.println(ex.getMessage());
            }
            catch (MyException ex) {
                System.err.println(ex.message);
            }
            finally {
                try {
                    if (fis != null) fis.close();
                }
                catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }
    }
}