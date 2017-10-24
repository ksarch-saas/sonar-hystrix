package org.ksarch.sonar.checks;

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
import java.util.List;


@Rule(key = "HystrixCheck", name = "Fallback")
public class HystrixCheck extends BaseTreeVisitor implements JavaFileScanner {
    private JavaFileScannerContext context;
    private Boolean implementsHystrix = Boolean.FALSE;
    private static final Logger LOGGER = LoggerFactory.getLogger(HystrixCheck.class);

    public void scanFile(JavaFileScannerContext context) {
        this.context = context;
        scan(context.getTree());
    }

    private Boolean vaildateAnnotation(List<AnnotationTree> annotations, String name, String key) {
        Boolean retVal = Boolean.TRUE;
        for (AnnotationTree annotationTree : annotations) {
            if (annotationTree.annotationType().is(Tree.Kind.IDENTIFIER)) {
                retVal = Boolean.FALSE;
                IdentifierTree idf = (IdentifierTree)annotationTree.annotationType();
                for (ExpressionTree argument : annotationTree.arguments()) {
                    if (argument.is(Tree.Kind.ASSIGNMENT)) {
                        AssignmentExpressionTree assignmentExpressionTree = (AssignmentExpressionTree) argument;
                        IdentifierTree nameTree = (IdentifierTree) assignmentExpressionTree.variable();
                        if (nameTree.name().equals(key)) {
                            retVal = Boolean.TRUE;
                        }
                    }
                }
            }
        }
        return retVal;
    }

    @Override
    public void visitMethod(MethodTree tree) {
        List<AnnotationTree> annotations = tree.modifiers().annotations();
        Boolean retVal = vaildateAnnotation(annotations, "HystrixCommand", "fallbackMethod");
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
                List<TypeTree> interfaces = tree.superInterfaces();
                for (TypeTree typeTree : interfaces) {
                    if ("getFallback".equals(typeTree.toString())) {
                        implementsHystrix = Boolean.TRUE;
                    }
                }

                if (!implementsHystrix) {
                    /* no fallback found, report the defect */
                    context.reportIssue(this, tree, String.format("There is no Hystrix fallback method in class @%s", tree.simpleName()));
                }
            }
        }

        /* check FeignClient */
        List<AnnotationTree> annotations = tree.modifiers().annotations();
        boolean retVal = vaildateAnnotation(annotations, "FeignClient", "fallback");
        if (!retVal) {
            context.reportIssue(this, tree, String.format("There is no Hystrix fallback processor for class @%s", tree.simpleName()));
        }

        super.visitClass(tree);
    }
}