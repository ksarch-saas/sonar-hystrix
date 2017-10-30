package org.ksarch.sonar.checks;

import com.google.common.collect.Lists;
import com.sonar.sslr.api.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.*;

import java.beans.Expression;
import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;


@Rule(key = "HystrixCheck", name = "Fallback")
public class HystrixCheck extends BaseTreeVisitor implements JavaFileScanner {
    private JavaFileScannerContext context;
    private static final Logger LOGGER = LoggerFactory.getLogger(HystrixCheck.class);

    public void scanFile(JavaFileScannerContext context) {
        this.context = context;
        scan(context.getTree());
    }

    private static boolean validateAnnotation(List<AnnotationTree> annotations, String name, String key) {
        boolean retVal = true;
        for (AnnotationTree annotationTree : annotations) {
            if (annotationTree.annotationType().is(Tree.Kind.IDENTIFIER)) {
                retVal = false;
                IdentifierTree idf = (IdentifierTree)annotationTree.annotationType();
                for (ExpressionTree argument : annotationTree.arguments()) {
                    if (argument.is(Tree.Kind.ASSIGNMENT)) {
                        AssignmentExpressionTree assignmentExpressionTree = (AssignmentExpressionTree) argument;
                        IdentifierTree nameTree = (IdentifierTree) assignmentExpressionTree.variable();
                        if (nameTree.name().equals(key)) {
                            retVal = true;
                        }
                    }
                }
            }
        }
        return retVal;
    }

    private static boolean isThisOrSuper(Symbol symbol) {
        String name = symbol.name();
        return "this".equals(name) || "super".equals(name);
    }

    private static boolean isConstructor(Symbol symbol) {
        return "<init>".equals(symbol.name());
    }

    private static Collection<Symbol> filterMethod(Collection<Symbol> symbols) {
        List<Symbol> methods = Lists.newArrayList();
        for (Symbol symbol : symbols) {
            if (symbol.isMethodSymbol() && !isConstructor(symbol)) {
                methods.add(symbol);
            }
        }
        return methods;
    }

    private static boolean hasMehtod(Symbol.TypeSymbol classSymbol, String methodName) {
        Collection<Symbol> symbols = filterMethod(classSymbol.memberSymbols());
        if (symbols.isEmpty()) {
            return false;
        }

        for (Symbol symbol : symbols) {
            if (symbol.name().equals(methodName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void visitMethod(MethodTree tree) {
        List<AnnotationTree> annotations = tree.modifiers().annotations();
        boolean retVal = validateAnnotation(annotations, "HystrixCommand", "fallbackMethod");
        if (!retVal) {
            context.reportIssue(this, tree, String.format("There is no Hystrix fallback method in class @%s", tree.simpleName()));
        }
        super.visitMethod(tree);
    }

    @Override
    public void visitClass(ClassTree tree) {
        if (((ClassTree) tree).superClass() != null) {
            /* with superclass */
            /* For 'symbolType' usage, jar in dependencies must be on classpath, !unknownSymbol! result otherwise */
            String superClassName = tree.superClass().symbolType().name();
            if (superClassName.equals("HystrixCommand")) {
                /* hystrix command, find the method 'getFallback'*/
                if (!hasMehtod(tree.symbol(), "getfallback")) {
                    /* no fallback found, report the defect */
                    context.reportIssue(this, tree, String.format("There is no Hystrix fallback method in class @%s", tree.simpleName()));
                }
            }
        }

        /* check FeignClient */
        List<AnnotationTree> annotations = tree.modifiers().annotations();
        boolean retVal = validateAnnotation(annotations, "FeignClient", "fallback");
        if (!retVal) {
            context.reportIssue(this, tree, String.format("There is no Hystrix fallback processor for class @%s", tree.simpleName()));
        }

        super.visitClass(tree);
    }
}
