# Intermediate-code-generator
 Converting MiniJava code into the intermediate representation used by the LLVM compiler project.

## Types
Some of the available types are:

- ```i1``` - a single bit, used for booleans (practically takes up one byte)
- ```i8``` - a single byte
- ```i8*``` - similar to a char* pointer
- ```i32``` - a single integer
- ```i32*``` - a pointer to an integer, can be used to point to an integer array
- static arrays, e.g., [20 x i8] - a constant array of 20 characters

## Instructions to be used
- ```declare``` is used for the declaration of external methods. Only a few specific methods (e.g., ```calloc, printf```) need to be declared.
Example: ````declare i32 @puts(i8*)````

- ```define``` is used for defining our own methods. The return and argument types need to be specified, and the method needs to end with a ```ret``` instruction of the same type.
Example: ```define i32 @main(i32 %argc, i8** argv) {...}```

- ```ret``` is the return instruction. It is used to return the control flow and a value to the caller of the current function. Example: ```ret i32 %rv```

- ```alloca``` is used to allocate space on the stack of the current function for local variables. It returns a pointer to the given type. This space is freed when the method returns.
Example: ```%ptr = alloca i32```

- ```store``` is used to store a value to a memory location. The parameters are the value to be stored and a pointer to the memory.
Example: ```store i32 %val, i32* %ptr```

- ```load``` is used to load a value from a memory location. The parameters are the type of the value and a pointer to the memory.
Example: ```%val = load i32, i32* %ptr```

- ```call``` is used to call a method. The result can be assigned to a register. (LLVM bitcode temporary variables are called "registers".) The return type and parameters (with their types) need to be specified.
Example: ```%result = call i8* @calloc(i32 1, i32 %val)```

- ```add, and, sub, mul, xor``` are used for mathematical operations. The result is the same type as the operands.
Example: ```%sum = add i32 %a, %b```

- ```icmp``` is used for comparing two operands. ```icmp slt``` for instance does a signed comparison of the operands and will return ```i1 1``` if the first operand is less than the second, otherwise ```i1 0```.
Example: ```%case = icmp slt i32 %a, %b```

- ```br``` with a ```i1``` operand and two labels will jump to the first label if the ```i1``` is one, and to the second label otherwise.
Example: ```br i1 %case, label %if, label %else```

- ```br``` with only a single label will jump to that label.
Example: ```br label %goto```

- ```label```: declares a label with the given name. The instruction before declaring a label needs to be a ```br`` operation, even if that ```br``` is simply a jump to the label.
Example: ```label123:```

- ```bitcast```` is used to cast between different pointer types. It takes the value and type to be cast, and the type that it will be cast to.
Example: ```%ptr = bitcast i32* %ptr2 to i8**```

- ```getelementptr``` is used to get the pointer to an element of an array from a pointer to that array and the index of the element. The result is also a pointer to the type that is passed as the first parameter (in the case below it's an ```i8*```). This example is like doing ```ptr_idx = &ptr[idx]``` in C (you still need to do a ```load``` to get the actual value at that position). The first argument is always a type used as the basis for the calculations.
Example: ```%ptr_idx = getelementptr i8, i8* %ptr, i32 %idx```

- ```constant``` is used to define a constant, such as a string. The size of the constant needs to be declared too. In the example below, the string is 12 bytes (```[12 x i8]```). The result is a pointer to the given type (in the example below, ```@.str``` is a ```[12 x i8]*```).
Example: ```@.str = constant [12 x i8] c"Hello world\00"```

- ```global``` is used for declaring global variables - something you will need to do for creating v-tables. Just like ```constant```, the result is a pointer to the given type.
Example:
```@.vtable = global [2 x i8*] [i8* bitcast (i32 ()* @func1 to i8*), i8* bitcast (i8* (i32, i32*)* @func2 to i8*)]```

- ```phi``` is used for selecting a value from previous basic blocks, depending on which one was executed before the current block. Phi instructions must be the first in a basic block. It takes as arguments a list of pairs. Each pair contains the value to be selected and the predecessor block for that value. This is necessary in single-assignment languages, in places where multiple control-flow paths join, such as if-else statements, if one wants to select a value from the different paths. In the context of the exercise, you will need this for short-circuiting and (&&) expressions.
Example:
```
br i1 1, label %lb1, label %lb2
lb1:
    %a = add i32 0, 100
    br label %lb3
lb2:
    %b = add i32 0, 200
    br label %lb3
lb3:
    %c = phi i32 [%a, %lb1], [%b, %lb2]
```
    
## Execution
Execute the produced LLVM IR files in order to see that their output is the same as compiling the input java file with javac and executing it with java. To do that, you will need Clang with version >=4.0.0. You may download it on your Linux machine, or use it via SSH on the linuxvm machines.

## In Ubuntu
- ```sudo apt update && sudo apt install clang```
- Save the code to a file (e.g. ```ex.ll```)
- ```clang -o out1 ex.ll```
- ```./out1```

## In linuxvm machines
- ```/home/users/compilers/clang/clang -o out1 ex.ll```
- ```./out1```

## Run
```java Main.java [file1.java] [file2.java] ... [fileN.java]```
The program compiles to LLVM IR all .java files given as arguments. Moreover, the outputs is stored in files named file1.ll, file2.ll, ... fileN.ll respectively.
