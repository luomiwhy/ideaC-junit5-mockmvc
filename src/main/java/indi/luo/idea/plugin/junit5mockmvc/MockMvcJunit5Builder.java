package indi.luo.idea.plugin.junit5mockmvc;

import com.google.common.collect.Lists;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import indi.luo.idea.plugin.junit5mockmvc.constant.RequestMethodEnum;
import indi.luo.idea.plugin.junit5mockmvc.constant.WebAnnotation;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * author WangYi
 * created on 2017/1/15.
 */
public class MockMvcJunit5Builder extends BaseBuilder {
    @Override
    public void build(PsiElementFactory elementFactory, Project project, PsiClass testPsiClass, PsiMethod sourcePsiMethod, String className) {
//        if (psiClass.getConstructors().length == 0) {
//            PsiMethod constructor = elementFactory.createConstructor();
//            constructor.getModifierList().setModifierProperty(PsiModifier.PRIVATE, true);
//            psiClass.add(constructor);
//        }

        PsiField psiField = elementFactory.createField("mockMvc",
                PsiType.getTypeByName("MockMvc", project, GlobalSearchScope.EMPTY_SCOPE));
        psiField.getModifierList().setModifierProperty(PsiModifier.PRIVATE, false);
        if (!containFiled(testPsiClass, psiField)) {
            psiField.getModifierList().addAnnotation("org.springframework.beans.factory.annotation.Autowired");
            testPsiClass.add(psiField);
        }
        psiField = elementFactory.createField("objectMapper",
                PsiType.getTypeByName("ObjectMapper", project, GlobalSearchScope.EMPTY_SCOPE));
        psiField.getModifierList().setModifierProperty(PsiModifier.PRIVATE, false);
        if (!containFiled(testPsiClass, psiField)) {
            psiField.setInitializer(elementFactory.createExpressionFromText("new ObjectMapper()", null));
            testPsiClass.add(psiField);
        }

        String methodText = buildMethodText(testPsiClass, sourcePsiMethod);
        PsiMethod psiMethod2 = elementFactory.createMethodFromText(methodText, testPsiClass);
        if (!containMethod(testPsiClass, psiMethod2)) {
            testPsiClass.add(psiMethod2);
        }
    }


    private String buildMethodText(PsiClass psiClass, PsiMethod psiMethod) {
        PsiAnnotation methodMapping = getMethodMapping(psiMethod);
        RequestMethodEnum requestMethodEnum = getMethodFromAnnotation(methodMapping);
        String getOrPost, b="";
        if (requestMethodEnum == RequestMethodEnum.POST) {
            getOrPost = buildPostText(psiMethod);
            PsiParameter parameter =
                    (PsiParameter) Arrays.stream(psiMethod.getParameters())
                            .filter(p -> p.hasAnnotation("org.springframework.web.bind.annotation.RequestBody"))
                            .findFirst().get();
            ((PsiJavaFile) psiClass.getContainingFile()).getImportList()
                    .add(elementFactory.createImportStatement(findClass(parameter.getType().getCanonicalText())));
            b = buildObjectSet(parameter);
        } else {
            getOrPost = buildGetText(psiMethod);
        }

        return "@Test\n" +
                "    void " + psiMethod.getName() + "() throws Exception {\n" +
                b +
                "       System.out.println(mockMvc.perform(MockMvcRequestBuilders\n" +
                "                                "+ getOrPost +
                "                        ).andReturn()\n" +
                "                        .getResponse()\n" +
                "                        .getContentAsString()\n" +
                "        );" +
                "    }";
    }

    private String buildGetText(PsiMethod psiMethod) {
        String s = "." + "get(\"" + getUri(psiMethod) + "\")\n";
        for (String param : getParams(psiMethod)) {
            s += ".param(\"" + param + "\", \"\")\n";
        }
        return s;
    }

    private String buildObjectSet(PsiParameter parameter) {
        String c = parameter.getType().getPresentableText();
        String p = "param";
        StringBuilder sb = new StringBuilder(c).append(" ").append(p).append(" = ").append("new ").append(c).append("();");
        for (String param : getParams(parameter.getType())) {
            sb.append(p).append(".set").append(WordUtils.capitalize(param)).append("();\n");
        }
        return sb.toString();
    }

    private String buildPostText(PsiMethod psiMethod) {
        String requestBuilder = "." + "post(\"" + getUri(psiMethod) + "\")\n" +
                ".contentType(MediaType.APPLICATION_JSON)\n";
        requestBuilder += ".content(objectMapper.writeValueAsString(param))\n";
        return requestBuilder;
    }

    private List<String> getParams(PsiType psiType) {
        return getFieldFromClass(psiType);
    }
    private List<String> getParams(PsiMethod psiMethod) {
        PsiParameter[] psiParameters = psiMethod.getParameterList().getParameters();
        ArrayList<String> list = Lists.newArrayList();
        for (PsiParameter psiParameter : psiParameters) {
            PsiType psiType = psiParameter.getType();
            if (excludeParamTypes.contains(psiType.getPresentableText())) {
                continue;
            }
            if (isPrimitive(psiType.getPresentableText())) {
                list.add(psiParameter.getName());
            } else {
                list.addAll(getFieldFromClass(psiType));
            }
        }
        return list;
    }

    private List<String> getFieldFromClass(PsiType psiType) {
        PsiClass aClass = findClass(psiType.getCanonicalText());
        return Arrays.stream(aClass.getAllFields()).filter(psiField -> !psiField.hasModifier(JvmModifier.STATIC))
                .map(PsiField::getName).collect(Collectors.toList());
    }

    private RequestMethodEnum getMethodFromAnnotation(PsiAnnotation methodMapping) {
        String text = methodMapping.getText();
        if (text.contains(WebAnnotation.RequestMapping)) {
            return extractMethodFromAttribute(methodMapping);
        }
        return extractMethodFromMappingText(text);
    }
    private RequestMethodEnum extractMethodFromAttribute(PsiAnnotation annotation) {
        PsiNameValuePair[] psiNameValuePairs = annotation.getParameterList().getAttributes();
        for (PsiNameValuePair psiNameValuePair : psiNameValuePairs) {
            if ("method".equals(psiNameValuePair.getName())) {
                return RequestMethodEnum.valueOf(extractMethodName(psiNameValuePair));
            }
        }
        return RequestMethodEnum.GET;
    }
    private RequestMethodEnum extractMethodFromMappingText(String text) {
        if (text.contains(WebAnnotation.GetMapping)) {
            return RequestMethodEnum.GET;
        }
        if (text.contains(WebAnnotation.PutMapping)) {
            return RequestMethodEnum.PUT;
        }
        if (text.contains(WebAnnotation.DeleteMapping)) {
            return RequestMethodEnum.DELETE;
        }
        if (text.contains(WebAnnotation.PatchMapping)) {
            return RequestMethodEnum.PATCH;
        }
        return RequestMethodEnum.POST;
    }
    private String extractMethodName(PsiNameValuePair psiNameValuePair) {
        PsiAnnotationMemberValue value = psiNameValuePair.getValue();
        if(value != null) {
            PsiReference reference = value.getReference();
            if(reference != null) {
                PsiElement resolve = reference.resolve();
                if(resolve != null) {
                    return resolve.getText();
                }
            }
            PsiElement[] children = value.getChildren();
            if(children.length == 0) {
                return RequestMethodEnum.POST.name();
            }
            if(children.length > 1) {
                for (PsiElement child : children) {
                    if(child instanceof PsiReference) {
                        PsiElement resolve = ((PsiReference) child).resolve();
                        if(resolve != null) {
                            return resolve.getText();
                        }
                    }
                }
            }
        }
        return RequestMethodEnum.POST.name();
    }


    private String getUri(PsiMethod psiMethod) {
        PsiClass containingClass = psiMethod.getContainingClass();
        PsiAnnotation classRequestMapping = null;
        for (PsiAnnotation annotation : containingClass.getAnnotations()) {
            String text = annotation.getText();
            if (text.contains(WebAnnotation.RequestMapping)) {
                classRequestMapping = annotation;
            }
        }
        PsiAnnotation methodMapping = getMethodMapping(psiMethod);
        return buildPath(classRequestMapping, methodMapping);
    }

    private String buildPath(PsiAnnotation classRequestMapping, PsiAnnotation methodMapping) {
        String classPath = getPathFromAnnotation(classRequestMapping);
        String methodPath = getPathFromAnnotation(methodMapping);
        return classPath + methodPath;
    }
    private String getPathFromAnnotation(PsiAnnotation annotation) {
        if (annotation == null) {
            return "";
        }
        PsiNameValuePair[] psiNameValuePairs = annotation.getParameterList().getAttributes();
        if (psiNameValuePairs.length == 1 && psiNameValuePairs[0].getName() == null) {
            return appendSlash(psiNameValuePairs[0].getLiteralValue());
        }
        if (psiNameValuePairs.length >= 1) {
            for (PsiNameValuePair psiNameValuePair : psiNameValuePairs) {
                if (psiNameValuePair.getName().equals("value") || psiNameValuePair.getName().equals("path")) {
                    String text = psiNameValuePair.getValue().getText();
                    if(StringUtils.isEmpty(text)) {
                        return "";
                    }
                    text = text.replace("{\"","").replace("\"}","").replace("\"","");
                    if(text.contains(",")) {
                        return appendSlash(text.split(",")[0]);
                    }
                    return appendSlash(text);
                }
            }
        }
        return "";
    }
    private String appendSlash(String path) {
        if (StringUtils.isEmpty(path)) {
            return "";
        }
        String p = path;
        if (!path.startsWith(SLASH)) {
            p = SLASH + path;
        }
        if(path.endsWith(SLASH)) {
            p = p.substring(0,p.length()-1);
        }
        return p;
    }

    private PsiAnnotation getMethodMapping(PsiMethod psiMethod) {
        for (PsiAnnotation annotation : psiMethod.getAnnotations()) {
            String text = annotation.getText();
            if (text.contains("Mapping")) {
                return annotation;
            }
        }
        return null;
    }

}
