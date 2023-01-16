package indi.luo.idea.plugin.junit5mockmvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.apigcc.core.schema.Method;
import com.github.apigcc.core.schema.Section;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.apache.commons.lang.WordUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

/**
 * author WangYi
 * created on 2017/1/15.
 */
public class MockMvcJunit5Builder extends BaseBuilder {
    @Override
    public void build(PsiElementFactory elementFactory, Project project, PsiClass testPsiClass, PsiMethod sourcePsiMethod, String className, Section section) {
//        if (psiClass.getConstructors().length == 0) {
//            PsiMethod constructor = elementFactory.createConstructor();
//            constructor.getModifierList().setModifierProperty(PsiModifier.PRIVATE, true);
//            psiClass.add(constructor);
//        }

        PsiField psiField = elementFactory.createField("mockMvc",
                PsiType.getTypeByName("MockMvc", project, GlobalSearchScope.EMPTY_SCOPE));
        if (!containFiled(testPsiClass, psiField)) {
            psiField.getModifierList().addAnnotation("org.springframework.beans.factory.annotation.Autowired");
            testPsiClass.add(psiField);
        }
        psiField = elementFactory.createField("objectMapper",
                PsiType.getTypeByName("ObjectMapper", project, GlobalSearchScope.EMPTY_SCOPE));
        if (!containFiled(testPsiClass, psiField)) {
            psiField.setInitializer(elementFactory.createExpressionFromText("new ObjectMapper()", null));
            testPsiClass.add(psiField);
        }

        String methodText = buildMethodText(testPsiClass, sourcePsiMethod, section);
        PsiMethod psiMethod2 = elementFactory.createMethodFromText(methodText, testPsiClass);
        if (!containMethod(testPsiClass, psiMethod2)) {
            testPsiClass.add(psiMethod2);
        }
    }


    private String buildMethodText(PsiClass psiClass, PsiMethod psiMethod, Section section) {
        String getOrPost, b="";
        if (section.getMethod() == Method.POST) {
            getOrPost = buildPostText(section);
            PsiParameter parameter =
                    (PsiParameter) Arrays.stream(psiMethod.getParameters())
                            .filter(p -> p.hasAnnotation("org.springframework.web.bind.annotation.RequestBody"))
                            .findFirst().get();
            ((PsiJavaFile)psiClass.getContainingFile()).getImportList()
                    .add(elementFactory.createImportStatement(findClass(parameter.getType().getCanonicalText())));
            b=buildObjectSet(parameter,section);
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

    private String buildObjectSet(PsiParameter parameter,Section section) {
        String c = parameter.getType().getPresentableText();
        String p = "param";
        StringBuilder sb = new StringBuilder(c).append(" ").append(p).append(" = ").append("new ").append(c).append("();");
        for (Iterator<Map.Entry<String, JsonNode>> it = section.getParameter().fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            sb.append(p).append(".set").append(WordUtils.capitalize(entry.getKey())).append("(")
                    .append(entry.getValue().asText()).append(");\n");
        }
        return sb.toString();
    }

    private String buildPostText(Section section) {
        String requestBuilder = "." + "post(\"" + section.getUri() + "\")\n" +
                ".contentType(MediaType.APPLICATION_JSON)\n";
        requestBuilder += ".content(objectMapper.writeValueAsString(param))\n";
        return requestBuilder;
    }

}
