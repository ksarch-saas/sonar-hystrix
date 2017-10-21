package org.ksarch.sonar.checks;

import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.TypeTree;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.List;

public class HystrixCheck extends BaseTreeVisitor implements JavaFileScanner {
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface HystrixCommand {
        String fallbackMethod();
    }

    private JavaFileScannerContext context;
    private Boolean implementsHystrix = Boolean.FALSE;

    public void scanFile(JavaFileScannerContext context) {
        this.context = context;
        scan(context.getTree());
    }

    @Override
    public void visitClass(ClassTree tree) {
        if (tree.superClass() != null) {
            /* with superclass */
            String superClassName = tree.superClass().getClass().getName();
            if (superClassName.equals("HystrixCommand")) {
                /* hystrix command, find the method 'getFallback'*/
                List<TypeTree> interfaces = tree.superInterfaces();
                for (TypeTree typeTree : interfaces) {
                    if ("getFallback".equals(typeTree.toString())) {
                        implementsHystrix = Boolean.TRUE;
                    }
                }

                if (!implementsHystrix) {
                    /* no fallback found, report the defect */
                    context.reportIssue(this, tree, String.format("There is no fallback method in class @%s", tree.getClass().getName()));
                }
            }
        }

        /* possible spring annotation usage */
        Method[] methods = tree.getClass().getDeclaredMethods();
        for (Method method : methods) {
            HystrixCommand hystrixCommand = method.getAnnotation(HystrixCommand.class);
            if (hystrixCommand == null) {
                continue;
            }

            /* Hystrix, check fallback */
            String fallback = hystrixCommand.fallbackMethod();
            if (fallback.length() <= 0) {
                /* no fallback found, report the defect */
                context.reportIssue(this, tree, String.format("There is no fallback for method @%s", method.getName()));
            }
        }

        // The call to the super implementation allows to continue the visit of the AST.
        // Be careful to always call this method to visit every node of the tree.
        super.visitClass(tree);
    }
}