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
    public static <T> Class<? extends T> lossless(Class<T> sourceClass) {
        ClassPool classPool = ClassPool.getDefault();
        classPool.insertClassPath(new ClassClassPath(LosslessJackson.class));

        CtClass losslessClass = createSubclass(sourceClass, classPool);

        addAttribute(losslessClass, "java.util.Map other = new java.util.LinkedHashMap();");

        addMethodWithAnnotation(
                losslessClass,
                "public java.util.Map any() {   \n" +
                    "    return other;          \n" +
                "}",
                ANY_GETTER_ANNOTATION);

        addMethodWithAnnotation(
                losslessClass,
                "public void set(String name, Object value) {   \n" +
                "    other.put(name, value);                    \n" +
                "}",
                ANY_SETTER_ANNOTATION);

        return toClass(losslessClass);
    }

    private static Class toClass(CtClass ctClass) {
        try {
            return ctClass.toClass();
        } catch (CannotCompileException e) {
            throw new IllegalArgumentException("supplied CtClass is not compilable", e);
        }
    }

    private static void addAttribute(CtClass losslessClass, String src) {
        try {
            CtField otherAttributesField = CtField.make(
                    src,
                    losslessClass);
            losslessClass.addField(otherAttributesField);
        } catch (CannotCompileException e) {
            throw new IllegalArgumentException("src is not compilable", e);
        }
    }

    private static void addMethodWithAnnotation(CtClass ctClass, String src, String annotationClass) {
        try {
            CtMethod anyMethod = CtNewMethod.make(src, ctClass);
            addAnnotationWithNoParams(ctClass, anyMethod, annotationClass);
            ctClass.addMethod(anyMethod);
        } catch (CannotCompileException e) {
            throw new IllegalArgumentException("src is not compilable", e);
        }
    }

    private static <T> CtClass createSubclass(Class<T> sourceClass, ClassPool classPool) {
        CtClass losslessClass = getCtClass(sourceClass, classPool);
        losslessClass.setName(sourceClass.getName() + "_Lossless");
        try {
            losslessClass.setSuperclass(getCtClass(sourceClass, classPool));
        } catch (CannotCompileException e) {
            // Should never happen
            throw new RuntimeException(e);
        }
        removeAllFieldsAndMethods(losslessClass);
        replaceBodyWithSuperCall(losslessClass.getConstructors()[0]);
        return losslessClass;
    }

    private static <T> CtClass getCtClass(Class<T> sourceClass, ClassPool classPool) {
        try {
            return classPool.get(sourceClass.getName());
        } catch (NotFoundException e) {
            throw new IllegalArgumentException(sourceClass.getName() + " couldn't be loaded by Javassist.", e);
        }
    }

    private static void replaceBodyWithSuperCall(CtConstructor constructor) {
        try {
            StringBuilder sb = new StringBuilder("super(");
            for (int i = 1; i <= constructor.getParameterTypes().length; i++) {
                sb.append("$").append(i);
                if (i < constructor.getParameterTypes().length) {
                    sb.append(", ");
                }
            }

            sb.append(");");
            constructor.setBody(sb.toString());
        } catch (NotFoundException e) {
            throw new IllegalArgumentException("Currently only constructors with parameters are supported");
        } catch (CannotCompileException e) {
            // This should never happen
            throw new RuntimeException(e);
        }
    }

    private static void removeAllFieldsAndMethods(CtClass losslessContactDetailsClass) {
        try {
            for (CtField field: losslessContactDetailsClass.getDeclaredFields()) {
                losslessContactDetailsClass.removeField(field);
            }

            for (CtMethod method: losslessContactDetailsClass.getDeclaredMethods()) {
                losslessContactDetailsClass.removeMethod(method);
            }
        } catch (NotFoundException e) {
            //This shouldn't ever happen
            throw new RuntimeException(e);
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