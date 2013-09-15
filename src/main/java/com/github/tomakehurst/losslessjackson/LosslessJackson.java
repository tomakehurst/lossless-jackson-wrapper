package com.github.tomakehurst.losslessjackson;

import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;

public class LosslessJackson {

    private static final String ANY_GETTER_ANNOTATION = "com.fasterxml.jackson.annotation.JsonAnyGetter";
    private static final String ANY_SETTER_ANNOTATION = "com.fasterxml.jackson.annotation.JsonAnySetter";

    @SuppressWarnings("unchecked")
    public static <T> Class<? extends T> lossless(Class<T> sourceClass) throws Exception {
        ClassPool classPool = ClassPool.getDefault();

        CtClass losslessClass = classPool.get(sourceClass.getName());
        losslessClass.setName(sourceClass.getName() + "_Lossless");
        losslessClass.setSuperclass(classPool.get(sourceClass.getName()));
        removeAllFieldsAndMethods(losslessClass);
        replaceBodyWithSuperCall(losslessClass.getConstructors()[0]);

        CtField otherAttributesField = CtField.make(
                "java.util.Map other = new java.util.LinkedHashMap();",
                losslessClass);
        losslessClass.addField(otherAttributesField);

        CtMethod anyMethod = CtNewMethod.make(
                "public java.util.Map any() {\n" +
                        "    return other;\n" +
                        "}",
                losslessClass);
        addAnnotationWithNoParams(losslessClass, anyMethod, ANY_GETTER_ANNOTATION);
        losslessClass.addMethod(anyMethod);

        CtMethod setMethod = CtNewMethod.make(
                "public void set(String name, Object value) {\n" +
                        "    other.put(name, value);\n" +
                        "}",
                losslessClass);
        addAnnotationWithNoParams(losslessClass, setMethod, ANY_SETTER_ANNOTATION);
        losslessClass.addMethod(setMethod);

        return losslessClass.toClass();
    }

    private static void replaceBodyWithSuperCall(CtConstructor constructor) throws Exception {
        StringBuilder sb = new StringBuilder("super(");
        for (int i = 1; i <= constructor.getParameterTypes().length; i++) {
            sb.append("$").append(i);
            if (i < constructor.getParameterTypes().length) {
                sb.append(", ");
            }
        }

        sb.append(");");
        constructor.setBody(sb.toString());
    }

    private static void removeAllFieldsAndMethods(CtClass losslessContactDetailsClass) throws NotFoundException {
        for (CtField field: losslessContactDetailsClass.getDeclaredFields()) {
            losslessContactDetailsClass.removeField(field);
        }

        for (CtMethod method: losslessContactDetailsClass.getDeclaredMethods()) {
            losslessContactDetailsClass.removeMethod(method);
        }
    }

    private static void addAnnotationWithNoParams(CtClass ctClass, CtBehavior ctElement, String annotationClass) {
        ConstPool constpool = getConstPool(ctClass);
        AnnotationsAttribute annotationAttribute = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
        Annotation annotation = new Annotation(annotationClass, constpool);
        annotationAttribute.addAnnotation(annotation);
        ctElement.getMethodInfo().addAttribute(annotationAttribute);
    }

    private static ConstPool getConstPool(CtClass ctClass) {
        ClassFile ccFile = ctClass.getClassFile();
        return ccFile.getConstPool();
    }
}