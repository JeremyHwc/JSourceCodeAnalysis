package com.tencent.annotationprocessor;

import com.tencent.annotations.BindView;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

/**
 * 在Java7以后，也可以使用注解来代替getSupportedSourceVersion和getSupportedAnnotationTypes
 * 但是考虑到Android的兼容性问题，这里不建议采用这种注解的方式
 */
//@SupportedSourceVersion(SourceVersion.RELEASE_8)
//@SupportedAnnotationTypes("com.tencent.annotations.BindView")
public class ClassProcess extends AbstractProcessor {

    /**
     * 只是必须指定的方法，指定这个注解处理器是注册给哪个注解的。注意，它的返回值是一个字符串的集合，
     * 包含本处理器想要处理的注解类型的合法全称
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(BindView.class.getCanonicalName());
        return annotations;
    }

    /**
     * 用来指定你用的Java版本，通常这里返回SourceVersion.latestSupported()
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * 被注解处理工具调用，并输入ProcessingEnvironment提供很多有用的工具类，比如Elements、Types、Filter和Messenger等
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
    }

    /**
     * 相当于每个注解处理器的主函数main()，在这里写你的扫描、评估和处理注解的代码，以及生成Java文件。输入RoundEnvironment
     * 可以让你查询出包含本处理器想要处理的注解类型的合法全称
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Messager messager = processingEnv.getMessager();
        for (Element element : roundEnvironment.getElementsAnnotatedWith(BindView.class)) {
            if (element.getKind() == ElementKind.FIELD) {
                messager.printMessage(Diagnostic.Kind.NOTE, "printMessage" + element.toString());
            }
        }
        return true;
    }
}
