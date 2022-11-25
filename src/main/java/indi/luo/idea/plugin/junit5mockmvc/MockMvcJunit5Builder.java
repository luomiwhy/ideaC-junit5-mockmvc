package indi.luo.idea.plugin.junit5mockmvc;

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.StringJoiner;

/**
 * author WangYi
 * created on 2017/1/15.
 */
public class MockMvcJunit5Builder extends BaseBuilder {
    @Override
    public void build(PsiElementFactory elementFactory, Project project, PsiClass psiClass, PsiMethod psiMethod, String className) {
//        if (psiClass.getConstructors().length == 0) {
//            PsiMethod constructor = elementFactory.createConstructor();
//            constructor.getModifierList().setModifierProperty(PsiModifier.PRIVATE, true);
//            psiClass.add(constructor);
//        }

        PsiType psiType = PsiType.getTypeByName(className, project
                , GlobalSearchScope.EMPTY_SCOPE);
        PsiField psiField = elementFactory.createField(StringConvertUtil.UpperCamel2LowerCamel(className), psiType);

        if (!containFiled(psiClass, psiField)) {
            psiField.getModifierList().addAnnotation("org.springframework.beans.factory.annotation.Autowired");
            psiClass.add(psiField);
        }

        String methodText = buildMethodText(psiMethod, psiField);
        PsiMethod psiMethod2 = elementFactory.createMethodFromText(methodText, psiClass);
        if (!containMethod(psiClass, psiMethod2)) {
            psiClass.add(psiMethod2);
        }
    }


    private String buildMethodText(PsiMethod psiMethod, PsiField psiField) {
        StringBuilder b = new StringBuilder();
        ArrayList<String> params = Lists.newArrayList();
        for (PsiParameter parameter : psiMethod.getParameterList().getParameters()) {
            b.append(parameter.getType().getPresentableText()).append(" ").append(parameter.getName())
                    .append(" = ")
                    .append("null")
                    .append(";").append(System.lineSeparator());
            params.add(parameter.getName());
        }
        String invoke = psiField.getName() + "." + psiMethod.getName() + "(" + String.join(", ", params) + ")";
        return "@Test\n" +
                "    void " + psiMethod.getName() + "() {\n" +
                "       " + b +
                "       System.out.println(JSON.toJSONString(" + invoke + ", true));" +
                "    }";
    }

}
