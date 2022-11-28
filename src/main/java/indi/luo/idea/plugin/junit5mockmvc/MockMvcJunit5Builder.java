package indi.luo.idea.plugin.junit5mockmvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.apigcc.core.schema.Method;
import com.github.apigcc.core.schema.Section;
import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.search.GlobalSearchScope;
import org.apache.commons.collections.map.MultiValueMap;

import java.util.ArrayList;

/**
 * author WangYi
 * created on 2017/1/15.
 */
public class MockMvcJunit5Builder extends BaseBuilder {
    @Override
    public void build(PsiElementFactory elementFactory, Project project, PsiClass psiClass, PsiMethod psiMethod, String className, Section section) {
//        if (psiClass.getConstructors().length == 0) {
//            PsiMethod constructor = elementFactory.createConstructor();
//            constructor.getModifierList().setModifierProperty(PsiModifier.PRIVATE, true);
//            psiClass.add(constructor);
//        }

        PsiField psiField = elementFactory.createField("mockMvc",
                PsiType.getTypeByName("MockMvc", project, GlobalSearchScope.EMPTY_SCOPE));
        if (!containFiled(psiClass, psiField)) {
            psiField.getModifierList().addAnnotation("org.springframework.beans.factory.annotation.Autowired");
            psiClass.add(psiField);
        }

        String methodText = buildMethodText(psiMethod, psiField, section);
        PsiMethod psiMethod2 = elementFactory.createMethodFromText(methodText, psiClass);
        if (!containMethod(psiClass, psiMethod2)) {
            psiClass.add(psiMethod2);
        }
    }


    private String buildMethodText(PsiMethod psiMethod, PsiField psiField, Section section) {
        StringBuilder b = new StringBuilder();
        ArrayList<String> params = Lists.newArrayList();
        for (PsiParameter parameter : psiMethod.getParameterList().getParameters()) {
            b.append(parameter.getType().getPresentableText()).append(" ").append(parameter.getName())
                    .append(" = ")
                    .append("null")
                    .append(";").append(System.lineSeparator());
            params.add(parameter.getName());
        }
        String getOrPost;
        if (section.getMethod() == Method.POST) {
            getOrPost = buildPostText(section);
        } else {
            getOrPost = buildGetText(section, psiMethod);
        }

        String invoke = psiField.getName() + "." + psiMethod.getName() + "(" + String.join(", ", params) + ")";
        return "@Test\n" +
                "    void " + psiMethod.getName() + "() {\n" +
                "       " + b +
                "       System.out.println(\n" +
                "                mockMvc.perform(MockMvcRequestBuilders\n" +
                "                                "+ getOrPost +
                "                        ).andReturn()\n" +
                "                        .getResponse()\n" +
                "                        .getContentAsString()\n" +
                "        );" +
                "    }";
    }

    private String buildGetText(Section section, PsiMethod psiMethod) {
        String requestBuilder = "." + "get(\"" + section.getUri() + "\")\n";
//        MultiValueMap vals = new MultiValueMap();

        requestBuilder += ".params(vals)\n";
//                ".queryParams(vals)\n";
        return requestBuilder;
    }
    private String buildPostText(Section section) {
        String requestBuilder = "." + "post(\"" + section.getUri() + "\")\n" +
                ".contentType(MediaType.APPLICATION_JSON)";
        requestBuilder += ".content(JSON.toJSONString(vals))";
        return requestBuilder;
    }

}
