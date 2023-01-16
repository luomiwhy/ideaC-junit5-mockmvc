package indi.luo.idea.plugin.junit5mockmvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.apigcc.core.schema.Method;
import com.github.apigcc.core.schema.Row;
import com.github.apigcc.core.schema.Section;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.apache.commons.collections.map.MultiValueMap;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

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

        String methodText = buildMethodText(psiMethod, section);
        PsiMethod psiMethod2 = elementFactory.createMethodFromText(methodText, psiClass);
        if (!containMethod(psiClass, psiMethod2)) {
            psiClass.add(psiMethod2);
        }
    }


    private String buildMethodText(PsiMethod psiMethod, Section section) {
        String getOrPost, b="";
        if (section.getMethod() == Method.POST) {
            getOrPost = buildPostText(section);
            PsiParameter parameter =
                    (PsiParameter) Arrays.stream(psiMethod.getParameters())
                            .filter(p -> p.hasAnnotation("org.springframework.web.bind.annotation.RequestBody"))
                            .findFirst().get();
//            ((PsiJavaFile)psiMethod.getContainingFile()).getImportList()
//                    .add(elementFactory.createImportStatement(parameter.getType()));
//            psiMethod.getContainingClass().getImplementsList().add(elementFactory.createImportStatement(c))
        } else {
            getOrPost = buildGetText(section, psiMethod);
        }

        return "@Test\n" +
                "    void " + psiMethod.getName() + "() throws Exception {\n" +
                b +
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
        String s = "." + "get(\"" + section.getUri() + "\")\n";
        for (Iterator<Map.Entry<String, JsonNode>> it = section.getParameter().fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            s += ".param(\"" + entry.getKey() + "\", \"" + entry.getValue().asText() + "\")\n";
        }
        return s;
    }

    private String buildPostText(Section section) {
        String requestBuilder = "." + "post(\"" + section.getUri() + "\")\n" +
                ".contentType(MediaType.APPLICATION_JSON)\n";
        requestBuilder += ".content(JSON.toJSONString(vals))\n";
        return requestBuilder;
    }

}
