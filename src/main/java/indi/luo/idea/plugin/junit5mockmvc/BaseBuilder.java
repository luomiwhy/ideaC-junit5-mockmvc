package indi.luo.idea.plugin.junit5mockmvc;

import com.google.common.collect.Lists;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * author WangYi
 * created on 2017/1/14.
 */
public abstract class BaseBuilder {

    static final String SLASH = "/";
    List<String> excludeParamTypes = Lists.newArrayList("RedirectAttributes", "HttpServletRequest", "HttpServletResponse");
    List<String> primitiveList = Lists.newArrayList("int", "boolean", "byte", "short", "long", "float", "double", "char",
            "Boolean", "Byte", "Short", "Integer", "Long", "Float", "Double", "String",
            "Date", "BigDecimal", "LocalDateTime", "BigInteger");

    Path outputFile;
    Project project;
    PsiElementFactory elementFactory;

    public void build(AnActionEvent event) {
        project = event.getProject();
        String projectBasePath = project.getBasePath();
        assert projectBasePath != null;
        PsiFile psiFile = event.getData(LangDataKeys.PSI_FILE);
        if (psiFile == null) return;
        outputFile = Paths.get(projectBasePath, "src", "test", "java");
        elementFactory = JavaPsiFacade.getElementFactory(project);

        WriteCommandAction.runWriteCommandAction(event.getProject(), () -> {
            PsiMethod sourcePsiMethod = (PsiMethod) event.getData(LangDataKeys.PSI_ELEMENT);
            PsiClass psiClass = PsiTreeUtil.getParentOfType(sourcePsiMethod, PsiClass.class);
            if (psiClass == null) return;
            if (psiClass.getNameIdentifier() == null) return;
            String className = psiClass.getNameIdentifier().getText();
            String packageName = ((PsiJavaFileImpl) psiFile).getPackageName();
            for (String s : packageName.split("\\.")) {
                outputFile = outputFile.resolve(s);
            }
            String testClassName = className + "Test";
            outputFile=outputFile.resolve(testClassName + "." + psiFile.getVirtualFile().getExtension());

            String classPath = psiFile.getVirtualFile().getPath();
            DumbService dumbService = DumbService.getInstance(project);

            VirtualFile virtualFile = getVF(outputFile);
            if (virtualFile == null || !virtualFile.exists()) {
                if (!outputFile.toFile().exists()) {
                    try {
                        Files.createDirectories(outputFile.getParent());
                        Files.createFile(outputFile);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                dumbService.runWithAlternativeResolveEnabled(()->{
                    PsiJavaFile pf = (PsiJavaFile) PsiManager.getInstance(project).findFile(getVF(outputFile));
                    PsiClass testPsiClass = initTestClass(packageName, testClassName, elementFactory, pf);

                    build(elementFactory, project, testPsiClass, sourcePsiMethod, className);
                    pf.add(testPsiClass);
                });
            }else {
                PsiClass testPsiClass = findClass(packageName + "." + testClassName);
                build(elementFactory, project, testPsiClass, sourcePsiMethod, className);
            }
            dumbService.runWithAlternativeResolveEnabled(() ->
                    FileEditorManager.getInstance(project).openFile(getVF(outputFile), true, true));
        });
    }

    private PsiClass initTestClass(String packageName, String testClassName, PsiElementFactory elementFactory, PsiJavaFile pf) {
        pf.setPackageName(packageName);
        for (String s : Lists.newArrayList(
                "com.fasterxml.jackson.databind.ObjectMapper",
                "org.junit.jupiter.api.Test",
                "org.springframework.beans.factory.annotation.Autowired",
                "org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc",
                "org.springframework.boot.test.context.SpringBootTest",
                "org.springframework.http.MediaType",
                "org.springframework.test.web.servlet.MockMvc",
                "org.springframework.test.web.servlet.request.MockMvcRequestBuilders")) {
            PsiClass c = findClass(s);
            if (c != null) {
                pf.getImportList().add(elementFactory.createImportStatement(c));
            }
        }
        PsiClass testPsiClass = elementFactory.createClass(testClassName);
        testPsiClass.getModifierList().addAnnotation("org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc");
        testPsiClass.getModifierList().addAnnotation("org.springframework.boot.test.context.SpringBootTest");
        return testPsiClass;
    }

    public abstract void build(PsiElementFactory elementFactory, Project project, PsiClass testPsiClass, PsiMethod sourcePsiMethod,
                               String className);

    protected boolean containFiled(PsiClass psiClass, PsiField psiField) {
        return psiClass.findFieldByName(psiField.getName(), true) != null;
    }

    protected boolean containMethod(PsiClass psiClass, PsiMethod psiMethod) {
        return psiClass.findMethodsByName(psiMethod.getName(), true).length > 0;
    }

    protected boolean containClass(PsiClass psiClass, PsiClass innerClass) {
        return psiClass.findInnerClassByName(innerClass.getName(), true) != null;
    }

    PsiClass findClass(String qualifiedName) {
        return JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.allScope(project));
    }

    VirtualFile getVF(Path path) {
        return VirtualFileManager.getInstance().refreshAndFindFileByNioPath(outputFile);
    }

    boolean isPrimitive(String typeName) {
        return primitiveList.contains(typeName);
    }
}
