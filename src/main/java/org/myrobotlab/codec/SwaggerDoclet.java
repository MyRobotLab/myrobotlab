package org.myrobotlab.codec;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doclet;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;


/**
 * This is a custom Doclet class that the javadoc tool uses
 * in order to generate custom javadoc that obtains all the comment from the source code
 * and saves it as a txt file in common/src/main/resources
 * 
 * This file is compiled and run before the rest of the project is compiled
 * (View the Jenkinsfile to see how this is compiled / run)
 * 
 * To run (from root): 
 * $ javac -cp common/swagger-javadoc/doclet.jar common/swagger-javadoc/SwaggerDoclet.java
 * $ javadoc -classpath common/src/main/java -docletpath common/swagger-javadoc -doclet SwaggerDoclet com.daimler.eb.endpoint
 */
public class SwaggerDoclet extends Doclet{
    public static boolean start(RootDoc root){
        try{ 
            FileWriter fw = new FileWriter(new File("common/src/main/resources/comments.txt"));

            for(ClassDoc c : root.classes()){
                for(MethodDoc m : c.methods()){
                    //in order to separate overloaded methods, we store the fully 
                    //qualified name of each method with the types of its parameters
                    String paramTypes = Arrays.toString(Arrays.stream(m.parameters()).map(x -> x.type()).toArray()).replace(" ", "");
                    fw.write(m.qualifiedName() + paramTypes + "\n" + m.commentText().replace("\n", "") + "\n");
                }
            }

            fw.close();

            return true;
        }catch(IOException e){
            e.printStackTrace();
            return false;
        }
    }
}